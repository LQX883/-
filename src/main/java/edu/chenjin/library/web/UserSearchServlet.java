package edu.chenjin.library.web;

import edu.chenjin.library.dao.UserDao;
import edu.chenjin.library.entity.User;
import edu.chenjin.library.util.DtoUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员：用户信息模糊查询（按创建时间倒序）。
 */
@WebServlet("/api/users/search")
public class UserSearchServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

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
            String keyword = param(request, "keyword");
            List<User> users = userDao.search(keyword);
            List<Map<String, Object>> list = new ArrayList<>();
            for (User user : users) {
                list.add(DtoUtil.user(user, false));
            }
            writeJson(response, Result.ok(list));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询用户失败：" + e.getMessage()));
        }
    }
}
