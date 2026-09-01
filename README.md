# Product Management RESTful API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6-blue.svg)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-85%20Passed-success.svg)]()

Production-ready, resource-oriented RESTful API solution for managing Products and Inventory Items, developed with **Java 21**, **Spring Boot 3.3.5**, **Spring Data JPA (Hibernate)**, **Spring Security 6 with JWT & Refresh Token Rotation**, and **PostgreSQL**.

---

## Table of Contents
1. [Architecture & Design](#architecture--design)
2. [Technology Stack](#technology-stack)
3. [Database Schema & Indexing](#database-schema--indexing)
4. [API Endpoints & RBAC Matrix](#api-endpoints--rbac-matrix)
5. [Security & Authentication](#security--authentication)
6. [Pagination & Filtering](#pagination--filtering)
7. [Error Handling & Validation](#error-handling--validation)
8. [Testing Layer (85/85 Passing)](#testing-layer-8585-passing)
9. [Running with Docker & Docker Compose](#running-with-docker--docker-compose)
10. [Running Locally](#running-locally)
11. [Testing with Swagger / OpenAPI](#testing-with-swagger--openapi)

---

## Architecture & Design

The application follows a clean, decoupled **Layered Architecture**:

```
com.productmanagement
├── config               # SecurityConfig, OpenApiConfig, CorsConfig, DataInitializer
├── controller           # REST Controllers with /api/v1/ versioning & RBAC
├── dto                  # Request & Response records/POJOs with Jakarta Validation
├── entity               # JPA Entities mapped to PostgreSQL (Product, Item, User, Role, RefreshToken)
├── exception            # GlobalExceptionHandler & Custom Domain Exceptions
├── repository           # Spring Data JPA Repositories
├── security             # JWT Token Provider, Auth Filters, UserDetailsService
└── service              # Business logic interfaces and implementations
```

---

## Technology Stack

| Component | Technology | Version / Details |
|---|---|---|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.3.5 |
| **Persistence** | Spring Data JPA / Hibernate | 6.5.3 |
| **Database** | PostgreSQL | 16 / 18 (Local & Dockerized) |
| **Test Database** | H2 In-Memory | Mode=PostgreSQL (Isolated) |
| **Security** | Spring Security 6 | JWT (JJWT 0.12.6) + Refresh Tokens |
| **Documentation** | SpringDoc OpenAPI | 2.6.0 (Swagger UI 5) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, AssertJ | 85 automated tests |
| **Containerization** | Docker, Docker Compose | Multi-stage build (Alpine JRE 21) |
| **Build Tool** | Apache Maven | 3.9+ |

---

## Database Schema & Indexing

The relational model implements the exact DDL schema specification:

```sql
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    modified_by VARCHAR(100),
    modified_on TIMESTAMP
);

CREATE TABLE item (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL
);
```

### Indexing Strategy
- **`idx_product_name`**: B-tree index on `product(product_name)` for fast search and duplicate check queries.
- **`idx_item_product_id`**: Index on `item(product_id)` foreign key for rapid joins and nested lookups.
- **Unique Indexes**: Automatic B-tree indexing on `users(username)`, `users(email)`, and `refresh_tokens(token)`.

---

## API Endpoints & RBAC Matrix

All endpoints are versioned under `/api/v1/`.

| Method | Endpoint | Description | Permitted Roles | Status Code |
|---|---|---|:---:|:---:|
| **POST** | `/api/v1/auth/register` | Register a new user | Public | `201 Created` |
| **POST** | `/api/v1/auth/login` | Login & receive JWT + Refresh Token | Public | `200 OK` |
| **POST** | `/api/v1/auth/refresh` | Rotate and issue new tokens | Public | `200 OK` |
| **GET** | `/api/v1/products` | Get all products (Paginated) | `ADMIN`, `USER` | `200 OK` |
| **GET** | `/api/v1/products/{id}` | Get product by ID | `ADMIN`, `USER` | `200 OK` |
| **POST** | `/api/v1/products` | Create a new product | `ADMIN` only | `201 Created` |
| **PUT** | `/api/v1/products/{id}` | Update an existing product | `ADMIN` only | `200 OK` |
| **DELETE** | `/api/v1/products/{id}` | Delete product and its items | `ADMIN` only | `204 No Content` |
| **GET** | `/api/v1/products/{id}/items` | Get items of a product (Paginated) | `ADMIN`, `USER` | `200 OK` |
| **GET** | `/api/v1/items` | Get all inventory items (Paginated) | `ADMIN`, `USER` | `200 OK` |
| **GET** | `/api/v1/items/{id}` | Get inventory item by ID | `ADMIN`, `USER` | `200 OK` |
| **POST** | `/api/v1/items` | Add a new inventory item | `ADMIN` only | `201 Created` |
| **PUT** | `/api/v1/items/{id}` | Update inventory item | `ADMIN` only | `200 OK` |
| **DELETE** | `/api/v1/items/{id}` | Delete inventory item | `ADMIN` only | `204 No Content` |

---

## Security & Authentication

1. **Stateless JWT Authentication**:
   - Short-lived Access Token (default: 15 minutes), signed with HMAC-SHA256.
   - Sent via standard HTTP header: `Authorization: Bearer <access_token>`.
2. **Refresh Token Rotation**:
   - Long-lived cryptographically secure UUID token (default: 7 days) stored in the database.
   - Calling `POST /api/v1/auth/refresh` revokes/deletes the old refresh token and returns a fresh pair of tokens.
3. **Role-Based Access Control (RBAC)**:
   - Method-level security enabled (`@EnableMethodSecurity`).
   - Write/Mutate operations restricted to `ROLE_ADMIN`.
   - Read operations accessible to `ROLE_ADMIN` and `ROLE_USER`.
4. **CORS & Secure Headers**:
   - Dedicated `CorsConfigurationSource` with configurable allowed origins, headers, and standard REST methods.
   - HTTP Strict Transport Security (HSTS) with `max-age=31536000` and `X-Frame-Options: DENY`.

---

## Pagination & Filtering

Collection endpoints accept standard Spring Data `Pageable` parameters:
- `page`: 0-indexed page number (default: `0`).
- `size`: Number of records per page (default: `10`).
- `sort`: Sorting field and direction (e.g., `sort=productName,asc` or `sort=id,desc`).

### Sample Request
```http
GET /api/v1/products?page=0&size=10&sort=createdOn,desc
```

### Sample Response Format
```json
{
  "content": [
    {
      "id": 1,
      "productName": "MacBook Pro 16",
      "createdBy": "admin",
      "createdOn": "2026-09-01T12:40:56.267Z",
      "modifiedBy": null,
      "modifiedOn": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false, "empty": false }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 10,
  "number": 0,
  "empty": false
}
```

---

## Error Handling & Validation

Centralized handling is managed by `@RestControllerAdvice` in `GlobalExceptionHandler`.

### Standard Error Response Schema
```json
{
  "timestamp": "2026-09-01T16:50:12.450Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 999",
  "path": "/api/v1/products/999"
}
```

### Handled Status Codes
- `400 Bad Request`: Validation failure (e.g. blank product name, negative quantity).
- `401 Unauthorized`: Missing, expired, or invalid JWT token.
- `403 Forbidden`: Authenticated user lacks `ROLE_ADMIN` permissions.
- `404 Not Found`: Entity not found (`ResourceNotFoundException`).
- `409 Conflict`: Duplicate entry (`DuplicateResourceException`).
- `500 Internal Server Error`: Unhandled server exceptions.

---

## Testing Layer (85/85 Passing)

Comprehensive test coverage built using JUnit 5, Mockito, and Spring Boot Test:

```text
Results:
Tests run: 85, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test Suites
1. **Unit Tests (Mockito & AssertJ)**:
   - `ProductServiceImplTest` (11 tests): CRUD logic, duplicate prevention, pagination.
   - `ItemServiceImplTest` (12 tests): Item lifecycle, product association, pagination, not-found cases.
   - `RefreshTokenServiceImplTest` (6 tests): Creation, validation, token expiration, rotation.
   - `AuthServiceImplTest` (10 tests): User registration, duplicate username/email, login verification.
   - `GlobalExceptionHandlerTest` (7 tests): Correct status codes and payload structure for all errors.
2. **Integration Tests (MockMvc & Spring Boot Test)**:
   - `AuthControllerTest` (8 tests): Authentication endpoints, token generation.
   - `ProductControllerTest` (10 tests): RBAC, input validation, CRUD, nested items.
   - `ItemControllerTest` (9 tests): Inventory CRUD, validation, role restrictions.
   - `JwtSecurityTest` (4 tests): Header extraction, expired token rejection, unauthorized paths.
   - `ProductManagementApiApplicationTests` (8 tests): Complete E2E user-flow integration test.

### Running Tests
```bash
mvn clean test
```

---

## Running with Docker & Docker Compose

A single command builds the application and boots both PostgreSQL and the Spring Boot API:

```bash
docker compose up -d --build
```

### Services Started:
- **`postgres`**: Runs on port `5432` with a persistent Docker volume (`pgdata`).
- **`product-management-api`**: Multi-stage build running on port `8080`, configured with healthcheck dependency waiting for PostgreSQL to be healthy before starting.

To stop the containers:
```bash
docker compose down
```

---

## Running Locally

### Prerequisites
- Java 21 JDK
- PostgreSQL (or run via Docker)
- Maven 3.9+

### Configure Database
Verify or edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/product_management_db
spring.datasource.username=postgres
spring.datasource.password=root
```

### Start the Application
```bash
mvn spring-boot:run
```

*Note: On first startup, `DataInitializer` automatically creates the `ROLE_ADMIN` and `ROLE_USER` roles and provisions a default administrator user:*
- **Username**: `admin`
- **Password**: `Admin@123`

---

## Testing with Swagger / OpenAPI

Interactive Swagger UI documentation is available at:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Quick Testing Walkthrough:
1. Open the URL in your browser.
2. Under **`auth-controller`**, execute **`POST /api/v1/auth/login`** with:
   ```json
   {
     "username": "admin",
     "password": "Admin@123"
   }
   ```
3. Copy the returned `accessToken`.
4. Click the green **Authorize 🔓** button at the top-right and paste the token.
5. All protected endpoints are now authorized and ready for live testing.
