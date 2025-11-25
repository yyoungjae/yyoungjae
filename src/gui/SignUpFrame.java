package gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SignUpFrame extends JFrame {

    private PlaceholderTextField idField;
    private PlaceholderTextField emailField;
    private PlaceholderTextField displayNameField;
    private PlaceholderPasswordField passwordField;

    private static final Color TWITTER_BLUE = new Color(29, 161, 242);
    private UserDAO userDao = new UserDAO();

    public SignUpFrame() {
        setTitle("Twitter Clone - Sign Up");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 680);
        setLocationRelativeTo(null);
        setResizable(false);

        // 📌 그라디언트 배경
        JPanel bgPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color top = new Color(239, 246, 255);
                Color bottom = new Color(219, 234, 254);
                g2d.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bgPanel.setLayout(new GridBagLayout());
        setContentPane(bgPanel);

        // 📌 카드 패널
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(360, 520));

        // 타이틀
        JLabel logo = new JLabel("t", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logo.setForeground(TWITTER_BLUE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Create your account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(12));
        card.add(title);
        card.add(Box.createVerticalStrut(20));

        // ===========================
        // 📌 입력 필드
        // ===========================
        idField = new PlaceholderTextField("아이디 (USERNAME)");
        styleField(idField);

        emailField = new PlaceholderTextField("이메일");
        styleField(emailField);

        displayNameField = new PlaceholderTextField("닉네임 (DISPLAY NAME)");
        styleField(displayNameField);

        passwordField = new PlaceholderPasswordField("비밀번호");
        styleField(passwordField);

        card.add(idField);
        card.add(Box.createVerticalStrut(12));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));
        card.add(displayNameField);
        card.add(Box.createVerticalStrut(12));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(20));

        // ===========================
        // 📌 회원가입 버튼
        // ===========================
        JButton signUpButton = new JButton("회원가입");
        signUpButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setBackground(TWITTER_BLUE);
        signUpButton.setForeground(Color.BLACK);
        signUpButton.setFocusPainted(false);
        signUpButton.setOpaque(true);
        signUpButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        signUpButton.addActionListener(this::signUpAction);

        card.add(signUpButton);
        card.add(Box.createVerticalStrut(20));

        // ===========================
        // 📌 로그인 이동 링크
        // ===========================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footer.setOpaque(false);

        JLabel info = new JLabel("이미 계정이 있으신가요?");
        JButton loginLink = new JButton("로그인");
        loginLink.setForeground(TWITTER_BLUE);
        loginLink.setContentAreaFilled(false);
        loginLink.setBorderPainted(false);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addActionListener(e -> backToLogin());

        footer.add(info);
        footer.add(loginLink);
        card.add(footer);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        bgPanel.add(card, gbc);

        setVisible(true);
    }

    // ===========================
    // 📌 필드 스타일 통합
    // ===========================
    private void styleField(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        field.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        // 핵심 포인트!!
        field.setAlignmentX(Component.CENTER_ALIGNMENT);  // BoxLayout 전체 너비 사용
    }

    // ===========================
    // 📌 회원가입 처리
    // ===========================
    private void signUpAction(ActionEvent e) {
        String id = idField.getText().trim();
        String email = emailField.getText().trim();
        String displayName = displayNameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (id.isEmpty() || email.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 항목을 입력해주세요.");
            return;
        }

        // 📌 UserDAO.register() 구조에 정확히 맞춘 User 객체 생성
        User newUser = new User(
                id,             // user_id
                password,       // pwd
                id,             // username (username 없으므로 user_id와 동일하게 저장)
                email,
                displayName,
                "",             // bio
                null,           // createdAt (DB에서 NOW() 자동 생성)
                "",             // profile_image_path
                false           // isDeactivated
        );

        boolean success = userDao.register(newUser);

        if (success) {
            JOptionPane.showMessageDialog(this, "회원가입 완료!");
            new LoginFrame();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "회원가입 실패 - 이미 존재하는 계정일 수 있습니다.");
        }
    }

    private void backToLogin() {
        new LoginFrame();
        dispose();
    }
}
