package dao;

import model.User;
import util.DBConnection;
import java.sql.*;

public class UserDAO {
    
    public UserDAO() {
        ensureDeactivatedColumnExists();
    }

    // 💡 Helper: 단일 User 객체를 ResultSet에서 생성 (User 모델 생성자 순서에 맞춰 수정)
    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        // User 생성자 순서: userId, pwd, username, email, displayName, bio, createdAt, profileImagePath, isDeactivated
        boolean isDeactivated = false;
        try {
            // is_deactivated 컬럼이 있으면 읽기, 없으면 기본값 false
            isDeactivated = rs.getBoolean("is_deactivated");
        } catch (SQLException e) {
            // 컬럼이 없으면 기본값 false 사용
            isDeactivated = false;
        }
        
        return new User(
            rs.getString("user_id"),
            rs.getString("pwd"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getString("bio"),
            rs.getTimestamp("created_at_datetime"),
            rs.getString("profile_image_path"),
            isDeactivated
        );
    }
    
    // 데이터베이스에 is_deactivated 컬럼이 있는지 확인하고 없으면 추가
    private void ensureDeactivatedColumnExists() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 컬럼 존재 여부 확인 (MySQL)
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                                   "WHERE TABLE_SCHEMA = DATABASE() " +
                                   "AND TABLE_NAME = 'user' " +
                                   "AND COLUMN_NAME = 'is_deactivated'";
            
