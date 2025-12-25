package com.pdbp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdbp.controller.dto.PluginInfoDTO;
import com.pdbp.controller.dto.PluginInstallRequest;
import com.pdbp.controller.dto.PluginDescriptorDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * REST API controller for plugin management.
 *
 * @author Saurabh Maurya
 */
public class PluginController {

    private static final Logger logger = LoggerFactory.getLogger(PluginController.class);

    private final PluginService pluginService;
    private final ObjectMapper objectMapper;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Registers all REST API routes.
     */
    public void registerRoutes() {
        // Enable CORS
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.type("application/json");
        });

        // Health check
        get("/health", (req, res) -> {
            res.status(200);
            return "{\"status\":\"UP\"}";
        });

        // List all plugins
        get("/api/plugins", this::listPlugins);

        // Discover plugins in directory (must come before /api/plugins/:name)
        get("/api/plugins/discover", this::discoverPlugins);

        // Get plugin info
        get("/api/plugins/:name", this::getPluginInfo);

        // Install plugin
        post("/api/plugins/install", this::installPlugin);

        // Start plugin
        post("/api/plugins/:name/start", this::startPlugin);

        // Stop plugin
        post("/api/plugins/:name/stop", this::stopPlugin);

        // Unload plugin
        delete("/api/plugins/:name", this::unloadPlugin);

        // 404 handler
        notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return "{\"error\":\"Resource not found: " + req.pathInfo() + "\"}";
        });

        // Exception handling
        exception(PluginService.PluginServiceException.class, (exception, request, response) -> {
            response.status(400);
            response.type("application/json");
            String errorMsg = exception.getMessage();
            if (exception.getCause() != null) {
                errorMsg += " (Cause: " + exception.getCause().getMessage() + ")";
            }
            response.body("{\"error\":\"" + escapeJson(errorMsg) + "\"}");
        });

        exception(Exception.class, (exception, request, response) -> {
            logger.error("Unexpected error", exception);
            response.status(500);
            response.type("application/json");
            response.body("{\"error\":\"Internal server error\"}");
        });
    }

    /**
     * Lists all installed plugins.
     */
    private String listPlugins(Request request, Response response) {
        try {
            Set<String> pluginNames = pluginService.listPlugins();
            List<PluginInfoDTO> plugins = pluginNames.stream().map(name -> {
                PluginService.PluginInfo info = pluginService.getPluginInfo(name);
                if (info != null) {
                    return new PluginInfoDTO(info.getName(), info.getVersion(), info.getState(), info.getJarPath());
                }
                return new PluginInfoDTO(name, "unknown", "UNKNOWN", null);
            }).collect(Collectors.toList());

            response.status(200);
            return objectMapper.writeValueAsString(plugins);
        } catch (Exception e) {
            logger.error("Error listing plugins", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Gets information about a specific plugin.
     */
    private String getPluginInfo(Request request, Response response) {
        try {
            String pluginName = request.params(":name");
            PluginService.PluginInfo info = pluginService.getPluginInfo(pluginName);

            if (info == null) {
                response.status(404);
                return "{\"error\":\"Plugin not found: " + pluginName + "\"}";
            }

            PluginInfoDTO dto = new PluginInfoDTO(info.getName(), info.getVersion(), info.getState(),
                    info.getJarPath());

            response.status(200);
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            logger.error("Error getting plugin info", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Discovers plugins in the plugin directory.
     */
    private String discoverPlugins(Request request, Response response) {
        try {
            List<PluginService.PluginDescriptor> descriptors = pluginService.discoverPlugins();
            List<PluginDescriptorDTO> plugins = descriptors.stream().map(
                    desc -> new PluginDescriptorDTO(desc.getName(), desc.getJarPath(), desc.getClassName(),
                            desc.getSize())).collect(Collectors.toList());

            response.status(200);
            return objectMapper.writeValueAsString(plugins);
        } catch (Exception e) {
            logger.error("Error discovering plugins", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Installs a plugin from a JAR file.
     */
    private String installPlugin(Request request, Response response) {
        try {
            PluginInstallRequest installRequest = objectMapper.readValue(request.body(), PluginInstallRequest.class);

            String pluginName = installRequest.getPluginName();
            String jarPath = installRequest.getJarPath();
            String className = installRequest.getClassName();

            if (pluginName == null || jarPath == null || className == null) {
                response.status(400);
                return "{\"error\":\"Missing required fields: pluginName, jarPath, className\"}";
            }

            // Delegate to service
            PluginService.PluginInfo info = pluginService.installPlugin(pluginName, jarPath, className);

            PluginInfoDTO dto = new PluginInfoDTO(info.getName(), info.getVersion(), info.getState(),
                    info.getJarPath());

            response.status(201);
            return objectMapper.writeValueAsString(dto);
        } catch (PluginService.PluginServiceException e) {
            logger.error("Error installing plugin", e);
            response.status(400);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        } catch (Exception e) {
            logger.error("Unexpected error installing plugin", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Starts a plugin.
     */
    private String startPlugin(Request request, Response response) {
        try {
            String pluginName = request.params(":name");
            
            // Check if plugin exists first for better error message
            PluginService.PluginInfo existingInfo = pluginService.getPluginInfo(pluginName);
            if (existingInfo == null) {
                response.status(404);
                String availablePlugins = String.join(", ", pluginService.listPlugins());
                String message = "Plugin not found: " + pluginName;
                if (availablePlugins.isEmpty()) {
                    message += ". No plugins installed. Install a plugin first: POST /api/plugins/install";
                } else {
                    message += ". Available plugins: [" + availablePlugins + "]. Install the plugin first: POST /api/plugins/install";
                }
                return "{\"error\":\"" + escapeJson(message) + "\"}";
            }
            
            PluginService.PluginInfo info = pluginService.startPlugin(pluginName);

            PluginInfoDTO dto = new PluginInfoDTO(info.getName(), info.getVersion(), info.getState(),
                    info.getJarPath());

            response.status(200);
            return objectMapper.writeValueAsString(dto);
        } catch (PluginService.PluginServiceException e) {
            logger.error("Error starting plugin", e);
            response.status(400);
            // Extract root cause message for better user experience
            String errorMsg = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMsg = e.getCause().getMessage();
            }
            return "{\"error\":\"" + escapeJson(errorMsg) + "\"}";
        } catch (Exception e) {
            logger.error("Unexpected error starting plugin", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Stops a plugin.
     */
    private String stopPlugin(Request request, Response response) {
        try {
            String pluginName = request.params(":name");
            
            // Check if plugin exists first
            PluginService.PluginInfo existingInfo = pluginService.getPluginInfo(pluginName);
            if (existingInfo == null) {
                response.status(404);
                return "{\"error\":\"Plugin not found: " + pluginName + ". Available plugins: [" + String.join(", ", pluginService.listPlugins()) + "]\"}";
            }
            
            PluginService.PluginInfo info = pluginService.stopPlugin(pluginName);

            PluginInfoDTO dto = new PluginInfoDTO(info.getName(), info.getVersion(), info.getState(),
                    info.getJarPath());

            response.status(200);
            return objectMapper.writeValueAsString(dto);
        } catch (PluginService.PluginServiceException e) {
            logger.error("Error stopping plugin", e);
            response.status(400);
            String errorMsg = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMsg = e.getCause().getMessage();
            }
            return "{\"error\":\"" + escapeJson(errorMsg) + "\"}";
        } catch (Exception e) {
            logger.error("Unexpected error stopping plugin", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Unloads a plugin.
     */
    private String unloadPlugin(Request request, Response response) {
        try {
            String pluginName = request.params(":name");
            
            // Check if plugin exists first
            PluginService.PluginInfo existingInfo = pluginService.getPluginInfo(pluginName);
            if (existingInfo == null) {
                response.status(404);
                return "{\"error\":\"Plugin not found: " + pluginName + ". Available plugins: [" + String.join(", ", pluginService.listPlugins()) + "]\"}";
            }
            
            pluginService.unloadPlugin(pluginName);

            response.status(200);
            return "{\"message\":\"Plugin unloaded: " + pluginName + "\"}";
        } catch (PluginService.PluginServiceException e) {
            logger.error("Error unloading plugin", e);
            response.status(400);
            String errorMsg = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMsg = e.getCause().getMessage();
            }
            return "{\"error\":\"" + escapeJson(errorMsg) + "\"}";
        } catch (Exception e) {
            logger.error("Unexpected error unloading plugin", e);
            response.status(500);
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Escapes JSON string to prevent injection.
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
                "\\t");
    }
}

