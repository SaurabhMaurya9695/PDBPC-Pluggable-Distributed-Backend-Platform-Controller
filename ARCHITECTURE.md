# PDBPC Architecture

## Overview

**PDBPC** (PDBP Controllers) is a separate module that contains **only** the presentation layer:
- REST API controllers
- DTOs (Data Transfer Objects)
- HTTP request/response handling
- JSON serialization

## Design Principles

### 1. Separation of Concerns
- **PDBPC**: Handles HTTP layer only
- **PDBP**: Contains all business logic

### 2. Dependency Inversion
- PDBPC defines `PluginService` **interface**
- PDBP implements `PluginService` via `PluginServiceAdapter`
- Controllers depend on interface, not implementation

### 3. Single Responsibility
- `PluginController`: HTTP routing and JSON handling
- `PluginService`: Business operations interface
- DTOs: Data transfer only

## Architecture Diagram

```
┌─────────────────────────────────────┐
│         HTTP Request                │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│    PluginController (PDBPC)         │
│  - Route handling                    │
│  - JSON serialization                │
│  - HTTP status codes                 │
└──────────────┬──────────────────────┘
               │
               │ uses
               ▼
┌─────────────────────────────────────┐
│    PluginService (Interface)        │
│  - Defined in PDBPC                  │
│  - No implementation                │
└──────────────┬──────────────────────┘
               │
               │ implemented by
               ▼
┌─────────────────────────────────────┐
│  PluginServiceAdapter (PDBP)        │
│  - Adapts PluginManager              │
│  - Implements PluginService         │
└──────────────┬──────────────────────┘
               │
               │ uses
               ▼
┌─────────────────────────────────────┐
│    PluginManager (PDBP)              │
│  - Business logic                    │
│  - Plugin lifecycle                  │
└─────────────────────────────────────┘
```

## Module Structure

```
pdbpc/
├── src/main/java/com/pdbp/controller/
│   ├── PluginController.java          # REST API controller
│   ├── PluginService.java             # Service interface (no impl)
│   └── dto/
│       ├── PluginInfoDTO.java         # Plugin info DTO
│       ├── PluginInstallRequest.java  # Install request DTO
│       └── PluginDescriptorDTO.java   # Discovery DTO
└── pom.xml
```

## Benefits

1. **Reusability**: Controllers can be used with different implementations
2. **Testability**: Easy to mock `PluginService` for testing
3. **Maintainability**: Clear separation between HTTP and business logic
4. **Flexibility**: Can swap HTTP framework without changing business logic

## Usage in PDBP

```java
// In PDBPServer
PluginManager manager = new PluginManager();
PluginDiscoveryService discovery = new PluginDiscoveryService();
PluginService service = new PluginServiceAdapter(manager, discovery);
PluginController controller = new PluginController(service);
controller.registerRoutes();
```

## API Endpoints

All endpoints are registered by `PluginController.registerRoutes()`:

- `GET /health` - Health check
- `GET /api/plugins` - List all plugins
- `GET /api/plugins/discover` - Discover plugins
- `GET /api/plugins/:name` - Get plugin info
- `POST /api/plugins/install` - Install plugin
- `POST /api/plugins/:name/start` - Start plugin
- `POST /api/plugins/:name/stop` - Stop plugin
- `DELETE /api/plugins/:name` - Unload plugin

