package edu.chenjin.library.dao;

import edu.chenjin.jdbc.util.DBUtil;
import edu.chenjin.jdbc.util.JDBCTool;
import edu.chenjin.library.entity.Loan;
import edu.chenjin.library.util.IdUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 借阅数据访问：借书、还书、借阅记录查询与统计。
 */
public class LoanDao {

    private static final String BASE_SELECT =
            "SELECT l.id, l.bookCopyId, l.userId, l.borrowDate, l.returnDate, l.dueDate, l.status, "
                    + "b.title AS bookTitle, b.author AS bookAuthor, b.type AS bookType, b.days AS bookDays, "
                    + "u.name AS userName, u.role AS userRole, u.major AS userMajor "
                    + "FROM loan l "
                    + "JOIN bookcopy bc ON l.bookCopyId = bc.id "
                    + "JOIN book b ON bc.bookId = b.id "
                    + "JOIN user u ON l.userId = u.id ";

    /**
     * 个人借阅记录。onlyUnreturned 为 true 时只返回未归还记录。
     */
    public List<Map<String, Object>> myLoans(String userId, boolean onlyUnreturned, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE l.userId = ? ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(userId);
        if (onlyUnreturned) {
            sql.append("AND l.returnDate IS NULL ");
        }
        if (start != null && end != null) {
            sql.append("AND l.borrowDate >= ? AND l.borrowDate < ? ");
            params.add(start.atStartOfDay());
            params.add(end.plusDays(1).atStartOfDay());
        }
        sql.append("ORDER BY l.borrowDate DESC");
        try (Connection connection = DBUtil.getConnection()) {
            List<Map<String, Object>> rows = Sql.queryMaps(connection, sql.toString(), params.toArray());
            decorateStatus(rows);
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("查询借阅记录失败", e);
        }
    }

    /**
     * 管理员借阅记录查询。
     * mode: keyword（关键字模糊）、overdue（超期）、date（借阅时间范围）。
     */
    public List<Map<String, Object>> search(String keyword, String mode, LocalDate start, LocalDate end) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if ("overdue".equals(mode)) {
            sql.append("WHERE ((l.status <> '已还' AND l.dueDate < NOW()) "
                    + "OR (l.returnDate IS NOT NULL AND l.returnDate > l.dueDate)) "
                    + "AND (u.name LIKE ? OR b.title LIKE ?) ");
            params.add(like);
            params.add(like);
        } else if ("date".equals(mode)) {
            sql.append("WHERE l.borrowDate >= ? AND l.borrowDate < ? ");
            params.add(start == null ? LocalDate.of(1970, 1, 1).atStartOfDay() : start.atStartOfDay());
            params.add(end == null ? LocalDate.now().plusDays(1).atStartOfDay() : end.plusDays(1).atStartOfDay());
        } else {
            sql.append("WHERE (u.name LIKE ? OR b.title LIKE ?) ");
            params.add(like);
            params.add(like);
        }
        sql.append("ORDER BY l.borrowDate DESC");
        try (Connection connection = DBUtil.getConnection()) {
            List<Map<String, Object>> rows = Sql.queryMaps(connection, sql.toString(), params.toArray());
            decorateStatus(rows);
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("查询借阅记录失败", e);
        }
    }

    /**
     * 借书：为用户分配一个可借副本并生成借阅记录（同一事务）。
     */
    public Loan borrow(String userId, String bookId) {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Map<String, Object>> books = Sql.queryMaps(connection,
                        "SELECT id, days, status FROM book WHERE id = ?", bookId);
                if (books.isEmpty()) {
                    throw new IllegalArgumentException("图书不存在");
                }
                Map<String, Object> book = books.get(0);
                if (!"正常".equals(String.valueOf(book.get("status")))) {
                    throw new IllegalStateException("该图书已废弃，无法借阅");
                }
                int days = book.get("days") == null ? 30 : ((Number) book.get("days")).intValue();

                List<Map<String, Object>> copies = Sql.queryMaps(connection,
                        "SELECT id FROM bookcopy WHERE bookId = ? AND isBorrowed = 0 LIMIT 1 FOR UPDATE", bookId);
                if (copies.isEmpty()) {
                    throw new IllegalStateException("该图书暂无可借副本");
                }
                String copyId = String.valueOf(copies.get(0).get("id"));

                JDBCTool.executeUpdate(connection,
                        "UPDATE bookcopy SET isBorrowed = 1, updateTime = ? WHERE id = ?", now, copyId);

                Loan loan = new Loan();
                loan.setId(IdUtil.uuid());
                loan.setCreateTime(now);
                loan.setUpdateTime(now);
                loan.setBookCopyId(copyId);
                loan.setUserId(userId);
                loan.setBorrowDate(now);
                loan.setDueDate(now.plusDays(days));
                loan.setStatus("未还");
                JDBCTool.save(loan, connection);

                connection.commit();
                return loan;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("借书失败", e);
        }
    }

    /**
     * 还书：根据借阅记录归还副本（同一事务）。
     */
    public void returnBook(String userId, String loanId) {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Map<String, Object>> rows = Sql.queryMaps(connection,
                        "SELECT bookCopyId, returnDate FROM loan WHERE id = ? AND userId = ? FOR UPDATE",
                        loanId, userId);
                if (rows.isEmpty()) {
                    throw new IllegalArgumentException("借阅记录不存在");
                }
                Map<String, Object> row = rows.get(0);
                if (row.get("returnDate") != null) {
                    throw new IllegalStateException("该借阅已归还");
                }
                String copyId = String.valueOf(row.get("bookCopyId"));
                JDBCTool.executeUpdate(connection,
                        "UPDATE loan SET returnDate = ?, status = '已还', updateTime = ? WHERE id = ?",
                        now, now, loanId);
                JDBCTool.executeUpdate(connection,
                        "UPDATE bookcopy SET isBorrowed = 0, updateTime = ? WHERE id = ?", now, copyId);
                connection.commit();
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("还书失败", e);
        }
    }

    /**
     * 按图书类型统计借阅次数（包含 0 次的类型）。
     */
    public List<Map<String, Object>> statsByType() {
        String sql = "SELECT t.type AS type, COUNT(l.id) AS total FROM "
                + "(SELECT DISTINCT type FROM book WHERE type IS NOT NULL AND type <> '') t "
                + "LEFT JOIN book b ON b.type = t.type "
                + "LEFT JOIN bookcopy bc ON bc.bookId = b.id "
                + "LEFT JOIN loan l ON l.bookCopyId = bc.id "
                + "GROUP BY t.type ORDER BY t.type";
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryMaps(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException("统计借阅数据失败", e);
        }
    }

    /**
     * 按用户专业统计借阅次数。
     */
    public List<Map<String, Object>> statsByMajor() {
        String sql = "SELECT u.major AS major, COUNT(l.id) AS total FROM user u "
                + "LEFT JOIN loan l ON l.userId = u.id "
                + "WHERE u.major IS NOT NULL AND u.major <> '' "
                + "GROUP BY u.major ORDER BY u.major";
        try (Connection connection = DBUtil.getConnection()) {
            return Sql.queryMaps(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException("统计借阅数据失败", e);
        }
    }

    private void decorateStatus(List<Map<String, Object>> rows) {
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object dueDate = row.get("dueDate");
            if ("未还".equals(status) && dueDate instanceof LocalDateTime due && due.isBefore(now)) {
                row.put("status", "超期");
            }
        }
    }
}
