# Ikasan Dashboard - Architecture Overview

## Introduction

The Ikasan Dashboard is a unified web-based management and monitoring platform that serves two primary integration solutions:

1. **Ikasan ESB (Enterprise Service Bus)** - Traditional integration platform for module, flow, and component management
2. **Ikasan Scheduler** - Job orchestration and scheduling platform for complex workflow management

Built on **Spring Boot** and **Vaadin Flow**, the dashboard provides real-time monitoring, configuration management, and operational control through a responsive web interface.

---

## High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Web Browser]
    end

    subgraph "Presentation Layer - Vaadin Flow"
        UI[Dashboard UI<br/>Vaadin Components]
        Login[Login View]
        ESBViews[ESB Views]
        SchedulerViews[Scheduler Views]
        AdminViews[Admin Views]
    end

    subgraph "Application Layer - Spring Boot"
        DashboardApp[Dashboard Application<br/>@SpringBootApplication]
        Security[Spring Security]
        RestAPI[REST Controllers]
    end

    subgraph "Service Layer"
        ESBServices[ESB Services<br/>Module/Flow Management]
        SchedulerServices[Scheduler Services<br/>Job Orchestration]
        CommonServices[Common Services<br/>User/Auth/Config]
    end

    subgraph "Data Access Layer"
        SolrDAO[Solr DAOs<br/>Search & Analytics]
        JPADAO[JPA DAOs<br/>Metadata Storage]
    end

    subgraph "External Systems"
        ESBModules[Ikasan ESB<br/>Integration Modules]
        SchedulerAgents[Scheduler Agents<br/>Job Execution]
        Solr[Apache Solr<br/>Search Engine]
        DB[(RDBMS<br/>PostgreSQL/H2)]
    end

    Browser --> UI
    UI --> Login
    UI --> ESBViews
    UI --> SchedulerViews
    UI --> AdminViews

    ESBViews --> DashboardApp
    SchedulerViews --> DashboardApp
    AdminViews --> DashboardApp

    DashboardApp --> Security
    DashboardApp --> RestAPI

    RestAPI --> ESBServices
    RestAPI --> SchedulerServices
    RestAPI --> CommonServices

    ESBServices --> SolrDAO
    SchedulerServices --> SolrDAO
    CommonServices --> JPADAO

    SolrDAO --> Solr
    JPADAO --> DB

    ESBServices -.->|REST API| ESBModules
    SchedulerServices -.->|REST API| SchedulerAgents
```

---

## Core Components

### 1. Presentation Layer (Vaadin Flow)

The dashboard uses **Vaadin Flow** for server-side UI rendering with the following key views:

#### Main Layout
- **`IkasanAppLayout`** - Main application shell with navigation

#### ESB Views
- **`GraphView`** - Interactive module and flow topology visualization
- **`ModuleVisualisation`** - Real-time module state and component visualization
- **`BusinessStreamDesignerView`** - Visual business stream composition
- **`MapView`** - Geographic topology mapping
- **`SearchView`** - Wiretap, error, and replay event search
- **Module Control** - Start/stop/pause flows and components
- **Configuration Management** - Dynamic component configuration
- **Replay Management** - Event replay and resubmission
- **Exclusion Management** - Error exclusion handling
- **Wiretap Viewer** - Message payload inspection

#### Scheduler Views
- **`SchedulerView`** - Main scheduler dashboard
- **`ContextTemplateManagementView`** - Job plan template management
- **Context Instance Widget** - Real-time job execution monitoring
- **Context Instance Tree View** - Hierarchical job visualization
- **Scheduler Visualisation** - DAG (Directed Acyclic Graph) visualization
- **Job Plan Editor** - Visual job dependency editor

#### Administration Views
- **`UserDirectoriesView`** - LDAP/user management
- **`RoleManagementView`** - Role-based access control
- **`JobLockManagementView`** - Distributed lock management
- **`SystemEventSearchView`** - System audit logs
- **`DashboardSessionsView`** - Active session management

#### Security
- **`LoginView`** - Authentication interface

---

### 2. Application Layer (Spring Boot)

```mermaid
graph LR
    subgraph "Spring Boot Application"
        App[Application.java<br/>@SpringBootApplication]
        Security[Security Config<br/>Spring Security]
        WebConfig[Web Config<br/>CORS/Sessions]
        SchedulerConfig[Scheduler Config]
    end

    subgraph "REST API Controllers"
        ESBControllers[ESB Controllers<br/>Module/Flow/Component]
        JobControllers[Job Orchestration Controllers<br/>Context/Job Management]
        EventControllers[Event Controllers<br/>Process Events]
    end

    App --> Security
    App --> WebConfig
    App --> SchedulerConfig

    App --> ESBControllers
    App --> JobControllers
    App --> EventControllers
