# Terraform Deployment Guide

## Overview

The Terraform configuration provisions AWS infrastructure using a **modular architecture** — each infrastructure concern (VPC, Security Groups, IAM, EC2) is a self-contained, reusable module.

## Module Architecture

```
terraform/
├── main.tf              ← Root: Orchestrates all modules
├── variables.tf         ← Root: Input variables
├── outputs.tf           ← Root: Outputs (consumed by downstream tools)
├── terraform.tfvars.example  ← Example variable values
└── modules/
    ├── vpc/              ← Network: VPC, subnets, gateways, routes
    ├── security_groups/ ← Firewall: EC2 & DB ingress/egress rules
    ├── iam/             ← Identity: EC2 role, policies, instance profile
    └── ec2/             ← Compute: Launch template, Auto Scaling Group
```

## Network Topology

```
       Internet
          │
     ┌────┴────┐
     │   IGW    │
     └────┬────┘
          │
    ┌─────┴──────┐
    │ Public SN  │  ← NAT GW, Bastion, ALB
    └─────┬──────┘
          │
    ┌─────┴──────┐
    │ Private SN │  ← EC2 (app), PostgreSQL (RDS)
    └────────────┘
```

Private subnets have **no direct internet access**. Outbound traffic goes through the NAT Gateway. This is a security best practice — instances in private subnets cannot be reached from the internet.

## Getting Started

### 1. Configure AWS Credentials

```bash
# Option A: AWS CLI
aws configure

# Option B: Environment variables
export AWS_ACCESS_KEY_ID="your_key"
export AWS_SECRET_ACCESS_KEY="your_secret"
export AWS_DEFAULT_REGION="us-east-1"

# Verify
aws sts get-caller-identity
```

### 2. Set Up Remote State (Recommended for Team Use)

Create an S3 bucket and DynamoDB table before running `terraform init`:

```bash
# S3 bucket for state
aws s3api create-bucket \
  --bucket secure-delivery-pipeline-tfstate \
  --region us-east-1

# DynamoDB table for state locking
aws dynamodb create-table \
  --table-name terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema KeySchemaAttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

Then uncomment the `backend "s3"` block in `main.tf`.

### 3. Configure Variables

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your project-specific values.

### 4. Initialize, Plan, Apply

```bash
# Initialize Terraform (download providers & modules)
terraform init

# Validate
terraform validate

# Plan (see what WILL be created — not applied yet)
terraform plan

# Apply (provision the infrastructure)
terraform apply

# Output useful values
terraform output

# Destroy when done (saves money)
terraform destroy
```

## Variable Reference

| Variable | Type | Default | Description |
|---|---|---|---|
| `environment` | string | `dev` | Environment name (dev, staging, prod) |
| `aws_region` | string | `us-east-1` | AWS region |
| `vpc_cidr` | string | `10.0.0.0/16` | VPC CIDR block |
| `availability_zones` | list | `[us-east-1a, us-east-1b]` | AZ list |
| `public_subnet_cidrs` | list | `10.0.1.0/24, 10.0.2.0/24` | Public subnet CIDRs |
| `private_subnet_cidrs` | list | `10.0.3.0/24, 10.0.4.0/24` | Private subnet CIDRs |
| `instance_type` | string | `t3.micro` | EC2 instance type |
| `key_name` | string | `secure-delivery-pipeline-key` | SSH key pair |
| `min_instances` | number | `1` | Min EC2 instances |
| `max_instances` | number | `4` | Max EC2 instances |

## Security Features

1. **Private subnets** — EC2 instances are not accessible from the internet
2. **NAT Gateway** — Private instances can reach the internet for updates (outbound only)
3. **Security group chaining** — DB security group only accepts traffic from the EC2 security group
4. **EBS encryption** — All volumes are encrypted at rest
5. **IAM least-privilege** — EC2 role only has SSM, CloudWatch, and S3 read permissions
6. **Trust policy trust restrictions** — EC2 role can only be assumed within the same AWS account
7. **Default tags** — All resources tagged for cost tracking and compliance