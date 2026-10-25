package edu.chenjin.library.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 SQL 辅助类：面向报表/联表查询返回 Map 列表，或取单值。
 */
public final class Sql {

    private Sql() {
    }

    public static List<Map<String, Object>> queryMaps(Connection connection, String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= count; i++) {
                        String label = meta.getColumnLabel(i);
                        row.put(label, normalize(rs.getObject(i)));
                    }
                    list.add(row);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败: " + sql, e);
        }
    }

    public static String queryString(Connection connection, String sql, Object... params) {
        List<Map<String, Object>> rows = queryMaps(connection, sql, params);
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0).values().iterator().next();
        return value == null ? null : String.valueOf(value);
    }

    public static long queryLong(Connection connection, String sql, Object... params) {
        String value = queryString(connection, sql, params);
        return value == null ? 0L : Long.parseLong(value);
    }

    public static int queryInt(Connection connection, String sql, Object... params) {
        return (int) queryLong(connection, sql, params);
    }

    private static Object normalize(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof byte[] bytes) {
            return bytes.length > 0 && bytes[0] != 0;
        }
        return value;
    }

    public static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (value instanceof LocalDate localDate) {
                ps.setDate(i + 1, Date.valueOf(localDate));
            } else if (value instanceof LocalDateTime localDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf(localDateTime));
            } else if (value instanceof LocalTime localTime) {
                ps.setTime(i + 1, Time.valueOf(localTime));
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }
}
