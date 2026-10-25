package edu.chenjin.library.entity;

import edu.chenjin.jdbc.annotation.Column;
import edu.chenjin.jdbc.annotation.Table;
import edu.chenjin.jdbc.annotation.Transient;

import java.time.LocalDateTime;

/**
 * 借阅记录。
 */
@Table("loan")
public class Loan extends Base {

    /** 图书副本（非数据库字段，数据库通过 bookCopyId 关联） */
    @Transient
    private BookCopy bookCopy;

    /** 图书副本主键 */
    @Column("bookCopyId")
    private String bookCopyId;

    /** 用户（非数据库字段，数据库通过 userId 关联） */
    @Transient
    private User user;

    /** 用户主键 */
    @Column("userId")
    private String userId;

    /** 借书日期 */
    @Column("borrowDate")
    private LocalDateTime borrowDate;

    /** 还书日期 */
    @Column("returnDate")
    private LocalDateTime returnDate;

    /** 应还日期（根据图书的 days 属性计算） */
    @Column("dueDate")
    private LocalDateTime dueDate;

    /** 状态：未还、已还、超期 */
    @Column("status")
    private String status;

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public String getBookCopyId() {
        return bookCopyId;
    }

    public void setBookCopyId(String bookCopyId) {
        this.bookCopyId = bookCopyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
