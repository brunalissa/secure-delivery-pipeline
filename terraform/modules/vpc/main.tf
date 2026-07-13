# ═══════════════════════════════════════════════════════════════════════════════
# modules/vpc/main.tf — VPC Module
# ═══════════════════════════════════════════════════════════════════════════════
#
# Creates a production-grade VPC with:
#   - Public subnets (with Internet Gateway — resources have public IPs)
#   - Private subnets (no direct internet access — use NAT gateway for outbound)
#   - Route tables
#   - VPC flow logs endpoint (optional)
#
# ARCHITECTURE:
#
#   Internet → IGW → Public Subnets (ALB, Bastion)
#                  ↓
#                  Private Subnets (App, DB) → NAT GW → Internet (outbound only)
#
# Private subnets have NO inbound internet access — this is a security best practice.
# Outbound internet access (for updates, API calls) goes through a NAT Gateway
# located in a public subnet.
# ═══════════════════════════════════════════════════════════════════════════════

# ── VPC ────────────────────────────────────────────────────────────────────────
resource "aws_vpc" "this" {
  cidr_block           = var.cidr_block
  enable_dns_support   = true  # Required for EKS, RDS, and VPC endpoints
  enable_dns_hostnames = true  # Assign public DNS hostnames to instances

  tags = merge(var.tags, {
    Name = "${var.environment}-vpc"
  })
}

# ── Internet Gateway ──────────────────────────────────────────────────────────
# The IGW allows communication between the VPC and the public internet.
# It is attached to the VPC and referenced in the public route table.
resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.tags, {
    Name = "${var.environment}-igw"
  })
}

# ── Public Subnets ─────────────────────────────────────────────────────────────
# Resources in public subnets (ALB, Bastion hosts) have internet access.
# They auto-assign public IPs so they're reachable from outside the VPC.
resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true  # Auto-assign public IPs to instances

  tags = merge(var.tags, {
    Name = "${var.environment}-public-subnet-${count.index + 1}"
    # The "kubernetes.io/role/elb" tag tells EKS to find these subnets for ELB provisioning
    "kubernetes.io/role/elb" = "1"
  })
}

# ── Private Subnets ────────────────────────────────────────────────────────────
# Private subnets host databases, internal services, and EKS worker nodes.
# NO direct internet access — all outbound traffic goes through the NAT Gateway.
resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id            = aws_vpc.this.id
  cidr_block         = var.private_subnet_cidrs[count.index]
  availability_zone  = var.availability_zones[count.index]

  tags = merge(var.tags, {
    Name = "${var.environment}-private-subnet-${count.index + 1}"
    # For EKS internal ELBs
    "kubernetes.io/role/internal-elb" = "1"
  })
}

# ── Elastic IP for NAT Gateway ────────────────────────────────────────────────
# The NAT Gateway needs a static public IP (Elastic IP).
# This IP is the source IP for ALL outbound traffic from private subnets.
resource "aws_eip" "nat" {
  count = length(var.public_subnet_cidrs)

  domain = "vpc"

  tags = merge(var.tags, {
    Name = "${var.environment}-nat-eip-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.this]
}

# ── NAT Gateway ────────────────────────────────────────────────────────────────
# The NAT Gateway sits in the public subnet and provides outbound internet
# access for private subnet resources.
# In production, one NAT GW per AZ provides fault tolerance. This example
# creates one per public subnet.
resource "aws_nat_gateway" "this" {
  count = length(var.public_subnet_cidrs)

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(var.tags, {
    Name = "${var.environment}-nat-gw-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.this]
}

# ── Public Route Table ─────────────────────────────────────────────────────────
# Routes ALL traffic (0.0.0.0/0) from public subnets to the Internet Gateway.
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-public-rt"
  })
}

# Associate public subnets with the public route table
resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ── Private Route Tables — one per AZ (each routes to its own NAT GW) ───────────
resource "aws_route_table" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this[count.index].id
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-private-rt-${count.index + 1}"
  })
}

# Associate private subnets with their route tables
resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}