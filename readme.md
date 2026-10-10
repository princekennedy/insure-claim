# InsureClaim Portal

A comprehensive digital platform designed to streamline and enhance the motor insurance claims process for both customers and insurers.

## Overview

The solution enables customers to:
- File claims online
- Complete instant digital KYC verification
- Track claim progress in real time
- Provide feedback on garage services
- Access AI-powered chatbot support (24/7)

Insurer-side features include:
- Centralized dashboard with advanced analytics
- Claim performance monitoring
- Garage quality assessment
- Customer feedback analysis
- Automated fraud detection alerts
- Underperforming service provider alerts

## Tech Stack

### Backend
- **Framework:** Spring Boot 4.1.1 with Kotlin 2.3.21
- **Database:** PostgreSQL 17
- **File Storage:** Local filesystem or MinIO/S3-compatible object storage
- **Build Tool:** Maven 3.9
- **Java Version:** 21
- **Security:** Spring Security with OAuth2 Resource Server (JWT)
- **API Documentation:** SpringDoc OpenAPI (Swagger UI)

### Frontend
- **Framework:** Next.js 16.3.8
- **React:** 19.2.8
- **Language:** TypeScript 5
- **Styling:** Tailwind CSS 4

## Project Structure

```
.
├── backend/
│   ├── src/main/kotlin/com/britam/insureclaim/
│   │   ├── claim/          # Claims management
│   │   ├── common/         # Shared utilities & exceptions
│   │   ├── config/         # Spring configuration
│   │   ├── garage/         # Garage & repair job management
│   │   ├── kyc/            # KYC verification
│   │   ├── policy/         # Policy management
│   │   ├── security/       # Security configuration
│   │   ├── storage/        # File storage services
│   │   ├── user/           # User management
│   │   └── vehicle/        # Vehicle management
│   ├── src/main/resources/
│   │   ├── db/migration/   # Flyway database migrations
│   │   ├── seed/           # Seed data
│   │   └── application.properties
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── frontend/
│   ├── app/
│   │   ├── (auth)/         # Auth pages (login, register)
│   │   ├── (dashboard)/    # Dashboard pages
│   │   ├── api/            # API route handlers
│   │   ├── layout.tsx      # Root layout
│   │   └── page.tsx        # Home redirect
│   ├── lib/
│   │   ├── components/     # React components
│   │   ├── services/       # API service layer
│   │   └── types/          # TypeScript type definitions
│   ├── Dockerfile
│   └── next.config.ts
│
└── readme.md
```

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21+ (for local backend development)
- Node.js 22+ (for local frontend development)
- Maven 3.9+ (for local backend builds)

### Quick Start with Docker

```bash
# Build and start all services (including MinIO)
cd backend
docker compose up -d

# Access the services:
# - Frontend: http://localhost:3000
# - Backend API: http://localhost:8080
# - API Docs: http://localhost:8080/swagger-ui.html
# - Database Admin: http://localhost:8081 (Adminer)
# - Database: localhost:55432 (PostgreSQL)
# - MinIO Storage: http://localhost:9000
# - MinIO Console: http://localhost:9001
```

### Local Development

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

## File Storage Configuration

### Local Filesystem (Default)
Files are stored in `./data/uploads` relative to the backend. No additional configuration needed.

### MinIO Object Storage

The docker-compose includes MinIO services by default:

- **MinIO Server:** Port 9000 (API), Port 9001 (Console)
- **Credentials:** `insureclaim` / `insureclaim123`
- **Bucket:** `insureclaim` (auto-created)

To use MinIO in other environments, set these environment variables:

```bash
MINIO_ENABLED=true
MINIO_ENDPOINT=minio:9000
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key
MINIO_BUCKET=insureclaim
MINIO_URL_BASE=http://localhost:9000
```

## API Documentation

Once the backend is running, access the interactive API documentation at:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8080/v3/api-docs

### Key Endpoints

#### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Get current user
- `POST /api/v1/auth/forgot-password` - Request a password reset link
- `POST /api/v1/auth/reset-password` - Reset password using a token

#### Claims
- `GET /api/v1/claims` - List claims
- `POST /api/v1/claims` - File new claim
- `GET /api/v1/claims/{id}` - Get claim details
- `PUT /api/v1/claims/{id}/status` - Update claim status
- `POST /api/v1/claims/{id}/documents` - Upload document

#### Vehicles
- `GET /api/v1/me/vehicles` - List user vehicles
- `POST /api/v1/me/vehicles` - Add vehicle
- `PUT /api/v1/me/vehicles/{id}` - Update vehicle
- `DELETE /api/v1/me/vehicles/{id}` - Delete vehicle

#### Policies
- `GET /api/v1/me/policies` - List user policies
- `GET /api/v1/me/policies/{id}` - Get policy details

#### KYC
- `POST /api/v1/kyc` - Submit KYC verification
- `GET /api/v1/kyc/verifications/current` - Get current KYC status

#### Garages
- `GET /api/v1/garages` - List garages
- `GET /api/v1/garages/{id}` - Get garage details
- `GET /api/v1/garages/{id}/feedback` - Get garage feedback

#### Feedback
- `POST /api/v1/claims/{claimId}/feedback` - Submit feedback
- `GET /api/v1/me/feedback` - Get user's feedback

## Environment Variables

### Backend

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Server port |
| `BACKEND_BASE_URL` | http://localhost:8080 | Public backend URL |
| `DB_URL` | jdbc:postgresql://localhost:55432/insureclaim | Database URL |
| `DB_USERNAME` | insureclaim | Database username |
| `DB_PASSWORD` | insureclaim | Database password |
| `JWT_SECRET` | (dev secret) | JWT signing secret |
| `MINIO_ENABLED` | false | Enable MinIO storage |
| `MINIO_ENDPOINT` | localhost:9000 | MinIO endpoint |
| `MINIO_ACCESS_KEY` | minioadmin | MinIO access key |
| `MINIO_SECRET_KEY` | minioadmin | MinIO secret key |
| `MINIO_BUCKET` | insureclaim | Bucket name |
| `MINIO_URL_BASE` | http://localhost:9000 | Public bucket URL |

See [backend/.env.example](backend/.env.example) for all variables.

### Frontend

| Variable | Default | Description |
|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | http://localhost:8080 | Backend API URL |
| `NEXT_PUBLIC_FRONTEND_URL` | http://localhost:3000 | Frontend URL |
| `NEXT_PUBLIC_STORAGE_URL` | (uses API_URL) | Storage URL for files |

See [frontend/.env.example](frontend/.env.example) for all variables.

## License

Proprietary - Britam Insurance PLC
