package actions.helpers;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static actions.common.BasePage.*;

public class SQLiteHelper {

    private final String dbFilePath;

    /**
     * table/tableName/columns là state dùng chung của instance,
     * nên phải được bảo vệ bằng lock.
     */
    private volatile String table;

    @Getter
    private volatile String tableName;

    private final Map<String, Integer> columns = new HashMap<>();

    /**
     * Mỗi thread dùng một Connection riêng để tránh race condition khi parallel methods.
     */
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    /**
     * Bảo vệ toàn bộ state dùng chung của instance.
     * - Read lock: các hàm đọc dữ liệu
     * - Write lock: init/switch/close/update state
     */
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock(true);

    public SQLiteHelper(String dbFilePath, String tableName) {
        this.dbFilePath = dbFilePath;
        this.tableName = tableName;
        initDatabase(tableName);
    }

    // ========== INIT ==========

    private Connection getConnection() throws SQLException {
        Connection connection = connectionHolder.get();
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000");
            }
            connectionHolder.set(connection);
        }
        return connection;
    }

    public void initDatabase(String tableName) {
        stateLock.writeLock().lock();
        try {
            this.table = tableName;

            Connection conn = getConnection();
            DatabaseMetaData meta = conn.getMetaData();

            try (ResultSet tableRs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
                if (!tableRs.next()) {
                    throw new RuntimeException("Table not found: " + tableName);
                }
            }

            columns.clear();

            try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
                int index = 0;
                while (rs.next()) {
                    columns.put(rs.getString("COLUMN_NAME"), index++);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Init DB error: " + e.getMessage(), e);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    // ========== BACKUP DB ==========

    public void copyFile(String source_FilePath, String target_Directory) {
        try {
            Path sourcePath = Paths.get(source_FilePath);
            String fileName = sourcePath.getFileName().toString();
            Path targetPath = Paths.get(target_Directory, fileName);

            Path parentDirectory = targetPath.getParent();
            if (parentDirectory != null && !Files.exists(parentDirectory)) {
                Files.createDirectories(parentDirectory);
                logInfo("📁 Created folder: {}", parentDirectory);
            }

            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logInfo("💾 Copy file success → " + fileName +
                    "\n---------------------------------------------------------------------------------------");
        } catch (IOException e) {
            errorMsg("Copy file error: {}", e.getMessage());
        }
    }

    // ========== GET DATA ==========

    public String getCellData(int rowIndex, int columnIndex) {
        stateLock.readLock().lock();
        try {
            String sql = "SELECT * FROM " + table + " LIMIT 1 OFFSET ?";

            try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
                preparedStatement.setInt(1, rowIndex);

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(columnIndex + 1);
                    }
                }
            }
        } catch (Exception e) {
            logError("Get cell data error: {}", e.getMessage());
        } finally {
            stateLock.readLock().unlock();
        }

        return "";
    }

    public String getCellData(String columnName, int rowIndex) {
        stateLock.readLock().lock();
        try {
            Integer colIndex = columns.get(columnName);
            if (colIndex == null) return "";
            return getCellData(rowIndex, colIndex);
        } finally {
            stateLock.readLock().unlock();
        }
    }


    public List<Map<String, String>> getTableData() {
        stateLock.readLock().lock();
        try {
            List<Map<String, String>> data = new ArrayList<>();
            String sql = "SELECT * FROM " + table;

            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                int index = 0;
                while (rs.next()) {
                    Map<String, String> rowData = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        rowData.put(meta.getColumnName(i), rs.getString(i));
                    }
                    rowData.put("index", String.valueOf(index++));
                    data.add(rowData);
                }
            } catch (Exception e) {
                logError("Read table error: " + e.getMessage());
            }

            return data;
        } finally {
            stateLock.readLock().unlock();
        }
    }


    private List<String> getExistingColumns(String... columnNames) {
        stateLock.readLock().lock();
        try {
            List<String> result = new ArrayList<>();
            String sql = "PRAGMA table_info(" + table + ")";

            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                Set<String> existingCols = new HashSet<>();
                while (rs.next()) {
                    String colName = rs.getString("name");
                    if (!isValueNullOrEmpty(colName)) {
                        existingCols.add(colName.toLowerCase());
                    }
                }

                for (String col : columnNames) {
                    if (existingCols.contains(col.toLowerCase())) {
                        result.add(col);
                    }
                }

            } catch (Exception e) {
                logError("Get existing columns error: {}", e.getMessage());
            }

            return result;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    public static String quote(String... columns) {
        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("At least one column is required");
        }

        return Arrays.stream(columns)
                .map(col -> {
                    if (isValueNullOrEmpty(col)) {
                        throw new IllegalArgumentException("Column name must not be null or empty");
                    }
                    return "\"" + col.replace("\"", "\"\"") + "\"";
                })
                .collect(Collectors.joining(", "));
    }

    // ========== WRITE ==========

    public void setCellData(String value, int rowIndex, String columnName) {
        stateLock.writeLock().lock();
        try {
            String sql = "UPDATE " + table + " SET " + quote(columnName) + " = ? WHERE rowid = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, value);
                ps.setInt(2, rowIndex + 1);

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    logError("No row updated for column {} at rowIndex {}", columnName, rowIndex);
                }
            }
        } catch (SQLException e) {
            logError("Failed to update column {} | error: {}", columnName, e.getMessage());
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public void setCellDataByStt(String value, int stt, String columnName) {
        stateLock.writeLock().lock();
        try {
            String sql = "UPDATE " + table + " SET " + quote(columnName) + " = ?" + " WHERE STT = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, value);
                ps.setInt(2, stt);

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    logError("No row updated for column {} at stt {}", columnName, stt);
                }
            }
        } catch (SQLException e) {
            logError("Failed to update column {} | error: {}", columnName, e.getMessage());
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public void executeUpdate(String sql) {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException("Cannot execute SQL: " + sql, e);
        }
    }

    // ========== FIND ==========

    public List<Integer> findPositions(int columnIndex, int targetValue) {
        stateLock.readLock().lock();
        try {
            List<Integer> positions = new ArrayList<>();

            try {
                String colName = getColumnNameByIndex(columnIndex);
                String sql = "SELECT rowid FROM " + table + " WHERE " + colName + " = ?";

                try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
                    preparedStatement.setInt(1, targetValue);

                    try (ResultSet rs = preparedStatement.executeQuery()) {
                        while (rs.next()) {
                            positions.add(rs.getInt("rowid") - 1);
                        }
                    }
                }
            } catch (Exception e) {
                logError("Find position error: {}", e.getMessage());
            }

            return positions;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    // ========== DELETE ==========

    public void deleteByCondition(Set<Integer> ids, List<String> columnsToDelete) {
        stateLock.writeLock().lock();
        try {
            if (columnsToDelete == null || columnsToDelete.isEmpty()) {
                logInfo("⚠️ No columns to update");
                return;
            }

            String setClause = columnsToDelete.stream().map(col -> quote(col) + " = NULL").collect(Collectors.joining(", "));
            String sql = "UPDATE " + table + " SET " + setClause + " WHERE rowid = ?";

            try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
                for (Integer id : ids) {
                    preparedStatement.setInt(1, id + 1);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }

        } catch (Exception e) {
            errorMsg("Update column to NULL error: {}", e.getMessage());
        } finally {
            stateLock.writeLock().unlock();
        }
    }




    // ========== UTIL ==========

    public String getColumnNameByIndex(int index) {
        stateLock.readLock().lock();
        try {
            for (Map.Entry<String, Integer> entry : columns.entrySet()) {
                if (entry.getValue() == index) {
                    return entry.getKey();
                }
            }
            return null;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    public int getColumnIndexByName(String columnName) {
        stateLock.readLock().lock();
        try {
            if (columnName == null || columnName.trim().isEmpty()) {
                return -1;
            }

            Integer index = columns.get(columnName);
            if (index == null) {
                logInfo("⚠️ Column not found: " + columnName + " in table: " + tableName);
                return -1;
            }

            return index;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    // ========== CLOSE ==========

    public void closeConnection() {
        stateLock.writeLock().lock();
        try {
            Connection connection = connectionHolder.get();
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logInfo("🔒 Closed DB connection");
            }
            connectionHolder.remove();
        } catch (SQLException e) {
            throw new RuntimeException("Close DB connection error: " + e.getMessage(), e);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public void switchTable(String newTableName) {
        stateLock.writeLock().lock();
        try {
            if (newTableName == null || newTableName.trim().isEmpty()) {
                throw new IllegalArgumentException("Table name is invalid");
            }

            DatabaseMetaData meta = getConnection().getMetaData();

            try (ResultSet rs = meta.getTables(null, null, newTableName, null)) {
                if (!rs.next()) {
                    throw new RuntimeException("Table does not exist: " + newTableName);
                }
            }

            this.tableName = newTableName;
            this.table = newTableName;
            columns.clear();

            try (ResultSet colRs = meta.getColumns(null, null, newTableName, null)) {
                int index = 0;
                while (colRs.next()) {
                    columns.put(colRs.getString("COLUMN_NAME"), index++);
                }
            }

            logInfo("🔄 Switched to table: {}", newTableName);

        } catch (Exception e) {
            logError("Switch table error: {}", e.getMessage());
        } finally {
            stateLock.writeLock().unlock();
        }
    }




    public boolean isRecordExist(String sql) {
        try (Statement statement = getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();

        } catch (Exception e) {
            throw new RuntimeException("Cannot execute exists query: " + sql, e);
        }
    }
}

