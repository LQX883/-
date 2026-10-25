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
 * 图书列表 / 模糊查询（关键字为空时返回全部）。
 */
@WebServlet("/api/books")
public class BookListServlet extends BaseServlet {

    private final BookDao bookDao = new BookDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            List<Map<String, Object>> books = bookDao.listWithAvailable(param(request, "keyword"));
            writeJson(response, Result.ok(books));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询图书失败：" + e.getMessage()));
        }
    }
}
