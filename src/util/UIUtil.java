package util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

/** Colours, fonts and small factory methods so every screen looks the same. */
public final class UIUtil {

    // ----- colour palette -----
    public static final Color PRIMARY = new Color(0x26, 0x32, 0x38);        // dark slate
    public static final Color SECONDARY = new Color(0x3F, 0x5F, 0x73);      // muted blue
    public static final Color BACKGROUND = new Color(0xF4, 0xF5, 0xF6);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(0x26, 0x32, 0x38);
    public static final Color TEXT_MUTED = new Color(0x68, 0x74, 0x7C);
    public static final Color BORDER = new Color(0xD9, 0xDE, 0xE2);
    public static final Color SUCCESS = new Color(0x3F, 0x6B, 0x50);
    public static final Color WARNING = new Color(0x9A, 0x6A, 0x24);
    public static final Color ERROR = new Color(0x8A, 0x3F, 0x3F);

    // ----- extra neutral shades derived from the palette -----
    public static final Color SELECTION = new Color(0xDC, 0xE4, 0xE9);
    public static final Color HEADER_BG = new Color(0xE9, 0xED, 0xF0);
    public static final Color STRIPE = new Color(0xFA, 0xFB, 0xFB);
    public static final Color SIDEBAR_ACCENT = new Color(0xB8, 0xC7, 0xD0);
    public static final Color SIDEBAR_TEXT_MUTED = new Color(0xAE, 0xBB, 0xC2);

    // ----- fonts -----
    public static final String FONT_NAME = "Segoe UI";
    public static final Font TITLE_FONT = new Font(FONT_NAME, Font.BOLD, 20);
    public static final Font HEADING_FONT = new Font(FONT_NAME, Font.BOLD, 15);
    public static final Font NORMAL_FONT = new Font(FONT_NAME, Font.PLAIN, 13);
    public static final Font BOLD_FONT = new Font(FONT_NAME, Font.BOLD, 13);

    private UIUtil() {
    }

    // =====================================================================
    //  Look and feel
    // =====================================================================

    /** Uses the native look and feel and applies the application font everywhere. */
    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("[UI] Could not apply system look and feel: " + e.getMessage());
        }
        List<Object> keys = Collections.list(UIManager.getDefaults().keys());
        for (Object key : keys) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(NORMAL_FONT));
            }
        }
    }

    // =====================================================================
    //  Labels, panels, inputs
    // =====================================================================

    public static JLabel pageTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HEADING_FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(NORMAL_FONT);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    public static JLabel normalLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(NORMAL_FONT);
        label.setForeground(TEXT);
        return label;
    }

    /** Standard background and padding for a full-screen page panel. */
    public static void stylePage(JPanel page) {
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(20, 24, 20, 24));
    }

    /** White panel with a thin border (used for dashboard figures and the allocation form). */
    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(12, 16, 12, 16)));
        return card;
    }

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        styleInput(field);
        return field;
    }

    public static void styleInput(JTextComponent component) {
        component.setFont(NORMAL_FONT);
        component.setForeground(TEXT);
        component.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(5, 8, 5, 8)));
    }

    /** Adds "label : field" as one row of a GridBagLayout form (labels in column 0, fields in column 1). */
    public static void addFormRow(JPanel form, int row, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 14);
        form.add(normalLabel(labelText), c);

        field.setFont(NORMAL_FONT);
        Dimension size = field.getPreferredSize();
        field.setPreferredSize(new Dimension(size.width, Math.max(size.height, 30)));

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(field, c);
    }

    // =====================================================================
    //  Buttons
    // =====================================================================

    /** Dark slate button for the main action of a screen. */
    public static JButton primaryButton(String text) {
        return createButton(text, PRIMARY, Color.WHITE, SECONDARY, PRIMARY);
    }

    /** White button with a thin border for normal actions. */
    public static JButton secondaryButton(String text) {
        return createButton(text, SURFACE, TEXT, HEADER_BG, BORDER);
    }

    /** White button with a red label for destructive actions such as Delete. */
    public static JButton dangerButton(String text) {
        return createButton(text, SURFACE, ERROR, HEADER_BG, ERROR);
    }

    private static JButton createButton(String text, Color background, Color foreground,
            Color hoverBackground, Color borderColor) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI()); // flat button that honours our colours on every look and feel
        button.setFont(NORMAL_FONT);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(new LineBorder(borderColor, 1), new EmptyBorder(6, 16, 6, 16)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverBackground);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }

    /** Flat text button used in the dark sidebar. Colours are changed by DashboardFrame when selected. */
    public static JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(NORMAL_FONT.deriveFont(14f));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // =====================================================================
    //  Tables
    // =====================================================================

    /** A table model whose cells cannot be edited and whose numbers sort as numbers. */
    public static DefaultTableModel createReadOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (getRowCount() > 0 && getValueAt(0, column) != null) {
                    return getValueAt(0, column).getClass();
                }
                return Object.class;
            }
        };
    }

    /** Creates a plain, flat, sortable table in the application style. */
    public static JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(NORMAL_FONT);
        table.setForeground(TEXT);
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        table.setDefaultRenderer(Object.class, new BodyRenderer());
        table.setDefaultRenderer(Number.class, new BodyRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 30));
        return table;
    }

    /** Wraps a table in a scroll pane with a thin border. */
    public static JScrollPane wrapTable(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER, 1));
        scrollPane.getViewport().setBackground(SURFACE);
        return scrollPane;
    }

    /** Shows the text of this column in a status colour (ACTIVE, FULL, ...). */
    public static void applyStatusRenderer(JTable table, int columnIndex) {
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new StatusRenderer());
    }

    public static Color statusColor(String status) {
        if (status == null) {
            return TEXT;
        }
        switch (status) {
            case "ACTIVE":
            case "AVAILABLE":
                return SUCCESS;
            case "FULL":
                return ERROR;
            case "EMPTY":
                return SECONDARY;
            default:
                return TEXT_MUTED; // VACATED and anything else
        }
    }

    /** Date text for tables: ISO date, or "-" when there is no date (e.g. not vacated yet). */
    public static String formatDate(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    private static class BodyRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(NORMAL_FONT);
            setForeground(TEXT);
            if (isSelected) {
                setBackground(SELECTION);
            } else {
                setBackground(row % 2 == 0 ? SURFACE : STRIPE);
            }
            return this;
        }
    }

    private static class StatusRenderer extends BodyRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(statusColor(value == null ? null : value.toString()));
            setFont(BOLD_FONT);
            return this;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            String text = value == null ? "" : value.toString();
            RowSorter<?> sorter = table.getRowSorter();
            if (sorter != null) {
                int modelColumn = table.convertColumnIndexToModel(column);
                for (RowSorter.SortKey key : sorter.getSortKeys()) {
                    if (key.getColumn() == modelColumn) {
                        if (key.getSortOrder() == SortOrder.ASCENDING) {
                            text += "  \u25B2";
                        } else if (key.getSortOrder() == SortOrder.DESCENDING) {
                            text += "  \u25BC";
                        }
                    }
                }
            }
            setText(text);
            setOpaque(true);
            setBackground(HEADER_BG);
            setForeground(TEXT);
            setFont(BOLD_FONT);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 8, 0, 8)));
            return this;
        }
    }

    // =====================================================================
    //  Message boxes
    // =====================================================================

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Yes/No question. Returns true only when the user clicks Yes. */
    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
