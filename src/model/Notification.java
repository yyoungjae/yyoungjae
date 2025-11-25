package model;

import java.sql.Timestamp;

public class Notification {
    private String notificationId;
    private String userId; // 알림을 받는 사용자
    private String actorId; // 알림을 발생시킨 사용자
    private String postId; // 관련 트윗
    private String type; // like, comment, retweet, quote
    private Timestamp createdAt;
    private boolean isRead;
    private String actorDisplayName; // JOIN용
    private String postContent; // JOIN용
    private String actorProfileImagePath; // 알림을 발생시킨 사용자의 프로필 이미지 경로

    // 전체 생성자
    public Notification(String notificationId, String userId, String actorId, String postId,
                       String type, Timestamp createdAt, boolean isRead,
                       String actorDisplayName, String postContent, String actorProfileImagePath) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.actorId = actorId;
        this.postId = postId;
        this.type = type;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.actorDisplayName = actorDisplayName;
        this.postContent = postContent;
        this.actorProfileImagePath = actorProfileImagePath;
    }
    
    // 기존 생성자 (하위 호환성)
    public Notification(String notificationId, String userId, String actorId, String postId,
                       String type, Timestamp createdAt, boolean isRead,
                       String actorDisplayName, String postContent) {
        this(notificationId, userId, actorId, postId, type, createdAt, isRead, actorDisplayName, postContent, null);
    }

    // 생성용 생성자
    public Notification(String notificationId, String userId, String actorId, String postId, String type) {
        this(notificationId, userId, actorId, postId, type, null, false, null, null);
    }

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String actorDisplayName) { this.actorDisplayName = actorDisplayName; }

    public String getPostContent() { return postContent; }
    public void setPostContent(String postContent) { this.postContent = postContent; }
    
    public String getActorProfileImagePath() { return actorProfileImagePath; }
    public void setActorProfileImagePath(String actorProfileImagePath) { this.actorProfileImagePath = actorProfileImagePath; }
}

