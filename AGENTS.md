# Project Documentation: Azure Search OpenAI Demo Java

## Overview

This is a production-ready RAG (Retrieval Augmented Generation) chat application that demonstrates best practices for creating ChatGPT-like experiences over enterprise data. The solution implements an event-driven microservices architecture using Java, React, and Azure AI services, with support for multiple deployment platforms (Azure Container Apps, Azure Kubernetes Service, and Azure App Service).

The application serves as a fictional Contoso Electronics employee portal where users can ask questions about benefits, policies, job descriptions, and roles. It features document upload management, role-based access control with EntraID, and real-time document indexing.

---

## Technical Stack

### Backend (API Service)
- **Framework**: Spring Boot 3.3.2
- **Java Version**: 17 (Microsoft JDK)
- **AI Orchestration**: Langchain4J 1.0.1
- **Key Dependencies**:
  - `azure-ai-openai` 1.0.0-beta.16 - Azure OpenAI Service integration
  - `langchain4j-azure-open-ai` - LangChain4J Azure adapter
  - `spring-cloud-azure` 5.14.0 - Azure service integrations
  - `azure-search-documents` 11.7.2 - Azure AI Search SDK
  - Spring WebFlux - Reactive/streaming support
- **Build Tool**: Maven
- **Features**:
  - Synchronous and asynchronous streaming chat endpoints
  - RAG pattern orchestration between LLM and content retrieval
  - Response tracking with citations, search keywords, and token usage
  - CosmosDB integration for chat history

### Frontend (Web Application)
- **Framework**: React 18.3.1
- **Build Tool**: Vite 5.4.18
- **Language**: TypeScript 5.6.3
- **UI Library**: Fluent UI (React Components & Icons)
- **Key Dependencies**:
  - `@azure/msal-react` 2.2.0 - Microsoft Authentication Library
  - `@azure/msal-browser` 3.26.1 - MSAL browser support
  - `react-router-dom` 6.28.0 - Routing
  - `react-markdown` 9.0.1 - Markdown rendering
  - `react-syntax-highlighter` 15.6.1 - Code highlighting
  - `i18next` 24.2.0 - Internationalization
  - `idb` 8.0.0 - IndexedDB for browser-based chat history
- **Web Server**: Nginx (reverse proxy in production)
- **Node Version**: >= 20.0.0

### Indexer Service
- **Framework**: Spring Boot 3.3.2
- **Java Version**: 17
- **AI Components**: Langchain4J 1.0.1
- **Key Dependencies**:
  - `azure-ai-documentintelligence` 4.1.4 - Document parsing
  - `azure-sdk-bom` 1.2.33 - Azure SDK management
  - `picocli` 4.7.5 - CLI interface (for CLI module)
  - `itextpdf` 5.5.13.3 - PDF processing
- **Modules**:
  - **Core**: Shared indexing logic and utilities
  - **CLI**: Command-line interface for batch indexing
  - **Microservice**: Event-driven indexing service
  - **Functions**: Azure Functions implementation for blob processing
- **Build Tool**: Maven

### Azure Services
- **Azure OpenAI Service**: GPT-4o-mini model for chat completions
- **Azure AI Search**: Vector and hybrid search for document retrieval
- **Azure Document Intelligence**: Structured text and table extraction from PDFs
- **Azure Cosmos DB**: Chat conversation history storage
- **Azure Blob Storage**: Document storage (staging and default containers)
- **Azure Service Bus**: Message queue for async indexing triggers
- **Azure Event Grid**: Real-time blob upload notifications
- **Azure Application Insights**: Monitoring and telemetry
- **Azure Container Apps / AKS / App Service**: Hosting platforms
- **Azure Key Vault**: Secrets management
- **Azure Monitor**: Observability and diagnostics
- **EntraID (Azure Active Directory)**: Authentication and authorization

### DevOps & Infrastructure
- **IaC Tool**: Bicep (Azure native)
- **Orchestration**: Azure Developer CLI (azd)
- **Containerization**: Docker
- **Container Orchestration Options**:
  - Docker Compose (local development)
  - Azure Container Apps (serverless containers)
  - Azure Kubernetes Service (full Kubernetes)
- **CI/CD**: Azure Pipelines, GitHub Actions
- **Development Environment**: Dev Containers support

### Evaluation & Testing
- **Framework**: Python-based evaluation scripts
- **Test Types**:
  - Ground truth evaluation
  - Safety evaluation
  - Response quality metrics
- **Dependencies**: Python 3.x with custom evaluation requirements

---

## Repository Structure

### Root Level Files

