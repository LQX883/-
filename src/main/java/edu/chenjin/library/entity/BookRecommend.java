package edu.chenjin.library.entity;

import edu.chenjin.jdbc.annotation.Column;
import edu.chenjin.jdbc.annotation.Table;
import edu.chenjin.jdbc.annotation.Transient;

/**
 * 图书推荐（待管理员审阅）。
 */
@Table("bookrecommend")
public class BookRecommend extends Base {

    /** 推荐人主键 */
    @Column("userId")
    private String userId;

    /** 书名 */
    @Column("bookName")
    private String bookName;

    /** 作者 */
    @Column("author")
    private String author;

    /** 出版社 */
    @Column("publisher")
    private String publisher;

    /** 价格 */
    @Column("price")
    private Double price;

    /** 状态：待审核、已通过、已驳回 */
    @Column("status")
    private String status;

    /** 驳回理由 */
    @Column("reason")
    private String reason;

    /** 推荐人（非数据库字段） */
    @Transient
    private User user;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
