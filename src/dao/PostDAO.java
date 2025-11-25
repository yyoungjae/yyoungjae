package dao;

import model.Post;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    // 💡 Helper: ResultSet을 Post 객체로 매핑하는 통합 메서드 (13개 필드로 수정)
    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        // Post 생성자: 15개 인자 (imagePath, videoPath 포함)
        return new Post(
            rs.getString("post_id"),
            rs.getString("writer_id"),
            rs.getString("display_name"),
            rs.getString("content"),
            rs.getInt("num_of_likes"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at"),
            rs.getInt("nums_of_views"),
            rs.getInt("comment_count"),
            rs.getString("profile_image_path"),
            rs.getString("post_type"),
            rs.getInt("retweet_count"),
            rs.getString("parent_post_id"),
            rs.getString("image_path"), // 이미지 경로
            rs.getString("video_path")  // 동영상 경로
        );
    }
    
    // ----------------------------------------------------
    // R(Read) - 리트윗 수 계산 SQL 템플릿
    // ----------------------------------------------------
    private final String RETWEET_COUNT_SUBQUERY = 
        "(SELECT COUNT(*) FROM posts sub_p WHERE sub_p.post_type = 'retweet' AND sub_p.parent_post_id = p.post_id) AS retweet_count";


    // ----------------------------------------------------
    // C(Create) - 리트윗 / 인용 트윗 생성 메서드 (기존 로직 유지)
    // ----------------------------------------------------
    public boolean createInteractionPost(String writerId, String originalPostId, String type, String quoteContent) {
        // ... (기존 로직 유지) ...
        String postId = type.substring(0, 1) + System.currentTimeMillis();
        String content = type.equals("quote") ? quoteContent : "";
        
        String sql = "INSERT INTO posts (post_id, writer_id, content, parent_post_id, created_at, updated_at, post_type) " +
                     "VALUES (?, ?, ?, ?, NOW(), NOW(), ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, postId);
            pstmt.setString(2, writerId);
            pstmt.setString(3, content);
            pstmt.setString(4, originalPostId); 
            pstmt.setString(5, type);          
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 테이블 컬럼 확인 및 자동 추가
    private void ensureColumnsExist() {
        // MySQL은 IF NOT EXISTS를 지원하지 않으므로 직접 확인
        try (Connection conn = DBConnection.getConnection()) {
            // image_path 컬럼 확인 및 추가
            try (Statement stmt = conn.createStatement()) {
                String checkSQL = "SELECT image_path FROM posts LIMIT 1";
                try {
                    stmt.executeQuery(checkSQL);
                } catch (SQLException e) {
                    // 컬럼이 없으면 추가
                    stmt.execute("ALTER TABLE posts ADD COLUMN image_path VARCHAR(500)");
                }
            }
            
            // video_path 컬럼 확인 및 추가
            try (Statement stmt = conn.createStatement()) {
                String checkSQL = "SELECT video_path FROM posts LIMIT 1";
                try {
                    stmt.executeQuery(checkSQL);
                } catch (SQLException e) {
                    // 컬럼이 없으면 추가
                    stmt.execute("ALTER TABLE posts ADD COLUMN video_path VARCHAR(500)");
                }
            }
        } catch (SQLException e) {
            // 컬럼 추가 실패 시 로그만 출력 (이미 존재하는 경우 무시)
            System.err.println("posts 테이블 컬럼 확인/추가: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // C(Create) - 일반 게시글 작성 (image_path, video_path 포함)
    // ----------------------------------------------------
    public boolean writePost(Post post) {
        ensureColumnsExist();
        String sql = "INSERT INTO posts (post_id, writer_id, content, image_path, video_path, created_at, updated_at, num_of_likes, nums_of_views, post_type) " +
                     "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 0, 0, 'tweet')"; 

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, post.getPostId());
            pstmt.setString(2, post.getWriterId());
            pstmt.setString(3, post.getContent());
            pstmt.setString(4, post.getImagePath());
            pstmt.setString(5, post.getVideoPath());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ----------------------------------------------------
    // R(Read) - 타임라인 로드 (리트윗 수 포함, 뮤트 필터링)
    // ----------------------------------------------------
    public List<Post> getTimelinePosts(String userId) {
        List<Post> posts = new ArrayList<>();
        
        // 뮤트된 사용자 목록 가져오기
        List<String> mutedUserIds = getMutedUsers(userId);
        
        // SQL에 뮤트된 사용자 제외 조건 추가
        String sql = "SELECT p.*, u.display_name, u.profile_image_path, COUNT(c.comment_id) AS comment_count, p.post_type, p.parent_post_id, " + 
                     RETWEET_COUNT_SUBQUERY + // ✅ 리트윗 수 서브쿼리 추가
                     " FROM posts p " +
                     " JOIN user u ON p.writer_id = u.user_id " +
                     " LEFT JOIN comments c ON p.post_id = c.post_id " +
                     " WHERE (p.writer_id IN ( SELECT user_id FROM following WHERE follower_id = ? ) OR p.writer_id = ?) ";
        
        // 뮤트된 사용자 제외
        if (!mutedUserIds.isEmpty()) {
            sql += " AND p.writer_id NOT IN (";
            for (int i = 0; i < mutedUserIds.size(); i++) {
                if (i > 0) sql += ",";
                sql += "?";
            }
            sql += ") ";
        }
        
        sql += " GROUP BY p.post_id, p.writer_id, u.display_name, u.profile_image_path, p.content, p.num_of_likes, p.created_at, p.updated_at, p.nums_of_views, p.post_type, p.parent_post_id " + 
               " ORDER BY p.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            pstmt.setString(paramIndex++, userId);
            pstmt.setString(paramIndex++, userId);
            
            // 뮤트된 사용자 ID 바인딩
            for (String mutedUserId : mutedUserIds) {
                pstmt.setString(paramIndex++, mutedUserId);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Post post = mapResultSetToPost(rs);
                    // 뮤트된 단어 필터링 (Java 레벨에서)
                    if (!containsMutedWord(userId, post.getContent())) {
                        posts.add(post);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }
    
    // 뮤트된 사용자 목록 조회 헬퍼 메서드
    private List<String> getMutedUsers(String userId) {
        List<String> mutedUsers = new ArrayList<>();
        String sql = "SELECT muted_user_id FROM muted_users WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mutedUsers.add(rs.getString("muted_user_id"));
                }
            }
        } catch (SQLException e) {
            // 테이블이 없을 수 있으므로 무시
        }
        return mutedUsers;
    }
    
    // 뮤트된 단어 포함 여부 확인 헬퍼 메서드
    private boolean containsMutedWord(String userId, String content) {
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
    
    // 뮤트된 단어 목록 조회 헬퍼 메서드
    private List<String> getMutedWords(String userId) {
        List<String> mutedWords = new ArrayList<>();
        String sql = "SELECT muted_word FROM muted_words WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mutedWords.add(rs.getString("muted_word"));
                }
            }
        } catch (SQLException e) {
            // 테이블이 없을 수 있으므로 무시
        }
        return mutedWords;
    }
    
    // ----------------------------------------------------
    // R(Read) - 단일 게시글 로드 (리트윗 수 포함)
    // ----------------------------------------------------
    public Post getPostById(String postId) {
        String sql = "SELECT p.*, u.display_name, u.profile_image_path, COUNT(c.comment_id) AS comment_count, p.post_type, p.parent_post_id, " +
                     RETWEET_COUNT_SUBQUERY + // ✅ 리트윗 수 서브쿼리 추가
                     " FROM posts p " +
                     " JOIN user u ON p.writer_id = u.user_id " +
                     " LEFT JOIN comments c ON p.post_id = c.post_id " +
                     " WHERE p.post_id = ? " +
                     " GROUP BY p.post_id, p.writer_id, u.display_name, u.profile_image_path, p.content, p.num_of_likes, p.created_at, p.updated_at, p.nums_of_views, p.post_type, p.parent_post_id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, postId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPost(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ----------------------------------------------------
    // R(Read) - 특정 사용자 게시글 로드 (리트윗 수 포함)
    // ----------------------------------------------------
    public List<Post> getPostsByUserId(String userId) {
        List<Post> posts = new ArrayList<>();
        
        String sql = "SELECT p.*, u.display_name, u.profile_image_path, p.post_type, p.parent_post_id, COUNT(c.comment_id) AS comment_count, " + 
                     RETWEET_COUNT_SUBQUERY + // ✅ 리트윗 수 서브쿼리 추가
                     " FROM posts p " + 
                     " JOIN user u ON p.writer_id = u.user_id " +
                     " LEFT JOIN comments c ON p.post_id = c.post_id " +
                     " WHERE p.writer_id = ? " + 
                     " GROUP BY p.post_id, p.writer_id, u.display_name, u.profile_image_path, p.content, p.num_of_likes, p.created_at, p.updated_at, p.nums_of_views, p.post_type, p.parent_post_id " + 
                     " ORDER BY p.created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId); 
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapResultSetToPost(rs)); 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }
    
    // ----------------------------------------------------
    // U(Update) - 리트윗 토글 (한 번만 가능 및 취소 구현)
    // ----------------------------------------------------
    /**
     * 특정 게시글에 대한 사용자 리트윗을 토글합니다.
     * @return true: 리트윗 성공 (새로 생성됨), false: 리트윗 취소 성공 (삭제됨) 또는 실패
     */
    public boolean toggleRetweet(String originalPostId, String userId) {
        // 1. 해당 사용자가 이미 리트윗한 포스트가 있는지 확인
        String checkSql = "SELECT post_id FROM posts WHERE writer_id = ? AND parent_post_id = ? AND post_type = 'retweet'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, userId);
            checkStmt.setString(2, originalPostId);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // 2. 이미 리트윗한 경우: 리트윗 취소 (삭제)
                    String retweetId = rs.getString("post_id");
                    String deleteSql = "DELETE FROM posts WHERE post_id = ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, retweetId);
                        return deleteStmt.executeUpdate() > 0 ? false : false; // false = 취소됨
                    }
                } else {
                    // 3. 리트윗하지 않은 경우: 리트윗 실행 (생성)
                    String newPostId = "r" + System.currentTimeMillis();
                    String insertSql = "INSERT INTO posts (post_id, writer_id, content, parent_post_id, created_at, updated_at, post_type) " +
                                       "VALUES (?, ?, '', ?, NOW(), NOW(), 'retweet')";
                    
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, newPostId);
                        insertStmt.setString(2, userId);
                        insertStmt.setString(3, originalPostId);
                        return insertStmt.executeUpdate() > 0 ? true : false; // true = 생성됨
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // ----------------------------------------------------
    // U(Update) - 좋아요/취소 토글 (기존 로직 유지)
    // ----------------------------------------------------
    public boolean toggleLike(String postId, String userId) {
        // ... (기존 좋아요 토글 로직) ...
        if (isLikedBy(postId, userId)) {
            String deleteSql = "DELETE FROM likes WHERE post_id = ? AND user_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, postId);
                pstmt.setString(2, userId);
                
                if (pstmt.executeUpdate() > 0) {
                    updatePostLikeCount(postId, -1);
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String insertSql = "INSERT INTO likes (post_id, user_id) VALUES (?, ?)"; 
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, postId);
                pstmt.setString(2, userId);
                
                if (pstmt.executeUpdate() > 0) {
                    updatePostLikeCount(postId, 1);
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // 좋아요 여부 확인 (기존 로직 유지)
    public boolean isLikedBy(String postId, String userId) {
        String sql = "SELECT 1 FROM likes WHERE post_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 리트윗 여부 확인
    public boolean isRetweetedBy(String postId, String userId) {
        String sql = "SELECT 1 FROM posts WHERE parent_post_id = ? AND writer_id = ? AND post_type = 'retweet'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // posts 테이블의 좋아요 수 업데이트 (기존 로직 유지)
    private void updatePostLikeCount(String postId, int change) {
        String sql = "UPDATE posts SET num_of_likes = num_of_likes + ?, updated_at = NOW() WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, change);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 좋아요 목록 로드 (Likers List) (기존 로직 유지)
    public List<String> getLikersDisplayName(String postId) {
        List<String> likers = new ArrayList<>();
        String sql = "SELECT u.display_name " +
                     "FROM likes l JOIN user u ON l.user_id = u.user_id " +
                     "WHERE l.post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    likers.add(rs.getString("display_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return likers;
    }
    
    // D(Delete) - 게시글 삭제 (관련 데이터도 함께 삭제)
    public boolean deletePost(String postId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // 트랜잭션 시작
            
            try {
                // 1. 관련 댓글 삭제
                String deleteCommentsSql = "DELETE FROM comments WHERE post_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteCommentsSql)) {
                    pstmt.setString(1, postId);
                    pstmt.executeUpdate();
                }
                
                // 2. 관련 좋아요 삭제
                String deleteLikesSql = "DELETE FROM likes WHERE post_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteLikesSql)) {
                    pstmt.setString(1, postId);
                    pstmt.executeUpdate();
                }
                
                // 3. 관련 알림 삭제
                String deleteNotificationsSql = "DELETE FROM notifications WHERE post_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteNotificationsSql)) {
                    pstmt.setString(1, postId);
                    pstmt.executeUpdate();
                }
                
                // 4. 이 게시글을 부모로 하는 리트윗/인용트 삭제
                String deleteRetweetsSql = "DELETE FROM posts WHERE parent_post_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteRetweetsSql)) {
                    pstmt.setString(1, postId);
                    pstmt.executeUpdate();
                }
                
                // 5. 게시글 자체 삭제
                String deletePostSql = "DELETE FROM posts WHERE post_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deletePostSql)) {
                    pstmt.setString(1, postId);
                    int rowsAffected = pstmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        conn.commit(); // 모든 삭제 성공 시 커밋
                        return true;
                    } else {
                        conn.rollback(); // 삭제할 게시글이 없으면 롤백
                        return false;
                    }
                }
            } catch (SQLException e) {
                conn.rollback(); // 오류 발생 시 롤백
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true); // 자동 커밋 복원
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 게시글 내용, 작성자 ID, 또는 해시태그를 기준으로 게시글을 검색합니다.
     */
    public List<Post> searchPosts(String query) {
        List<Post> posts = new ArrayList<>();
        
        // 검색어가 비어있으면 빈 리스트 반환
        if (query == null || query.trim().isEmpty()) {
            return posts;
        }
        
        String searchPattern = "%" + query + "%";
        
        String sql = "SELECT p.*, u.display_name, u.profile_image_path, COUNT(c.comment_id) AS comment_count, p.post_type, p.parent_post_id, " +
                     RETWEET_COUNT_SUBQUERY + // ✅ 리트윗 수 서브쿼리 추가
                     " FROM posts p " +
                     " JOIN user u ON p.writer_id = u.user_id " +
                     " LEFT JOIN comments c ON p.post_id = c.post_id " +
                     " WHERE p.content LIKE ? " + 
                     "   OR u.user_id LIKE ? " +  
                     "   OR u.display_name LIKE ? " + 
                     " GROUP BY p.post_id, p.writer_id, u.display_name, u.profile_image_path, p.content, p.num_of_likes, p.created_at, p.updated_at, p.nums_of_views, p.post_type, p.parent_post_id " + 
                     " ORDER BY p.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapResultSetToPost(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }
}
