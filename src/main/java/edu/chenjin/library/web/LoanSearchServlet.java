package edu.chenjin.library.web;

import edu.chenjin.library.dao.LoanDao;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理员：借阅记录查询（关键字 / 超期 / 日期范围）。
 */
@WebServlet("/api/loans/search")
public class LoanSearchServlet extends BaseServlet {

    private final LoanDao loanDao = new LoanDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        if (!requireRole(response, sessionUser, "管理员")) {
            return;
        }
        try {
            String mode = param(request, "mode");
            if (mode == null || mode.isBlank()) {
                mode = "keyword";
            }
            LocalDate start = parseDate(param(request, "start"));
            LocalDate end = parseDate(param(request, "end"));
            List<Map<String, Object>> loans = loanDao.search(param(request, "keyword"), mode, start, end);
            writeJson(response, Result.ok(loans));
        } catch (IllegalArgumentException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询借阅记录失败：" + e.getMessage()));
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd");
        }
    }
}
