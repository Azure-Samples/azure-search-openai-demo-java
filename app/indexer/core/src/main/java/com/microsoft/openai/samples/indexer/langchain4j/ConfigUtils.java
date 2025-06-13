package com.microsoft.openai.samples.indexer.langchain4j;

import java.util.Map;

public class ConfigUtils {

    public static String getString(String name, Map<String,String> config){
        String value = config.get(name);
        validateString(name,value);
        return value;
    }

    public static int getInt(String name, Map<String,String> config) {
        String value = getString(name, config);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IndexingConfigException("Configuration value for '" + name + "' is not a valid integer: " + value, e);
        }
    }
    public static boolean getBoolean(String name, Map<String,String> config) {
        String value = getString(name, config);
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        } else {
            throw new IndexingConfigException("Configuration value for '" + name + "' is not a valid boolean: " + value);
        }
    }

    public static double getDouble(String name, Map<String,String> config) {
        String value = getString(name, config);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IndexingConfigException("Configuration value for '" + name + "' is not a valid double: " + value, e);
        }
    }

    public static void validateString(String name, String value) {
        if (value == null || value.isEmpty()) {
            throw new IndexingConfigException("Configuration value for '" + name + "' is missing or empty.");
        }
    }

    public static int parseIntOrDefault(Map<String, String> params, String key, int defaultValue) {
        if (params != null && params.get(key) != null) {
            try {
                return Integer.parseInt(params.get(key));
            } catch (NumberFormatException e) {
                // keep default
            }
        }
        return defaultValue;
    }

    public static boolean parseBooleanOrDefault(Map<String, String> params, String key, boolean defaultValue) {
        if (params != null && params.get(key) != null) {
            String value = params.get(key).toLowerCase();
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            }
        }
        return defaultValue;
    }



}
