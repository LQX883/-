package edu.chenjin.library.entity;

import edu.chenjin.jdbc.annotation.Column;
import edu.chenjin.jdbc.annotation.Table;
import edu.chenjin.jdbc.annotation.Transient;

import java.util.List;

/**
 * 图书。
 */
@Table("book")
public class Book extends Base {

    /** 书名 */
    @Column("title")
    private String title;

    /** 作者 */
    @Column("author")
    private String author;

    /** 类型 */
    @Column("type")
    private String type;

    /** 出版社 */
    @Column("publisher")
    private String publisher;

    /** 还书期限天数 */
    @Column("days")
    private Integer days;

    /** 价格 */
    @Column("price")
    private Double price;

    /** 库存（副本总数） */
    @Column("stock")
    private Integer stock;

    /** 图书状态：正常、已废弃 */
    @Column("status")
    private String status;

    /** 图书简介 */
    @Column("introduction")
    private String introduction;

    /** 图书副本集合（非数据库字段） */
    @Transient
    private List<BookCopy> bookCopies;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public List<BookCopy> getBookCopies() {
        return bookCopies;
    }

    public void setBookCopies(List<BookCopy> bookCopies) {
        this.bookCopies = bookCopies;
    }
}