| File/Folder | Description |
|-------------|-------------|
| `README.md` | Main project documentation with quick start guides |
| `CHANGELOG.md` | Version history and release notes |
| `CONTRIBUTING.md` | Contribution guidelines for developers |
| `LICENSE` / `LICENSE.md` | MIT License files |
| `SECURITY.md` | Security policy and vulnerability reporting |
| `CODEOWNERS` | GitHub code ownership definitions |
| `ps-rule.yaml` | Azure PSRule configuration for infrastructure validation |

### `/app` - Application Source Code

Main application directory containing all microservices and frontend code.

#### `/app/backend`
Spring Boot API service implementing the RAG chat orchestration.

- **Key Files**:
  - `pom.xml` - Maven build configuration with dependencies
  - `Dockerfile` - Container image definition
  - `manifest.yml` - Azure deployment manifest
  - `applicationinsights.json` - Application Insights configuration
  - `mvnw`, `mvnw.cmd` - Maven wrapper scripts
- **Subdirectories**:
  - `src/main/` - Java source code (controllers, services, models)
  - `manifests/` - Kubernetes deployment manifests
    - `backend-deployment.tmpl.yml` - Backend deployment template
    - `backend-service.yml` - Kubernetes service definition
    - `ingress.yml` - Ingress configuration

##### Backend Java Package Structure (`com.microsoft.openai.samples.rag`)

The backend follows a layered architecture pattern with clear separation of concerns:

- **`Application.java`** - Spring Boot application entry point and main class

- **`approaches/`** - RAG pattern configuration and options
  - Defines retrieval strategies, search modes, and RAG types
  - Contains `RAGOptions`, `RAGResponse`, `RetrievalMode`, `RAGType` enums
  - `ContentSource` interface for content retrieval abstraction

- **`ask/`** - Single-turn question/answer implementation
  - `controller/` - REST endpoints for one-shot Q&A interactions
  - Implements simple RAG flow without conversation history

- **`chat/`** - Multi-turn conversational chat implementation
  - `langchain4j/` - Langchain4J-based chat orchestration services
  - Manages conversation context and history integration
  - Implements streaming and synchronous chat responses

- **`common/`** - Shared utilities and helper classes
  - `ChatGPTConversation` - Conversation state management
  - `ChatGPTMessage` - Message abstraction and formatting
  - `ChatGPTUtils` - Azure OpenAI API utilities
  - `ResponseMessageUtils` - Response formatting helpers

- **`config/`** - Spring configuration classes and beans
  - `AppConfigurationProperties` - Application-wide settings
  - `AppAuthConfigurationProperties` - Authentication configuration
  - `AzureAISearchConfiguration` - Azure AI Search client setup
  - `AzureAuthenticationConfiguration` - Azure SDK authentication
  - `CosmosConfiguration` - CosmosDB client configuration
  - `Langchain4JConfiguration` - Langchain4J components wiring
  - `OpenAIConfiguration` - Azure OpenAI client setup
  - `WebSecurityConfiguration` - Spring Security settings

- **`content/`** - Document content management
  - `ContentController` - REST APIs for content operations
  - `IndexService` - Azure AI Search indexing operations
  - `FilenameRequest`, `IndexClientRequest` - Request DTOs
  - Handles document upload, retrieval, and management

- **`controller/`** - Additional REST controllers
  - `auth/` - Authentication and authorization endpoints
  - `config/` - Configuration exposure endpoints

- **`history/`** - Chat conversation history management
  - `ChatHistoryService` - Business logic for history operations
  - `ChatHistoryRepository` - CosmosDB data access layer
  - `ChatHistoryItem` - Entity model for conversation persistence
  - `ChatHistoryRequest` - History query/update requests
  - `ChatHistoryItemHierarchicalPartitionKey` - CosmosDB partitioning
  - `Session`, `MessagePair` - Conversation data structures

- **`model/`** - Data Transfer Objects (DTOs) and API models
  - `ChatAppRequest` - Incoming chat request structure
  - `ChatAppResponse` - Chat response envelope
  - `ChatAppRequestOverrides` - User-configurable options
  - `ChatAppRequestContext` - Request context metadata
  - `ResponseMessage`, `ResponseChoice`, `ResponseContext` - Response components
  - `ResponseThought`, `ResponseDataPoint` - Response metadata

- **`proxy/`** - External service proxies and adapters
  - `BlobStorageProxy` - Azure Blob Storage operations wrapper
  - Abstracts storage operations for content management

