# ═══════════════════════════════════════════════════════════════════════════════
# outputs.tf — Root Module Outputs
# ═══════════════════════════════════════════════════════════════════════════════
#
# Outputs expose values from the Terraform state to:
#   1. The user (printed after `terraform apply`)
#   2. Other Terraform configurations (via `terraform_remote_state`)
#   3. CI/CD pipelines (via `terraform output -json`)
#
# Outputs are NOT sensitive unless marked explicitly with `sensitive = true`.
# Mark outputs as sensitive if they contain passwords, tokens, or private IPs
# that should not appear in CI logs.
# ═══════════════════════════════════════════════════════════════════════════════

# ── VPC Outputs ──────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "ID of the created VPC. Used by other infrastructure to reference the network."
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC (e.g., 10.0.0.0/16)."
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets. Resources needing internet access go here."
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets. Resources NOT needing internet access go here (databases, internal services)."
  value       = module.vpc.private_subnet_ids
}

# ── Security Group Outputs ────────────────────────────────────────────────────

output "ec2_security_group_id" {
  description = "Security Group ID for EC2 instances. Attach this to instances."
  value       = module.security_groups.ec2_security_group_id
}

output "db_security_group_id" {
  description = "Security Group ID for database instances. Only allows traffic from EC2 SG."
  value       = module.security_groups.db_security_group_id
}

# ── IAM Outputs ───────────────────────────────────────────────────────────────

output "ec2_iam_role_arn" {
  description = "ARN of the IAM role assigned to EC2 instances."
  value       = module.iam.ec2_role_arn
}

output "ec2_instance_profile_name" {
  description = "Name of the IAM instance profile for EC2."
  value       = module.iam.ec2_instance_profile_name
}

# ── EC2 Outputs ──────────────────────────────────────────────────────────────

output "ec2_instance_ids" {
  description = "IDs of the EC2 instances created by the Auto Scaling Group."
  value       = module.ec2.instance_ids
}

output "ec2_public_ips" {
  description = "Public IP addresses of the EC2 instances."
  value       = module.ec2.public_ips
}

output "ec2_private_ips" {
  description = "Private IP addresses of the EC2 instances."
  value       = module.ec2.private_ips
  sensitive   = true  # Don't expose private IPs in CI logs
}