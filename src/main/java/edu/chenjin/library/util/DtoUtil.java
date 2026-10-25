package edu.chenjin.library.util;

import edu.chenjin.library.entity.Admin;
import edu.chenjin.library.entity.Book;
import edu.chenjin.library.entity.BookCopy;
import edu.chenjin.library.entity.BookRecommend;
import edu.chenjin.library.entity.Operator;
import edu.chenjin.library.entity.Student;
import edu.chenjin.library.entity.Teacher;
import edu.chenjin.library.entity.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实体转前端展示 Map 的工具，避免 Gson 序列化对象关联造成循环引用。
 */
public final class DtoUtil {

    private DtoUtil() {
    }

    public static Map<String, Object> user(User user, boolean detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (user == null) {
            return map;
        }
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("role", user.getRole());
        map.put("phone", user.getPhone());
        map.put("major", user.getMajor());
        map.put("createTime", user.getCreateTime());
        map.put("loginTime", user.getLoginTime());
        if (detail) {
            if (user instanceof Admin admin) {
                map.put("permissionLevel", admin.getPermissionLevel());
                map.put("entryTime", admin.getEntryTime());
            } else if (user instanceof Operator operator) {
                map.put("area", operator.getArea());
                map.put("duty", operator.getDuty());
            } else if (user instanceof Student student) {
                map.put("grade", student.getGrade());
                map.put("college", student.getCollege());
            } else if (user instanceof Teacher teacher) {
                map.put("college", teacher.getCollege());
                map.put("title", teacher.getTitle());
            }
        }
        return map;
    }

    public static Map<String, Object> book(Book book, Integer available) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (book == null) {
            return map;
        }
        map.put("id", book.getId());
        map.put("title", book.getTitle());
        map.put("author", book.getAuthor());
        map.put("type", book.getType());
        map.put("publisher", book.getPublisher());
        map.put("days", book.getDays());
        map.put("price", book.getPrice());
        map.put("stock", book.getStock());
        map.put("status", book.getStatus());
        map.put("introduction", book.getIntroduction());
        map.put("createTime", book.getCreateTime());
        map.put("available", available);
        return map;
    }

    public static Map<String, Object> bookCopy(BookCopy copy) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (copy == null) {
            return map;
        }
        map.put("id", copy.getId());
        map.put("bookId", copy.getBookId());
        map.put("isBorrowed", copy.isBorrowed());
        map.put("createTime", copy.getCreateTime());
        if (copy.getBook() != null) {
            map.put("title", copy.getBook().getTitle());
            map.put("author", copy.getBook().getAuthor());
        }
        return map;
    }

    public static Map<String, Object> recommend(BookRecommend recommend) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (recommend == null) {
            return map;
        }
        map.put("id", recommend.getId());
        map.put("userId", recommend.getUserId());
        map.put("bookName", recommend.getBookName());
        map.put("author", recommend.getAuthor());
        map.put("publisher", recommend.getPublisher());
        map.put("price", recommend.getPrice());
        map.put("status", recommend.getStatus());
        map.put("reason", recommend.getReason());
        map.put("createTime", recommend.getCreateTime());
        if (recommend.getUser() != null) {
            map.put("userName", recommend.getUser().getName());
            map.put("userRole", recommend.getUser().getRole());
        }
        return map;
    }
}
