# 🎯 Automation Test Framework - Selenium & SQLite

Framework tự động hóa kiểm thử End-to-End kết hợp **Selenium WebDriver** (kiểm thử giao diện Web theo mô hình Page Object Model), **Rest-Assured** (gọi & xác thực API), **SQLite JDBC** (quản lý test data và kết quả), **MongoDB Driver**, **TestNG** (quản lý lifecycle & thực thi song song qua `ThreadLocal`), **Allure Report** và **Telegram Bot** tự động gửi báo cáo.

---

## 📑 Mục lục

1. [Tổng quan](#-tổng-quan)
2. [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
3. [Cấu trúc dự án](#-cấu-trúc-dự-án)
4. [Cấu hình môi trường](#-cấu-hình-môi-trường)
5. [Cơ sở dữ liệu & Helpers](#-cơ-sở-dữ-liệu--helpers)
6. [Hướng dẫn cài đặt & Thực thi](#-hướng-dẫn-cài-đặt--thực-thi)
7. [Báo cáo kiểm thử](#-báo-cáo-kiểm-thử)
8. [Hướng dẫn phát triển & Mở rộng](#-hướng-dẫn-phát-triển--mở-rộng)

---

## 🔍 Tổng quan

Framework được thiết kế linh hoạt, hỗ trợ:
- **UI Automation (Selenium WebDriver)**: Quản lý trình duyệt (Chrome, Firefox, Edge, Headless) qua `DriverManager` dùng `ThreadLocal`, đảm bảo cô lập hoàn toàn giữa các luồng khi chạy song song.
- **Page Object Model (POM)**: Phân tách rõ ràng giữa định vị phần tử giao diện (`pageUI`) và phương thức xử lý nghiệp vụ (`pageObject`), kế thừa tiện ích mạnh mẽ từ `BasePage`.
- **API Testing (Rest-Assured)**: Tích hợp `ApiHelper` gửi request HTTP (GET, POST, PUT, DELETE) kèm token xác thực, tự động ghi nhận request/response vào Allure Report.
- **SQLite Test Data Management**: Hỗ trợ đọc/ghi dữ liệu kiểm thử linh hoạt, an toàn đa luồng bằng `SQLiteHelper` tích hợp `ReadWriteLock`.
- **Giám sát & Báo cáo tự động**: Tích hợp `TestListener` chụp ảnh màn hình khi kiểm thử thất bại, đính kèm Allure Report và tự động gửi thông báo kết quả tổng hợp về Telegram Bot.

---

## 🛠 Công nghệ sử dụng

| Công nghệ | Phiên bản | Vai trò |
| :--- | :--- | :--- |
| **Java** | 21 | Ngôn ngữ phát triển chính |
| **Selenium WebDriver** | 4.46.0 | Tự động hóa trình duyệt (Page Object Model) |
| **TestNG** | 7.10.2 | Test Runner, quản lý suite, test lifecycle và thực thi song song |
| **Rest Assured** | 4.5.1 | Gọi API và xác thực response payload / status code |
| **SQLite JDBC** | 3.45.1.0 | Thao tác cơ sở dữ liệu SQLite quản lý dữ liệu kiểm thử |
| **MongoDB Driver** | 4.10.2 | Kết nối & truy vấn dữ liệu từ MongoDB |
| **Awaitility** | 4.2.2 | Cơ chế polling & chờ điều kiện bất đồng bộ linh hoạt |
| **Allure Report** | 2.17.3 | Tạo báo cáo kiểm thử trực quan, đính kèm screenshot và API steps |
| **Log4j2 / SLF4J** | 2.24.0 | Hệ thống ghi log chi tiết phục vụ điều tra lỗi |
| **Telegram Bot API** | 6.9.1 / 6.1.0 | Tự động gửi thông báo kết quả kiểm thử về nhóm Telegram |
| **Jackson Databind** | 2.17.0 | Xử lý, đọc ghi và chuyển đổi cấu trúc JSON |
| **Lombok** | 1.18.38 | Giảm thiểu mã lặp (boilerplate code) |

---

## 📂 Cấu trúc dự án

```text
promotion-auto-sqlite/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── actions/
│   │   │   │   ├── common/             # Các lớp dùng chung cốt lõi
│   │   │   │   │   ├── BasePage.java       # Wrapper Selenium, tương tác UI, retry action, screenshot, Allure steps
│   │   │   │   │   ├── BaseTest.java       # TestNG base setup/teardown, khởi tạo WebDriver (Chrome/Firefox/Edge), headless
│   │   │   │   │   ├── ConfigLoader.java   # Nạp cấu hình theo môi trường (dev/uat) với cơ chế fallback thông minh
│   │   │   │   │   └── GlobalConstants.java# Hằng số toàn cục (môi trường mặc định, credentials, project path)
│   │   │   │   ├── database/           # Thao tác cơ sở dữ liệu
│   │   │   │   │   └── MongoDB.java        # Kết nối MongoDB, truy vấn thông tin dữ liệu từ collection
│   │   │   │   ├── drivers/            # Quản lý trình duyệt
│   │   │   │   │   └── DriverManager.java  # Quản lý WebDriver qua ThreadLocal hỗ trợ chạy song song đa luồng
│   │   │   │   ├── helpers/            # Tiện ích API, cơ sở dữ liệu, JSON và cơ chế chờ
│   │   │   │   │   ├── ApiHelper.java      # Wrapper Rest-Assured gửi request (GET, POST, PUT, DELETE), xác thực token
│   │   │   │   │   ├── JsonHelper.java     # Tiện ích thao tác và parse JSON (Jackson Engine)
│   │   │   │   │   ├── SQLiteHelper.java   # Quản lý kết nối & thao tác CRUD SQLite thread-safe (ReadWriteLock)
│   │   │   │   │   └── WaitHelper.java     # Polling và chờ bất đồng bộ linh hoạt dựa trên Awaitility
│   │   │   │   ├── listener/           # Lắng nghe sự kiện TestNG và thông báo
│   │   │   │   │   ├── TestListener.java   # Hook lifecycle test, đếm thống kê Pass/Fail, chụp ảnh đính kèm Allure
│   │   │   │   │   ├── PushTelgram.java    # Gửi tin nhắn và kết quả tổng kết lên Telegram Bot
│   │   │   │   │   └── VerificationFailures.java # Quản lý danh sách lỗi phát sinh phục vụ soft assertion
│   │   │   │   └── utils/              # Tiện ích bổ trợ
│   │   │   │       └── LogUtils.java       # Hệ thống ghi log Log4j2 chuẩn hóa (info, warn, error, debug)
│   │   │   ├── pageObject/             # Page Object Model - Chứa các phương thức xử lý nghiệp vụ trên từng trang
│   │   │   │   └── LoginOb.java        # Xử lý các thao tác và nghiệp vụ trên màn hình Đăng nhập (kế thừa BasePage)
│   │   │   └── pageUI/                 # Định nghĩa các Web Elements / Locators (XPath/CSS)
│   │   │       └── LoginUI.java        # Danh sách locator cho trang Đăng nhập
│   │   └── resources/
│   │       ├── config/                 # File cấu hình môi trường
│   │       │   ├── dev.properties      # Cấu hình môi trường DEV (URLs, credentials, API endpoints)
│   │       │   └── uat.properties      # Cấu hình môi trường UAT
│   │       ├── json_file/              # File dữ liệu cấu trúc JSON
│   │       │   └── masterdata.json     # Mapping mã viết tắt (COMP, NPP, SKU)
│   │       └── log4j2.properties       # Cấu hình định dạng và đường dẫn file log
│   └── test/
│       ├── java/
│       │   └── testcases/              # Kịch bản kiểm thử (TestNG Test Classes)
│       │       ├── ecoCardPortal/      # Nhóm kiểm thử chức năng Eco Card Portal
│       │       │   └── LoginTest.java  # Test case kiểm thử đăng nhập hệ thống (kế thừa BaseTest)
│       │       └── test1.java          # Test class phục vụ chạy thử nghiệm
│       └── resources/
│           ├── test-data/              # Lưu trữ cơ sở dữ liệu SQLite (.db) và file test data cho kịch bản tự động
│           └── test-suites/            # File cấu hình TestNG Suite XML
│               └── RegresstionTest.xml # File Suite XML chạy kiểm thử hồi quy & cấu hình thực thi song song
├── pom.xml                             # Quản lý thư viện Maven dependencies và cấu hình build plugins
└── README.md                           # Tài liệu hướng dẫn chi tiết framework
```

---

## ⚙ Cấu hình môi trường

### 1. Cơ chế ConfigLoader & Thứ tự ưu tiên môi trường
Hệ thống sử dụng **[ConfigLoader](src/main/java/actions/common/ConfigLoader.java)** để nạp cấu hình môi trường tương ứng từ thư mục `src/main/resources/config/` (`dev.properties` hoặc `uat.properties`).

Thứ tự ưu tiên phân giải môi trường:
1. **Ưu tiên 1 (Cao nhất): Cấu hình trong file XML Suite**:
   ```xml
   <parameter name="env" value="uat"/>  <!-- hoặc dev -->
   ```
2. **Ưu tiên 2: Tham số dòng lệnh Maven / JVM**:
   ```bash
   mvn test -Denv=dev    # Ghi đè môi trường khi chạy lệnh
   ```
3. **Ưu tiên 3 (Mặc định): Giá trị từ `GlobalConstants.ENV`**:
   Nếu file XML không truyền `env` và dòng lệnh không có `-Denv`, hệ thống tự động fallback về giá trị mặc định được định nghĩa trong `GlobalConstants.ENV` (mặc định: `uat`).

**Cách truy xuất cấu hình trong mã nguồn:**
```java
String username = ConfigLoader.get("USER_NAME_PORTAL");
String password = ConfigLoader.get("PASSWORD_PORTAL");
String baseUrl  = ConfigLoader.get("BASE_URL_ECO_CARD_PORTAL");
```

### 2. Danh mục các Key cấu hình chính

| Key cấu hình | Mô tả |
| :--- | :--- |
| `USER_NAME_PORTAL` | Tài khoản đăng nhập hệ thống Portal |
| `PASSWORD_PORTAL` | Mật khẩu đăng nhập hệ thống Portal |
| `BASE_URL_ECO_CARD_PORTAL` | URL gốc của cổng dịch vụ (Eco Card Portal) |

---

## 💾 Cơ sở dữ liệu & Helpers

### 1. SQLite Database (`SQLiteHelper.java`)
- **Quản lý kết nối an toàn đa luồng:** Mỗi luồng kiểm thử sở hữu một kết nối độc lập qua `ThreadLocal<Connection>` kết hợp PRAGMA `busy_timeout` để tránh race condition khi chạy song song.
- **Đồng bộ với ReadWriteLock:** Phân tách khóa đọc (`readLock`) khi truy vấn dữ liệu và khóa ghi (`writeLock`) khi cập nhật dữ liệu.
- **Tiện ích hỗ trợ:**
  - `getCellData(String columnName, int rowIndex)`: Đọc giá trị ô theo tên cột và chỉ số dòng.
  - `getTableData()`: Đọc toàn bộ bảng dữ liệu dưới dạng `List<Map<String, String>>`.
  - `setCellData(String value, int rowIndex, String columnName)`: Cập nhật giá trị ô theo chỉ số dòng.
  - `setCellDataByStt(String value, int stt, String columnName)`: Cập nhật theo cột STT.
  - `switchTable(String newTableName)`: Chuyển đổi bảng thao tác trong database.
  - `copyFile(String source, String target)`: Hỗ trợ sao lưu file `.db`.
- Thư mục lưu trữ file database: `src/test/resources/test-data/`.

### 2. MongoDB Helper (`MongoDB.java`)
- Khởi tạo và lưu cache connection pool (`MongoClient`) qua `ConcurrentHashMap`.
- Cung cấp hàm `connectMongoDbAndGetData` hỗ trợ retry tự động, truy vấn theo điều kiện filter và trích xuất dữ liệu từ Document/Collection.

### 3. Rest-Assured API Helper (`ApiHelper.java`)
- Chuẩn hóa việc gửi các HTTP method (GET, POST, PUT, DELETE) với `RequestSpecification`, JSON Body và Bearer Token.
- Tự động đính kèm thông tin request/response vào báo cáo Allure Report phục vụ tra cứu.

### 4. Polling & Chờ bất đồng bộ (`WaitHelper.java`)
- Cung cấp các hàm `waitUntil` dựa trên thư viện **Awaitility** để chờ điều kiện mà không cần gọi `Thread.sleep` cứng, tối ưu thời gian thực thi.

### 5. Master Data (`masterdata.json`)
- Tọa lạc tại `src/main/resources/json_file/masterdata.json`, lưu trữ cấu trúc JSON ánh xạ ID thực tế của công ty, nhà phân phối, mã SKU và ngành hàng.

---

## 🚀 Hướng dẫn cài đặt & Thực thi

### 1. Yêu cầu hệ thống
- **Java JDK 21**
- **Maven 3.8+**
- **Google Chrome / Mozilla Firefox / Microsoft Edge** (Selenium Manager tự động điều phối driver)

### 2. Cài đặt & Build dự án
```bash
git clone https://gitlab.finviet.com.vn/promotions/promotion-auto-sqlite.git
mvn clean install -DskipTests
```

### 3. Lệnh chạy kiểm thử qua Maven

Kiểm thử được thực thi thông qua các file TestNG Suite XML trong thư mục `src/test/resources/test-suites/`:

```bash
# 1. Chạy suite kiểm thử hồi quy trên môi trường UAT
mvn test -Denv=uat -DsuiteXmlFile=src/test/resources/test-suites/RegresstionTest.xml

# 2. Chạy trên môi trường DEV kèm chế độ Headless
mvn test -Denv=dev -DHEADLESS=true -DsuiteXmlFile=src/test/resources/test-suites/RegresstionTest.xml

# 3. Lựa chọn trình duyệt (chrome, firefox, edge)
mvn test -Denv=uat -DBROWSER=firefox -DsuiteXmlFile=src/test/resources/test-suites/RegresstionTest.xml
```

### 4. Các tham số điều khiển trong TestNG Suite XML

| Tham số | Giá trị | Mô tả |
| :--- | :--- | :--- |
| `env` | `uat` / `dev` | Môi trường kiểm thử mục tiêu |
| `BROWSER` | `chrome` / `firefox` / `edge` | Trình duyệt thực thi UI (mặc định: `chrome`) |
| `HEADLESS` | `true` / `false` | Chạy trình duyệt ẩn (khuyến nghị `true` cho môi trường CI/CD) |

---

## 📊 Báo cáo kiểm thử

1. **Allure Report:**
   ```bash
   allure serve allure-results
   ```
   Tự động chụp và đính kèm ảnh màn hình vào report khi test case bị Failed thông qua `TestListener`.
2. **Telegram Notification:**
   Sau khi suite hoàn thành, `TestListener` tự động tổng hợp số lượng Passed, Failed, Skipped, tỷ lệ Pass Rate và thời gian thực thi gửi về nhóm Telegram qua `PushTelgram`.
3. **Log hệ thống (Log4j2):**
   Ghi log chi tiết ra console và file log theo cấu hình tại `src/main/resources/log4j2.properties`.

---

## 🛠 Hướng dẫn phát triển & Mở rộng

### 1. Thêm mới màn hình kiểm thử (Page Object Model)
1. **Khai báo Element Locators**: Tạo class trong package `src/main/java/pageUI/` (ví dụ: `HomeUI.java`), định nghĩa các selector bằng XPath hoặc CSS.
2. **Tạo Page Object**: Tạo class trong package `src/main/java/pageObject/` (ví dụ: `HomePO.java`) kế thừa `BasePage` để viết các hàm tương tác nghiệp vụ.

### 2. Thêm mới kịch bản kiểm thử (Test Case)
1. Tạo class kiểm thử trong `src/test/java/testcases/` kế thừa `BaseTest`.
2. Viết các method kiểm thử với annotation `@Test` của TestNG.
3. Cấu hình class test vào file Suite XML trong `src/test/resources/test-suites/` (ví dụ: `RegresstionTest.xml`).

### 3. Cấu hình chạy song song (Parallel Execution)
Để tăng tốc độ thực thi, có thể cấu hình thuộc tính `parallel` trong file TestNG XML:
```xml
<suite name="Regression Suite" parallel="classes" thread-count="2">
    ...
</suite>
```
`DriverManager` và `SQLiteHelper` đã được xây dựng an toàn đa luồng (`ThreadLocal`), đảm bảo các test class/method chạy độc lập mà không gây xung đột dữ liệu hay trình duyệt.

---

## 👥 Tác giả & Quản trị

- **Kim Anh**
- **Repository:*https://github.com/anh1314/SeleniumJavaFw* ``