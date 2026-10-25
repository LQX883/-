package edu.chenjin.library.web;

import edu.chenjin.library.dao.LoanDao;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员：借阅数据统计（按图书类型、按用户专业）。
 */
@WebServlet("/api/loans/stats")
public class LoanStatsServlet extends BaseServlet {

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
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("byType", loanDao.statsByType());
            data.put("byMajor", loanDao.statsByMajor());
            writeJson(response, Result.ok(data));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "统计借阅数据失败：" + e.getMessage()));
        }
    }
}
