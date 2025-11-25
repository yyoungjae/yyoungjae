package gui;

import dao.FollowDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class FollowerFollowingListFrame extends JFrame {
    private FollowDAO followDao;
    private User loggedInUser;
    private PostFrame mainPostFrame;
    private JPanel userListPanel;
    
    public FollowerFollowingListFrame(String title, List<User> users, User loggedInUser, PostFrame mainPostFrame, FollowDAO followDao) {
        this.loggedInUser = loggedInUser;
        this.mainPostFrame = mainPostFrame;
        this.followDao = followDao;
        
        setTitle(title + " 목록");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // 상단 헤더
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JButton closeBtn = new JButton("닫기");
        closeBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        headerPanel.add(closeBtn, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // 사용자 리스트 패널
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 사용자 리스트 표시
        displayUsers(users);
        
        setVisible(true);
    }
    
    private void displayUsers(List<User> users) {
        userListPanel.removeAll();
        
        if (users.isEmpty()) {
            JLabel emptyLabel = new JLabel("목록이 비어있습니다.");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            userListPanel.add(emptyLabel);
        } else {
            for (User user : users) {
                userListPanel.add(createUserPanel(user));
            }
        }
        
        userListPanel.revalidate();
        userListPanel.repaint();
    }
    
    private JPanel createUserPanel(User user) {
        JPanel userPanel = new JPanel(new BorderLayout(10, 5));
        userPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            new EmptyBorder(10, 15, 10, 15)
        ));
        userPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 프로필 이미지
        JLabel profileImageLabel = new JLabel("👤");
        profileImageLabel.setPreferredSize(new Dimension(50, 50));
        profileImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profileImageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(user.getProfileImagePath());
                Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                profileImageLabel.setIcon(new ImageIcon(img));
                profileImageLabel.setText("");
            } catch (Exception e) {
                // 이미지 로드 실패 시 기본 아이콘 유지
            }
        }
        
        // 사용자 정보 패널
        JPanel infoPanel = new JPanel(new BorderLayout(0, 3));
        
        JLabel nameLabel = new JLabel("<html><b>" + user.getDisplayName() + "</b> <span style='color:gray;'>@" + user.getUserId() + "</span></html>");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        nameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 프로필 이미지와 이름 클릭 시 프로필 페이지로 이동
        profileImageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showUserProfile(user);
            }
        });
        
        nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showUserProfile(user);
            }
        });
        
        // Bio 표시
        String bio = user.getBio();
        if (bio != null && !bio.isEmpty()) {
            JLabel bioLabel = new JLabel(bio.length() > 50 ? bio.substring(0, 50) + "..." : bio);
            bioLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            bioLabel.setForeground(Color.GRAY);
            infoPanel.add(bioLabel, BorderLayout.SOUTH);
        }
        
        infoPanel.add(nameLabel, BorderLayout.NORTH);
        
        // 팔로우 버튼 (자기 자신이 아닌 경우에만)
        JPanel rightPanel = new JPanel(new BorderLayout());
        if (!user.getUserId().equals(loggedInUser.getUserId())) {
            boolean isFollowing = followDao.isFollowing(loggedInUser.getUserId(), user.getUserId());
            JButton followBtn = createFollowButton(user, isFollowing);
            rightPanel.add(followBtn, BorderLayout.CENTER);
        }
        
        userPanel.add(profileImageLabel, BorderLayout.WEST);
        userPanel.add(infoPanel, BorderLayout.CENTER);
        userPanel.add(rightPanel, BorderLayout.EAST);
        
        return userPanel;
    }
    
    private JButton createFollowButton(User user, boolean isFollowing) {
        JButton followBtn = new JButton(isFollowing ? "팔로잉" : "팔로우");
        followBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        followBtn.setPreferredSize(new Dimension(90, 32));
        followBtn.setFocusPainted(false);
        followBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (isFollowing) {
            followBtn.setBackground(Color.WHITE);
            followBtn.setForeground(Color.BLACK);
            followBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(5, 15, 5, 15)
            ));
        } else {
            followBtn.setBackground(new Color(29, 161, 242)); // 트위터 블루
            followBtn.setForeground(Color.WHITE);
            followBtn.setBorderPainted(false);
        }
        
        followBtn.setOpaque(true);
        
        followBtn.addActionListener(e -> {
            boolean success = followDao.toggleFollow(loggedInUser.getUserId(), user.getUserId());
            if (success) {
                // 버튼 상태 업데이트
                boolean newFollowingState = followDao.isFollowing(loggedInUser.getUserId(), user.getUserId());
                followBtn.setText(newFollowingState ? "팔로잉" : "팔로우");
                
                if (newFollowingState) {
                    followBtn.setBackground(Color.WHITE);
                    followBtn.setForeground(Color.BLACK);
                    followBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        new EmptyBorder(5, 15, 5, 15)
                    ));
                } else {
                    followBtn.setBackground(new Color(29, 161, 242));
                    followBtn.setForeground(Color.WHITE);
                    followBtn.setBorderPainted(false);
                }
                
                // 메인 프레임 갱신
                if (mainPostFrame != null) {
                    mainPostFrame.refreshTimeline();
                    // 알림 버튼도 업데이트 (팔로우 알림이 생성되었을 수 있음)
                    SwingUtilities.invokeLater(() -> {
                        mainPostFrame.refreshNotifications();
                    });
                }
            }
        });
        
        return followBtn;
    }
    
    private void showUserProfile(User user) {
        if (mainPostFrame != null) {
            mainPostFrame.showUserProfile(user);
        } else {
            // PostFrame이 없는 경우 직접 ProfileFrame 열기
            new ProfileFrame(user, null);
        }
    }
}
