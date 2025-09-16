# 📄 CloudDocs - Enterprise Document Management System

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18+-blue)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13+-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ed)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

*Revolutionizing enterprise document management with enterprise-grade security, AI-powered search, and intelligent workflow automation*

[Live Demo](https://cloud-docs-tan.vercel.app/) | [API Documentation](#-api-documentation) | [Getting Started](#-quick-start)

</div>

## 🚀 Overview

CloudDocs is a cutting-edge, enterprise-grade Document Management System that revolutionizes how organizations handle document workflows. Built with modern TypeScript and Java Spring Boot architecture, it provides comprehensive document storage, collaboration, and automated approval workflows with military-grade security and AI-powered intelligent search capabilities.

## ✨ Key Highlights

🔐 **Enterprise Security**: JWT-based authentication with role-based access control  
🤖 **AI-Powered Search**: Intelligent document discovery with natural language queries  
⚡ **Workflow Automation**: Multi-step approval processes with real-time task management  
📧 **Smart Notifications**: SendGrid-powered email alerts for workflow events  
📊 **Complete Audit Trail**: Full compliance tracking for enterprise requirements  
🎯 **Real-time Collaboration**: Live updates and notifications across teams  
🌐 **Modern Architecture**: Scalable microservices with RESTful APIs  

## 🎯 Features

### 📋 Document Management
- **Secure Upload & Storage**: Multiple format support with virus scanning
- **Version Control**: Complete document history with rollback capabilities
- **Smart Organization**: Tags, categories, and advanced search functionality
- **Access Control**: Granular permissions per document and folder
- **AI-Enhanced Search**: Natural language queries with intelligent content discovery

### 🤖 AI-Powered Intelligence
- **Intelligent Search**: Natural language document queries with context understanding
- **Content Analysis**: AI-powered document classification and tagging
- **Smart Suggestions**: Contextual recommendations for related documents
- **Enhanced Discovery**: Find documents based on content meaning, not just keywords
- **Multi-Provider Support**: OpenAI and Cohere API integration with fallback capabilities

### ⚙️ Workflow Automation
- **Custom Workflows**: Drag-and-drop workflow designer
- **Multi-step Approvals**: Sequential and parallel approval processes
- **Task Management**: Assignment tracking with SLA monitoring
- **Escalation Rules**: Automated escalation for overdue tasks
- **Email Notifications**: SendGrid-powered professional email alerts

### 👥 User Management
- **Role-Based Access**: Admin, Manager, and User roles with custom permissions
- **Department Integration**: Hierarchical organization structure
- **Single Sign-On**: LDAP/Active Directory integration ready
- **User Dashboard**: Personalized task and document views

### 📊 Analytics & Reporting
- **Real-time Metrics**: Workflow performance and bottleneck analysis
- **Compliance Reports**: Audit trail exports for regulatory requirements
- **Usage Analytics**: Document access patterns and user activity
- **Custom Dashboards**: Executive-level insights and KPIs

### 📧 Professional Email System
- **SendGrid Integration**: Enterprise-grade email delivery service
- **Workflow Notifications**: Automated task assignment and completion alerts
- **Customizable Templates**: Professional email formatting with branding
- **Reliable Delivery**: High deliverability rates with detailed tracking

## 🛠️ Technology Stack

### Backend Architecture
Java 17 + Spring Boot 3.x
├── Spring Security (JWT Authentication)
├── Spring Data JPA (Database Layer)
├── Spring Web (REST APIs)
├── Hibernate (ORM)
├── SendGrid API (Email Services)
├── OpenAI API (AI Search & Analysis)
├── Cohere API (Backup AI Provider)
├── Maven (Build Tool)
└── PostgreSQL (Database)

text

### Frontend Architecture
React 18 + TypeScript 5.x
├── Redux Toolkit (State Management)
├── React Router (Navigation)
├── Axios (HTTP Client)
├── Material-UI (Component Library)
├── AI Search Interface
├── Webpack (Build Tool)
└── ESLint + Prettier (Code Quality)

text

### AI & Analytics Stack
Intelligent Services
├── OpenAI GPT (Primary AI Provider)
├── Cohere API (Secondary Provider)
├── Natural Language Processing
├── Document Classification
├── Smart Search Algorithms
└── Analytics Engine

text

### Email & Notifications
Communication Stack
├── SendGrid API (Email Delivery)
├── Professional Email Templates
├── Workflow Notification System
├── Task Assignment Alerts
└── Delivery Tracking

text

### DevOps & Infrastructure
Production Ready Setup
├── Docker + Docker Compose
├── Nginx (Reverse Proxy)
├── SSL/TLS Encryption
├── Environment Configuration
├── Health Check Endpoints
└── Railway Cloud Deployment

text

## 🚀 Quick Start

### Prerequisites
Ensure you have the following installed:

- **Java 17+** ([Download](https://adoptium.net/))
- **Node.js 18+** ([Download](https://nodejs.org/))
- **PostgreSQL 13+** ([Download](https://www.postgresql.org/downloads/))
- **Git** ([Download](https://git-scm.com/downloads))

### 📦 Installation

1. **Clone the Repository**
git clone https://github.com/diwansh12/CloudDocs.git
cd clouddocs

text

2. **Database Setup**
-- Create database and user
CREATE DATABASE clouddocs_db;
CREATE USER clouddocs_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE clouddocs_db TO clouddocs_user;

text

3. **Backend Configuration**
Navigate to backend directory
cd backend

Copy and configure application properties
cp src/main/resources/application.properties.example src/main/resources/application.properties

Edit database configuration
nano src/main/resources/application.properties

text

4. **Environment Variables**
Database Configuration
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=clouddocs_db
export DB_USERNAME=clouddocs_user
export DB_PASSWORD=your_password

Security
export JWT_SECRET=your_jwt_secret_key

SendGrid Email Service
export SENDGRID_API_KEY=your_sendgrid_api_key
export APP_EMAIL_FROM=noreply@yourdomain.com

AI Services
export OPENAI_API_KEY=your_openai_api_key
export COHERE_API_KEY=your_cohere_api_key

Application URLs
export APP_BASE_URL=https://yourdomain.com
export CORS_ORIGINS=https://yourdomain.com,http://localhost:3000

text

5. **Start Backend Server**
Run Spring Boot application
./mvnw clean install
./mvnw spring-boot:run

Server will start on http://localhost:8080
text

6. **Start Frontend Application**
Navigate to frontend directory
cd ../frontend

Install dependencies
npm install

Start development server
npm start

Application will open on http://localhost:3000
text

### 🐳 Docker Setup (Recommended for Production)
Build and run with Docker Compose
docker-compose up --build -d

Access the application
Frontend: http://localhost:3000
Backend API: http://localhost:8080
Database: localhost:5432
text

## 📚 API Documentation

### 🔗 Interactive API Docs
Once the backend is running, access the Swagger UI documentation:

- **Local**: http://localhost:8080/swagger-ui/index.html
- **API Docs**: http://localhost:8080/v3/api-docs

### 🛡️ Authentication
All API endpoints require JWT authentication (except registration/login):

// Include JWT token in headers
Authorization: Bearer <your_jwt_token>

text

### 📋 Core Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User authentication |
| GET | `/api/workflows` | Get user workflows |
| POST | `/api/workflows` | Create new workflow |
| PUT | `/api/workflows/{id}/tasks/{taskId}` | Approve/reject task |
| GET | `/api/documents` | List user documents |
| POST | `/api/documents/upload` | Upload document |
| GET | `/api/ai/search` | AI-powered document search |
| POST | `/api/ai/analyze` | Document content analysis |

### 🤖 AI Search Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ai/search?query=natural language query` | Natural language document search |
| POST | `/api/ai/analyze/document/{id}` | AI-powered document analysis |
| GET | `/api/ai/suggestions/{documentId}` | Get related document suggestions |

## 🏗️ Project Structure

clouddocs/
├── 📁 backend/ # Spring Boot API
│ ├── 📁 src/main/java/
│ │ └── 📁 com/clouddocs/
│ │ ├── 📁 controller/ # REST Controllers
│ │ ├── 📁 service/ # Business Logic
│ │ │ ├── 📄 AISearchService.java
│ │ │ ├── 📄 NotificationService.java
│ │ │ └── 📄 WorkflowService.java
│ │ ├── 📁 repository/ # Data Access Layer
│ │ ├── 📁 entity/ # JPA Entities
│ │ ├── 📁 dto/ # Data Transfer Objects
│ │ ├── 📁 security/ # Authentication & Authorization
│ │ └── 📁 config/ # Configuration Classes
│ ├── 📁 src/main/resources/
│ │ ├── 📄 application.properties
│ │ └── 📁 db/migration/ # Database Scripts
│ └── 📄 pom.xml # Maven Dependencies
│
├── 📁 frontend/ # React TypeScript App
│ ├── 📁 src/
│ │ ├── 📁 components/ # Reusable Components
│ │ │ ├── 📁 search/ # AI Search Components
│ │ │ └── 📁 workflow/ # Workflow Components
│ │ ├── 📁 pages/ # Page Components
│ │ ├── 📁 services/ # API Services
│ │ │ ├── 📄 aiService.ts # AI API Integration
│ │ │ └── 📄 emailService.ts # Email API Integration
│ │ ├── 📁 store/ # Redux Store
│ │ ├── 📁 types/ # TypeScript Definitions
│ │ └── 📁 utils/ # Utility Functions
│ ├── 📄 package.json
│ └── 📄 tsconfig.json # TypeScript Configuration
│
├── 📁 docs/ # Documentation
├── 📄 docker-compose.yml # Docker Configuration
├── 📄 .gitignore
└── 📄 README.md

text

## 🧪 Testing

### Backend Tests
Run unit tests
./mvnw test

Run integration tests
./mvnw verify

Generate test coverage report
./mvnw jacoco:report

text

### Frontend Tests
Run unit tests
npm test

Run tests with coverage
npm run test:coverage

Run E2E tests
npm run test:e2e

text

## 🚢 Deployment

### Production Build

**Backend Production Build**
./mvnw clean package -Pprod
java -jar target/clouddocs-backend-1.0.0.jar

text

**Frontend Production Build**
npm run build

Static files generated in build/ directory
text

### Environment Configuration

Create production environment files:
- `application-prod.properties` (Backend)
- `.env.production` (Frontend)

### Docker Production Deployment
Build production images
docker-compose -f docker-compose.prod.yml build

Deploy to production
docker-compose -f docker-compose.prod.yml up -d

text

## 📋 Current Features & Roadmap

### ✅ Currently Available
- Complete document management with version control
- Role-based authentication and authorization
- Advanced workflow automation system
- AI-powered intelligent search with OpenAI integration
- SendGrid professional email notifications
- Real-time task management and assignments
- Comprehensive audit trails and analytics
- Modern React TypeScript frontend
- Robust Spring Boot backend

### 🔄 Temporarily Disabled
- **OCR (Optical Character Recognition)**: Currently disabled for performance optimization
  - Will be re-enabled in future updates with enhanced processing capabilities
  - Document text extraction temporarily unavailable

### 🚧 Coming Soon
- Enhanced AI document analysis and classification
- OCR re-enablement with improved performance
- Advanced analytics dashboards
- Mobile application
- Enhanced collaboration features

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the Repository**
git clone https://github.com/diwansh12/CloudDocs.git

text

2. **Create Feature Branch**
git checkout -b feature/your-feature-name

text

3. **Commit Changes**
git commit -m "feat: add your feature description"

text

4. **Push and Create PR**
git push origin feature/your-feature-name

Then create a Pull Request on GitHub
text

### 📋 Development Guidelines
- Follow conventional commit messages
- Write unit tests for new features
- Update documentation for API changes
- Ensure code coverage stays above 80%

## 🔧 Configuration Notes

### SendGrid Email Configuration
SendGrid API configuration
sendgrid.api.key=${SENDGRID_API_KEY}
app.email.from=${APP_EMAIL_FROM}
app.notifications.email.enabled=true

text

### AI Search Configuration
AI provider configuration
ai.providers.openai.enabled=true
ai.providers.openai.api-key=${OPENAI_API_KEY}
ai.providers.cohere.enabled=true
ai.providers.cohere.api-key=${COHERE_API_KEY}
ai.fallback.enabled=true

text

### Performance Optimization
OCR temporarily disabled for performance
ocr.enabled=false

Memory optimization settings
spring.main.lazy-initialization=true
server.tomcat.threads.max=2
spring.cache.type=none

text

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support & Contact

### 🎯 Project Maintainer
- **Name**: Diwansh Sood
- **Email**: [diwansh1112@gmail.com](mailto:diwansh1112@gmail.com)
- **LinkedIn**: [Diwansh Sood](https://www.linkedin.com/in/diwansh-sood-201ab0239/)
- **GitHub**: [@diwansh12](https://github.com/diwansh12)

### 🐛 Issues & Bug Reports
- **GitHub Issues**: [Report Bug](https://github.com/diwansh12/CloudDocs/issues)
- **Feature Requests**: [Request Feature](https://github.com/diwansh12/CloudDocs/issues/new?assignees=&labels=enhancement&template=feature_request.md)

---

<div align="center">

🌟 **Star this repository if you find it helpful!**

[![GitHub stars](https://img.shields.io/github/stars/diwansh12/CloudDocs?style=social)](https://github.com/diwansh12/CloudDocs/stargazers)

</div>

## 🎉 Acknowledgments

- **Spring Boot Team** for the amazing framework
- **React Community** for continuous innovation
- **OpenAI & Cohere** for AI capabilities
- **SendGrid** for reliable email delivery
- **Open Source Contributors** who make projects like this possible

---

**CloudDocs - Transforming document management for the digital enterprise with AI-powered intelligence and seamless workflow automation** 🚀