```

**Key Configuration:**
- Spring Boot auto-configuration
- Embedded Tomcat server
- Session management
- CORS configuration
- Security filters

---

### 3. Service Layer Architecture

```mermaid
graph TB
    subgraph "ESB Services"
        ModuleService[Module Service]
        FlowService[Flow Service]
        ComponentService[Component Service]
        TopologyService[Topology Service]
    end

    subgraph "Scheduler Services"
        ContextService[Context Instance Service<br/>Job Plan Execution]
        TemplateService[Context Template Service<br/>Job Plan Templates]
        JobService[Scheduler Job Service<br/>Job Management]
        EventService[Process Event Service<br/>Event Handling]
        LockService[Job Lock Service<br/>Distributed Locking]
    end

    subgraph "Common Services"
        UserService[User Service]
        AuthService[Authentication Service]
        ConfigService[Configuration Service]
        AuditService[Audit Service]
    end
```

#### Scheduler Service Components

**Context Management:**
- **Context Templates** - Job plan definitions (blueprints)
- **Context Instances** - Runtime job plan executions
- **Context Parameters** - Dynamic configuration injection

**Job Orchestration:**
- Job dependency management
- Lock group coordination
- State machine transitions
- Event-driven execution

---

### 4. Data Access Layer

The dashboard uses a hybrid persistence strategy:

```mermaid
graph TB
    subgraph "Solr-based DAOs"
        SchedulerJobDAO[SolrSchedulerJobInstanceDao<br/>Job execution records]
        ContextDAO[SolrScheduledContextInstanceDao<br/>Job plan instances]
        AuditDAO[SolrScheduledContextInstanceAuditDao<br/>Execution audit trail]
        AggregateDAO[SolrScheduledContextInstanceAuditAggregateDao<br/>Aggregated metrics]
        EventDAO[SolrScheduledProcessEventDao<br/>Process events]
    end

    subgraph "JPA-based DAOs"
        MetadataDAO[Metadata DAOs<br/>Configuration data]
        UserDAO[User DAOs<br/>Security data]
    end

    subgraph "Storage"
        Solr[Apache Solr<br/>Time-series & Search]
        DB[(RDBMS<br/>Transactional Data)]
    end

    SchedulerJobDAO --> Solr
    ContextDAO --> Solr
    AuditDAO --> Solr
    AggregateDAO --> Solr
    EventDAO --> Solr

    MetadataDAO --> DB
    UserDAO --> DB
```

#### Why Solr for Scheduler Data?

1. **Time-series optimization** - Excellent for job execution history
2. **Full-text search** - Fast filtering and querying
3. **Faceted search** - Status aggregations and counts
4. **Scalability** - Handles millions of job records
5. **Real-time indexing** - Near-instant query updates

---

## ESB Architecture Deep Dive

### Integration Module Model

The Ikasan ESB organizes integration logic into a hierarchical structure:

```mermaid
graph TB
    subgraph "Module - Integration Application"
        Module[Module<br/>e.g., 'OrderProcessing']

        subgraph "Flow - Integration Flow"
            Flow1[Flow: 'InboundOrders']
            Flow2[Flow: 'OutboundShipments']
        end

        subgraph "Components - Integration Components"
            Consumer[Consumer<br/>JMS/FTP/HTTP]
            Converter[Converter<br/>Transform Data]
            Broker[Broker<br/>Route/Split]
            Filter[Filter<br/>Conditional Logic]
            Producer[Producer<br/>Deliver Message]
        end
    end

    Module --> Flow1
    Module --> Flow2

    Flow1 --> Consumer
    Consumer --> Converter
    Converter --> Broker
    Broker --> Filter
    Filter --> Producer
