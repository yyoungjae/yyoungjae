package dao;

import util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MuteDAO {
    
    // 테이블이 없으면 생성
    private void ensureTablesExist() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 뮤트 단어 테이블 생성
            String createMutedWordsTable = 
                "CREATE TABLE IF NOT EXISTS muted_words (" +
                "mute_id VARCHAR(50) PRIMARY KEY, " +
                "user_id VARCHAR(50) NOT NULL, " +
                "muted_word VARCHAR(100) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE" +
                ")";
            stmt.execute(createMutedWordsTable);
            
            // 뮤트 사용자 테이블 생성
            String createMutedUsersTable = 
                "CREATE TABLE IF NOT EXISTS muted_users (" +
                "mute_id VARCHAR(50) PRIMARY KEY, " +
                "user_id VARCHAR(50) NOT NULL, " +
                "muted_user_id VARCHAR(50) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (muted_user_id) REFERENCES user(user_id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_mute (user_id, muted_user_id)" +
                ")";
            stmt.execute(createMutedUsersTable);
            
        } catch (SQLException e) {
            System.err.println("뮤트 테이블 생성 오류: " + e.getMessage());
        }
    }
    
    // 뮤트 단어 추가
    public boolean addMutedWord(String userId, String word) {
        ensureTablesExist();
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        String muteId = "mw" + System.currentTimeMillis();
        String sql = "INSERT INTO muted_words (mute_id, user_id, muted_word) VALUES (?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, muteId);
            pstmt.setString(2, userId);
            pstmt.setString(3, word.trim());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 뮤트 단어 삭제
    public boolean removeMutedWord(String userId, String word) {
        String sql = "DELETE FROM muted_words WHERE user_id = ? AND muted_word = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, word);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 뮤트 단어 목록 조회
    public List<String> getMutedWords(String userId) {
        ensureTablesExist();
        List<String> words = new ArrayList<>();
        String sql = "SELECT muted_word FROM muted_words WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    words.add(rs.getString("muted_word"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return words;
    }
    
    // 뮤트 사용자 추가
    public boolean addMutedUser(String userId, String mutedUserId) {
        ensureTablesExist();
        if (userId.equals(mutedUserId)) {
            return false; // 자기 자신은 뮤트할 수 없음
        }
        
        String muteId = "mu" + System.currentTimeMillis();
        String sql = "INSERT INTO muted_users (mute_id, user_id, muted_user_id) VALUES (?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, muteId);
            pstmt.setString(2, userId);
            pstmt.setString(3, mutedUserId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 뮤트 사용자 삭제
    public boolean removeMutedUser(String userId, String mutedUserId) {
        String sql = "DELETE FROM muted_users WHERE user_id = ? AND muted_user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, mutedUserId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 뮤트 사용자 목록 조회
    public List<String> getMutedUsers(String userId) {
        ensureTablesExist();
        List<String> users = new ArrayList<>();
        String sql = "SELECT muted_user_id FROM muted_users WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("muted_user_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    // 특정 사용자가 뮤트되었는지 확인
    public boolean isUserMuted(String userId, String mutedUserId) {
        ensureTablesExist();
        String sql = "SELECT 1 FROM muted_users WHERE user_id = ? AND muted_user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, mutedUserId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 특정 단어가 뮤트되었는지 확인 (포함 여부)
    public boolean containsMutedWord(String userId, String content) {
        ensureTablesExist();
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        List<String> mutedWords = getMutedWords(userId);
        String lowerContent = content.toLowerCase();
        
        for (String word : mutedWords) {
            if (lowerContent.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

