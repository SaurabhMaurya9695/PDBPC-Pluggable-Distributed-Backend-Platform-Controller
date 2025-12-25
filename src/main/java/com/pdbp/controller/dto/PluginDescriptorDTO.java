package com.pdbp.controller.dto;

/**
 * DTO for discovered plugin descriptor.
 *
 * @author Saurabh Maurya
 */
public class PluginDescriptorDTO {
    
    private String name;
    private String jarPath;
    private String className;
    private long size;
    
    public PluginDescriptorDTO() {
    }
    
    public PluginDescriptorDTO(String name, String jarPath, String className, long size) {
        this.name = name;
        this.jarPath = jarPath;
        this.className = className;
        this.size = size;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
}