```

### Module, Flow, and Component Hierarchy

```mermaid
graph LR
    subgraph "ESB Topology"
        M1[Module 1<br/>'CRM-Integration']
        M2[Module 2<br/>'ERP-Integration']

        F1[Flow: 'Customer-Sync']
        F2[Flow: 'Order-Import']
        F3[Flow: 'Inventory-Update']

        C1[JMS Consumer]
        C2[XML Converter]
        C3[Routing Broker]
        C4[HTTP Producer]
    end

    M1 --> F1
    M1 --> F2
    M2 --> F3

    F1 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> C4
```

### ESB Component Types

```mermaid
graph TB
    subgraph "Component Categories"
        Consumers[Consumers<br/>Entry Points]
        Transformers[Transformers<br/>Data Manipulation]
        Routers[Routers<br/>Flow Control]
        Producers[Producers<br/>Exit Points]
    end

    subgraph "Consumer Types"
        JMSConsumer[JMS Consumer]
        FTPConsumer[FTP Consumer]
        HTTPConsumer[HTTP Consumer]
        ScheduledConsumer[Scheduled Consumer]
        EmailConsumer[Email Consumer]
        SFTPConsumer[SFTP Consumer]
    end

    subgraph "Transformer Types"
        XSLTConverter[XSLT Converter]
        JSONConverter[JSON Converter]
        XMLConverter[XML Converter]
        JavaConverter[Java Converter]
    end

    subgraph "Producer Types"
        JMSProducer[JMS Producer]
        FTPProducer[FTP Producer]
        HTTPProducer[HTTP Producer]
        EmailProducer[Email Producer]
        DBProducer[Database Producer]
    end

    Consumers --> JMSConsumer
    Consumers --> FTPConsumer
    Consumers --> HTTPConsumer
    Consumers --> ScheduledConsumer
    Consumers --> EmailConsumer
    Consumers --> SFTPConsumer

    Transformers --> XSLTConverter
    Transformers --> JSONConverter
    Transformers --> XMLConverter
    Transformers --> JavaConverter

    Producers --> JMSProducer
    Producers --> FTPProducer
    Producers --> HTTPProducer
    Producers --> EmailProducer
    Producers --> DBProducer
```

### Flow State Management

```mermaid
stateDiagram-v2
    [*] --> Stopped
    Stopped --> Starting: start()
    Starting --> Running: initialization complete
    Starting --> Stopped: initialization failed

    Running --> Paused: pause()
    Running --> Stopping: stop()
    Running --> Recovering: error occurred

    Paused --> Running: resume()
    Paused --> Stopping: stop()

    Recovering --> Running: recovery successful
    Recovering --> Stopped: recovery failed

    Stopping --> Stopped: cleanup complete

    Stopped --> [*]
```

**Flow States:**
- **Stopped** - Flow is not running, no message processing
- **Starting** - Flow is initializing components
- **Running** - Flow is actively processing messages
- **Paused** - Flow is suspended, messages queued
- **Stopping** - Flow is shutting down gracefully
- **Recovering** - Flow is in error recovery mode
- **Stale** - Flow heartbeat lost (module unreachable)

### Real-time Flow Monitoring

The dashboard provides real-time flow state updates via broadcasters:

```mermaid
sequenceDiagram
    participant Module as ESB Module
    participant Heartbeat as Heartbeat Endpoint
    participant Cache as Flow State Cache
    participant Broadcaster as Flow State Broadcaster
    participant UI as Dashboard UI

    Module->>Heartbeat: POST /heartbeat<br/>{module, flows[], state}
    Heartbeat->>Cache: update(flowState)
    Cache->>Broadcaster: broadcast(flowState)
    Broadcaster->>UI: push update
    UI->>UI: update visualization

    Note over Module,Heartbeat: Every 5 seconds
    Note over UI: Real-time color coding<br/>Green=Running, Red=Stopped
