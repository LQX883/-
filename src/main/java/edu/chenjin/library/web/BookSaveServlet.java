package edu.chenjin.library.web;

import edu.chenjin.library.dao.BookDao;
import edu.chenjin.library.entity.Book;
import edu.chenjin.library.util.DtoUtil;
import edu.chenjin.library.util.IdUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 操作员：新增图书（POST）与编辑图书（PUT）。
 */
@WebServlet("/api/books/save")
public class BookSaveServlet extends BaseServlet {

    private final BookDao bookDao = new BookDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "操作员")) {
            return;
        }
        try {
            Requests.BookRequest body = readBody(request, Requests.BookRequest.class);
            validate(body, true);
            Book book = new Book();
            book.setId(IdUtil.uuid());
            book.setTitle(body.title.trim());
            book.setAuthor(trim(body.author));
            book.setType(trim(body.type));
            book.setPublisher(trim(body.publisher));
            book.setDays(body.days == null ? 30 : body.days);
            book.setPrice(body.price == null ? 0D : body.price);
            book.setStock(body.stock == null ? 0 : body.stock);
            book.setStatus("正常");
            book.setIntroduction(trim(body.introduction));
            bookDao.insertWithCopies(book);
            writeJson(response, Result.ok("新增图书成功", DtoUtil.book(book, book.getStock())));
        } catch (IllegalArgumentException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "新增图书失败：" + e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "操作员")) {
            return;
        }
        try {
            Requests.BookRequest body = readBody(request, Requests.BookRequest.class);
            if (body == null || body.id == null || body.id.isBlank()) {
                throw new IllegalArgumentException("缺少图书 id");
            }
            validate(body, false);
            Book book = new Book();
            book.setId(body.id);
            book.setTitle(body.title.trim());
            book.setAuthor(trim(body.author));
            book.setType(trim(body.type));
            book.setPublisher(trim(body.publisher));
            book.setDays(body.days == null ? 30 : body.days);
            book.setPrice(body.price == null ? 0D : body.price);
            bookDao.updateFields(book);
            Book updated = bookDao.findById(body.id);
            writeJson(response, Result.ok("编辑图书成功",
                    DtoUtil.book(updated, bookDao.countAvailable(body.id))));
        } catch (IllegalArgumentException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "编辑图书失败：" + e.getMessage()));
        }
    }

    private void validate(Requests.BookRequest body, boolean requireStock) {
        if (body == null) {
            throw new IllegalArgumentException("请求数据为空");
        }
        if (body.title == null || body.title.isBlank()) {
            throw new IllegalArgumentException("书名不能为空");
        }
        if (body.days != null && body.days <= 0) {
            throw new IllegalArgumentException("还书期限必须大于 0 天");
        }
        if (body.price != null && body.price < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        if (requireStock && (body.stock == null || body.stock < 0)) {
            throw new IllegalArgumentException("库存必须大于等于 0");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
