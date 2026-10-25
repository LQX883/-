package edu.chenjin.library.web;

import edu.chenjin.library.dao.BookCopyDao;
import edu.chenjin.library.entity.BookCopy;
import edu.chenjin.library.util.DtoUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 操作员：图书副本的查看、新增、删除。
 */
@WebServlet("/api/bookcopies")
public class BookCopyServlet extends BaseServlet {

    private final BookCopyDao bookCopyDao = new BookCopyDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            String bookId = requireParam(request, "bookId");
            List<Map<String, Object>> copies = bookCopyDao.listByBook(bookId);
            writeJson(response, Result.ok(copies));
        } catch (IllegalArgumentException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询图书副本失败：" + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "操作员")) {
            return;
        }
        try {
            Requests.BookIdRequest body = readBody(request, Requests.BookIdRequest.class);
            if (body == null || body.bookId == null || body.bookId.isBlank()) {
                throw new IllegalArgumentException("缺少图书 id");
            }
            BookCopy copy = bookCopyDao.addCopy(body.bookId);
            writeJson(response, Result.ok("新增副本成功", DtoUtil.bookCopy(copy)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "新增副本失败：" + e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "操作员")) {
            return;
        }
        try {
            String id = requireParam(request, "id");
            bookCopyDao.deleteCopy(id);
            writeJson(response, Result.ok("删除副本成功", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "删除副本失败：" + e.getMessage()));
        }
    }
}
