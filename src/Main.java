import javax.swing.UIManager; 

import gui.LoginFrame;

public class Main {
    public static void main(String[] args) {
        // Look and Feel 설정: OS 기본 디자인 적용
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace(); 
        }
        
        new LoginFrame(); // 로그인 창 시작
    }
}