# SmartCommerce AI — Backend Skeleton (Step 1 & 2)

Multi-module Maven project. Import the **root folder** into STS as
an existing Maven project — it will pick up all 3 modules.

## Modules included so far

| Module | Port | Purpose |
|---|---|---|
| `eureka-server` | 8761 | Service registry — every other service registers here |
| `api-gateway` | 8080 | Single entry point, routes `/api/auth/**` to auth-service |
| `auth-service` | 8081 | Registration, login, JWT issuance |

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL running locally, with a database named `smartcommerce_auth`
  (or update `auth-service/src/main/resources/application.yml`)

## Run order

1. `eureka-server` — start first, visit http://localhost:8761 to confirm it's up
2. `api-gateway` — starts and registers with Eureka
3. `auth-service` — starts and registers with Eureka

In STS: right-click each module → Run As → Spring Boot App, in that order.

## Try it

Once all 3 are running:

```
POST http://localhost:8080/api/auth/register
{
  "fullName": "Yogesh Kumar",
  "email": "yogesh@example.com",
  "password": "SecurePass123"
}
```

```
POST http://localhost:8080/api/auth/login
{
  "email": "yogesh@example.com",
  "password": "SecurePass123"
}
```

Both return a JWT in the response.

Swagger UI for auth-service directly: http://localhost:8081/swagger-ui.html

## Known TODOs (intentionally left for next steps)

- `JwtAuthenticationFilter` in `api-gateway` — currently the gateway lets
  every request through; it needs a `GlobalFilter` that validates the
  JWT and rejects/forwards requests accordingly.
- Google OAuth2 login flow — client registration is scaffolded in
  `application.yml` but the redirect/success handler isn't implemented yet.
- Refresh token endpoint.
- Replace `ddl-auto: update` with Flyway/Liquibase migrations before
  this goes anywhere near production.

## Next service to build

Per the roadmap: **User/Vendor Service** — builds on top of the
`userId` / `roles` claims this Auth Service puts into the JWT.