```

### ESB Data Flow

```mermaid
graph LR
    subgraph "Message Flow"
        Source[External Source<br/>JMS/FTP/HTTP]
        Consumer[Consumer Component]
        Wiretap[Wiretap Listener<br/>Capture Message]
        Transform[Converter/Broker]
        Error[Error Handler]
        Exclusion[Exclusion Service<br/>Store Errors]
        Producer[Producer Component]
        Dest[External Destination]
    end

    subgraph "Dashboard Storage"
        SolrWiretap[Solr: Wiretap Events]
        SolrError[Solr: Error Events]
        SolrReplay[Solr: Replay Events]
        SolrExclusion[Solr: Exclusions]
    end

    Source --> Consumer
    Consumer --> Wiretap
    Wiretap --> Transform
    Transform --> Producer
    Producer --> Dest

    Transform -.->|error| Error
    Error --> Exclusion

    Wiretap -.->|index| SolrWiretap
    Error -.->|index| SolrError
    Exclusion -.->|store| SolrExclusion
```

### Module Control Operations

The dashboard provides full operational control over ESB modules:

```mermaid
graph TB
    subgraph "Dashboard Control Panel"
        Start[Start Flow]
        Stop[Stop Flow]
        Pause[Pause Flow]
        Resume[Resume Flow]
        StartComponent[Start Component]
        StopComponent[Stop Component]
    end

    subgraph "Module REST API"
        ModuleControl[Module Control Service]
        FlowControl[Flow Control]
        ComponentControl[Component Control]
    end

    subgraph "ESB Module Runtime"
        FlowRuntime[Flow Runtime]
        ComponentRuntime[Component Lifecycle]
    end

    Start --> ModuleControl
    Stop --> ModuleControl
    Pause --> ModuleControl
    Resume --> ModuleControl

    ModuleControl --> FlowControl
    FlowControl --> FlowRuntime

    StartComponent --> ComponentControl
    StopComponent --> ComponentControl
    ComponentControl --> ComponentRuntime
```

### Configuration Management

```mermaid
graph TB
    subgraph "Dashboard Configuration UI"
        ConfigView[Configuration View]
        Params[Parameter Editor]
        Validate[Validation]
    end

    subgraph "Configuration Service"
        ConfigREST[Configuration REST API]
        ConfigStore[Configuration Store]
    end

    subgraph "ESB Module"
        ComponentConfig[Component Configuration]
        Runtime[Runtime Application]
    end

    ConfigView --> Params
    Params --> Validate
    Validate --> ConfigREST
    ConfigREST --> ConfigStore
    ConfigStore -.->|pull on startup| ComponentConfig
    ComponentConfig --> Runtime
```

### Business Stream Visualization

Business Streams provide high-level integration topology visualization:

```mermaid
graph LR
    subgraph "Business Stream: Order-to-Cash"
        CRM[CRM System]
        OrderModule[Order Processing<br/>Module]
        BillingModule[Billing<br/>Module]
        ShippingModule[Shipping<br/>Module]
        ERP[ERP System]
    end

    CRM -->|Customer Orders| OrderModule
    OrderModule -->|Invoice Request| BillingModule
    OrderModule -->|Fulfillment| ShippingModule
    BillingModule -->|Financial Data| ERP
    ShippingModule -->|Shipment Status| ERP
```

### Wiretap and Replay

```mermaid
sequenceDiagram
    participant Consumer
    participant Wiretap as Wiretap Service
    participant Solr
    participant Dashboard
    participant User
    participant Replay as Replay Service

    Consumer->>Wiretap: capture(message, componentId)
    Wiretap->>Solr: index(wiretapEvent)

    User->>Dashboard: search wiretaps
    Dashboard->>Solr: query(filters)
    Solr->>Dashboard: return events
    Dashboard->>User: display messages

    User->>Dashboard: replay event
    Dashboard->>Replay: submit(eventId)
    Replay->>Consumer: re-inject message
    Consumer->>Wiretap: capture(replayed message)
```

**Wiretap Features:**
- **Message capture** - Capture payloads at any component
- **Payload inspection** - View XML/JSON/Text messages
- **Metadata** - Timestamps, component IDs, correlation IDs
- **Search** - Filter by time, module, flow, component, payload content
- **TTL management** - Configurable retention periods

**Replay Features:**
- **Event resubmission** - Replay failed or successful messages
- **Bulk replay** - Replay multiple events
- **Time-based replay** - Replay events from time range
- **Selective replay** - Replay specific exclusions

### Error Handling and Exclusions

```mermaid
graph TB
    subgraph "Error Flow"
        Component[Component Processing]
        Error[Error Occurs]
        ExclusionService[Exclusion Service]
        ExclusionDB[(Exclusion Database)]
    end

    subgraph "Dashboard Actions"
        View[View Exclusions]
        Resubmit[Resubmit to Flow]
        Ignore[Mark as Ignored]
        Comment[Add Comment]
    end

    Component -->|exception| Error
    Error --> ExclusionService
    ExclusionService --> ExclusionDB

    ExclusionDB --> View
    View --> Resubmit
    View --> Ignore
    View --> Comment

    Resubmit -.->|reinject| Component
