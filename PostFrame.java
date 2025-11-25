package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.Timestamp;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import dao.CommentDAO;
import dao.FollowDAO;
import dao.MessageDAO;
import dao.MuteDAO;
import dao.NotificationDAO;
import dao.PostDAO;
import dao.UserDAO;
import model.Comment;
import model.Message;
import model.Notification;
import model.Post;
import model.User;
import util.TimeFormatter;

public class PostFrame extends JFrame {
    private User loginUser;
    private JPanel timelinePanel;
    private JPanel searchPanel;
    private JPanel notificationPanel;
    private JPanel messagePanel;
    private JTextField searchField; 
    
    private PostDAO postDAO;
    private CommentDAO commentDAO;
    private FollowDAO followDAO;
    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private NotificationDAO notificationDAO;
    private MuteDAO muteDAO;
    
    private JFrame postDetailFrame; 
    private final String DEFAULT_SEARCH_TEXT = "검색 (내용, @ID, #태그)";
    private JPanel mainContentPanel;
    private JScrollPane homeScrollPane; // 홈 탭의 스크롤 패널 저장
    private JButton notificationNavBtn; // 알림 버튼 참조 저장
    private JButton dmNavBtn; // DM 버튼 참조 저장
    
    // 💡 트위터 색상 정의
    private static final Color TWITTER_BLUE = new Color(29, 161, 242);
    private static final Color TWITTER_GREEN = new Color(23, 191, 99);
    private static final Color LIGHT_GRAY_BORDER = new Color(235, 238, 240); // 아주 연한 경계선 색

    public PostFrame(User user) {
        this.loginUser = user;
        postDAO = new PostDAO();
        commentDAO = new CommentDAO();
        followDAO = new FollowDAO();
        userDAO = new UserDAO();
        messageDAO = new MessageDAO();
        notificationDAO = new NotificationDAO();
        muteDAO = new MuteDAO();

        setTitle("Mini Twitter - " + user.getDisplayName());
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        Color backgroundColor = new Color(240, 248, 255); // 앨리스 블루
        getContentPane().setBackground(backgroundColor);

        // 상단 헤더 (프로필 사진만)
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // 프로필 사진 (25x25, 동그랗게)
        JLabel profilePicLabel = new JLabel("👤", SwingConstants.CENTER);
        profilePicLabel.setPreferredSize(new Dimension(25, 25));
        profilePicLabel.setMinimumSize(new Dimension(25, 25));
        profilePicLabel.setMaximumSize(new Dimension(25, 25));
        profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profilePicLabel.setVerticalAlignment(SwingConstants.CENTER);
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 프로필 이미지 로드
        if (loginUser.getProfileImagePath() != null && !loginUser.getProfileImagePath().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(loginUser.getProfileImagePath(), 25);
            if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                profilePicLabel.setIcon(circularIcon);
                profilePicLabel.setText("");
            } else {
                profilePicLabel.setIcon(null);
                profilePicLabel.setText("👤");
                profilePicLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
            }
        } else {
            profilePicLabel.setIcon(null);
            profilePicLabel.setText("👤");
            profilePicLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
        }
        
        profilePicLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new ProfileFrame(loginUser, PostFrame.this);
            }
        });
        
        headerPanel.add(profilePicLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // 메인 컨텐츠 영역 (CardLayout으로 탭 전환)
        mainContentPanel = new JPanel(new CardLayout());
        mainContentPanel.setBackground(backgroundColor);
        
        // 홈 탭 (타임라인)
        setupHomeTab();
        homeScrollPane = new JScrollPane(timelinePanel);
        homeScrollPane.setBorder(null);
        homeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        homeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // 스크롤 속도 조절 (마우스 휠로 더 빠르게 스크롤)
        homeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        homeScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        mainContentPanel.add(homeScrollPane, "HOME");
        
        // 검색 탭
        setupSearchTab();
        mainContentPanel.add(searchPanel, "SEARCH");
        
        // 알림 탭
        setupNotificationTab();
        mainContentPanel.add(notificationScrollPane, "NOTIFICATION");
        
        // DM 탭
        setupMessageTab();
        mainContentPanel.add(messagePanel, "DM");
        
        add(mainContentPanel, BorderLayout.CENTER);
        
        // 하단 네비게이션 바 (트위터 스타일)
        JPanel bottomNavBar = new JPanel(new GridLayout(1, 4, 0, 0));
        bottomNavBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        bottomNavBar.setBackground(Color.WHITE);
        
        // 새로운 네비게이션 버튼 (인자를 하나만 받도록 수정됨)
        JButton homeNavBtn = createNavButton("HOME");
        JButton searchNavBtn = createNavButton("SEARCH");
        notificationNavBtn = createNavButton("NOTIFICATION");
        dmNavBtn = createNavButton("DM");
        
        // 알림 버튼 초기 업데이트
        updateNotificationButton(notificationNavBtn);
        // DM 버튼 초기 업데이트
        updateDMButton(dmNavBtn);
        
        // 홈 버튼
        homeNavBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) mainContentPanel.getLayout();
            cl.show(mainContentPanel, "HOME");
            updateNavButtonState(homeNavBtn, true);
            updateNavButtonState(searchNavBtn, false);
            updateNavButtonState(notificationNavBtn, false);
            updateNavButtonState(dmNavBtn, false);
            refreshTimeline();
        });
        
        // 검색 버튼
        searchNavBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) mainContentPanel.getLayout();
            cl.show(mainContentPanel, "SEARCH");
            updateNavButtonState(homeNavBtn, false);
            updateNavButtonState(searchNavBtn, true);
            updateNavButtonState(notificationNavBtn, false);
            updateNavButtonState(dmNavBtn, false);
            // 검색 필드에 포커스 설정
            SwingUtilities.invokeLater(() -> {
                if (searchField != null) {
                    searchField.requestFocus();
                    // 검색어가 있으면 검색 결과 표시
                    String currentQuery = searchField.getText().trim();
                    if (!currentQuery.equals(DEFAULT_SEARCH_TEXT) && !currentQuery.isEmpty()) {
                        showSearchResultsInTab(currentQuery);
                    }
                }
            });
        });
        
        // 알림 버튼
        notificationNavBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) mainContentPanel.getLayout();
            cl.show(mainContentPanel, "NOTIFICATION");
            updateNavButtonState(homeNavBtn, false);
            updateNavButtonState(searchNavBtn, false);
            updateNavButtonState(notificationNavBtn, true);
            updateNavButtonState(dmNavBtn, false);
            // 알림 탭을 열 때마다 새로고침 및 버튼 업데이트
            SwingUtilities.invokeLater(() -> {
                refreshNotifications();
                updateNotificationButton(notificationNavBtn);
            });
        });
        
        // DM 버튼
        dmNavBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) mainContentPanel.getLayout();
            cl.show(mainContentPanel, "DM");
            updateNavButtonState(homeNavBtn, false);
            updateNavButtonState(searchNavBtn, false);
            updateNavButtonState(notificationNavBtn, false);
            updateNavButtonState(dmNavBtn, true);
            refreshMessageConversations();
            // DM 탭을 열 때 버튼 업데이트
            updateDMButton(dmNavBtn);
        });
        
        bottomNavBar.add(homeNavBtn);
        bottomNavBar.add(searchNavBtn);
        bottomNavBar.add(notificationNavBtn);
        bottomNavBar.add(dmNavBtn);
        
        // 첫 번째 버튼 활성화
        updateNavButtonState(homeNavBtn, true);
        
     // 1. 하단 네비게이션 바를 화면 맨 아래에 부착
        add(bottomNavBar, BorderLayout.SOUTH);

        // 2. 둥둥 떠있는 파란색 트윗 버튼 생성 (JLayeredPane 사용)
        JLayeredPane layeredPane = getLayeredPane(); // 프레임의 레이어드 페인 가져오기

        JButton tweetButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 버튼 실제 크기 및 여백 설정
                int diameter = 56; // 원의 지름
                int shadowGap = 4; // 그림자를 위한 여백
                int x = shadowGap;
                int y = shadowGap;

                // 그림자 그리기
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillOval(x + 1, y + 3, diameter, diameter);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillOval(x - 1, y + 2, diameter + 4, diameter + 4);

                // 메인 파란색 원 그리기
                g2.setColor(new Color(29, 161, 242)); // 트위터 블루
                g2.fillOval(x, y, diameter, diameter);

                // 흰색 플러스(+) 아이콘 그리기
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                int centerX = x + diameter / 2;
                int centerY = y + diameter / 2;
                int armLength = 10;

                g2.drawLine(centerX - armLength, centerY, centerX + armLength, centerY);
                g2.drawLine(centerX, centerY - armLength, centerX, centerY + armLength);

                g2.dispose();
            }
        };

        // 버튼 기본 스타일 제거
        tweetButton.setContentAreaFilled(false);
        tweetButton.setFocusPainted(false);
        tweetButton.setBorderPainted(false);
        tweetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 버튼 위치 지정 (오른쪽 하단)
        tweetButton.setBounds(490, 530, 64, 64);

        // 클릭 이벤트
        tweetButton.addActionListener(e -> {
            showPostDialog(); // 기존의 글쓰기 다이얼로그 메서드 호출
        });

        // 화면의 가장 위쪽 레이어에 버튼 추가
        layeredPane.add(tweetButton, JLayeredPane.PALETTE_LAYER);
        
        refreshTimeline();
        setVisible(true);
    }
    
    // 글쓰기 버튼 생성 (동그라미 모양)
    private JButton createPostButton() {
        JButton postBtn = new JButton("+") {
            @Override
            protected void paintComponent(Graphics g) {
                // 원형 배경 그리기
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        postBtn.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        postBtn.setForeground(Color.BLACK);
        postBtn.setBackground(Color.WHITE);
        postBtn.setFocusPainted(false);
        postBtn.setBorderPainted(false);
        postBtn.setContentAreaFilled(false);
        postBtn.setOpaque(false);
        postBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        postBtn.setPreferredSize(new Dimension(56, 56));
        postBtn.setMinimumSize(new Dimension(56, 56));
        postBtn.setMaximumSize(new Dimension(56, 56));
        
        // 액션 리스너
        postBtn.addActionListener(e -> {
            showPostDialog();
        });
        
        return postBtn;
    }
    
 // 네비게이션 버튼 생성 (수정됨: 아이콘 사용)
    private JButton createNavButton(String type) {
        JButton btn = new JButton();
        // 기본 아이콘 설정 (선택되지 않은 상태)
        btn.setIcon(createNavIcon(type, false));
        // 선택된 아이콘 설정 (롤오버 효과 등)
        btn.setRolloverIcon(createNavIcon(type, true));
        
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false); // 배경 투명하게
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 50));
        
        // 클릭 시 어떤 버튼인지 식별하기 위해 ActionCommand 설정
        btn.setActionCommand(type);
        
        return btn;
    }
    
    // 알림 버튼 업데이트 (읽지 않은 알림 수 표시)
    private void updateNotificationButton(JButton notificationBtn) {
        int unreadCount = notificationDAO.getUnreadCount(loginUser.getUserId());
        if (unreadCount > 0) {
            notificationBtn.setText("알림(" + unreadCount + ")");
        } else {
            notificationBtn.setText("알림");
        }
    }
    
    // DM 버튼 업데이트 (읽지 않은 메시지 수 표시)
    private void updateDMButton(JButton dmBtn) {
        int unreadCount = messageDAO.getUnreadCount(loginUser.getUserId());
        if (unreadCount > 0) {
            dmBtn.setText("DM(" + unreadCount + ")");
        } else {
            dmBtn.setText("DM");
        }
    }
    
 // 네비게이션 버튼 상태 업데이트 (수정됨: 아이콘 교체)
    private void updateNavButtonState(JButton btn, boolean selected) {
        String type = btn.getActionCommand();
        if (type != null) {
            btn.setIcon(createNavIcon(type, selected));
        }
    }
    
    // 홈 탭 설정
    private void setupHomeTab() {
        timelinePanel = new JPanel();
        timelinePanel.setBackground(new Color(240, 248, 255));
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(timelinePanel);
        scrollPane.setBorder(null);
        // CardLayout에 추가는 생성자에서 처리
    }
    
    // 검색 탭 설정 (검색창 + 검색 결과)
    private JPanel searchResultsPanel; // 검색 결과 패널을 멤버 변수로 저장
    private JPanel userSearchResultsPanel; // 사용자 검색 결과 패널
    private void setupSearchTab() {
        searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(Color.WHITE);
        
        // 검색창 패널
        JPanel searchTopPanel = new JPanel(new BorderLayout(10, 10));
        searchTopPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        searchTopPanel.setBackground(Color.WHITE);
        searchTopPanel.setOpaque(true);
        
        searchField = new JTextField(DEFAULT_SEARCH_TEXT, 20);
        searchField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        searchField.setBackground(Color.WHITE);
        searchField.setOpaque(true);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        searchField.setPreferredSize(new Dimension(0, 45));
        
        JButton searchButton = new JButton("검색");
        searchButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        searchButton.setBackground(new Color(240, 240, 240));
        searchButton.setForeground(Color.DARK_GRAY);
        searchButton.setFocusPainted(false);
        searchButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        searchButton.setPreferredSize(new Dimension(90, 45));
        searchButton.setOpaque(true);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(DEFAULT_SEARCH_TEXT)) searchField.setText("");
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) searchField.setText(DEFAULT_SEARCH_TEXT);
            }
        });
        
        // 검색 실행 메서드
        Runnable performSearchAction = () -> {
            String rawQuery = searchField.getText().trim();
            String query = rawQuery.equals(DEFAULT_SEARCH_TEXT) ? "" : rawQuery;
            
            if (query.isEmpty()) {
                JOptionPane.showMessageDialog(this, "검색어를 입력해주세요.");
                searchField.requestFocus();
                return;
            }
            
            final String finalQuery = query;
            SwingUtilities.invokeLater(() -> {
                showSearchResultsInTab(finalQuery);
            });
        };
        
        searchField.addActionListener(e -> performSearchAction.run());
        searchButton.addActionListener(e -> performSearchAction.run());
        
        searchTopPanel.add(searchField, BorderLayout.CENTER);
        searchTopPanel.add(searchButton, BorderLayout.EAST);
        
        // 검색 결과 탭 패널
        JTabbedPane searchTabbedPane = new JTabbedPane();
        
        // 트윗/답글 검색 결과 패널
        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(Color.WHITE);
        searchResultsPanel.setOpaque(true);
        
        JScrollPane resultsScrollPane = new JScrollPane(searchResultsPanel);
        resultsScrollPane.setBorder(null);
        resultsScrollPane.setBackground(Color.WHITE);
        resultsScrollPane.getViewport().setBackground(Color.WHITE);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        resultsScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        // 사용자 검색 결과 패널
        JPanel userSearchResultsPanel = new JPanel();
        userSearchResultsPanel.setLayout(new BoxLayout(userSearchResultsPanel, BoxLayout.Y_AXIS));
        userSearchResultsPanel.setBackground(Color.WHITE);
        userSearchResultsPanel.setOpaque(true);
        
        JScrollPane userResultsScrollPane = new JScrollPane(userSearchResultsPanel);
        userResultsScrollPane.setBorder(null);
        userResultsScrollPane.setBackground(Color.WHITE);
        userResultsScrollPane.getViewport().setBackground(Color.WHITE);
        userResultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        userResultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        userResultsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        userResultsScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        // 초기 안내 메시지 표시
        JLabel hintLabel = new JLabel("검색어를 입력하고 검색 버튼을 누르세요.");
        hintLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchResultsPanel.add(Box.createVerticalGlue());
        searchResultsPanel.add(hintLabel);
        searchResultsPanel.add(Box.createVerticalGlue());
        
        JLabel userHintLabel = new JLabel("검색어를 입력하고 검색 버튼을 누르세요.");
        userHintLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        userHintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        userHintLabel.setForeground(Color.GRAY);
        userHintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userSearchResultsPanel.add(Box.createVerticalGlue());
        userSearchResultsPanel.add(userHintLabel);
        userSearchResultsPanel.add(Box.createVerticalGlue());
        
        searchTabbedPane.addTab("최근", resultsScrollPane);
        searchTabbedPane.addTab("사용자", userResultsScrollPane);
        
        // userSearchResultsPanel을 멤버 변수로 저장
        this.userSearchResultsPanel = userSearchResultsPanel;
        
        searchPanel.add(searchTopPanel, BorderLayout.NORTH);
        searchPanel.add(searchTabbedPane, BorderLayout.CENTER);
    }
    
    // 알림 탭 설정
    private JScrollPane notificationScrollPane;
    private void setupNotificationTab() {
        notificationPanel = new JPanel();
        notificationPanel.setBackground(Color.WHITE);
        notificationPanel.setLayout(new BoxLayout(notificationPanel, BoxLayout.Y_AXIS));
        notificationScrollPane = new JScrollPane(notificationPanel);
        notificationScrollPane.setBorder(null);
        notificationScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        notificationScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // 스크롤 속도 조절 (마우스 휠로 더 빠르게 스크롤)
        notificationScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        notificationScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        refreshNotifications();
    }
    
    // DM 탭 설정
    private JPanel messageConversationListPanel;
    private void setupMessageTab() {
        messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(Color.WHITE);
        
        // 상단 헤더
        JPanel messageTopPanel = new JPanel(new BorderLayout());
        messageTopPanel.setBackground(Color.WHITE);
        messageTopPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("DM");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        
        JButton newMessageBtn = new JButton("+ 새 메시지");
        newMessageBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        newMessageBtn.setBackground(new Color(240, 240, 240));
        newMessageBtn.setForeground(Color.DARK_GRAY);
        newMessageBtn.setFocusPainted(false);
        newMessageBtn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        newMessageBtn.setOpaque(true);
        newMessageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newMessageBtn.addActionListener(e -> {
            // 사용자 선택 다이얼로그
            String userId = JOptionPane.showInputDialog(this, "메시지를 보낼 사용자 ID를 입력하세요:", "새 메시지", JOptionPane.PLAIN_MESSAGE);
            if (userId != null && !userId.trim().isEmpty()) {
                User targetUser = userDAO.getUserById(userId.trim());
                if (targetUser != null) {
                    ConversationFrame convFrame = new ConversationFrame(loginUser, targetUser, this);
                // 대화창이 닫힐 때 대화 목록 새로고침
                convFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        refreshMessageConversations();
                    }
                });
                } else {
                    JOptionPane.showMessageDialog(this, "사용자를 찾을 수 없습니다.");
                }
            }
        });
        
        messageTopPanel.add(titleLabel, BorderLayout.WEST);
        messageTopPanel.add(newMessageBtn, BorderLayout.EAST);
        
        // 대화 목록 패널
        messageConversationListPanel = new JPanel();
        messageConversationListPanel.setBackground(Color.WHITE);
        messageConversationListPanel.setLayout(new BoxLayout(messageConversationListPanel, BoxLayout.Y_AXIS));
        JScrollPane conversationScrollPane = new JScrollPane(messageConversationListPanel);
        conversationScrollPane.setBorder(null);
        conversationScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        conversationScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // 스크롤 속도 조절 (마우스 휠로 더 빠르게 스크롤)
        conversationScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        conversationScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        messagePanel.add(messageTopPanel, BorderLayout.NORTH);
        messagePanel.add(conversationScrollPane, BorderLayout.CENTER);
        
        // 대화 목록 새로고침
        refreshMessageConversations();
    }
    
    // DM 대화 목록 새로고침
    public void refreshMessageConversations() {
        if (messageConversationListPanel == null) return;
        
        messageConversationListPanel.removeAll();
        
        List<Message> conversations = messageDAO.getConversationList(loginUser.getUserId());
        
        if (conversations.isEmpty()) {
            JLabel emptyLabel = new JLabel("메시지가 없습니다.");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messageConversationListPanel.add(emptyLabel);
        } else {
            for (Message msg : conversations) {
                messageConversationListPanel.add(createMessageConversationPanel(msg));
            }
        }
        
        messageConversationListPanel.revalidate();
        messageConversationListPanel.repaint();
        
        // DM 버튼 업데이트 (읽지 않은 메시지 수 표시)
        SwingUtilities.invokeLater(() -> {
            if (dmNavBtn != null) {
                updateDMButton(dmNavBtn);
            }
        });
    }
    
    // DM 대화 패널 생성
    private JPanel createMessageConversationPanel(Message lastMessage) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 상대방 정보 가져오기
        String otherUserId = lastMessage.getSenderId().equals(loginUser.getUserId()) 
                           ? lastMessage.getReceiverId() 
                           : lastMessage.getSenderId();
        User otherUser = userDAO.getUserById(otherUserId);
        
        if (otherUser == null) return panel;
        
        // 왼쪽: 프로필 이미지와 사용자 정보를 한 줄에 배치
        JPanel leftPanel = new JPanel(new BorderLayout(10, 0));
        leftPanel.setBackground(Color.WHITE);
        
        // 프로필 이미지 (작은 크기)
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("Dialog", Font.PLAIN, 35));
        iconLabel.setPreferredSize(new Dimension(45, 45));
        if (otherUser.getProfileImagePath() != null && !otherUser.getProfileImagePath().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(otherUser.getProfileImagePath(), 45);
            if (circularIcon != null) {
                iconLabel.setIcon(circularIcon);
                iconLabel.setText("");
            }
        }
        
        // 사용자 정보 및 최근 메시지를 한 패널에
        JPanel infoPanel = new JPanel(new BorderLayout(0, 3));
        infoPanel.setBackground(Color.WHITE);
        
        // 이름과 시간을 한 줄에
        JPanel nameTimePanel = new JPanel(new BorderLayout());
        nameTimePanel.setBackground(Color.WHITE);
        JLabel nameLabel = new JLabel("<html><b>" + otherUser.getDisplayName() + "</b> <span style='color:gray;'>@" + otherUser.getUserId() + "</span></html>");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        nameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 사용자 이름/프로필 클릭 시 프로필 페이지로 이동
        nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                e.consume(); // 패널 클릭 이벤트 전파 방지
                showUserProfile(otherUser);
            }
        });
        
        // 프로필 이미지 클릭 시에도 프로필 페이지로 이동
        iconLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                e.consume(); // 패널 클릭 이벤트 전파 방지
                showUserProfile(otherUser);
            }
        });
        
        // 최근 메시지 미리보기
        String preview = lastMessage.getContent();
        if (preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }
        
        JLabel messagePreview = new JLabel(preview);
        messagePreview.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        messagePreview.setForeground(Color.GRAY);
        
        nameTimePanel.add(nameLabel, BorderLayout.WEST);
        infoPanel.add(nameTimePanel, BorderLayout.NORTH);
        infoPanel.add(messagePreview, BorderLayout.CENTER);
        
        leftPanel.add(iconLabel, BorderLayout.WEST);
        leftPanel.add(infoPanel, BorderLayout.CENTER);
        
        // 오른쪽: 시간, 읽음 상태, 읽지 않은 메시지 수
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        String timeAgo = TimeFormatter.formatRelativeTime(lastMessage.getCreatedAt());
        JLabel timeLabel = new JLabel(timeAgo);
        timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        rightPanel.add(timeLabel);
        
        // 내가 보낸 마지막 메시지가 있는지 확인하고, 읽음 상태 표시
        Message myLastMessage = messageDAO.getMyLastMessage(loginUser.getUserId(), otherUserId);
        if (myLastMessage != null && myLastMessage.isRead()) {
            // 읽은 시간 조회
            Timestamp readTime = messageDAO.getLastReadTime(loginUser.getUserId(), otherUserId);
            if (readTime != null) {
                String readTimeAgo = TimeFormatter.formatRelativeTime(readTime);
                JLabel readLabel = new JLabel("(" + readTimeAgo + " 확인함)");
                readLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
                readLabel.setForeground(new Color(100, 150, 200));
                readLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                readLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
                rightPanel.add(readLabel);
            }
        }
        
        // 읽지 않은 메시지 수 표시 (내가 받은 메시지인 경우만)
        int unreadCount = messageDAO.getUnreadCountFrom(loginUser.getUserId(), otherUserId);
        if (unreadCount > 0) {
            JLabel unreadLabel = new JLabel(String.valueOf(unreadCount));
            unreadLabel.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            unreadLabel.setForeground(Color.WHITE);
            unreadLabel.setBackground(Color.RED);
            unreadLabel.setOpaque(true);
            unreadLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            unreadLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            rightPanel.add(Box.createVerticalStrut(5));
            rightPanel.add(unreadLabel);
        }
        
        panel.add(leftPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);
        
        // 패널 클릭 시 대화창 열기
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ConversationFrame convFrame = new ConversationFrame(loginUser, otherUser, PostFrame.this);
                // 읽음 처리
                messageDAO.markAsRead(otherUserId, loginUser.getUserId());
                // 대화창이 닫힐 때 대화 목록 새로고침
                convFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        // 읽음 처리 후 대화 목록 새로고침 (읽지 않은 메시지 수 업데이트)
                        refreshMessageConversations();
                    }
                });
                // 대화창이 열릴 때도 즉시 새로고침 (읽지 않은 메시지 수 제거)
                SwingUtilities.invokeLater(() -> refreshMessageConversations());
            }
        });
        
        return panel;
    }
    
    
    // 알림 목록 새로고침
    public void refreshNotifications() {
        notificationPanel.removeAll();
        
        List<Notification> notifications = notificationDAO.getNotifications(loginUser.getUserId());
        
        if (notifications.isEmpty()) {
            JLabel emptyLabel = new JLabel("알림이 없습니다.");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            notificationPanel.add(emptyLabel);
        } else {
            for (Notification notif : notifications) {
                notificationPanel.add(createNotificationPanel(notif));
            }
        }
        
        notificationPanel.revalidate();
        notificationPanel.repaint();
        
        // 알림 버튼 업데이트 (읽지 않은 알림 수 표시)
        SwingUtilities.invokeLater(() -> {
            if (notificationNavBtn != null) {
                updateNotificationButton(notificationNavBtn);
            }
        });
    }
    
 // 리트윗 아이콘 생성 메서드 (최종 개선판: 화살표 머리 대폭 확대)
    private ImageIcon createRetweetIcon(int size, Color color) {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();

        // 1. 그래픽 품질 설정
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setColor(color);
        
        // 2. 선 두께 설정 (머리가 커진 만큼 선도 약간 더 두껍게 균형 맞춤)
        float strokeWidth = Math.max(2.4f, size / 6.5f);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 좌표 계산
        int padding = (int)(size * 0.15); // 여백
        float w = size - (padding * 2);
        float h = size - (padding * 2);
        float x = padding;
        float y = padding;

        // --- 위쪽 화살표 (왼쪽 -> 오른쪽) ---
        // 화살표 머리의 뒷부분 X좌표 (머리가 커질수록 이 값은 작아짐)
        float topArrowHeadBackX = x + w * 0.45f; 
        
        Path2D topArrow = new Path2D.Float();
        // 선 그리기: 꼬리(좌하단) -> 위 -> 오른쪽(머리 뒷부분까지 정확히 연결)
        topArrow.moveTo(x + w * 0.15, y + h * 0.55); 
        topArrow.lineTo(x + w * 0.15, y + h * 0.15); 
        topArrow.lineTo(topArrowHeadBackX, y + h * 0.15); 
        g2.draw(topArrow);

        // 화살표 머리 (대폭 확대)
        Path2D topHead = new Path2D.Float();
        // 날개를 더 뒤로(0.55 -> 0.45) 보내고, 위아래로 더 넓게 벌림(-0.05 -> -0.15)
        topHead.moveTo(topArrowHeadBackX, y - h * 0.15); // 위쪽 날개
        topHead.lineTo(x + w * 1.00, y + h * 0.15);      // 끝점 (중심)
        topHead.lineTo(topArrowHeadBackX, y + h * 0.45); // 아래쪽 날개
        topHead.closePath();
        g2.fill(topHead);

        // --- 아래쪽 화살표 (오른쪽 -> 왼쪽) ---
        // 화살표 머리의 뒷부분 X좌표 (머리가 커질수록 이 값은 커짐)
        float bottomArrowHeadBackX = x + w * 0.55f;

        Path2D bottomArrow = new Path2D.Float();
        // 선 그리기: 꼬리(우상단) -> 아래 -> 왼쪽(머리 뒷부분까지 정확히 연결)
        bottomArrow.moveTo(x + w * 0.85, y + h * 0.45);
        bottomArrow.lineTo(x + w * 0.85, y + h * 0.85);
        bottomArrow.lineTo(bottomArrowHeadBackX, y + h * 0.85); 
        g2.draw(bottomArrow);

        // 화살표 머리 (대폭 확대)
        Path2D bottomHead = new Path2D.Float();
        // 날개를 더 뒤로(0.45 -> 0.55) 보내고, 위아래로 더 넓게 벌림
        bottomHead.moveTo(bottomArrowHeadBackX, y + h * 0.55); // 위쪽 날개
        bottomHead.lineTo(x + w * 0.00, y + h * 0.85);         // 끝점 (중심)
        bottomHead.lineTo(bottomArrowHeadBackX, y + h * 1.15); // 아래쪽 날개
        bottomHead.closePath();
        g2.fill(bottomHead);

        g2.dispose();
        return new ImageIcon(icon);
    }
    
 // 네비게이션 아이콘 생성 메서드 (홈, 검색, 알림, DM) - 알림 아이콘 수정됨
    private ImageIcon createNavIcon(String type, boolean isSelected) {
        int size = 26; // 아이콘 크기
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();

        // 그래픽 품질 설정
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 색상 및 선 두께 설정
        g2.setColor(Color.BLACK);
        float strokeWidth = isSelected ? 2.5f : 1.8f; // 선택되면 더 두껍게
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int p = 3; // 패딩
        float w = size - (p * 2);
        float h = size - (p * 2);

        switch (type) {
            case "HOME":
                // 집 모양 (기존 동일)
                Path2D home = new Path2D.Float();
                home.moveTo(p + w/2, p);
                home.lineTo(p, p + h * 0.45);
                home.lineTo(p + w * 0.15, p + h * 0.45);
                home.lineTo(p + w * 0.15, p + h);
                home.lineTo(p + w * 0.85, p + h);
                home.lineTo(p + w * 0.85, p + h * 0.45);
                home.lineTo(p + w, p + h * 0.45);
                home.closePath();
                if (isSelected) g2.fill(home);
                else g2.draw(home);
                break;

            case "SEARCH":
                // 돋보기 모양 (기존 동일)
                float r = w * 0.4f;
                Ellipse2D glass = new Ellipse2D.Float(p, p, r * 2, r * 2);
                Path2D handle = new Path2D.Float();
                handle.moveTo(p + r * 1.5, p + r * 1.5);
                handle.lineTo(p + w, p + h);
                if (isSelected) {
                    g2.setStroke(new BasicStroke(strokeWidth + 1.0f));
                    g2.draw(glass);
                    g2.draw(handle);
                } else {
                    g2.draw(glass);
                    g2.draw(handle);
                }
                break;

            case "NOTIFICATION":
                // ▼▼▼ 수정된 벨 모양 (요청하신 이미지와 정확히 일치) ▼▼▼
                Path2D bell = new Path2D.Float();
                
                // 종 몸통 그리기 시작 (상단 중앙)
                bell.moveTo(p + w / 2.0, p); 
                
                // 오른쪽 곡선 (부드럽게 내려옴)
                bell.curveTo(p + w * 0.9, p + h * 0.1,  // 제어점 1
                             p + w, p + h * 0.35,       // 제어점 2
                             p + w, p + h * 0.85);      // 끝점 (오른쪽 하단)
                
                // 하단 직선 (오른쪽 -> 왼쪽)
                bell.lineTo(p, p + h * 0.85);
                
                // 왼쪽 곡선 (부드럽게 올라감)
                bell.curveTo(p, p + h * 0.35,           // 제어점 1
                             p + w * 0.1, p + h * 0.1,  // 제어점 2
                             p + w / 2.0, p);           // 끝점 (상단 중앙으로 복귀)
                
                bell.closePath();

                // 선택 상태에 따라 채우기 또는 테두리 그리기
                if (isSelected) {
                    g2.fill(bell);
                } else {
                    g2.draw(bell);
                }

                // 종 추 (하단 중앙의 작은 원) - 항상 채움
                float ringerRadius = w * 0.15f; // 추의 반지름 설정
                Ellipse2D ringer = new Ellipse2D.Float(
                        p + w / 2.0f - ringerRadius, // X 좌표
                        p + h * 0.85f,               // Y 좌표 (몸통 하단 라인에 맞춤)
                        ringerRadius * 2,            // 너비
                        ringerRadius * 2             // 높이
                );
                g2.fill(ringer);
                break;

            case "DM":
                // 편지봉투 모양 (기존 동일)
                Rectangle2D envelope = new Rectangle2D.Float(p, p + h * 0.15f, w, h * 0.7f);
                Path2D flap = new Path2D.Float();
                flap.moveTo(p, p + h * 0.15f);
                flap.lineTo(p + w/2, p + h * 0.55f);
                flap.lineTo(p + w, p + h * 0.15f);
                if (isSelected) {
                    g2.fill(envelope);
                    g2.setColor(Color.WHITE);
                    g2.draw(flap);
                } else {
                    g2.draw(envelope);
                    g2.draw(flap);
                }
                break;
        }

        g2.dispose();
        return new ImageIcon(icon);
    }
    
    // 원형 프로필 이미지 생성 헬퍼 메서드
    private ImageIcon createCircularProfileImage(String imagePath, int size) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            String trimmedPath = imagePath.trim();
            java.io.File imgFile = new java.io.File(trimmedPath);
            
            // 파일 존재 여부 확인
            if (!imgFile.exists() || !imgFile.isFile()) {
                return null;
            }
            
            // ImageIO를 사용하여 이미지 로드 (더 안정적)
            BufferedImage originalImage = null;
            try {
                originalImage = javax.imageio.ImageIO.read(imgFile);
            } catch (Exception e) {
                // ImageIO 실패 시 ImageIcon으로 시도
                ImageIcon originalIcon = new ImageIcon(trimmedPath);
                Image img = originalIcon.getImage();
                if (img == null) {
                    return null;
                }
                
                // Image를 BufferedImage로 변환
                originalImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = originalImage.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
            }
            
            if (originalImage == null || originalImage.getWidth() <= 0 || originalImage.getHeight() <= 0) {
                return null;
            }
            
            // 원형 이미지 생성
            BufferedImage circularImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = circularImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // 원형 클리핑 경로 설정
            java.awt.geom.Ellipse2D circle = new java.awt.geom.Ellipse2D.Float(0, 0, size, size);
            g2.setClip(circle);
            
            // 이미지 그리기 (비율 유지하며 중앙 정렬)
            int imgWidth = originalImage.getWidth();
            int imgHeight = originalImage.getHeight();
            double scale = Math.max((double)size / imgWidth, (double)size / imgHeight);
            int scaledWidth = (int)(imgWidth * scale);
            int scaledHeight = (int)(imgHeight * scale);
            int x = (size - scaledWidth) / 2;
            int y = (size - scaledHeight) / 2;
            
            g2.drawImage(originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH), x, y, null);
            g2.dispose();
            
            return new ImageIcon(circularImage);
        } catch (Exception e) {
            // 예외 발생 시 null 반환 (기본 아이콘 표시)
            e.printStackTrace();
            return null;
        }
    }
    
    // 알림 패널 생성
    private JPanel createNotificationPanel(Notification notif) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // 패널의 최대 너비 설정 (메시지 길이에 맞게 조정)
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setPreferredSize(new Dimension(0, 0)); // 내용에 맞게 높이 조정
        
        // 읽지 않은 알림만 배경색 설정
        if (!notif.isRead()) {
            panel.setBackground(new Color(240, 248, 255)); // 연한 파란색 배경
            panel.setOpaque(true);
        } else {
            panel.setBackground(Color.WHITE);
            panel.setOpaque(true);
        }
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 프로필 이미지 레이블
        JLabel profileImageLabel = new JLabel("👤", SwingConstants.CENTER);
        profileImageLabel.setPreferredSize(new Dimension(50, 50));
        profileImageLabel.setMinimumSize(new Dimension(50, 50));
        profileImageLabel.setMaximumSize(new Dimension(50, 50));
        profileImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profileImageLabel.setVerticalAlignment(SwingConstants.CENTER);
        profileImageLabel.setOpaque(false);
        profileImageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 30)); // 기본 아이콘 크기
        
        // 사용자 프로필 이미지 로드 (원형) - 타임라인과 동일한 방식
        String profileImagePath = null;
        if (notif.getActorId() != null) {
            User actorUser = userDAO.getUserById(notif.getActorId());
            if (actorUser != null) {
                profileImagePath = actorUser.getProfileImagePath();
            }
        }
        
        // Notification에서 가져온 경로도 시도
        if ((profileImagePath == null || profileImagePath.isEmpty())) {
            profileImagePath = notif.getActorProfileImagePath();
        }
        
        // 프로필 이미지 표시 (타임라인과 동일한 로직)
        if (profileImagePath != null && !profileImagePath.trim().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(profileImagePath, 50);
            if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                profileImageLabel.setIcon(circularIcon);
                profileImageLabel.setText("");
                profileImageLabel.setPreferredSize(new Dimension(50, 50));
            } else {
                profileImageLabel.setIcon(null);
                profileImageLabel.setText("👤");
                profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 30));
            }
        } else {
            profileImageLabel.setIcon(null);
            profileImageLabel.setText("👤");
            profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 30));
        }
        
        // 프로필 이미지 클릭 시 프로필로 이동
        profileImageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                e.consume(); // 패널 클릭 이벤트 전파 방지
                String actorId = notif.getActorId();
                if (actorId == null || actorId.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(PostFrame.this, "사용자 정보를 찾을 수 없습니다.");
                    return;
                }
                User actorUser = userDAO.getUserById(actorId);
                if (actorUser != null) {
                    // 읽지 않은 알림만 읽음 처리
                    if (!notif.isRead()) {
                        notificationDAO.markAsRead(notif.getNotificationId());
                        refreshNotifications();
                    }
                    showUserProfile(actorUser);
                } else {
                    JOptionPane.showMessageDialog(PostFrame.this, "사용자 정보를 찾을 수 없습니다: " + actorId);
                }
            }
        });
        
        // 아이콘 (타입별)
        String icon = "";
        String actionText = "";
        switch (notif.getType()) {
            case "like":
                icon = "♥";
                actionText = "좋아요를 눌렀습니다";
                break;
            case "comment":
                icon = "💬";
                actionText = "답글을 남겼습니다";
                break;
            case "retweet":
                icon = "🔄";
                actionText = "리트윗했습니다";
                break;
            case "quote":
                icon = "📝";
                actionText = "인용했습니다";
                break;
            case "follow":
                icon = "👤";
                actionText = "팔로우했습니다";
                break;
        }
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Dialog", Font.PLAIN, 20));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        
        // 알림 내용
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.setPreferredSize(new Dimension(0, 0)); // 내용에 맞게 크기 조정
        infoPanel.setMaximumSize(new Dimension(500, Integer.MAX_VALUE)); // 최대 너비 제한
        
        String actorName = notif.getActorDisplayName() != null ? notif.getActorDisplayName() : notif.getActorId();
        String notificationText = "";
        
        // 알림 타입에 따른 메시지 생성 (줄바꿈 처리)
        if ("like".equals(notif.getType())) {
            String postContent = notif.getPostContent();
            if (postContent != null && !postContent.isEmpty()) {
                // 긴 내용은 줄바꿈 처리
                String escapedContent = escapeHtml(postContent);
                if (escapedContent.length() > 50) {
                    escapedContent = escapedContent.substring(0, 50) + "...";
                }
                notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                                  "<b>" + escapeHtml(actorName) + "</b> 님이 <b>" + escapedContent + "</b> 포스트에 좋아요를 눌렀습니다" +
                                  "</div></html>";
            } else {
                notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                                  "<b>" + escapeHtml(actorName) + "</b> 님이 포스트에 좋아요를 눌렀습니다" +
                                  "</div></html>";
            }
        } else if ("retweet".equals(notif.getType())) {
            // 리트윗 알림: 원본 포스트 내용 표시
            String postContent = notif.getPostContent();
            if (postContent != null && !postContent.isEmpty()) {
                String escapedContent = escapeHtml(postContent);
                if (escapedContent.length() > 50) {
                    escapedContent = escapedContent.substring(0, 50) + "...";
                }
                notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                                  "<b>" + escapeHtml(actorName) + "</b> 님이 <b>" + escapedContent + "</b> 포스트를 리트윗했습니다" +
                                  "</div></html>";
            } else {
                notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                                  "<b>" + escapeHtml(actorName) + "</b> 님이 포스트를 리트윗했습니다" +
                                  "</div></html>";
            }
        } else if ("follow".equals(notif.getType())) {
            notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                              "<b>" + escapeHtml(actorName) + "</b> 님이 팔로우했습니다" +
                              "</div></html>";
        } else {
            // 답글, 인용 등
            String postContent = notif.getPostContent();
            if (postContent != null && !postContent.isEmpty() && ("comment".equals(notif.getType()) || "quote".equals(notif.getType()))) {
                String escapedContent = escapeHtml(postContent);
                if (escapedContent.length() > 50) {
                    escapedContent = escapedContent.substring(0, 50) + "...";
                }
                notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                                  "<b>" + escapeHtml(actorName) + "</b> 님이 " + actionText + "<br>" +
                                  "<span style='color:gray;'>" + escapedContent + "</span>" +
                                  "</div></html>";
            } else {
                notificationText = "<html><div style='width: 450px; word-wrap: break-word; white-space: normal;'>" +
                              "<b>" + escapeHtml(actorName) + "</b> 님이 " + actionText +
                              "</div></html>";
            }
        }
        
        JLabel actionLabel = new JLabel(notificationText);
        actionLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        actionLabel.setVerticalAlignment(SwingConstants.TOP);
        actionLabel.setAlignmentY(Component.TOP_ALIGNMENT);
        actionLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 이름 클릭 시 프로필로 이동 (HTML 내부의 이름 부분 클릭 감지)
        actionLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // 이름 부분 클릭 시 프로필로 이동
                String actorId = notif.getActorId();
                if (actorId == null || actorId.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(PostFrame.this, "사용자 정보를 찾을 수 없습니다.");
                    return;
                }
                User actorUser = userDAO.getUserById(actorId);
                if (actorUser != null) {
                    // 읽지 않은 알림만 읽음 처리
                    if (!notif.isRead()) {
                        notificationDAO.markAsRead(notif.getNotificationId());
                        refreshNotifications();
                    }
                    showUserProfile(actorUser);
                } else {
                    JOptionPane.showMessageDialog(PostFrame.this, "사용자 정보를 찾을 수 없습니다: " + actorId);
                }
            }
        });
        
        // 시간 라벨을 알림 텍스트 아래에 배치
        String timeAgo = TimeFormatter.formatRelativeTime(notif.getCreatedAt());
        JLabel timeLabel = new JLabel(timeAgo);
        timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(actionLabel, BorderLayout.NORTH);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        infoPanel.add(contentPanel, BorderLayout.CENTER);
        
        // 프로필 이미지와 타입 아이콘을 함께 배치
        JPanel leftPanel = new JPanel(new BorderLayout(5, 0));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(70, 50));
        leftPanel.setMinimumSize(new Dimension(70, 50));
        
        // 프로필 이미지를 별도 패널에 넣어서 크기 보장
        JPanel profileImagePanel = new JPanel(new BorderLayout());
        profileImagePanel.setOpaque(false);
        profileImagePanel.setPreferredSize(new Dimension(50, 50));
        profileImagePanel.add(profileImageLabel, BorderLayout.CENTER);
        
        leftPanel.add(profileImagePanel, BorderLayout.WEST);
        leftPanel.add(iconLabel, BorderLayout.CENTER);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        // 클릭 시 트윗 상세보기 또는 프로필 보기
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // 읽지 않은 알림만 읽음 처리
                if (!notif.isRead()) {
                    notificationDAO.markAsRead(notif.getNotificationId());
                    // 배경색 제거를 위해 즉시 새로고침
                    refreshNotifications();
                }
                
                if ("follow".equals(notif.getType())) {
                    // 팔로우 알림은 프로필로 이동
                    String actorId = notif.getActorId();
                    if (actorId != null && !actorId.trim().isEmpty()) {
                        User actorUser = userDAO.getUserById(actorId);
                        if (actorUser != null) {
                            showUserProfile(actorUser);
                        } else {
                            JOptionPane.showMessageDialog(PostFrame.this, "사용자 정보를 찾을 수 없습니다: " + actorId);
                        }
                    }
                } else if (notif.getPostId() != null) {
                    // 리트윗 알림의 경우 원본 포스트로 이동
                    if ("retweet".equals(notif.getType())) {
                        Post retweetPost = postDAO.getPostById(notif.getPostId());
                        if (retweetPost != null && retweetPost.getParentPostId() != null) {
                            Post originalPost = postDAO.getPostById(retweetPost.getParentPostId());
                            if (originalPost != null) {
                                showPostDetail(originalPost);
                                return;
                            }
                        }
                    }
                    // 다른 알림은 트윗 상세보기
                    Post post = postDAO.getPostById(notif.getPostId());
                    if (post != null) {
                        showPostDetail(post);
                    }
                }
            }
        });
        
        return panel;
    }
    
    // 검색 탭에서 검색 결과 표시
    private void showSearchResultsInTab(String query) {
        // searchResultsPanel이 null이면 초기화 시도
        if (searchResultsPanel == null || userSearchResultsPanel == null) {
            // 검색 패널에서 검색 결과 패널 찾기
            if (searchPanel != null) {
                findSearchPanels(searchPanel);
            }
            
            // 그래도 null이면 새로 생성
            if (searchResultsPanel == null) {
                searchResultsPanel = new JPanel();
                searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
            }
            if (userSearchResultsPanel == null) {
                userSearchResultsPanel = new JPanel();
                userSearchResultsPanel.setLayout(new BoxLayout(userSearchResultsPanel, BoxLayout.Y_AXIS));
            }
        }
        
        // 검색어 정리
        String trimmedQuery = (query == null) ? "" : query.trim();
        if (trimmedQuery.equals(DEFAULT_SEARCH_TEXT)) {
            trimmedQuery = "";
        }
        
        // 트윗/답글 검색 결과 패널 업데이트
        searchResultsPanel.removeAll();
        
        // 사용자 검색 결과 패널 업데이트
        if (userSearchResultsPanel != null) {
            userSearchResultsPanel.removeAll();
        }
        
        // 검색어가 비어있으면 안내 메시지 표시
        if (trimmedQuery.isEmpty()) {
            JLabel hintLabel = new JLabel("검색어를 입력하고 검색 버튼을 누르세요.");
            hintLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            hintLabel.setForeground(Color.GRAY);
            hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            searchResultsPanel.add(Box.createVerticalGlue());
            searchResultsPanel.add(hintLabel);
            searchResultsPanel.add(Box.createVerticalGlue());
            
            if (userSearchResultsPanel != null) {
                JLabel userHintLabel = new JLabel("검색어를 입력하고 검색 버튼을 누르세요.");
                userHintLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                userHintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                userHintLabel.setForeground(Color.GRAY);
                userHintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                userSearchResultsPanel.add(Box.createVerticalGlue());
                userSearchResultsPanel.add(userHintLabel);
                userSearchResultsPanel.add(Box.createVerticalGlue());
            }
        } else {
            // 트윗 검색 결과
            List<Post> tweetResults = postDAO.searchPosts(trimmedQuery);
            // 답글 검색 결과
            List<Comment> commentResults = commentDAO.searchComments(trimmedQuery);
            // 사용자 검색 결과
            List<User> userResults = userDAO.searchUsers(trimmedQuery);
            
            // 트윗/답글 탭 업데이트
            if (tweetResults.isEmpty() && commentResults.isEmpty()) {
                JLabel noResult = new JLabel("'" + trimmedQuery + "'에 대한 트윗/답글 검색 결과가 없습니다.");
                noResult.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                noResult.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                noResult.setAlignmentX(Component.CENTER_ALIGNMENT);
                searchResultsPanel.add(Box.createVerticalGlue());
                searchResultsPanel.add(noResult);
                searchResultsPanel.add(Box.createVerticalGlue());
            } else {
                // 트윗 표시
                for (Post post : tweetResults) {
                    searchResultsPanel.add(createSearchTweetPanelForTab(post, trimmedQuery));
                }
                // 답글 표시
                for (Comment comment : commentResults) {
                    searchResultsPanel.add(createSearchCommentPanelForTab(comment, trimmedQuery));
                }
            }
            
            // 사용자 탭 업데이트
            if (userSearchResultsPanel != null) {
                if (userResults.isEmpty()) {
                    JLabel noUserResult = new JLabel("'" + trimmedQuery + "'에 대한 사용자 검색 결과가 없습니다.");
                    noUserResult.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    noUserResult.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                    noUserResult.setAlignmentX(Component.CENTER_ALIGNMENT);
                    userSearchResultsPanel.add(Box.createVerticalGlue());
                    userSearchResultsPanel.add(noUserResult);
                    userSearchResultsPanel.add(Box.createVerticalGlue());
                } else {
                    for (User user : userResults) {
                        userSearchResultsPanel.add(createSearchUserPanelForTab(user, trimmedQuery));
                    }
                }
                userSearchResultsPanel.revalidate();
                userSearchResultsPanel.repaint();
            }
        }
        
        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
    }
    
    // 검색 패널에서 검색 결과 패널 찾기
    private void findSearchPanels(JPanel searchPanel) {
        for (Component comp : searchPanel.getComponents()) {
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) comp;
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component tabComponent = tabbedPane.getComponentAt(i);
                    if (tabComponent instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) tabComponent;
                        Component view = scrollPane.getViewport().getView();
                        if (view instanceof JPanel) {
                            if (i == 0) { // 첫 번째 탭 (최근)
                                searchResultsPanel = (JPanel) view;
                            } else if (i == 1) { // 두 번째 탭 (사용자)
                                userSearchResultsPanel = (JPanel) view;
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 검색 탭용 트윗 패널 생성 (null parentDialog 대응)
    private JPanel createSearchTweetPanelForTab(Post post, String query) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // 작성자 정보 (검색어 하이라이트)
        String writerText = "<html><b>" + highlightText(post.getDisplayName(), query) + "</b> " +
                           "<span style='color:gray;'>@" + highlightText(post.getWriterId(), query) + "</span></html>";
        JLabel writerLabel = new JLabel(writerText);
        writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        writerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        writerLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                User clickedUser = userDAO.getUserById(post.getWriterId());
                if (clickedUser != null) {
                    showUserProfile(clickedUser);
                }
            }
        });
        
        // 트윗 내용 (검색어 하이라이트)
        String highlightedContent = "<html><div style='width:550px;'>" + 
                                   highlightText(escapeHtml(post.getContent()), query).replace("\n", "<br>") + 
                                   "</div></html>";
        JLabel contentLabel = new JLabel(highlightedContent);
        contentLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        contentLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        contentLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showPostDetail(post);
            }
        });
        
        panel.add(writerLabel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // 검색 탭용 답글 패널 생성 (null parentDialog 대응)
    private JPanel createSearchCommentPanelForTab(Comment comment, String query) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // 작성자 정보 (검색어 하이라이트)
        String writerText = "<html><b>" + highlightText(comment.getDisplayName(), query) + "</b> " +
                           "<span style='color:gray;'>@" + highlightText(comment.getWriterId(), query) + " · 답글</span></html>";
        JLabel writerLabel = new JLabel(writerText);
        writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        
        // 답글 내용 (검색어 하이라이트)
        String highlightedContent = "<html><div style='width:550px;'>" + 
                                   highlightText(escapeHtml(comment.getContent()), query).replace("\n", "<br>") + 
                                   "</div></html>";
        JLabel contentLabel = new JLabel(highlightedContent);
        contentLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        contentLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        contentLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Post originalPost = postDAO.getPostById(comment.getPostId());
                if (originalPost != null) {
                    showPostDetail(originalPost);
                }
            }
        });
        
        panel.add(writerLabel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        
        return panel;
    }
    private void showPostDialog() {
        JDialog postDialog = new JDialog(this, "새 트윗 작성", true);
        postDialog.setSize(500, 450);
        postDialog.setLocationRelativeTo(this);
        postDialog.setLayout(new BorderLayout(10, 10));

        JTextArea postArea = new JTextArea();
        postArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        postArea.setLineWrap(true);
        postArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(postArea);
        
        // 첨부 파일 정보 저장 변수
        final String[] selectedImagePath = {null};
        final String[] selectedVideoPath = {null};
        
        // 첨부 파일 미리보기 패널
        JPanel mediaPreviewPanel = new JPanel(new BorderLayout());
        mediaPreviewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mediaPreviewPanel.setPreferredSize(new Dimension(0, 200));
        JLabel mediaPreviewLabel = new JLabel();
        mediaPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mediaPreviewPanel.add(mediaPreviewLabel, BorderLayout.CENTER);
        mediaPreviewPanel.setVisible(false);
        
        JPanel topBar = new JPanel(new BorderLayout());
        JButton closeBtn = new JButton("취소");
        JButton postBtn = new JButton("게시");
        
        // 💡 게시 버튼 스타일링
        postBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        postBtn.setBackground(new Color(240, 240, 240));
        postBtn.setForeground(Color.DARK_GRAY);
        postBtn.setFocusPainted(false);
        postBtn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        postBtn.setOpaque(true);
        postBtn.setPreferredSize(new Dimension(80, 35));
        postBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        closeBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 사진 추가 버튼
        JButton addImageBtn = new JButton("📷 사진");
        addImageBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        addImageBtn.setFocusPainted(false);
        addImageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addImageBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "이미지 파일", "jpg", "jpeg", "png", "gif", "bmp"));
            int result = fileChooser.showOpenDialog(postDialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                selectedImagePath[0] = selectedFile.getAbsolutePath();
                selectedVideoPath[0] = null; // 사진 선택 시 동영상 초기화
                
                // 미리보기 표시
                try {
                    ImageIcon icon = new ImageIcon(selectedImagePath[0]);
                    Image img = icon.getImage().getScaledInstance(400, 200, Image.SCALE_SMOOTH);
                    mediaPreviewLabel.setIcon(new ImageIcon(img));
                    mediaPreviewPanel.setVisible(true);
                    postDialog.pack();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(postDialog, "이미지 로드 실패: " + ex.getMessage());
                }
            }
        });
        
        // 동영상 추가 버튼
        JButton addVideoBtn = new JButton("🎬 동영상");
        addVideoBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        addVideoBtn.setFocusPainted(false);
        addVideoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addVideoBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "동영상 파일", "mp4", "avi", "mov", "wmv", "mkv"));
            int result = fileChooser.showOpenDialog(postDialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                selectedVideoPath[0] = selectedFile.getAbsolutePath();
                selectedImagePath[0] = null; // 동영상 선택 시 사진 초기화
                
                // 미리보기 표시 (동영상 아이콘)
                mediaPreviewLabel.setIcon(null);
                mediaPreviewLabel.setText("<html><b>동영상 선택됨:</b><br>" + selectedFile.getName() + "</html>");
                mediaPreviewPanel.setVisible(true);
                postDialog.pack();
            }
        });
        
        // 첨부 파일 제거 버튼
        JButton removeMediaBtn = new JButton("✖ 제거");
        removeMediaBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        removeMediaBtn.setFocusPainted(false);
        removeMediaBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeMediaBtn.addActionListener(e -> {
            selectedImagePath[0] = null;
            selectedVideoPath[0] = null;
            mediaPreviewLabel.setIcon(null);
            mediaPreviewLabel.setText("");
            mediaPreviewPanel.setVisible(false);
            postDialog.pack();
        });
        
        JPanel mediaButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        mediaButtonPanel.add(addImageBtn);
        mediaButtonPanel.add(addVideoBtn);
        
        closeBtn.addActionListener(e -> postDialog.dispose());
        postBtn.addActionListener(e -> {
            if (writePost(postArea.getText(), selectedImagePath[0], selectedVideoPath[0])) {
                postDialog.dispose();
            }
        });
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonWrapper.add(postBtn);
        
        topBar.add(closeBtn, BorderLayout.WEST);
        topBar.add(mediaButtonPanel, BorderLayout.CENTER);
        topBar.add(buttonWrapper, BorderLayout.EAST);

        postDialog.add(topBar, BorderLayout.NORTH);
        postDialog.add(scrollPane, BorderLayout.CENTER);
        postDialog.add(mediaPreviewPanel, BorderLayout.SOUTH);

        postDialog.setVisible(true);
    }
    
    private boolean writePost(String content, String imagePath, String videoPath) {
        if ((content == null || content.trim().isEmpty()) && imagePath == null && videoPath == null) {
            JOptionPane.showMessageDialog(this, "내용 또는 사진/동영상을 입력하세요!");
            return false;
        }

        content = (content != null) ? content.trim() : "";
        String postId = "p" + System.currentTimeMillis();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        // 15개 인자 생성자 호출 (imagePath, videoPath 포함)
        Post post = new Post(
                postId,
                loginUser.getUserId(),
                loginUser.getDisplayName(),
                content,
                0, now, now, 0, 0, 
                loginUser.getProfileImagePath(),
                "tweet",
                0,
                null,
                imagePath,
                videoPath
        );

        boolean success = postDAO.writePost(post);
        if (success) {
            refreshTimeline();
        } 
        return success;
    }

    // ----------------------------------------------------
    // R(Read) - 타임라인 로드/갱신
    // ----------------------------------------------------
    public void refreshTimeline() {
        timelinePanel.removeAll();

        List<Post> posts = postDAO.getTimelinePosts(loginUser.getUserId()); 

        for (Post p : posts) {
            // 리트윗/인용 글 처리
            if (p.getPostType() != null && (p.getPostType().equals("retweet") || p.getPostType().equals("quote"))) {
                displayInteractionPost(p);
                continue;
            }
            
            // 일반 게시글 표시 - displayNormalPost 메서드 사용
            displayNormalPost(p);
        }

        timelinePanel.revalidate();
        timelinePanel.repaint();
        
        // 스크롤 위치를 상단으로 설정
        SwingUtilities.invokeLater(() -> {
            if (homeScrollPane != null) {
                JScrollBar verticalScrollBar = homeScrollPane.getVerticalScrollBar();
                if (verticalScrollBar != null) {
                    verticalScrollBar.setValue(0); // 스크롤을 맨 위로
                }
            }
        });
    }
    
    // ----------------------------------------------------
    // S(Search) - 검색 액션 및 헬퍼 메서드 
    // ----------------------------------------------------
    private void searchAction() {
        String query = searchField.getText().trim();
        
        if (query.equals(DEFAULT_SEARCH_TEXT) || query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "검색어를 입력해주세요.");
            return;
        }
        
        showSearchDialog(query);
    }
    
    private void showSearchDialog(String query) {
        JDialog searchDialog = new JDialog(this, "검색 결과: " + query, false);
        searchDialog.setSize(700, 600);
        searchDialog.setLocationRelativeTo(this);
        searchDialog.setLayout(new BorderLayout());
        
        // 탭 패널 생성
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 트윗 및 답글 검색 탭
        JPanel tweetPanel = new JPanel(new BorderLayout());
        JPanel tweetListPanel = new JPanel();
        tweetListPanel.setLayout(new BoxLayout(tweetListPanel, BoxLayout.Y_AXIS));
        
        List<Post> tweetResults = postDAO.searchPosts(query);
        List<Comment> commentResults = commentDAO.searchComments(query);
        
        if (tweetResults.isEmpty() && commentResults.isEmpty()) {
            JLabel noResult = new JLabel("검색 결과가 없습니다.");
            noResult.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            tweetListPanel.add(noResult);
        } else {
            // 트윗 표시
            for (Post post : tweetResults) {
                tweetListPanel.add(createSearchTweetPanel(post, searchDialog));
            }
            
            // 답글 표시
            for (Comment comment : commentResults) {
                tweetListPanel.add(createSearchCommentPanel(comment, searchDialog, query));
            }
        }
        
        JScrollPane tweetScrollPane = new JScrollPane(tweetListPanel);
        tweetPanel.add(tweetScrollPane, BorderLayout.CENTER);
        
        // 사용자 검색 탭
        JPanel userPanel = new JPanel(new BorderLayout());
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        
        List<User> userResults = userDAO.searchUsers(query);
        if (userResults.isEmpty()) {
            JLabel noResult = new JLabel("사용자 검색 결과가 없습니다.");
            noResult.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            userListPanel.add(noResult);
        } else {
            for (User user : userResults) {
                userListPanel.add(createSearchUserPanel(user, searchDialog));
            }
        }
        
        JScrollPane userScrollPane = new JScrollPane(userListPanel);
        userPanel.add(userScrollPane, BorderLayout.CENTER);
        
        tabbedPane.addTab("최근", tweetPanel);
        tabbedPane.addTab("사용자", userPanel);
        
        searchDialog.add(tabbedPane, BorderLayout.CENTER);
        searchDialog.setVisible(true);
    }
    
    private JPanel createSearchTweetPanel(Post post, JDialog parentDialog) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // 검색어 추출
        String query = parentDialog.getTitle().replace("검색 결과: ", "");
        
        // 작성자 정보 (검색어 하이라이트)
        String writerText = "<html><b>" + highlightText(post.getDisplayName(), query) + "</b> " +
                           "<span style='color:gray;'>@" + highlightText(post.getWriterId(), query) + "</span></html>";
        JLabel writerLabel = new JLabel(writerText);
        writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        
        // 트윗 내용 (검색어 하이라이트) - JLabel로 변경하여 HTML 지원
        String highlightedContent = "<html><div style='width:550px;'>" + 
                                   highlightText(escapeHtml(post.getContent()), query).replace("\n", "<br>") + 
                                   "</div></html>";
        JLabel contentLabel = new JLabel(highlightedContent);
        contentLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // 상세보기 버튼
        JButton viewBtn = new JButton("상세보기");
        viewBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        viewBtn.setBackground(new Color(240, 240, 240));
        viewBtn.setForeground(Color.DARK_GRAY);
        viewBtn.setFocusPainted(false);
        viewBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        viewBtn.addActionListener(e -> {
            parentDialog.dispose();
            showPostDetail(post);
        });
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(viewBtn);
        
        panel.add(writerLabel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSearchCommentPanel(Comment comment, JDialog parentDialog, String query) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // 작성자 정보 (검색어 하이라이트)
        String writerText = "<html><b>" + highlightText(comment.getDisplayName(), query) + "</b> " +
                           "<span style='color:gray;'>@" + highlightText(comment.getWriterId(), query) + " · 답글</span></html>";
        JLabel writerLabel = new JLabel(writerText);
        writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        
        // 답글 내용 (검색어 하이라이트)
        String highlightedContent = "<html><div style='width:550px;'>" + 
                                   highlightText(escapeHtml(comment.getContent()), query).replace("\n", "<br>") + 
                                   "</div></html>";
        JLabel contentLabel = new JLabel(highlightedContent);
        contentLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // 좋아요 표시
        JLabel likeLabel = new JLabel("♥ " + comment.getNumOfLikes());
        likeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        likeLabel.setForeground(Color.GRAY);
        
        // 원본 트윗 보기 버튼
        JButton viewPostBtn = new JButton("원본 트윗 보기");
        viewPostBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        viewPostBtn.setBackground(new Color(240, 240, 240));
        viewPostBtn.setForeground(Color.DARK_GRAY);
        viewPostBtn.setFocusPainted(false);
        viewPostBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        viewPostBtn.addActionListener(e -> {
            Post originalPost = postDAO.getPostById(comment.getPostId());
            if (originalPost != null) {
                parentDialog.dispose();
                showPostDetail(originalPost);
            }
        });
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        bottomPanel.add(likeLabel);
        bottomPanel.add(viewPostBtn);
        
        panel.add(writerLabel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // 검색 탭용 사용자 패널 생성
    private JPanel createSearchUserPanelForTab(User user, String query) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        panel.setBackground(Color.WHITE);
        // 최대 너비 제한 (600px)
        panel.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 프로필 이미지 표시 (원형, 40x40)
        JLabel profilePicLabel = new JLabel("👤", SwingConstants.CENTER);
        profilePicLabel.setPreferredSize(new Dimension(40, 40));
        profilePicLabel.setMinimumSize(new Dimension(40, 40));
        profilePicLabel.setMaximumSize(new Dimension(40, 40));
        profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profilePicLabel.setVerticalAlignment(SwingConstants.TOP);
        profilePicLabel.setAlignmentY(Component.TOP_ALIGNMENT);
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(user.getProfileImagePath(), 40);
            if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                profilePicLabel.setIcon(circularIcon);
                profilePicLabel.setText("");
            } else {
                profilePicLabel.setIcon(null);
                profilePicLabel.setText("👤");
            }
        } else {
            profilePicLabel.setIcon(null);
            profilePicLabel.setText("👤");
        }
        
        // 프로필 이미지 클릭 시 프로필 보기
        profilePicLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showUserProfile(user);
            }
        });
        
        // 사용자 정보 (검색어 하이라이트)
        String nameText = "<html><b>" + highlightText(user.getDisplayName(), query) + "</b> " +
                         "<span style='color:gray;'>@" + highlightText(user.getUserId(), query) + "</span></html>";
        JLabel nameLabel = new JLabel(nameText);
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        nameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showUserProfile(user);
            }
        });
        
        String bioText = user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "소개가 없습니다.";
        String highlightedBio = "<html>" + highlightText(escapeHtml(bioText), query) + "</html>";
        JLabel bioLabel = new JLabel(highlightedBio);
        bioLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        bioLabel.setForeground(Color.GRAY);
        
        // 이름과 팔로우 버튼을 함께 표시할 패널
        JPanel nameAndFollowPanel = new JPanel(new BorderLayout());
        nameAndFollowPanel.setBackground(Color.WHITE);
        nameAndFollowPanel.add(nameLabel, BorderLayout.CENTER);
        
        // 팔로우 버튼 (자기 자신이 아닌 경우에만)
        if (!user.getUserId().equals(loginUser.getUserId())) {
            boolean isFollowing = followDAO.isFollowing(loginUser.getUserId(), user.getUserId());
            JButton followBtn = new JButton(isFollowing ? "팔로잉" : "팔로우");
            followBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            followBtn.setPreferredSize(new Dimension(90, 32));
            followBtn.setFocusPainted(false);
            followBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            if (isFollowing) {
                followBtn.setBackground(Color.BLACK);
                followBtn.setForeground(Color.WHITE);
                followBtn.setBorderPainted(false);
                followBtn.setContentAreaFilled(true);
            } else {
                followBtn.setBackground(Color.WHITE);
                followBtn.setForeground(Color.BLACK);
                followBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)
                ));
                followBtn.setContentAreaFilled(true);
            }
            
            followBtn.setOpaque(true);
            
            followBtn.addActionListener(e -> {
                boolean success = followDAO.toggleFollow(loginUser.getUserId(), user.getUserId());
                if (success) {
                    // 버튼 상태 업데이트
                    boolean newFollowingState = followDAO.isFollowing(loginUser.getUserId(), user.getUserId());
                    followBtn.setText(newFollowingState ? "팔로잉" : "팔로우");
                    
                    if (newFollowingState) {
                        followBtn.setBackground(Color.BLACK);
                        followBtn.setForeground(Color.WHITE);
                        followBtn.setBorderPainted(false);
                        followBtn.setContentAreaFilled(true);
                    } else {
                        followBtn.setBackground(Color.WHITE);
                        followBtn.setForeground(Color.BLACK);
                        followBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(5, 15, 5, 15)
                        ));
                        followBtn.setContentAreaFilled(true);
                    }
                    
                    // 타임라인 갱신
                    refreshTimeline();
                }
            });
            
            nameAndFollowPanel.add(followBtn, BorderLayout.EAST);
        }
        
        // 오른쪽 패널: 이름/팔로우 버튼과 바이오를 세로로 배치
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        rightPanel.add(nameAndFollowPanel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(bioLabel);
        
        // 메인 패널: 프로필 이미지(왼쪽)와 오른쪽 패널(이름+바이오)을 가로로 배치
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 0));
        mainContentPanel.setBackground(Color.WHITE);
        mainContentPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // 프로필 이미지를 감싸는 패널 (상단 정렬을 위해)
        JPanel profileWrapper = new JPanel();
        profileWrapper.setLayout(new BoxLayout(profileWrapper, BoxLayout.Y_AXIS));
        profileWrapper.setBackground(Color.WHITE);
        profileWrapper.setPreferredSize(new Dimension(40, 40));
        profileWrapper.setMaximumSize(new Dimension(40, 40));
        profileWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
        profileWrapper.add(profilePicLabel);
        
        mainContentPanel.add(profileWrapper, BorderLayout.WEST);
        mainContentPanel.add(rightPanel, BorderLayout.CENTER);
        
        panel.add(mainContentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSearchUserPanel(User user, JDialog parentDialog) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // 검색어 추출
        String query = parentDialog.getTitle().replace("검색 결과: ", "");
        
        // 사용자 정보 (검색어 하이라이트)
        String nameText = "<html><b>" + highlightText(user.getDisplayName(), query) + "</b> " +
                         "<span style='color:gray;'>@" + highlightText(user.getUserId(), query) + "</span></html>";
        JLabel nameLabel = new JLabel(nameText);
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        String bioText = user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "소개가 없습니다.";
        String highlightedBio = "<html>" + highlightText(escapeHtml(bioText), query) + "</html>";
        JLabel bioLabel = new JLabel(highlightedBio);
        bioLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        bioLabel.setForeground(Color.GRAY);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(bioLabel);
        
        // 팔로우 버튼
        boolean isFollowing = followDAO.isFollowing(loginUser.getUserId(), user.getUserId());
        JButton followBtn = new JButton(isFollowing ? "팔로잉" : "팔로우");
        followBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        followBtn.setFocusPainted(false);
        followBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (isFollowing) {
            followBtn.setBackground(Color.BLACK);
            followBtn.setForeground(Color.WHITE);
            followBtn.setBorderPainted(false);
            followBtn.setContentAreaFilled(true);
        } else {
            followBtn.setBackground(Color.WHITE);
            followBtn.setForeground(Color.BLACK);
            followBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
            ));
            followBtn.setContentAreaFilled(true);
        }
        followBtn.setOpaque(true);
        
        // 자기 자신이면 팔로우 버튼 숨기기
        if (user.getUserId().equals(loginUser.getUserId())) {
            followBtn.setVisible(false);
        }
        
        followBtn.addActionListener(e -> {
            followDAO.toggleFollow(loginUser.getUserId(), user.getUserId());
            boolean nowFollowing = followDAO.isFollowing(loginUser.getUserId(), user.getUserId());
            followBtn.setText(nowFollowing ? "팔로잉" : "팔로우");
            
            if (nowFollowing) {
                followBtn.setBackground(Color.BLACK);
                followBtn.setForeground(Color.WHITE);
                followBtn.setBorderPainted(false);
                followBtn.setContentAreaFilled(true);
            } else {
                followBtn.setBackground(Color.WHITE);
                followBtn.setForeground(Color.BLACK);
                followBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(4, 12, 4, 12)
                ));
                followBtn.setContentAreaFilled(true);
            }
        });
        
        // 프로필 보기 버튼
        JButton profileBtn = new JButton("프로필 보기");
        profileBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        profileBtn.setBackground(new Color(240, 240, 240));
        profileBtn.setForeground(Color.DARK_GRAY);
        profileBtn.setFocusPainted(false);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        
        profileBtn.addActionListener(e -> {
            parentDialog.dispose();
            showUserProfile(user);
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(profileBtn);
        buttonPanel.add(followBtn);
        
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void displaySearchResults(List<Post> posts) {
        // refreshTimeline과 동일한 로직으로 검색 결과 표시
        for (Post p : posts) {
            // 리트윗/인용 글 처리
            if (p.getPostType() != null && (p.getPostType().equals("retweet") || p.getPostType().equals("quote"))) {
                displayInteractionPost(p);
                continue;
            }
            
            // 일반 게시글 표시 (refreshTimeline과 동일한 로직)
            displayNormalPost(p);
        }
    }
    
    // 일반 게시글을 표시하는 헬퍼 메서드 (코드 재사용)
    private void displayNormalPost(Post p) {
            JPanel postContainer = new JPanel(new BorderLayout());
            postContainer.setBackground(new Color(240, 248, 255));
            JPanel postPanel = new JPanel(new BorderLayout(5, 5));
            postPanel.setBackground(Color.WHITE);
            postPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 5, 10) 
            ));
            
            postPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            postPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) { 
                        showPostDetail(p);
                    }
                }
            });

            String relativeTime = TimeFormatter.formatRelativeTime(p.getCreatedAt()); 
            JLabel writerLabel = new JLabel(
                "<html><b>" + p.getDisplayName() + "</b> <span style='color:gray;'>@" + p.getWriterId() + " · " + relativeTime + "</span></html>"
            );
        writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        writerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 사용자 이름 클릭 시 프로필 보기
        writerLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                User clickedUser = userDAO.getUserById(p.getWriterId());
                if (clickedUser != null) {
                    showUserProfile(clickedUser);
                }
            }
        });

            // 프로필 이미지 표시 로직
            JLabel profilePicLabel = new JLabel("👤", SwingConstants.CENTER);
            profilePicLabel.setPreferredSize(new Dimension(38, 38));
            profilePicLabel.setMinimumSize(new Dimension(38, 38));
            profilePicLabel.setMaximumSize(new Dimension(38, 38));
            profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
            profilePicLabel.setVerticalAlignment(SwingConstants.TOP);
            profilePicLabel.setAlignmentY(Component.TOP_ALIGNMENT);
            profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            if (p.getWriterProfileImagePath() != null && !p.getWriterProfileImagePath().isEmpty()) {
                ImageIcon circularIcon = createCircularProfileImage(p.getWriterProfileImagePath(), 38);
                if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                    profilePicLabel.setIcon(circularIcon);
                    profilePicLabel.setText("");
                } else {
                    profilePicLabel.setIcon(null);
                    profilePicLabel.setText("👤"); 
                }
            } else {
                profilePicLabel.setIcon(null);
                profilePicLabel.setText("👤"); 
            }
            
            // 프로필 이미지 클릭 시 프로필 보기
            profilePicLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    User clickedUser = userDAO.getUserById(p.getWriterId());
                    if (clickedUser != null) {
                        showUserProfile(clickedUser);
                    }
                }
            });

            JPanel nameAndFollowPanel = new JPanel(new BorderLayout());
            nameAndFollowPanel.setBackground(Color.WHITE);
            nameAndFollowPanel.setAlignmentY(Component.TOP_ALIGNMENT);
            nameAndFollowPanel.add(writerLabel, BorderLayout.CENTER); 

        // 팔로우 버튼 로직 (다른 사람의 트윗에만)
            if (!p.getWriterId().equals(loginUser.getUserId())) {
                JButton followButton = new JButton();
                boolean isFollowing = followDAO.isFollowing(loginUser.getUserId(), p.getWriterId());
                
            followButton.setText(isFollowing ? "팔로잉" : "팔로우");
            followButton.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            followButton.setFocusPainted(false);
            followButton.setOpaque(true);
            followButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            if (isFollowing) {
                followButton.setBackground(Color.WHITE);
                followButton.setForeground(Color.BLACK);
                followButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
                ));
                followButton.setContentAreaFilled(true);
                followButton.setOpaque(true);
            } else {
                followButton.setBackground(Color.WHITE);
                followButton.setForeground(Color.BLACK);
                followButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
                ));
                followButton.setContentAreaFilled(true);
                followButton.setOpaque(true);
            }
            
                followButton.addActionListener(e -> {
                    followDAO.toggleFollow(loginUser.getUserId(), p.getWriterId());
                    boolean nowFollowing = followDAO.isFollowing(loginUser.getUserId(), p.getWriterId());
                    followButton.setText(nowFollowing ? "팔로잉" : "팔로우");
                    if (nowFollowing) {
                        followButton.setBackground(Color.WHITE);
                        followButton.setForeground(Color.BLACK);
                        followButton.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(3, 8, 3, 8)
                        ));
                        followButton.setContentAreaFilled(true);
                        followButton.setOpaque(true);
                    } else {
                        followButton.setBackground(Color.WHITE);
                        followButton.setForeground(Color.BLACK);
                        followButton.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(3, 8, 3, 8)
                        ));
                        followButton.setContentAreaFilled(true);
                        followButton.setOpaque(true);
                    }
                    refreshTimeline();
                });
                nameAndFollowPanel.add(followButton, BorderLayout.EAST);
            }
            
            // 내용 영역
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(Color.WHITE);
        contentWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
            JTextArea contentArea = new JTextArea(p.getContent());
        contentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setEditable(false);
            contentArea.setBackground(Color.WHITE);
            contentArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        contentArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 트윗 내용 클릭 시 상세보기
        contentArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showPostDetail(p);
            }
        });
        
        contentWrapper.add(contentArea, BorderLayout.NORTH);
        
        // 오른쪽 패널: 이름/팔로우 버튼, 트윗 내용, 버튼 패널을 세로로 배치
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        rightPanel.add(nameAndFollowPanel);
        rightPanel.add(contentWrapper);
        
        // 메인 패널: 프로필 이미지(왼쪽)와 오른쪽 패널(이름+내용)을 가로로 배치
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 0));
        mainContentPanel.setBackground(Color.WHITE);
        mainContentPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // 프로필 이미지를 감싸는 패널 (상단 정렬을 위해)
        JPanel profileWrapper = new JPanel();
        profileWrapper.setLayout(new BoxLayout(profileWrapper, BoxLayout.Y_AXIS));
        profileWrapper.setBackground(Color.WHITE);
        profileWrapper.setPreferredSize(new Dimension(40, 40));
        profileWrapper.setMaximumSize(new Dimension(40, 40));
        profileWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
        profileWrapper.add(profilePicLabel);
        
        mainContentPanel.add(profileWrapper, BorderLayout.WEST);
        mainContentPanel.add(rightPanel, BorderLayout.CENTER);
        
        // 이미지/동영상 표시
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try {
                java.io.File imgFile = new java.io.File(p.getImagePath());
                if (imgFile.exists()) {
                    JLabel imageLabel = new JLabel();
                    ImageIcon icon = new ImageIcon(p.getImagePath());
                    Image img = icon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(img));
                    imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            showPostDetail(p);
                        }
                    });
                    imageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                    contentWrapper.add(imageLabel, BorderLayout.CENTER);
                }
            } catch (Exception e) {
                // 이미지 로드 실패 시 무시
            }
        }
        
        if (p.getVideoPath() != null && !p.getVideoPath().isEmpty()) {
            try {
                java.io.File videoFile = new java.io.File(p.getVideoPath());
                if (videoFile.exists()) {
                    JPanel videoPanel = new JPanel(new BorderLayout());
                    videoPanel.setBackground(Color.WHITE);
                    videoPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
                    JLabel videoLabel = new JLabel("<html><b>🎬 동영상:</b><br>" + videoFile.getName() + "</html>");
                    videoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                    videoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    videoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            try {
                                java.awt.Desktop.getDesktop().open(videoFile);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(PostFrame.this, "동영상을 열 수 없습니다: " + ex.getMessage());
                            }
                        }
                    });
                    videoPanel.add(videoLabel, BorderLayout.CENTER);
                    contentWrapper.add(videoPanel, BorderLayout.CENTER);
                }
            } catch (Exception e) {
                // 동영상 로드 실패 시 무시
            }
        }
            
            // 좋아요 및 답글 버튼 영역
            boolean alreadyLiked = postDAO.isLikedBy(p.getPostId(), loginUser.getUserId());
            
        JButton likeButton = new JButton("♥ " + p.getNumOfLikes());
        likeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            likeButton.setOpaque(false);
            likeButton.setContentAreaFilled(false);
            likeButton.setBorderPainted(false);
        likeButton.setForeground(alreadyLiked ? Color.RED : Color.GRAY);
        likeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            likeButton.addActionListener(e -> {
                boolean likedNow = postDAO.toggleLike(p.getPostId(), loginUser.getUserId());
                p.setNumOfLikes(likedNow ? p.getNumOfLikes() + 1 : p.getNumOfLikes() - 1);
            likeButton.setText("♥ " + p.getNumOfLikes());
            likeButton.setForeground(likedNow ? Color.RED : Color.GRAY);
            
            // 좋아요 시 알림 생성 (자기 자신의 트윗이 아닐 때)
            if (likedNow && !p.getWriterId().equals(loginUser.getUserId())) {
                createNotification(p.getWriterId(), p.getPostId(), "like");
            }
            // 타임라인 동기화
            refreshTimeline();
        });

        likeButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) { // 우클릭만
                         showLikersList(p);
                    }
                }
            });

        // 답글 버튼 (숫자 포함)
        JButton replyToPostBtn = new JButton("답글 " + p.getNumOfComments()); 
        replyToPostBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            replyToPostBtn.setOpaque(false);
            replyToPostBtn.setContentAreaFilled(false);
            replyToPostBtn.setBorderPainted(false);
        replyToPostBtn.setForeground(Color.GRAY);
        replyToPostBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            replyToPostBtn.addActionListener(ev -> {
                String replyText = JOptionPane.showInputDialog(PostFrame.this, p.getDisplayName() + "님에게 답글을 작성하세요:");
                
                if (replyText != null && !replyText.trim().isEmpty()) { 
                    Comment reply = new Comment(
                        "r" + System.currentTimeMillis(), 
                        replyText,
                        loginUser.getUserId(),
                        p.getPostId(),
                        0,
                        null,
                        loginUser.getDisplayName()
                    );
                    boolean success = commentDAO.addComment(reply);
                    if (success) {
                    // 답글 시 알림 생성 (자기 자신의 트윗이 아닐 때)
                    if (!p.getWriterId().equals(loginUser.getUserId())) {
                        createNotification(p.getWriterId(), p.getPostId(), "comment");
                    }
                        refreshTimeline();
                    } else {
                        JOptionPane.showMessageDialog(PostFrame.this, "답글 작성 실패!");
                    }
                }
            });

            boolean alreadyRetweeted = postDAO.isRetweetedBy(p.getPostId(), loginUser.getUserId());

        // 리트윗 버튼 (숫자 포함) - 트위터 스타일 아이콘 사용
        JButton retweetBtn = new JButton();
        retweetBtn.setLayout(new BorderLayout(5, 0));
        ImageIcon retweetIcon = createRetweetIcon(16, alreadyRetweeted ? TWITTER_GREEN : Color.GRAY);
        JLabel iconLabel = new JLabel(retweetIcon);
        JLabel countLabel = new JLabel(String.valueOf(p.getNumOfRetweets()));
        countLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        countLabel.setForeground(alreadyRetweeted ? TWITTER_GREEN : Color.GRAY);
        retweetBtn.add(iconLabel, BorderLayout.WEST);
        retweetBtn.add(countLabel, BorderLayout.CENTER);
        retweetBtn.setOpaque(false); 
        retweetBtn.setContentAreaFilled(false); 
        retweetBtn.setBorderPainted(false);
        retweetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            retweetBtn.addActionListener(e -> {
                showRetweetOption(p, alreadyRetweeted);
                // 리트윗 후 아이콘 업데이트
                SwingUtilities.invokeLater(() -> {
                    boolean nowRetweeted = postDAO.isRetweetedBy(p.getPostId(), loginUser.getUserId());
                    ImageIcon newIcon = createRetweetIcon(16, nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                    iconLabel.setIcon(newIcon);
                    countLabel.setForeground(nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                    countLabel.setText(String.valueOf(p.getNumOfRetweets()));
                });
            }); 

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        bottomPanel.setBackground(Color.WHITE);
            bottomPanel.add(replyToPostBtn); 
            bottomPanel.add(retweetBtn); 
        bottomPanel.add(likeButton);
        
        // 자신의 트윗인 경우 삭제 버튼 추가
        if (p.getWriterId().equals(loginUser.getUserId())) {
            JButton deleteButton = new JButton("삭제");
            deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            deleteButton.setForeground(new Color(220, 53, 69)); // 빨간색 텍스트
            deleteButton.setOpaque(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                    PostFrame.this,
                    "이 트윗을 삭제하시겠습니까?",
                    "트윗 삭제",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = postDAO.deletePost(p.getPostId());
                    if (success) {
                        // 상세 보기 창이 열려있고 삭제한 트윗을 보여주고 있다면 닫기
                        if (postDetailFrame != null && postDetailFrame.isVisible()) {
                            Post currentPost = postDAO.getPostById(p.getPostId());
                            if (currentPost == null) { // 삭제된 트윗이면
                                postDetailFrame.dispose();
                                postDetailFrame = null;
                            }
                        }
                        JOptionPane.showMessageDialog(PostFrame.this, "트윗이 삭제되었습니다.");
                        refreshTimeline();
                        // 검색 결과도 갱신 (검색 탭이 활성화되어 있다면)
                        String currentQuery = searchField != null ? searchField.getText().trim() : "";
                        if (!currentQuery.equals(DEFAULT_SEARCH_TEXT) && !currentQuery.isEmpty()) {
                            showSearchResultsInTab(currentQuery);
                        }
                    } else {
                        JOptionPane.showMessageDialog(PostFrame.this, "트윗 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            bottomPanel.add(deleteButton);
        }    

        // 버튼 패널도 오른쪽 패널에 추가하여 프로필 이미지 옆에서 시작하도록
        rightPanel.add(bottomPanel);
        
        postPanel.add(mainContentPanel, BorderLayout.CENTER);

        postContainer.add(postPanel, BorderLayout.NORTH);
        
        postContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, postContainer.getPreferredSize().height));
        
        timelinePanel.add(postContainer);
    }
    
    // ----------------------------------------------------
    // F(Function) - 상호작용 글 (리트윗/인용) 처리 헬퍼 메서드
    // ----------------------------------------------------
    private void displayInteractionPost(Post p) {
        if (p.getPostType().equals("retweet") && p.getContent().isEmpty()) {
            // 단순 리트윗인 경우, 원본 트윗을 직접 표시할 필요는 없으므로 간단히 처리
            JPanel panel = createSimpleRetweetHeader(p);
            timelinePanel.add(panel);
            
        } else if (p.getPostType().equals("quote")) {
            // 인용 트윗인 경우: 일반 트윗처럼 프로필 사진 옆에 내용이 시작되도록
            JPanel postContainer = new JPanel();
            postContainer.setLayout(new BoxLayout(postContainer, BoxLayout.Y_AXIS));
            postContainer.setBackground(Color.WHITE);
            
            JPanel postPanel = new JPanel(new BorderLayout(5, 5));
            postPanel.setBackground(Color.WHITE);
            postPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 5, 10)
            ));
            
            // 프로필 이미지 표시 로직
            JLabel profilePicLabel = new JLabel("👤", SwingConstants.CENTER);
            profilePicLabel.setPreferredSize(new Dimension(55, 55));
            profilePicLabel.setMinimumSize(new Dimension(55, 55));
            profilePicLabel.setMaximumSize(new Dimension(55, 55));
            profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
            profilePicLabel.setVerticalAlignment(SwingConstants.TOP);
            profilePicLabel.setAlignmentY(Component.TOP_ALIGNMENT);
            profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            if (p.getWriterProfileImagePath() != null && !p.getWriterProfileImagePath().isEmpty()) {
            	ImageIcon circularIcon = createCircularProfileImage(p.getWriterProfileImagePath(), 55);
                if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                    profilePicLabel.setIcon(circularIcon);
                    profilePicLabel.setText("");
                } else {
                    profilePicLabel.setIcon(null);
                    profilePicLabel.setText("👤"); 
                }
            } else {
                profilePicLabel.setIcon(null);
                profilePicLabel.setText("👤"); 
            }
            
            profilePicLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    User clickedUser = userDAO.getUserById(p.getWriterId());
                    if (clickedUser != null) {
                        showUserProfile(clickedUser);
                    }
                }
            });
            
            // 작성자 정보 레이블
            String relativeTime = TimeFormatter.formatRelativeTime(p.getCreatedAt());
            JLabel writerLabel = new JLabel("<html><b>" + p.getDisplayName() + "</b> <span style='color:gray;'>@" + p.getWriterId() + " · " + relativeTime + " • 인용함</span></html>");
            writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            writerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            writerLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    User clickedUser = userDAO.getUserById(p.getWriterId());
                    if (clickedUser != null) {
                        showUserProfile(clickedUser);
                    }
                }
            });
            
            JPanel nameAndFollowPanel = new JPanel(new BorderLayout());
            nameAndFollowPanel.setBackground(Color.WHITE);
            nameAndFollowPanel.setAlignmentY(Component.TOP_ALIGNMENT);
            nameAndFollowPanel.add(writerLabel, BorderLayout.CENTER);
            
            // 팔로우 버튼 로직 (다른 사람의 인용트에만)
            if (!p.getWriterId().equals(loginUser.getUserId())) {
                JButton followButton = new JButton();
                boolean isFollowing = followDAO.isFollowing(loginUser.getUserId(), p.getWriterId());
                
                followButton.setText(isFollowing ? "팔로잉" : "팔로우");
                followButton.setFont(new Font("맑은 고딕", Font.BOLD, 11));
                followButton.setFocusPainted(false);
                followButton.setOpaque(true);
                followButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                if (isFollowing) {
                    followButton.setBackground(Color.WHITE);
                    followButton.setForeground(Color.BLACK);
                    followButton.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        BorderFactory.createEmptyBorder(3, 8, 3, 8)
                    ));
                    followButton.setContentAreaFilled(true);
                    followButton.setOpaque(true);
                } else {
                    followButton.setBackground(Color.WHITE);
                    followButton.setForeground(Color.BLACK);
                    followButton.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        BorderFactory.createEmptyBorder(3, 8, 3, 8)
                    ));
                    followButton.setContentAreaFilled(true);
                    followButton.setOpaque(true);
                }
                
                followButton.addActionListener(e -> {
                    followDAO.toggleFollow(loginUser.getUserId(), p.getWriterId());
                    boolean nowFollowing = followDAO.isFollowing(loginUser.getUserId(), p.getWriterId());
                    followButton.setText(nowFollowing ? "팔로잉" : "팔로우");
                    if (nowFollowing) {
                        followButton.setBackground(Color.WHITE);
                        followButton.setForeground(Color.BLACK);
                        followButton.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(3, 8, 3, 8)
                        ));
                        followButton.setContentAreaFilled(true);
                        followButton.setOpaque(true);
                    } else {
                        followButton.setBackground(Color.WHITE);
                        followButton.setForeground(Color.BLACK);
                        followButton.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(3, 8, 3, 8)
                        ));
                        followButton.setContentAreaFilled(true);
                        followButton.setOpaque(true);
                    }
                    refreshTimeline();
                });
                nameAndFollowPanel.add(followButton, BorderLayout.EAST);
            }
            
            // 인용 트윗 본문
            JPanel contentWrapper = new JPanel(new BorderLayout());
            contentWrapper.setBackground(Color.WHITE);
            JTextArea quoteContentArea = new JTextArea(p.getContent());
            quoteContentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            quoteContentArea.setLineWrap(true);
            quoteContentArea.setWrapStyleWord(true);
            quoteContentArea.setEditable(false);
            quoteContentArea.setBackground(Color.WHITE);
            quoteContentArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            quoteContentArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            quoteContentArea.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showPostDetail(p);
                }
            });
            
            contentWrapper.add(quoteContentArea, BorderLayout.NORTH);
            
            // 오른쪽 패널: 이름/팔로우 버튼, 트윗 내용을 세로로 배치
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.setBackground(Color.WHITE);
            rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
            
            rightPanel.add(nameAndFollowPanel);
            rightPanel.add(contentWrapper);
            
            // 메인 패널: 프로필 이미지(왼쪽)와 오른쪽 패널(이름+내용)을 가로로 배치
            JPanel mainContentPanel = new JPanel(new BorderLayout(10, 0));
            mainContentPanel.setBackground(Color.WHITE);
            mainContentPanel.setAlignmentY(Component.TOP_ALIGNMENT);
            
            // 프로필 이미지를 감싸는 패널 (상단 정렬을 위해)
            JPanel profileWrapper = new JPanel();
            profileWrapper.setLayout(new BoxLayout(profileWrapper, BoxLayout.Y_AXIS));
            profileWrapper.setBackground(Color.WHITE);
            profileWrapper.setPreferredSize(new Dimension(40, 40));
            profileWrapper.setMaximumSize(new Dimension(40, 40));
            profileWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
            profileWrapper.add(profilePicLabel);
            
            mainContentPanel.add(profileWrapper, BorderLayout.WEST);
            mainContentPanel.add(rightPanel, BorderLayout.CENTER);
            
            postPanel.add(mainContentPanel, BorderLayout.CENTER);
            
            // 원본 트윗 로드 및 표시
            if (p.getParentPostId() != null) {
                Post originalPost = postDAO.getPostById(p.getParentPostId());
                if (originalPost != null) {
                    JPanel originalPostPanel = createOriginalPostBox(originalPost);
                    rightPanel.add(originalPostPanel);
                }
            }
            
            // 좋아요, 답글, 리트윗 버튼 추가 (인용트 자체에 대한 상호작용)
            boolean alreadyLiked = postDAO.isLikedBy(p.getPostId(), loginUser.getUserId());
            
            JButton likeButton = new JButton("♥ " + p.getNumOfLikes());
            likeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            likeButton.setOpaque(false);
            likeButton.setContentAreaFilled(false);
            likeButton.setBorderPainted(false);
            likeButton.setForeground(alreadyLiked ? Color.RED : Color.GRAY);
            likeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            likeButton.addActionListener(e -> {
                boolean likedNow = postDAO.toggleLike(p.getPostId(), loginUser.getUserId());
                p.setNumOfLikes(likedNow ? p.getNumOfLikes() + 1 : p.getNumOfLikes() - 1);
                likeButton.setText("♥ " + p.getNumOfLikes());
                likeButton.setForeground(likedNow ? Color.RED : Color.GRAY);
                
                // 좋아요 시 알림 생성 (자기 자신의 트윗이 아닐 때)
                if (likedNow && !p.getWriterId().equals(loginUser.getUserId())) {
                    createNotification(p.getWriterId(), p.getPostId(), "like");
                }
                // 타임라인 동기화
                refreshTimeline();
            });
            
            likeButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) { // 우클릭만
                        showLikersList(p);
                    }
                }
            });
            
            // 답글 버튼
            JButton replyToPostBtn = new JButton("답글 " + p.getNumOfComments());
            replyToPostBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            replyToPostBtn.setOpaque(false);
            replyToPostBtn.setContentAreaFilled(false);
            replyToPostBtn.setBorderPainted(false);
            replyToPostBtn.setForeground(Color.GRAY);
            replyToPostBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            replyToPostBtn.addActionListener(ev -> {
                String replyText = JOptionPane.showInputDialog(PostFrame.this, p.getDisplayName() + "님에게 답글을 작성하세요:");
                
                if (replyText != null && !replyText.trim().isEmpty()) {
                    Comment reply = new Comment(
                        "r" + System.currentTimeMillis(),
                        replyText,
                        loginUser.getUserId(),
                        p.getPostId(),
                        0,
                        null,
                        loginUser.getDisplayName()
                    );
                    boolean success = commentDAO.addComment(reply);
                    if (success) {
                        // 답글 시 알림 생성 (자기 자신의 트윗이 아닐 때)
                        if (!p.getWriterId().equals(loginUser.getUserId())) {
                            createNotification(p.getWriterId(), p.getPostId(), "comment");
                        }
                        refreshTimeline();
                    } else {
                        JOptionPane.showMessageDialog(PostFrame.this, "답글 작성 실패!");
                    }
                }
            });
            
            // 리트윗 버튼
            boolean alreadyRetweeted = postDAO.isRetweetedBy(p.getPostId(), loginUser.getUserId());
            
            // 리트윗 버튼 (인용 트윗) - 트위터 스타일 아이콘 사용
            JButton retweetBtn = new JButton();
            retweetBtn.setLayout(new BorderLayout(5, 0));
            ImageIcon retweetIcon = createRetweetIcon(16, alreadyRetweeted ? TWITTER_GREEN : Color.GRAY);
            JLabel iconLabel2 = new JLabel(retweetIcon);
            JLabel countLabel2 = new JLabel(String.valueOf(p.getNumOfRetweets()));
            countLabel2.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            countLabel2.setForeground(alreadyRetweeted ? TWITTER_GREEN : Color.GRAY);
            retweetBtn.add(iconLabel2, BorderLayout.WEST);
            retweetBtn.add(countLabel2, BorderLayout.CENTER);
            retweetBtn.setOpaque(false);
            retweetBtn.setContentAreaFilled(false);
            retweetBtn.setBorderPainted(false);
            retweetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            retweetBtn.addActionListener(e -> {
                showRetweetOption(p, alreadyRetweeted);
                // 리트윗 후 아이콘 업데이트
                SwingUtilities.invokeLater(() -> {
                    boolean nowRetweeted = postDAO.isRetweetedBy(p.getPostId(), loginUser.getUserId());
                    ImageIcon newIcon = createRetweetIcon(16, nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                    iconLabel2.setIcon(newIcon);
                    countLabel2.setForeground(nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                    countLabel2.setText(String.valueOf(p.getNumOfRetweets()));
                });
            });
            
            // 버튼 패널
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
            bottomPanel.setBackground(Color.WHITE);
            bottomPanel.add(replyToPostBtn);
            bottomPanel.add(retweetBtn);
            bottomPanel.add(likeButton);
            
            // 자신의 인용트인 경우 삭제 버튼 추가
            if (p.getWriterId().equals(loginUser.getUserId())) {
                JButton deleteButton = new JButton("삭제");
                deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
                deleteButton.setForeground(new Color(220, 53, 69)); // 빨간색 텍스트
                deleteButton.setOpaque(false);
                deleteButton.setContentAreaFilled(false);
                deleteButton.setBorderPainted(false);
                deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                deleteButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(
                        PostFrame.this,
                        "이 인용트를 삭제하시겠습니까?",
                        "인용트 삭제",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        boolean success = postDAO.deletePost(p.getPostId());
                        if (success) {
                            // 상세 보기 창이 열려있고 삭제한 트윗을 보여주고 있다면 닫기
                            if (postDetailFrame != null && postDetailFrame.isVisible()) {
                                Post currentPost = postDAO.getPostById(p.getPostId());
                                if (currentPost == null) { // 삭제된 트윗이면
                                    postDetailFrame.dispose();
                                    postDetailFrame = null;
                                }
                            }
                            JOptionPane.showMessageDialog(PostFrame.this, "인용트가 삭제되었습니다.");
                            refreshTimeline();
                            // 검색 결과도 갱신 (검색 탭이 활성화되어 있다면)
                            String currentQuery = searchField != null ? searchField.getText().trim() : "";
                            if (!currentQuery.equals(DEFAULT_SEARCH_TEXT) && !currentQuery.isEmpty()) {
                                showSearchResultsInTab(currentQuery);
                            }
                        } else {
                            JOptionPane.showMessageDialog(PostFrame.this, "인용트 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                bottomPanel.add(deleteButton);
            }
            
            // 버튼 패널도 오른쪽 패널에 추가하여 프로필 이미지 옆에서 시작하도록
            rightPanel.add(bottomPanel);
            
            postPanel.add(mainContentPanel, BorderLayout.CENTER);
            
            postContainer.add(postPanel, BorderLayout.NORTH);
            
            timelinePanel.add(postContainer);
        }
    }

    // ----------------------------------------------------
    // 인용 트윗 내부에 표시될 원본 트윗 박스를 생성하는 헬퍼 메서드
    // ----------------------------------------------------
    private JPanel createOriginalPostBox(Post originalPost) {
        // 레이아웃 간격 축소 (5,5 -> 0,2)
        JPanel originalPanel = new JPanel(new BorderLayout(0, 2));
        
        // 테두리 및 내부 여백 대폭 축소 (12px -> 8px)
        originalPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(207, 217, 222), 1, true), // 둥근 테두리
            BorderFactory.createEmptyBorder(8, 10, 8, 10) // 상하좌우 여백 줄임
        ));
        originalPanel.setBackground(Color.WHITE);

        // 원본 작성자 정보 (폰트 크기 13 -> 12로 축소)
        JLabel originalWriterLabel = new JLabel(
            "<html><b>" + originalPost.getDisplayName() + "</b> <span style='color:gray;'>@" + originalPost.getWriterId() + "</span></html>"
        );
        originalWriterLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        originalWriterLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0)); // 헤더와 내용 사이 간격 줄임
        originalPanel.add(originalWriterLabel, BorderLayout.NORTH);

        // 원본 내용
        JTextArea originalContent = new JTextArea(originalPost.getContent());
        originalContent.setEditable(false);
        originalContent.setLineWrap(true);
        originalContent.setWrapStyleWord(true);
        originalContent.setBackground(originalPanel.getBackground());
        // 내용 폰트 크기 13 -> 12로 축소
        originalContent.setFont(new Font("맑은 고딕", Font.PLAIN, 12)); 
        originalContent.setForeground(Color.DARK_GRAY); // 글자색 약간 연하게
        originalContent.setBorder(null);
        
        // 불필요한 마진 제거
        originalContent.setMargin(new Insets(0,0,0,0));
        
        originalPanel.add(originalContent, BorderLayout.CENTER);
        
        return originalPanel;
    }

    // ----------------------------------------------------
    // 단순 리트윗 헤더를 생성하는 헬퍼 메서드 (기존 displayInteractionPost의 단순 리트윗 대체)
    // ----------------------------------------------------
    private JPanel createSimpleRetweetHeader(Post p) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 5, 10)
        ));
        
        JLabel headerLabel = new JLabel("<html><span style='color:gray;'><b>" + p.getDisplayName() + "</b> 님이 리트윗했습니다</span></html>");
        headerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        headerLabel.setForeground(Color.GRAY);
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // 원본 트윗 표시
        if (p.getParentPostId() != null) {
            Post originalPost = postDAO.getPostById(p.getParentPostId());
            if (originalPost != null) {
                JPanel originalPostPanel = createOriginalPostBox(originalPost);
                panel.add(originalPostPanel, BorderLayout.CENTER);
            }
        }
        
        return panel;
    }
    private void handleRetweet(Post p, String type, String quoteContent) {
        int confirm = JOptionPane.showConfirmDialog(
            PostFrame.this, 
            "\"" + p.getDisplayName() + "\"님의 트윗을 리트윗하시겠습니까?", 
            "리트윗 확인", 
            JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            if (postDAO.createInteractionPost(loginUser.getUserId(), p.getPostId(), type, quoteContent)) {
                // 리트윗 시 알림 생성 (자기 자신의 트윗이 아닐 때)
                if (!p.getWriterId().equals(loginUser.getUserId())) {
                    createNotification(p.getWriterId(), p.getPostId(), "retweet");
                }
                refreshTimeline();
            } else {
                JOptionPane.showMessageDialog(PostFrame.this, "리트윗 실패!");
            }
        }
    }
    
    private void handleQuote(Post p) {
        String quoteText = JOptionPane.showInputDialog(
            PostFrame.this, 
            "인용할 내용을 작성하세요:", 
            "인용 트윗", 
            JOptionPane.PLAIN_MESSAGE
        );
        if (quoteText != null && !quoteText.trim().isEmpty()) {
            if (postDAO.createInteractionPost(loginUser.getUserId(), p.getPostId(), "quote", quoteText)) {
                // 인용 시 알림 생성 (자기 자신의 트윗이 아닐 때)
                if (!p.getWriterId().equals(loginUser.getUserId())) {
                    createNotification(p.getWriterId(), p.getPostId(), "quote");
                }
                refreshTimeline();
            } else {
                JOptionPane.showMessageDialog(PostFrame.this, "인용 트윗 실패!");
            }
        }
    }
    
    // 알림 생성 헬퍼 메서드
    private void createNotification(String userId, String postId, String type) {
        Notification notification = new Notification(
            "n" + System.currentTimeMillis(),
            userId,
            loginUser.getUserId(),
            postId,
            type
        );
        notificationDAO.createNotification(notification);
        
        // 알림 버튼 업데이트 (읽지 않은 알림 수 표시)
        SwingUtilities.invokeLater(() -> {
            if (notificationNavBtn != null) {
                updateNotificationButton(notificationNavBtn);
            }
        });
    }
    
    // ----------------------------------------------------
    // F(Function) - 상세 보기 및 좋아요 목록 
    // ----------------------------------------------------
    
    public void showPostDetail(Post post) {
        if (postDetailFrame != null) {
            postDetailFrame.dispose();
        }
        
        postDetailFrame = new JFrame("트윗 상세 - " + post.getDisplayName());
        postDetailFrame.setSize(500, 500);
        postDetailFrame.setLocationRelativeTo(this);
        postDetailFrame.setLayout(new BorderLayout());

        JPanel postPanel = new JPanel(new BorderLayout(5, 5));
        postPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        postPanel.add(new JLabel("<html><b>" + post.getDisplayName() + "</b> <span style='color:gray;'>@" + post.getWriterId() + "</span></html>"), BorderLayout.NORTH);
        JTextArea contentArea = new JTextArea(post.getContent());
        contentArea.setEditable(false);
        postPanel.add(contentArea, BorderLayout.CENTER);
        
        JPanel commentListPanel = new JPanel();
        commentListPanel.setLayout(new BoxLayout(commentListPanel, BoxLayout.Y_AXIS));

        List<Comment> comments = commentDAO.getCommentsByPost(post.getPostId());
        
        if (comments.isEmpty()) {
            commentListPanel.add(new JLabel("댓글이 없습니다."));
        } else {
            displayCommentsForDetail(commentListPanel, comments, null, 10, post.getPostId());
        }

        JScrollPane commentScrollPane = new JScrollPane(commentListPanel);
        commentScrollPane.setBorder(null);

        postDetailFrame.add(postPanel, BorderLayout.NORTH);
        postDetailFrame.add(commentScrollPane, BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("닫기");
        closeBtn.addActionListener(e -> postDetailFrame.dispose());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(closeBtn);
        postDetailFrame.add(southPanel, BorderLayout.SOUTH);
        
        postDetailFrame.setVisible(true);
    }
    
    private void displayCommentsForDetail(JPanel parentPanel, List<Comment> comments, String parentId, int indent, String postId) {
        for (Comment c : comments) {
             if ((c.getParentCommentId() == null && parentId == null) || 
                 (c.getParentCommentId() != null && c.getParentCommentId().equals(parentId))) {
                
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(5, indent, 5, 10)); 
                
                JLabel writerLabel = new JLabel(
                    "<html><b>" + c.getDisplayName() + "</b> <span style='color:gray;'>@" + c.getWriterId() + "</span></html>"
                );
                writerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                
                JTextArea contentArea = new JTextArea(c.getContent());
                contentArea.setEditable(false);
                contentArea.setBackground(panel.getBackground()); 
                contentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                contentArea.setLineWrap(true);
                contentArea.setWrapStyleWord(true);
                
                JButton replyBtn = new JButton("답글");
                replyBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
                replyBtn.setForeground(Color.GRAY);
                replyBtn.setOpaque(false);
                replyBtn.setContentAreaFilled(false);
                replyBtn.setBorderPainted(false);
                replyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                replyBtn.addActionListener(ev -> {
                    String replyText = JOptionPane.showInputDialog(postDetailFrame, c.getDisplayName() + "님에게 답글 내용을 입력하세요:");
                    if (replyText != null && !replyText.trim().isEmpty()) { 
                        Comment reply = new Comment(
                                "c" + System.currentTimeMillis(),
                                replyText,
                                loginUser.getUserId(),
                                postId, 
                                0,
                                c.getCommentId(), 
                                loginUser.getDisplayName()
                        );
                        boolean success = commentDAO.addComment(reply);
                        
                        if (success) {
                            postDetailFrame.dispose();
                            Post updatedPost = postDAO.getPostById(postId); 
                            
                            if (updatedPost != null) {
                                showPostDetail(updatedPost); 
                            }
                            refreshTimeline(); 
                        } else {
                             JOptionPane.showMessageDialog(postDetailFrame, "답글 작성 실패!");
                        }
                    }
                });
                
                // 좋아요 버튼
                boolean commentLiked = commentDAO.isLikedBy(c.getCommentId(), loginUser.getUserId());
                JButton likeBtn = new JButton("♥");
                likeBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                likeBtn.setForeground(commentLiked ? Color.RED : Color.GRAY);
                likeBtn.setOpaque(false);
                likeBtn.setContentAreaFilled(false);
                likeBtn.setBorderPainted(false);
                likeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                JLabel likeLabel = new JLabel(String.valueOf(c.getNumOfLikes()));
                likeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                likeLabel.setForeground(Color.GRAY);
                
                likeBtn.addActionListener(ev -> {
                    commentDAO.toggleLike(c.getCommentId(), loginUser.getUserId());
                    postDetailFrame.dispose();
                    Post updatedPost = postDAO.getPostById(postId);
                    if (updatedPost != null) {
                        showPostDetail(updatedPost);
                    }
                    refreshTimeline();
                });
                
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
                bottomPanel.add(replyBtn);
                bottomPanel.add(likeBtn);
                bottomPanel.add(likeLabel);

                panel.add(writerLabel, BorderLayout.NORTH);
                panel.add(contentArea, BorderLayout.CENTER);
                panel.add(bottomPanel, BorderLayout.SOUTH);
                
                parentPanel.add(panel);

                displayCommentsForDetail(parentPanel, comments, c.getCommentId(), indent + 20, postId);
            }
        }
    }
    
    private void showLikersList(Post post) {
        List<String> likers = postDAO.getLikersDisplayName(post.getPostId()); 
        
        if (likers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이 게시글에 좋아요를 누른 사람이 없습니다.");
            return;
        }

        String likersText = String.join("\n", likers);
        JTextArea textArea = new JTextArea("좋아요를 누른 사용자:\n" + likersText);
        textArea.setEditable(false);
        
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "좋아요 목록 (" + post.getNumOfLikes() + "명)", JOptionPane.PLAIN_MESSAGE);
    }
    
    // 팔로워/팔로잉 목록 표시
    private void showFollowList(User user, String listType) {
        JDialog listDialog = new JDialog(this, user.getDisplayName() + "님의 " + listType, false);
        listDialog.setSize(500, 600);
        listDialog.setLocationRelativeTo(this);
        listDialog.setLayout(new BorderLayout());
        
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        List<User> users;
        if (listType.equals("팔로워")) {
            users = followDAO.getFollowers(user.getUserId());
        } else {
            users = followDAO.getFollowing(user.getUserId());
        }
        
        if (users.isEmpty()) {
            JLabel emptyLabel = new JLabel(listType + "가 없습니다.");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            userListPanel.add(emptyLabel);
        } else {
            for (User followUser : users) {
                JPanel userPanel = new JPanel(new BorderLayout(10, 5));
                userPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                userPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                // 프로필 아이콘
                JLabel iconLabel = new JLabel("👤");
                iconLabel.setFont(new Font("Dialog", Font.PLAIN, 40));
                
                // 사용자 정보
                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                
                JLabel nameLabel = new JLabel("<html><b>" + followUser.getDisplayName() + "</b></html>");
                nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
                
                JLabel idLabel = new JLabel("@" + followUser.getUserId());
                idLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                idLabel.setForeground(Color.GRAY);
                
                String bio = followUser.getBio();
                if (bio != null && !bio.isEmpty()) {
                    JLabel bioLabel = new JLabel("<html>" + bio + "</html>");
                    bioLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                    bioLabel.setForeground(Color.DARK_GRAY);
                    infoPanel.add(nameLabel);
                    infoPanel.add(Box.createVerticalStrut(3));
                    infoPanel.add(idLabel);
                    infoPanel.add(Box.createVerticalStrut(5));
                    infoPanel.add(bioLabel);
                } else {
                    infoPanel.add(nameLabel);
                    infoPanel.add(Box.createVerticalStrut(3));
                    infoPanel.add(idLabel);
                }
                
                // 팔로우 버튼 (자기 자신이 아닐 경우)
                JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                if (!followUser.getUserId().equals(loginUser.getUserId())) {
                    boolean isFollowing = followDAO.isFollowing(loginUser.getUserId(), followUser.getUserId());
                    JButton followBtn = new JButton(isFollowing ? "팔로잉" : "팔로우");
                    followBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                    followBtn.setFocusPainted(false);
                    
                    if (isFollowing) {
                        followBtn.setBackground(Color.WHITE);
                        followBtn.setForeground(Color.BLACK);
                        followBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(5, 15, 5, 15)
                        ));
                        followBtn.setContentAreaFilled(true);
                    } else {
                        followBtn.setBackground(Color.WHITE);
                        followBtn.setForeground(Color.BLACK);
                        followBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                            BorderFactory.createEmptyBorder(5, 15, 5, 15)
                        ));
                        followBtn.setContentAreaFilled(true);
                    }
                    followBtn.setOpaque(true);
                    followBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    
                    followBtn.addActionListener(e -> {
                        followDAO.toggleFollow(loginUser.getUserId(), followUser.getUserId());
                        boolean nowFollowing = followDAO.isFollowing(loginUser.getUserId(), followUser.getUserId());
                        followBtn.setText(nowFollowing ? "팔로잉" : "팔로우");
                        
                        if (nowFollowing) {
                            followBtn.setBackground(Color.WHITE);
                            followBtn.setForeground(Color.BLACK);
                            followBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                                BorderFactory.createEmptyBorder(5, 15, 5, 15)
                            ));
                            followBtn.setContentAreaFilled(true);
                        } else {
                            followBtn.setBackground(Color.WHITE);
                            followBtn.setForeground(Color.BLACK);
                            followBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                                BorderFactory.createEmptyBorder(5, 15, 5, 15)
                            ));
                            followBtn.setContentAreaFilled(true);
                        }
                    });
                    
                    rightPanel.add(followBtn);
                }
                
                userPanel.add(iconLabel, BorderLayout.WEST);
                userPanel.add(infoPanel, BorderLayout.CENTER);
                userPanel.add(rightPanel, BorderLayout.EAST);
                
                // 사용자 패널 클릭 시 프로필 보기
                userPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        // 버튼 클릭이 아닐 때만 프로필 보기
                        Component clickedComponent = e.getComponent();
                        if (!(clickedComponent instanceof JButton)) {
                            listDialog.dispose();
                            showUserProfile(followUser);
                        }
                    }
                });
                
                userListPanel.add(userPanel);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setBorder(null);
        listDialog.add(scrollPane, BorderLayout.CENTER);
        listDialog.setVisible(true);
    }
    
    // 사용자 프로필 상세 페이지
    public void showUserProfile(User user) {
        JFrame profileFrame = new JFrame(user.getDisplayName() + "님의 프로필");
        profileFrame.setSize(600, 700);
        profileFrame.setLocationRelativeTo(this);
        profileFrame.setLayout(new BorderLayout());
        
        // 상단 프로필 정보 패널
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        headerPanel.setBackground(Color.WHITE);
        
        // 프로필 이미지
        JLabel profileIcon = new JLabel("👤", SwingConstants.CENTER);
        profileIcon.setFont(new Font("Dialog", Font.PLAIN, 80));
        profileIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 실제 프로필 이미지가 있으면 표시 (원형)
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(user.getProfileImagePath(), 100);
            if (circularIcon != null) {
                profileIcon.setIcon(circularIcon);
                profileIcon.setText(""); // 아이콘 있으면 텍스트 제거
            }
        }
        
        // 사용자 이름
        JLabel nameLabel = new JLabel(user.getDisplayName());
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 사용자 ID
        JLabel idLabel = new JLabel("@" + user.getUserId());
        idLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        idLabel.setForeground(Color.GRAY);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 바이오
        JTextArea bioArea = new JTextArea(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "소개가 없습니다.");
        bioArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        bioArea.setEditable(false);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setBackground(headerPanel.getBackground());
        bioArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        bioArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 가입 날짜
        String joinDateText = "가입일: 정보 없음";
        if (user.getCreatedAt() != null) {
            joinDateText = "가입일: " + user.getCreatedAt().toString().substring(0, 10);
        }
        JLabel joinDateLabel = new JLabel(joinDateText);
        joinDateLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        joinDateLabel.setForeground(Color.GRAY);
        joinDateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 통계 정보
        int followerCount = userDAO.getFollowerCount(user.getUserId());
        int followingCount = userDAO.getFollowingCount(user.getUserId());
        int postCount = userDAO.getPostCount(user.getUserId());
        
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        statsPanel.setBackground(Color.WHITE);
        
        JLabel postCountLabel = new JLabel("<html><b>" + postCount + "</b> 트윗</html>");
        postCountLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        JLabel followingCountLabel = new JLabel("<html><b>" + followingCount + "</b> 팔로잉</html>");
        followingCountLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        followingCountLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        followingCountLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showFollowList(user, "팔로잉");
            }
        });
        
        JLabel followerCountLabel = new JLabel("<html><b>" + followerCount + "</b> 팔로워</html>");
        followerCountLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        followerCountLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        followerCountLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showFollowList(user, "팔로워");
            }
        });
        
        statsPanel.add(postCountLabel);
        statsPanel.add(followingCountLabel);
        statsPanel.add(followerCountLabel);
        
        // 팔로우 버튼 및 뮤트 버튼 (자기 자신이 아닐 경우)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        if (!user.getUserId().equals(loginUser.getUserId())) {
            boolean isFollowing = followDAO.isFollowing(loginUser.getUserId(), user.getUserId());
            boolean isMuted = muteDAO.isUserMuted(loginUser.getUserId(), user.getUserId());
            
            JButton followBtn = new JButton(isFollowing ? "팔로잉" : "팔로우");
            followBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            followBtn.setFocusPainted(false);
            followBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            if (isFollowing) {
                followBtn.setBackground(Color.WHITE);
                followBtn.setForeground(Color.BLACK);
                followBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
                    BorderFactory.createEmptyBorder(8, 20, 8, 20)
                ));
                followBtn.setContentAreaFilled(true);
            } else {
                followBtn.setBackground(Color.WHITE);
                followBtn.setForeground(Color.BLACK);
                followBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
                    BorderFactory.createEmptyBorder(8, 20, 8, 20)
                ));
                followBtn.setContentAreaFilled(true);
            }
            followBtn.setOpaque(true);
            
            followBtn.addActionListener(e -> {
                followDAO.toggleFollow(loginUser.getUserId(), user.getUserId());
                boolean nowFollowing = followDAO.isFollowing(loginUser.getUserId(), user.getUserId());
                followBtn.setText(nowFollowing ? "팔로잉" : "팔로우");
                
                if (nowFollowing) {
                    followBtn.setBackground(Color.WHITE);
                    followBtn.setForeground(Color.BLACK);
                    followBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
                        BorderFactory.createEmptyBorder(8, 20, 8, 20)
                    ));
                    followBtn.setContentAreaFilled(true);
                } else {
                    followBtn.setBackground(Color.WHITE);
                    followBtn.setForeground(Color.BLACK);
                    followBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
                        BorderFactory.createEmptyBorder(8, 20, 8, 20)
                    ));
                    followBtn.setContentAreaFilled(true);
                }
                
                // 통계 업데이트
                int newFollowerCount = userDAO.getFollowerCount(user.getUserId());
                followerCountLabel.setText("<html><b>" + newFollowerCount + "</b> 팔로워</html>");
            });
            
            // 뮤트 버튼
            JButton muteBtn = new JButton(isMuted ? "뮤트 해제" : "뮤트");
            muteBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            muteBtn.setFocusPainted(false);
            muteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            if (isMuted) {
                muteBtn.setBackground(new Color(220, 53, 69));
                muteBtn.setForeground(Color.WHITE);
                muteBtn.setBorderPainted(false);
                muteBtn.setContentAreaFilled(true);
            } else {
                muteBtn.setBackground(Color.WHITE);
                muteBtn.setForeground(new Color(220, 53, 69));
                muteBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 53, 69), 2),
                    BorderFactory.createEmptyBorder(8, 20, 8, 20)
                ));
                muteBtn.setContentAreaFilled(true);
            }
            muteBtn.setOpaque(true);
            
            muteBtn.addActionListener(e -> {
                boolean currentlyMuted = muteDAO.isUserMuted(loginUser.getUserId(), user.getUserId());
                if (currentlyMuted) {
                    // 뮤트 해제
                    if (muteDAO.removeMutedUser(loginUser.getUserId(), user.getUserId())) {
                        muteBtn.setText("뮤트");
                        muteBtn.setBackground(Color.WHITE);
                        muteBtn.setForeground(new Color(220, 53, 69));
                        muteBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(220, 53, 69), 2),
                            BorderFactory.createEmptyBorder(8, 20, 8, 20)
                        ));
                        muteBtn.setBorderPainted(true);
                        refreshTimeline();
                        JOptionPane.showMessageDialog(profileFrame, user.getDisplayName() + "님의 뮤트가 해제되었습니다.");
                    }
                } else {
                    // 뮤트
                    if (muteDAO.addMutedUser(loginUser.getUserId(), user.getUserId())) {
                        muteBtn.setText("뮤트 해제");
                        muteBtn.setBackground(new Color(220, 53, 69));
                        muteBtn.setForeground(Color.WHITE);
                        muteBtn.setBorderPainted(false);
                        refreshTimeline();
                        JOptionPane.showMessageDialog(profileFrame, user.getDisplayName() + "님이 뮤트되었습니다.");
                    } else {
                        JOptionPane.showMessageDialog(profileFrame, "뮤트에 실패했습니다.");
                    }
                }
            });
            
            buttonPanel.add(followBtn);
            buttonPanel.add(muteBtn);
        }
        
        headerPanel.add(profileIcon);
        headerPanel.add(Box.createVerticalStrut(15));
        headerPanel.add(nameLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(idLabel);
        headerPanel.add(Box.createVerticalStrut(15));
        headerPanel.add(bioArea);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(joinDateLabel);
        headerPanel.add(Box.createVerticalStrut(15));
        headerPanel.add(statsPanel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(buttonPanel);
        
        // 사용자 트윗 목록
        JPanel tweetsPanel = new JPanel();
        tweetsPanel.setLayout(new BoxLayout(tweetsPanel, BoxLayout.Y_AXIS));
        tweetsPanel.setBorder(BorderFactory.createTitledBorder("트윗"));
        
        List<Post> userPosts = postDAO.getPostsByUserId(user.getUserId());
        
        if (userPosts.isEmpty()) {
            JLabel noTweets = new JLabel("작성한 트윗이 없습니다.");
            noTweets.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            noTweets.setForeground(Color.GRAY);
            tweetsPanel.add(noTweets);
        } else {
            for (Post post : userPosts) {
                JPanel postPanel = new JPanel(new BorderLayout(5, 5));
                postPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_GRAY_BORDER),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                
                JTextArea contentArea = new JTextArea(post.getContent());
                contentArea.setEditable(false);
                contentArea.setLineWrap(true);
                contentArea.setWrapStyleWord(true);
                contentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                contentArea.setBackground(postPanel.getBackground());
                contentArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                // 트윗 클릭 시 상세보기
                contentArea.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        profileFrame.dispose();
                        showPostDetail(post);
                    }
                });
                
                // 좋아요/답글/리트윗 버튼
                JPanel interactionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
                
                // 좋아요 버튼
                boolean alreadyLiked = postDAO.isLikedBy(post.getPostId(), loginUser.getUserId());
                JButton likeBtn = new JButton("♥ " + post.getNumOfLikes());
                likeBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                likeBtn.setOpaque(false);
                likeBtn.setContentAreaFilled(false);
                likeBtn.setBorderPainted(false);
                likeBtn.setForeground(alreadyLiked ? Color.RED : Color.GRAY);
                likeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                likeBtn.addActionListener(e -> {
                    boolean likedNow = postDAO.toggleLike(post.getPostId(), loginUser.getUserId());
                    post.setNumOfLikes(likedNow ? post.getNumOfLikes() + 1 : post.getNumOfLikes() - 1);
                    likeBtn.setText("♥ " + post.getNumOfLikes());
                    likeBtn.setForeground(likedNow ? Color.RED : Color.GRAY);
                    // 타임라인 동기화
                    refreshTimeline();
                });
                
                // 답글 버튼
                JButton replyBtn = new JButton("답글 " + post.getNumOfComments());
                replyBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                replyBtn.setOpaque(false);
                replyBtn.setContentAreaFilled(false);
                replyBtn.setBorderPainted(false);
                replyBtn.setForeground(Color.GRAY);
                replyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                replyBtn.addActionListener(e -> {
                    String replyText = JOptionPane.showInputDialog(profileFrame, post.getDisplayName() + "님에게 답글을 작성하세요:");
                    if (replyText != null && !replyText.trim().isEmpty()) {
                        Comment reply = new Comment(
                            "r" + System.currentTimeMillis(),
                            replyText,
                            loginUser.getUserId(),
                            post.getPostId(),
                            0,
                            null,
                            loginUser.getDisplayName()
                        );
                        boolean success = commentDAO.addComment(reply);
                        if (success) {
                            JOptionPane.showMessageDialog(profileFrame, "답글이 작성되었습니다!");
                            post.setNumOfComments(post.getNumOfComments() + 1);
                            replyBtn.setText("답글 " + post.getNumOfComments());
                        } else {
                            JOptionPane.showMessageDialog(profileFrame, "답글 작성 실패!");
                        }
                    }
                });
                
                // 리트윗 버튼 (프로필) - 트위터 스타일 아이콘 사용
                boolean alreadyRetweeted = postDAO.isRetweetedBy(post.getPostId(), loginUser.getUserId());
                JButton retweetBtn = new JButton();
                retweetBtn.setLayout(new BorderLayout(5, 0));
                ImageIcon retweetIcon = createRetweetIcon(16, alreadyRetweeted ? TWITTER_GREEN : Color.GRAY);
                JLabel iconLabel3 = new JLabel(retweetIcon);
                JLabel countLabel3 = new JLabel(String.valueOf(post.getNumOfRetweets()));
                countLabel3.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                countLabel3.setForeground(alreadyRetweeted ? TWITTER_GREEN : Color.GRAY);
                retweetBtn.add(iconLabel3, BorderLayout.WEST);
                retweetBtn.add(countLabel3, BorderLayout.CENTER);
                retweetBtn.setOpaque(false);
                retweetBtn.setContentAreaFilled(false);
                retweetBtn.setBorderPainted(false);
                retweetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                retweetBtn.addActionListener(e -> {
                    showRetweetOption(post, alreadyRetweeted);
                    // 리트윗 후 아이콘 업데이트
                    SwingUtilities.invokeLater(() -> {
                        boolean nowRetweeted = postDAO.isRetweetedBy(post.getPostId(), loginUser.getUserId());
                        ImageIcon newIcon = createRetweetIcon(16, nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                        iconLabel3.setIcon(newIcon);
                        countLabel3.setForeground(nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                        countLabel3.setText(String.valueOf(post.getNumOfRetweets()));
                    });
                });
                
                interactionPanel.add(replyBtn);
                interactionPanel.add(retweetBtn);
                interactionPanel.add(likeBtn);
                
                postPanel.add(contentArea, BorderLayout.CENTER);
                postPanel.add(interactionPanel, BorderLayout.SOUTH);
                
                tweetsPanel.add(postPanel);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(tweetsPanel);
        scrollPane.setBorder(null);
        
        profileFrame.add(headerPanel, BorderLayout.NORTH);
        profileFrame.add(scrollPane, BorderLayout.CENTER);
        
        profileFrame.setVisible(true);
    }
    
    // 검색어 하이라이트 헬퍼 메서드
    private String highlightText(String text, String query) {
        if (text == null || query == null || query.trim().isEmpty()) {
            return text;
        }
        
        // 대소문자 구분 없이 검색어를 찾아서 볼드 처리
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        if (!lowerText.contains(lowerQuery)) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        int index;
        
        while ((index = lowerText.indexOf(lowerQuery, lastIndex)) != -1) {
            // 검색어 이전 텍스트 추가
            result.append(text.substring(lastIndex, index));
            // 검색어를 볼드 처리하여 추가
            result.append("<b style='background-color:#FFEB3B; color:black;'>");
            result.append(text.substring(index, index + query.length()));
            result.append("</b>");
            lastIndex = index + query.length();
        }
        // 나머지 텍스트 추가
        result.append(text.substring(lastIndex));
        
        return result.toString();
    }
    
    // HTML 이스케이프 헬퍼 메서드
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * 리트윗/취소/인용 옵션 다이얼로그 표시
     */
    public void showRetweetOption(Post originalPost, boolean isCurrentlyRetweeted) {
        JDialog optionDialog = new JDialog(this, "리트윗", true);
        optionDialog.setSize(320, 180);
        optionDialog.setLocationRelativeTo(this);
        optionDialog.setLayout(new BorderLayout(10, 10));
        optionDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel buttonPanel = new JPanel(new GridLayout(isCurrentlyRetweeted ? 2 : 3, 1, 10, 10));
        
        JButton retweetBtn = new JButton(isCurrentlyRetweeted ? "리트윗 취소" : "바로 리트윗");
        JButton quoteBtn = new JButton("인용 트윗");
        
        // 버튼 스타일링
        retweetBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        retweetBtn.setFocusPainted(false);
        retweetBtn.setOpaque(true);
        if (isCurrentlyRetweeted) {
            retweetBtn.setForeground(Color.RED);
            retweetBtn.setBackground(Color.WHITE);
            retweetBtn.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        } else {
            retweetBtn.setForeground(Color.DARK_GRAY);
            retweetBtn.setBackground(new Color(240, 240, 240));
            retweetBtn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        }
        
        quoteBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        quoteBtn.setFocusPainted(false);
        quoteBtn.setOpaque(true);
        quoteBtn.setForeground(Color.DARK_GRAY);
        quoteBtn.setBackground(new Color(240, 240, 240));
        quoteBtn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        retweetBtn.addActionListener(e -> {
            boolean wasRetweeted = isCurrentlyRetweeted;
            postDAO.toggleRetweet(originalPost.getPostId(), loginUser.getUserId());
            
            // 리트윗 취소가 아니라 새로 리트윗했을 때만 알림 생성
            if (!wasRetweeted && !originalPost.getWriterId().equals(loginUser.getUserId())) {
                createNotification(originalPost.getWriterId(), originalPost.getPostId(), "retweet");
            }
            
            optionDialog.dispose();
            refreshTimeline();
        });

        quoteBtn.addActionListener(e -> {
            handleQuote(originalPost);
            optionDialog.dispose();
        });

        buttonPanel.add(retweetBtn);
        buttonPanel.add(quoteBtn);
        
        if (!isCurrentlyRetweeted) {
            JButton cancelBtn = new JButton("취소");
            cancelBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            cancelBtn.setFocusPainted(false);
            cancelBtn.addActionListener(e -> optionDialog.dispose());
            buttonPanel.add(cancelBtn);
        }

        optionDialog.add(buttonPanel, BorderLayout.CENTER);
        optionDialog.setVisible(true);
    }
}

