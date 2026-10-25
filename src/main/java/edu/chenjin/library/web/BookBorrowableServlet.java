package edu.chenjin.library.web;

import edu.chenjin.library.dao.BookDao;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 可借阅图书列表（状态正常且有未借出副本）。
 */
@WebServlet("/api/books/borrowable")
public class BookBorrowableServlet extends BaseServlet {

    private final BookDao bookDao = new BookDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            List<Map<String, Object>> books = bookDao.listBorrowable(param(request, "keyword"));
            writeJson(response, Result.ok(books));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询可借图书失败：" + e.getMessage()));
        }
    }
}