```

---

## Scheduler Architecture Deep Dive

### Job Orchestration Model

```mermaid
graph TB
    subgraph "Context Template (Blueprint)"
        Template[Context Template]
        JobDef1[Job Definition 1]
        JobDef2[Job Definition 2]
        JobDef3[Job Definition 3]
        Dependencies[Dependency Rules<br/>AND/OR Logic]

        Template --> JobDef1
        Template --> JobDef2
        Template --> JobDef3
        Template --> Dependencies
    end

    subgraph "Context Instance (Runtime)"
        Instance[Context Instance]
        Job1[Job Instance 1<br/>Status: COMPLETE]
        Job2[Job Instance 2<br/>Status: RUNNING]
        Job3[Job Instance 3<br/>Status: WAITING]
        StateMachine[Context State Machine]

        Instance --> Job1
        Instance --> Job2
        Instance --> Job3
        Instance --> StateMachine
    end

    Template -.->|instantiate| Instance
```

### Job Types

The scheduler supports multiple job types:

```mermaid
graph LR
    subgraph "Job Types"
        Internal[Internal Event Driven<br/>Event-based triggers]
        File[File Event Driven<br/>File system monitors]
        Quartz[Quartz Schedule Driven<br/>Cron-based scheduling]
        Global[Global Event<br/>Broadcast events]
        Local[Local Event<br/>Context-scoped events]
        Start[Context Start<br/>Initialization jobs]
        Terminal[Context Terminal<br/>Cleanup jobs]
        Bridging[Bridging<br/>Cross-context jobs]
    end
```

### Job State Machine

```mermaid
stateDiagram-v2
    [*] --> WAITING
    WAITING --> RUNNING: Execute
    WAITING --> ON_HOLD: Hold
    WAITING --> SKIPPED: Skip
    WAITING --> LOCK_QUEUED: Lock Unavailable

    LOCK_QUEUED --> WAITING: Lock Available
    LOCK_QUEUED --> ON_HOLD: Hold

    ON_HOLD --> WAITING: Release

    RUNNING --> COMPLETE: Success
    RUNNING --> ERROR: Failure

    SKIPPED --> SKIPPED_RUNNING: Unskip During Execution
    SKIPPED --> SKIPPED_COMPLETE: Already Skipped

    ERROR --> WAITING: Reset
    COMPLETE --> [*]
    ERROR --> [*]
```

### Lock Group Management

```mermaid
graph TB
    subgraph "Lock Group: DB_MAINTENANCE"
        Lock1[Lock Instance]
        Queue[Job Queue<br/>FIFO]
    end

    subgraph "Jobs Requiring Lock"
        Job1[Backup Job<br/>Status: RUNNING<br/>Has Lock]
        Job2[Archive Job<br/>Status: LOCK_QUEUED<br/>In Queue]
        Job3[Cleanup Job<br/>Status: LOCK_QUEUED<br/>In Queue]
    end

    Job1 -->|holds| Lock1
    Job2 -->|waiting| Queue
    Job3 -->|waiting| Queue

    Queue -.->|next in line| Job2
```

**Lock Group Features:**
- **Exclusive locks** - Only one job executes at a time
- **Shared locks** - Multiple jobs can execute concurrently
- **Queue management** - FIFO ordering for waiting jobs
- **Distributed coordination** - Cross-context lock support
- **Held job handling** - Jobs ON_HOLD remain held even when locks available (IKASAN-2694)

---

## Data Flow Architecture

### Scheduler Job Execution Flow

```mermaid
sequenceDiagram
    participant Agent as Scheduler Agent
    participant REST as Dashboard REST API
    participant Service as Context Service
    participant StateMachine as State Machine
    participant Solr as Solr Index
    participant UI as Dashboard UI

    Agent->>REST: POST /scheduledProcessEvent<br/>{job completion}
    REST->>Service: processEvent(event)
    Service->>StateMachine: transition(job, event)
    StateMachine->>StateMachine: evaluate dependencies
    StateMachine->>Service: raise next events
    Service->>Solr: save(jobInstance)
    Service->>Solr: save(auditRecord)
    Service->>REST: return status
    REST->>Agent: 200 OK

    UI->>REST: GET /contextInstance/{id}
    REST->>Service: getContextInstance(id)
    Service->>Solr: query(id)
    Solr->>Service: return instance
    Service->>REST: return DTO
    REST->>UI: render visualization
