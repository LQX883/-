package edu.chenjin.library.web;

import edu.chenjin.library.dao.UserDao;
import edu.chenjin.library.entity.User;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户登录：校验验证码、用户名与密码，成功后写入 Session。
 */
@WebServlet("/api/login")
public class LoginServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Requests.LoginRequest body = readBody(request, Requests.LoginRequest.class);
            if (body == null) {
                writeJson(response, Result.fail("请求数据为空"));
                return;
            }
            HttpSession session = request.getSession(true);
            Object expected = session.getAttribute(SESSION_CAPTCHA);
            session.removeAttribute(SESSION_CAPTCHA);
            if (expected == null) {
                writeJson(response, Result.fail("验证码已失效，请点击图片刷新"));
                return;
            }
            String captcha = body.captcha == null ? "" : body.captcha.trim();
            if (!expected.toString().equalsIgnoreCase(captcha)) {
                writeJson(response, Result.fail("验证码错误"));
                return;
            }
            String name = body.name == null ? "" : body.name.trim();
            if (name.isEmpty() || body.password == null || body.password.isEmpty()) {
                writeJson(response, Result.fail("用户名和密码不能为空"));
                return;
            }
            User user = userDao.findByNameAndPassword(name, body.password);
            if (user == null) {
                writeJson(response, Result.fail("用户名或密码错误"));
                return;
            }
            userDao.updateLoginTime(user.getId(), LocalDateTime.now());
            session.setAttribute(SESSION_USER,
                    new SessionUser(user.getId(), user.getName(), user.getRole(), user.getMajor()));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", user.getId());
            data.put("name", user.getName());
            data.put("role", user.getRole());
            data.put("major", user.getMajor());
            data.put("permissions", user.hasPermission());
            writeJson(response, Result.ok("登录成功", data));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "登录失败：" + e.getMessage()));
        }
    }
}
