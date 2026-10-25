package edu.chenjin.library.entity;

import edu.chenjin.jdbc.annotation.Column;
import edu.chenjin.jdbc.annotation.Table;

/**
 * 学生。
 */
@Table("user")
public class Student extends User {

    /** 年级 */
    @Column("grade")
    private String grade;

    /** 所属学院 */
    @Column("college")
    private String college;

    @Override
    public String[] hasPermission() {
        return new String[]{"借书", "还书", "个人借阅查询"};
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
}
