# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Package as JAR
./mvnw package
```

Maven wrapper scripts (`mvnw`, `mvnw.cmd`) are included for consistent builds.

## Architecture Overview

This is a **Spring Boot 4.0.5 WebSocket chat application** using Java 21, implementing real-time messaging with STOMP over WebSocket.

### Layer Structure

```
Controller (REST + WebSocket @MessageMapping)
    ↓
Service (Business Logic)
    ↓
Repository (Spring Data JPA)
    ↓
PostgreSQL + Redis
```

### Key Technology Stack

- **WebSocket**: STOMP protocol at `/wss` endpoint with SockJS fallback
- **Authentication**: JWT (HS512) with OAuth2 Resource Server pattern
- **Database**: PostgreSQL (primary), Redis (session storage + token blacklist)
- **File Storage**: AWS S3 for media uploads
- **Security**: Stateless JWT authentication, role-based access (USER, ADMIN)

### Core Domain Model

- **User** → implements Spring Security's UserDetails
- **Conversation** → supports PRIVATE and GROUP types; private conversations use `participantHash` for deduplication
- **ChatMessage** → linked to Conversation and User (sender), supports MessageMedia attachments
- **RedisToken** → JWT blacklist for logout mechanism

### Real-Time Message Flow

1. Client sends STOMP message to `/app/chat`
2. `ChatController.handleChat()` receives with authenticated Principal
3. `ChatService.sendChatMessage()` persists to database
4. Message broadcast via `SimpMessagingTemplate` to `/user/{userId}/queue/chat`

### WebSocket Security Chain

1. `WebSocketHandshakeInterceptor` → handshake validation
2. `ClientInboundAuthentication` → JWT verification on STOMP frames
3. `JwtAuthenticationEntryPoint` / `JwtAccessDeniedHandler` → error handling

### Configuration

Main config in `src/main/resources/application.yml`:
- Database: `jdbc:postgresql://localhost:5432/booking`
- Redis: `localhost:6379`
- CORS origins: `localhost:3000`, `localhost:5173`
- JWT: 1 hour access tokens, 14 day refresh tokens
- Upload limits: 20MB per file, 100MB per request

### REST API Base Paths

- `/api/v1/` and `/api/` prefixes used for REST endpoints
- Controllers: `AuthenticationController`, `ConversationController`, `MediaController`, `UserController`
