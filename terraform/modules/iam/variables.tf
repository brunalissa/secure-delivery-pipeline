variable "environment" {
  description = "Environment name."
  type        = string
}

variable "tags" {
  description = "Tags to apply to all IAM resources."
  type        = map(string)
  default     = {}
}