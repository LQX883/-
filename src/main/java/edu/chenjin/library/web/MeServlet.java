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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 获取当前登录用户信息与权限列表（首页展示用）。
 */
@WebServlet("/api/me")
public class MeServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            User user = userDao.findById(sessionUser.getId());
            if (user == null) {
                writeJson(response, Result.fail(401, "用户不存在，请重新登录"));
                return;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", user.getId());
            data.put("name", user.getName());
            data.put("role", user.getRole());
            data.put("major", user.getMajor());
            data.put("permissions", user.hasPermission());
            data.put("detail", DtoUtil.user(user, true));
            writeJson(response, Result.ok(data));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "获取用户信息失败：" + e.getMessage()));
        }
    }
}
