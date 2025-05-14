# Obtaining OAuth2 Tokens with Postman

This guide provides detailed step-by-step instructions for obtaining OAuth2 tokens using Postman for each authentication flow supported by the Spring Boot OAuth2 microservices architecture.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Client Credentials Flow](#client-credentials-flow)
3. [Authorization Code Flow](#authorization-code-flow)
4. [Password Grant Flow (After Modification)](#password-grant-flow-after-modification)
5. [Using Tokens to Access Protected Resources](#using-tokens-to-access-protected-resources)
6. [Troubleshooting](#troubleshooting)

## Prerequisites

Before you begin, ensure you have:

1. Postman installed (https://www.postman.com/downloads/)
2. The Spring Boot OAuth2 microservices running:
   - Auth Server on port 9000
   - Resource Service on port 8081
   - API Gateway on port 8090

## Client Credentials Flow

The Client Credentials flow is used for service-to-service communication where no user is involved.

### Step 1: Create a new request in Postman

1. Open Postman and create a new request
2. Set the request method to **POST**
3. Enter the token endpoint URL: `http://localhost:9000/oauth2/token`

### Step 2: Configure Basic Authentication

1. Go to the **Authorization** tab
2. Select **Basic Auth** from the Type dropdown
3. Enter the client credentials:
   - Username: `resource-server-1`
   - Password: `resource-server-1-secret`

### Step 3: Configure request body

1. Go to the **Body** tab
2. Select **x-www-form-urlencoded**
3. Add the following key-value pairs:
   - `grant_type`: `client_credentials`
   - `scope`: `read write`

### Step 4: Send the request

1. Click the **Send** button
2. You should receive a response with a status code of 200 OK
3. The response body should contain:
   - `access_token`: The JWT token to use for authentication
   - `token_type`: "Bearer"
   - `expires_in`: Token expiration time in seconds
   - `scope`: The granted scopes

### Example Response

```json
{
  "access_token": "eyJraWQiOiI4ZjU3YTRhNi1jYmI0LTRkM2UtOGQwMS1mMzVlYmJkZTg4NWIiLCJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read write"
}
```

**Note**: The token obtained through client credentials will not contain user-specific roles or permissions because it represents the client application itself, not a user.

## Authorization Code Flow

The Authorization Code flow is designed for web applications where the client secret can be securely stored.

### Step 1: Obtain an Authorization Code

1. Open a web browser
2. Navigate to the authorization endpoint with the following parameters:
   ```
   http://localhost:9000/oauth2/authorize?response_type=code&client_id=resource-server-1&scope=read%20write&redirect_uri=http://localhost:8081/login/oauth2/code/resource-server-1
   ```
3. You will be redirected to the login page
4. Enter user credentials:
   - Username: `admin`
   - Password: `password`
5. After successful authentication, you will be asked to approve the requested permissions
6. Click "Approve"
7. You will be redirected to the redirect URI with an authorization code in the URL:
   ```
   http://localhost:8081/login/oauth2/code/resource-server-1?code=YOUR_AUTHORIZATION_CODE
   ```
8. Copy the authorization code from the URL (the value after `code=`)

### Step 2: Exchange the Authorization Code for a Token

1. Open Postman and create a new request
2. Set the request method to **POST**
3. Enter the token endpoint URL: `http://localhost:9000/oauth2/token`

### Step 3: Configure Basic Authentication

1. Go to the **Authorization** tab
2. Select **Basic Auth** from the Type dropdown
3. Enter the client credentials:
   - Username: `resource-server-1`
   - Password: `resource-server-1-secret`

### Step 4: Configure request body

1. Go to the **Body** tab
2. Select **x-www-form-urlencoded**
3. Add the following key-value pairs:
   - `grant_type`: `authorization_code`
   - `code`: `YOUR_AUTHORIZATION_CODE` (the code you copied in Step 1)
   - `redirect_uri`: `http://localhost:8081/login/oauth2/code/resource-server-1`

### Step 5: Send the request

1. Click the **Send** button
2. You should receive a response with a status code of 200 OK
3. The response body should contain:
   - `access_token`: The JWT token to use for authentication
   - `refresh_token`: A token that can be used to obtain a new access token
   - `token_type`: "Bearer"
   - `expires_in`: Token expiration time in seconds
   - `scope`: The granted scopes

### Example Response

```json
{
  "access_token": "eyJraWQiOiI4ZjU3YTRhNi1jYmI0LTRkM2UtOGQwMS1mMzVlYmJkZTg4NWIiLCJhbGciOiJSUzI1NiJ9...",
  "refresh_token": "LJoMgCOji89KlF-rJ7-VvQ",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read write"
}
```

### Step 6: Decode the JWT Token

1. Copy the access_token value
2. Go to https://jwt.io/
3. Paste the token in the "Encoded" field
4. Examine the decoded payload to verify that it contains the user's roles

## Password Grant Flow (After Modification)

**Note**: This flow requires modifying the project to add Password Grant support as described in the [OAUTH2_GUIDE.md](OAUTH2_GUIDE.md) document.

### Step 1: Create a new request in Postman

1. Open Postman and create a new request
2. Set the request method to **POST**
3. Enter the token endpoint URL: `http://localhost:9000/oauth2/token`

### Step 2: Configure Basic Authentication

1. Go to the **Authorization** tab
2. Select **Basic Auth** from the Type dropdown
3. Enter the client credentials:
   - Username: `resource-server-1`
   - Password: `resource-server-1-secret`

### Step 3: Configure request body

1. Go to the **Body** tab
2. Select **x-www-form-urlencoded**
3. Add the following key-value pairs:
   - `grant_type`: `password`
   - `username`: `admin`
   - `password`: `password`
   - `scope`: `read write`

### Step 4: Send the request

1. Click the **Send** button
2. You should receive a response with a status code of 200 OK
3. The response body should contain:
   - `access_token`: The JWT token to use for authentication
   - `refresh_token`: A token that can be used to obtain a new access token
   - `token_type`: "Bearer"
   - `expires_in`: Token expiration time in seconds
   - `scope`: The granted scopes

### Example Response

```json
{
  "access_token": "eyJraWQiOiI4ZjU3YTRhNi1jYmI0LTRkM2UtOGQwMS1mMzVlYmJkZTg4NWIiLCJhbGciOiJSUzI1NiJ9...",
  "refresh_token": "LJoMgCOji89KlF-rJ7-VvQ",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read write"
}
```

## Using Tokens to Access Protected Resources

Once you have obtained an access token, you can use it to access protected resources.

### Step 1: Create a new request in Postman

1. Open Postman and create a new request
2. Set the request method to **GET**
3. Enter the resource endpoint URL:
   - For user resources: `http://localhost:8081/api/user/info`
   - For admin resources: `http://localhost:8081/api/admin/info`
   - Or through the gateway: `http://localhost:8090/api/user/info`

### Step 2: Configure Bearer Token Authentication

1. Go to the **Authorization** tab
2. Select **Bearer Token** from the Type dropdown
3. Enter the access token you obtained in the previous steps

### Step 3: Send the request

1. Click the **Send** button
2. If the token is valid and has the required roles/permissions, you should receive a response with a status code of 200 OK
3. If the token is invalid or lacks the required roles/permissions, you will receive a 401 Unauthorized or 403 Forbidden response

### Example Response (Success)

```json
{
  "message": "Protected resource accessed successfully",
  "username": "admin",
  "service": "Resource Service",
  "roles": ["ADMIN"]
}
```

## Troubleshooting

### Invalid Client

If you receive an "invalid_client" error, check that:
- The client ID and secret are correct
- The client is registered with the authorization server
- The client is authorized for the requested grant type

### Invalid Grant

If you receive an "invalid_grant" error, check that:
- The authorization code is valid and has not expired (they typically expire after a few minutes)
- The redirect URI matches exactly what was used to obtain the authorization code
- The user credentials are correct (for password grant)

### Invalid Scope

If you receive an "invalid_scope" error, check that:
- The requested scopes are registered for the client
- The scopes are properly formatted in the request

### Unauthorized/Forbidden

If you receive a 401 Unauthorized or 403 Forbidden when accessing a protected resource, check that:
- The token is valid and has not expired
- The token contains the required roles/permissions for the resource
- The token is properly formatted in the Authorization header

### Token Expiration

If your token has expired, you can:
1. Obtain a new token using the original grant flow
2. Use the refresh token (if available) to obtain a new access token without requiring user interaction

#### Using Refresh Token

1. Create a new POST request to `http://localhost:9000/oauth2/token`
2. Configure Basic Authentication with client credentials
3. Add the following form parameters:
   - `grant_type`: `refresh_token`
   - `refresh_token`: Your refresh token
4. Send the request to receive a new access token
