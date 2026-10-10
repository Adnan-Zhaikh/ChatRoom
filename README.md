# PingRoom

A real-time chat room built with Spring Boot, using WebSockets with STOMP messaging over SockJS. Type a message and everyone connected sees it instantly, with no page refresh.

**Live:** [pingroom-9n22.onrender.com/chat](https://pingroom-9n22.onrender.com/chat)

> The free host sleeps when idle, so the first load can take a while to wake up.

<!-- Add a screenshot or GIF of two browser windows chatting with each other. -->

## Why I built it

This was my first project with Spring Boot, and my first time working with WebSockets. I wanted to understand how real-time apps work: how a server can push data to many users at once instead of waiting for them to ask.

## How it works

PingRoom uses the publish-subscribe pattern instead of raw socket handling.

- **Transport (SockJS):** opens a WebSocket connection from the browser to `/chat`, and falls back automatically when a raw WebSocket isn't available.
- **Messaging (STOMP):** runs on top of that connection and gives the app named destinations (`/app/...`, `/topic/...`) instead of one undifferentiated stream of bytes.
- **Broker:** Spring's built-in simple message broker routes messages. Clients publish to `/app/sendMessage`, and the broker sends each broadcast to everyone subscribed to `/topic/message`.
- **Page:** the chat UI is rendered by Thymeleaf (`chat.html`) and uses vanilla JavaScript for the client.

### Message flow

1. The browser sends `{ sender, content }` as JSON to `/app/sendMessage`.
2. `ChatController.sendMessage` receives it through `@MessageMapping("/sendMessage")`.
3. The return value is broadcast to `/topic/message` through `@SendTo`.
4. Every connected client subscribed to `/topic/message` receives the message and displays it.

Messages are not stored anywhere. They exist only while the broker is relaying them, so refreshing the page clears the history.

## Tech

- Java 17
- Spring Boot (WebSocket support, Thymeleaf)
- STOMP over SockJS
- Vanilla JavaScript on the client
- Maven (with the Maven wrapper)
- Docker (multi-stage build)
- Deployed on Render

## Project structure

```
src/main/java/com/chatroom/app/
  AppApplication.java             Spring Boot entry point
  config/webSocketConfig.java     STOMP endpoint and broker configuration
  controller/ChatController.java  Message handling and page routing
  model/ChatMessage.java          Message payload (sender, content)
src/main/resources/
  application.properties          Port binding config
  templates/chat.html             Chat UI (SockJS/STOMP client)
Dockerfile                        Multi-stage build (Maven build, then JRE runtime)
```

## What I learned

- **WebSockets:** a persistent two-way connection instead of the usual request and response. This was the big new idea of the project.
- **STOMP and pub/sub:** how destinations and subscriptions turn one connection into something that feels like chat rooms and channels.
- **Spring Boot basics:** project structure, controllers, configuration, and how much it handles for you.
- **Message mapping:** `@MessageMapping` and `@SendTo` to receive and broadcast messages.
- **Docker multi-stage builds:** one stage builds the jar, and a small runtime stage ships only the jar. The final image has no build tools, and dependency downloads are cached so source-only changes rebuild quickly.
- **Deploying on Render:** reading the platform's `PORT` variable (`server.port=${PORT:8080}`), and deploying straight from GitHub.
- **A deployment gotcha:** the WebSocket handshake is rejected if the allowed origin doesn't match the live URL. If the Render service is recreated under a new subdomain, `setAllowedOriginPatterns` in the config has to be updated.

## Run it locally

Requirements: Java 17.

```bash
git clone https://github.com/Adnan-Zhaikh/PingRoom.git
cd PingRoom
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
```

Open [http://localhost:8080/chat](http://localhost:8080/chat). Open it in two windows to see messages arrive in both.

**Note:** the allowed origin in `webSocketConfig` is set to the deployed URL. If the connection is rejected locally, add `http://localhost:8080` to it.

### With Docker

```bash
docker build -t pingroom .
docker run -p 8080:8080 pingroom
```

## Known limitations

- No message history or storage.
- No login: `sender` is just a name typed by the user and is not verified.
- One shared room for everyone.
- The free hosting tier sleeps after inactivity, so the first request after idle time is slow.

## Roadmap

- [ ] **User login system**
- [ ] **Topic-based chat rooms:** a user creates a room for a specific topic
- [ ] **Invites:** the room owner invites people to a specific room, with proper security so only invited users can join
- [ ] Message history (needs a database)

## Author

**Adnan** — [@Adnan-Zhaikh](https://github.com/Adnan-Zhaikh)
