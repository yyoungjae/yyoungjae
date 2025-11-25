package gui;

import dao.MessageDAO;
import dao.UserDAO;
import model.Message;
import model.User;
import util.TimeFormatter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MessageFrame extends JFrame {
    private User loginUser;
    private MessageDAO messageDAO;
    private UserDAO userDAO;
    private JPanel conversationListPanel;
    private static final Color LIGHT_GRAY_BORDER = new Color(235, 238, 240);

    public MessageFrame(User user) {
        this.loginUser = user;
        this.messageDAO = new MessageDAO();
        this.userDAO = new UserDAO();

        setTitle("메시지 - " + user.getDisplayName());
        setSize(700, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 헤더
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("메시지");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        
        // 새 메시지 버튼
        JButton newMessageBtn = new JButton("+ 새 메시지");
        newMessageBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        newMessageBtn.setBackground(new Color(100, 100, 100));
        newMessageBtn.setForeground(Color.BLACK);
        newMessageBtn.setFocusPainted(false);
        newMessageBtn.setBorderPainted(false);
        newMessageBtn.setOpaque(true);
        newMessageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newMessageBtn.addActionListener(e -> showNewMessageDialog());
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(newMessageBtn, BorderLayout.EAST);

        // 대화 목록
        conversationListPanel = new JPanel();
        conversationListPanel.setLayout(new BoxLayout(conversationListPanel, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(conversationListPanel);
        scrollPane.setBorder(null);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        refreshConversationList();
        setVisible(true);
    }

    private void refreshConversationList() {
        conversationListPanel.removeAll();
        
        List<Message> conversations = messageDAO.getConversationList(loginUser.getUserId());
        
        if (conversations.isEmpty()) {
            JLabel emptyLabel = new JLabel("메시지가 없습니다.");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            conversationListPanel.add(emptyLabel);
        } else {
            for (Message msg : conversations) {
                conversationListPanel.add(createConversationPanel(msg));
            }
        }
        
        conversationListPanel.revalidate();
        conversationListPanel.repaint();
    }

    private JPanel createConversationPanel(Message lastMessage) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 상대방 정보 가져오기
        String otherUserId = lastMessage.getSenderId().equals(loginUser.getUserId()) 
                           ? lastMessage.getReceiverId() 
                           : lastMessage.getSenderId();
        User otherUser = userDAO.getUserById(otherUserId);
        
        if (otherUser == null) return panel;
        
        // 프로필 아이콘
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("Dialog", Font.PLAIN, 40));
        
        // 사용자 정보 및 최근 메시지
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        
        JLabel nameLabel = new JLabel("<html><b>" + otherUser.getDisplayName() + "</b> <span style='color:gray;'>@" + otherUser.getUserId() + "</span></html>");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        // 최근 메시지 미리보기
        String preview = lastMessage.getContent();
        if (preview.length() > 40) {
            preview = preview.substring(0, 40) + "...";
        }
        
        JLabel messagePreview = new JLabel(preview);
        messagePreview.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        messagePreview.setForeground(Color.GRAY);
        
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(messagePreview);
        
        // 읽지 않은 메시지 표시
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        
        String timeAgo = TimeFormatter.formatRelativeTime(lastMessage.getCreatedAt());
        JLabel timeLabel = new JLabel(timeAgo);
        timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        timeLabel.setForeground(Color.GRAY);
        
        rightPanel.add(timeLabel);
        
        int unreadCount = messageDAO.getUnreadCountFrom(loginUser.getUserId(), otherUserId);
        if (unreadCount > 0) {
            JLabel unreadLabel = new JLabel(String.valueOf(unreadCount));
            unreadLabel.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            unreadLabel.setForeground(Color.WHITE);
            unreadLabel.setBackground(Color.RED);
            unreadLabel.setOpaque(true);
            unreadLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            rightPanel.add(Box.createVerticalStrut(5));
            rightPanel.add(unreadLabel);
        }
        
        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);
        
        // 클릭 시 대화창 열기
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openConversation(otherUser);
            }
        });
        
        return panel;
    }

    private void showNewMessageDialog() {
        String recipientId = JOptionPane.showInputDialog(this, "메시지를 보낼 사용자 ID를 입력하세요:");
        
        if (recipientId != null && !recipientId.trim().isEmpty()) {
            User recipient = userDAO.getUserById(recipientId.trim());
            
            if (recipient == null) {
                JOptionPane.showMessageDialog(this, "사용자를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            } else if (recipient.getUserId().equals(loginUser.getUserId())) {
                JOptionPane.showMessageDialog(this, "자기 자신에게는 메시지를 보낼 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            } else {
                openConversation(recipient);
            }
        }
    }

    private void openConversation(User otherUser) {
        new ConversationFrame(loginUser, otherUser);

        // 읽음 처리
        messageDAO.markAsRead(otherUser.getUserId(), loginUser.getUserId());
        refreshConversationList();
    }
}

