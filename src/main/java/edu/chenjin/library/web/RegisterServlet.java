package edu.chenjin.library.web;

import edu.chenjin.library.dao.UserDao;
import edu.chenjin.library.entity.Admin;
import edu.chenjin.library.entity.Operator;
import edu.chenjin.library.entity.Student;
import edu.chenjin.library.entity.Teacher;
import edu.chenjin.library.entity.User;
import edu.chenjin.library.util.IdUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户注册：同名与手机号去重，按角色保存不同属性。
 */
@WebServlet("/api/register")
public class RegisterServlet extends BaseServlet {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Requests.RegisterRequest body = readBody(request, Requests.RegisterRequest.class);
            if (body == null) {
                writeJson(response, Result.fail("请求数据为空"));
                return;
            }
            String name = body.name == null ? "" : body.name.trim();
            String phone = body.phone == null ? "" : body.phone.trim();
            String role = body.role == null ? "" : body.role.trim();
            String major = body.major == null ? "" : body.major.trim();

            if (name.isEmpty()) {
                writeJson(response, Result.fail("姓名不能为空"));
                return;
            }
            if (body.password == null || body.password.length() < 3) {
                writeJson(response, Result.fail("密码至少 3 位"));
                return;
            }
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                writeJson(response, Result.fail("请输入正确的 11 位手机号"));
                return;
            }
            if (major.isEmpty()) {
                writeJson(response, Result.fail("请选择专业"));
                return;
            }
            if (!role.equals("管理员") && !role.equals("操作员") && !role.equals("学生") && !role.equals("教师")) {
                writeJson(response, Result.fail("请选择正确的角色"));
                return;
            }
            if (userDao.existsByName(name)) {
                writeJson(response, Result.fail("同名账号已存在，不能重复注册"));
                return;
            }
            if (userDao.existsByPhone(phone)) {
                writeJson(response, Result.fail("手机号已被注册"));
                return;
            }

            User user = buildUser(body, name, phone, role, major);
            userDao.insert(user);
            writeJson(response, Result.ok("注册成功", null));
        } catch (IllegalArgumentException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "注册失败：" + e.getMessage()));
        }
    }

    private User buildUser(Requests.RegisterRequest body, String name, String phone, String role, String major) {
        User user;
        switch (role) {
            case "管理员" -> {
                Admin admin = new Admin();
                admin.setPermissionLevel(body.permissionLevel == null ? 1 : body.permissionLevel);
                admin.setEntryTime(body.entryTime == null ? LocalDate.now() : body.entryTime);
                user = admin;
            }
            case "操作员" -> {
                Operator operator = new Operator();
                operator.setArea(body.area);
                operator.setDuty(body.duty);
                user = operator;
            }
            case "学生" -> {
                Student student = new Student();
                student.setGrade(body.grade);
                student.setCollege(body.college);
                user = student;
            }
            case "教师" -> {
                Teacher teacher = new Teacher();
                teacher.setCollege(body.college);
                teacher.setTitle(body.title);
                user = teacher;
            }
            default -> throw new IllegalArgumentException("请选择正确的角色");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setId(IdUtil.uuid());
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setName(name);
        user.setPassword(body.password);
        user.setPhone(phone);
        user.setRole(role);
        user.setMajor(major);
        return user;
    }
}
