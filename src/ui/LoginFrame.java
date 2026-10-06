package ui;

import dao.AdminDAO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import model.Admin;
import util.HostelException;
import util.UIUtil;

/** Login screen. Credentials are checked against the admins table in MySQL. */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AdminDAO adminDAO = new AdminDAO();
    private final JTextField usernameField = UIUtil.createTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = UIUtil.primaryButton("Login");

    public LoginFrame() {
        super("Hostel Room Allocation System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(460, 440));
        setSize(520, 480);
        buildUI();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(UIUtil.BACKGROUND);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UIUtil.SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(UIUtil.BORDER, 1), new EmptyBorder(28, 36, 28, 36)));

        UIUtil.styleInput(passwordField);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JLabel title = UIUtil.pageTitle("HOSTEL MANAGEMENT");
        c.gridy = 0;
        c.insets = new Insets(0, 0, 2, 0);
        card.add(title, c);

        c.gridy = 1;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(UIUtil.mutedLabel("Hostel Room Allocation System"), c);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 18, 0);
        card.add(new JSeparator(), c);

        c.gridy = 3;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(UIUtil.normalLabel("Username"), c);

        c.gridy = 4;
        c.insets = new Insets(0, 0, 12, 0);
        card.add(usernameField, c);

        c.gridy = 5;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(UIUtil.normalLabel("Password"), c);

        c.gridy = 6;
        c.insets = new Insets(0, 0, 20, 0);
        card.add(passwordField, c);

        c.gridy = 7;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(loginButton, c);

        root.add(card, new GridBagConstraints());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtil.BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        JLabel footerLabel = UIUtil.mutedLabel("Advanced Java Programming Lab - Mini Project");
        footerLabel.setHorizontalAlignment(JLabel.CENTER);
        footer.add(footerLabel, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> attemptLogin());
        getRootPane().setDefaultButton(loginButton); // Enter key = Login
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            UIUtil.showWarning(this, "Please enter both username and password.");
            return;
        }

        try {
            Admin admin = adminDAO.login(username, password);
            if (admin == null) {
                UIUtil.showError(this, "Invalid username or password.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
                return;
            }
            new DashboardFrame(admin).setVisible(true);
            dispose();
        } catch (HostelException ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }
}
