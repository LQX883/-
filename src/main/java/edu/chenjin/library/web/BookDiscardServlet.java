package edu.chenjin.library.web;

import edu.chenjin.library.dao.BookDao;
import edu.chenjin.library.entity.Book;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 操作员：废弃图书（所有副本归还后才允许废弃）。
 */
@WebServlet("/api/books/discard")
public class BookDiscardServlet extends BaseServlet {

    private final BookDao bookDao = new BookDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "操作员")) {
            return;
        }
        try {
            Requests.IdRequest body = readBody(request, Requests.IdRequest.class);
            if (body == null || body.id == null || body.id.isBlank()) {
                throw new IllegalArgumentException("缺少图书 id");
            }
            Book book = bookDao.findById(body.id);
            if (book == null) {
                throw new IllegalArgumentException("图书不存在");
            }
            if ("已废弃".equals(book.getStatus())) {
                throw new IllegalStateException("该图书已经是废弃状态");
            }
            int borrowed = bookDao.countBorrowed(body.id);
            if (borrowed > 0) {
                throw new IllegalStateException("还有 " + borrowed + " 本副本在外借，全部归还后才能废弃");
            }
            bookDao.updateStatus(body.id, "已废弃");
            writeJson(response, Result.ok("图书已废弃", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "废弃图书失败：" + e.getMessage()));
        }
    }
}
