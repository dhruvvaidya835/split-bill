import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class GroupServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String name = req.getParameter("name");

        String sql = "INSERT INTO bill_groups(name) VALUES(?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.executeUpdate();

            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"success\",\"message\":\"Group added\"}");

        } catch (Exception e) {
            e.printStackTrace();
            res.getWriter().write("{\"status\":\"error\"}");
        }
    }
}
