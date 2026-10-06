package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import model.Admin;
import util.UIUtil;

/**
 * Main window: dark sidebar on the left, the selected page on the right.
 * Every time a page is opened its refreshData() method reloads the data from MySQL,
 * so no page can show stale information.
 */
public class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String PAGE_DASHBOARD = "Dashboard";
    private static final String PAGE_STUDENTS = "Students";
    private static final String PAGE_ROOMS = "Rooms";
    private static final String PAGE_ALLOCATIONS = "Allocations";
    private static final String PAGE_REPORTS = "Reports";

    private final Admin admin;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final Map<String, JButton> menuButtons = new LinkedHashMap<>();

    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final StudentPanel studentPanel = new StudentPanel();
    private final RoomPanel roomPanel = new RoomPanel();
    private final AllocationPanel allocationPanel = new AllocationPanel();
    private final ReportsPanel reportsPanel = new ReportsPanel();

    public DashboardFrame(Admin admin) {
        super("Hostel Room Allocation System");
        this.admin = admin;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setSize(1200, 720);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);

        setLocationRelativeTo(null);
        showPage(PAGE_DASHBOARD);
    }

    // ------------------------------------------------------------------
    //  Sidebar
    // ------------------------------------------------------------------
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIUtil.PRIMARY);
        sidebar.setPreferredSize(new Dimension(210, 0));

        // Application name
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UIUtil.SECONDARY), new EmptyBorder(24, 20, 20, 20)));
        JLabel line1 = new JLabel("HOSTEL");
        line1.setFont(new Font(UIUtil.FONT_NAME, Font.BOLD, 22));
        line1.setForeground(Color.WHITE);
        JLabel line2 = new JLabel("MANAGEMENT");
        line2.setFont(new Font(UIUtil.FONT_NAME, Font.PLAIN, 15));
        line2.setForeground(UIUtil.SIDEBAR_TEXT_MUTED);
        header.add(line1);
        header.add(line2);
        sidebar.add(header, BorderLayout.NORTH);

        // Menu items
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(UIUtil.PRIMARY);
        menu.setBorder(new EmptyBorder(12, 0, 0, 0));
        String[] pages = {PAGE_DASHBOARD, PAGE_STUDENTS, PAGE_ROOMS, PAGE_ALLOCATIONS, PAGE_REPORTS};
        for (String page : pages) {
            JButton button = UIUtil.createSidebarButton(page);
            styleMenuButton(button, false);
            button.addActionListener(e -> showPage(page));
            menuButtons.put(page, button);
            menu.add(button);
        }
        sidebar.add(menu, BorderLayout.CENTER);

        // Logout and signed-in user
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(UIUtil.PRIMARY);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, UIUtil.SECONDARY));
        JButton logoutButton = UIUtil.createSidebarButton("Logout");
        styleMenuButton(logoutButton, false);
        logoutButton.addActionListener(e -> logout());
        JLabel userLabel = new JLabel("Signed in as: " + admin.getUsername());
        userLabel.setFont(new Font(UIUtil.FONT_NAME, Font.PLAIN, 12));
        userLabel.setForeground(UIUtil.SIDEBAR_TEXT_MUTED);
        userLabel.setBorder(new EmptyBorder(4, 20, 14, 10));
        userLabel.setAlignmentX(LEFT_ALIGNMENT);
        footer.add(Box.createVerticalStrut(8));
        footer.add(logoutButton);
        footer.add(userLabel);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    /** Selected item: muted blue background with a light accent line on the left. */
    private void styleMenuButton(JButton button, boolean selected) {
        button.setBackground(selected ? UIUtil.SECONDARY : UIUtil.PRIMARY);
        button.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, selected ? UIUtil.SIDEBAR_ACCENT : UIUtil.PRIMARY),
                new EmptyBorder(11, 16, 11, 16)));
    }

    // ------------------------------------------------------------------
    //  Content pages
    // ------------------------------------------------------------------
    private JPanel buildContent() {
        cardPanel.setBackground(UIUtil.BACKGROUND);
        cardPanel.add(dashboardPanel, PAGE_DASHBOARD);
        cardPanel.add(studentPanel, PAGE_STUDENTS);
        cardPanel.add(roomPanel, PAGE_ROOMS);
        cardPanel.add(allocationPanel, PAGE_ALLOCATIONS);
        cardPanel.add(reportsPanel, PAGE_REPORTS);
        return cardPanel;
    }

    /** Shows a page, highlights its menu item and reloads its data from MySQL. */
    private void showPage(String page) {
        cardLayout.show(cardPanel, page);
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            styleMenuButton(entry.getValue(), entry.getKey().equals(page));
        }

        switch (page) {
            case PAGE_DASHBOARD:
                dashboardPanel.refreshData();
                break;
            case PAGE_STUDENTS:
                studentPanel.refreshData();
                break;
            case PAGE_ROOMS:
                roomPanel.refreshData();
                break;
            case PAGE_ALLOCATIONS:
                allocationPanel.refreshData();
                break;
            case PAGE_REPORTS:
                reportsPanel.refreshData();
                break;
            default:
                break;
        }
    }

    private void logout() {
        if (UIUtil.confirm(this, "Do you want to log out?")) {
            new LoginFrame().setVisible(true); // open the login window first,
            dispose();                          // then close this one
        }
    }
}
