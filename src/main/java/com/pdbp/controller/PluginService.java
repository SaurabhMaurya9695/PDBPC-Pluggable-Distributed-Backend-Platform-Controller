package com.pdbp.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service interface for plugin operations.
 *
 * <p>This interface abstracts the plugin management operations
 * so that controllers don't depend directly on PluginManager.
 *
 * <p>The implementation will be provided by the PDBP platform.
 *
 * @author Saurabh Maurya
 */
public interface PluginService {

    /**
     * Lists all installed plugins.
     *
     * @return set of plugin names
     */
    Set<String> listPlugins();

    /**
     * Gets plugin information.
     *
     * @param pluginName the plugin name
     * @return plugin info (name, version, state) or null if not found
     */
    PluginInfo getPluginInfo(String pluginName);

    /**
     * Discovers plugins in the plugin directory.
     *
     * @return list of discovered plugin descriptors
     */
    List<PluginDescriptor> discoverPlugins();

    /**
     * Installs a plugin.
     *
     * @param pluginName the plugin name
     * @param jarPath    path to the plugin JAR
     * @param className  fully qualified class name
     * @return plugin info after installation
     * @throws PluginServiceException if installation fails
     */
    PluginInfo installPlugin(String pluginName, String jarPath, String className) throws PluginServiceException;

    /**
     * Starts a plugin.
     *
     * @param pluginName the plugin name
     * @return updated plugin info
     * @throws PluginServiceException if startup fails
     */
    PluginInfo startPlugin(String pluginName) throws PluginServiceException;

    /**
     * Stops a plugin.
     *
     * @param pluginName the plugin name
     * @return updated plugin info
     * @throws PluginServiceException if shutdown fails
     */
    PluginInfo stopPlugin(String pluginName) throws PluginServiceException;

    /**
     * Unloads a plugin.
     *
     * @param pluginName the plugin name
     * @throws PluginServiceException if unloading fails
     */
    void unloadPlugin(String pluginName) throws PluginServiceException;

    /**
     * Gets platform metrics.
     *
     * @return metrics data as a map
     */
    Map<String, Object> getMetrics();

    /**
     * Records an API request for metrics tracking.
     *
     * @param endpoint the API endpoint path
     */
    void recordApiRequest(String endpoint);

    /**
     * Records an API error for metrics tracking.
     *
     * @param endpoint the API endpoint path
     */
    void recordApiError(String endpoint);

    /**
     * Gets plugin configuration.
     *
     * @param pluginName the plugin name
     * @return configuration map, or null if plugin not found
     * @throws PluginServiceException if operation fails
     */
    Map<String, String> getPluginConfig(String pluginName) throws PluginServiceException;

    /**
     * Updates plugin configuration.
     *
     * @param pluginName the plugin name
     * @param config    configuration map to update
     * @throws PluginServiceException if operation fails
     */
    void updatePluginConfig(String pluginName, Map<String, String> config) throws PluginServiceException;

    /**
     * Plugin information model.
     */
    class PluginInfo {

        private final String name;
        private final String version;
        private final String state;
        private final String jarPath;

        public PluginInfo(String name, String version, String state, String jarPath) {
            this.name = name;
            this.version = version;
            this.state = state;
            this.jarPath = jarPath;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getState() {
            return state;
        }

        public String getJarPath() {
            return jarPath;
        }
    }

    /**
     * Plugin descriptor model.
     */
    class PluginDescriptor {

        private final String name;
        private final String jarPath;
        private final String className;
        private final long size;

        public PluginDescriptor(String name, String jarPath, String className, long size) {
            this.name = name;
            this.jarPath = jarPath;
            this.className = className;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getJarPath() {
            return jarPath;
        }

        public String getClassName() {
            return className;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * Exception for plugin service operations.
     */
    class PluginServiceException extends Exception {

        private static final long serialVersionUID = 1L;

        public PluginServiceException(String message) {
            super(message);
        }

        public PluginServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

