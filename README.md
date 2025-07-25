# 🛡️ Spring Boot Auth & Role-Based Access System + Document Management System

A secure, production-ready Spring Boot 3 application with complete user authentication and role-based authorization. Built for `ADMIN`, `TUTOR`, `STAFF`, and `STUDENT` roles with:

- ✅ Google OAuth2 Login
- ✅ JWT session management
- ✅ Redis-based refresh tokens
- ✅ OTP password recovery
- ✅ PostgreSQL integration

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

## ⚙️ Tech Stack

- Spring Boot 3
- Spring Security 6
- Spring Data JPA
- Redis & PostgreSQL
- JWT Authentication
- OAuth2 (Google)
- Swagger / Actuator
- Prometheus / Grafana
- Maven

---

## 📁 Project Structure

## Project Structure

```bash
src/
├── config/ # Security, OAuth2, Redis, and CORS config
├── controller/ # REST API controllers
├── dto/ # Request/response models
├── entity/ # JPA entities
├── repository/ # JPA Repositories
├── security/ # Filters, token providers, handlers
├── service/ # Core business services
└── utils/ # Email, OTP, file upload, token utils

```

---

<details>
<summary><strong>🔐 Authentication APIs</strong></summary>

| Method | Endpoint                         | Description                             |
|--------|----------------------------------|-----------------------------------------|
| POST   | `/api/auth/signup`              | Register new user                       |
| POST   | `/api/auth/signin`              | Login with credentials                  |
| POST   | `/api/auth/signout`             | Logout from current device              |
| POST   | `/api/auth/signout-all-devices` | Logout from all sessions                |
| POST   | `/api/auth/send-otp`            | Send email OTP                          |
| POST   | `/api/auth/reset-password`      | Reset password via OTP                  |
| POST   | `/api/auth/refresh-token`       | Get new access token using refresh      |
| POST   | `/api/auth/cleanup-tokens`      | Clear unused/expired tokens             |
| GET    | `/oauth2/authorization/google`  | OAuth2 login initiation                 |
| GET    | `/login/oauth2/code/google`     | Google login callback                   |
| GET    | `/oauth2/redirect`              | Frontend redirect post-auth             |

</details>

<details>
<summary><strong>📄 Document Management APIs</strong></summary>

| Method | Endpoint                        | Description            |
|--------|----------------------------------|------------------------|
| POST   | `/api/document/upload`          | Upload a single file   |
| POST   | `/api/document/upload/multiple` | Upload multiple files  |
| GET    | `/api/document/{id}`            | Retrieve uploaded file |

</details>

<details>
<summary><strong>👤 User Management APIs</strong></summary>

| Method | Endpoint                         | Description                    |
|--------|----------------------------------|--------------------------------|
| GET    | `/api/users/me`                 | Get current user profile       |
| PUT    | `/api/users/updatePersonalInfo` | Update personal information    |
| GET    | `/api/users/**`                 | Admin-only user management     |

</details>

<details>
<summary><strong>📊 Health & Monitoring APIs</strong></summary>

| Method | Endpoint                | Description                        |
|--------|-------------------------|------------------------------------|
| GET    | `/actuator/health`     | App health status                  |
| GET    | `/actuator/info`       | App info (name, version, etc.)     |
| GET    | `/actuator/metrics`    | System metrics                     |
| GET    | `/actuator/prometheus` | Prometheus-formatted metrics       |

</details>

---

## 🔧 Core Functionalities

### 🔐 Authentication System

- JWT access tokens (15-min expiry)
- Redis-based refresh tokens (7-day expiry)
- Token blacklisting (for logout)
- Multi-device session management
- Google OAuth2 auto-register flow

### 🛡️ Security Features

- BCrypt password hashing
- Secure cookie (HttpOnly, Secure, SameSite)
- CORS with domain whitelist
- JWT token validation & blacklist checks
- Redis-based rate limiting (optional)

### 🔁 Redis Token Structure

refresh-token:{uuid} → user@email.com

user-tokens:{email} → Set[tokenId1, tokenId2]

blacklist:{tokenId} → "blacklisted"


---

## 📤 File Upload System

- PNG, JPG, JPEG, PDF support
- Max 10MB per file; 100MB per request
- Temporary `/tmp/uploads` folder
- Metadata stored in PostgreSQL

---

## ✉️ Email Integration

- 6-digit OTP with 10-minute TTL
- SMTP config (e.g., Gmail)
- Password reset + Email verification support

---

## Token Lifecycle

1. Login/Register
2. Create JWT (jti = unique token ID)
3. Store refresh token in Redis
4. Send JWT to client + cookie
---

## Token Refresh

1. Extract refresh token from cookie
2. Validate it from Redis
3. Generate new JWT + refresh token
4. Invalidate previous one

---

## Signout

1. Add token to Redis blacklist
2. Delete refresh tokens for user
3. Clear cookie

---

## Signout

1. Add token to Redis blacklist
2. Delete refresh tokens for user
3. Clear cookie