```

### Real-time Job Monitoring

```mermaid
graph LR
    subgraph "Data Collection"
        Agents[Scheduler Agents<br/>Job Execution]
        Events[Process Events<br/>Job Status Updates]
    end

    subgraph "Ingestion"
        REST[REST Endpoints]
        Queue[Event Queue<br/>BigQueue]
    end

    subgraph "Processing"
        StateMachine[Context State Machine]
        Logic[Job Logic Engine]
    end

    subgraph "Storage"
        SolrJobs[Job Instances<br/>Solr Collection]
        SolrAudit[Audit Records<br/>Solr Collection]
    end

    subgraph "Visualization"
        Dashboard[Dashboard UI]
        DAG[DAG Visualization]
        Gantt[Timeline View]
    end

    Agents --> Events
    Events --> REST
    REST --> Queue
    Queue --> StateMachine
    StateMachine --> Logic
    Logic --> SolrJobs
    Logic --> SolrAudit

    SolrJobs --> Dashboard
    SolrAudit --> DAG
    SolrAudit --> Gantt
```

---

## Solr Data Model

### Core Collections

#### 1. Scheduler Job Instance Collection

**Document Structure:**
```json
{
  "id": "jobName_contextInstanceId_childContextName_INTERNAL_EVENT_DRIVEN_JOB_INSTANCE",
  "type": "INTERNAL_EVENT_DRIVEN_JOB_INSTANCE",
  "payload_content": "{...serialized job...}",
  "status": "COMPLETE",
  "module_name": "jobName",
  "flow_name": "contextName",
  "child_context_name": "childContextName",
  "component_name": "contextInstanceId",
  "created_date_time": 1709683200000,
  "updated_date_time": 1709683300000,
  "start_time": 1709683200000,
  "end_time": 1709683300000,
  "target_residing_context_only": false,
  "participates_in_lock": true,
  "modified_by": "admin",
  "manually_submitted_by": null
}
```

**Query Patterns:**
- By context instance ID → all jobs in a job plan execution
- By status → filter jobs by WAITING/RUNNING/COMPLETE/ERROR
- By time windows → historical job execution queries
- By lock participation → identify locked jobs

#### 2. Context Instance Collection

**Document Structure:**
```json
{
  "id": "contextInstanceId_scheduledContextInstance",
  "type": "scheduledContextInstance",
  "payload_content": "{...serialized context...}",
  "status": "RUNNING",
  "module_name": "contextName",
  "component_name": "contextInstanceId",
  "created_date_time": 1709683200000,
  "updated_date_time": 1709683300000,
  "start_time": 1709683200000,
  "end_time": null,
  "contains_repeating_jobs": true
}
```

#### 3. Audit Aggregate Collection

**Document Structure:**
```json
{
  "id": "scheduledContextInstanceAuditAggregateId_UUID",
  "type": "scheduledContextInstanceAuditAggregate",
  "payload_content": "{...audit data...}",
  "flow_name": "contextInstanceId",
  "module_name": "contextName",
  "component_name": "scheduledProcessEventName",
  "event": "jobName1 jobName2",
  "is_repeating_job": true,
  "status": "COMPLETE",
  "job_type": "INTERNAL_EVENT_DRIVEN"
}
```

**Aggregation Queries:**
- Repeating job status counts
- Job type distribution
- Execution frequency analysis

---

## Security Architecture

```mermaid
graph TB
    subgraph "Authentication"
        LoginForm[Login Form]
        LDAP[LDAP Provider]
        Local[Local DB Auth]
    end

    subgraph "Authorization"
        Roles[Role Management<br/>ADMIN/USER/READONLY]
        Permissions[Permissions<br/>ESB/Scheduler/Admin]
        RBAC[Spring Security RBAC]
    end

    subgraph "Session Management"
        Sessions[Active Sessions]
        Timeout[Session Timeout]
        Concurrent[Concurrent Session Control]
    end

    LoginForm --> LDAP
    LoginForm --> Local

    LDAP --> Roles
    Local --> Roles

    Roles --> Permissions
    Permissions --> RBAC

    RBAC --> Sessions
    Sessions --> Timeout
    Sessions --> Concurrent
