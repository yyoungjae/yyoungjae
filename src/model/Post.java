package model;

import java.sql.Timestamp;

public class Post {
    private String postId;
    private String writerId;
    private String displayName;
    private String content;
    private int numOfLikes;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int numsOfViews;
    private int numOfComments;
    private String writerProfileImagePath;
    private String postType; 
    private int numOfRetweets; // ✅ 1. 리트윗 수 필드 추가
    private String parentPostId; // ✅ 부모 게시글 ID (리트윗/인용용)
    private String imagePath; // 이미지 파일 경로
    private String videoPath; // 동영상 파일 경로

    // ----------------------------------------------------
    // 생성자 (15개 인자 - 모든 필드 초기화)
    // ----------------------------------------------------
    public Post(String postId, String writerId, String displayName, String content, 
                int numOfLikes, Timestamp createdAt, Timestamp updatedAt, 
                int numsOfViews, int numOfComments, String writerProfileImagePath, 
                String postType, int numOfRetweets, String parentPostId, 
                String imagePath, String videoPath) {
        this.postId = postId;
        this.writerId = writerId;
        this.displayName = displayName;
        this.content = content;
        this.numOfLikes = numOfLikes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.numsOfViews = numsOfViews;
        this.numOfComments = numOfComments;
        this.writerProfileImagePath = writerProfileImagePath;
        this.postType = postType;
        this.numOfRetweets = numOfRetweets;
        this.parentPostId = parentPostId;
        this.imagePath = imagePath;
        this.videoPath = videoPath;
    }
    
    // ----------------------------------------------------
    // 생성자 오버로드 (13개 인자 - imagePath, videoPath 없음)
    // ----------------------------------------------------
    public Post(String postId, String writerId, String displayName, String content, 
                int numOfLikes, Timestamp createdAt, Timestamp updatedAt, 
                int numsOfViews, int numOfComments, String writerProfileImagePath, 
                String postType, int numOfRetweets, String parentPostId) {
        this(postId, writerId, displayName, content, numOfLikes, createdAt, updatedAt, 
             numsOfViews, numOfComments, writerProfileImagePath, postType, numOfRetweets, 
             parentPostId, null, null);
    }
    
    // ----------------------------------------------------
    // 생성자 오버로드 (12개 인자 - parentPostId 없음)
    // ----------------------------------------------------
    public Post(String postId, String writerId, String displayName, String content, 
                int numOfLikes, Timestamp createdAt, Timestamp updatedAt, 
                int numsOfViews, int numOfComments, String writerProfileImagePath, 
                String postType, int numOfRetweets) {
        this(postId, writerId, displayName, content, numOfLikes, createdAt, updatedAt, 
             numsOfViews, numOfComments, writerProfileImagePath, postType, numOfRetweets, null);
    }

    // ----------------------------------------------------
    // 생성자 오버로드 (11개 인자)
    // ----------------------------------------------------
    public Post(String postId, String writerId, String displayName, String content, 
                int numOfLikes, Timestamp createdAt, Timestamp updatedAt, 
                int numsOfViews, int numOfComments, String writerProfileImagePath, String postType) {
        this(postId, writerId, displayName, content, numOfLikes, createdAt, updatedAt, 
             numsOfViews, numOfComments, writerProfileImagePath, postType, 0, null);
    }
    
    // ----------------------------------------------------
    // 생성자 오버로드 (10개 인자)
    // ----------------------------------------------------
    public Post(String postId, String writerId, String displayName, String content, 
                int numOfLikes, Timestamp createdAt, Timestamp updatedAt, 
                int numsOfViews, int numOfComments, String writerProfileImagePath) {
        this(postId, writerId, displayName, content, numOfLikes, createdAt, updatedAt, 
             numsOfViews, numOfComments, writerProfileImagePath, "tweet", 0, null);
    }

    // ----------------------------------------------------
    // Getter 및 Setter
    // ----------------------------------------------------
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getWriterId() {
        return writerId;
    }

    public void setWriterId(String writerId) {
        this.writerId = writerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getNumOfLikes() {
        return numOfLikes;
    }

    public void setNumOfLikes(int numOfLikes) {
        this.numOfLikes = numOfLikes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getNumsOfViews() {
        return numsOfViews;
    }

    public void setNumsOfViews(int numsOfViews) {
        this.numsOfViews = numsOfViews;
    }

    public int getNumOfComments() {
        return numOfComments;
    }

    public void setNumOfComments(int numOfComments) {
        this.numOfComments = numOfComments;
    }

    public String getWriterProfileImagePath() {
        return writerProfileImagePath;
    }

    public void setWriterProfileImagePath(String writerProfileImagePath) {
        this.writerProfileImagePath = writerProfileImagePath;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }
    
    public int getNumOfRetweets() { // ✅ 4. Getter 추가
        return numOfRetweets;
    }

    public void setNumOfRetweets(int numOfRetweets) { // ✅ 5. Setter 추가
        this.numOfRetweets = numOfRetweets;
    }
    
    public String getParentPostId() {
        return parentPostId;
    }
    
    public void setParentPostId(String parentPostId) {
        this.parentPostId = parentPostId;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getVideoPath() {
        return videoPath;
    }
    
    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}