package actions.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Quản lý nạp file cấu hình môi trường từ thư mục src/main/resources/config/
 * Thứ tự ưu tiên môi trường:
 * 1. Cấu hình truyền từ file TestNG XML (<parameter name="env" value="..."/>)
 * 2. Tham số dòng lệnh JVM (-Denv=... hoặc -Denvironment=...)
 * 3. Giá trị mặc định trong GlobalConstants.ENV
 */
public class ConfigLoader {
    private static final Logger logger = LogManager.getLogger(ConfigLoader.class);
    private static volatile ConfigLoader instance;

    private final Properties properties = new Properties();
    private String currentEnv;

    private ConfigLoader() {
        this(null);
    }

    private ConfigLoader(String env) {
        this.currentEnv = resolveEnv(env);
        String source = resolveSource(env);
        System.setProperty("env", this.currentEnv);
        loadProperties(this.currentEnv, source);
    }

    /**
     * Phân giải tên môi trường theo thứ tự ưu tiên:
     * 1. rawEnv truyền vào (ví dụ từ TestNG XML parameter)
     * 2. System property: -Denv hoặc -Denvironment
     * 3. GlobalConstants.ENV
     */
    public static String resolveEnv(String rawEnv) {
        if (rawEnv != null && !rawEnv.trim().isEmpty()) {
            return rawEnv.trim().toLowerCase();
        }

        String sysEnv = System.getProperty("env");
        if (sysEnv == null || sysEnv.trim().isEmpty()) {
            sysEnv = System.getProperty("environment");
        }
        if (sysEnv != null && !sysEnv.trim().isEmpty()) {
            return sysEnv.trim().toLowerCase();
        }

        return GlobalConstants.ENV.trim().toLowerCase();
    }

    /**
     * Xác định nguồn cấu hình môi trường
     */
    public static String resolveSource(String rawEnv) {
        if (rawEnv != null && !rawEnv.trim().isEmpty()) {
            return "XML Suite";
        }

        String sysEnv = System.getProperty("env");
        if (sysEnv == null || sysEnv.trim().isEmpty()) {
            sysEnv = System.getProperty("environment");
        }
        if (sysEnv != null && !sysEnv.trim().isEmpty()) {
            return "System Properties (-Denv)";
        }

        return "GlobalConstants.ENV";
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    private void loadProperties(String env, String source) {
        String path = "config/" + env + ".properties";
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);

        if (is == null) {
            String defaultEnv = GlobalConstants.ENV.trim().toLowerCase();
            String defaultPath = "config/" + defaultEnv + ".properties";
            logger.warn("⚠️ Không tìm thấy file cấu hình: [{}] (Nguồn: {}). Tự động fallback về môi trường mặc định: [{}] ({})",
                    path, source, defaultEnv.toUpperCase(), defaultPath);
            is = getClass().getClassLoader().getResourceAsStream(defaultPath);
            if (is == null) {
                throw new IllegalStateException("❌ Không tìm thấy cả file cấu hình mặc định: " + defaultPath + " trong src/main/resources/config/");
            }
            this.currentEnv = defaultEnv;
            System.setProperty("env", defaultEnv);
            env = defaultEnv;
            path = defaultPath;
        }

        try (InputStream stream = is) {
            properties.load(stream);
            logger.info("✅ Đã nạp thành công cấu hình môi trường [{}] từ file: {} (Nguồn: {})", env.toUpperCase(), path, source);
        } catch (IOException e) {
            throw new IllegalStateException("❌ Lỗi khi đọc file cấu hình: " + path, e);
        }
    }

    /**
     * Cho phép chuyển đổi môi trường hoặc reload cấu hình theo nhu cầu.
     *
     * @param env Tên môi trường (dev, uat...). Nếu null/rỗng sẽ tự động phân giải qua System Prop hoặc GlobalConstants.ENV.
     */
    public static synchronized void reload(String env) {
        instance = new ConfigLoader(env);
    }

    public static String getEnv() {
        return getInstance().currentEnv;
    }

    // ==========================================
    // CÁC HÀM LẤY THEO KEY (KEY-BASED RETRIEVAL)
    // ==========================================

    public String getProperty(String key) {
        if (key == null) return null;

        String val = properties.getProperty(key);
        if (val != null) return val.trim();

        val = properties.getProperty(key.toUpperCase());
        if (val != null) return val.trim();

        val = properties.getProperty(key.replace(".", "_").toUpperCase());
        if (val != null) return val.trim();

        val = properties.getProperty(key.replace("_", ".").toLowerCase());
        if (val != null) return val.trim();

        return null;
    }

    public String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        return val != null ? val : defaultValue;
    }

    public static String get(String key) {
        return getInstance().getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return getInstance().getProperty(key, defaultValue);
    }

    public static Integer getInt(String key) {
        String val = get(key);
        return val != null ? Integer.parseInt(val) : null;
    }

    public static Integer getInt(String key, int defaultValue) {
        String val = get(key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public static Long getLong(String key) {
        String val = get(key);
        return val != null ? Long.parseLong(val) : null;
    }

    public static Boolean getBoolean(String key) {
        String val = get(key);
        return val != null ? Boolean.parseBoolean(val) : null;
    }

    // ==========================================
    // BASE URLS & URIs CHO API ENDPOINTS
    // ==========================================

    public static String getBaseUrlPromotion() {
        return getInstance().getProperty("BASE_URL_PROMOTION");
    }

    public static String getBaseUrlEcom() {
        return getInstance().getProperty("BASE_URL_ECOM");
    }

    public static String getRedirectUriEcom() {
        return getInstance().getProperty("REDIRECT_URI_ECOM");
    }

    public static String getBasePortalPromotion() {
        return getInstance().getProperty("BASE_PORTAL_PROMOTION");
    }

    public static String getBaseSso() {
        return getInstance().getProperty("BASE_SSO");
    }

    public static String getPromotionDbUri() {
        return getInstance().getProperty("PROMOTION_DB_URI");
    }
}
