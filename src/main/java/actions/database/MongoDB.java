package actions.database;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static actions.common.BasePage.logError;
import static actions.common.BasePage.logInfo;
import static actions.common.BasePage.sleepInSecond;

import java.util.concurrent.ConcurrentHashMap;

public class MongoDB {

    private static final Map<String, MongoClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(MongoDB::closeAll));
    }

    public static MongoClient getClient(String connectionUri) {
        if (connectionUri == null || connectionUri.trim().isEmpty()) {
            throw new IllegalArgumentException("MongoDB connectionUri must not be null or empty");
        }
        return CLIENT_CACHE.computeIfAbsent(connectionUri, uri -> {
            logInfo("🔌 Initializing shared MongoClient connection pool for URI: " + uri);
            return MongoClients.create(uri);
        });
    }

    public static synchronized void closeAll() {
        CLIENT_CACHE.forEach((uri, client) -> {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        });
        CLIENT_CACHE.clear();
    }

    // MongoDB -> Database -> Collection -> Document -> Field
    public static String connectMongoDbAndGetData(String connectionUri, String dbName, String collectionName, String findKey, String findValue, String fieldGetData) {
        MongoClient mongoClient = getClient(connectionUri);
        Bson query = Filters.eq(findKey, findValue);
        MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collectionName);
        Document document = collection.find(query).first();
        logInfo("✅ Query MongoDB success, DB name: " + dbName);

        if (document == null) {
            logInfo("⚠️ No document found with query: " + query.toBsonDocument().toJson());
            return null;
        }
        Object data = document.get(fieldGetData);
        logInfo("✅ Get " + fieldGetData + " from " + collectionName + " collection, data: " + data);
        return data.toString();
    }


    public static Map<String, String> connectMongoDbAndGetData(String connectionUri, String dbName, String collectionName, String findKey, String findValue, List<String> fieldsGetData) {
        int maxRetry = 4, attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetry) {
            attempt++;
            try {
                MongoClient mongoClient = getClient(connectionUri);

                Bson query = Filters.eq(findKey, findValue);
                MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collectionName);
                Document document = collection.find(query).first();
                logInfo("✅ [Attempt " + attempt + "] Query MongoDB success, DB name: " + dbName);

                if (document == null) {
                    logInfo("⚠️ [Attempt " + attempt + "] No document found, retrying...");
                    continue;
                }

                Map<String, String> result = new LinkedHashMap<>();
                for (String field : fieldsGetData) {
                    Object value = field.contains(".") ? getValueByPath(document, field) : document.get(field);
                    if (value instanceof Date) {
                        result.put(field, toUtcSecond((Date) value));
                    } else {
                        result.put(field, value == null ? "" : value.toString());
                    }
                }

                logInfo("✅ Get fields " + fieldsGetData + " from " + collectionName + " collection, data list: " + result);
                return result;

            } catch (Exception e) {
                lastException = e;
                logError("[Attempt " + attempt + "] MongoDB error: " + e.getMessage());
                sleepInSecond(3);
            }
        }

        throw new RuntimeException("❌ MongoDB failed after " + maxRetry + " attempts", lastException);
    }


    private static Object getValueByPath(Document document, String path) {
        String normalizedPath = path.replaceAll("\\[(\\d+)]", ".$1");
        String[] keys = normalizedPath.split("\\.");
        Object current = document;

        for (String key : keys) {
            if (current == null) return null;

            if (current instanceof Document) {
                current = ((Document) current).get(key);
            } else if (current instanceof List) {
                try {
                    int index = Integer.parseInt(key);
                    List<?> list = (List<?>) current;
                    current = index < list.size() ? list.get(index) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }


    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC);

    public static String toUtcSecond(Date date) {
        if (date == null) return "";
        return UTC_FORMATTER.format(
                date.toInstant().truncatedTo(ChronoUnit.SECONDS)
        );
    }
}
