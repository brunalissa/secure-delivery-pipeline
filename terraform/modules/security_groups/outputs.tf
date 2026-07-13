output "ec2_security_group_id" {
  description = "ID of the EC2 security group."
  value       = aws_security_group.ec2.id
}

output "db_security_group_id" {
  description = "ID of the database security group."
  value       = aws_security_group.db.id
}