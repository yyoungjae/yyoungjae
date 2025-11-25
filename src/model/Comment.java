package model;

public class Comment {
    private String commentId;
    private String content;
    private String writerId;
    private String postId;
    private int numOfLikes;
    private String parentCommentId; // 대댓글일 경우 부모 댓글 ID
    private String displayName;     // 작성자 이름 (JOIN용)

    public Comment(String commentId, String content, String writerId,
                   String postId, int numOfLikes, String parentCommentId, String displayName) {
        this.commentId = commentId;
        this.content = content;
        this.writerId = writerId;
        this.postId = postId;
        this.numOfLikes = numOfLikes;
        this.parentCommentId = parentCommentId;
        this.displayName = displayName;
    }

    // 댓글 작성용 생성자
    public Comment(String commentId, String content, String writerId, String postId, int numOfLikes) {
        this(commentId, content, writerId, postId, numOfLikes, null, null);
    }

    public String getCommentId() { return commentId; }
    public String getContent() { return content; }
    public String getWriterId() { return writerId; }
    public String getPostId() { return postId; }
    public int getNumOfLikes() { return numOfLikes; }
    public String getParentCommentId() { return parentCommentId; }
    public String getDisplayName() { return displayName; }

    public void setNumOfLikes(int numOfLikes) { this.numOfLikes = numOfLikes; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}