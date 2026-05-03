# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise Travel AI Agent platform built with Spring AI 1.0, implementing RAG + MCP + ReAct + **Three-Layer Memory System** for corporate travel policy Q&A and itinerary planning scenarios. The system solves weak model tool-calling limitations through a complexity assessment framework, achieving 100% tool invocation rate across all domestic LLMs.

**Tech Stack**: Spring Boot 3.4, Spring AI 1.0, Alibaba DashScope (Qwen), Java 21, Maven

**New Feature (Phase 2)**: Three-layer memory system for context retention and personalized recommendations

## Build & Run Commands

### Starting the Application

```bash
# Windows (recommended)
./run-backend.bat

# Maven directly
./mvnw spring-boot:run

# Build JAR
./mvnw clean package
java -jar target/yu-ai-agent-0.0.1-SNAPSHOT.jar
```

The backend runs on `http://localhost:8123/api`

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=RAGEvaluationTest

# Run evaluation test suite
./mvnw test -Dtest=EvaluationTestSuite

# Run specific evaluation tests
./mvnw test -Dtest=AccuracyQualityTest        # RAG accuracy tests
./mvnw test -Dtest=ComplexityFrameworkTest    # Workflow orchestration tests
./mvnw test -Dtest=PerformanceStressTest      # Performance tests
./mvnw test -Dtest=NegationQueryTest          # Negation query tests

# Memory system tests (NEW)
./mvnw test -Dtest=MemorySystemIntegrationTest  # Three-layer memory integration tests
```

### API Endpoints

```bash
# Health check
curl http://localhost:8123/api/health

# Synchronous chat
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=去上海出差住宿标准&chatId=test123"

# SSE streaming chat (recommended)
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=帮我规划明天去杭州的行程&chatId=test123"

# ReAct Agent demo
curl -N "http://localhost:8123/api/ai/manus/chat?message=查询公司到虹桥机场的距离"

# Memory System APIs (NEW)
curl http://localhost:8123/api/memory/working/test123          # Get working memory
curl http://localhost:8123/api/memory/profile/user001          # Get user profile
curl -X POST "http://localhost:8123/api/memory/learn?userId=user001&conversationId=test123"  # Trigger learning

# Swagger UI
open http://localhost:8123/api/swagger-ui.html
```

## Architecture Overview

### Core Components

**Three-Layer Memory System** ([src/main/java/com/jblmj/aiagent/chatmemory/](src/main/java/com/jblmj/aiagent/chatmemory/))
- **Layer 1 (Short-term)**: File-based chat history with sliding window (20 messages)
- **Layer 2 (Working)**: Entity extraction and intent tracking for current session
- **Layer 3 (Long-term)**: User profile learning for personalized recommendations
- See [MEMORY_SYSTEM_DESIGN.md](docs/MEMORY_SYSTEM_DESIGN.md) for detailed documentation

**WorkflowOrchestrator** ([WorkflowOrchestrator.java](src/main/java/com/jblmj/aiagent/app/WorkflowOrchestrator.java))
- Central routing engine that orchestrates all query processing
- Routing strategy: Skill-first → Complexity-based fallback
- Handles SIMPLE (single tool call), MEDIUM (multiple calls), COMPLEX (task decomposition + parallel execution)

**Skill System** ([src/main/java/com/jblmj/aiagent/skill/](src/main/java/com/jblmj/aiagent/skill/))
- User-facing task units (one task = one Skill)
- Auto-registration via `@SkillComponent` annotation
- Current skills: `WeatherQuerySkill`, `TravelPlanningSkill`
- Skills call Services (framework capabilities) and Tools (atomic operations)

**ComplexityAssessor** ([ComplexityAssessor.java](src/main/java/com/jblmj/aiagent/service/ComplexityAssessor.java))
- Hybrid approach: 80% rule-based (fast), 20% LLM-based (accurate)
- Classifies queries as SIMPLE/MEDIUM/COMPLEX
- Achieves 100% tool invocation rate by pre-orchestrating workflows

**TaskDecomposer** ([TaskDecomposer.java](src/main/java/com/jblmj/aiagent/service/TaskDecomposer.java))
- Decomposes complex queries into structured subtasks (JSON format)
- Supports task dependencies and topological sorting
- Enables parallel execution of independent tasks via CompletableFuture
- Includes cyclic dependency detection

**RAG Pipeline** ([src/main/java/com/jblmj/aiagent/rag/](src/main/java/com/jblmj/aiagent/rag/))
- Query Rewriting: Converts colloquial queries to structured searches, handles negation queries
- Negation Detection: Detects negative keywords (不能、不是、没有) and rewrites queries to preserve negation semantics
- Metadata Enrichment: Pre-annotates documents with city tier, expense types
- Uses in-memory SimpleVectorStore (PgVector support commented out)
- Achieves 80% accuracy (40% improvement over baseline)

**EnterpriseAssistantApp** ([EnterpriseAssistantApp.java](src/main/java/com/jblmj/aiagent/app/EnterpriseAssistantApp.java))
- Main RAG-based chat application
- Handles travel policy queries via vector retrieval
- Supports SSE streaming responses

### Layer Architecture

```
Skill Layer (user tasks)
  ↓ calls
