# ═══════════════════════════════════════════════════════════════════════════════
# modules/ec2/main.tf — EC2 Compute Module
# ═══════════════════════════════════════════════════════════════════════════════
#
# Creates:
#   1. Launch Template — defines the EC2 configuration (AMI, instance type, user-data)
#   2. Auto Scaling Group — maintains min/max instances across AZs
#   3. User Data script — runs on first boot (installs Docker, app, etc.)
#
# The Auto Scaling Group ensures high availability:
#   - If an instance fails, a new one is automatically launched
#   - Instances are spread across availability zones
#   - Can scale based on CPU utilization, schedule, or custom metrics
# ═══════════════════════════════════════════════════════════════════════════════

# ── Data source: most recent Amazon Linux 2023 AMI ───────────────────────────
# Amazon Linux 2023 is the latest AWS-optimized Linux AMI.
# Using a data source ensures we always get the latest patches.
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

# ── Launch Template ───────────────────────────────────────────────────────────
# The launch template defines the instance configuration.
# Using a launch template (not launch configuration) allows versioning.
resource "aws_launch_template" "app" {
  name          = "${var.environment}-app-launch-template"
  image_id      = data.aws_ami.amazon_linux_2023.id
  instance_type = var.instance_type
  key_name      = var.key_name

  # IAM instance profile — grants the instance IAM permissions
  iam_instance_profile {
    name = var.iam_instance_profile
  }

  # Security group — controls network access
  vpc_security_group_ids = [var.security_group_id]

  # User data — script executed on first boot (base64-encoded by Terraform)
  user_data = base64encode(<<-EOF
#!/bin/bash
# This script runs when the EC2 instance first boots.
# It installs Docker, pulls the application image, and starts it.

set -euo pipefail
exec > >(tee /var/log/user-data.log) 2>&1

echo "=== Starting EC2 user-data script ==="

# Update system packages
yum update -y

# Install Docker
yum install -y docker
systemctl enable docker
systemctl start docker

# Add ec2-user to docker group
usermod -aG docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Pull and run the application container
# The image is hosted at GitHub Container Registry (GHCR)
# In production, use a private registry with IAM authentication
docker pull ghcr.io/devsecops/secure-delivery-pipeline:latest

docker run -d \
  --name secure-delivery-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=$${DB_URL:-jdbc:postgresql://localhost:5432/devsecops_db} \
  -e DB_USERNAME=$${DB_USERNAME:-devsecops_user} \
  -e DB_PASSWORD=$${DB_PASSWORD:-changeme} \
  ghcr.io/devsecops/secure-delivery-pipeline:latest

echo "=== User-data script completed ==="
EOF
  )

  # Tag the instances
  tag_specifications {
    resource_type = "instance"
    tags = merge(var.tags, {
      Name = "${var.environment}-app-instance"
    })
  }

  # Block device — EBS volume configuration
  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size           = 20   # GB
      volume_type           = "gp3" # General purpose SSD
      delete_on_termination = true
      encrypted             = true  # Encrypt the volume (security best practice)
    }
  }

  tags = var.tags
}

# ── Auto Scaling Group ─────────────────────────────────────────────────────────
# The ASG maintains the desired number of instances across multiple AZs.
resource "aws_autoscaling_group" "app" {
  name                = "${var.environment}-app-asg"
  vpc_zone_identifier = var.private_subnet_ids
  desired_capacity    = var.min_instances
  min_size            = var.min_instances
  max_size            = var.max_instances

  launch_template {
    id      = aws_launch_template.app.id
    version = "$Latest"
  }

  # Health check type — EC2 checks instance health
  health_check_type    = "EC2"
  health_check_grace_period = 300  # Wait 5 min before checking health (allows app to boot)

  # Tag all instances with the same tags
  dynamic "tag" {
    for_each = var.tags
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

# ── CloudWatch Alarm — CPU Auto Scaling ───────────────────────────────────────
# Scale UP when average CPU > 70% for 5 minutes
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "${var.environment}-high-cpu-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"  # 5 minutes
  statistic           = "Average"
  threshold           = "70"
  alarm_description   = "Scale up if CPU > 70%"

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app.name
  }
}

# ── Auto Scaling Policy — Scale Up ─────────────────────────────────────────────
resource "aws_autoscaling_policy" "scale_up" {
  name                   = "${var.environment}-scale-up"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.app.name
}

# ── Auto Scaling Policy — Scale Down ───────────────────────────────────────────
resource "aws_autoscaling_policy" "scale_down" {
  name                   = "${var.environment}-scale-down"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.app.name
}