package com.pdbp.controller.util;

/**
 * Utility class for JSON operations.
 *
 * @author Saurabh Maurya
 */
public final class JsonUtils {

    private JsonUtils() {
        // Utility class
    }

    /**
     * Escapes JSON string to prevent injection.
     *
     * @param str the string to escape
     * @return escaped string
     */
    public static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Creates a JSON error response.
     *
     * @param message error message
     * @return JSON error response string
     */
    public static String errorResponse(String message) {
        return "{\"error\":\"" + escapeJson(message) + "\"}";
    }

    /**
     * Creates a JSON success message response.
     *
     * @param message success message
     * @return JSON message response string
     */
    public static String messageResponse(String message) {
        return "{\"message\":\"" + escapeJson(message) + "\"}";
    }
}

