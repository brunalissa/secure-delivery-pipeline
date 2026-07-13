# ═══════════════════════════════════════════════════════════════════════════════
# modules/security_groups/main.tf — Security Group Module
# ═══════════════════════════════════════════════════════════════════════════════
#
# Security Groups (SGs) are AWS virtual firewalls attached to resources.
# They are STATEFUL — return traffic is automatically allowed, regardless of rules.
# Best practices demonstrated here:
#   - Least privilege: only necessary ports are open
#   - No 0.0.0.0/0 for sensitive ports (SSH, DB)
#   - DB SG only allows traffic from EC2 SG (layered security)
#   - All ports documented with comments
# ═══════════════════════════════════════════════════════════════════════════════

# ── EC2 Application Security Group ─────────────────────────────────────────────
# Allows HTTP/HTTPS from the internet (for web apps/APIs).
# SSH access is restricted to the VPC CIDR (bastion host pattern) — NOT open to 0.0.0.0/0.
resource "aws_security_group" "ec2" {
  name        = "${var.environment}-ec2-sg"
  description = "Security group for EC2 application instances"
  vpc_id      = var.vpc_id

  # HTTP — port 8080 (Spring Boot default)
  # In production, this should be restricted to the ALB SG, not 0.0.0.0/0.
  ingress {
    description = "HTTP application port"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Actuator health/metrics port (usually proxied by ALB, but open for K8s probes)
  ingress {
    description = "Actuator health and metrics"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # SSH — restricted to VPC internal traffic (bastion host pattern)
  # NEVER open SSH to 0.0.0.0/0 in production. Use SSM Session Manager instead.
  ingress {
    description = "SSH from VPC (bastion host access)"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # All outbound traffic (internet access for updates, API calls)
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-ec2-sg"
  })
}

# ── Database Security Group ────────────────────────────────────────────────────
# Only allows PostgreSQL traffic (port 5432) FROM the EC2 SG.
# This is the layered security pattern: DB is only accessible from the app layer.
resource "aws_security_group" "db" {
  name        = "${var.environment}-db-sg"
  description = "Security group for PostgreSQL database — only accepts traffic from EC2 SG"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from EC2 SG"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]  # Reference by SG ID
  }

  # No outbound rules — databases don't need internet access.
  # If outbound is needed (e.g., for updates), add a restricted egress rule.

  tags = merge(var.tags, {
    Name = "${var.environment}-db-sg"
  })
}