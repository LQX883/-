package edu.chenjin.library.web.dto;

import java.time.LocalDate;

/**
 * 请求体 DTO 集合（Gson 反序列化用，字段直接映射 JSON）。
 */
public final class Requests {

    private Requests() {
    }

    public static class LoginRequest {
        public String name;
        public String password;
        public String captcha;
    }

    public static class RegisterRequest {
        public String name;
        public String password;
        public String phone;
        public String major;
        public String role;
        public Integer permissionLevel;
        public LocalDate entryTime;
        public String area;
        public String duty;
        public String grade;
        public String college;
        public String title;
    }

    public static class BookRequest {
        public String id;
        public String title;
        public String author;
        public String type;
        public String publisher;
        public Integer days;
        public Double price;
        public Integer stock;
        public String introduction;
    }

    public static class IdRequest {
        public String id;
    }

    public static class BookIdRequest {
        public String bookId;
    }

    public static class LoanIdRequest {
        public String loanId;
    }

    public static class RecommendRequest {
        public String bookName;
        public String author;
        public String publisher;
        public Double price;
    }

    public static class ReviewRequest {
        public String id;
        /** approve 或 reject */
        public String action;
        public String reason;
        public Integer days;
        public Integer stock;
        public String type;
        public String introduction;
    }
}
