package edu.chenjin.library.dao;

import edu.chenjin.jdbc.util.DBUtil;
import edu.chenjin.jdbc.util.JDBCTool;
import edu.chenjin.library.entity.BookRecommend;
import edu.chenjin.library.util.IdUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 图书推荐数据访问。
 */
public class RecommendDao {

    public BookRecommend findById(String id) {
        try (Connection connection = DBUtil.getConnection()) {
            List<BookRecommend> list = JDBCTool.queryList(connection,
                    "SELECT * FROM bookrecommend WHERE id = ?", BookRecommend.class, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("查询推荐失败", e);
        }
    }

    public BookRecommend insert(BookRecommend recommend) {
        LocalDateTime now = LocalDateTime.now();
        recommend.setId(IdUtil.uuid());
        recommend.setCreateTime(now);
        recommend.setUpdateTime(now);
        try (Connection connection = DBUtil.getConnection()) {
            JDBCTool.save(recommend, connection);
            return recommend;
        } catch (SQLException e) {
            throw new RuntimeException("提交推荐失败", e);
        }
    }

    public boolean existsPendingByTitle(String bookName) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryLong(connection,
                    "SELECT COUNT(*) FROM bookrecommend WHERE bookName = ? AND status = '待审核'",
                    bookName) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("查询推荐失败", e);
        }
    }

    public List<Map<String, Object>> listByStatus(String status) {
        String sql = "SELECT r.id, r.userId, r.bookName, r.author, r.publisher, r.price, r.status, r.reason, "
                + "r.createTime, u.name AS userName, u.role AS userRole "
                + "FROM bookrecommend r LEFT JOIN user u ON r.userId = u.id ";
        try (Connection connection = DBUtil.getConnection()) {
            if (status == null || status.isBlank()) {
                return Sql.queryMaps(connection, sql + "ORDER BY r.createTime DESC");
            }
            return Sql.queryMaps(connection, sql + "WHERE r.status = ? ORDER BY r.createTime DESC", status);
        } catch (SQLException e) {
            throw new RuntimeException("查询推荐失败", e);
        }
    }

    public List<Map<String, Object>> listByUser(String userId) {
        String sql = "SELECT r.id, r.userId, r.bookName, r.author, r.publisher, r.price, r.status, r.reason, "
                + "r.createTime, u.name AS userName, u.role AS userRole "
                + "FROM bookrecommend r LEFT JOIN user u ON r.userId = u.id "
                + "WHERE r.userId = ? ORDER BY r.createTime DESC";
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryMaps(connection, sql, userId);
        } catch (SQLException e) {
            throw new RuntimeException("查询推荐失败", e);
        }
    }

    public int updateStatus(String id, String status, String reason) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE bookrecommend SET status = ?, reason = ?, updateTime = ? WHERE id = ?",
                    status, reason, LocalDateTime.now(), id);
        } catch (SQLException e) {
            throw new RuntimeException("更新推荐状态失败", e);
        }
    }
}
