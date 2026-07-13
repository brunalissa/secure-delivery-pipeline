variable "environment" {
  description = "Environment name."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID (unused in module but kept for future use)."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the Auto Scaling Group."
  type        = list(string)
}

variable "security_group_id" {
  description = "Security Group ID to attach to EC2 instances."
  type        = string
}

variable "iam_instance_profile" {
  description = "IAM instance profile name to assign to EC2 instances."
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type."
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "SSH key pair name for EC2 access."
  type        = string
}

variable "min_instances" {
  description = "Minimum number of instances."
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum number of instances."
  type        = number
  default     = 4
}

variable "tags" {
  description = "Tags to apply to all EC2 resources."
  type        = map(string)
  default     = {}
}