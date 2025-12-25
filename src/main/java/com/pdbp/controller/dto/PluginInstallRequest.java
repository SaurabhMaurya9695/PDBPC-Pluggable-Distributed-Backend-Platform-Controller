package com.pdbp.controller.dto;

/**
 * Request DTO for plugin installation.
 *
 * @author Saurabh Maurya
 */
public class PluginInstallRequest {
    
    private String pluginName;
    private String jarPath;
    private String className;
    
    public PluginInstallRequest() {
    }
    
    public PluginInstallRequest(String pluginName, String jarPath, String className) {
        this.pluginName = pluginName;
        this.jarPath = jarPath;
        this.className = className;
    }
    
    public String getPluginName() {
        return pluginName;
    }
    
    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
    
    public String getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
}

