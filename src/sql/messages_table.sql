-- DM(Direct Message) 테이블 생성
CREATE TABLE IF NOT EXISTS messages (
    message_id VARCHAR(50) PRIMARY KEY,
    sender_id VARCHAR(20) NOT NULL,
    receiver_id VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    is_read TINYINT(1) DEFAULT 0,
    FOREIGN KEY (sender_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_created_at (created_at)
);

-- 메시지 테이블 설명:
-- message_id: 메시지 고유 ID (PK)
-- sender_id: 발신자 user_id (FK)
-- receiver_id: 수신자 user_id (FK)
-- content: 메시지 내용
-- created_at: 메시지 전송 시간
-- is_read: 읽음 여부 (0: 안 읽음, 1: 읽음)

