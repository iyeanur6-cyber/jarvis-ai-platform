# 🗺️ Jarvis AI Platform — Roadmap

> **Last Updated:** June 2026
> **Current Status:** Phase 2 — In Progress 🔨

---

## 📊 Status Legend

| Symbol | Meaning        |
| ------ | -------------- |
| ✅      | Complete       |
| 🔨     | In Progress    |
| 📋     | Planned        |
| 💭     | Considering    |
| ❌      | Will NOT build |

---

## 🏗️ Phase 0 — Foundation ✅ Complete

All architecture, documentation, and project setup completed before writing code.

---

## 🔥 Phase 1 — AI Core Foundation ✅ Complete (v0.1.0)

**Released:** June 2026

### Infrastructure

* ✅ Spring Boot 4.0.6 + Spring AI 2.0 (M8)
* ✅ PostgreSQL 16 + pgvector + Flyway migrations
* ✅ Redis 7 for session caching
* ✅ Docker Compose (one command setup)
* ✅ Custom PostgreSQL image with pgvector

### AI Engine

* ✅ OllamaProvider (local AI, primary)
* ✅ GeminiProvider (cloud AI, fallback)
* ✅ ProviderRouter (automatic failover)
* ✅ PromptAssembler (context builder)
* ✅ WorkingMemoryBuilder (date/time/user)
* ✅ SSE token streaming

### Security

* ✅ JWT authentication (15min access + 7day refresh)
* ✅ Argon2id password hashing
* ✅ Role-based access (ADMIN / USER)
* ✅ Spring Security 7 reactive filter chain

### CLI (Spring Shell 4.0)

* ✅ Custom `jarvis:>` prompt
* ✅ First-run setup wizard
* ✅ login / logout / whoami / setup
* ✅ Interactive streaming chat
* ✅ Session management commands
* ✅ status / doctor / jarvis-version / about
* ✅ Backspace + history (JLine)

### Database (V1-V9)

* ✅ users table
* ✅ ai_providers table
* ✅ chat_sessions table
* ✅ messages table
* ✅ system_prompts table
* ✅ conversation_summaries table
* ✅ refresh_tokens table
* ✅ seed data (Ollama + Gemini providers)
* ✅ memories table (V9)

### Phase 2 Infrastructure (completed in Phase 1)

* ✅ pgvector extension (V10)
* ✅ embedding column on memories (V11)
* ✅ Redis session cache (SessionCacheService)
* ✅ EmbeddingService (nomic-embed-text)

---

## 🧠 Phase 2 — Memory System 🔨 In Progress (v0.2.0)

**Goal:** Jarvis remembers you across all sessions.

### Infrastructure ✅ Complete

* ✅ memories table with pgvector column
* ✅ pgvector 0.7.4 in Docker
* ✅ EmbeddingService (Ollama nomic-embed-text)
* ✅ Redis session caching

### Core Memory 🔨 In Progress

* 🔨 Memory entity + MemoryRepository
* 📋 MemoryService (CRUD operations)
* 📋 Memory REST API endpoints

### Memory Intelligence 📋 Planned

* 📋 Automatic extraction from conversations
* 📋 Memory injection into prompts
* 📋 pgvector semantic search
* 📋 Conversation summarization

### User Interface 📋 Planned

* 📋 CLI memory commands

---

## 📚 Phase 3 — RAG Engine 📋 Planned (v0.3.0)

**Goal:** Chat with your own documents.

* 📋 PDF, TXT, Markdown upload
* 📋 Smart chunking (300-500 tokens)
* 📋 pgvector document storage
* 📋 Semantic search on documents
* 📋 Source citation in responses
* 📋 CLI document commands
* 📋 curl installer for easier setup

---

## 🔧 Phase 4 — Tool Engine 📋 Planned (v0.4.0)

**Goal:** Jarvis takes real actions.

* 📋 @Tool annotation framework
* 📋 DateTimeTool, CalculatorTool
* 📋 WeatherTool (OpenWeatherMap)
* 📋 WebSearchTool (DuckDuckGo)
* 📋 MCP Server (@EnableMcpServer)

---

## 🎙️ Phase 5 — Voice Assistant 📋 Planned (v0.5.0)

**Goal:** Talk to Jarvis, hear Jarvis respond.

* 📋 Browser speech-to-text
* 📋 Local Whisper (Ollama)
* 📋 Text-to-speech output
* 📋 Voice conversation loop

---

## 🤖 Phase 6 — Agent System 📋 Planned (v0.6.0)

**Goal:** Jarvis plans and executes multi-step tasks.

* 📋 ReACT pattern
* 📋 AgentPlanner + AgentExecutor
* 📋 Multi-step workflow persistence
* 📋 Multi-agent collaboration

---

## 🌐 Phase 7 — Web UI 💭 Considering (v1.0.0)

* 💭 Angular 21 + Angular Material
* 💭 Real-time streaming chat UI
* 💭 Session management
* 💭 Document upload (RAG)
* 💭 Agent dashboard

---

## 🤝 How To Help

| Area                       | Phase   | Skill          |
| -------------------------- | ------- | -------------- |
| Memory entity + repository | Phase 2 | Java, R2DBC    |
| MemoryService CRUD         | Phase 2 | Java, Spring   |
| Memory REST API            | Phase 2 | Spring WebFlux |
| Memory CLI commands        | Phase 2 | Spring Shell   |
| Unit tests                 | All     | JUnit 5        |
| Documentation              | All     | Writing        |
| Architecture diagrams      | All     | Draw.io        |

---

*Last reviewed: June 2026*
