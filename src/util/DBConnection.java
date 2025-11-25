package util;
import java.sql.*;

public class DBConnection {
    // 🚨 사용자 환경에 맞게 DB 정보 변경 필요
    private static final String URL = "jdbc:mysql://localhost:3306/twitter";
    private static final String USER = "root";
    private static final String PASSWORD = "0616"; // 네 비밀번호로 변경

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}