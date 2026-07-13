# ═══════════════════════════════════════════════════════════════════════════════
# variables.tf — Input Variables for Root Module
# ═══════════════════════════════════════════════════════════════════════════════
#
# Variables make Terraform reusable. Instead of hardcoding values, we define
# variables with defaults that can be overridden:
#   - Via terraform.tfvars file (see terraform.tfvars.example)
#   - Via command line: terraform apply -var="environment=prod"
#   - Via environment variables: TF_VAR_environment=prod
#
# Each variable includes:
#   - description: Human-readable explanation (shows in `terraform plan`)
#   - type:        Terraform type constraint (string, list, map, object)
#   - default:     Default value if not overridden
#   - validation:  Custom validation rules (optional)
# ═══════════════════════════════════════════════════════════════════════════════

# ─────────────────────────────────────────────────────────────────────────────
# Environment & Region
# ─────────────────────────────────────────────────────────────────────────────

variable "environment" {
  description = "Deployment environment name (dev, staging, prod). Used for naming and tagging resources."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}

variable "aws_region" {
  description = "AWS region where all resources will be created. Example: us-east-1, eu-west-1."
  type        = string
  default     = "us-east-1"

  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-[0-9]+$", var.aws_region))
    error_message = "AWS region must be in format like 'us-east-1'."
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# VPC / Network Configuration
# ─────────────────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC. Must be a valid /16 or larger network."
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR must be a valid CIDR block."
  }
}

variable "availability_zones" {
  description = "List of AWS availability zones for high availability. Minimum 2 for HA."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ). These have internet access via IGW."
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (one per AZ). No direct internet access — NAT gateway required for outbound."
  type        = list(string)
  default     = ["10.0.3.0/24", "10.0.4.0/24"]
}

# ─────────────────────────────────────────────────────────────────────────────
# EC2 Configuration
# ─────────────────────────────────────────────────────────────────────────────

variable "instance_type" {
  description = "EC2 instance type. t3.micro is free-tier eligible. Use t3.small+ for production."
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "Name of the SSH key pair already registered in AWS. Used for EC2 SSH access."
  type        = string
  default     = "secure-delivery-pipeline-key"
}

variable "min_instances" {
  description = "Minimum number of EC2 instances in the Auto Scaling Group (for HA, use >= 2)."
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum number of EC2 instances the Auto Scaling Group can scale to."
  type        = number
  default     = 4
}