Service Layer (framework capabilities: ComplexityAssessor, TaskDecomposer)
  ↓ calls
Tool Layer (atomic operations: WeatherQueryTool, CLI tools, MCP clients)
```

**Important**: ComplexityAssessor and TaskDecomposer are Services, NOT Skills. Skills are user-facing tasks like "query weather" or "plan trip".

## Configuration

### Required API Keys

Edit [src/main/resources/application.yml](src/main/resources/application.yml):

```yaml
spring:
  ai:
    dashscope:
      api-key: YOUR_DASHSCOPE_API_KEY  # Get from https://dashscope.aliyun.com/

qweather:
  api-key: YOUR_QWEATHER_API_KEY      # Get from https://dev.qweather.com/
```

### Model Configuration

Default model: `qwen-max-2025-07-28`

To change models, update `application.yml`:
```yaml
spring:
  ai:
    dashscope:
      chat:
        options:
          model: qwen-max-2025-07-28  # or qwen-turbo, qwen-max
```

### Vector Store

Currently uses in-memory `SimpleVectorStore`. PgVector support is available but commented out in [pom.xml](pom.xml) and [application.yml](src/main/resources/application.yml).

To enable PgVector:
1. Uncomment PostgreSQL dependencies in `pom.xml`
2. Uncomment datasource config in `application.yml`
3. Switch vector store bean in `LoveAppVectorStoreConfig.java`

## Development Guidelines

### Adding a New Skill

1. Create class implementing `Skill` interface
2. Annotate with `@SkillComponent(name, description, keywords)`
3. Implement `canHandle()` for keyword matching
4. Implement `execute()` with business logic
5. Skills auto-register on startup via `SkillRegistry`

Example:
```java
@SkillComponent(
    name = "hotel_booking",
    description = "Book hotels for business travel",
    keywords = {"酒店", "预订", "住宿"}
)
public class HotelBookingSkill implements Skill {
    public String execute(String query, String chatId) {
        // Call services and tools
    }
}
```

### Adding a New Tool

Tools are atomic operations (API calls, CLI commands, database queries):

1. Create class annotated with `@Component`
2. Implement single-purpose method
3. Tools are called by Skills or Services

Example: [WeatherQueryTool.java](src/main/java/com/jblmj/aiagent/tools/WeatherQueryTool.java)

### Working with RAG Documents

Documents are in [src/main/resources/document/](src/main/resources/document/):
- `TravelPolicy.md` - Corporate travel policies
- `CustomerList.md` - Customer information
- `PreferredHotels.md` - Hotel recommendations
- `Transportation_Guide.md` - Transportation guidelines

To update RAG knowledge base:
1. Modify markdown files in `document/` directory
2. Restart application (vector store rebuilds on startup)
3. Test with relevant queries

### Task Decomposition

When adding new task types to `TaskDecomposer`:

1. Add task type to prompt in `buildDecomposePrompt()`
2. Add case handler in `executeSubTask()` switch statement
3. Ensure task parameters are JSON-serializable
4. Test with complex multi-intent queries

### Complexity Assessment

To adjust complexity thresholds in `ComplexityAssessor`:

- SIMPLE: Single intent, single tool call (e.g., "北京天气")
- MEDIUM: Single intent, multiple tool calls (e.g., "上海vs广州天气对比")
- COMPLEX: Multiple intents requiring decomposition (e.g., "去深圳出差，查天气和推荐酒店")

Modify keyword counting logic in `assessByRule()` method.

## Testing Strategy

### Evaluation Tests

Located in [src/test/java/com/jblmj/aiagent/evaluation/](src/test/java/com/jblmj/aiagent/evaluation/):

- `RAGEvaluationTest` - 25 travel policy Q&A test cases
- `ComplexityFrameworkTest` - 5 weather query test cases validating tool invocation
- `PerformanceStressTest` - Latency and throughput benchmarks
- `SystemIntegrationTest` - End-to-end integration tests

Test data in [src/test/resources/evaluation/](src/test/resources/evaluation/)

### Running Evaluations

```bash
# Full evaluation suite
./mvnw test -Dtest=EvaluationTestSuite

