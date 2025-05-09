# Spring Boot OAuth2 Microservices with PostgreSQL

This project demonstrates a complete OAuth2 implementation with Spring Boot 3.3.11 using a microservices architecture. It includes an Authorization Server, API Gateway, and Resource Service, all integrated with PostgreSQL for user authentication and authorization.

## Project Architecture

### 1. Authorization Server (auth-server)
- Implements OAuth2 Authorization Server using Spring Security OAuth2
- Handles user authentication and token issuance
- Manages client registrations
- Runs on port 9000

### 2. API Gateway (api-gateway)
- Routes requests to the appropriate services
- Handles authentication and authorization at the gateway level
- Implements token relay to backend services
- Runs on port 8090

### 3. Resource Service (resource-service)
- Protects API resources using OAuth2 tokens
- Validates tokens issued by the Authorization Server
- Implements role-based access control
- Runs on port 8081

### 4. Common Module (common)
- Shared models and DTOs used across services
- Database entity definitions

## Database Schema

The project uses the following database tables:

1. **users** - Stores user information
   - id (PK)
   - username (unique)
   - password (encrypted)
   - email (unique)
   - enabled

2. **roles** - Stores role information
   - id (PK)
   - name (unique)

3. **permissions** - Stores permission information
   - id (PK)
   - name (unique)

4. **user_roles** - Many-to-many relationship between users and roles
   - user_id (FK)
   - role_id (FK)

5. **role_permissions** - Many-to-many relationship between roles and permissions
   - role_id (FK)
   - permission_id (FK)

## Setup and Running

### Prerequisites
- Java 17+
- PostgreSQL database
- Maven

### Database Setup
1. Create a PostgreSQL database named `oauth2db`
2. Update database credentials in each service's `application.yml` if needed

### Building and Running
1. Build the project: `mvn clean install`
2. Start the Authorization Server: `cd auth-server && mvn spring-boot:run`
3. Start the Resource Service: `cd resource-service && mvn spring-boot:run`
4. Start the API Gateway: `cd api-gateway && mvn spring-boot:run`

The services will initialize with sample users:
- Username: `user`, Password: `password`, Role: `USER`
- Username: `admin`, Password: `password`, Role: `ADMIN`

## Getting Tokens with Postman

### 1. Client Credentials Grant (Service-to-Service)

1. Open Postman and create a new request
2. Set the request method to `POST`
3. Set the URL to `http://localhost:9000/oauth2/token`
4. In the Authorization tab:
   - Type: Basic Auth
   - Username: `resource-server-1` (client ID)
   - Password: `resource-server-1-secret` (client secret)
5. In the Body tab:
   - Select `x-www-form-urlencoded`
   - Add key-value pairs:
     - `grant_type`: `client_credentials`
     - `scope`: `read write`
6. Send the request
7. You will receive a response with an access token:
   ```json
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "token_type": "Bearer",
     "expires_in": 3600,
     "scope": "read write"
   }
   ```

### 2. Password Grant (User Authentication)

1. Open Postman and create a new request
2. Set the request method to `POST`
3. Set the URL to `http://localhost:9000/oauth2/token`
4. In the Authorization tab:
   - Type: Basic Auth
   - Username: `resource-server-1` (client ID)
   - Password: `resource-server-1-secret` (client secret)
5. In the Body tab:
   - Select `x-www-form-urlencoded`
   - Add key-value pairs:
     - `grant_type`: `password`
     - `username`: `user` or `admin`
     - `password`: `password`
     - `scope`: `read write`
6. Send the request
7. You will receive a response with an access token and refresh token:
   ```json
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "token_type": "Bearer",
     "expires_in": 3600,
     "scope": "read write"
   }
   ```

### 3. Authorization Code Grant (Web Application Flow)

1. Open a browser and navigate to:
   ```
   http://localhost:9000/oauth2/authorize?response_type=code&client_id=api-gateway&scope=read%20write&redirect_uri=http://localhost:8090/login/oauth2/code/gateway
   ```
2. Log in with username `user` or `admin` and password `password`
3. Approve the requested permissions
4. You will be redirected to the gateway with an authorization code
5. In Postman, create a new request:
   - Method: `POST`
   - URL: `http://localhost:9000/oauth2/token`
   - Authorization: Basic Auth with client ID `api-gateway` and secret `gateway-secret`
   - Body (x-www-form-urlencoded):
     - `grant_type`: `authorization_code`
     - `code`: `<the code from the redirect URL>`
     - `redirect_uri`: `http://localhost:8090/login/oauth2/code/gateway`
6. Send the request
7. You will receive a response with an access token and refresh token

### 4. Using the Token to Access Protected Resources

1. Create a new request in Postman
2. Set the URL to `http://localhost:8090/api/user/info` (through the gateway)
3. In the Authorization tab:
   - Type: Bearer Token
   - Token: `<paste the access token>`
4. Send the request
5. You should receive a successful response if the token is valid and has the required permissions

## Testing Different Endpoints

- Public endpoint (no authentication required): `http://localhost:8090/api/public/info`
- User endpoint (requires USER role): `http://localhost:8090/api/user/info`
- Admin endpoint (requires ADMIN role): `http://localhost:8090/api/admin/info`

## Production Considerations

For a production deployment, consider the following:

1. **Security Enhancements**:
   - Use HTTPS for all services
   - Store secrets in a secure vault (e.g., HashiCorp Vault, AWS Secrets Manager)
   - Implement rate limiting
   - Add IP filtering

2. **High Availability**:
   - Deploy multiple instances of each service
   - Use a load balancer
   - Implement service discovery (e.g., Eureka, Consul)

3. **Monitoring and Logging**:
   - Add centralized logging (e.g., ELK stack)
   - Implement metrics collection (e.g., Prometheus, Grafana)
   - Set up alerting

4. **Token Management**:
   - Implement token revocation
   - Use shorter token lifetimes
   - Implement refresh token rotation

5. **Database**:
   - Use connection pooling
   - Implement database replication
   - Set up regular backups
