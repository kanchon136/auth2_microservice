# Spring Boot OAuth2 Microservices - Detailed Instructions

This document provides detailed step-by-step instructions for setting up, running, and using the Spring Boot OAuth2 microservices architecture with PostgreSQL.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Setup Instructions](#setup-instructions)
3. [Running the Services](#running-the-services)
4. [User Management](#user-management)
5. [Role and Permission Management](#role-and-permission-management)
6. [Authentication Flow](#authentication-flow)
7. [Obtaining Tokens with Postman](#obtaining-tokens-with-postman)
8. [Testing Protected Resources](#testing-protected-resources)
9. [Request Flow](#request-flow)
10. [Troubleshooting](#troubleshooting)

## Architecture Overview

The system consists of the following microservices:

1. **Registry Service (port 8761)**
   - Service discovery using Netflix Eureka
   - Allows services to find and communicate with each other

2. **Authorization Server (port 9000)**
   - Handles user authentication and token issuance
   - Manages user accounts, roles, and permissions
   - Provides endpoints for user and role management

3. **API Gateway (port 8090)**
   - Routes requests to appropriate services
   - Handles authentication and authorization at the gateway level
   - Implements token relay to backend services
   - Provides CORS configuration

4. **Resource Service (port 8081)**
   - Protects API resources using OAuth2 tokens
   - Validates tokens issued by the Authorization Server
   - Implements role-based and permission-based access control

5. **Common Module**
   - Shared models and DTOs used across services
   - Database entity definitions for User, Role, and Permission

## Setup Instructions

### Prerequisites

- Java 17+
- PostgreSQL database
- Maven

### Database Setup

1. Create a PostgreSQL database named `oauth2db`:

```sql
CREATE DATABASE oauth2db;
```

2. Update database credentials in each service's `application.yml` if needed.

### Building the Project

1. Clone the repository:

```bash
git clone <repository-url>
cd spring-oauth2-microservices
```

2. Build the project:

```bash
mvn clean install
```

## Running the Services

Start the services in the following order:

1. **Registry Service**:

```bash
cd registry-service
mvn spring-boot:run
```

2. **Authorization Server**:

```bash
cd auth-server
mvn spring-boot:run
```

3. **Resource Service**:

```bash
cd resource-service
mvn spring-boot:run
```

4. **API Gateway**:

```bash
cd api-gateway
mvn spring-boot:run
```

Verify that all services are registered with Eureka by visiting:
http://localhost:8761

## User Management

The system initializes with the following users:

- **Regular User**:
  - Username: `user`
  - Password: `password`
  - Role: `USER`
  - Permissions: `READ`

- **Admin User**:
  - Username: `admin`
  - Password: `password`
  - Role: `ADMIN`
  - Permissions: `READ`, `WRITE`, `DELETE`, `ADMIN`

- **Superadmin User**:
  - Username: `superadmin`
  - Password: `password`
  - Role: `SUPERADMIN`
  - Permissions: `READ`, `WRITE`, `DELETE`, `ADMIN`, `SUPERADMIN`, `USER_MANAGEMENT`, `ROLE_MANAGEMENT`

### Creating a New User

Only the superadmin can create new users:

```http
POST http://localhost:9000/users
Authorization: Bearer <superadmin-token>
Content-Type: application/json

{
  "username": "newuser",
  "password": "password",
  "email": "newuser@example.com",
  "roles": [1, 2]  // Role IDs
}
```

### Getting All Users

```http
GET http://localhost:9000/users
Authorization: Bearer <superadmin-token>
```

### Getting a User by ID

```http
GET http://localhost:9000/users/{id}
Authorization: Bearer <superadmin-token>
```

### Assigning Roles to a User

```http
PUT http://localhost:9000/users/{id}/roles
Authorization: Bearer <superadmin-token>
Content-Type: application/json

{
  "roleIds": [1, 2, 3]  // Role IDs
}
```

### Removing a Role from a User

```http
DELETE http://localhost:9000/users/{id}/roles/{roleId}
Authorization: Bearer <superadmin-token>
```

### Deleting a User

```http
DELETE http://localhost:9000/users/{id}
Authorization: Bearer <superadmin-token>
```

## Role and Permission Management

### Creating a New Role

```http
POST http://localhost:9000/roles
Authorization: Bearer <superadmin-token>
Content-Type: application/json

{
  "name": "CUSTOM_ROLE",
  "permissions": [1, 2, 3]  // Permission IDs
}
```

### Getting All Roles

```http
GET http://localhost:9000/roles
Authorization: Bearer <superadmin-token>
```

### Getting a Role by ID

```http
GET http://localhost:9000/roles/{id}
Authorization: Bearer <superadmin-token>
```

### Assigning Permissions to a Role

```http
PUT http://localhost:9000/roles/{id}/permissions
Authorization: Bearer <superadmin-token>
Content-Type: application/json

{
  "permissionIds": [1, 2, 3, 4]  // Permission IDs
}
```

### Removing a Permission from a Role

```http
DELETE http://localhost:9000/roles/{id}/permissions/{permissionId}
Authorization: Bearer <superadmin-token>
```

### Deleting a Role

```http
DELETE http://localhost:9000/roles/{id}
Authorization: Bearer <superadmin-token>
```

## Authentication Flow

The system supports multiple OAuth2 grant types:

1. **Authorization Code Flow** (for web applications)
2. **Password Grant** (for direct user authentication)
3. **Client Credentials** (for service-to-service communication)
4. **Refresh Token** (for obtaining new access tokens)

## Obtaining Tokens with Postman

### 1. Client Credentials Grant (Service-to-Service)

1. Create a new POST request to `http://localhost:9000/oauth2/token`
2. In the Authorization tab, select "Basic Auth" and enter:
   - Username: `resource-server-1`
   - Password: `resource-server-1-secret`
3. In the Body tab, select "x-www-form-urlencoded" and add:
   - `grant_type`: `client_credentials`
   - `scope`: `read write`
4. Send the request to receive an access token

### 2. Password Grant (User Authentication)

1. Create a new POST request to `http://localhost:9000/oauth2/token`
2. In the Authorization tab, select "Basic Auth" and enter:
   - Username: `resource-server-1`
   - Password: `resource-server-1-secret`
3. In the Body tab, select "x-www-form-urlencoded" and add:
   - `grant_type`: `password`
   - `username`: `user`, `admin`, or `superadmin`
   - `password`: `password`
   - `scope`: `read write`
4. Send the request to receive an access token and refresh token

### 3. Authorization Code Grant (Web Application Flow)

1. In a browser, navigate to:
   ```
   http://localhost:9000/oauth2/authorize?response_type=code&client_id=api-gateway&scope=read%20write&redirect_uri=http://localhost:8090/login/oauth2/code/gateway
   ```
2. Log in with username `user`, `admin`, or `superadmin` and password `password`
3. Approve the requested permissions
4. You will be redirected with an authorization code
5. In Postman, create a POST request to `http://localhost:9000/oauth2/token` with:
   - Basic Auth: `api-gateway`/`gateway-secret`
   - Body: 
     - `grant_type`: `authorization_code`
     - `code`: `<the code from the redirect URL>`
     - `redirect_uri`: `http://localhost:8090/login/oauth2/code/gateway`

### 4. Refresh Token Grant

1. Create a new POST request to `http://localhost:9000/oauth2/token`
2. In the Authorization tab, select "Basic Auth" and enter:
   - Username: `resource-server-1`
   - Password: `resource-server-1-secret`
3. In the Body tab, select "x-www-form-urlencoded" and add:
   - `grant_type`: `refresh_token`
   - `refresh_token`: `<your refresh token>`
4. Send the request to receive a new access token

## Testing Protected Resources

The resource service provides several endpoints with different permission requirements:

### Public Endpoint (No Authentication)

```http
GET http://localhost:8090/api/sample/public
```

### User Role Required

```http
GET http://localhost:8090/api/sample/user
Authorization: Bearer <token>
```

### Admin Role Required

```http
GET http://localhost:8090/api/sample/admin
Authorization: Bearer <token>
```

### Superadmin Role Required

```http
GET http://localhost:8090/api/sample/superadmin
Authorization: Bearer <token>
```

### Write Permission Required

```http
POST http://localhost:8090/api/sample/write
Authorization: Bearer <token>
Content-Type: application/json

{
  "data": "Sample data"
}
```

### Delete Permission Required

```http
DELETE http://localhost:8090/api/sample/delete/123
Authorization: Bearer <token>
```

### User Management Permission Required

```http
GET http://localhost:8090/api/sample/user-management
Authorization: Bearer <token>
```

## Request Flow

Here's a detailed step-by-step flow of how requests are processed in the system:

1. **Client makes a request to the API Gateway**
   - The request is sent to `http://localhost:8090/api/sample/user`

2. **API Gateway authenticates the request**
   - If no token is provided, the gateway redirects to the authorization server
   - If a token is provided, the gateway validates it

3. **API Gateway routes the request**
   - Based on the path, the gateway routes the request to the appropriate service
   - For `/api/**` paths, requests are routed to the resource service
   - The gateway adds the token to the request using TokenRelay

4. **Resource Service receives the request**
   - The resource service validates the token
   - It extracts roles and permissions from the token
   - It checks if the user has the required role/permission for the endpoint

5. **Resource Service processes the request**
   - If authorized, the service processes the request and returns a response
   - If not authorized, it returns a 403 Forbidden response

6. **API Gateway returns the response to the client**
   - The gateway forwards the response from the resource service to the client

## Troubleshooting

### Common Issues

1. **Service not registered with Eureka**
   - Ensure the registry service is running
   - Check the Eureka client configuration in the service's `application.yml`
   - Verify network connectivity between services

2. **Authentication failures**
   - Check that you're using the correct client credentials
   - Verify the token is valid and not expired
   - Ensure the token has the required scopes and roles

3. **Database connection issues**
   - Verify PostgreSQL is running
   - Check database credentials in `application.yml`
   - Ensure the database schema is created correctly

4. **CORS errors**
   - Check the CORS configuration in the API Gateway
   - Ensure the client's origin is allowed
   - Verify that the required headers are exposed

### Logs

Each service has detailed logging enabled. Check the logs for more information:

- Registry Service: `registry-service/logs`
- Authorization Server: `auth-server/logs`
- Resource Service: `resource-service/logs`
- API Gateway: `api-gateway/logs`

Increase log verbosity by setting the log level in `application.yml`:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    com.example: DEBUG
```
