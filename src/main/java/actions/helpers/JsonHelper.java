package actions.helpers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.path.json.JsonPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.qameta.allure.Allure.step;

/**
 * Lớp tiện ích duy nhất xử lý toàn bộ thao tác JSON (Object, Array, Serialization, I/O)
 * dựa trên Jackson Engine.
 */
public class JsonHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    // ==================== FACTORY & PARSE ====================

    public static ObjectNode createObjectNode() {
        return MAPPER.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return MAPPER.createArrayNode();
    }

    public static JsonNode parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string: " + e.getMessage(), e);
        }
    }

    public static ObjectNode parseObject(String json) {
        JsonNode node = parseJson(json);
        if (node instanceof ObjectNode on) {
            return on;
        }
        ObjectNode on = MAPPER.createObjectNode();
        if (node != null && !node.isNull()) {
            on.set("value", node);
        }
        return on;
    }

    public static ArrayNode parseArray(String json) {
        JsonNode node = parseJson(json);
        if (node instanceof ArrayNode an) {
            return an;
        }
        ArrayNode an = MAPPER.createArrayNode();
        if (node != null && !node.isNull()) {
            an.add(node);
        }
        return an;
    }

    // ==================== OBJECT NODE OPERATIONS ====================

    public static ObjectNode put(ObjectNode node, String key, Object value) {
        if (node == null) return null;
        if (value == null) {
            node.putNull(key);
        } else if (value instanceof String s) {
            node.put(key, s);
        } else if (value instanceof Integer i) {
            node.put(key, i);
        } else if (value instanceof Long l) {
            node.put(key, l);
        } else if (value instanceof Double d) {
            node.put(key, d);
        } else if (value instanceof Float f) {
            node.put(key, f);
        } else if (value instanceof Boolean b) {
            node.put(key, b);
        } else if (value instanceof JsonNode jn) {
            node.set(key, jn);
        } else {
            node.putPOJO(key, value);
        }
        return node;
    }

    public static String getString(JsonNode node, String key, String defaultValue) {
        if (node == null) return defaultValue;
        JsonNode child = node.get(key);
        return child == null || child.isNull() ? defaultValue : child.asText();
    }

    public static String getString(JsonNode node, String key) {
        return getString(node, key, "");
    }

    public static int getInt(JsonNode node, String key, int defaultValue) {
        if (node == null) return defaultValue;
        JsonNode child = node.get(key);
        return child == null || child.isNull() ? defaultValue : child.asInt(defaultValue);
    }

    public static int getInt(JsonNode node, String key) {
        return getInt(node, key, 0);
    }

    public static long getLong(JsonNode node, String key, long defaultValue) {
        if (node == null) return defaultValue;
        JsonNode child = node.get(key);
        return child == null || child.isNull() ? defaultValue : child.asLong(defaultValue);
    }

    public static long getLong(JsonNode node, String key) {
        return getLong(node, key, 0L);
    }

    public static double getDouble(JsonNode node, String key, double defaultValue) {
        if (node == null) return defaultValue;
        JsonNode child = node.get(key);
        return child == null || child.isNull() ? defaultValue : child.asDouble(defaultValue);
    }

    public static double getDouble(JsonNode node, String key) {
        return getDouble(node, key, 0.0);
    }

    public static boolean getBoolean(JsonNode node, String key, boolean defaultValue) {
        if (node == null) return defaultValue;
        JsonNode child = node.get(key);
        return child == null || child.isNull() ? defaultValue : child.asBoolean(defaultValue);
    }

    public static boolean getBoolean(JsonNode node, String key) {
        return getBoolean(node, key, false);
    }

    public static boolean has(JsonNode node, String key) {
        if (node == null) return false;
        JsonNode child = node.get(key);
        return child != null && !child.isMissingNode() && !child.isNull();
    }

    // ==================== ARRAY NODE OPERATIONS ====================

    public static ArrayNode add(ArrayNode arrayNode, Object value) {
        if (arrayNode == null) return null;
        if (value == null) {
            arrayNode.addNull();
        } else if (value instanceof String s) {
            arrayNode.add(s);
        } else if (value instanceof Integer i) {
            arrayNode.add(i);
        } else if (value instanceof Long l) {
            arrayNode.add(l);
        } else if (value instanceof Double d) {
            arrayNode.add(d);
        } else if (value instanceof Float f) {
            arrayNode.add(f);
        } else if (value instanceof Boolean b) {
            arrayNode.add(b);
        } else if (value instanceof JsonNode jn) {
            arrayNode.add(jn);
        } else {
            arrayNode.addPOJO(value);
        }
        return arrayNode;
    }

    public static List<String> toList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                list.add(item.asText());
            }
        }
        return list;
    }

    public static boolean isJsonArrayContainSmallArray(JsonNode jsonArray, String[] smallArray) {
        if (jsonArray == null || smallArray == null) return false;
        List<String> list = toList(jsonArray);
        for (String element : smallArray) {
            if (!list.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isJsonArrayContainSmallArray(String jsonArrayString, String[] smallArray) {
        JsonNode node = parseJson(jsonArrayString);
        return isJsonArrayContainSmallArray(node, smallArray);
    }

    // ==================== SERIALIZATION & FILE I/O ====================

    public static String toJson(Object value) {
        if (value == null) return "null";
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON: " + e.getMessage(), e);
        }
    }

    public static String toPrettyJson(Object value) {
        if (value == null) return "null";
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return toJson(value);
        }
    }

    public static JsonNode readFileJson(String pathJsonFile) {
        try {
            return MAPPER.readTree(new File(pathJsonFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + e.getMessage(), e);
        }
    }

    public static void writeFileJson(String pathJsonFile, Object object) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(pathJsonFile), object);
            step("Update file json: " + pathJsonFile + " || " + object);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON file: " + e.getMessage(), e);
        }
    }

    public static JsonNode readJsonFromFile(Path filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath));
        return parseJson(content);
    }

    public static void writeJsonToFile(Object json, Path filePath) throws IOException {
        Files.write(filePath, toPrettyJson(json).getBytes());
    }

    public static String getResponseKeyValue(String filePath, String responseKey) {
        try {
            String responseBody = new String(Files.readAllBytes(Paths.get(filePath)));
            JsonPath jsonPath = new JsonPath(responseBody);
            return jsonPath.get(responseKey).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read or parse the JSON file: " + e.getMessage());
        }
    }

    public static void updateJsonValue(String jsonPath, String key, Object newValue) {
        try {
            Path path = Paths.get(jsonPath);
            JsonNode root = readJsonFromFile(path);
            if (root instanceof ObjectNode on) {
                put(on, key, newValue);
                Files.write(path, toPrettyJson(on).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update JSON file: " + e.getMessage(), e);
        }
    }

    public static String sanitizeInvalidJsonEscapes(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        StringBuilder result = new StringBuilder(json.length());
        boolean insideString = false;

        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);

            if (current == '"' && !isEscaped(json, i)) {
                insideString = !insideString;
                result.append(current);
                continue;
            }

            if (current == '\\' && insideString) {
                if (i + 1 >= json.length()) {
                    continue;
                }

                if (isValidJsonEscape(json, i + 1)) {
                    result.append(current);
                }
            } else {
                result.append(current);
            }
        }

        return result.toString();
    }

    private static boolean isValidJsonEscape(String json, int index) {
        char c = json.charAt(index);
        if ("\"\\/bfnrt".indexOf(c) >= 0) return true;

        if (c == 'u') {
            return index + 4 < json.length()
                    && isHex(json.charAt(index + 1))
                    && isHex(json.charAt(index + 2))
                    && isHex(json.charAt(index + 3))
                    && isHex(json.charAt(index + 4));
        }
        return false;
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) != -1;
    }

    private static boolean isEscaped(String text, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 != 0;
    }
}
