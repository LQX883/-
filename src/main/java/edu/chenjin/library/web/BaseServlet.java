package edu.chenjin.library.web;

import edu.chenjin.library.util.JsonUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet 基类：封装 Session 用户、权限校验、参数读取与 JSON 输出。
 */
public abstract class BaseServlet extends HttpServlet {

    public static final String SESSION_USER = "LOGIN_USER";
    public static final String SESSION_CAPTCHA = "LOGIN_CAPTCHA";

    protected SessionUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (SessionUser) session.getAttribute(SESSION_USER);
    }

    /**
     * 要求登录；未登录时写出 401 并返回 null。
     */
    protected SessionUser requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser user = currentUser(request);
        if (user == null) {
            writeJson(response, Result.fail(401, "登录已过期，请重新登录"));
        }
        return user;
    }

    /**
     * 要求指定角色之一；不满足时写出 403 并返回 false。
     */
    protected boolean requireRole(HttpServletResponse response, SessionUser user, String... roles) throws IOException {
        if (user == null) {
            writeJson(response, Result.fail(401, "登录已过期，请重新登录"));
            return false;
        }
        for (String role : roles) {
            if (role.equals(user.getRole())) {
                return true;
            }
        }
        writeJson(response, Result.fail(403, "当前角色没有权限执行该操作"));
        return false;
    }

    protected void writeJson(HttpServletResponse response, Result result) throws IOException {
        JsonUtil.write(response, result);
    }

    protected String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? null : value.trim();
    }

    protected String requireParam(HttpServletRequest request, String name) {
        String value = param(request, name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("缺少参数：" + name);
        }
        return value;
    }

    protected Integer integerParam(HttpServletRequest request, String name) {
        String value = param(request, name);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数格式错误：" + name);
        }
    }

    protected Double doubleParam(HttpServletRequest request, String name) {
        String value = param(request, name);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数格式错误：" + name);
        }
    }

    protected <T> T readBody(HttpServletRequest request, Class<T> clazz) throws IOException {
        return JsonUtil.readBody(request, clazz);
    }
}
