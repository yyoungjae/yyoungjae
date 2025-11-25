package dao;

import java.sql.*;
import java.util.*;
import model.Comment;
import util.DBConnection;

public class CommentDAO {

    // 🔹 특정 포스트의 댓글 + 대댓글까지 모두 불러오기
    public List<Comment> getCommentsByPost(String postId) {
        List<Comment> comments = new ArrayList<>();
        String sql = 
        	    "SELECT c.comment_id, c.content, c.writer_id, c.post_id, c.num_of_likes, " +
        	    "       c.parent_comment_id, u.display_name " +
        	    "FROM comments c " +
        	    "JOIN user u ON c.writer_id = u.user_id " +
        	    "WHERE c.post_id = ? " +
        	    "ORDER BY " +
        	    "    CASE WHEN c.parent_comment_id IS NULL THEN c.comment_id ELSE c.parent_comment_id END, " +
        	    "    c.comment_id";


        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, postId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                comments.add(new Comment(
                        rs.getString("comment_id"),
                        rs.getString("content"),
                        rs.getString("writer_id"),
                        rs.getString("post_id"),
                        rs.getInt("num_of_likes"),
                        rs.getString("parent_comment_id"),
                        rs.getString("display_name")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return comments;
    }

    // 🔹 댓글 또는 대댓글 추가
    public boolean addComment(Comment c) {
        String sql = "INSERT INTO comments (comment_id, content, writer_id, post_id, num_of_likes, parent_comment_id) VALUES (?, ?, ?, ?, 0, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, c.getCommentId());
            pstmt.setString(2, c.getContent());
            pstmt.setString(3, c.getWriterId());
            pstmt.setString(4, c.getPostId());
            pstmt.setString(5, c.getParentCommentId());
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 🔹 좋아요 토글 (있으면 취소, 없으면 추가)
    public boolean toggleLike(String commentId, String userId) {
        String checkSql = "SELECT * FROM comment_like WHERE comment_id = ? AND liker_id = ?"; 
        
        try (Connection conn = DBConnection.getConnection()) {
            
            // 1. 이미 좋아요 눌렀는지 확인
            boolean alreadyLiked;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, commentId);
                checkStmt.setString(2, userId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    alreadyLiked = rs.next();
                }
            }

            if (alreadyLiked) { 
                // 좋아요 취소
                String deleteSql = "DELETE FROM comment_like WHERE comment_id = ? AND liker_id = ?"; 
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, commentId);
                    deleteStmt.setString(2, userId);
                    deleteStmt.executeUpdate();
                }
                updateLikeCount(commentId, -1);
                return false;
            } else { 
                // 좋아요 추가
                String likeId = "cl" + System.currentTimeMillis(); // 👈 l_id 생성 (오류 수정 반영)
                String insertSql = "INSERT INTO comment_like (l_id, comment_id, liker_id) VALUES (?, ?, ?)";
                
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, likeId);
                    insertStmt.setString(2, commentId);
                    insertStmt.setString(3, userId);
                    insertStmt.executeUpdate();
                }
                updateLikeCount(commentId, +1);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 🔹 좋아요 개수 업데이트
    private void updateLikeCount(String commentId, int delta) {
        String sql = "UPDATE comments SET num_of_likes = num_of_likes + ? WHERE comment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setString(2, commentId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 🔹 사용자가 이미 좋아요를 눌렀는지 확인
    public boolean isLikedBy(String commentId, String userId) {
        String sql = "SELECT 1 FROM comment_like WHERE comment_id = ? AND liker_id = ?"; 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, commentId);
            pstmt.setString(2, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // 🔹 댓글 좋아요 누른 사람 목록 가져오기
    public List<String> getLikers(String commentId) {
        List<String> likers = new ArrayList<>();
        String sql = "SELECT u.display_name FROM comment_like cl " +
                     "JOIN user u ON cl.liker_id = u.user_id " +
                     "WHERE cl.comment_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, commentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    likers.add(rs.getString("display_name"));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return likers;
    }
    
    // 🔹 댓글 검색 (내용, 작성자 ID, 작성자 이름으로 검색)
    public List<Comment> searchComments(String query) {
        List<Comment> comments = new ArrayList<>();
        
        // 검색어가 비어있으면 빈 리스트 반환
        if (query == null || query.trim().isEmpty()) {
            return comments;
        }
        
        String searchPattern = "%" + query + "%";
        String sql = "SELECT c.comment_id, c.content, c.writer_id, c.post_id, c.num_of_likes, " +
                     "       c.parent_comment_id, u.display_name " +
                     "FROM comments c " +
                     "JOIN user u ON c.writer_id = u.user_id " +
                     "WHERE c.content LIKE ? OR u.user_id LIKE ? OR u.display_name LIKE ? " +
                     "ORDER BY c.comment_id DESC " +
                     "LIMIT 50";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(new Comment(
                        rs.getString("comment_id"),
                        rs.getString("content"),
                        rs.getString("writer_id"),
                        rs.getString("post_id"),
                        rs.getInt("num_of_likes"),
                        rs.getString("parent_comment_id"),
                        rs.getString("display_name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }
}