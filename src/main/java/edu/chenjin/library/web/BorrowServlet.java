package edu.chenjin.library.web;

import edu.chenjin.library.dao.LoanDao;
import edu.chenjin.library.entity.Loan;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 借书：为当前登录用户借阅指定图书。
 */
@WebServlet("/api/borrow")
public class BorrowServlet extends BaseServlet {

    private final LoanDao loanDao = new LoanDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            Requests.BookIdRequest body = readBody(request, Requests.BookIdRequest.class);
            if (body == null || body.bookId == null || body.bookId.isBlank()) {
                throw new IllegalArgumentException("请选择要借阅的图书");
            }
            Loan loan = loanDao.borrow(sessionUser.getId(), body.bookId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", loan.getId());
            data.put("borrowDate", loan.getBorrowDate());
            data.put("dueDate", loan.getDueDate());
            writeJson(response, Result.ok("借书成功", data));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "借书失败：" + e.getMessage()));
        }
    }
}
