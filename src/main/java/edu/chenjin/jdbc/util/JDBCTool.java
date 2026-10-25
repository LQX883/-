package edu.chenjin.jdbc.util;

import edu.chenjin.jdbc.annotation.Column;
import edu.chenjin.jdbc.annotation.Id;
import edu.chenjin.jdbc.annotation.Table;
import edu.chenjin.jdbc.annotation.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 基于 JDBC + 反射的简易 ORM 工具。
 * <p>
 * 核心思想：用注解描述「类 ↔ 表、属性 ↔ 列」的映射关系，
 * 运行时通过反射创建对象、读写字段，从而把 ResultSet 转成 Java 对象，
 * 并把 Java 对象转成 INSERT / UPDATE / DELETE SQL。
 * <p>
 * 来自作业1《JDBC工具的设计与开发》，为支持实体继承关系做了增强：
 * 字段收集会沿继承链向上遍历父类（Base → User → Admin），
 * 并支持 {@link Transient} 标注的非数据库字段。
 */
public final class JDBCTool {

    private JDBCTool() {
    }

    /**
     * 将 ResultSet 中的每一行映射为目标类的一个实例。
     * 列名与属性通过 {@link Column} 注解或驼峰转下划线规则对应。
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        if (rs == null || clazz == null) {
            return list;
        }
        try {
            Map<String, Integer> columnIndex = buildColumnIndex(rs.getMetaData());
            List<Field> fields = getAllFields(clazz);
            while (rs.next()) {
                T instance = clazz.getDeclaredConstructor().newInstance();
                for (Field field : fields) {
                    String columnName = resolveColumnName(field);
                    Integer index = columnIndex.get(columnName.toLowerCase(Locale.ROOT));
                    if (index == null) {
                        continue;
                    }
                    Object raw = rs.getObject(index);
                    Object converted = convertValue(raw, field.getType());
                    field.setAccessible(true);
                    field.set(instance, converted);
                }
                list.add(instance);
            }
        } catch (Exception e) {
            throw new RuntimeException("ResultSet 映射为 " + clazz.getSimpleName() + " 失败", e);
        }
        return list;
    }

    /**
     * 插入一条记录。返回受影响行数。
     */
    public static <T> int save(T obj, Connection connection) {
        if (obj == null) {
            throw new IllegalArgumentException("待保存对象不能为空");
        }
        Class<?> clazz = obj.getClass();
        String table = resolveTableName(clazz);
        List<Field> fields = getAllFields(clazz);

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (i > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(resolveColumnName(field));
            placeholders.append("?");
            values.add(readField(obj, field));
        }

        String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
        return executeUpdate(connection, sql, values.toArray());
    }

    /**
     * 按主键更新一条记录。返回受影响行数。
     */
    public static <T> int update(T obj, Connection connection) {
        if (obj == null) {
            throw new IllegalArgumentException("待更新对象不能为空");
        }
        Class<?> clazz = obj.getClass();
        String table = resolveTableName(clazz);
        Field idField = resolveIdField(clazz);
        Object idValue = readField(obj, idField);
        if (idValue == null) {
            throw new IllegalArgumentException(clazz.getSimpleName() + " 的主键为空，无法更新");
        }

        StringBuilder setClause = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Field field : getAllFields(clazz)) {
            if (field.equals(idField)) {
                continue;
            }
            if (!setClause.isEmpty()) {
                setClause.append(", ");
            }
            setClause.append(resolveColumnName(field)).append(" = ?");
            values.add(readField(obj, field));
        }
        values.add(idValue);

