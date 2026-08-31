# ZidTech API Gateway (`api-gateway`)

The **API Gateway** is the central edge proxy, perimeter security checkpoint, and traffic manager for the ZidTech Shopping Cart microservices ecosystem. 

Built on **Spring Cloud Gateway (WebFlux / Netty)**, it provides an ultra-low-latency, asynchronous, non-blocking entry point capable of routing traffic, enforcing distributed rate limiting via **Redis**, and dynamic client-side load balancing via **Netflix Eureka**.

---

## 🌟 Key Architectural Features

* **Non-Blocking Reactive Core:** Powered by Netty and Project Reactor to handle high-concurrency traffic without per-thread blocking.
* **Dynamic Service Discovery & Routing:** Automatically resolves downstream container endpoints via **Eureka Service Registry** using the `lb://` scheme.
* **Token Bucket Rate Limiting:** Backed by **Redis** to protect downstream microservices against spam, brute-force attacks, and DDoS spikes (`HTTP 429 Too Many Requests`).
* **Client-Side Load Balancing:** Built-in Spring Cloud LoadBalancer distributing requests across healthy instances (Round-Robin).
* **Perimeter Header Enrichment:** Ready for distributed transaction tracing, passing `X-Correlation-Id`, `X-User-Id`, and `X-User-Role` to internal services.
* **Observability & Health Checks:** Integrated Spring Boot Actuator endpoints for container liveness and readiness probes.

---

## 🏗️ Tech Stack & Dependencies

| Technology | Version | Purpose |
| :--- | :--- | :--- |
| **Java** | `21 (LTS)` | Runtime platform |
| **Spring Boot** | `3.2.0` | Core framework |
| **Spring Cloud** | `2023.0.0` | Microservice routing & discovery |
| **Spring Cloud Gateway** | `4.1.0` | Reactive HTTP edge routing |
| **Spring Cloud Netflix Eureka Client** | `4.1.0` | Service registry registration |
| **Spring Data Redis (Reactive)** | `3.2.0` | Token bucket rate limiting engine |
| **Spring Boot Actuator** | `3.2.0` | Health check & metrics exposure |

---

## 📐 Network & Port Mapping

```text
              [ Client / Frontend / Postman ]
                            │
                            │ HTTP Requests (Port 8080)
                            ▼
               +───────────────────────────+
               │   API GATEWAY (:8080)     │
               +───────────────────────────+
                 │             │         │
    (Rate Limit) │             │         │ (Service Discovery)
                 ▼             │         ▼
         +───────────────+     │    +──────────────────────+
         │ REDIS (:6379) │     │    │ EUREKA SERVER (:8761)│
         +───────────────+     │    +──────────────────────+
                               │
            (Dynamic Load-Balanced Routing)
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
   +──────────────+     +──────────────+     +──────────────+
   │ AUTH-SERVICE │     │PRODUCT-SERVIC│     │ ORDER-SERVIC │
   │   (:8082)    │     │   (:8083)    │     │   (:8084)    │
   +──────────────+     +──────────────+     +──────────────+