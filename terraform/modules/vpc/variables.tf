# modules/vpc/variables.tf — VPC Module Input Variables

variable "environment" {
  description = "Environment name for resource naming (dev, staging, prod)."
  type        = string
}

variable "cidr_block" {
  description = "CIDR block for the VPC."
  type        = string
}

variable "availability_zones" {
  description = "List of AWS availability zones."
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets."
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets."
  type        = list(string)
}

variable "tags" {
  description = "Tags to apply to all VPC resources."
  type        = map(string)
  default     = {}
}