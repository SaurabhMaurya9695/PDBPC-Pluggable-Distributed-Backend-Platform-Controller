# PDBPC - PDBP Controllers

REST API controllers and DTOs for the PDBP platform.

## Purpose

This module contains **only** the presentation layer:
- REST API controllers
- DTOs (Data Transfer Objects)
- Request/Response handling
- JSON serialization

**No business logic** - all logic is delegated to `PluginService` interface.

## Architecture

```
┌─────────────────────────┐
│   PluginController      │  ← This module (PDBPC)
│   (HTTP handling)       │
└──────────┬──────────────┘
           │
           │ uses
           ▼
┌─────────────────────────┐
│   PluginService         │  ← Interface (defined here)
│   (Business logic)      │  ← Implementation (in PDBP)
└─────────────────────────┘
```

## Design Principles

- **Separation of Concerns**: Controllers only handle HTTP
- **Dependency Inversion**: Depends on PluginService interface, not implementation
- **Single Responsibility**: Each class has one clear purpose
- **No Business Logic**: All logic delegated to service layer

## Usage

This module is used as a dependency by the main PDBP project.

The PDBP project will:
1. Implement `PluginService` interface
2. Create `PluginController` instance with the implementation
3. Register routes

## Module Structure

```
pdbpc/
├── src/main/java/com/pdbp/controller/
│   ├── PluginController.java      # REST API controller
│   ├── PluginService.java         # Service interface
│   └── dto/
│       ├── PluginInfoDTO.java
│       ├── PluginInstallRequest.java
│       └── PluginDescriptorDTO.java
└── pom.xml
```

