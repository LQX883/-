package edu.chenjin.library.web;

import edu.chenjin.library.dao.BookDao;
import edu.chenjin.library.dao.RecommendDao;
import edu.chenjin.library.entity.BookRecommend;
import edu.chenjin.library.util.DtoUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 图书推荐：教师提交推荐，管理员/教师查看列表。
 */
@WebServlet("/api/recommends")
public class RecommendServlet extends BaseServlet {

    private final RecommendDao recommendDao = new RecommendDao();
    private final BookDao bookDao = new BookDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null) {
            return;
        }
        try {
            String scope = param(request, "scope");
            List<Map<String, Object>> list;
            if ("all".equals(scope) && "管理员".equals(sessionUser.getRole())) {
                list = recommendDao.listByStatus(param(request, "status"));
            } else {
                list = recommendDao.listByUser(sessionUser.getId());
            }
            writeJson(response, Result.ok(list));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "查询推荐失败：" + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "教师")) {
            return;
        }
        try {
            Requests.RecommendRequest body = readBody(request, Requests.RecommendRequest.class);
            if (body == null || body.bookName == null || body.bookName.isBlank()) {
                throw new IllegalArgumentException("书名不能为空");
            }
            String bookName = body.bookName.trim();
            if (bookDao.existsByTitle(bookName)) {
                writeJson(response, Result.fail("图书馆已有《" + bookName + "》，无需推荐"));
                return;
            }
            if (recommendDao.existsPendingByTitle(bookName)) {
                writeJson(response, Result.fail("已有相同书名的推荐正在审核中"));
                return;
            }
            BookRecommend recommend = new BookRecommend();
            recommend.setUserId(sessionUser.getId());
            recommend.setBookName(bookName);
            recommend.setAuthor(body.author == null ? null : body.author.trim());
            recommend.setPublisher(body.publisher == null ? null : body.publisher.trim());
            recommend.setPrice(body.price);
            recommend.setStatus("待审核");
            recommendDao.insert(recommend);
            writeJson(response, Result.ok("推荐已提交，等待管理员审核", DtoUtil.recommend(recommend)));
        } catch (IllegalArgumentException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "提交推荐失败：" + e.getMessage()));
        }
    }
}