            try (ResultSet rs = stmt.executeQuery(checkColumnSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // 컬럼이 없으면 추가
                    String addColumnSql = "ALTER TABLE user ADD COLUMN is_deactivated BOOLEAN DEFAULT FALSE";
                    stmt.executeUpdate(addColumnSql);
                }
            }
        } catch (SQLException e) {
            // 테이블이 없거나 오류 발생 시 무시 (나중에 테이블 생성 시 포함될 것)
            System.err.println("is_deactivated 컬럼 확인/추가 중 오류: " + e.getMessage());
        }
    }
    
    // ----------------------------------------------------
    // 핵심 기능 (로그인 / 회원가입)
    // ----------------------------------------------------

    public User login(String input, String pwd) {
        String inputTrimmed = input.trim(); 
        String pwdTrimmed = pwd.trim();
        
        // 🚨 평문 비밀번호 비교 로직으로 회귀
        String sql = "SELECT user_id, pwd, username, email, display_name, bio, created_at_datetime, profile_image_path, " +
                     "COALESCE(is_deactivated, FALSE) AS is_deactivated " +
                     "FROM user WHERE (username = ? OR user_id = ?) AND pwd = ?"; // 👈 pwd 조건을 SQL에 추가

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, inputTrimmed);
            pstmt.setString(2, inputTrimmed); 
            pstmt.setString(3, pwdTrimmed); // 👈 평문 비밀번호 바인딩

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createUserFromResultSet(rs); // 로그인 성공 (User 객체 반환)
                }
            }
        } catch (SQLException e) {
            System.err.println("로그인 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // 로그인 실패
    }

    public boolean register(User user) {
        // 🚨 평문 비밀번호 저장 로직으로 회귀
        String plainPwd = user.getPwd();
        
        String sql = "INSERT INTO user (user_id, pwd, username, email, display_name, bio, created_at_datetime, profile_image_path) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        
        String profilePath = (user.getProfileImagePath() == null) ? "" : user.getProfileImagePath(); 
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, plainPwd); // 👈 평문 비밀번호 저장
            pstmt.setString(3, user.getUsername());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getDisplayName());
            pstmt.setString(6, user.getBio());
            pstmt.setString(7, profilePath); 
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) { 
            e.printStackTrace();
            return false;
        }
    }
    
    // ----------------------------------------------------
    // 프로필 및 비밀번호 업데이트
    // ----------------------------------------------------

    // 🔹 사용자 정보 업데이트 (기존 로직 유지)
    public boolean updateProfile(User user) {
        String sql = "UPDATE user SET username = ?, email = ?, display_name = ?, bio = ?, profile_image_path = ? WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getDisplayName());
            pstmt.setString(4, user.getBio());
            pstmt.setString(5, user.getProfileImagePath()); 
            pstmt.setString(6, user.getUserId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 🔹 비밀번호 업데이트 (기존 평문 비교 로직으로 회귀)
    public boolean updatePassword(String userId, String currentPwd, String newPwd) {
        String checkSql = "SELECT pwd FROM user WHERE user_id = ? AND pwd = ?"; // 👈 평문 비밀번호로 현재 비밀번호 확인
        String updateSql = "UPDATE user SET pwd = ? WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();) {
            
            // 1단계: 현재 비밀번호 확인
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, userId);
                checkStmt.setString(2, currentPwd); // 👈 평문 비교
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) return false; // 비밀번호 불일치
                }
            }
            
            // 2단계: 새 비밀번호로 업데이트
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newPwd); // 👈 새 평문 비밀번호 저장
                updateStmt.setString(2, userId);
                return updateStmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // ----------------------------------------------------
    // 통계 기능 (기존 로직 유지)
    // ----------------------------------------------------
    // ... (getFollowingCount, getFollowerCount, getPostCount 메서드는 변경 없음) ...
    public int getFollowingCount(String userId) {
        String sql = "SELECT COUNT(*) FROM following WHERE follower_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getFollowerCount(String userId) {
        String sql = "SELECT COUNT(*) FROM following WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    public int getPostCount(String userId) {
        String sql = "SELECT COUNT(*) FROM posts WHERE writer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    // ----------------------------------------------------
    // 검색 기능
    // ----------------------------------------------------
    
    // 🔹 사용자 검색 (username, display_name, user_id에서 검색)
    public java.util.List<User> searchUsers(String query) {
        java.util.List<User> users = new java.util.ArrayList<>();
        
        // 검색어가 비어있으면 빈 리스트 반환
        if (query == null || query.trim().isEmpty()) {
            return users;
        }
        
        String sql = "SELECT user_id, pwd, username, email, display_name, bio, created_at_datetime, profile_image_path, " +
                     "COALESCE(is_deactivated, FALSE) AS is_deactivated " +
                     "FROM user WHERE username LIKE ? OR display_name LIKE ? OR user_id LIKE ? LIMIT 50";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(createUserFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    // 🔹 user_id로 사용자 조회
    public User getUserById(String userId) {
        String sql = "SELECT user_id, pwd, username, email, display_name, bio, created_at_datetime, profile_image_path, " +
                     "COALESCE(is_deactivated, FALSE) AS is_deactivated " +
                     "FROM user WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // ----------------------------------------------------
    // 계정 비활성화/활성화 기능
    // ----------------------------------------------------
    
    // 🔹 계정 비활성화 (비밀번호 확인 필요)
    public boolean deactivateAccount(String userId, String password) {
        // 먼저 비밀번호 확인
        String checkSql = "SELECT pwd FROM user WHERE user_id = ? AND pwd = ?";
        String updateSql = "UPDATE user SET is_deactivated = TRUE WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection()) {
            // 1단계: 비밀번호 확인
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, userId);
                checkStmt.setString(2, password);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) return false; // 비밀번호 불일치
                }
            }
            
            // 2단계: 계정 비활성화
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, userId);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 🔹 계정 활성화 (비밀번호 확인 필요)
    public boolean activateAccount(String userId, String password) {
        // 먼저 비밀번호 확인
        String checkSql = "SELECT pwd FROM user WHERE user_id = ? AND pwd = ?";
        String updateSql = "UPDATE user SET is_deactivated = FALSE WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection()) {
            // 1단계: 비밀번호 확인
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, userId);
                checkStmt.setString(2, password);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) return false; // 비밀번호 불일치
                }
            }
            
            // 2단계: 계정 활성화
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, userId);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 🔹 계정 비활성화 상태 확인
    public boolean isAccountDeactivated(String userId) {
        String sql = "SELECT COALESCE(is_deactivated, FALSE) AS is_deactivated FROM user WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_deactivated");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}