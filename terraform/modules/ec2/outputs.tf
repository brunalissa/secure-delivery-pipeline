output "instance_ids" {
  description = "IDs of the EC2 instances in the Auto Scaling Group."
  value       = aws_autoscaling_group.app.instance_ids
}

output "public_ips" {
  description = "Public IP addresses of the EC2 instances (if assigned)."
  value       = aws_autoscaling_group.app.*.public_ip
  # Note: instances in private subnets won't have public IPs.
  # This output will be empty for private deployments.
}

output "private_ips" {
  description = "Private IP addresses of the EC2 instances."
  value       = aws_autoscaling_group.app.*.private_ip
}

output "asg_name" {
  description = "Name of the Auto Scaling Group."
  value       = aws_autoscaling_group.app.name
}

output "launch_template_id" {
  description = "ID of the EC2 Launch Template."
  value       = aws_launch_template.app.id
}