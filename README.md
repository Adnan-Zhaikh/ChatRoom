# pingroom

A real-time chat room built on Spring Boot's WebSocket support, using STOMP messaging over SockJS. Deployed as a Docker container on Render.

## Architecture

The app uses the publish-subscribe pattern rather than raw socket handling:

- **Transport layer:** SockJS provides a WebSocket connection with automatic fallback behavior for environments where a raw WebSocket isn't available. The client opens a SockJS connection to `/chat`.
- **Messaging layer:** STOMP (Simple Text Oriented Messaging Protocol) runs on top of that connection, giving the app named destinations (`/app/...`, `/topic/...`) instead of a single undifferentiated byte stream.
- **Broker:** Spring's built-in simple message broker handles routing. Clients publish to `/app/sendMessage`; the broker fans out broadcasts to every client subscribed to `/topic/message`.
- **Rendering:** The chat page itself is server-rendered via Thymeleaf (`chat.html`), served from a single controller.

### Message flow

1. Client sends a JSON payload (`{ sender, content }`) to `/app/sendMessage` over the STOMP connection.
2. `ChatController.sendMessage` receives it, mapped via `@MessageMapping("/sendMessage")`.
3. The return value is broadcast to `/topic/message` via `@SendTo`.
4. Every connected client subscribed to `/topic/message` receives the message and renders it client-side.

There's no persistence — messages exist only in-memory, for the duration of the broker relaying them. Nothing is stored; refreshing a client loses prior history.

## Project structure

```
src/main/java/com/chatroom/app/
  AppApplication.java          Spring Boot entry point
  config/webSocketConfig.java  STOMP endpoint + broker configuration
  controller/ChatController.java  Message handling + page routing
  model/ChatMessage.java       Message payload (sender, content)
src/main/resources/
  application.properties       Port binding config
  templates/chat.html          Chat UI (SockJS/STOMP client, vanilla JS)
Dockerfile                     Multi-stage build (Maven build -> JRE runtime)
```

## Configuration notes

- **Port binding:** `server.port=${PORT:8080}` — reads the platform-assigned `PORT` env var at runtime (required by Render), falls back to 8080 for local runs.
- **CORS / allowed origins:** `setAllowedOriginPatterns` in `webSocketConfig` is locked to the deployed origin. This has to be updated any time the deployment URL changes (e.g. if the Render service is recreated under a new subdomain) — the WebSocket handshake is rejected otherwise.

## Deployment

Built as a Docker image via a two-stage `Dockerfile`:

1. **Build stage** (`maven:3.8.8-eclipse-temurin-17`): resolves dependencies from `pom.xml`, compiles the app, packages a jar. Dependency resolution is cached separately from source compilation, so source-only changes don't re-trigger a full dependency re-download.
2. **Runtime stage** (`eclipse-temurin:17-jre-alpine`): copies only the built jar from the build stage. No build tooling ships in the final image.

Deployed on Render as a Docker-based web service, connected directly to this GitHub repo. Render rebuilds and redeploys on push to the connected branch.

## Known limitations

- No message history or persistence.
- No authentication — `sender` is a free-text field the client provides, not verified identity.
- Single shared room; no concept of multiple rooms/channels.
- Render's free tier spins the service down after inactivity, so the first request after idle time will be slow while it cold-starts.