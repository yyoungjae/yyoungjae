package dao;

import model.Message;
import util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    // 테이블 생성 확인 및 자동 생성
    private void ensureTableExists() {
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS messages (" +
            "    message_id VARCHAR(50) PRIMARY KEY, " +
            "    sender_id VARCHAR(20) NOT NULL, " +
            "    receiver_id VARCHAR(20) NOT NULL, " +
            "    content TEXT NOT NULL, " +
            "    created_at DATETIME NOT NULL, " +
            "    is_read TINYINT(1) DEFAULT 0, " +
            "    read_at DATETIME, " +
            "    FOREIGN KEY (sender_id) REFERENCES user(user_id) ON DELETE CASCADE, " +
            "    FOREIGN KEY (receiver_id) REFERENCES user(user_id) ON DELETE CASCADE, " +
            "    INDEX idx_sender (sender_id), " +
            "    INDEX idx_receiver (receiver_id), " +
            "    INDEX idx_created_at (created_at) " +
            ")";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            
            // read_at 컬럼이 없으면 추가
            try {
                String checkSQL = "SELECT read_at FROM messages LIMIT 1";
                stmt.executeQuery(checkSQL);
            } catch (SQLException e) {
                // 컬럼이 없으면 추가
                try {
                    stmt.execute("ALTER TABLE messages ADD COLUMN read_at DATETIME");
                } catch (SQLException e2) {
                    // 이미 추가되었거나 실패한 경우 무시
                }
            }
        } catch (SQLException e) {
            System.err.println("messages 테이블 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 메시지 전송
    public boolean sendMessage(Message message) {
        // 테이블이 없으면 생성
        ensureTableExists();
        String sql = "INSERT INTO messages (message_id, sender_id, receiver_id, content, created_at, is_read) " +
                     "VALUES (?, ?, ?, ?, NOW(), 0)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, message.getMessageId());
            pstmt.setString(2, message.getSenderId());
            pstmt.setString(3, message.getReceiverId());
            pstmt.setString(4, message.getContent());
            
            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("메시지 전송 실패: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("메시지 전송 중 예상치 못한 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 두 사용자 간 대화 내역 조회
    public List<Message> getConversation(String user1Id, String user2Id) {
        ensureTableExists();
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                     "       s.display_name AS sender_name, " +
                     "       r.display_name AS receiver_name " +
                     "FROM messages m " +
                     "JOIN user s ON m.sender_id = s.user_id " +
                     "JOIN user r ON m.receiver_id = r.user_id " +
                     "WHERE (m.sender_id = ? AND m.receiver_id = ?) " +
                     "   OR (m.sender_id = ? AND m.receiver_id = ?) " +
                     "ORDER BY m.created_at ASC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user1Id);
            pstmt.setString(2, user2Id);
            pstmt.setString(3, user2Id);
            pstmt.setString(4, user1Id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                        rs.getString("message_id"),
                        rs.getString("sender_id"),
                        rs.getString("receiver_id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_read"),
                        rs.getString("sender_name"),
                        rs.getString("receiver_name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // 대화 목록 조회 (최근 메시지와 함께) - 중복 제거
    public List<Message> getConversationList(String userId) {
        ensureTableExists();
        List<Message> conversations = new ArrayList<>();
        // 각 대화 상대방당 최신 메시지 하나만 가져오기 (중복 제거)
        // LEAST/GREATEST를 사용하여 대화 상대방 쌍을 고유하게 식별
        String sql = "SELECT m.*, " +
                     "       s.display_name AS sender_name, " +
                     "       r.display_name AS receiver_name " +
                     "FROM messages m " +
                     "JOIN user s ON m.sender_id = s.user_id " +
                     "JOIN user r ON m.receiver_id = r.user_id " +
                     "WHERE m.message_id IN ( " +
                     "    SELECT MAX(message_id) " +
                     "    FROM messages " +
                     "    WHERE sender_id = ? OR receiver_id = ? " +
                     "    GROUP BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id) " +
                     ") " +
                     "ORDER BY m.created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(new Message(
                        rs.getString("message_id"),
                        rs.getString("sender_id"),
                        rs.getString("receiver_id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_read"),
                        rs.getString("sender_name"),
                        rs.getString("receiver_name")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("대화 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return conversations;
    }

    // 메시지 읽음 처리 (읽은 시간 기록)
    public boolean markAsRead(String senderId, String receiverId) {
        ensureTableExists();
        String sql = "UPDATE messages SET is_read = 1, read_at = NOW() " +
                     "WHERE sender_id = ? AND receiver_id = ? AND is_read = 0";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, senderId);
            pstmt.setString(2, receiverId);
            
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 읽지 않은 메시지 수 조회
    public int getUnreadCount(String userId) {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = 0";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 특정 사용자로부터의 읽지 않은 메시지 수
    public int getUnreadCountFrom(String receiverId, String senderId) {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM messages " +
                     "WHERE receiver_id = ? AND sender_id = ? AND is_read = 0";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, receiverId);
            pstmt.setString(2, senderId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    // 내가 보낸 마지막 메시지 조회 (읽은 시간 포함)
    public Message getMyLastMessage(String myUserId, String otherUserId) {
        ensureTableExists();
        String sql = "SELECT m.*, " +
                     "       s.display_name AS sender_name, " +
                     "       r.display_name AS receiver_name, " +
                     "       m.read_at " +
                     "FROM messages m " +
                     "JOIN user s ON m.sender_id = s.user_id " +
                     "JOIN user r ON m.receiver_id = r.user_id " +
                     "WHERE m.sender_id = ? AND m.receiver_id = ? " +
                     "ORDER BY m.created_at DESC " +
                     "LIMIT 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, myUserId);
            pstmt.setString(2, otherUserId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Message message = new Message(
                        rs.getString("message_id"),
                        rs.getString("sender_id"),
                        rs.getString("receiver_id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_read"),
                        rs.getString("sender_name"),
                        rs.getString("receiver_name")
                    );
                    // read_at을 Message 객체에 저장하는 방법 필요
                    // 일단 별도로 조회하거나, Message 모델에 readAt 필드 추가 필요
                    return message;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // 내가 보낸 마지막 메시지의 읽은 시간 조회
    public Timestamp getLastReadTime(String myUserId, String otherUserId) {
        ensureTableExists();
        String sql = "SELECT read_at " +
                     "FROM messages " +
                     "WHERE sender_id = ? AND receiver_id = ? AND is_read = 1 " +
                     "ORDER BY created_at DESC " +
                     "LIMIT 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, myUserId);
            pstmt.setString(2, otherUserId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("read_at");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

