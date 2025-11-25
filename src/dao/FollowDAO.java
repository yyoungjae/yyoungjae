package dao;

import model.User;
import model.Notification;
import util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FollowDAO {
    private NotificationDAO notificationDAO;
    
    public FollowDAO() {
        this.notificationDAO = new NotificationDAO();
    }

    // 팔로우 상태 확인: followerId(나)가 followingId(상대방)를 팔로우하고 있는지 확인
    public boolean isFollowing(String followerId, String followingId) {
        // DDL 상의 following 테이블 스키마에 맞게 쿼리 작성 (user_id = followingId, follower_id = followerId)
        String sql = "SELECT 1 FROM following WHERE user_id = ? AND follower_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, followingId);  // user_id 컬럼에 상대방 ID
            pstmt.setString(2, followerId);   // follower_id 컬럼에 내 ID
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 팔로우/언팔로우 토글
    public boolean toggleFollow(String followerId, String followingId) {
        if (followerId.equals(followingId)) return false;

        if (isFollowing(followerId, followingId)) {
            // 이미 팔로우 중이면 -> 언팔로우
            return unfollow(followerId, followingId);
        } else {
            // 팔로우 중이 아니면 -> 팔로우
            return follow(followerId, followingId);
        }
    }

    // 팔로우 수행 (DB 삽입)
    private boolean follow(String followerId, String followingId) {
        // 'following' 테이블에 삽입할 때 필요한 l_id 생성 (VARCHAR(10) 이므로 클라이언트 생성)
        String followId = "f" + System.currentTimeMillis(); 
        
        String sql = "INSERT INTO following (f_id, user_id, follower_id) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, followId);
            pstmt.setString(2, followingId);  // DDL 상의 user_id (팔로우 당하는 사람)
            pstmt.setString(3, followerId);   // DDL 상의 follower_id (팔로우 하는 사람)
            
            int rows = pstmt.executeUpdate();
            
            // 팔로우 알림 생성 (팔로우 당하는 사람에게 알림)
            if (rows > 0) {
                String notificationId = "n" + System.currentTimeMillis();
                Notification notification = new Notification(
                    notificationId,
                    followingId,  // 알림을 받는 사람 (팔로우 당하는 사람)
                    followerId,   // 알림을 발생시킨 사람 (팔로우 하는 사람)
                    null,         // 팔로우는 postId가 없음
                    "follow"      // 알림 타입
                );
                notificationDAO.createNotification(notification);
            }
            
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 언팔로우 수행 (DB 삭제)
    private boolean unfollow(String followerId, String followingId) {
        String sql = "DELETE FROM following WHERE user_id = ? AND follower_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, followingId);  // DDL 상의 user_id (팔로우 당하는 사람)
            pstmt.setString(2, followerId);   // DDL 상의 follower_id (팔로우 하는 사람)
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 팔로워 목록 조회 (나를 팔로우하는 사람들)
    public List<User> getFollowers(String userId) {
        List<User> followers = new ArrayList<>();
        String sql = "SELECT u.user_id, u.pwd, u.username, u.email, u.display_name, u.bio, u.created_at_datetime, u.profile_image_path " +
                     "FROM following f " +
                     "JOIN user u ON f.follower_id = u.user_id " +
                     "WHERE f.user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    followers.add(new User(
                        rs.getString("user_id"),
                        rs.getString("pwd"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("bio"),
                        rs.getTimestamp("created_at_datetime"),
                        rs.getString("profile_image_path")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return followers;
    }
    
    // 팔로잉 목록 조회 (내가 팔로우하는 사람들)
    public List<User> getFollowing(String userId) {
        List<User> following = new ArrayList<>();
        String sql = "SELECT u.user_id, u.pwd, u.username, u.email, u.display_name, u.bio, u.created_at_datetime, u.profile_image_path " +
                     "FROM following f " +
                     "JOIN user u ON f.user_id = u.user_id " +
                     "WHERE f.follower_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    following.add(new User(
                        rs.getString("user_id"),
                        rs.getString("pwd"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("bio"),
                        rs.getTimestamp("created_at_datetime"),
                        rs.getString("profile_image_path")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return following;
    }
}