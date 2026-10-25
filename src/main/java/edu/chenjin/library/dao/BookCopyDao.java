package edu.chenjin.library.dao;

import edu.chenjin.jdbc.util.DBUtil;
import edu.chenjin.jdbc.util.JDBCTool;
import edu.chenjin.library.entity.BookCopy;
import edu.chenjin.library.util.IdUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 图书副本数据访问。
 */
public class BookCopyDao {

    public List<Map<String, Object>> listByBook(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryMaps(connection,
                    "SELECT id, bookId, isBorrowed, createTime FROM bookcopy WHERE bookId = ? ORDER BY createTime, id",
                    bookId);
        } catch (SQLException e) {
            throw new RuntimeException("查询图书副本失败", e);
        }
    }

    public BookCopy findById(String id) {
        try (Connection connection = DBUtil.getConnection()) {
            List<BookCopy> copies = JDBCTool.queryList(connection,
                    "SELECT * FROM bookcopy WHERE id = ?", BookCopy.class, id);
            return copies.isEmpty() ? null : copies.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("查询图书副本失败", e);
        }
    }

    /**
     * 新增一个副本，并把图书库存 +1（同一事务）。
     */
    public BookCopy addCopy(String bookId) {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BookCopy copy = new BookCopy();
                copy.setId(IdUtil.uuid());
                copy.setCreateTime(now);
                copy.setUpdateTime(now);
                copy.setBookId(bookId);
                copy.setBorrowed(false);
                JDBCTool.save(copy, connection);
                JDBCTool.executeUpdate(connection,
                        "UPDATE book SET stock = stock + 1, updateTime = ? WHERE id = ?", now, bookId);
                connection.commit();
                return copy;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("新增图书副本失败", e);
        }
    }

    /**
     * 删除一个副本，并把图书库存 -1（同一事务）。已借出的副本不允许删除。
     */
    public void deleteCopy(String copyId) {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Map<String, Object>> rows = Sql.queryMaps(connection,
                        "SELECT bookId, isBorrowed FROM bookcopy WHERE id = ?", copyId);
                if (rows.isEmpty()) {
                    throw new IllegalArgumentException("副本不存在");
                }
                Map<String, Object> row = rows.get(0);
                Object borrowed = row.get("isBorrowed");
                boolean isBorrowed = Boolean.TRUE.equals(borrowed)
                        || (borrowed instanceof Number number && number.intValue() != 0);
                if (isBorrowed) {
                    throw new IllegalStateException("该副本处于借阅状态，不能删除");
                }
                String bookId = String.valueOf(row.get("bookId"));
                JDBCTool.executeUpdate(connection, "DELETE FROM bookcopy WHERE id = ?", copyId);
                JDBCTool.executeUpdate(connection,
                        "UPDATE book SET stock = stock - 1, updateTime = ? WHERE id = ? AND stock > 0", now, bookId);
                connection.commit();
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("删除图书副本失败", e);
        }
    }
}
