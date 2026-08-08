# Technical Requirements Document
## SmartCommerce AI — Enterprise Multi-Vendor E-Commerce Platform

**Document owner:** Engineering
**Status:** Draft v1
**Companion to:** SmartCommerce AI PRD

---

## 1. Architecture Overview

Microservices architecture, not a modular monolith — chosen deliberately for this project since one of its explicit goals is to demonstrate production-grade distributed system design.

```
                        ┌─────────────────┐
                        │   React Client   │
                        └────────┬─────────┘
                                 │ HTTPS
                        ┌────────▼─────────┐
                        │   API Gateway     │  (Spring Cloud Gateway)
                        │  + Rate Limiting  │
                        │  + JWT validation │
                        └────────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────────┐
              │                  │                       │
    ┌─────────▼──────┐ ┌────────▼────────┐   ┌───────────▼─────────┐
    │  Auth Service   │ │ User/Vendor Svc  │   │   Product Service    │
    └─────────────────┘ └──────────────────┘   └───────────────────────┘
              │                  │                       │
    ┌─────────▼──────┐ ┌────────▼────────┐   ┌───────────▼─────────┐
    │ Cart/Wishlist   │ │  Order Service   │   │   Payment Service    │
    └─────────────────┘ └──────────────────┘   └───────────────────────┘
              │
    ┌─────────▼──────┐   ┌──────────────────┐
    │ Notification Svc│  │   AI/RAG Service  │
    └─────────────────┘  └──────────────────┘

    Service Registry: Eureka          Config: Spring Cloud Config (optional)
    Async messaging: Kafka/RabbitMQ (order events → notification, inventory)
```

**Why microservices over a monolith:** independent scaling (the AI service has very different resource needs — GPU/embedding calls — than the cart service), independent deployability per team/module, and it's the architecture pattern the project is explicitly meant to showcase. Trade-off accepted: more operational overhead (service discovery, distributed tracing, network latency between services) — justified here because it's a portfolio project meant to demonstrate exactly that complexity being handled well.

## 2. Frontend Stack

| Layer | Choice | Reason |
|---|---|---|
| Framework | React 18 | Component model fits a catalog/cart/checkout UI well; largest ecosystem for e-commerce UI patterns |
| Styling | Tailwind CSS | Fast to build consistent, responsive UI without hand-rolled CSS; pairs well with component libraries |
| State management | React Query (server state) + lightweight local state (Context/Zustand) | Most app state here is server data (products, cart, orders) — React Query handles caching/invalidation better than a global store for that; avoids Redux boilerplate for what is mostly CRUD data |
| API communication | REST over HTTPS, calling the API Gateway only (never individual services directly) | Keeps the frontend decoupled from backend service topology |

## 3. Backend Stack

| Layer | Choice | Reason |
|---|---|---|
| Language/runtime | Java 21 | LTS, virtual threads available for I/O-bound service calls |
| Framework | Spring Boot 3.x | Team's existing expertise; first-class Spring Cloud integration for microservices concerns |
| Service discovery | Netflix Eureka (Spring Cloud) | Simplest, most widely documented option for a Spring-native microservices setup |
| API Gateway | Spring Cloud Gateway | Reactive, integrates directly with Eureka for dynamic routing; avoids adding a separate non-Java gateway (e.g., Kong) for a Java-first stack |
| Inter-service messaging | Kafka (preferred) or RabbitMQ | Event-driven flows: order placed → notify, order placed → adjust inventory. Kafka chosen if throughput/replay matters; RabbitMQ acceptable if the team wants simpler ops for a portfolio-scale project |
| AI orchestration | Spring AI | Keeps AI calls (embeddings, chat completions, RAG orchestration) inside the Java ecosystem instead of introducing a separate Python service — reduces cross-language operational overhead |

## 4. Database

| Store | Used by | Reason |
|---|---|---|
| PostgreSQL (one schema/DB per service) | Auth, User/Vendor, Product, Order, Payment | Relational integrity for transactional data (orders, payments) matters more here than horizontal write scale; Postgres is battle-tested and has first-class Spring Data JPA support |
| Redis | Cart/session cache, rate limiting at gateway | Cart data is ephemeral/high-read — a cache-first store avoids hammering Postgres for something that doesn't need durability guarantees as strict as an order |
| PgVector (or Qdrant) | AI/RAG Service | Vector similarity search for NL product search and recommendations. PgVector chosen by default (stays inside Postgres, one less system to operate); Qdrant is the fallback if vector search performance/scale becomes a bottleneck |

