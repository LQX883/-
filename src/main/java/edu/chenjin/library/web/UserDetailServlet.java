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

/**
 * 管理员：查看单个用户详情（弹窗展示）。
 */
@WebServlet("/api/users/detail")
public class UserDetailServlet extends BaseServlet {

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
            String id = param(request, "id");
            if (id == null || id.isEmpty()) {
                writeJson(response, Result.fail("缺少用户 id"));
                return;
            }
            User user = userDao.findById(id);
            if (user == null) {
                writeJson(response, Result.fail("用户不存在"));
                return;
            }
            writeJson(response, Result.ok(DtoUtil.user(user, true)));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询用户失败：" + e.getMessage()));
        }
    }
}
