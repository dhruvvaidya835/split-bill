import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {
    public static Connection getConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/splitbill";
            String user = "root"; 
            String pass = "your_password"; 
            return DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