# Individual evaluations
./mvnw test -Dtest=RAGEvaluationTest
./mvnw test -Dtest=ComplexityFrameworkTest
```

Results are logged with metrics:
- RAG accuracy rate
- Tool invocation rate (target: 100%)
- Complexity assessment accuracy
- Average response latency

## Key Design Decisions

### Why Skill-First Routing?

Skills provide stable, predictable behavior for common tasks. Complexity assessment is a fallback for queries that don't match any Skill pattern. This hybrid approach balances flexibility (LLM decision-making) with reliability (pre-orchestrated workflows).

### Why Hybrid Complexity Assessment?

Rule-based assessment is fast (<1ms) but less accurate. LLM-based assessment is accurate but slow (1-2s). The hybrid approach uses rules for 80% of cases and LLM confirmation only for COMPLEX queries, achieving 90% accuracy with <500ms latency.

### Why Task Dependencies?

Complex queries like "查询客户地址并规划路线" require sequential execution (must get address before planning route). The dependency system enables topological sorting and parallel execution of independent tasks while respecting dependencies.

### Why Not Full LLM Tool Calling?

Weak models (Qwen, domestic LLMs) have poor tool-calling reliability when multiple tools are registered. The complexity framework achieves 100% tool invocation by using code-controlled workflows instead of relying on LLM decision-making.

## Common Issues

### Application Won't Start

- Check JDK version: Requires JDK 21 (or 17 minimum)
- Verify API keys in `application.yml`
- If using PgVector, ensure PostgreSQL is running

### RAG Returns Incorrect Results

- Check if query needs rewriting (colloquial → structured)
- Verify document metadata in `document/*.md` files
- Increase context window in RAG retrieval (default: top 5 chunks)

### Tool Not Being Called

- Check if query matches Skill keywords in `canHandle()`
- Verify complexity assessment classifies correctly
- Check tool registration in Spring context

### Tests Failing

- Ensure API keys are configured
- Check network connectivity for external API calls (weather, LLM)
- Some tests require specific model responses - may need adjustment for different models

## Project Structure

```
src/main/java/com/jblmj/aiagent/
├── app/                    # Main applications
│   ├── WorkflowOrchestrator.java    # Central routing engine
│   └── EnterpriseAssistantApp.java  # RAG chat application
├── skill/                  # Skill system
│   ├── Skill.java                   # Skill interface
│   ├── SkillRegistry.java           # Auto-registration
│   └── business/                    # Business skills
├── service/                # Framework services
│   ├── ComplexityAssessor.java      # Query complexity evaluation
│   └── TaskDecomposer.java          # Task decomposition
├── tools/                  # Atomic tools
│   └── WeatherQueryTool.java        # Weather API integration
├── rag/                    # RAG pipeline
│   ├── QueryRewriter.java           # Query rewriting
│   └── MyKeywordEnricher.java       # Metadata enrichment
├── agent/                  # Agent implementations
│   ├── ReActAgent.java              # ReAct pattern agent
│   └── JblmjManus.java              # Custom agent
├── controller/             # REST controllers
├── model/                  # Data models
└── config/                 # Configuration

src/main/resources/
├── document/               # RAG knowledge base (markdown)
├── application.yml         # Main configuration
└── mcp-servers.json        # MCP server configuration

src/test/java/com/jblmj/aiagent/
└── evaluation/             # Evaluation test suite
    ├── RAGEvaluationTest.java       # 30 test cases (including negation queries)
    ├── NegationQueryTest.java       # Negation query specific tests
    ├── ComplexityFrameworkTest.java # Workflow orchestration tests
    └── PerformanceStressTest.java   # Performance benchmarks

docs/
├── RAG_INTERVIEW_QA.md              # Interview Q&A for RAG issues
├── EMBEDDING_MODELS_COMPARISON.md   # Detailed embedding model comparison
└── RAG_INTERVIEW_CHEATSHEET.md      # Quick reference for interviews
```

## Performance Characteristics

Based on evaluation results with Qwen-Plus model:

- RAG accuracy: 80% (40% improvement over baseline)
- Tool invocation rate: 100% (vs 0% with pure LLM tool calling)
- Average latency: 7.5s (Full RAG), 9.4s (with tool calls)
- Bottleneck: LLM API calls (75% of total latency)

Optimization opportunities:
- Prompt compression
- Model downgrade for simple tasks (qwen-turbo)
- Parallel tool calls where possible
