package edu.chenjin.library.web;

import edu.chenjin.library.dao.BookDao;
import edu.chenjin.library.dao.RecommendDao;
import edu.chenjin.library.entity.Book;
import edu.chenjin.library.entity.BookRecommend;
import edu.chenjin.library.util.IdUtil;
import edu.chenjin.library.util.Result;
import edu.chenjin.library.util.SessionUser;
import edu.chenjin.library.web.dto.Requests;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 管理员：审阅图书推荐（通过则加入图书列表，驳回需填写理由）。
 */
@WebServlet("/api/recommends/review")
public class RecommendReviewServlet extends BaseServlet {

    private final RecommendDao recommendDao = new RecommendDao();
    private final BookDao bookDao = new BookDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser sessionUser = requireLogin(request, response);
        if (sessionUser == null || !requireRole(response, sessionUser, "管理员")) {
            return;
        }
        try {
            Requests.ReviewRequest body = readBody(request, Requests.ReviewRequest.class);
            if (body == null || body.id == null || body.id.isBlank()) {
                throw new IllegalArgumentException("缺少推荐 id");
            }
            BookRecommend recommend = recommendDao.findById(body.id);
            if (recommend == null) {
                throw new IllegalArgumentException("推荐记录不存在");
            }
            if (!"待审核".equals(recommend.getStatus())) {
                throw new IllegalStateException("该推荐已审阅过");
            }
            String action = body.action == null ? "" : body.action.trim();
            if ("reject".equals(action)) {
                if (body.reason == null || body.reason.isBlank()) {
                    throw new IllegalArgumentException("驳回时必须填写理由");
                }
                recommendDao.updateStatus(body.id, "已驳回", body.reason.trim());
                writeJson(response, Result.ok("已驳回该推荐", null));
                return;
            }
            if (!"approve".equals(action)) {
                throw new IllegalArgumentException("操作类型不正确");
            }
            Book book = new Book();
            book.setId(IdUtil.uuid());
            book.setTitle(recommend.getBookName());
            book.setAuthor(recommend.getAuthor());
            book.setPublisher(recommend.getPublisher());
            book.setPrice(recommend.getPrice() == null ? 0D : recommend.getPrice());
            book.setType(body.type == null || body.type.isBlank() ? "推荐" : body.type.trim());
            book.setDays(body.days == null || body.days <= 0 ? 30 : body.days);
            book.setStock(body.stock == null || body.stock <= 0 ? 1 : body.stock);
            book.setStatus("正常");
            book.setIntroduction(body.introduction == null ? null : body.introduction.trim());
            bookDao.insertWithCopies(book);
            recommendDao.updateStatus(body.id, "已通过", null);
            writeJson(response, Result.ok("已通过并加入图书列表", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(response, Result.fail(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, Result.fail(500, "审阅推荐失败：" + e.getMessage()));
        }
    }
}
