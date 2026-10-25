package edu.chenjin.library.web;

import edu.chenjin.library.util.CaptchaUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 生成登录图形验证码，验证码文本保存在 Session 中。
 */
@WebServlet("/api/captcha")
public class CaptchaServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
        String code = CaptchaUtil.generate(120, 40, imageOut);
        byte[] imageBytes = imageOut.toByteArray();

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CAPTCHA, code);

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/png");
        response.setContentLength(imageBytes.length);
        response.getOutputStream().write(imageBytes);
        response.getOutputStream().flush();
    }
}
