package edu.chenjin.library.web;

import edu.chenjin.library.dao.LoanDao;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 还书：根据借阅记录归还副本。
 */
@WebServlet("/api/return")
public class ReturnServlet extends BaseServlet {

    private final LoanDao loanDao = new LoanDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            Requests.LoanIdRequest body = readBody(request, Requests.LoanIdRequest.class);
            if (body == null || body.loanId == null || body.loanId.isBlank()) {
                throw new IllegalArgumentException("请选择要归还的借阅记录");
            }
            loanDao.returnBook(sessionUser.getId(), body.loanId);
            writeJson(response, Result.ok("还书成功", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "还书失败：" + e.getMessage()));
        }
    }
}
