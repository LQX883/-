package edu.chenjin.library.dao;

import edu.chenjin.jdbc.util.DBUtil;
import edu.chenjin.jdbc.util.JDBCTool;
import edu.chenjin.library.entity.Book;
import edu.chenjin.library.entity.BookCopy;
import edu.chenjin.library.util.IdUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图书数据访问。
 */
public class BookDao {

    public Book findById(String id) {
        try (Connection connection = DBUtil.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new RuntimeException("查询图书失败", e);
        }
    }

    public Book findById(Connection connection, String id) {
        List<Book> books = JDBCTool.queryList(connection,
                "SELECT * FROM book WHERE id = ?", Book.class, id);
        return books.isEmpty() ? null : books.get(0);
    }

    public List<Book> search(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.queryList(connection,
                    "SELECT * FROM book WHERE title LIKE ? OR author LIKE ? OR type LIKE ? OR publisher LIKE ? "
                            + "ORDER BY createTime DESC",
                    Book.class, like, like, like, like);
        } catch (SQLException e) {
            throw new RuntimeException("查询图书失败", e);
        }
    }

    /**
     * 图书列表（含可借副本数），按创建时间倒序。
     */
    public List<Map<String, Object>> listWithAvailable(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT b.id, b.createTime, b.updateTime, b.title, b.author, b.type, b.publisher, "
                + "b.days, b.price, b.stock, b.status, b.introduction, "
                + "(SELECT COUNT(*) FROM bookcopy bc WHERE bc.bookId = b.id AND bc.isBorrowed = 0) AS available "
                + "FROM book b WHERE b.title LIKE ? OR b.author LIKE ? OR b.type LIKE ? OR b.publisher LIKE ? "
                + "ORDER BY b.createTime DESC";
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryMaps(connection, sql, like, like, like, like);
        } catch (SQLException e) {
            throw new RuntimeException("查询图书失败", e);
        }
    }

    /**
     * 可借阅图书列表：状态正常且存在未借出副本。
     */
    public List<Map<String, Object>> listBorrowable(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT b.id, b.title, b.author, b.type, b.publisher, b.days, b.price, b.stock, b.status, "
                + "(SELECT COUNT(*) FROM bookcopy bc WHERE bc.bookId = b.id AND bc.isBorrowed = 0) AS available "
                + "FROM book b WHERE b.status = '正常' AND (b.title LIKE ? OR b.author LIKE ? OR b.type LIKE ?) "
                + "HAVING available > 0 ORDER BY b.createTime DESC";
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryMaps(connection, sql, like, like, like);
        } catch (SQLException e) {
            throw new RuntimeException("查询可借图书失败", e);
        }
    }

    /**
     * 新增图书，并按 stock 自动生成对应数量的副本（同一事务）。
     */
    public Book insertWithCopies(Book book) {
        LocalDateTime now = LocalDateTime.now();
        book.setCreateTime(now);
        book.setUpdateTime(now);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                JDBCTool.save(book, connection);
                int stock = book.getStock() == null ? 0 : book.getStock();
                for (int i = 0; i < stock; i++) {
                    BookCopy copy = new BookCopy();
                    copy.setId(IdUtil.uuid());
                    copy.setCreateTime(now);
                    copy.setUpdateTime(now);
                    copy.setBookId(book.getId());
                    copy.setBorrowed(false);
                    JDBCTool.save(copy, connection);
                }
                connection.commit();
                return book;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("新增图书失败", e);
        }
    }

    public int updateFields(Book book) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE book SET title = ?, author = ?, type = ?, publisher = ?, days = ?, price = ?, "
                            + "updateTime = ? WHERE id = ?",
                    book.getTitle(), book.getAuthor(), book.getType(), book.getPublisher(),
                    book.getDays(), book.getPrice(), LocalDateTime.now(), book.getId());
        } catch (SQLException e) {
            throw new RuntimeException("编辑图书失败", e);
        }
    }

    public int updateStatus(String id, String status) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE book SET status = ?, updateTime = ? WHERE id = ?",
                    status, LocalDateTime.now(), id);
        } catch (SQLException e) {
            throw new RuntimeException("更新图书状态失败", e);
        }
    }

    public int increaseStock(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE book SET stock = stock + 1, updateTime = ? WHERE id = ?",
                    LocalDateTime.now(), bookId);
        } catch (SQLException e) {
            throw new RuntimeException("更新库存失败", e);
        }
    }

    public int decreaseStock(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.executeUpdate(connection,
                    "UPDATE book SET stock = stock - 1, updateTime = ? WHERE id = ? AND stock > 0",
                    LocalDateTime.now(), bookId);
        } catch (SQLException e) {
            throw new RuntimeException("更新库存失败", e);
        }
    }

    public int countAvailable(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryInt(connection,
                    "SELECT COUNT(*) FROM bookcopy WHERE bookId = ? AND isBorrowed = 0", bookId);
        } catch (SQLException e) {
            throw new RuntimeException("统计可借副本失败", e);
        }
    }

    public int countBorrowed(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryInt(connection,
                    "SELECT COUNT(*) FROM bookcopy WHERE bookId = ? AND isBorrowed = 1", bookId);
        } catch (SQLException e) {
            throw new RuntimeException("统计借出副本失败", e);
        }
    }

    public List<String> allTypes() {
        try (Connection connection = DBUtil.getConnection()) {
            List<Map<String, Object>> rows = Sql.queryMaps(connection,
                    "SELECT DISTINCT type FROM book WHERE type IS NOT NULL AND type <> '' ORDER BY type");
            List<String> types = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                types.add(String.valueOf(row.get("type")));
            }
            return types;
        } catch (SQLException e) {
            throw new RuntimeException("查询图书类型失败", e);
        }
    }

    public boolean existsByTitle(String title) {
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryLong(connection,
                    "SELECT COUNT(*) FROM book WHERE title = ?", title) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("查询图书失败", e);
        }
    }
}
