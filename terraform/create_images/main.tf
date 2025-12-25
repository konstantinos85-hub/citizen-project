provider "aws" {
  region = var.region
}

# --- 1. DATA SOURCES (Αυτόματος εντοπισμός VPC/Subnets) ---
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# --- 2. SECURITY GROUPS (Κανόνες Πρόσβασης) ---

# SG για τον Load Balancer
resource "aws_security_group" "lb_sg" {
  name   = "citizen-alb-sg"
  vpc_id = data.aws_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# SG για την Spring Boot Εφαρμογή (Πόρτα 8089)
resource "aws_security_group" "app_sg" {
  name   = "citizen-app-sg"
  vpc_id = data.aws_vpc.default.id

  ingress {
    from_port       = 8089
    to_port         = 8089
    protocol        = "tcp"
    security_groups = [aws_security_group.lb_sg.id]
  }

  # SSH κανόνας για να μην βγάζει timeout το remote-exec
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# SG για τη MySQL
resource "aws_security_group" "db_sg" {
  name   = "citizen-db-sg"
  vpc_id = data.aws_vpc.default.id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app_sg.id]
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# --- 3. ΦΑΣΗ ΠΡΟΕΤΟΙΜΑΣΙΑΣ: ΔΗΜΙΟΥΡΓΙΑ ΕΙΚΟΝΩΝ (AMIs) ---

resource "aws_instance" "db_temp" {
  ami                         = "ami-00f46ccd1cbfb363e" # Ubuntu 24.04 us-west-2
  instance_type               = var.instance_type_db
  key_name                    = var.key_name
  vpc_security_group_ids      = [aws_security_group.db_sg.id]
  associate_public_ip_address = true

  user_data = <<-EOF
                #!/bin/bash
                sudo apt-get update
                sudo apt-get install -y mysql-server
                sudo sed -i 's/^bind-address\s*=.*$/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf
                sudo systemctl restart mysql
                sudo mysql -e "CREATE USER '${var.db_user}'@'%' IDENTIFIED BY '${var.db_password}';"
                sudo mysql -e "CREATE DATABASE ${var.db_name};"
                sudo mysql -e "GRANT ALL PRIVILEGES ON ${var.db_name}.* TO '${var.db_user}'@'%';"
                sudo mysql -e "FLUSH PRIVILEGES;"
                touch /home/ubuntu/db_ready
                EOF
}

resource "null_resource" "wait_db" {
  depends_on = [aws_instance.db_temp]
  provisioner "remote-exec" {
    inline = ["while [ ! -f /home/ubuntu/db_ready ]; do sleep 10; done"]
    connection {
      type        = "ssh"
      user        = "ubuntu"
      private_key = file("/Users/konstantinoskouyouris/Desktop/Multi-cloud-DevOps-Engineer/Υπολογιστικό Νέφος και Διαχείριση Πόρων Κατά Μήκος Νεφών/citizen-project-main/terraform/cloud.test.pem")
      host        = aws_instance.db_temp.public_ip
    }
  }
}

resource "aws_ami_from_instance" "db_ami" {
  name               = "citizen-db-ami-final-2025"
  source_instance_id = aws_instance.db_temp.id
  depends_on         = [null_resource.wait_db]
}

resource "aws_instance" "app_temp" {
  ami                         = "ami-00f46ccd1cbfb363e"
  instance_type               = var.instance_type_app
  key_name                    = var.key_name
  vpc_security_group_ids      = [aws_security_group.app_sg.id]
  associate_public_ip_address = true

  user_data = <<-EOF
                #!/bin/bash
                sudo apt-get update
                sudo apt-get install -y openjdk-21-jdk git maven
                cd /home/ubuntu
                git clone -b ${var.git_repo_branch} ${var.spring_boot_app_git-repo} citizen-app
                cd citizen-app
                mvn clean package -DskipTests
                touch /home/ubuntu/app_ready
                EOF
}

resource "null_resource" "wait_app" {
  depends_on = [aws_instance.app_temp]
  provisioner "remote-exec" {
    inline = ["while [ ! -f /home/ubuntu/app_ready ]; do sleep 10; done"]
    connection {
      type        = "ssh"
      user        = "ubuntu"
      private_key = file("/Users/konstantinoskouyouris/Desktop/Multi-cloud-DevOps-Engineer/Υπολογιστικό Νέφος και Διαχείριση Πόρων Κατά Μήκος Νεφών/citizen-project-main/terraform/cloud.test.pem")
      host        = aws_instance.app_temp.public_ip
    }
  }
}

resource "aws_ami_from_instance" "app_ami" {
  name               = "citizen-app-ami-final-2025"
  source_instance_id = aws_instance.app_temp.id
  depends_on         = [null_resource.wait_app]
}

# --- 4. ΦΑΣΗ ΕΚΤΕΛΕΣΗΣ: DEPLOYMENT ---

resource "aws_instance" "db_prod" {
  ami                    = aws_ami_from_instance.db_ami.id
  instance_type          = var.instance_type_db
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  tags = { Name = "Citizen-DB-Final" }
}

resource "aws_instance" "app_prod" {
  count                  = 3
  ami                    = aws_ami_from_instance.app_ami.id
  instance_type          = var.instance_type_app
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  
  user_data = <<-EOF
                #!/bin/bash
                export DB_HOST=${aws_instance.db_prod.private_ip}
                export DB_NAME=${var.db_name}
                export DB_USER=${var.db_user}
                export DB_PASSWORD=${var.db_password}
                java -jar /home/ubuntu/citizen-app/target/*.jar &
                EOF

  tags = { Name = "Citizen-App-${count.index + 1}" }
}

# --- 5. LOAD BALANCER ---

resource "aws_lb" "citizen_lb" {
  name               = "citizen-load-balancer"
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb_sg.id]
  subnets            = data.aws_subnets.default.ids
}

resource "aws_lb_target_group" "citizen_tg" {
  name     = "citizen-tg"
  port     = 8089 # Εδώ η πόρτα σας
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.citizen_lb.arn
  port              = "80"
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.citizen_tg.arn
  }
}

resource "aws_lb_target_group_attachment" "app_attach" {
  count            = 3
  target_group_arn = aws_lb_target_group.citizen_tg.arn
  target_id        = aws_instance.app_prod[count.index].id
  port             = 8089 # Εδώ η πόρτα σας
}
