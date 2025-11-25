package gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private UserDAO userDao = new UserDAO();

    private static final Color TWITTER_BLUE = new Color(29, 161, 242);

    public LoginFrame() {
        setTitle("Twitter Clone - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 640);
        setLocationRelativeTo(null);
        setResizable(false);

        // 📌 그라디언트 배경 패널
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color top = new Color(239, 246, 255);
                Color bottom = new Color(219, 234, 254);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        setContentPane(backgroundPanel);

        // 📌 카드 패널 (흰색 박스)
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new EmptyBorder(32, 36, 32, 36));
        cardPanel.setPreferredSize(new Dimension(360, 460)); // ← 아주 중요!
        cardPanel.setMaximumSize(new Dimension(360, 460));

        // 로고
        JLabel logoLabel = new JLabel("t", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logoLabel.setForeground(TWITTER_BLUE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Sign in to Twitter", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(logoLabel);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(20));

        // 📌 입력 영역
        usernameField = new PlaceholderTextField("아이디 또는 이메일");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        passwordField = new PlaceholderPasswordField("비밀번호");
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        // 아이디 라벨
        JLabel userLabel = new JLabel("ID or EMAIL");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        userLabel.setHorizontalAlignment(SwingConstants.LEFT);
        userLabel.setBorder(new EmptyBorder(0, 10, 0, 0));  // ← 입력필드와 딱 정렬
        cardPanel.add(userLabel);

// 아이디 입력 필드
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(15));

// 비밀번호 라벨
        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passLabel.setHorizontalAlignment(SwingConstants.LEFT);
        passLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        cardPanel.add(passLabel);

// 비밀번호 입력 필드
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(20));

        // 로그인 버튼
        JButton loginButton = new JButton("로그인");
        loginButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        loginButton.setForeground(Color.BLACK);
        loginButton.setBackground(TWITTER_BLUE);
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(true);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        loginButton.addActionListener(this::loginAction);
        cardPanel.add(loginButton);

        cardPanel.add(Box.createVerticalStrut(20));

        // 회원가입 안내
        JPanel signUpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        signUpPanel.setOpaque(false);
        JLabel txt = new JLabel("계정이 없으신가요?");
        JButton signupBtn = new JButton("회원가입");
        signupBtn.setBorderPainted(false);
        signupBtn.setContentAreaFilled(false);
        signupBtn.setForeground(TWITTER_BLUE);
        signupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signupBtn.addActionListener(e -> signUpAction());

        signUpPanel.add(txt);
        signUpPanel.add(signupBtn);
        cardPanel.add(signUpPanel);

        // 📌 레이아웃 중앙 배치
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        backgroundPanel.add(cardPanel, gbc);

        // 반드시 마지막에 실행
        setVisible(true);
    }

    private void loginAction(ActionEvent e) {
        String input = usernameField.getText().trim();
        String pwd = new String(passwordField.getPassword()).trim();

        User u = userDao.login(input, pwd);

        if (u != null) {
            if (u.isDeactivated()) {
                JOptionPane.showMessageDialog(this, "비활성 계정입니다.");
            } else {
                new PostFrame(u);
                dispose();
            }
        } else {
            JOptionPane.showMessageDialog(this, "로그인 실패");
        }
    }

    private void signUpAction() {
        new SignUpFrame();
        dispose();
    }
}
