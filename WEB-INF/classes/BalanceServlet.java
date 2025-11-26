import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.*;

public class BalanceServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        int groupId = Integer.parseInt(req.getParameter("groupId"));

        String sql =
        "SELECT m.name, " +
        "COALESCE(p.total_paid,0) - COALESCE(o.total_owed,0) AS net " +
        "FROM group_members gm " +
        "JOIN members m ON gm.member_id=m.id " +
        "LEFT JOIN (SELECT payer_member_id,SUM(amount) AS total_paid FROM expenses WHERE group_id=? GROUP BY payer_member_id) p ON p.payer_member_id = m.id " +
        "LEFT JOIN (SELECT member_id,SUM(share_amount) AS total_owed FROM expense_shares es JOIN expenses e ON es.expense_id=e.id WHERE e.group_id=? GROUP BY member_id) o ON o.member_id=m.id " +
        "WHERE gm.group_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ps.setInt(2, groupId);
            ps.setInt(3, groupId);

            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("name", rs.getString("name"));
                obj.put("net", rs.getDouble("net"));
                arr.put(obj);
            }

            res.setContentType("application/json");
            res.getWriter().write(arr.toString());

        } catch (Exception e) {
            e.printStackTrace();
            res.getWriter().write("[]");
        }
    }
}
