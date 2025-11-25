package util;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일").withZone(ZoneId.systemDefault());

    /**
     * Timestamp를 "X분 전", "X시간 전", "YYYY년 MM월 dd일" 형식으로 변환합니다.
     */
    public static String formatRelativeTime(Timestamp timestamp) {
        Instant now = Instant.now();
        Instant past = timestamp.toInstant();
        Duration duration = Duration.between(past, now);

        long seconds = duration.getSeconds();
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (seconds < 60) {
            return seconds <= 5 ? "방금" : seconds + "초 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            // 일주일 이상 된 경우, "YYYY년 MM월 dd일" 형식으로 표시
            return DATE_FORMATTER.format(past);
        }
    }
}