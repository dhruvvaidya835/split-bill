import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class SettlementServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        int groupId = Integer.parseInt(req.getParameter("groupId"));

        // STEP 1 — SQL to fetch net balances
        String sql =
            "SELECT m.id AS member_id, m.name, " +
            "COALESCE(p.total_paid,0) - COALESCE(o.total_owed,0) AS net " +
            "FROM group_members gm " +
            "JOIN members m ON gm.member_id = m.id " +
            "LEFT JOIN (" +
            "  SELECT payer_member_id, SUM(amount) AS total_paid " +
            "  FROM expenses WHERE group_id = ? GROUP BY payer_member_id" +
            ") p ON p.payer_member_id = m.id " +
            "LEFT JOIN (" +
            "  SELECT es.member_id, SUM(es.share_amount) AS total_owed " +
            "  FROM expense_shares es " +
            "  JOIN expenses e ON es.expense_id = e.id " +
            "  WHERE e.group_id = ? GROUP BY es.member_id" +
            ") o ON o.member_id = m.id " +
            "WHERE gm.group_id = ?";

        // Lists for creditors and debtors
        List<String> creditors = new ArrayList<>();
        List<Double> creditAmt = new ArrayList<>();

        List<String> debtors = new ArrayList<>();
        List<Double> debtAmt = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ps.setInt(2, groupId);
            ps.setInt(3, groupId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");
                double bal = rs.getDouble("net");

                if (bal > 0) {
                    creditors.add(name);
                    creditAmt.add(bal);
                } else if (bal < 0) {
                    debtors.add(name);
                    debtAmt.add(-bal); // convert negative to positive
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // STEP 2 — Settlement Algorithm
        JSONArray result = new JSONArray();

        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {

            double pay = Math.min(debtAmt.get(i), creditAmt.get(j));

            JSONObject obj = new JSONObject();
            obj.put("from", debtors.get(i));
            obj.put("to", creditors.get(j));
            obj.put("amount", pay);
            result.put(obj);

            debtAmt.set(i, debtAmt.get(i) - pay);
            creditAmt.set(j, creditAmt.get(j) - pay);

            if (debtAmt.get(i) == 0) i++;
            if (creditAmt.get(j) == 0) j++;
        }

        res.setContentType("application/json");
        res.getWriter().write(result.toString());
    }
}
