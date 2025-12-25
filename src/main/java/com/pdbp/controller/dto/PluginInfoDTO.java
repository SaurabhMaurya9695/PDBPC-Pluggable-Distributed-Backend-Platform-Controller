package com.pdbp.controller.dto;

/**
 * Data Transfer Object for plugin information.
 * Used for REST API responses.
 *
 * @author Saurabh Maurya
 */
public class PluginInfoDTO {
    
    private String name;
    private String version;
    private String state; // String to avoid dependency on PluginState enum
    private String jarPath;
    
    public PluginInfoDTO() {
    }
    
    public PluginInfoDTO(String name, String version, String state, String jarPath) {
        this.name = name;
        this.version = version;
        this.state = state;
        this.jarPath = jarPath;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
}

