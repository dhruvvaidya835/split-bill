import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class ExpenseServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        int groupId = Integer.parseInt(req.getParameter("groupId"));
        int payerId = Integer.parseInt(req.getParameter("payerId"));
        double amount = Double.parseDouble(req.getParameter("amount"));
        String title = req.getParameter("title");

        String[] participantsArr = req.getParameter("participants").split(",");

        double share = Math.round((amount / participantsArr.length) * 100.0) / 100.0;

        try (Connection conn = DBUtil.getConnection()) {

            conn.setAutoCommit(false);

            String expSQL = "INSERT INTO expenses(group_id, title, amount, payer_member_id) VALUES(?, ?, ?, ?)";
            PreparedStatement psExp = conn.prepareStatement(expSQL, Statement.RETURN_GENERATED_KEYS);

            psExp.setInt(1, groupId);
            psExp.setString(2, title);
            psExp.setDouble(3, amount);
            psExp.setInt(4, payerId);
            psExp.executeUpdate();

            ResultSet rs = psExp.getGeneratedKeys();
            rs.next();
            int expenseId = rs.getInt(1);

            String shareSQL = "INSERT INTO expense_shares(expense_id, member_id, share_amount) VALUES(?, ?, ?)";
            PreparedStatement psShare = conn.prepareStatement(shareSQL);

            for (String pid : participantsArr) {
                psShare.setInt(1, expenseId);
                psShare.setInt(2, Integer.parseInt(pid.trim()));
                psShare.setDouble(3, share);
                psShare.addBatch();
            }

            psShare.executeBatch();
            conn.commit();

            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"success\"}");

        } catch (Exception e) {
            e.printStackTrace();
            res.getWriter().write("{\"status\":\"error\"}");
        }
    }
}