```

**Security Features:**
- Spring Security integration
- LDAP/AD authentication support
- Role-based access control (RBAC)
- Session management and monitoring
- CORS configuration for REST APIs
- Secure REST endpoints

---

## Deployment Architecture

```mermaid
graph TB
    subgraph "Production Deployment"
        LB[Load Balancer]

        subgraph "Dashboard Cluster"
            D1[Dashboard Instance 1<br/>Spring Boot + Vaadin]
            D2[Dashboard Instance 2<br/>Spring Boot + Vaadin]
        end

        subgraph "Search Cluster"
            S1[Solr Node 1]
            S2[Solr Node 2]
            S3[Solr Node 3]
        end

        subgraph "Database"
            DB[(PostgreSQL<br/>High Availability)]
        end

        subgraph "ESB Environment"
            ESB1[ESB Module 1]
            ESB2[ESB Module 2]
        end

        subgraph "Scheduler Environment"
            Agent1[Scheduler Agent 1]
            Agent2[Scheduler Agent 2]
            Agent3[Scheduler Agent 3]
        end
    end

    LB --> D1
    LB --> D2

    D1 --> S1
    D1 --> S2
    D1 --> S3

    D2 --> S1
    D2 --> S2
    D2 --> S3

    D1 --> DB
    D2 --> DB

    ESB1 -.->|REST| D1
    ESB1 -.->|REST| D2
    ESB2 -.->|REST| D1
    ESB2 -.->|REST| D2

    Agent1 -.->|REST| D1
    Agent1 -.->|REST| D2
    Agent2 -.->|REST| D1
    Agent2 -.->|REST| D2
    Agent3 -.->|REST| D1
    Agent3 -.->|REST| D2
