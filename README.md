# 🔒 Spring Boot Authentication & Authorization System

A robust, production-ready Spring Boot 3 application featuring comprehensive authentication, role-based access control, and document management. Built with security and scalability in mind, this system provides a solid foundation for enterprise applications.

[//]: # ([![License]&#40;https://img.shields.io/badge/License-MIT-blue.svg&#41;]&#40;https://opensource.org/licenses/MIT&#41;)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)

## ✨ Key Features

- 🔐 **Secure Authentication**
  - JWT-based stateless authentication
  - Google OAuth2 integration
  - Redis-based refresh tokens (7-day expiry)
  - Multi-device session management

- 👥 **Role-Based Access Control**
  - Fine-grained permission system
  - Four distinct user roles: ADMIN, TUTOR, STAFF, STUDENT
  - Dynamic role management

- 📁 **Document Management**
  - Secure file upload and storage
  - Role-based access control for documents
  - Support for multiple file types (PDF, images, documents)
  - Metadata management and search

- 🚀 **Developer Experience**
  - Comprehensive API documentation (Swagger UI)
  - Actuator endpoints for monitoring
  - Prometheus metrics integration
  - Containerized deployment ready

---

## ✅ Features

- User Sign Up with email verification
- Sign In with JWT-based authentication
- OTP-based Password Reset via Email
- Google OAuth2 Login and auto-registration
- Role-based Access Control (RBAC)
- Admin Panel for user management
- Secure APIs using Spring Security 6
- Redis for refresh token and session management
- PostgreSQL with Spring Data JPA
- Prometheus + Actuator Metrics
- Swagger UI for API Testing

---

## 👥 Roles & Access

| Role     | Description                 |
|----------|-----------------------------|
| `ADMIN`  | Full access to all features |
| `TUTOR`  | Tutor-related access        |
| `STAFF`  | Internal staff features     |
| `STUDENT`| Student-specific access     |

---

## 🛠️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.5
- **Security**: Spring Security 6 + JWT + OAuth2 (Google)
- **Database**: PostgreSQL 15 + Redis 7
- **Build Tool**: Maven
- **Java Version**: 17

### Monitoring & Documentation
- **API Docs**: Swagger UI
- **Metrics**: Prometheus + Grafana
- **Health**: Spring Boot Actuator

### Key Dependencies
- Spring Data JPA
- Spring Web
- Spring Mail
- Spring OAuth2 Client
- JJWT (Java JWT)
- Lombok
- MapStruct (for DTO mapping)

---

## 📁 Project Structure

```
src/
├── config/       # Security, OAuth2, Redis, and CORS configuration
├── controller/   # REST API endpoints
├── dto/          # Request/response models
├── entity/       # JPA entities (database models)
├── repository/   # Database repositories
├── security/     # Security configurations and filters
├── service/      # Business logic
└── utils/        # Helper classes and utilities
```

---

## 📚 API Documentation

### 🔐 Authentication & Authorization

#### User Registration & Login

| Method | Endpoint                | Description                                  | Authentication |
|--------|-------------------------|----------------------------------------------|----------------|
| POST   | `/api/auth/signup`      | Register a new user                          | Public         |
| POST   | `/api/auth/signin`      | Authenticate user and get JWT token         | Public         |
| POST   | `/api/auth/signout`     | Invalidate current session                  | Authenticated  |
| POST   | `/api/auth/refresh`     | Get new access token using refresh token    | Public         |

#### OAuth2 Authentication

| Method | Endpoint                        | Description                         |
|--------|---------------------------------|-------------------------------------|
| GET    | `/oauth2/authorization/google`  | Initiate Google OAuth2 login flow  |
| GET    | `/login/oauth2/code/google`     | Google OAuth2 callback endpoint    |

#### Password Management

| Method | Endpoint                     | Description                          |
|--------|------------------------------|--------------------------------------|
| POST   | `/api/auth/send-otp`         | Send OTP to email for password reset |
| POST   | `/api/auth/verify-otp`       | Verify OTP code                      |
| POST   | `/api/auth/reset-password`   | Reset password using verified OTP    |

### 👤 User Management

#### User Profile

| Method | Endpoint                | Description                          | Required Role |
|--------|-------------------------|--------------------------------------|---------------|
| GET    | `/api/users/me`         | Get current user profile             | Any role      |
| PUT    | `/api/users/profile`    | Update user profile                  | Any role      |
| DELETE | `/api/users/me`        | Delete current user account          | Any role      |

#### Admin Endpoints

| Method | Endpoint                | Description                          | Required Role |
|--------|-------------------------|--------------------------------------|---------------|
| GET    | `/api/users`           | Get all users (paginated)            | ADMIN         |
| GET    | `/api/users/{id}`      | Get user by ID                       | ADMIN         |
| PUT    | `/api/users/{id}/role` | Update user role                     | ADMIN         |
| DELETE | `/api/users/{id}`      | Delete user (admin only)             | ADMIN         |

### 📄 Document Management

#### File Operations

| Method | Endpoint                          | Description                              | Required Role            |
|--------|-----------------------------------|------------------------------------------|--------------------------|
| POST   | `/api/documents`                 | Upload a single file                     | Any authenticated user   |
| POST   | `/api/documents/batch`           | Upload multiple files                    | Any authenticated user   |
| GET    | `/api/documents/{id}`            | Download a file                          | File owner or ADMIN      |
| GET    | `/api/documents/{id}/metadata`   | Get file metadata                        | File owner or ADMIN      |
| GET    | `/api/documents`                 | List all accessible files (paginated)    | Any authenticated user   |
| DELETE | `/api/documents/{id}`            | Delete a file                            | File owner or ADMIN      |

#### Document Types & Limits
- **Supported Formats**: PDF, JPG, JPEG, PNG
- **Max File Size**: 10MB per file
- **Max Request Size**: 100MB (for batch uploads)
- **Storage**: Files are stored temporarily in `/tmp/uploads`
- **Metadata**: All file metadata is stored in PostgreSQL

<details>
<summary><strong>👤 User Management APIs</strong></summary>

| Method | Endpoint                         | Description                    |
|--------|----------------------------------|--------------------------------|
| GET    | `/api/users/me`                 | Get current user profile       |
| PUT    | `/api/users/updatePersonalInfo` | Update personal information    |
| GET    | `/api/users/**`                 | Admin-only user management     |

</details>

### 📊 System Monitoring & Health

#### Actuator Endpoints

| Endpoint                  | Description                              | Authentication |
|---------------------------|------------------------------------------|----------------|
| `/actuator/health`        | Application health status                | Public         |
| `/actuator/info`          | Application information                  | Public         |
| `/actuator/metrics`       | Application metrics                      | ADMIN          |
| `/actuator/prometheus`    | Prometheus metrics endpoint              | ADMIN          |
| `/actuator/redis`         | Redis health and metrics                 | ADMIN          |
| `/actuator/database`      | Database connection status               | ADMIN          |

#### Metrics Collection
- **Prometheus**: Scrapes metrics from `/actuator/prometheus`
- **Grafana**: Pre-configured dashboards for monitoring
- **Alerts**: Configured for critical system metrics

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- SMTP server (for email functionality)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/springboot-authentication.git
   cd backend
   ```

2. **Configure Environment Variables**
   Create a `.env` file in the root directory with the following variables:
   ```env
   # Database
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=yourpassword
   
   # JWT
   JWT_SECRET=your-jwt-secret-key
   JWT_EXPIRATION_MS=86400000
   
   # Redis
   SPRING_REDIS_HOST=localhost
   SPRING_REDIS_PORT=6379
   
   # Email
   SPRING_MAIL_HOST=smtp.example.com
   SPRING_MAIL_USERNAME=your-email@example.com
   SPRING_MAIL_PASSWORD=your-email-password
   ```

3. **Build the Application**
   ```bash
   mvn clean install
   ```

4. **Run the Application**
   ```bash
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

5. **Access the Application**
   - API Documentation: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

---

## 🔒 Security Implementation

### Authentication Flow

1. **User Login**
   - Client sends credentials to `/api/auth/signin`
   - Server validates and issues JWT + refresh token
   - Refresh token stored in Redis with user email mapping

2. **Token Management**
   - **Access Token**: JWT with 15-minute expiry
   - **Refresh Token**: Stored in Redis (7-day expiry)
   - **Automatic Refresh**: Client uses refresh token to get new access token

3. **Security Headers**
   - HTTP Strict Transport Security (HSTS)
   - X-Content-Type-Options: nosniff
   - X-Frame-Options: DENY
   - X-XSS-Protection: 1; mode=block
   - Content Security Policy (CSP)

### 🔐 Security Best Practices

- **Password Security**
  - BCrypt hashing with strength 12
  - Minimum 8 characters with complexity
  - Account lockout after 5 failed attempts

- **Session Security**
  - JWT in secure, HTTP-only cookies
  - CSRF protection for state changes
  - Secure flag on all cookies
  - SameSite=Lax cookie policy

- **Rate Limiting**
  - 100 requests/minute per IP (public)
  - 1000 requests/minute per user (authenticated)
  - 10 requests/minute for sensitive operations


---

## 📂 File Management System

### Storage Architecture

```
storage/
├── uploads/          # Temporary file storage
│   └── user_{id}/    # User-specific uploads
├── temp/             # Temporary processing
└── backups/          # Automated backups
```

### Key Features

- **Supported Formats**: PNG, JPG, JPEG, PDF
- **File Size Limits**:
  - Max 10MB per file
  - Max 100MB per request
  - Max 1000 files per batch
- **Storage Locations**:
  - Temporary: `/tmp/uploads` (automatically cleaned)
  - Persistent: Configured storage directory
  - Metadata: PostgreSQL database

### File Operations

1. **Upload Process**
   - File validation (type, size, virus scan)
   - Generate unique filename
   - Store file in temporary location
   - Save metadata to database
   - Trigger async processing if needed

2. **Access Control**
   - Role-based access to files
   - File ownership validation
   - Signed URLs for secure file access
   - Automatic cleanup of orphaned files

---

## 🚀 Deployment

### Docker Setup

1. **Docker Compose**
   ```yaml
   version: '3.8'
   
   services:
     app:
       build: .
       ports:
         - "8080:8080"
       env_file: .env
       depends_on:
         - postgres
         - redis
     
     postgres:
       image: postgres:15
       environment:
         POSTGRES_PASSWORD: ${DB_PASSWORD}
         POSTGRES_USER: ${DB_USERNAME}
         POSTGRES_DB: ${DB_NAME}
       volumes:
         - postgres_data:/var/lib/postgresql/data
     
     redis:
       image: redis:7
       command: redis-server --requirepass ${REDIS_PASSWORD}
       volumes:
         - redis_data:/data
   
   volumes:
     postgres_data:
     redis_data:
   ```

2. **Environment Variables**
   ```env
   # Application
   SPRING_PROFILES_ACTIVE=prod
   
   # JWT
   JWT_SECRET=your-secure-jwt-secret
   JWT_EXPIRATION_MS=86400000
   
   # Database
   SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${DB_NAME}
   SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
   SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
   
   # Redis
   SPRING_REDIS_HOST=redis
   SPRING_REDIS_PASSWORD=${REDIS_PASSWORD}
   ```

### Production Best Practices

1. **Security**
   - Use HTTPS with valid certificate
   - Enable CORS only for trusted domains
   - Regular security audits
   - Dependency vulnerability scanning

2. **Performance**
   - Enable HTTP/2
   - Configure connection pooling
   - Implement caching strategy
   - Database indexing

3. **Monitoring**
   - Centralized logging (ELK Stack)
   - Application Performance Monitoring (APM)
   - Alerting on critical metrics
   - Regular backups

---

## 📚 Additional Resources

- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JWT Best Practices](https://auth0.com/docs/secure/tokens/json-web-tokens)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👏 Acknowledgments

- Built with ❤️ using Spring Boot
- Inspired by industry best practices
- Community contributions are welcome!