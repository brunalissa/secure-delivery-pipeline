variable "environment" {
  description = "Environment name."
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC where security groups will be created."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC (used for internal access rules)."
  type        = string
}

variable "tags" {
  description = "Tags to apply to all resources."
  type        = map(string)
  default     = {}
}