```

**Deployment Characteristics:**
- Horizontally scalable dashboard instances
- Solr SolrCloud for distributed search
- High-availability database
- Stateless REST APIs
- Session replication support

---

## Technology Stack

### Frontend
- **Vaadin Flow 24.x** - Server-side UI framework
- **TypeScript/JavaScript** - Custom components
- **Vis.js** - Graph visualization
- **D3.js** - Data visualization
- **Vite** - Build tool

### Backend
- **Spring Boot 3.x** - Application framework
- **Spring Security** - Authentication/Authorization
- **Spring Data JPA** - ORM layer
- **Quartz Scheduler** - Job scheduling engine
- **Jackson** - JSON serialization

### Data Layer
- **Apache Solr 9.x** - Search and analytics
- **PostgreSQL / H2** - Relational database
- **HikariCP** - Connection pooling

### Integration
- **REST APIs** - Inter-service communication
- **BigQueue** - Event queue management
- **Jackson ObjectMapper** - Serialization

---

## Module Structure

```
ikasan-dashboard/
├── ikasaneip/
│   ├── visualisation/
│   │   ├── dashboard/              # Main Vaadin dashboard application
│   │   │   ├── src/main/java/
│   │   │   │   └── org/ikasan/dashboard/
│   │   │   │       ├── Application.java           # Spring Boot entry point
│   │   │   │       └── ui/
│   │   │   │           ├── layout/               # Main layout components
│   │   │   │           ├── scheduler/            # Scheduler views/widgets
│   │   │   │           ├── visualisation/        # ESB visualization
│   │   │   │           ├── administration/       # Admin views
│   │   │   │           ├── search/              # Search functionality
│   │   │   │           └── security/            # Login/security
│   │   │   └── frontend/                         # Frontend resources
│   │   ├── dashboard-dist/          # Distribution packaging
│   │   └── vis.js/                  # Graph visualization library
│   │
│   ├── job-orchestration/
│   │   ├── core/                    # Scheduler core engine
│   │   │   └── machine/             # State machine implementation
│   │   ├── model/                   # Domain models
│   │   ├── rest/                    # REST API controllers
│   │   └── provision/               # Job plan provisioning
│   │
│   ├── solr/
│   │   └── solr-client/             # Solr DAOs and models
│   │       ├── dao/                 # Data access objects
│   │       │   ├── SolrSchedulerJobInstanceDaoImpl.java
│   │       │   ├── SolrScheduledContextInstanceDaoImpl.java
│   │       │   ├── SolrScheduledContextInstanceAuditDaoImpl.java
│   │       │   └── SolrScheduledContextInstanceAuditAggregateDaoImpl.java
│   │       └── model/               # Solr-specific models
│   │
│   └── rest/
│       ├── rest-dashboard/          # Dashboard REST endpoints
│       ├── rest-module-client/      # ESB module client
│       └── rest-scheduler-agent-client/  # Scheduler agent client
```

---

## Key Design Patterns

### 1. State Machine Pattern
- **Context State Machine** - Manages job plan execution lifecycle
- **Job State Machine** - Controls individual job transitions
- **Event-driven** - State changes triggered by process events

### 2. DAO Pattern
- Abstraction layer for data persistence
- Solr-specific implementations for time-series data
- JPA implementations for transactional data

### 3. Service Layer Pattern
- Business logic encapsulation
- Transaction management
- API orchestration

### 4. MVC Pattern (Server-side)
- Vaadin Flow components (View)
- Spring controllers (Controller)
- Domain services (Model)

### 5. Observer Pattern
- Event listeners for job state changes
- UI updates via server push
- Real-time dashboard refresh

---

## Performance Considerations

### Solr Optimization
- **Indexed fields**: status, contextInstanceId, jobName, timestamps
- **Faceted queries**: Fast status aggregations
- **Pagination**: Large result set handling
- **Caching**: Query result caching

### UI Performance
- **Server-side rendering**: Reduces client-side processing
- **Lazy loading**: Grid components with virtual scrolling
- **Push updates**: WebSocket-based real-time updates
- **Component caching**: Reusable UI components

### Concurrency
- **Thread-safe state machine**: Synchronized state transitions
- **Lock-free reads**: Optimistic concurrency for queries
- **Event queue**: Asynchronous event processing
- **Connection pooling**: Database connection management

---

## API Integration

### Scheduler Agent → Dashboard

**Job Completion Event:**
```http
POST /api/scheduledProcessEvent
Content-Type: application/json

{
  "agentName": "agent1",
  "jobName": "backupJob",
  "contextName": "dailyMaintenance",
  "contextInstanceId": "uuid-12345",
  "returnCode": 0,
  "successful": true,
  "fireTime": 1709683200000,
  "completionTime": 1709683300000
}
```

### Dashboard → Scheduler Agent

**Job Submission:**
```http
POST /api/submitJob
Content-Type: application/json

{
  "agentName": "agent1",
  "jobName": "backupJob",
  "contextInstanceId": "uuid-12345",
  "parameters": {
    "database": "production",
    "retentionDays": "30"
  }
}
```

---

## Monitoring & Observability

### Dashboard Metrics
- Active context instances
- Job execution rates
- Error rates by job type
- Lock contention statistics
- Agent health status

### Audit Trail
- User actions (login, configuration changes)
- Job submissions and cancellations
- System events (startup, shutdown)
- Configuration changes

### Logging
- Structured logging (JSON format)
- Log levels per package
- Correlation IDs for request tracing
- Audit logs for compliance

---

## Future Enhancements

1. **GraphQL API** - More flexible querying
2. **Elasticsearch Integration** - Alternative to Solr
3. **Prometheus Metrics** - Enhanced monitoring
4. **Kubernetes Deployment** - Cloud-native deployment
5. **Real-time Collaboration** - Multi-user job plan editing
6. **ML-based Predictions** - Job failure prediction
7. **Mobile Dashboard** - Responsive mobile UI

---

## References

- **Vaadin Flow Documentation**: https://vaadin.com/docs/latest/flow
- **Spring Boot Reference**: https://spring.io/projects/spring-boot
- **Apache Solr Guide**: https://solr.apache.org/guide
- **Ikasan GitHub**: https://github.com/ikasanEIP/ikasan-dashboard

---

**Document Version**: 1.0
**Last Updated**: 2026-03-06
**Maintained By**: Ikasan Development Team