**Database-per-service** is used rather than one shared database — each service owns its schema and exposes data to others only via its API, avoiding tight coupling through shared tables.

## 5. Authentication

- **JWT** for stateless session management across all services — access token issued by Auth Service, validated at the API Gateway (and optionally re-validated by individual services for defense in depth).
- **OAuth2 (Google login)** as an alternative sign-in path, handled entirely within Auth Service; downstream services never talk to Google directly.
- Token contains `userId` and `roles` claims so downstream services can authorize without a network call back to Auth Service on every request.
- **Refresh tokens**: short-lived access token (e.g., 15–60 min) + longer-lived refresh token, to limit the blast radius of a leaked access token without forcing frequent re-logins.
- Passwords hashed with BCrypt; never stored or logged in plaintext.

## 6. APIs

- **External-facing:** REST/JSON over HTTPS, all routed through the API Gateway. OpenAPI/Swagger generated per service for documentation.
- **Internal (service-to-service):** REST for synchronous calls (e.g., Order Service calling Product Service to check stock); Kafka/RabbitMQ events for asynchronous flows (order placed, payment completed, inventory low).
- **Versioning:** URL-based versioning (`/api/v1/...`) from day one, so breaking changes don't require a coordinated big-bang migration later.
- **AI endpoints:** exposed via the AI/RAG Service, e.g. `/api/v1/ai/search`, `/api/v1/ai/assistant`, `/api/v1/ai/recommendations` — kept behind the same gateway and auth as everything else, not treated as a special case.

## 7. Deployment Plan

- **Containerization:** every service Dockerized individually; `docker-compose` for local development spinning up all services + Postgres + Redis + Kafka + Eureka together.
- **CI/CD:** GitHub Actions — build, test, and push a Docker image per service on merge to main; deploy step triggers redeploy of the changed service only (not a full-platform redeploy).
- **Environments:** local (docker-compose) → staging → production, with environment-specific config via Spring profiles and environment variables (never hardcoded secrets).
- **Cloud:** AWS — S3 for product image storage; container hosting via ECS or a managed Kubernetes option (EKS) once the service count justifies orchestration overhead — for a portfolio-scale deployment, ECS is the pragmatic starting point over standing up a full k8s cluster.
- **Observability:** centralized logging (e.g., CloudWatch or ELK) and basic distributed tracing (Spring Cloud Sleuth / Micrometer Tracing) so a request can be followed across services — non-negotiable in a microservices system, since debugging without it is significantly harder than in a monolith.

## 8. Security Requirements

- All traffic over HTTPS/TLS, including internal service-to-service calls where feasible.
- JWT signature verification at the gateway; short token expiry + refresh flow.
- Role-based access control (`ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`) enforced at the controller level in each service, not just at the gateway.
- Input validation (Bean Validation annotations) on every request DTO to prevent injection and malformed-data issues.
- Rate limiting at the API Gateway to mitigate abuse/brute-force attempts on auth endpoints.
- Secrets (DB credentials, JWT signing key, OAuth client secret, AI provider API key) managed via environment variables / a secrets manager — never committed to source control.
- Payment data never touches your own database directly — delegate to the payment provider's tokenization (e.g., Stripe) so you're not storing raw card data.
- Dependency scanning (e.g., GitHub Dependabot) enabled given the number of third-party libraries (Spring Cloud, jjwt, Spring AI, etc.).

## 9. Key Technical Decisions Summary

| Decision | Reasoning |
|---|---|
| Microservices over monolith | Matches the project's goal of demonstrating distributed system design; independent scaling for the AI service in particular |
| Spring AI over a separate Python AI service | Keeps the whole backend in one language/runtime; avoids cross-language ops overhead for a portfolio-scale project |
| PostgreSQL + PgVector over a dedicated vector DB | One less system to run and learn; upgrade path to Qdrant left open if scale demands it |
| JWT + Gateway-level validation | Stateless auth scales horizontally without a shared session store; roles embedded in token avoid a network hop per request |
| Kafka/RabbitMQ for order-driven events | Decouples order placement from downstream effects (notify, inventory) so those services can fail/retry independently without blocking checkout |
| Database-per-service | Avoids tight coupling through shared tables; each service's schema can evolve independently |
| ECS over Kubernetes at this stage | Right-sized for the service count involved; k8s overhead not justified yet |

---

**Next step:** translate Section 3/4 into the concrete service-by-service data model (entities, relationships) for the User/Vendor Service, since Auth Service is already scaffolded.
