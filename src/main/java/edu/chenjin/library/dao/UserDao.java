package edu.chenjin.library.dao;

import edu.chenjin.jdbc.util.DBUtil;
import edu.chenjin.jdbc.util.JDBCTool;
import edu.chenjin.library.entity.Admin;
import edu.chenjin.library.entity.Operator;
import edu.chenjin.library.entity.Student;
import edu.chenjin.library.entity.Teacher;
import edu.chenjin.library.entity.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户数据访问。
 */
public class UserDao {

    private Class<? extends User> classForRole(String role) {
        if ("管理员".equals(role)) {
            return Admin.class;
        }
        if ("操作员".equals(role)) {
            return Operator.class;
        }
        if ("教师".equals(role)) {
            return Teacher.class;
        }
        return Student.class;
    }

    public User findById(String id) {
        try (Connection connection = DBUtil.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败", e);
        }
    }

    public User findById(Connection connection, String id) {
        String role = Sql.queryString(connection, "SELECT role FROM user WHERE id = ?", id);
        if (role == null) {
            return null;
        }
        List<? extends User> users = JDBCTool.queryList(connection,
                "SELECT * FROM user WHERE id = ?", classForRole(role), id);
        return users.isEmpty() ? null : users.get(0);
    }

    public User findByNameAndPassword(String name, String password) {
        try (Connection connection = DBUtil.getConnection()) {
            String id = Sql.queryString(connection,
                    "SELECT id FROM user WHERE name = ? AND password = ?", name, password);
            if (id == null) {
                return null;
            }
            return findById(connection, id);
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败", e);
        }
    }

    public boolean existsByName(String name) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryLong(connection, "SELECT COUNT(*) FROM user WHERE name = ?", name) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败", e);
        }
    }

    public boolean existsByPhone(String phone) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryLong(connection, "SELECT COUNT(*) FROM user WHERE phone = ?", phone) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败", e);
        }
    }

    public int insert(User user) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.save(user, connection);
        } catch (SQLException e) {
            throw new RuntimeException("新增用户失败", e);
        }
    }

    public int updateLoginTime(String id, LocalDateTime time) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE user SET loginTime = ?, updateTime = ? WHERE id = ?", time, time, id);
        } catch (SQLException e) {
            throw new RuntimeException("更新登录时间失败", e);
        }
    }

    /**
     * 模糊查询用户，按创建时间倒序。关键字为空时返回全部。
     */
    public List<User> search(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection connection = DBUtil.getConnection()) {
            List<Map<String, Object>> rows = Sql.queryMaps(connection,
                    "SELECT id FROM user WHERE name LIKE ? OR phone LIKE ? OR major LIKE ? OR role LIKE ? "
                            + "ORDER BY createTime DESC",
                    like, like, like, like);
            List<User> users = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                User user = findById(connection, String.valueOf(row.get("id")));
                if (user != null) {
                    users.add(user);
                }
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败", e);
        }
    }
}