- **`security/`** - Authentication and authorization services
  - `LoggedUserService` - Current user context management
  - `LoggedUser` - User principal and claims extraction
  - Integrates with EntraID/MSAL authentication

#### `/app/frontend`
React-based web application with TypeScript and Vite.

- **Key Files**:
  - `package.json` - NPM dependencies and scripts
  - `tsconfig.json` - TypeScript compiler configuration
  - `vite.config.ts` - Vite build tool configuration
  - `index.html` - HTML entry point
  - `Dockerfile` - Container image for standard deployment
  - `Dockerfile-aks` - Optimized container for AKS
- **Subdirectories**:
  - `src/` - React source code
    - `components/` - Reusable React components
    - `pages/` - Page-level components
    - `api/` - API client code
    - `assets/` - Static assets (images, fonts)
    - `locales/` - i18n translation files
    - `i18n/` - Internationalization configuration
    - `authConfig.ts` - MSAL authentication setup
    - `loginContext.tsx` - Authentication context provider
    - `index.tsx` - Application entry point
  - `public/` - Static public assets
  - `nginx/` - Nginx configuration for production
  - `manifests/` - Kubernetes manifests for frontend deployment

#### `/app/indexer`
Document indexing service with multiple deployment modes.

- **Key Files**:
  - `pom.xml` - Parent Maven POM for multi-module project
  - `mvnw`, `mvnw.cmd` - Maven wrapper scripts
- **Subdirectories**:
  - `core/` - Shared indexing logic, document processing, embedding generation
  - `cli/` - Command-line interface for batch document indexing
    - `dependency-reduced-pom.xml` - Shaded JAR configuration
  - `microservice/` - Spring Boot service for event-driven indexing
    - `Dockerfile` - Container image definition
    - `applicationinsights.json` - Monitoring configuration
  - `manifests/` - Kubernetes deployment manifests

##### Indexer Java Package Structure (`com.microsoft.openai.samples.indexer`)

The indexer follows a modular architecture with three deployment modules sharing a common core:

###### Core Module (`indexer/core`)

Shared indexing pipeline implementation used by all deployment modes:

- **`langchain4j/`** - Langchain4J-based document processing pipeline
  - **`ConfigUtils.java`** - Configuration utilities and helpers
  - **`Langchain4JIndexingPipeline.java`** - Main indexing orchestration pipeline
  - **`PagedDocument.java`** / **`DefaultPagedDocument.java`** - Document page abstraction
  - **`DocumentLoader.java`** - Document loading from various sources
  - **`PipelineContext.java`** - Execution context and metadata
  - **`IndexingMetadata.java`** - Tracking and monitoring metadata
  - **`IndexingConfigException`** / **`IndexingProcessingException`** - Custom exceptions
  
  - **`embedding/`** - Text embedding generation
    - Azure OpenAI embeddings integration
    - Batch processing and optimization
  
  - **`loader/`** - Document loaders for various sources
    - Blob storage loader
    - File system loader
    - HTTP/URL loaders
  
  - **`parser/`** - Document format parsers
    - PDF parser (Azure Document Intelligence integration)
    - HTML parser with markdown conversion
    - Office document parsers (DOCX, XLSX, PPTX)
    - Plain text and markdown parsers
    - Table extraction and preservation
  
  - **`providers/`** - Service provider implementations
    - Azure AI Search embedding store provider
    - Azure OpenAI service provider
    - Document Intelligence service provider
  
  - **`splitter/`** - Document chunking strategies
    - Recursive character text splitters
    - Sentence-based splitting
    - Token-aware splitting with overlap
    - Custom splitting strategies

- **`storage/`** - Azure Blob Storage operations
  - **`BlobManager.java`** - Blob upload/download/move operations
  - Container management and SAS token handling

###### CLI Module (`indexer/cli`)

Command-line interface for batch document indexing:

- **`cli/`** - CLI implementation package
  - **`CLI.java`** - Main CLI entry point with picocli framework
  - **`langchain4j/`** - CLI-specific Langchain4J integrations
    - **`UploadCommand.java`** - Document upload command implementation
    - Batch processing commands
    - Index management commands

###### Microservice Module (`indexer/microservice`)

Spring Boot event-driven microservice for automated indexing:

- **`service/`** - Main service package
  - **`IndexerApplication.java`** - Spring Boot application entry point
  
  - **`config/`** - Spring configuration classes
    - **`IngestionConfigurationProperties.java`** - Indexing settings
    - **`AzureAISearchEmbeddingStoreConfiguration.java`** - Search client setup
    - **`AzureBlobStorageConfiguration.java`** - Blob storage client setup
    - **`AzureAuthenticationConfiguration.java`** - Azure SDK authentication
    - **`DocumentIntelligenceConfiguration.java`** - Document Intelligence setup
    - **`OpenAIConfiguration.java`** - Azure OpenAI client configuration
    - **`ServiceBusConfig.java`** / **`ServiceBusProcessorClientConfiguration.java`** - Service Bus setup
  
  - **`controller/`** - REST API endpoints
    - **`IndexController.java`** - Indexing REST API controller
    - **`IndexingRequest.java`** - Request DTO for manual indexing triggers
    - Exposes endpoints for on-demand document indexing
  
  - **`events/`** - Event-driven message processing
    - **`BlobMessageListener.java`** - Service Bus message consumer
    - **`BlobUpsertEventGridEvent.java`** - Event Grid event model
    - **`BlobEventGridData.java`** - Blob event data extraction
    - Processes blob upload events for automatic indexing
  
  - **`langchain4j/`** - Microservice-specific Langchain4J services
    - **`Langchain4JIndexerService.java`** - Main indexing service orchestration
    - Integrates core pipeline with Spring Boot lifecycle
    - Async processing and error handling

**Indexer Architecture Flow**:
1. Document uploaded → Event Grid notification → Service Bus queue
2. `BlobMessageListener` receives message → extracts blob URL
3. `Langchain4JIndexerService` orchestrates processing
4. Core pipeline: Load → Parse (Document Intelligence) → Chunk → Embed → Index
5. Document moved from staging to default container
6. Metadata and status tracked throughout process

### `/data`
Sample documents and test data for the Contoso Electronics scenario (benefits PDFs, policies, job descriptions).

### `/deploy` - Deployment Configurations

Infrastructure-as-Code and deployment scripts for different Azure platforms.

#### `/deploy/aca` - Azure Container Apps Deployment
Serverless container platform deployment.

- **Key Files**:
  - `azure.yaml` - Azure Developer CLI configuration
  - `compose.yaml` - Docker Compose for local development
  - `start-compose.ps1` / `start-compose.sh` - Local startup scripts
- **Subdirectories**:
  - `infra/` - Bicep infrastructure templates
    - `main.bicep` - Main infrastructure definition
    - `main.parameters.json` - Environment parameters
    - `app/` - Application-specific resources
  - `scripts/` - Automation scripts
    - `auth_init.*` - EntraID authentication setup
    - `auth_update.*` - Authentication configuration updates
    - `load_azd_env.*` - Environment variable loading
    - Python and PowerShell utilities

#### `/deploy/aks` - Azure Kubernetes Service Deployment
Full Kubernetes orchestration deployment.

- **Key Files**:
  - `azure.yaml` - AKS-specific azd configuration
  - `compose.yaml` - Local Docker Compose setup
  - `ingress-tls.yml` - TLS ingress configuration
  - `start-compose.ps1` / `start-compose.sh` - Local startup scripts
- **Subdirectories**:
  - `infra/` - Bicep templates for AKS cluster and resources
  - `scripts/` - Deployment automation scripts
  - `easyauth/` - Easy Auth proxy configuration for authentication

#### `/deploy/app-service` - Azure App Service Deployment
Traditional PaaS web app deployment option.

- Contains scripts and configurations for deploying to Azure App Service

#### `/deploy/shared` - Shared Infrastructure Components
Common Bicep modules used across deployment types.

- **Key Files**:
  - `abbreviations.json` - Azure resource naming conventions
  - `abbreviations-old.json` - Legacy naming patterns
  - `backend-dashboard.bicep` - Azure Dashboard configuration
- **Subdirectories**:
  - `ai/` - Azure OpenAI and AI Search modules
  - `entra/` - EntraID authentication modules
  - `event/` - Event Grid and Service Bus modules
  - `host/` - Container hosting modules (ACA/AKS)
  - `monitor/` - Application Insights and monitoring
  - `search/` - Azure AI Search configuration
  - `security/` - Key Vault and security modules
  - `servicebus/` - Service Bus queue modules
  - `storage/` - Blob Storage modules

### `/docs` - Platform-Specific Documentation

#### `/docs/aca`
Azure Container Apps deployment guides.

- `README-ACA.md` - Complete ACA deployment documentation
- `local-development-intellij.md` - IntelliJ IDEA setup
- `login_and_acl.md` - Authentication and access control
- `evaluation.md` - Model evaluation guide
- `safety_evaluation.md` - Safety and content filtering

#### `/docs/aks`
Azure Kubernetes Service deployment guides.

