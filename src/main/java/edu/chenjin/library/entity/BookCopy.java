package edu.chenjin.library.entity;

import edu.chenjin.jdbc.annotation.Column;
import edu.chenjin.jdbc.annotation.Table;
import edu.chenjin.jdbc.annotation.Transient;

/**
 * 图书副本。
 */
@Table("bookcopy")
public class BookCopy extends Base {

    /** 属于哪种书（非数据库字段，数据库通过 bookId 关联） */
    @Transient
    private Book book;

    /** 所属图书主键（数据库外键列） */
    @Column("bookId")
    private String bookId;

    /** 借阅状态，true 为已借出，false 为未借阅 */
    @Column("isBorrowed")
    private boolean isBorrowed;

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public boolean isBorrowed() {
        return isBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        isBorrowed = borrowed;
    }
}
