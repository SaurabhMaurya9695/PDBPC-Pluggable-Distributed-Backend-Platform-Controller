package com.pdbp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdbp.controller.dto.PluginInfoDTO;
import com.pdbp.controller.dto.PluginInstallRequest;
import com.pdbp.controller.dto.PluginDescriptorDTO;
import com.pdbp.controller.util.JsonUtils;

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
        configureCors();
        registerHealthCheck();
        registerPluginRoutes();
        registerErrorHandlers();
    }

    /**
     * Configures CORS headers.
     */
    private void configureCors() {
        options("/*", (request, response) -> {
            String headers = request.headers("Access-Control-Request-Headers");
            if (headers != null) {
                response.header("Access-Control-Allow-Headers", headers);
            }
            String method = request.headers("Access-Control-Request-Method");
            if (method != null) {
                response.header("Access-Control-Allow-Methods", method);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.type("application/json");
        });
    }

    /**
     * Registers health check endpoint.
     */
    private void registerHealthCheck() {
        get("/health", (req, res) -> {
            res.status(200);
            return "{\"status\":\"UP\"}";
        });
    }

    /**
     * Registers plugin management routes.
     */
    private void registerPluginRoutes() {
        get("/api/plugins", this::listPlugins);
        get("/api/plugins/discover", this::discoverPlugins);
        get("/api/plugins/:name", this::getPluginInfo);
        post("/api/plugins/install", this::installPlugin);
        post("/api/plugins/:name/start", this::startPlugin);
        post("/api/plugins/:name/stop", this::stopPlugin);
        delete("/api/plugins/:name", this::unloadPlugin);
    }

    /**
     * Registers error handlers.
     */
    private void registerErrorHandlers() {
        notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return JsonUtils.errorResponse("Resource not found: " + req.pathInfo());
        });

        exception(PluginService.PluginServiceException.class, (exception, request, response) -> {
            response.status(400);
            response.type("application/json");
            String errorMsg = getRootCauseMessage(exception);
            response.body(JsonUtils.errorResponse(errorMsg));
        });

        exception(Exception.class, (exception, request, response) -> {
            logger.error("Unexpected error", exception);
            response.status(500);
            response.type("application/json");
            response.body(JsonUtils.errorResponse("Internal server error"));
        });
    }

    /**
     * Lists all installed plugins.
     */
    private String listPlugins(Request request, Response response) {
        return execute(() -> {
            Set<String> pluginNames = pluginService.listPlugins();
            List<PluginInfoDTO> plugins = pluginNames.stream().map(this::toPluginInfoDTO).collect(Collectors.toList());
            return successResponse(response, 200, plugins);
        }, response);
    }

    /**
     * Gets information about a specific plugin.
     */
    private String getPluginInfo(Request request, Response response) {
        return execute(() -> {
            String pluginName = request.params(":name");
            PluginService.PluginInfo info = pluginService.getPluginInfo(pluginName);
            if (info == null) {
                return errorResponse(response, 404, "Plugin not found: " + pluginName);
            }
            return successResponse(response, 200, toPluginInfoDTO(info));
        }, response);
    }

    /**
     * Discovers plugins in the plugin directory.
     */
    private String discoverPlugins(Request request, Response response) {
        return execute(() -> {
            List<PluginService.PluginDescriptor> descriptors = pluginService.discoverPlugins();
            List<PluginDescriptorDTO> plugins = descriptors.stream().map(
                    desc -> new PluginDescriptorDTO(desc.getName(), desc.getJarPath(), desc.getClassName(),
                            desc.getSize())).collect(Collectors.toList());
            return successResponse(response, 200, plugins);
        }, response);
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
                return errorResponse(response, 400, "Missing required fields: pluginName, jarPath, className");
            }

            PluginService.PluginInfo info = pluginService.installPlugin(pluginName, jarPath, className);
            return successResponse(response, 201, toPluginInfoDTO(info));
        } catch (PluginService.PluginServiceException e) {
            return handleServiceException(response, e);
        } catch (Exception e) {
            return handleUnexpectedException(response, e, "installing plugin");
        }
    }

    /**
     * Starts a plugin.
     */
    private String startPlugin(Request request, Response response) {
        return execute(() -> {
            String pluginName = request.params(":name");
            if (!validatePluginExists(pluginName, response)) {
                return null; // Error response already set
            }
            PluginService.PluginInfo info = pluginService.startPlugin(pluginName);
            return successResponse(response, 200, toPluginInfoDTO(info));
        }, response);
    }

    /**
     * Stops a plugin.
     */
    private String stopPlugin(Request request, Response response) {
        return execute(() -> {
            String pluginName = request.params(":name");
            if (!validatePluginExists(pluginName, response)) {
                return null; // Error response already set
            }
            PluginService.PluginInfo info = pluginService.stopPlugin(pluginName);
            return successResponse(response, 200, toPluginInfoDTO(info));
        }, response);
    }

    /**
     * Unloads a plugin.
     */
    private String unloadPlugin(Request request, Response response) {
        return execute(() -> {
            String pluginName = request.params(":name");
            if (!validatePluginExists(pluginName, response)) {
                return null; // Error response already set
            }
            pluginService.unloadPlugin(pluginName);
            response.status(200);
            return JsonUtils.messageResponse("Plugin unloaded: " + pluginName);
        }, response);
    }

    /**
     * Validates that a plugin exists, returns error response if not.
     */
    private boolean validatePluginExists(String pluginName, Response response) {
        PluginService.PluginInfo info = pluginService.getPluginInfo(pluginName);
        if (info == null) {
            Set<String> availablePlugins = pluginService.listPlugins();
            String message = buildPluginNotFoundMessage(pluginName, availablePlugins);
            errorResponse(response, 404, message);
            return false;
        }
        return true;
    }

    /**
     * Builds a helpful error message when plugin is not found.
     */
    private String buildPluginNotFoundMessage(String pluginName, Set<String> availablePlugins) {
        String message = "Plugin not found: " + pluginName;
        if (availablePlugins.isEmpty()) {
            message += ". No plugins installed. Install a plugin first: POST /api/plugins/install";
        } else {
            message += ". Available plugins: [" + String.join(", ", availablePlugins)
                    + "]. Install the plugin first: POST /api/plugins/install";
        }
        return message;
    }

    /**
     * Executes a handler with error handling.
     */
    private String execute(Handler handler, Response response) {
        try {
            return handler.execute();
        } catch (PluginService.PluginServiceException e) {
            return handleServiceException(response, e);
        } catch (Exception e) {
            return handleUnexpectedException(response, e, "processing request");
        }
    }

    /**
     * Functional interface for request handlers.
     */
    @FunctionalInterface
    private interface Handler {

        String execute() throws Exception;
    }

    /**
     * Handles PluginServiceException.
     */
    private String handleServiceException(Response response, PluginService.PluginServiceException e) {
        response.status(400);
        return JsonUtils.errorResponse(getRootCauseMessage(e));
    }

    /**
     * Handles unexpected exceptions.
     */
    private String handleUnexpectedException(Response response, Exception e, String context) {
        logger.error("Unexpected error {}", context, e);
        response.status(500);
        return JsonUtils.errorResponse("Internal server error");
    }

    /**
     * Gets root cause message from exception.
     */
    private String getRootCauseMessage(PluginService.PluginServiceException e) {
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            return e.getCause().getMessage();
        }
        return e.getMessage();
    }

    /**
     * Creates a success JSON response.
     */
    private String successResponse(Response response, int status, Object data) throws Exception {
        response.status(status);
        return objectMapper.writeValueAsString(data);
    }

    /**
     * Creates an error JSON response.
     */
    private String errorResponse(Response response, int status, String message) {
        response.status(status);
        return JsonUtils.errorResponse(message);
    }

    /**
     * Converts PluginInfo to PluginInfoDTO.
     */
    private PluginInfoDTO toPluginInfoDTO(PluginService.PluginInfo info) {
        return new PluginInfoDTO(info.getName(), info.getVersion(), info.getState(), info.getJarPath());
    }

    /**
     * Converts plugin name to PluginInfoDTO (with fallback for missing info).
     */
    private PluginInfoDTO toPluginInfoDTO(String pluginName) {
        PluginService.PluginInfo info = pluginService.getPluginInfo(pluginName);
        if (info != null) {
            return toPluginInfoDTO(info);
        }
        return new PluginInfoDTO(pluginName, "unknown", "UNKNOWN", null);
    }
}