- `README-AKS.md` - Complete AKS deployment documentation
- `login_and_acl.md` - Authentication and RBAC setup

#### `/docs/app-service`
Azure App Service deployment guide.

- `README-App-Service.md` - App Service deployment instructions

### `/evals` - Evaluation and Testing

Python-based evaluation framework for testing chat quality and safety.

- **Key Files**:
  - `evaluate.py` - Main evaluation script
  - `evaluate_config.json` - Evaluation configuration
  - `safety_evaluation.py` - Safety and content moderation tests
  - `generate_ground_truth.py` - Ground truth dataset generator
  - `ground_truth.jsonl` - Test dataset in JSONL format
  - `ground_truth_kg.json` - Knowledge graph ground truth
  - `safety_results.json` - Safety evaluation results
  - `requirements.txt` - Python dependencies
- **Subdirectories**:
  - `results/baseline/` - Baseline evaluation results

---

## Architecture Patterns

### RAG (Retrieval Augmented Generation) Pattern

**Chat Flow** (Backend API):
1. User query received
2. Query embedding generated (Azure OpenAI)
3. Vector/hybrid search executed (Azure AI Search)
4. Relevant document chunks retrieved
5. Context + query sent to LLM (GPT-4o-mini)
6. Response streamed back with citations

**Ingestion Flow** (Indexer Service):
1. Document uploaded to Blob Storage
2. Event Grid triggers Service Bus message
3. Indexer service processes blob URL
4. Azure Document Intelligence extracts text/tables
5. Document chunked and embedded
6. Chunks indexed in Azure AI Search
7. Document moved to default container

### Microservices Architecture

- **API Service**: Stateless REST API, horizontally scalable
- **Frontend**: Static React SPA served via Nginx
- **Indexer Service**: Event-driven worker, scales on queue depth
- **Persistence**: CosmosDB (chat history), Azure AI Search (vectors), Blob Storage (documents)
- **Communication**: Service Bus for async messaging, HTTPS for synchronous APIs

---

## Development Workflows

### Local Development
- Docker Compose for full stack local testing
- Dev Containers for consistent development environment
- Hot reload enabled for frontend (Vite) and backend (Spring Boot DevTools)

### Deployment
1. **Infrastructure Provisioning**: `azd provision` using Bicep templates
2. **Application Deployment**: `azd deploy` containerized services
3. **Authentication Setup**: Run auth scripts for EntraID configuration

### Supported Platforms
- **Azure Container Apps**: Serverless, auto-scaling, event-driven
- **Azure Kubernetes Service**: Full control, advanced networking, production-grade
- **Azure App Service**: Simple PaaS deployment for smaller workloads

---

## Key Features Implementation

### Search Strategies
- **Text Search**: Traditional full-text search with BM25 ranking
- **Vector Search**: Semantic similarity using embeddings
- **Hybrid Search**: Combined text + vector with score fusion
- **Semantic Ranking**: Azure AI Search semantic reranking

### Authentication & Authorization
- **EntraID Integration**: OAuth 2.0 / OpenID Connect via MSAL
- **Document ACLs**: Role-based access control for documents
- **Anonymous Mode**: Browser-local storage for unauthenticated users

### Monitoring & Observability
- **Application Insights**: Telemetry, traces, metrics
- **Azure Monitor**: Infrastructure monitoring
- **Logging**: Structured logging with correlation IDs

### Document Processing
- **Supported Formats**: PDF, HTML, Markdown, DOCX, XLSX, PPTX
- **Extraction Methods**: 
  - Azure Document Intelligence (tables, images, structure)
  - Langchain4J parsers (alternative)
- **Chunking**: Configurable chunk size and overlap strategies

---

## Getting Started

### Prerequisites
- Java 17+ (Microsoft JDK recommended)
- Node.js 20+
- Docker Desktop
- Azure subscription
- Azure Developer CLI (azd)

### Quick Start
```bash
# Clone repository
git clone <repository-url>

# Choose deployment target and follow guide
# For ACA: docs/aca/README-ACA.md
# For AKS: docs/aks/README-AKS.md
# For App Service: docs/app-service/README-App-Service.md

# Local development with Docker Compose
cd deploy/aca  # or deploy/aks
./start-compose.sh  # or start-compose.ps1 on Windows
```

### Build Commands
```bash
# Backend
cd app/backend
./mvnw clean package

# Frontend
cd app/frontend
npm install
npm run build

# Indexer
cd app/indexer
./mvnw clean package
```

---


**Last Updated**: February 2026  
**Version**: 1.4.0-SNAPSHOT
