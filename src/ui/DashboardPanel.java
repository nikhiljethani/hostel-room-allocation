package ui;

import dao.AllocationDAO;
import dao.ReportDAO;
import dao.RoomDAO;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.Allocation;
import model.HostelStats;
import model.Room;
import util.HostelException;
import util.UIUtil;

/** Dashboard: live statistics, recent allocations and a room occupancy summary. */
public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ReportDAO reportDAO = new ReportDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    private final JLabel studentsValue = createValueLabel();
    private final JLabel roomsValue = createValueLabel();
    private final JLabel capacityValue = createValueLabel();
    private final JLabel occupiedValue = createValueLabel();
    private final JLabel availableValue = createValueLabel();

    private final DefaultTableModel recentModel =
            UIUtil.createReadOnlyModel("Student", "Room", "Allocation Date", "Status");
    private final JTable recentTable = UIUtil.createTable(recentModel);

    private final DefaultTableModel occupancyModel =
            UIUtil.createReadOnlyModel("Room", "Block", "Capacity", "Occupied", "Available", "Status");
    private final JTable occupancyTable = UIUtil.createTable(occupancyModel);

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        UIUtil.stylePage(this);

        add(UIUtil.pageTitle("Dashboard"), BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 5, 12, 0));
        statsRow.setOpaque(false);
        statsRow.add(createStatCard("TOTAL STUDENTS", studentsValue));
        statsRow.add(createStatCard("TOTAL ROOMS", roomsValue));
        statsRow.add(createStatCard("TOTAL CAPACITY", capacityValue));
        statsRow.add(createStatCard("OCCUPIED BEDS", occupiedValue));
        statsRow.add(createStatCard("AVAILABLE BEDS", availableValue));

        UIUtil.applyStatusRenderer(recentTable, 3);
        UIUtil.applyStatusRenderer(occupancyTable, 5);

        JPanel tablesRow = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesRow.setOpaque(false);
        tablesRow.add(createTableSection("Recent Allocations", recentTable));
        tablesRow.add(createTableSection("Room Occupancy Summary", occupancyTable));

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(statsRow, BorderLayout.NORTH);
        body.add(tablesRow, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private static JLabel createValueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(new Font(UIUtil.FONT_NAME, Font.BOLD, 26));
        label.setForeground(UIUtil.PRIMARY);
        return label;
    }

    private JPanel createStatCard(String caption, JLabel valueLabel) {
        JPanel card = UIUtil.createCard();
        card.setLayout(new BorderLayout(0, 6));
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(new Font(UIUtil.FONT_NAME, Font.BOLD, 12));
        captionLabel.setForeground(UIUtil.TEXT_MUTED);
        card.add(captionLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableSection(String title, JTable table) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        JLabel heading = UIUtil.sectionHeading(title);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        section.add(heading, BorderLayout.NORTH);
        section.add(UIUtil.wrapTable(table), BorderLayout.CENTER);
        return section;
    }

    /** Reloads every figure and table from MySQL. */
    public void refreshData() {
        try {
            HostelStats stats = reportDAO.getStatistics();
            studentsValue.setText(String.valueOf(stats.getTotalStudents()));
            roomsValue.setText(String.valueOf(stats.getTotalRooms()));
            capacityValue.setText(String.valueOf(stats.getTotalCapacity()));
            occupiedValue.setText(String.valueOf(stats.getOccupiedBeds()));
            availableValue.setText(String.valueOf(stats.getAvailableBeds()));

            List<Allocation> recent = allocationDAO.getRecentAllocations(10);
            recentModel.setRowCount(0);
            for (Allocation a : recent) {
                recentModel.addRow(new Object[] {
                    a.getStudentName(),
                    a.getRoomNumber() + " (Block " + a.getBlock() + ")",
                    UIUtil.formatDate(a.getAllocationDate()),
                    a.getStatus()
                });
            }

            List<Room> rooms = roomDAO.getRooms("ALL", "");
            occupancyModel.setRowCount(0);
            for (Room r : rooms) {
                occupancyModel.addRow(new Object[] {
                    r.getRoomNumber(), r.getBlock(), r.getCapacity(),
                    r.getOccupied(), r.getAvailable(), r.getStatus()
                });
            }
        } catch (HostelException e) {
            UIUtil.showError(this, e.getMessage());
        }
    }
}
