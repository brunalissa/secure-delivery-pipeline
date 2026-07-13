# ═══════════════════════════════════════════════════════════════════════════════
# main.tf — Root Terraform Configuration (Orchestrator)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This is the ENTRY POINT for Terraform. It:
#   1. Configures the AWS provider
#   2. Instantiates modular infrastructure modules (VPC, Security, EC2, IAM)
#   3. Passes variables to each module and wires outputs together
#
# MODULAR ARCHITECTURE:
#   Each module is self-contained in terraform/modules/<name>/.
#   The root file orchestrates them — like a "main()" function for infra.
#
# Usage:
#   terraform init      # Download providers & modules
#   terraform plan      # Show what will be created
#   terraform apply     # Create the infrastructure
#   terraform destroy   # Tear everything down
# ═══════════════════════════════════════════════════════════════════════════════

terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
  }

  # ── Remote State ──────────────────────────────────────────────────────────
  # Remote state is stored in S3 with state locking via DynamoDB.
  # This enables team collaboration (multiple people running Terraform safely).
  #
  # NOTE: The S3 bucket and DynamoDB table must be created BEFORE running
  # terraform init. See the "bootstrap" section in docs/terraform.md.
  #
  # backend "s3" {
  #   bucket         = "secure-delivery-pipeline-tfstate"
  #   key            = "infrastructure/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "terraform-locks"
  #   encrypt        = true
  # }
}

# ─────────────────────────────────────────────────────────────────────────────
# AWS Provider — configures the connection to AWS
# ─────────────────────────────────────────────────────────────────────────────
provider "aws" {
  region = var.aws_region

  # Tags applied to ALL resources created by this Terraform project.
  # Useful for cost tracking, resource identification, and compliance.
  default_tags {
    tags = {
      Project     = "secure-delivery-pipeline"
      Environment = var.environment
      ManagedBy   = "terraform"
      Owner       = "devsecops-team"
    }
  }
}

# ═════════════════════════════════════════════════════════════════════════════
# MODULE: VPC — Virtual Private Cloud (Network Foundation)
# ═════════════════════════════════════════════════════════════════════════════
module "vpc" {
  source = "./modules/vpc"

  environment       = var.environment
  cidr_block        = var.vpc_cidr
  availability_zones = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs

  tags = {
    Project     = "secure-delivery-pipeline"
    Environment = var.environment
  }
}

# ═════════════════════════════════════════════════════════════════════════════
# MODULE: Security Groups — Firewall Rules for EC2 instances
# ═════════════════════════════════════════════════════════════════════════════
module "security_groups" {
  source = "./modules/security_groups"

  environment = var.environment
  vpc_id      = module.vpc.vpc_id
  vpc_cidr    = var.vpc_cidr

  tags = {
    Project     = "secure-delivery-pipeline"
    Environment = var.environment
  }
}

# ═════════════════════════════════════════════════════════════════════════════
# MODULE: IAM — Roles and Policies for EC2 instances
# ═════════════════════════════════════════════════════════════════════════════
module "iam" {
  source = "./modules/iam"

  environment = var.environment

  tags = {
    Project     = "secure-delivery-pipeline"
    Environment = var.environment
  }
}

# ═════════════════════════════════════════════════════════════════════════════
# MODULE: EC2 — Compute Instances
# ═════════════════════════════════════════════════════════════════════════════
module "ec2" {
  source = "./modules/ec2"

  environment          = var.environment
  vpc_id               = module.vpc.vpc_id
  private_subnet_ids   = module.vpc.private_subnet_ids
  security_group_id    = module.security_groups.ec2_security_group_id
  iam_instance_profile = module.iam.ec2_instance_profile_name
  instance_type        = var.instance_type
  key_name             = var.key_name
  min_instances        = var.min_instances
  max_instances        = var.max_instances

  tags = {
    Project     = "secure-delivery-pipeline"
    Environment = var.environment
  }
}