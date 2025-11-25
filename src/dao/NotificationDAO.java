package dao;

import model.Notification;
import util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    // 테이블 생성 확인 및 자동 생성
    private void ensureTableExists() {
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS notifications (" +
            "    notification_id VARCHAR(50) PRIMARY KEY, " +
            "    user_id VARCHAR(20) NOT NULL, " +
            "    actor_id VARCHAR(20) NOT NULL, " +
            "    post_id VARCHAR(50), " +
            "    type VARCHAR(20) NOT NULL, " +
            "    created_at DATETIME NOT NULL, " +
            "    is_read TINYINT(1) DEFAULT 0, " +
            "    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE, " +
            "    FOREIGN KEY (actor_id) REFERENCES user(user_id) ON DELETE CASCADE, " +
            "    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE, " +
            "    INDEX idx_user (user_id), " +
            "    INDEX idx_created_at (created_at), " +
            "    INDEX idx_is_read (is_read) " +
            ")";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.err.println("notifications 테이블 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 알림 생성
    public boolean createNotification(Notification notification) {
        ensureTableExists();
        String sql = "INSERT INTO notifications (notification_id, user_id, actor_id, post_id, type, created_at, is_read) " +
                     "VALUES (?, ?, ?, ?, ?, NOW(), 0)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, notification.getNotificationId());
            pstmt.setString(2, notification.getUserId());
            pstmt.setString(3, notification.getActorId());
            if (notification.getPostId() != null) {
                pstmt.setString(4, notification.getPostId());
            } else {
                pstmt.setNull(4, java.sql.Types.VARCHAR);
            }
            pstmt.setString(5, notification.getType());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 사용자의 알림 목록 조회
    public List<Notification> getNotifications(String userId) {
        ensureTableExists();
        List<Notification> notifications = new ArrayList<>();
        // 리트윗 알림의 경우 원본 포스트 내용도 조회하기 위해 LEFT JOIN 추가
        String sql = "SELECT n.*, u.display_name AS actor_name, u.profile_image_path AS actor_profile_image, " +
                     "CASE " +
                     "  WHEN n.type = 'retweet' AND p.post_type = 'retweet' THEN orig_p.content " +
                     "  ELSE p.content " +
                     "END AS post_content " +
                     "FROM notifications n " +
                     "JOIN user u ON n.actor_id = u.user_id " +
                     "LEFT JOIN posts p ON n.post_id = p.post_id " +
                     "LEFT JOIN posts orig_p ON p.parent_post_id = orig_p.post_id " +
                     "WHERE n.user_id = ? " +
                     "ORDER BY n.created_at DESC " +
                     "LIMIT 100";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Notification notif = new Notification(
                        rs.getString("notification_id"),
                        rs.getString("user_id"),
                        rs.getString("actor_id"),
                        rs.getString("post_id"),
                        rs.getString("type"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_read"),
                        rs.getString("actor_name"),
                        rs.getString("post_content"),
                        rs.getString("actor_profile_image")
                    );
                    notifications.add(notif);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }

    // 읽지 않은 알림 수
    public int getUnreadCount(String userId) {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        
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

    // 모든 알림 읽음 처리
    public boolean markAllAsRead(String userId) {
        ensureTableExists();
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 특정 알림 읽음 처리
    public boolean markAsRead(String notificationId) {
        ensureTableExists();
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

