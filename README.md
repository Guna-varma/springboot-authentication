# Spring Boot Auth & Role-Based Access System

A secure, production-ready Spring Boot 3 application with complete user authentication and role-based authorization. Built for ADMIN, TUTOR, STAFF, and STUDENT users with Google OAuth2 login, JWT session management, OTP-based password recovery, and PostgreSQL integration.

---

## Features

- ✅ User Sign Up with email verification
- ✅ Sign In with JWT-based authentication
- ✅ Forgot Password via Email OTP
- ✅ Google OAuth2 Login
- ✅ Role-based Access Control (RBAC)
- ✅ Admin Panel for user management
- ✅ Secure API using Spring Security 6
- ✅ PostgreSQL with Spring Data JPA
- ✅ Swagger UI for testing APIs

---

## Roles & Access

| Role     | Description                 |
|----------|-----------------------------|
| `ADMIN`  | Full access to all features |
| `TUTOR`  | Limited to tutor resources  |
| `STAFF`  | Internal staff operations   |
| `STUDENT`| Student-specific features   |

---

## Tech Stack

- Spring Boot 3
- Spring Security 6
- Spring Data JPA
- JWT (Token-based Auth)
- PostgreSQL
- OAuth2 (Google)
- Swagger UI
- Java Mail Sender
- Maven

---

## Project Structure

```bash
src/
├── config/             # Security and CORS config
├── controller/         # API controllers
├── dto/                # Request and response DTOs
├── entity/             # JPA entity models
├── repository/         # Spring Data JPA repositories
├── service/            # Business logic services
├── utils/              # Utility classes (Email, JWT, OTP)
└── security/           # JWT filters, entry points, etc.
