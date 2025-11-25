-- 알림 테이블 생성
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    actor_id VARCHAR(20) NOT NULL,
    post_id VARCHAR(50),
    type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    is_read TINYINT(1) DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_read (is_read)
);

-- 알림 테이블 설명:
-- notification_id: 알림 고유 ID (PK)
-- user_id: 알림을 받는 사용자 (FK)
-- actor_id: 알림을 발생시킨 사용자 (FK)
-- post_id: 관련 트윗 (FK, NULL 가능)
-- type: 알림 종류 (like, comment, retweet, quote)
-- created_at: 알림 생성 시간
-- is_read: 읽음 여부 (0: 안 읽음, 1: 읽음)

