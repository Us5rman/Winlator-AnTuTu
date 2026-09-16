package com.winlator.star.contentdialog;

import java.util.HashMap;
import java.util.Map;

public class GraphicsDriverConfigDialog {

    public static HashMap<String, String> parseGraphicsDriverConfig(String graphicsDriverConfig) {
        HashMap<String, String> mappedConfig = new HashMap<>();
        if (graphicsDriverConfig == null || graphicsDriverConfig.isEmpty()) {
            return mappedConfig;
        }
        String[] configElements = graphicsDriverConfig.split(";");
        for (String element : configElements) {
            if (element.isEmpty()) continue;
            String key;
            String value;
            String[] splittedElement = element.split("=");
            key = splittedElement[0];
            if (splittedElement.length > 1)
                value = element.split("=")[1];
            else
                value = "";
            mappedConfig.put(key, value);
        }
        return mappedConfig;
    }

    public static String toGraphicsDriverConfig(HashMap<String, String> config) {
        if (config == null || config.isEmpty()) {
            return "";
        }
        StringBuilder graphicsDriverConfig = new StringBuilder();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            graphicsDriverConfig.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        return graphicsDriverConfig.length() > 0 ? graphicsDriverConfig.substring(0, graphicsDriverConfig.length() - 1) : "";
    }

    public static String getVersion(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return config.get("version");
    }

    public static String getExtensionsBlacklist(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return config.get("blacklistedExtensions");
    }

    public static boolean isEtc1Enabled(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return Boolean.parseBoolean(config.getOrDefault("etc1", "false"));
    }

    public static boolean isEtc2Enabled(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return Boolean.parseBoolean(config.getOrDefault("etc2", "false"));
    }

    public static boolean isAstcEnabled(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return Boolean.parseBoolean(config.getOrDefault("astc", "false"));
    }

    public static String getAstcBlockSize(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return config.getOrDefault("astcBlockSize", "6x6");
    }
}
