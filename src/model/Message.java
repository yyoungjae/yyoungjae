package model;

import java.sql.Timestamp;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private Timestamp createdAt;
    private boolean isRead;
    private String senderDisplayName; // JOIN용
    private String receiverDisplayName; // JOIN용

    // 전체 생성자
    public Message(String messageId, String senderId, String receiverId, String content, 
                   Timestamp createdAt, boolean isRead, String senderDisplayName, String receiverDisplayName) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.senderDisplayName = senderDisplayName;
        this.receiverDisplayName = receiverDisplayName;
    }

    // 메시지 전송용 생성자
    public Message(String messageId, String senderId, String receiverId, String content) {
        this(messageId, senderId, receiverId, content, null, false, null, null);
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getSenderDisplayName() { return senderDisplayName; }
    public void setSenderDisplayName(String senderDisplayName) { this.senderDisplayName = senderDisplayName; }

    public String getReceiverDisplayName() { return receiverDisplayName; }
    public void setReceiverDisplayName(String receiverDisplayName) { this.receiverDisplayName = receiverDisplayName; }
}

