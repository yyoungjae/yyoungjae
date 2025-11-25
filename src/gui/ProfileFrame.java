package gui;

import dao.UserDAO;
import dao.PostDAO;
import dao.CommentDAO;
import dao.FollowDAO;
import dao.MuteDAO;
import model.User;
import model.Post;
import model.Comment;
import util.TimeFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class ProfileFrame extends JFrame {
    private User loggedInUser;
    private UserDAO userDao;
    private PostDAO postDao;
    private CommentDAO commentDao;
    private FollowDAO followDao;
    private MuteDAO muteDAO;
    private PostFrame mainPostFrame; // 👈 PostFrame 인스턴스
    
    // 설정 탭에서 사용할 컴포넌트
    private JTextField emailField, displayNameField;
    private JTextArea bioArea;
    private JLabel profileImagePreview; // 사진 미리보기 레이블
    
    // 💡 트위터 블루 색상 정의
    private static final Color TWITTER_BLUE = new Color(29, 161, 242);
    private static final Color TWITTER_GREEN = new Color(23, 191, 99);
    
    // 리트윗 아이콘 생성 메서드 (트위터 스타일)
    private ImageIcon createRetweetIcon(int size, Color color) {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2.setColor(color);
        float strokeWidth = Math.max(1.5f, size / 10.0f);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        int centerX = size / 2;
        int centerY = size / 2;
        int radius = (int)(size * 0.35);
        
        // 원형 경로 그리기
        java.awt.geom.Arc2D arc = new java.awt.geom.Arc2D.Float(
            centerX - radius, centerY - radius, 
            radius * 2, radius * 2, 
            0, 180, 
            java.awt.geom.Arc2D.OPEN
        );
        g2.draw(arc);
        
        // 위쪽 화살표 (시계 반대 방향)
        int arrowSize = (int)(size * 0.15);
        int topX = centerX;
        int topY = centerY - radius;
        
        // 화살표 머리 (위쪽)
        java.awt.geom.Path2D topArrow = new java.awt.geom.Path2D.Float();
        topArrow.moveTo(topX, topY - arrowSize);
        topArrow.lineTo(topX - arrowSize/2, topY);
        topArrow.lineTo(topX, topY + arrowSize/3);
        topArrow.lineTo(topX + arrowSize/2, topY);
        topArrow.closePath();
        g2.fill(topArrow);
        
        // 아래쪽 화살표 (시계 방향)
        int bottomX = centerX;
        int bottomY = centerY + radius;
        
        // 화살표 머리 (아래쪽)
        java.awt.geom.Path2D bottomArrow = new java.awt.geom.Path2D.Float();
        bottomArrow.moveTo(bottomX, bottomY + arrowSize);
        bottomArrow.lineTo(bottomX - arrowSize/2, bottomY);
        bottomArrow.lineTo(bottomX, bottomY - arrowSize/3);
        bottomArrow.lineTo(bottomX + arrowSize/2, bottomY);
        bottomArrow.closePath();
        g2.fill(bottomArrow);
        
        g2.dispose();
        return new ImageIcon(icon);
    }

    public ProfileFrame(User user, PostFrame mainFrame) {
        this.loggedInUser = user;
        this.mainPostFrame = mainFrame;
        this.userDao = new UserDAO();
        this.postDao = new PostDAO(); 
        this.commentDao = new CommentDAO();
        this.followDao = new FollowDAO();
        this.muteDAO = new MuteDAO();
        this.mainPostFrame = mainFrame; // 👈 인스턴스 저장

        setTitle("내 프로필 - " + user.getDisplayName());
        setSize(500, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // 1. 프로필 정보 및 통계 영역 (NORTH)
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. 탭 영역 (CENTER)
        JTabbedPane tabbedPane = new JTabbedPane();
        
        JPanel myPostsPanel = createMyPostsPanel();
        JScrollPane myPostsScrollPane = new JScrollPane(myPostsPanel);
        myPostsScrollPane.setBorder(null);
        myPostsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        myPostsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        myPostsScrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        myPostsScrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        
        tabbedPane.addTab("📝 내 트윗", myPostsScrollPane);
        tabbedPane.addTab("👤 프로필 설정", createProfileSettingsPanel());
        tabbedPane.addTab("🔇 뮤트 및 차단", createMuteAndBlockPanel());
        tabbedPane.addTab("⚙️ 내 계정", createAccountSettingsPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // 3. 닫기 버튼
        JButton closeBtn = new JButton("닫기");
        closeBtn.addActionListener(e -> dispose());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setBackground(Color.WHITE);
        southPanel.add(closeBtn);
        add(southPanel, BorderLayout.SOUTH);

        setVisible(true);
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
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // 1. 프로필 이미지 영역 (원형)
        JLabel profileImageLabel = new JLabel("👤", SwingConstants.CENTER);
        profileImageLabel.setPreferredSize(new Dimension(80, 80)); 
        profileImageLabel.setMinimumSize(new Dimension(80, 80));
        profileImageLabel.setMaximumSize(new Dimension(80, 80));
        profileImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profileImageLabel.setVerticalAlignment(SwingConstants.CENTER);
        profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 50)); // 기본 아이콘 크기

        if (loggedInUser.getProfileImagePath() != null && !loggedInUser.getProfileImagePath().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(loggedInUser.getProfileImagePath(), 80);
            if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                profileImageLabel.setIcon(circularIcon);
                profileImageLabel.setText("");
            } else {
                profileImageLabel.setIcon(null);
                profileImageLabel.setText("👤");
            }
        } else {
            profileImageLabel.setIcon(null);
            profileImageLabel.setText("👤"); 
        }
        
        // 2. 사용자 이름/ID/Bio
        JPanel userInfoPanel = new JPanel(new GridLayout(3, 1));
        userInfoPanel.setBackground(Color.WHITE);
        userInfoPanel.setBorder(new EmptyBorder(0, 5, 0, 0)); 

        JLabel nameLabel = new JLabel(loggedInUser.getDisplayName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        JLabel idLabel = new JLabel("@" + loggedInUser.getUserId(), SwingConstants.LEFT);
        idLabel.setForeground(Color.GRAY);
        JTextArea bioArea = new JTextArea(loggedInUser.getBio() != null ? loggedInUser.getBio() : "자기소개 없음");
        bioArea.setEditable(false);
        bioArea.setBackground(header.getBackground());
        bioArea.setLineWrap(true);
        
        userInfoPanel.add(nameLabel);
        userInfoPanel.add(idLabel);
        userInfoPanel.add(bioArea);
        
        JPanel topRowPanel = new JPanel(new BorderLayout());
        topRowPanel.setBackground(Color.WHITE);
        topRowPanel.add(profileImageLabel, BorderLayout.WEST);
        topRowPanel.add(userInfoPanel, BorderLayout.CENTER);
        
        header.add(topRowPanel, BorderLayout.NORTH);

        // 3. 통계 및 생성일
        JPanel statPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        statPanel.setBackground(Color.WHITE);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월");
        String createdDate = sdf.format(loggedInUser.getCreatedAt());
        JLabel dateLabel = new JLabel("가입일: " + createdDate);
        dateLabel.setForeground(Color.DARK_GRAY);
        
        int followingCount = userDao.getFollowingCount(loggedInUser.getUserId());
        int followerCount = userDao.getFollowerCount(loggedInUser.getUserId());
        int postCount = userDao.getPostCount(loggedInUser.getUserId());
        
        JLabel followingLabel = new JLabel("<html><b>" + followingCount + "</b> <span style='color:gray'>팔로잉</span></html>");
        followingLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        followingLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showFollowingList();
            }
        });
        
        JLabel followerLabel = new JLabel("<html><b>" + followerCount + "</b> <span style='color:gray'>팔로워</span></html>");
        followerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        followerLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showFollowerList();
            }
        });
        
        JLabel postCountLabel = new JLabel("<html><b>" + postCount + "</b> <span style='color:gray'>트윗</span></html>"); 
        
        statPanel.add(dateLabel);
        statPanel.add(new JLabel(""));
        statPanel.add(followingLabel);
        statPanel.add(followerLabel);
        statPanel.add(postCountLabel);
        statPanel.add(new JLabel("")); 

        header.add(statPanel, BorderLayout.CENTER);
        
        return header;
    }
    
    private JPanel createMyPostsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        List<Post> posts = postDao.getPostsByUserId(loggedInUser.getUserId());
        
        if (posts.isEmpty()) {
            panel.add(Box.createVerticalStrut(20));
            JLabel emptyLabel = new JLabel("작성한 게시글이 없습니다.", SwingConstants.CENTER);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(emptyLabel);
            panel.add(Box.createVerticalGlue());
        } else {
            for (Post p : posts) {
                // 리트윗/인용 글 처리
                if (p.getPostType() != null && (p.getPostType().equals("retweet") || p.getPostType().equals("quote"))) {
                    panel.add(createInteractionPostPanel(p));
                    continue;
                }
                
                // 일반 게시글 표시
                panel.add(createNormalPostPanel(p));
            }
        }
        return panel;
    }
    
    // 일반 트윗 패널 생성 (PostFrame의 displayNormalPost와 유사)
    private JPanel createNormalPostPanel(Post p) {
        JPanel postContainer = new JPanel(new BorderLayout());
        postContainer.setBackground(Color.WHITE);
        JPanel postPanel = new JPanel(new BorderLayout(5, 5));
        postPanel.setBackground(Color.WHITE);
        postPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 238, 240)),
                    BorderFactory.createEmptyBorder(10, 10, 5, 10)
                ));
                
        postPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        postPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    mainPostFrame.showPostDetail(p);
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
                User clickedUser = userDao.getUserById(p.getWriterId());
                if (clickedUser != null) {
                    mainPostFrame.showUserProfile(clickedUser);
                }
            }
        });

        JPanel writerInfoPanel = new JPanel(new BorderLayout(5, 0));
        writerInfoPanel.setBackground(Color.WHITE);

        // 프로필 이미지 표시 (원형)
        JLabel profilePicLabel = new JLabel("👤", SwingConstants.CENTER);
        profilePicLabel.setPreferredSize(new Dimension(40, 40));
        profilePicLabel.setMinimumSize(new Dimension(40, 40));
        profilePicLabel.setMaximumSize(new Dimension(40, 40));
        profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profilePicLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        if (p.getWriterProfileImagePath() != null && !p.getWriterProfileImagePath().isEmpty()) {
            ImageIcon circularIcon = createCircularProfileImage(p.getWriterProfileImagePath(), 40);
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

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBackground(Color.WHITE);
        namePanel.add(writerLabel, BorderLayout.CENTER);
        
        writerInfoPanel.add(profilePicLabel, BorderLayout.WEST);
        writerInfoPanel.add(namePanel, BorderLayout.CENTER);
        
        // 내용 영역
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(Color.WHITE);
        JTextArea contentArea = new JTextArea(p.getContent());
        contentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setBackground(Color.WHITE);
        contentArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        contentArea.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 트윗 내용 클릭 시 상세보기
        contentArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                mainPostFrame.showPostDetail(p);
            }
        });
        
        contentWrapper.add(contentArea, BorderLayout.NORTH);
        
        // 이미지/동영상 표시
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try {
                File imgFile = new File(p.getImagePath());
                if (imgFile.exists()) {
                    JLabel imageLabel = new JLabel();
                    ImageIcon icon = new ImageIcon(p.getImagePath());
                    Image img = icon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(img));
                    imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            mainPostFrame.showPostDetail(p);
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
                File videoFile = new File(p.getVideoPath());
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
                                JOptionPane.showMessageDialog(ProfileFrame.this, "동영상을 열 수 없습니다: " + ex.getMessage());
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
        boolean alreadyLiked = postDao.isLikedBy(p.getPostId(), loggedInUser.getUserId());
        
        JButton likeButton = new JButton("♥ " + p.getNumOfLikes());
        likeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        likeButton.setOpaque(false);
        likeButton.setContentAreaFilled(false);
        likeButton.setBorderPainted(false);
        likeButton.setForeground(alreadyLiked ? Color.RED : Color.GRAY);
        likeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        likeButton.addActionListener(e -> {
            boolean likedNow = postDao.toggleLike(p.getPostId(), loggedInUser.getUserId());
            p.setNumOfLikes(likedNow ? p.getNumOfLikes() + 1 : p.getNumOfLikes() - 1);
            likeButton.setText("♥ " + p.getNumOfLikes());
            likeButton.setForeground(likedNow ? Color.RED : Color.GRAY);
            // 프로필 패널 새로고침
            refreshMyPostsPanel();
            // 타임라인도 동기화
            if (mainPostFrame != null) {
                mainPostFrame.refreshTimeline();
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
            String replyText = JOptionPane.showInputDialog(ProfileFrame.this, p.getDisplayName() + "님에게 답글을 작성하세요:");
            
            if (replyText != null && !replyText.trim().isEmpty()) {
                Comment reply = new Comment(
                    "r" + System.currentTimeMillis(),
                    replyText,
                    loggedInUser.getUserId(),
                    p.getPostId(),
                    0,
                    null,
                    loggedInUser.getDisplayName()
                );
                boolean success = commentDao.addComment(reply);
                if (success) {
                    refreshMyPostsPanel();
                    if (mainPostFrame != null) {
                        mainPostFrame.refreshTimeline();
                    }
                } else {
                    JOptionPane.showMessageDialog(ProfileFrame.this, "답글 작성 실패!");
                }
            }
        });

        boolean alreadyRetweeted = postDao.isRetweetedBy(p.getPostId(), loggedInUser.getUserId());

        // 리트윗 버튼 - 트위터 스타일 아이콘 사용
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
            if (mainPostFrame != null) {
                mainPostFrame.showRetweetOption(p, alreadyRetweeted);
                // 리트윗 후 아이콘 업데이트
                SwingUtilities.invokeLater(() -> {
                    boolean nowRetweeted = postDao.isRetweetedBy(p.getPostId(), loggedInUser.getUserId());
                    ImageIcon newIcon = createRetweetIcon(16, nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                    iconLabel.setIcon(newIcon);
                    countLabel.setForeground(nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                    countLabel.setText(String.valueOf(p.getNumOfRetweets()));
                });
                refreshMyPostsPanel();
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(replyToPostBtn);
        bottomPanel.add(retweetBtn);
        bottomPanel.add(likeButton);
        
        // 자신의 트윗인 경우 삭제 버튼 추가
        if (p.getWriterId().equals(loggedInUser.getUserId())) {
            JButton deleteButton = new JButton("삭제");
            deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            deleteButton.setForeground(new Color(220, 53, 69));
            deleteButton.setOpaque(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                    ProfileFrame.this,
                    "이 트윗을 삭제하시겠습니까?",
                    "트윗 삭제",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = postDao.deletePost(p.getPostId());
                    if (success) {
                        JOptionPane.showMessageDialog(ProfileFrame.this, "트윗이 삭제되었습니다.");
                        refreshMyPostsPanel();
                        if (mainPostFrame != null) {
                            mainPostFrame.refreshTimeline();
                        }
                    } else {
                        JOptionPane.showMessageDialog(ProfileFrame.this, "트윗 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            bottomPanel.add(deleteButton);
        }

        postPanel.add(writerInfoPanel, BorderLayout.NORTH);
        postPanel.add(contentWrapper, BorderLayout.CENTER);
        postPanel.add(bottomPanel, BorderLayout.SOUTH);

        postContainer.add(postPanel, BorderLayout.NORTH);
        
        return postContainer;
    }
    
    // 리트윗/인용 트윗 패널 생성
    private JPanel createInteractionPostPanel(Post p) {
        if (p.getPostType().equals("retweet") && p.getContent().isEmpty()) {
            // 단순 리트윗
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 238, 240)),
                BorderFactory.createEmptyBorder(10, 10, 5, 10)
            ));
            
            JLabel headerLabel = new JLabel("<html><span style='color:gray;'><b>" + p.getDisplayName() + "</b> 님이 리트윗했습니다</span></html>");
            headerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            headerLabel.setForeground(Color.GRAY);
            panel.add(headerLabel, BorderLayout.NORTH);
            
            // 원본 트윗 표시
            if (p.getParentPostId() != null) {
                Post originalPost = postDao.getPostById(p.getParentPostId());
                if (originalPost != null) {
                    JPanel originalPostPanel = createOriginalPostBox(originalPost);
                    panel.add(originalPostPanel, BorderLayout.CENTER);
                }
            }
            
        return panel;
        } else if (p.getPostType().equals("quote")) {
            // 인용 트윗
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 238, 240)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            
            // 프로필 이미지와 작성자 정보 패널
            JPanel writerInfoPanel = new JPanel(new BorderLayout(5, 0));
            writerInfoPanel.setBackground(Color.WHITE);
            
            // 프로필 이미지 표시 (원형)
            JLabel profilePicLabel = new JLabel("👤", SwingConstants.CENTER);
            profilePicLabel.setPreferredSize(new Dimension(40, 40));
            profilePicLabel.setMinimumSize(new Dimension(40, 40));
            profilePicLabel.setMaximumSize(new Dimension(40, 40));
            profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
            profilePicLabel.setVerticalAlignment(SwingConstants.CENTER);
            
            if (p.getWriterProfileImagePath() != null && !p.getWriterProfileImagePath().isEmpty()) {
                ImageIcon circularIcon = createCircularProfileImage(p.getWriterProfileImagePath(), 40);
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
            
            String relativeTime = TimeFormatter.formatRelativeTime(p.getCreatedAt());
            JLabel headerLabel = new JLabel("<html><b>" + p.getDisplayName() + "</b> <span style='color:gray;'>@" + p.getWriterId() + " · " + relativeTime + " • 인용함</span></html>");
            headerLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            headerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JPanel namePanel = new JPanel(new BorderLayout());
            namePanel.setBackground(Color.WHITE);
            namePanel.add(headerLabel, BorderLayout.CENTER);
            
            writerInfoPanel.add(profilePicLabel, BorderLayout.WEST);
            writerInfoPanel.add(namePanel, BorderLayout.CENTER);
            
            // 인용 트윗 본문
            JTextArea quoteContentArea = new JTextArea(p.getContent());
            quoteContentArea.setEditable(false);
            quoteContentArea.setLineWrap(true);
            quoteContentArea.setWrapStyleWord(true);
            quoteContentArea.setBackground(Color.WHITE);
            quoteContentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            quoteContentArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
            
            // 상단 영역
            JPanel topArea = new JPanel(new BorderLayout());
            topArea.setBackground(Color.WHITE);
            topArea.add(writerInfoPanel, BorderLayout.NORTH);
            topArea.add(quoteContentArea, BorderLayout.CENTER);
            
            panel.add(topArea, BorderLayout.NORTH);
            
            // 원본 트윗 표시
            if (p.getParentPostId() != null) {
                Post originalPost = postDao.getPostById(p.getParentPostId());
                if (originalPost != null) {
                    JPanel originalPostPanel = createOriginalPostBox(originalPost);
                    panel.add(originalPostPanel, BorderLayout.CENTER);
                }
            }
            
            // 좋아요, 답글, 리트윗 버튼
            boolean alreadyLiked = postDao.isLikedBy(p.getPostId(), loggedInUser.getUserId());
            
            JButton likeButton = new JButton("♥ " + p.getNumOfLikes());
            likeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            likeButton.setOpaque(false);
            likeButton.setContentAreaFilled(false);
            likeButton.setBorderPainted(false);
            likeButton.setForeground(alreadyLiked ? Color.RED : Color.GRAY);
            likeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            likeButton.addActionListener(e -> {
                boolean likedNow = postDao.toggleLike(p.getPostId(), loggedInUser.getUserId());
                p.setNumOfLikes(likedNow ? p.getNumOfLikes() + 1 : p.getNumOfLikes() - 1);
                likeButton.setText("♥ " + p.getNumOfLikes());
                likeButton.setForeground(likedNow ? Color.RED : Color.GRAY);
                refreshMyPostsPanel();
                if (mainPostFrame != null) {
                    mainPostFrame.refreshTimeline();
                }
            });
            
            JButton replyToPostBtn = new JButton("답글 " + p.getNumOfComments());
            replyToPostBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            replyToPostBtn.setOpaque(false);
            replyToPostBtn.setContentAreaFilled(false);
            replyToPostBtn.setBorderPainted(false);
            replyToPostBtn.setForeground(Color.GRAY);
            replyToPostBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            replyToPostBtn.addActionListener(ev -> {
                String replyText = JOptionPane.showInputDialog(ProfileFrame.this, p.getDisplayName() + "님에게 답글을 작성하세요:");
                
                if (replyText != null && !replyText.trim().isEmpty()) {
                    Comment reply = new Comment(
                        "r" + System.currentTimeMillis(),
                        replyText,
                        loggedInUser.getUserId(),
                        p.getPostId(),
                        0,
                        null,
                        loggedInUser.getDisplayName()
                    );
                    boolean success = commentDao.addComment(reply);
                    if (success) {
                        refreshMyPostsPanel();
                        if (mainPostFrame != null) {
                            mainPostFrame.refreshTimeline();
                        }
                    } else {
                        JOptionPane.showMessageDialog(ProfileFrame.this, "답글 작성 실패!");
                    }
                }
            });
            
            boolean alreadyRetweeted = postDao.isRetweetedBy(p.getPostId(), loggedInUser.getUserId());
            
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
                if (mainPostFrame != null) {
                    mainPostFrame.showRetweetOption(p, alreadyRetweeted);
                    // 리트윗 후 아이콘 업데이트
                    SwingUtilities.invokeLater(() -> {
                        boolean nowRetweeted = postDao.isRetweetedBy(p.getPostId(), loggedInUser.getUserId());
                        ImageIcon newIcon = createRetweetIcon(16, nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                        iconLabel2.setIcon(newIcon);
                        countLabel2.setForeground(nowRetweeted ? TWITTER_GREEN : Color.GRAY);
                        countLabel2.setText(String.valueOf(p.getNumOfRetweets()));
                    });
                    refreshMyPostsPanel();
                }
            });
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
            bottomPanel.setBackground(Color.WHITE);
            bottomPanel.add(replyToPostBtn);
            bottomPanel.add(retweetBtn);
            bottomPanel.add(likeButton);
            
            // 자신의 인용트인 경우 삭제 버튼 추가
            if (p.getWriterId().equals(loggedInUser.getUserId())) {
                JButton deleteButton = new JButton("삭제");
                deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
                deleteButton.setForeground(new Color(220, 53, 69));
                deleteButton.setOpaque(false);
                deleteButton.setContentAreaFilled(false);
                deleteButton.setBorderPainted(false);
                deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                deleteButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(
                        ProfileFrame.this,
                        "이 인용트를 삭제하시겠습니까?",
                        "인용트 삭제",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        boolean success = postDao.deletePost(p.getPostId());
                        if (success) {
                            JOptionPane.showMessageDialog(ProfileFrame.this, "인용트가 삭제되었습니다.");
                            refreshMyPostsPanel();
                            if (mainPostFrame != null) {
                                mainPostFrame.refreshTimeline();
                            }
                        } else {
                            JOptionPane.showMessageDialog(ProfileFrame.this, "인용트 삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                bottomPanel.add(deleteButton);
            }
            
            panel.add(bottomPanel, BorderLayout.SOUTH);
            
            return panel;
        }
        return new JPanel();
    }
    
    // 원본 트윗 박스 생성
    private JPanel createOriginalPostBox(Post originalPost) {
        JPanel originalPanel = new JPanel(new BorderLayout(5, 5));
        originalPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(207, 217, 222), 1, true),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        originalPanel.setBackground(Color.WHITE);

        // 원본 작성자 정보
        JLabel originalWriterLabel = new JLabel(
            "<html><b>" + originalPost.getDisplayName() + "</b> <span style='color:gray;'>@" + originalPost.getWriterId() + "</span></html>"
        );
        originalWriterLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        originalWriterLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        originalPanel.add(originalWriterLabel, BorderLayout.NORTH);

        // 원본 내용
        JTextArea originalContent = new JTextArea(originalPost.getContent());
        originalContent.setEditable(false);
        originalContent.setLineWrap(true);
        originalContent.setWrapStyleWord(true);
        originalContent.setBackground(Color.WHITE);
        originalContent.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        originalContent.setBorder(null);
        originalPanel.add(originalContent, BorderLayout.CENTER);
        
        return originalPanel;
    }
    
    // 내 트윗 패널 새로고침
    private void refreshMyPostsPanel() {
        // 탭이 열려있을 때만 새로고침
        JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(1);
        if (tabbedPane.getSelectedIndex() == 0) { // "내 트윗" 탭이 선택된 경우
            JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(0);
            JPanel panel = (JPanel) scrollPane.getViewport().getView();
            
            // 현재 스크롤 위치 저장
            int scrollPosition = scrollPane.getVerticalScrollBar().getValue();
            
            panel.removeAll();
            
            List<Post> posts = postDao.getPostsByUserId(loggedInUser.getUserId());
            
            if (posts.isEmpty()) {
                panel.add(Box.createVerticalStrut(20));
                JLabel emptyLabel = new JLabel("작성한 게시글이 없습니다.", SwingConstants.CENTER);
                emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(emptyLabel);
                panel.add(Box.createVerticalGlue());
            } else {
                for (Post p : posts) {
                    if (p.getPostType() != null && (p.getPostType().equals("retweet") || p.getPostType().equals("quote"))) {
                        panel.add(createInteractionPostPanel(p));
                    } else {
                        panel.add(createNormalPostPanel(p));
                    }
                }
            }
            
            panel.revalidate();
            panel.repaint();
            
            // 스크롤 위치 복원 (SwingUtilities.invokeLater로 레이아웃 완료 후 실행)
            SwingUtilities.invokeLater(() -> {
                int maxScroll = scrollPane.getVerticalScrollBar().getMaximum() - scrollPane.getVerticalScrollBar().getVisibleAmount();
                int restorePosition = Math.min(scrollPosition, maxScroll);
                scrollPane.getVerticalScrollBar().setValue(restorePosition);
            });
        }
    }
    
    // 프로필 설정 패널 (프로필 사진, 표시 이름, Bio만)
    private JPanel createProfileSettingsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // 1. 프로필 사진 미리보기 및 선택
        profileImagePreview = new JLabel();
        profileImagePreview.setPreferredSize(new Dimension(80, 80)); 
        profileImagePreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JButton selectImageBtn = new JButton("프로필 사진 선택...");
        selectImageBtn.addActionListener(e -> selectProfileImage());
        
        JPanel imageControlPanel = new JPanel(new BorderLayout(10, 0));
        imageControlPanel.setBackground(Color.WHITE);
        imageControlPanel.add(profileImagePreview, BorderLayout.WEST);
        imageControlPanel.add(selectImageBtn, BorderLayout.CENTER);
        
        // 초기 이미지 로드
        updateImagePreview(loggedInUser.getProfileImagePath());

        // 2. 표시 이름 필드
        displayNameField = new JTextField(loggedInUser.getDisplayName());
        displayNameField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        displayNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, displayNameField.getPreferredSize().height));
        
        JPanel displayNamePanel = new JPanel(new BorderLayout(10, 5));
        displayNamePanel.setBackground(Color.WHITE);
        displayNamePanel.add(new JLabel("표시 이름 (닉네임):"), BorderLayout.NORTH);
        displayNamePanel.add(displayNameField, BorderLayout.CENTER);

        // 3. Bio 영역
        bioArea = new JTextArea(5, 20);
        bioArea.setText(loggedInUser.getBio() != null ? loggedInUser.getBio() : "");
        bioArea.setLineWrap(true);
        bioArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        JScrollPane bioScrollPane = new JScrollPane(bioArea);
        bioScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        JPanel bioPanel = new JPanel(new BorderLayout(0, 5));
        bioPanel.setBackground(Color.WHITE);
        bioPanel.add(new JLabel("자기소개 (Bio):"), BorderLayout.NORTH);
        bioPanel.add(bioScrollPane, BorderLayout.CENTER);
        
        // 4. 저장 버튼
        JButton saveBtn = new JButton("프로필 정보 저장");
        saveBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        saveBtn.setBackground(new Color(29, 161, 242));
        saveBtn.setForeground(Color.BLACK);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(200, 40));
        
        // 모든 컴포넌트를 BoxLayout으로 추가
        mainPanel.add(imageControlPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(displayNamePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(bioPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(saveBtn);
        mainPanel.add(Box.createVerticalGlue());
        
        saveBtn.addActionListener(e -> saveProfile());
        
        // 스크롤 가능한 패널로 반환
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // 뮤트 및 차단 패널
    private JPanel createMuteAndBlockPanel() {
        JPanel mutePanel = createMuteSettingsPanel();
        
        JScrollPane scrollPane = new JScrollPane(mutePanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // 내 계정 설정 패널 (서브탭 포함)
    private JPanel createAccountSettingsPanel() {
        JTabbedPane accountTabbedPane = new JTabbedPane();
        
        accountTabbedPane.addTab("계정 정보", createAccountInfoPanel());
        accountTabbedPane.addTab("비밀번호 변경하기", createChangePasswordPanel());
        accountTabbedPane.addTab("계정 비활성화하기", createDeactivateAccountPanel());
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(accountTabbedPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // 계정 정보 패널
    private JPanel createAccountInfoPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 사용자 ID (읽기 전용)
        JLabel userIdLabel = new JLabel("사용자 ID:");
        userIdLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        userIdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField userIdField = new JTextField(loggedInUser.getUserId());
        userIdField.setEditable(false);
        userIdField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        userIdField.setBackground(new Color(240, 240, 240));
        userIdField.setMaximumSize(new Dimension(Integer.MAX_VALUE, userIdField.getPreferredSize().height));
        
        JPanel userIdPanel = new JPanel(new BorderLayout(10, 5));
        userIdPanel.setBackground(Color.WHITE);
        userIdPanel.add(userIdLabel, BorderLayout.NORTH);
        userIdPanel.add(userIdField, BorderLayout.CENTER);
        userIdPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 이메일
        JLabel emailLabel = new JLabel("이메일:");
        emailLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        emailField = new JTextField(loggedInUser.getEmail());
        emailField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, emailField.getPreferredSize().height));
        
        JPanel emailPanel = new JPanel(new BorderLayout(10, 5));
        emailPanel.setBackground(Color.WHITE);
        emailPanel.add(emailLabel, BorderLayout.NORTH);
        emailPanel.add(emailField, BorderLayout.CENTER);
        emailPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 저장 버튼
        JButton saveBtn = new JButton("계정 정보 저장");
        saveBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        saveBtn.setBackground(new Color(29, 161, 242));
        saveBtn.setForeground(Color.BLACK);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(200, 40));
        
        mainPanel.add(userIdPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(emailPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(saveBtn);
        mainPanel.add(Box.createVerticalGlue());
        
        saveBtn.addActionListener(e -> {
            loggedInUser.setEmail(emailField.getText().trim());
            if (userDao.updateProfile(loggedInUser)) {
                JOptionPane.showMessageDialog(this, "✅ 계정 정보가 성공적으로 업데이트되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "❌ 계정 정보 업데이트 실패.");
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // 비밀번호 변경 패널
    private JPanel createChangePasswordPanel() {
        // 중앙 정렬을 위한 컨테이너 패널
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 현재 비밀번호
        JLabel currentPwdLabel = new JLabel("현재 비밀번호:");
        currentPwdLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        centerPanel.add(currentPwdLabel, gbc);
        
        JPasswordField currentPwdField = new JPasswordField();
        currentPwdField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        currentPwdField.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(currentPwdField, gbc);
        
        // 새 비밀번호
        JLabel newPwdLabel = new JLabel("새 비밀번호:");
        newPwdLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(newPwdLabel, gbc);
        
        JPasswordField newPwdField = new JPasswordField();
        newPwdField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        newPwdField.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(newPwdField, gbc);
        
        // 비밀번호 확인
        JLabel confirmPwdLabel = new JLabel("새 비밀번호 확인:");
        confirmPwdLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(confirmPwdLabel, gbc);
        
        JPasswordField confirmPwdField = new JPasswordField();
        confirmPwdField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        confirmPwdField.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(confirmPwdField, gbc);
        
        // 변경 버튼
        JButton changeBtn = new JButton("비밀번호 변경");
        changeBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        changeBtn.setBackground(new Color(29, 161, 242));
        changeBtn.setForeground(Color.BLACK);
        changeBtn.setFocusPainted(false);
        changeBtn.setBorderPainted(false);
        changeBtn.setOpaque(true);
        changeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeBtn.setPreferredSize(new Dimension(300, 40));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 10, 10, 10);
        centerPanel.add(changeBtn, gbc);
        
        changeBtn.addActionListener(e -> {
            String currentPwd = new String(currentPwdField.getPassword());
            String newPwd = new String(newPwdField.getPassword());
            String confirmPwd = new String(confirmPwdField.getPassword());
            
            if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                JOptionPane.showMessageDialog(this, "모든 필드를 입력하세요.");
                return;
            }
            
            if (!newPwd.equals(confirmPwd)) {
                JOptionPane.showMessageDialog(this, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
                return;
            }
            
            if (userDao.updatePassword(loggedInUser.getUserId(), currentPwd, newPwd)) {
                JOptionPane.showMessageDialog(this, "✅ 비밀번호가 성공적으로 변경되었습니다.");
                currentPwdField.setText("");
                newPwdField.setText("");
                confirmPwdField.setText("");
                loggedInUser.setPwd(newPwd);
            } else {
                JOptionPane.showMessageDialog(this, "❌ 비밀번호 변경 실패. 현재 비밀번호를 확인하세요.");
            }
        });
        
        // 중앙 정렬을 위한 래퍼 패널
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Color.WHITE);
        wrapperPanel.add(centerPanel, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // 계정 비활성화 패널
    private JPanel createDeactivateAccountPanel() {
        // 중앙 정렬을 위한 컨테이너 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(80, 100, 80, 100));
        
        // 경고 메시지
        JLabel warningLabel = new JLabel("<html><div style='text-align: center; width: 400px;'>" +
            "<h2 style='margin: 10px 0;'>정말로 비활성화하시겠습니까?</h2>" +
            "<p style='font-size: 14px; color: #666; margin-top: 15px; line-height: 1.5;'>" +
            "비활성화 버튼을 누르시고 30일 뒤면 계정이 영구삭제됩니다</p>" +
            "</div></html>");
        warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // 비활성화 버튼
        JButton deactivateBtn = new JButton("비활성화");
        deactivateBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        deactivateBtn.setBackground(new Color(220, 53, 69)); // 빨간색
        deactivateBtn.setForeground(Color.BLACK);
        deactivateBtn.setFocusPainted(false);
        deactivateBtn.setBorderPainted(false);
        deactivateBtn.setOpaque(true);
        deactivateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deactivateBtn.setPreferredSize(new Dimension(300, 50));
        deactivateBtn.setMaximumSize(new Dimension(300, 50));
        deactivateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        deactivateBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(warningLabel);
        centerPanel.add(Box.createVerticalStrut(40));
        centerPanel.add(deactivateBtn);
        centerPanel.add(Box.createVerticalGlue());
        
        deactivateBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "정말로 계정을 비활성화하시겠습니까?\n30일 후 계정이 영구적으로 삭제됩니다.",
                "계정 비활성화 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (confirm == JOptionPane.YES_OPTION) {
                // 비밀번호 입력 다이얼로그
                JPasswordField passwordField = new JPasswordField(20);
                passwordField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                
                JPanel passwordPanel = new JPanel(new BorderLayout(10, 10));
                passwordPanel.add(new JLabel("비밀번호를 입력하세요:"), BorderLayout.NORTH);
                passwordPanel.add(passwordField, BorderLayout.CENTER);
                passwordPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                int passwordResult = JOptionPane.showConfirmDialog(
                    this,
                    passwordPanel,
                    "비밀번호 확인",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                );
                
                if (passwordResult == JOptionPane.OK_OPTION) {
                    String password = new String(passwordField.getPassword());
                    if (password.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "비밀번호를 입력하세요.");
                        return;
                    }
                    
                    if (userDao.deactivateAccount(loggedInUser.getUserId(), password)) {
                        loggedInUser.setDeactivated(true);
                        JOptionPane.showMessageDialog(
                            this,
                            "계정이 비활성화되었습니다.\n30일 후 계정이 영구적으로 삭제됩니다.",
                            "계정 비활성화",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        // 로그아웃 처리
                        if (mainPostFrame != null) {
                            mainPostFrame.dispose();
                        }
                        dispose();
                        new LoginFrame();
                    } else {
                        JOptionPane.showMessageDialog(
                            this,
                            "비밀번호가 일치하지 않습니다.",
                            "비활성화 실패",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });
        
        // 중앙 정렬을 위한 래퍼 패널
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Color.WHITE);
        wrapperPanel.add(centerPanel, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    private void updateImagePreview(String path) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            ImageIcon circularIcon = createCircularProfileImage(path, 80);
            if (circularIcon != null && circularIcon.getIconWidth() > 0) {
                profileImagePreview.setIcon(circularIcon);
                profileImagePreview.setText("");
            } else {
                profileImagePreview.setIcon(null);
                profileImagePreview.setText("👤");
            }
        } else {
            profileImagePreview.setIcon(null);
            profileImagePreview.setText("👤");
        }
    }
    
    private void selectProfileImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("프로필 사진 선택");
        // 이미지 파일 필터 추가는 생략
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            loggedInUser.setProfileImagePath(path); // 모델에 경로 임시 저장
            updateImagePreview(path); // 미리보기 갱신
        }
    }
    
    private void saveProfile() {
        // User 객체의 필드 업데이트 (프로필 설정만)
        loggedInUser.setDisplayName(displayNameField.getText().trim());
        loggedInUser.setBio(bioArea.getText().trim());
        // profileImagePath는 selectProfileImage에서 이미 모델에 저장됨
        
        boolean success = userDao.updateProfile(loggedInUser);

        if (success) {
            JOptionPane.showMessageDialog(this, "✅ 프로필이 성공적으로 업데이트되었습니다.");
            
            // 메인 피드 갱신
            if (mainPostFrame != null) {
                mainPostFrame.refreshTimeline();
            }
            dispose();
            new ProfileFrame(loggedInUser, mainPostFrame); // 갱신된 정보로 프레임 다시 열기
        } else {
            JOptionPane.showMessageDialog(this, "❌ 프로필 업데이트 실패.");
        }
    }
    
    private void replyToPost(Post parentPost) {
        // ... (내 트윗 목록에서 답글 다는 로직은 ProfileFrame에서만 사용되므로 그대로 유지) ...
        // ... (성공 시) ...
        // if (success) {
        //     dispose();
        //     new ProfileFrame(loggedInUser, mainPostFrame);
        // }
    }
    
    // 팔로워 리스트 표시
    private void showFollowerList() {
        List<User> followers = followDao.getFollowers(loggedInUser.getUserId());
        new FollowerFollowingListFrame("팔로워", followers, loggedInUser, mainPostFrame, followDao);
    }
    
    // 팔로잉 리스트 표시
    private void showFollowingList() {
        List<User> following = followDao.getFollowing(loggedInUser.getUserId());
        new FollowerFollowingListFrame("팔로잉", following, loggedInUser, mainPostFrame, followDao);
    }
    
    // 뮤트 설정 패널 생성
    private JPanel createMuteSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("뮤트 설정"));
        
        // 뮤트 단어 설정
        JPanel wordMutePanel = new JPanel();
        wordMutePanel.setBackground(Color.WHITE);
        wordMutePanel.setLayout(new BoxLayout(wordMutePanel, BoxLayout.Y_AXIS));
        wordMutePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel wordLabel = new JLabel("뮤트할 단어:");
        wordLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        wordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField wordInput = new JTextField();
        wordInput.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        wordInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, wordInput.getPreferredSize().height));
        
        JButton addWordBtn = new JButton("추가");
        addWordBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        addWordBtn.setForeground(Color.BLACK);
        addWordBtn.setFocusPainted(false);
        addWordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 뮤트된 단어 목록
        DefaultListModel<String> wordListModel = new DefaultListModel<>();
        JList<String> wordList = new JList<>(wordListModel);
        wordList.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        wordList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wordList.setVisibleRowCount(5);
        JScrollPane wordScrollPane = new JScrollPane(wordList);
        wordScrollPane.setPreferredSize(new Dimension(0, 150));
        wordScrollPane.setMinimumSize(new Dimension(0, 150));
        wordScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        wordScrollPane.setBorder(BorderFactory.createTitledBorder("뮤트된 단어 목록"));
        // 마우스 휠 스크롤 속도 조절
        wordScrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        // 뮤트된 단어 목록 로드
        List<String> mutedWords = muteDAO.getMutedWords(loggedInUser.getUserId());
        if (mutedWords.isEmpty()) {
            wordListModel.addElement("(뮤트된 단어가 없습니다)");
            wordList.setEnabled(false);
        } else {
            for (String word : mutedWords) {
                wordListModel.addElement("• " + word);
            }
        }
        
        JButton removeWordBtn = new JButton("삭제");
        removeWordBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        removeWordBtn.setForeground(Color.BLACK);
        removeWordBtn.setFocusPainted(false);
        removeWordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeWordBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel wordInputPanel = new JPanel(new BorderLayout(5, 0));
        wordInputPanel.setBackground(Color.WHITE);
        wordInputPanel.add(wordInput, BorderLayout.CENTER);
        wordInputPanel.add(addWordBtn, BorderLayout.EAST);
        wordInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, wordInputPanel.getPreferredSize().height));
        
        JPanel wordButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wordButtonPanel.setBackground(Color.WHITE);
        wordButtonPanel.add(removeWordBtn);
        wordButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        wordMutePanel.add(wordLabel);
        wordMutePanel.add(Box.createVerticalStrut(5));
        wordMutePanel.add(wordInputPanel);
        wordMutePanel.add(Box.createVerticalStrut(10));
        wordMutePanel.add(wordScrollPane);
        wordMutePanel.add(Box.createVerticalStrut(5));
        wordMutePanel.add(wordButtonPanel);
        
        // 뮤트 사용자 설정
        JPanel userMutePanel = new JPanel();
        userMutePanel.setBackground(Color.WHITE);
        userMutePanel.setLayout(new BoxLayout(userMutePanel, BoxLayout.Y_AXIS));
        userMutePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel userLabel = new JLabel("뮤트할 사용자 ID:");
        userLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField userInput = new JTextField();
        userInput.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        userInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, userInput.getPreferredSize().height));
        
        JButton addUserBtn = new JButton("추가");
        addUserBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        addUserBtn.setForeground(Color.BLACK);
        addUserBtn.setFocusPainted(false);
        addUserBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 뮤트된 사용자 목록
        DefaultListModel<String> userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setVisibleRowCount(5);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(0, 150));
        userScrollPane.setMinimumSize(new Dimension(0, 150));
        userScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        userScrollPane.setBorder(BorderFactory.createTitledBorder("뮤트된 사용자 목록"));
        // 마우스 휠 스크롤 속도 조절
        userScrollPane.getVerticalScrollBar().setUnitIncrement(24); // 휠 속도 조정 (더 빠르게)
        
        // 뮤트된 사용자 목록 로드
        List<String> mutedUsers = muteDAO.getMutedUsers(loggedInUser.getUserId());
        if (mutedUsers.isEmpty()) {
            userListModel.addElement("(뮤트된 사용자가 없습니다)");
            userList.setEnabled(false);
        } else {
            for (String userId : mutedUsers) {
                User mutedUser = userDao.getUserById(userId);
                if (mutedUser != null) {
                    userListModel.addElement(mutedUser.getDisplayName() + " (@" + userId + ")");
                } else {
                    userListModel.addElement("@" + userId);
                }
            }
        }
        
        JButton removeUserBtn = new JButton("삭제");
        removeUserBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        removeUserBtn.setForeground(Color.BLACK);
        removeUserBtn.setFocusPainted(false);
        removeUserBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeUserBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel userInputPanel = new JPanel(new BorderLayout(5, 0));
        userInputPanel.setBackground(Color.WHITE);
        userInputPanel.add(userInput, BorderLayout.CENTER);
        userInputPanel.add(addUserBtn, BorderLayout.EAST);
        userInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, userInputPanel.getPreferredSize().height));
        
        JPanel userButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        userButtonPanel.setBackground(Color.WHITE);
        userButtonPanel.add(removeUserBtn);
        userButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        userMutePanel.add(userLabel);
        userMutePanel.add(Box.createVerticalStrut(5));
        userMutePanel.add(userInputPanel);
        userMutePanel.add(Box.createVerticalStrut(10));
        userMutePanel.add(userScrollPane);
        userMutePanel.add(Box.createVerticalStrut(5));
        userMutePanel.add(userButtonPanel);
        
        // 단어 추가 버튼 액션
        addWordBtn.addActionListener(e -> {
            String word = wordInput.getText().trim();
            if (word.isEmpty()) {
                JOptionPane.showMessageDialog(this, "단어를 입력하세요.");
                return;
            }
            // 중복 체크
            for (int i = 0; i < wordListModel.getSize(); i++) {
                String existing = wordListModel.getElementAt(i);
                if (existing.startsWith("• ")) {
                    existing = existing.substring(2);
                }
                if (existing.equals(word)) {
                    JOptionPane.showMessageDialog(this, "이미 뮤트된 단어입니다.");
                    return;
                }
            }
            if (muteDAO.addMutedWord(loggedInUser.getUserId(), word)) {
                // 빈 목록 메시지 제거
                if (wordListModel.getSize() == 1 && wordListModel.getElementAt(0).equals("(뮤트된 단어가 없습니다)")) {
                    wordListModel.clear();
                    wordList.setEnabled(true);
                }
                wordListModel.addElement("• " + word);
                wordInput.setText("");
                if (mainPostFrame != null) {
                    mainPostFrame.refreshTimeline();
                }
                JOptionPane.showMessageDialog(this, "뮤트 단어가 추가되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "뮤트 단어 추가에 실패했습니다.");
            }
        });
        
        // 단어 삭제 버튼 액션
        removeWordBtn.addActionListener(e -> {
            int selectedIndex = wordList.getSelectedIndex();
            if (selectedIndex >= 0) {
                String item = wordListModel.getElementAt(selectedIndex);
                // "• " 접두사 제거
                String word = item.startsWith("• ") ? item.substring(2) : item;
                
                // 빈 목록 메시지인 경우 무시
                if (word.equals("(뮤트된 단어가 없습니다)")) {
                    JOptionPane.showMessageDialog(this, "삭제할 단어를 선택하세요.");
                    return;
                }
                
                if (muteDAO.removeMutedWord(loggedInUser.getUserId(), word)) {
                    wordListModel.remove(selectedIndex);
                    // 목록이 비면 메시지 표시
                    if (wordListModel.getSize() == 0) {
                        wordListModel.addElement("(뮤트된 단어가 없습니다)");
                        wordList.setEnabled(false);
                    }
                    if (mainPostFrame != null) {
                        mainPostFrame.refreshTimeline();
                    }
                    JOptionPane.showMessageDialog(this, "뮤트 단어가 삭제되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(this, "뮤트 단어 삭제에 실패했습니다.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "삭제할 단어를 선택하세요.");
            }
        });
        
        // 리스트 더블클릭으로도 삭제 가능
        wordList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedIndex = wordList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        String item = wordListModel.getElementAt(selectedIndex);
                        String word = item.startsWith("• ") ? item.substring(2) : item;
                        if (!word.equals("(뮤트된 단어가 없습니다)")) {
                            if (muteDAO.removeMutedWord(loggedInUser.getUserId(), word)) {
                                wordListModel.remove(selectedIndex);
                                if (wordListModel.getSize() == 0) {
                                    wordListModel.addElement("(뮤트된 단어가 없습니다)");
                                    wordList.setEnabled(false);
                                }
                                if (mainPostFrame != null) {
                                    mainPostFrame.refreshTimeline();
                                }
                                JOptionPane.showMessageDialog(ProfileFrame.this, "뮤트 단어가 삭제되었습니다.");
                            }
                        }
                    }
                }
            }
        });
        
        // 사용자 추가 버튼 액션
        addUserBtn.addActionListener(e -> {
            String userId = userInput.getText().trim();
            if (userId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "사용자 ID를 입력하세요.");
                return;
            }
            if (userId.equals(loggedInUser.getUserId())) {
                JOptionPane.showMessageDialog(this, "자기 자신은 뮤트할 수 없습니다.");
                return;
            }
            User targetUser = userDao.getUserById(userId);
            if (targetUser == null) {
                JOptionPane.showMessageDialog(this, "존재하지 않는 사용자입니다.");
                return;
            }
            if (muteDAO.addMutedUser(loggedInUser.getUserId(), userId)) {
                // 빈 목록 메시지 제거
                if (userListModel.getSize() == 1 && userListModel.getElementAt(0).equals("(뮤트된 사용자가 없습니다)")) {
                    userListModel.clear();
                    userList.setEnabled(true);
                }
                userListModel.addElement(targetUser.getDisplayName() + " (@" + userId + ")");
                userInput.setText("");
                if (mainPostFrame != null) {
                    mainPostFrame.refreshTimeline();
                }
                JOptionPane.showMessageDialog(this, "사용자가 뮤트되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, "사용자 뮤트에 실패했습니다. (이미 뮤트된 사용자일 수 있습니다)");
            }
        });
        
        // 사용자 삭제 버튼 액션
        removeUserBtn.addActionListener(e -> {
            int selectedIndex = userList.getSelectedIndex();
            if (selectedIndex >= 0) {
                String item = userListModel.getElementAt(selectedIndex);
                // 빈 목록 메시지인 경우 무시
                if (item.equals("(뮤트된 사용자가 없습니다)")) {
                    JOptionPane.showMessageDialog(this, "삭제할 사용자를 선택하세요.");
                    return;
                }
                // "이름 (@userId)" 형식에서 userId 추출
                String userId = item.substring(item.indexOf("(@") + 2, item.indexOf(")"));
                if (muteDAO.removeMutedUser(loggedInUser.getUserId(), userId)) {
                    userListModel.remove(selectedIndex);
                    // 목록이 비면 메시지 표시
                    if (userListModel.getSize() == 0) {
                        userListModel.addElement("(뮤트된 사용자가 없습니다)");
                        userList.setEnabled(false);
                    }
                    if (mainPostFrame != null) {
                        mainPostFrame.refreshTimeline();
                    }
                    JOptionPane.showMessageDialog(this, "뮤트가 해제되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(this, "뮤트 해제에 실패했습니다.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "삭제할 사용자를 선택하세요.");
            }
        });
        
        // 사용자 리스트 더블클릭으로도 삭제 가능
        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedIndex = userList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        String item = userListModel.getElementAt(selectedIndex);
                        if (!item.equals("(뮤트된 사용자가 없습니다)")) {
                            String userId = item.substring(item.indexOf("(@") + 2, item.indexOf(")"));
                            if (muteDAO.removeMutedUser(loggedInUser.getUserId(), userId)) {
                                userListModel.remove(selectedIndex);
                                if (userListModel.getSize() == 0) {
                                    userListModel.addElement("(뮤트된 사용자가 없습니다)");
                                    userList.setEnabled(false);
                                }
                                if (mainPostFrame != null) {
                                    mainPostFrame.refreshTimeline();
                                }
                                JOptionPane.showMessageDialog(ProfileFrame.this, "뮤트가 해제되었습니다.");
                            }
                        }
                    }
                }
            }
        });
        
        // 패널 통합
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(wordMutePanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(userMutePanel);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
}