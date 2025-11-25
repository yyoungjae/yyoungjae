package gui;

import dao.MessageDAO;
import model.Message;
import model.User;
import util.TimeFormatter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ConversationFrame extends JFrame {
    private User loginUser;
    private User otherUser;
    private MessageDAO messageDAO;
    private JPanel messagesPanel;
    private JTextArea messageInput;
    private PostFrame postFrame; // PostFrame 참조 추가
    private static final Color LIGHT_GRAY_BORDER = new Color(235, 238, 240);
    private static final Color MY_MESSAGE_BG = new Color(29, 161, 242);
    private static final Color OTHER_MESSAGE_BG = new Color(240, 240, 240);

    public ConversationFrame(User loginUser, User otherUser) {
        this(loginUser, otherUser, null);
    }
    
    public ConversationFrame(User loginUser, User otherUser, PostFrame postFrame) {
        this.loginUser = loginUser;
        this.otherUser = otherUser;
        this.postFrame = postFrame;
        this.messageDAO = new MessageDAO();

        setTitle("@" + otherUser.getUserId() + " - " + otherUser.getDisplayName());
        setSize(500, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 헤더
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        headerPanel.setBackground(Color.WHITE);
        
        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("Dialog", Font.PLAIN, 30));
        iconLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 프로필 이미지 표시
        if (otherUser.getProfileImagePath() != null && !otherUser.getProfileImagePath().isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(otherUser.getProfileImagePath());
                Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(img));
                iconLabel.setText("");
            } catch (Exception e) {
                // 이미지 로드 실패 시 기본 아이콘 유지
            }
        }
        
        JLabel nameLabel = new JLabel("<html><b>" + otherUser.getDisplayName() + "</b><br><span style='color:gray;'>@" + otherUser.getUserId() + "</span></html>");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        nameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 프로필 클릭 시 프로필 페이지로 이동
        java.awt.event.MouseAdapter profileClickListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (postFrame != null) {
                    // PostFrame이 있으면 showUserProfile 사용
                    postFrame.showUserProfile(otherUser);
                } else {
                    // PostFrame이 없으면 ProfileFrame 사용
                    try {
                        new gui.ProfileFrame(otherUser, null);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ConversationFrame.this, 
                            "프로필: " + otherUser.getDisplayName() + " (@" + otherUser.getUserId() + ")");
                    }
                }
            }
        };
        
        iconLabel.addMouseListener(profileClickListener);
        nameLabel.addMouseListener(profileClickListener);
        
        userInfoPanel.add(iconLabel);
        userInfoPanel.add(nameLabel);
        headerPanel.add(userInfoPanel, BorderLayout.WEST);

        // 메시지 목록
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        messagesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane messagesScrollPane = new JScrollPane(messagesPanel);
        messagesScrollPane.setBorder(null);
        messagesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 하단 입력 영역
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        messageInput = new JTextArea(3, 30);
        messageInput.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        messageInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        JScrollPane inputScrollPane = new JScrollPane(messageInput);
        inputScrollPane.setBorder(null);
        
        JButton sendBtn = new JButton("전송");
        sendBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        sendBtn.setBackground(new Color(100, 100, 100));
        sendBtn.setForeground(Color.BLACK);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setOpaque(true);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.setPreferredSize(new Dimension(80, 60));
        
        sendBtn.addActionListener(e -> sendMessage());
        
        // Enter 키로 전송
        messageInput.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(messagesScrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        refreshMessages();
        setVisible(true);
    }

    private void refreshMessages() {
        messagesPanel.removeAll();
        
        List<Message> messages = messageDAO.getConversation(loginUser.getUserId(), otherUser.getUserId());
        
        if (messages.isEmpty()) {
            JLabel emptyLabel = new JLabel("메시지가 없습니다. 첫 메시지를 보내보세요!");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messagesPanel.add(Box.createVerticalStrut(20));
            messagesPanel.add(emptyLabel);
        } else {
            for (Message msg : messages) {
                messagesPanel.add(createMessageBubble(msg));
                messagesPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        messagesPanel.revalidate();
        messagesPanel.repaint();
        
        // 스크롤을 맨 아래로
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = ((JScrollPane)messagesPanel.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private JPanel createMessageBubble(Message msg) {
        boolean isMyMessage = msg.getSenderId().equals(loginUser.getUserId());
        
        JPanel bubbleContainer = new JPanel();
        bubbleContainer.setLayout(new BoxLayout(bubbleContainer, BoxLayout.X_AXIS));
        bubbleContainer.setOpaque(false);
        
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        bubble.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));
        
        if (isMyMessage) {
            bubble.setBackground(MY_MESSAGE_BG);
            bubbleContainer.add(Box.createHorizontalGlue());
        } else {
            bubble.setBackground(OTHER_MESSAGE_BG);
        }
        
        // 메시지 내용
        JTextArea contentArea = new JTextArea(msg.getContent());
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        contentArea.setForeground(isMyMessage ? Color.WHITE : Color.BLACK);
        contentArea.setBackground(isMyMessage ? MY_MESSAGE_BG : OTHER_MESSAGE_BG);
        contentArea.setBorder(null);
        
        // 시간
        String timeAgo = TimeFormatter.formatRelativeTime(msg.getCreatedAt());
        JLabel timeLabel = new JLabel(timeAgo);
        timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
        timeLabel.setForeground(isMyMessage ? new Color(200, 200, 255) : Color.GRAY);
        timeLabel.setAlignmentX(isMyMessage ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        
        bubble.add(contentArea);
        bubble.add(Box.createVerticalStrut(5));
        bubble.add(timeLabel);
        
        bubbleContainer.add(bubble);
        
        if (!isMyMessage) {
            bubbleContainer.add(Box.createHorizontalGlue());
        }
        
        return bubbleContainer;
    }

    private void sendMessage() {
        String content = messageInput.getText().trim();
        
        if (content.isEmpty()) {
            return;
        }
        
        Message message = new Message(
            "m" + System.currentTimeMillis(),
            loginUser.getUserId(),
            otherUser.getUserId(),
            content
        );
        
        boolean success = messageDAO.sendMessage(message);
        
        if (success) {
            messageInput.setText("");
            refreshMessages();
        } else {
            String errorMsg = "메시지 전송에 실패했습니다.\n\n" +
                             "가능한 원인:\n" +
                             "1. messages 테이블이 데이터베이스에 생성되지 않았습니다.\n" +
                             "   src/sql/messages_table.sql 파일의 SQL을 실행해주세요.\n" +
                             "2. 데이터베이스 연결 오류가 발생했습니다.\n\n" +
                             "자세한 에러는 콘솔을 확인하세요.";
            JOptionPane.showMessageDialog(this, errorMsg, "메시지 전송 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}

