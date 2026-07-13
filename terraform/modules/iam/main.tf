# ═══════════════════════════════════════════════════════════════════════════════
# modules/iam/main.tf — IAM Roles and Policies Module
# ═══════════════════════════════════════════════════════════════════════════════
#
# Creates:
#   1. IAM Role for EC2 instances (with trust policy allowing EC2 to assume it)
#   2. IAM Instance Profile (attaches the role to EC2 instances)
#   3. IAM Policy granting least-privilege permissions (SSM, S3, CloudWatch)
#
# SECURITY BEST PRACTICES:
#   - Least privilege: only the minimum permissions needed are granted
#   - SSM Session Manager replaces SSH (no inbound port 22 needed)
#   - CloudWatch Logs access for centralized logging
#   - S3 read access for application configuration / artifacts
# ═══════════════════════════════════════════════════════════════════════════════

# ── IAM Role for EC2 ───────────────────────────────────────────────────────────
# The trust policy allows EC2 to assume this role.
# The "Service": "ec2.amazonaws.com" principal is how AWS grants EC2 permission
# to use a role.
data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    # Security hardening: restrict which principals can assume this role
    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "${var.environment}-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = merge(var.tags, {
    Name = "${var.environment}-ec2-role"
  })
}

# ── IAM Policy — Least Privilege Permissions ───────────────────────────────────
# Grants the following permissions:
#   1. SSM Session Manager — SSH-less access (more secure than SSH)
#   2. CloudWatch Logs — send application logs
#   3. S3 (read-only on config bucket) — read application configuration
#   4. CloudWatch Metrics — push custom metrics

data "aws_iam_policy_document" "ec2_policy" {
  # SSM — for Session Manager (SSH-less access)
  statement {
    sid    = "SSMAccess"
    effect = "Allow"
    actions = [
      "ssmmessages:CreateControlChannel",
      "ssmmessages:CreateDataChannel",
      "ssmmessages:OpenControlChannel",
      "ssmmessages:OpenDataChannel",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = ["*"]
  }

  # CloudWatch Logs — application logging
  statement {
    sid    = "CloudWatchLogsAccess"
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:DescribeLogStreams"
    ]
    resources = ["arn:aws:logs:*:*:*"]
  }

  # S3 — read-only access to configuration bucket
  statement {
    sid    = "S3ConfigReadAccess"
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:GetBucketLocation"
    ]
    # Restrict to a specific bucket prefix — least privilege
    resources = [
      "arn:aws:s3:::secure-delivery-pipeline-config",
      "arn:aws:s3:::secure-delivery-pipeline-config/*"
    ]
  }

  # CloudWatch Metrics — custom metrics
  statement {
    sid    = "CloudWatchMetricsAccess"
    effect = "Allow"
    actions = [
      "cloudwatch:PutMetricData",
      "cloudwatch:GetMetricStatistics"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "ec2" {
  name   = "${var.environment}-ec2-policy"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ec2_policy.json
}

# ── IAM Instance Profile ────────────────────────────────────────────────────────
# An instance profile is a container for an IAM role that you can use to pass
# role information to an EC2 instance when the instance starts.
resource "aws_iam_instance_profile" "ec2" {
  name = "${var.environment}-ec2-profile"
  role = aws_iam_role.ec2.id

  tags = var.tags
}

# Get current AWS account ID for use in trust policy
data "aws_caller_identity" "current" {}