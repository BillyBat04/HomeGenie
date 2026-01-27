# HomeGenie - Smart Maintenance Management System

> **AI-Powered Microservices Platform for Residential Maintenance Management**

![Version](https://img.shields.io/badge/version-2.1.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-green?logo=spring)
![React](https://img.shields.io/badge/React-19-blue?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)
![Stripe](https://img.shields.io/badge/Stripe-v24.0.0-blueviolet?logo=stripe)
![Virtual Threads](https://img.shields.io/badge/Virtual%20Threads-Enabled-success)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5.0-black?logo=apache-kafka)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [System Architecture](#-system-architecture)
- [Quick Start](#-quick-start)
- [Documentation](#-documentation)
- [Technology Stack](#-technology-stack)
- [Features](#-features)
- [Development](#-development)
- [Deployment](#-deployment)
- [Monitoring](#-monitoring)
- [Contributing](#-contributing)

---

## 🎯 Overview

HomeGenie is a production-ready, **cloud-native microservices platform** for managing maintenance operations in residential societies. Built with modern technologies and best practices, it provides:

- **AI-Powered Classification**: Automatic categorization of maintenance requests using Google Gemini AI
- **Voice Assistant**: Speech-to-text maintenance request creation with Google Speech Recognition
- **Integrated Payment Processing**: Seamless Stripe integration with webhook handling and saga pattern
- **Event-Driven Architecture**: Real-time notifications via Apache Kafka
- **High Performance**: Java 21 Virtual Threads for 10x throughput improvement
- **Enterprise Monitoring**: Prometheus + Grafana with 3 dashboards and 25+ alerts
- **Multi-Channel Notifications**: Email (SMTP/AWS SES) and SMS (AWS SNS) delivery
- **Mini-App Platform**: Extensible marketplace for third-party integrations

### Business Value

HomeGenie streamlines maintenance management by:

- Reducing maintenance request resolution time by **60%**
- Automating technician assignment based on specialty
- Providing transparent payment tracking with Stripe
- Enabling proactive maintenance with AI-powered insights
- Delivering real-time notifications to all stakeholders

---

## 🏗️ System Architecture

HomeGenie follows a **microservices architecture** with 7 core services:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Client Applications                               │
│                 (React SPA, Mobile App, Postman)                        │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       API Gateway (:8080)                                │
│      - JWT Authentication   - Rate Limiting (10 req/s)                  │
│      - CORS Management     - Request Correlation                        │
└──────┬─────┬─────┬─────┬─────┬─────┬──────────────────────────────────┘
       │     │     │     │     │     │
  ┌────▼┐ ┌──▼──┐┌─▼───┐┌▼────┐┌▼───────┐┌─▼─────────┐
  │User │ │Maint││Pay- ││Noti-││Identity││Marketplace│
  │8081 │ │8082 ││ment ││fic. ││Platform││  Service  │
  │     │ │     ││8083 ││8084 ││  8086  ││           │
  └──┬──┘ └──┬──┘└──┬──┘└──┬──┘└───┬────┘└─────┬─────┘
     │       │      │      │       │            │
     └───────┴──────┴──────┴───────┴────────────┘
                          ▼
       ┌────────────────────────────────────────────────┐
       │            Infrastructure Layer                │
       │  PostgreSQL  Redis  Kafka  Prometheus  Grafana │
       └────────────────────────────────────────────────┘
```

**Service Responsibilities**:

- **Gateway Service** (:8080): Entry point, authentication, rate limiting, shadow mode testing
- **User Service** (:8081): User management, JWT generation, authentication
- **Maintenance Service** (:8082): Request CRUD, AI classification, S3 uploads, warranty checks
- **Payment Service** (:8083): Stripe integration, invoice generation, webhooks, saga pattern
- **Notification Service** (:8084): Multi-channel delivery (Email/SMS), Kafka consumers, retry logic
- **Identity Platform** (:8086): Platform-level authentication, JWT management, security
- **Marketplace Service**: Mini-app ecosystem, payment routing, commission handling

📖 **Detailed Architecture**: [docs/SYSTEM-ARCHITECTURE.md](docs/SYSTEM-ARCHITECTURE.md)

---

## 🚀 Quick Start

### Prerequisites

- **Docker Desktop** 24.0+
- **Java** 21+ (OpenJDK or Oracle JDK)
- **Maven** 3.9+
- **Node.js** 18+ (for frontend)

### 5-Minute Setup

**Step 1: Clone & Configure**

```powershell
git clone https://github.com/your-org/homegenie.git
cd homegenie

# Copy environment template
cp .env.example .env

# Edit .env with your credentials (Stripe, Gemini, SMTP, AWS)
notepad .env
```

**Step 2: Start Infrastructure**

```powershell
# Start PostgreSQL, Redis, Kafka, Prometheus, Grafana
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose ps
```

**Step 3: Start Services**

```powershell
# Option A: Start all services with one command
.\start-all-services.ps1

# Option B: Start manually (separate terminals)
cd gateway-service && mvnw spring-boot:run    # Terminal 1
cd userservice && mvnw spring-boot:run        # Terminal 2
cd maintenanceservice && mvnw spring-boot:run # Terminal 3
cd paymentservice && mvnw spring-boot:run     # Terminal 4
cd notificationservice && mvnw spring-boot:run # Terminal 5
```

**Step 4: Start Frontend**

```powershell
cd homegenie-app
npm install
npm run dev  # Starts at http://localhost:5173
```

**Step 5: Verify**

```powershell
# Test Gateway health
curl http://localhost:8080/actuator/health

# Access Grafana
Start-Process "http://localhost:3000"  # Login: admin/admin
```

### Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Frontend** | <http://localhost:5173> | - |
| **API Gateway** | <http://localhost:8080> | - |
| **Swagger UI** | <http://localhost:8080/swagger-ui.html> | - |
| **Grafana** | <http://localhost:3000> | admin/admin |
| **Prometheus** | <http://localhost:9090> | - |
| **Kafka UI** | <http://localhost:8090> | - |
| **PgAdmin** | <http://localhost:5050> | <admin@homegenie.com>/admin |

## 🛠️ Technology Stack

### Backend Services

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 (LTS) | Programming language with Virtual Threads |
| **Spring Boot** | 3.3.0 | Microservices framework |
| **Spring Cloud Gateway** | 2023.0.3 | API Gateway with rate limiting |
| **Spring Data JPA** | - | ORM for PostgreSQL |
| **Flyway** | - | Database migrations |
| **Kafka** | 7.5.0 | Event streaming (Confluent Platform) |
| **JWT (JJWT)** | 0.11.5 | Authentication & authorization |
| **BCrypt** | - | Password hashing |

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| **React** | 19.1.1 | UI framework |
| **Vite** | 7.1.7 | Build tool |
| **TailwindCSS** | 3.4.18 | Styling |
| **Lucide React** | 0.548.0 | Icons |
| **Axios** | - | HTTP client |

### Databases & Caching

| Technology | Version | Purpose |
|------------|---------|---------|
| **PostgreSQL** | 15-alpine | Primary database (5 schemas: users, maintenance, payment, notification, marketplace) |
| **Redis** | 7-alpine | Rate limiting, caching |
| **Flyway** | - | Schema versioning |
| **HikariCP** | - | Connection pooling |

### External Integrations

| Service | Purpose | Documentation |
|---------|---------|---------------|
| **Stripe** | Payment processing (v24.0.0) | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md#2-stripe-payment-integration) |
| **Google Gemini AI** | Maintenance classification | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md#6-ai-classification-google-gemini) |
| **Google Speech API** | Voice-to-text for voice assistant | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md) |
| **AWS S3** | Image/video storage (1.12.565) | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md#7-cloud-storage-aws-s3) |
| **AWS SNS** | SMS notifications (1.12.529) | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md#5-sms-service-aws-sns) |
| **AWS SES** | Email delivery (1.12.770) | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md#4-email-service-smtp) |
| **Gmail SMTP** | Alternative email delivery | [INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md#4-email-service-smtp) |

### Monitoring & Observability

| Technology | Version | Purpose |
|------------|---------|---------|
| **Prometheus** | Latest | Metrics collection |
| **Grafana** | Latest | Metrics visualization (3 dashboards, 25+ alerts) |
| **Actuator** | - | Health checks, metrics endpoints |
| **Kafka Lag Exporter** | 0.8.2 | Consumer lag monitoring |

### DevOps & Tools

| Technology | Purpose |
|------------|---------|
| **Docker Compose** | Local development orchestration |
| **Maven** | Build automation |
| **Lombok** | Boilerplate code reduction |
| **SpringDoc OpenAPI** | API documentation |

📖 **Detailed Tech Stack**: [docs/SYSTEM-ARCHITECTURE.md](docs/SYSTEM-ARCHITECTURE.md#13-technology-stack)

---

## ✨ Features

### 🔐 Authentication & Authorization

- JWT-based authentication with refresh token rotation
- Role-based access control (ADMIN, TECHNICIAN, RESIDENT)
- BCrypt password hashing (cost factor: 10)
- Token expiration: Access (24h), Refresh (7d)

### 🏠 Maintenance Management

- **AI-Powered Classification**: Automatic category and priority detection using Google Gemini AI
- **Status Workflow**: PENDING → IN_PROGRESS → COMPLETED (with state validation)
- **Media Uploads**: Image/video upload to AWS S3 (10MB limit)
- **Technician Assignment**: Auto-assign based on specialty with status transition
- **Scheduled Maintenance**: Cron jobs for reminders (8:00 AM) and warranty checks (8:15 AM)
- **Voice Assistant**: Speech-to-text voice commands for creating maintenance requests using Google Speech Recognition

### 💳 Payment Processing

- **Stripe Integration**: Payment intents, webhooks, refunds
- **Saga Pattern**: Automatic compensation on transaction failures
- **Payment Validation**: Block payments for PENDING requests (critical business rule)
- **Invoice Generation**: Auto-numbered invoices (INV-YYYYMMDD-XXXXX)
- **Payment Limits**: $1.00 - $10,000.00 with configurable thresholds

### 🔔 Notifications

- **Multi-Channel Delivery**: Email (SMTP) and SMS (AWS SNS)
- **Event-Driven**: Kafka consumers for 4 topics (user, maintenance, payment, invoice events)
- **Retry Logic**: 3 attempts with exponential backoff (5m, 15m, 45m)
- **User Preferences**: Configurable email notification opt-in/opt-out

### 📊 Monitoring & Observability

- **Real-Time Metrics**: Prometheus scraping from all services
- **Custom Dashboards**: 3 Grafana dashboards (System Overview, API Gateway, Kafka)
- **Alerting**: 25+ alerts (CPU, memory, error rate, consumer lag)
- **Health Checks**: Liveness, readiness, and startup probes

### 🚀 Performance

- **Virtual Threads**: Java 21 Virtual Threads for 10x throughput improvement
- **Rate Limiting**: Token bucket algorithm (10 req/s, burst 20) via Redis
- **Connection Pooling**: HikariCP with optimized settings (max 10, min-idle 5)
- **Caching**: Redis for rate limiting and AI classification results
- **Shadow Mode Testing**: API Gateway supports canary deployments and A/B testing with auto-rollback

📖 **Feature Details**: [docs/BUSINESS-PROCESSES.md](docs/BUSINESS-PROCESSES.md)

---

## 💻 Development

### Project Structure

```
homegenie/
├── gateway-service/          # API Gateway (Port 8080)
├── userservice/              # User & Auth (Port 8081)
├── maintenanceservice/       # Maintenance CRUD (Port 8082)
├── paymentservice/           # Payments & Invoices (Port 8083)
├── notificationservice/      # Notifications (Port 8084)
├── platform-services/
│   ├── identity-service/     # Platform Identity (Port 8086)
│   └── payment-platform/     # Payment Platform Layer
├── marketplaceservice/       # Mini-app Marketplace
├── homegenie-app/            # React Frontend (Vite)
├── docs/                     # Comprehensive documentation
│   ├── SYSTEM-ARCHITECTURE.md
│   ├── BUSINESS-PROCESSES.md
│   ├── API-REFERENCE.md
│   ├── DATABASE-SCHEMA.md
│   ├── INTEGRATION-GUIDE.md
│   ├── DEPLOYMENT-GUIDE.md
│   └── BUSINESS-RULES.md
├── grafana/                  # Grafana dashboards & provisioning
├── k8s/                      # Kubernetes manifests
├── docker-compose.yml        # Infrastructure orchestration
└── .env                      # Environment variables
```

### Running Tests

```powershell
# Unit tests (all services)
mvn test

# Integration tests (requires Docker)
mvn verify

# E2E tests
.\test-e2e-comprehensive.ps1

# Test specific service
cd paymentservice && mvn test

# Sprint-specific tests
.\test-sprint1.ps1  # User + Maintenance
.\test-sprint2.ps1  # Payment
.\test-sprint3.ps1  # Notification
```

### Code Quality

```powershell
# Run static analysis
mvn checkstyle:check

# Generate code coverage report
mvn jacoco:report

# View coverage report
Start-Process "target/site/jacoco/index.html"
```

📖 **Testing Guide**: [TESTING-GUIDE.md](TESTING-GUIDE.md)

---

## 🚢 Deployment

### Local Development

```powershell
docker-compose up -d
.\start-all-services.ps1
```

### Production (AWS ECS)

```bash
# Build Docker images
docker build -t homegenie/gateway-service:1.0.0 ./gateway-service

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
docker push <account>.dkr.ecr.us-east-1.amazonaws.com/homegenie/gateway-service:1.0.0

# Deploy to ECS
aws ecs update-service --cluster homegenie-cluster --service gateway-service --force-new-deployment
```

### Kubernetes

```bash
kubectl apply -f k8s/gateway-service.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/maintenance-service.yaml
kubectl apply -f k8s/payment-service.yaml
kubectl apply -f k8s/notification-service.yaml
```

📖 **Deployment Guide**: [docs/DEPLOYMENT-GUIDE.md](docs/DEPLOYMENT-GUIDE.md)

---

## 📈 Monitoring

### Grafana Dashboards

Access Grafana at <http://localhost:3000> (admin/admin)

**Available Dashboards**:

1. **HomeGenie Overview**: System health, JVM metrics, request rates
2. **API Gateway Metrics**: Latency, error rate, rate limiting statistics
3. **Kafka Consumer Lag**: Consumer group lag, partition metrics

### Key Metrics

- **Request Rate**: `rate(http_server_requests_seconds_count[1m])`
- **Error Rate**: `rate(http_server_requests_seconds_count{status=~"5.."}[1m])`
- **Response Time (p95)**: `histogram_quantile(0.95, http_server_requests_seconds_bucket)`
- **Consumer Lag**: `kafka_consumergroup_lag{group="notification-service"}`

### Alerts

**Critical Alerts**:

- High CPU usage (>80% for 5 minutes)
- High memory usage (>85%)
- Service down (health check failure)
- High Kafka consumer lag (>1000 messages)

📖 **Monitoring Setup**: [docs/DEPLOYMENT-GUIDE.md#8-monitoring-setup](docs/DEPLOYMENT-GUIDE.md#8-monitoring-setup)

---

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Spring Boot Team** for the excellent microservices framework
- **Stripe** for comprehensive payment API documentation
- **Google** for Gemini AI API
- **Confluent** for Kafka ecosystem
- **Grafana Labs** for monitoring tools

---


---

**HomeGenie** - Transforming residential maintenance management with AI-powered microservices 🏠✨
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/b0466935-8bcb-44ee-81af-bcafd721e142" />

2.Login
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/9453e72e-e274-44c2-ad8a-977353fa3ea1" />

3.Dashboard (Resident)
<img width="1898" height="1080" alt="image" src="https://github.com/user-attachments/assets/e4c3699e-0e03-42e5-b350-bd930cc3c09d" />

4.Create New Request(Resident)
<img width="1903" height="1080" alt="image" src="https://github.com/user-attachments/assets/a75ef4fd-89f7-4786-a062-c512d7fab699" />

5.Dashboard (Admin)
<img width="1891" height="1079" alt="image" src="https://github.com/user-attachments/assets/2a387206-836f-4506-981c-b6e3b5f5559b" />

6.Assign Technician
<img width="1895" height="1078" alt="image" src="https://github.com/user-attachments/assets/015232f2-a601-4032-84d9-01f0feada49f" />

7.Voice Assitant
<img width="1902" height="1079" alt="image" src="https://github.com/user-attachments/assets/4b42d4e0-a911-42fd-b512-58eb66bc1d34" />

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/661fddb9-25a9-4325-b647-e06dde59519e" />

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/be46800c-233d-49d4-933d-57a3310ac7f2" />

Email Notifications:
User-> Admin (on request creation)
<img width="1838" height="840" alt="image" src="https://github.com/user-attachments/assets/cfa1f126-2a56-47fd-bac1-7cbc3bc5d903" />

Admin-> Technician (on assigning request)
<img width="1842" height="826" alt="image" src="https://github.com/user-attachments/assets/a660efbc-69ce-4276-8517-b9328c4068a5" />

Admin-> User(on request status update)
<img width="1832" height="800" alt="image" src="https://github.com/user-attachments/assets/d46d7064-9181-4bcf-93dd-9b35be7789c8" />

Monitoring:

Grafana:

<img width="1896" height="1080" alt="image" src="https://github.com/user-attachments/assets/9842caaa-f5bd-4105-b599-3c2965ea975b" />

Prometheus:

<img width="1898" height="1077" alt="image" src="https://github.com/user-attachments/assets/871e9b6a-eee3-481c-bcc3-65e9d1035bb6" />

---

## Overview

HomeGenie centralizes the process of handling maintenance requests in residential complexes. It replaces manual communication methods such as WhatsApp or Excel sheets with a structured, automated workflow.

The platform uses AI to classify issues by category and priority and provides real-time status tracking for residents, administrators, and technicians.

### Key Problems Addressed

- Manual tracking of maintenance issues
- Lack of visibility into request status
- Missed or delayed handling of urgent problems
- No centralized reporting or accountability

### Solution Highlights

- Speech-to-Text: Converts voice commands to text using Google Speech Recognition
- Intent Recognition: Uses Google Gemini / Hugging Face AI to understand user requests
- Centralized management of all maintenance requests
- AI-driven categorization and prioritization
- Email notifications to all stakeholders
- Real-time tracking of progress and completion
- Cloud-based storage for uploaded issue photos
  
**Example Interactions:**

```
User: "My kitchen sink is leaking"
AI: "I've created your maintenance request for a plumbing issue 
     with high priority. Ticket #41 has been submitted."

User: "Emergency! Water flooding my apartment!"
AI: "I've detected an emergency. Creating critical priority ticket 
     immediately. Help is on the way!"

User: "What's the status of my AC repair?"
AI: "Your AC repair request #41 is in progress. Technician 
     will arrive tomorrow between 2-4 PM."
```

---

## Core Features

### AI-Based Classification

Example:

```
User Input: "Urgent! Water leaking from bathroom tap"
AI Output: Category = PLUMBING, Priority = CRITICAL
```

### Automated Email Notifications

Email notifications are automatically sent when:

- A resident creates a new request
- The admin assigns a technician
- A technician updates the request status
- A request is completed

### Admin Dashboard

- Displays total requests and their statuses
- Filters by category, priority, and technician
- Real-time updates and analytics

### Scheduler for Pending Requests

A background scheduler runs periodically to check for requests that have remained in a **Pending** state for more than 24 hours.  
If such requests are found, the system sends reminder emails to the admin.

**Implementation Details:**

- The scheduler runs every 2 minutes in test mode (configurable via cron expression)
- It fetches all pending maintenance requests from the database
- For each request older than 24 hours, it retrieves user details from the User Service
- Sends a reminder email to the admin with request details

---

## Architecture

```
React Frontend (Port 3000)
  - User registration and login
  - Request creation and tracking
  - Admin dashboard
  - Technician assignment

User Service (Port 8081)
  - Authentication (JWT)
  - User management
  - Role-based access control
  - Technician directory

Maintenance Service (Port 8082)
  - Request management (CRUD)
  - AI classification and priority assignment
  - Email notifications and scheduling
  - Statistics and analytics

PostgreSQL Databases
  - homegenie_users
  - homegenie_maintenance

AWS Integration
  - S3 for image storage
  - SES for email notifications
  - EC2 for deployment
```

---

## Tech Stack

**Backend**

- Spring Boot 3.2.0 (Java 17)
- Hibernate / JPA
- PostgreSQL
- JWT Authentication

**Frontend**

- React 18
- Tailwind CSS
- Lucide React for icons

**Cloud & AI**

- Hugging Face API for classification
- AWS S3 for image storage
- AWS SES for email delivery
- AWS EC2 for hosting

---

## Setup Instructions

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 14+
- AWS account (Free Tier)

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/homegenie.git
   cd homegenie
   ```

2. Create PostgreSQL databases:

   ```bash
   sudo -u postgres psql
   CREATE DATABASE homegenie_users;
   CREATE DATABASE homegenie_maintenance;
   \q
   ```

3. Configure AWS S3 and SES.

4. Start backend services:

   ```bash
   # User Service
   cd user-service
   mvn spring-boot:run

   # Maintenance Service
   cd maintenance-service
   mvn spring-boot:run
   ```

5. Start frontend:

   ```bash
   cd homegenie-app
   npm install
   npm start
   ```

   # Future Enhancements

- Dedicated **Technician Dashboard** for task acceptance, status updates, and ETA tracking  
- **Event-driven notification system** to decouple alerts from core business logic  
- **Multi-channel notifications** (Email, SMS, Push) for critical and emergency requests  
- **Resilience mechanisms** (circuit breakers, retries, fallbacks) for external services  
- **Audit logs & request history** for traceability and accountability  
- **Advanced analytics & predictive insights** using historical maintenance data  
