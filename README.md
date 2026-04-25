# AI Guardrail API & Real-time Virality Engine

A high-performance Spring Boot microservice designed as a central gateway and guardrail system for AI-human interactions. This project implements strict mathematical constraints to prevent AI compute runaway using Redis for atomic, distributed state management.

## 🚀 Key Features

- **Atomic Guardrails**: Thread-safe implementation of Horizontal, Vertical, and Cooldown caps using Redis `INCR` and `SETNX` operations.
- **Virality Engine**: Real-time scoring system (Bot Reply: +1, Human Like: +20, Human Comment: +50).
- **Notification Throttling**: Intelligent batching system to prevent user notification churn.
- **Stateless Architecture**: Zero in-memory state; all counters and cooldowns are managed by Redis.
- **Real-time Dashboard**: Premium glassmorphism monitoring dashboard with live stats and testing tools.

## 🛠 Tech Stack

- **Backend**: Java 17+, Spring Boot 3.x, JPA/Hibernate
- **Database**: PostgreSQL (Data Integrity Source of Truth)
- **Cache/Gatekeeper**: Redis (Atomic Operations & TTL Management)
- **Frontend**: Vanilla JS + Tailwind CSS + Lucide Icons (Premium Dashboard)

## 🏗 System Architecture

### Phase 1: Core API & Database
- Entities: `User`, `Bot`, `Post`, `Comment`.
- REST Endpoints for creating posts, adding comments, and liking content.

### Phase 2: Redis Virality Engine & Atomic Locks
- **Horizontal Cap**: Max 100 bot replies per post. Controlled via Redis `INCR`.
- **Vertical Cap**: Max 20 levels deep in comment threads.
- **Cooldown Cap**: 10-minute cooldown for Bot-to-Human interactions.

### Phase 3: Notification Engine (Smart Batching)
- Throttles bot-originating notifications if sent within 15 minutes.
- Pending notifications are stored in a Redis List (`RPUSH`).
- A `Scheduled` task sweeps and summarizes notifications every 5 minutes.

## 🚦 Corner Cases & Stress Testing

- **Race Conditions**: Specifically designed to handle concurrent bot spam. The system uses atomic increments followed by rollback on failure to ensure exactly 100 replies are allowed, even under high load.
- **statelessness**: The application can be scaled horizontally without session stickiness as all state resides in Redis.

## 🏁 Getting Started

### 1. Prerequisite: Infrastructure
Run the provided `docker-compose.yml` to spin up PostgreSQL and Redis:
```bash
docker-compose up -d
```

### 2. Run the Application
```bash
mvn spring-boot:run
```

### 3. Access the Dashboard
Open your browser and navigate to:
`http://localhost:8080/`

## 🧪 Testing with Postman
A Postman collection is included in the repository (`Postman_Collection.json`) for manual testing of API endpoints.

---
*Built with precision for Backend Engineering Excellence.*