        String sql = "UPDATE " + table + " SET " + setClause
                + " WHERE " + resolveColumnName(idField) + " = ?";
        return executeUpdate(connection, sql, values.toArray());
    }

    /**
     * 按主键删除一条记录。返回受影响行数。
     */
    public static <T> int delete(T obj, Connection connection) {
        if (obj == null) {
            throw new IllegalArgumentException("待删除对象不能为空");
        }
        Class<?> clazz = obj.getClass();
        Field idField = resolveIdField(clazz);
        Object idValue = readField(obj, idField);
        if (idValue == null) {
            throw new IllegalArgumentException(clazz.getSimpleName() + " 的主键为空，无法删除");
        }
        String sql = "DELETE FROM " + resolveTableName(clazz)
                + " WHERE " + resolveColumnName(idField) + " = ?";
        return executeUpdate(connection, sql, idValue);
    }

    /**
     * 按主键查询一条记录。找不到时返回 null。
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) {
        if (id == null || clazz == null) {
            throw new IllegalArgumentException("id 与 Class 均不能为空");
        }
        Field idField = resolveIdField(clazz);
        String sql = "SELECT * FROM " + resolveTableName(clazz)
                + " WHERE " + resolveColumnName(idField) + " = ?";
        List<T> list = queryList(connection, sql, clazz, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /* ======================== 辅助方法：统一数据库访问 ======================== */

    /**
     * 查询多条记录并映射为对象列表。
     */
    public static <T> List<T> queryList(Connection connection, String sql, Class<T> clazz, Object... params) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(sql);
            bindParameters(ps, params);
            rs = ps.executeQuery();
            return resultSetToList(rs, clazz);
        } catch (SQLException e) {
            throw new RuntimeException("查询失败: " + sql, e);
        } finally {
            DBUtil.close(rs, ps);
        }
    }

    /**
     * 查询某张表的全部记录。
     */
    public static <T> List<T> findAll(Class<T> clazz, Connection connection) {
        String sql = "SELECT * FROM " + resolveTableName(clazz);
        return queryList(connection, sql, clazz);
    }

    /**
     * 统一的增删改执行入口。
     */
    public static int executeUpdate(Connection connection, String sql, Object... params) {
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            bindParameters(ps, params);
            int rows = ps.executeUpdate();
            System.out.println("[SQL] " + sql + "  参数=" + java.util.Arrays.toString(params) + "  影响行数=" + rows);
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("执行更新失败: " + sql, e);
        } finally {
            DBUtil.close(ps);
        }
    }

    static String resolveTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.value().isBlank()) {
            return table.value();
        }
        return camelToUnderline(clazz.getSimpleName());
    }

    static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.value().isBlank()) {
            return column.value();
        }
        return camelToUnderline(field.getName());
    }

    /**
     * 沿继承链收集所有需要持久化的字段（父类字段在前，子类字段在后），
     * 跳过静态字段、合成字段以及 {@link Transient} 标注的字段。
     */
    static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Deque<Class<?>> stack = new ArrayDeque<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            stack.push(current);
            current = current.getSuperclass();
        }
        while (!stack.isEmpty()) {
            Class<?> type = stack.pop();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                fields.add(field);
            }
        }
        return fields;
    }

    static Field resolveIdField(Class<?> clazz) {
        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        try {
            return clazz.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(clazz.getSimpleName() + " 未标注 @Id，也没有名为 id 的字段");
        }
    }

    static Object readField(Object obj, Field field) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("读取字段失败: " + field.getName(), e);
        }
    }

    static void bindParameters(PreparedStatement ps, Object... params) throws SQLException {
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
            } else if (value instanceof Enum<?> enumeration) {
                ps.setString(i + 1, enumeration.name());
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    static Map<String, Integer> buildColumnIndex(ResultSetMetaData metaData) throws SQLException {
        Map<String, Integer> index = new HashMap<>();
        int count = metaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = metaData.getColumnLabel(i);
            if (label == null || label.isBlank()) {
                label = metaData.getColumnName(i);
            }
            index.put(label.toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    /**
     * 将 JDBC 取出的原始值转换成实体字段类型。
     */
    static Object convertValue(Object raw, Class<?> targetType) {
        if (raw == null) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) {
                    return false;
                }
                if (targetType == int.class || targetType == short.class || targetType == byte.class) {
                    return 0;
                }
                if (targetType == long.class) {
                    return 0L;
                }
                if (targetType == double.class) {
                    return 0D;
                }
                if (targetType == float.class) {
                    return 0F;
                }
            }
            return null;
        }
        if (targetType.isAssignableFrom(raw.getClass())) {
            return raw;
        }
        if ((targetType == Boolean.class || targetType == boolean.class) && raw instanceof byte[] bytes) {
            return bytes.length > 0 && bytes[0] != 0;
        }
        if (targetType == String.class) {
            return String.valueOf(raw);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return raw instanceof Number number ? number.intValue() : Integer.parseInt(raw.toString());
        }
        if (targetType == Long.class || targetType == long.class) {
            return raw instanceof Number number ? number.longValue() : Long.parseLong(raw.toString());
        }
        if (targetType == Double.class || targetType == double.class) {
            return raw instanceof Number number ? number.doubleValue() : Double.parseDouble(raw.toString());
        }
        if (targetType == Float.class || targetType == float.class) {
            return raw instanceof Number number ? number.floatValue() : Float.parseFloat(raw.toString());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (raw instanceof Boolean bool) {
                return bool;
            }
            if (raw instanceof Number number) {
                return number.intValue() != 0;
            }
            return Boolean.parseBoolean(raw.toString()) || "1".equals(raw.toString());
        }
        if (targetType == BigDecimal.class) {
            return raw instanceof BigDecimal bigDecimal ? bigDecimal : new BigDecimal(raw.toString());
        }
        if (targetType == LocalDate.class) {
            if (raw instanceof Date date) {
                return date.toLocalDate();
            }
            if (raw instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime().toLocalDate();
            }
            if (raw instanceof LocalDateTime localDateTime) {
                return localDateTime.toLocalDate();
            }
            return LocalDate.parse(raw.toString());
        }
        if (targetType == LocalDateTime.class) {
            if (raw instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime();
            }
            if (raw instanceof Date date) {
                return date.toLocalDate().atStartOfDay();
            }
            return LocalDateTime.parse(raw.toString().replace(' ', 'T'));
        }
        return raw;
    }

    /**
     * 驼峰转下划线，例如 enrollmentTime -> enrollment_time。
     */
    static String camelToUnderline(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
