package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class PlaceholderTextField extends JTextField {

    private String placeholder;
    private Color placeholderColor = new Color(180, 180, 180);

    public PlaceholderTextField(String placeholder) {
        this.placeholder = placeholder;

        setForeground(Color.BLACK);

        // 포커스 잃으면 placeholder 다시 표시
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(placeholderColor);
            g2.setFont(getFont().deriveFont(Font.PLAIN));
            Insets insets = getInsets();
            g2.drawString(placeholder, insets.left + 2, getHeight() / 2 + getFont().getSize() / 2 - 4);
            g2.dispose();
        }
    }
}
