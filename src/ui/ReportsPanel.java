package ui;

import dao.ReportDAO;
import dao.RoomDAO;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.HostelStats;
import model.Room;
import util.HostelException;
import util.UIUtil;

/** Reports: summary figures and a room occupancy table, all generated from SQL queries. */
public class ReportsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ReportDAO reportDAO = new ReportDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    private final DefaultTableModel summaryModel = UIUtil.createReadOnlyModel("Report Item", "Value");
    private final JTable summaryTable = UIUtil.createTable(summaryModel);

    private final DefaultTableModel occupancyModel = UIUtil.createReadOnlyModel(
            "Room Number", "Block", "Floor", "Room Type", "Capacity", "Occupied", "Available", "Status");
    private final JTable occupancyTable = UIUtil.createTable(occupancyModel);

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 14));
        UIUtil.stylePage(this);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UIUtil.pageTitle("Reports"), BorderLayout.WEST);
        JButton refreshButton = UIUtil.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        top.add(refreshButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        UIUtil.applyStatusRenderer(occupancyTable, 7);
        summaryTable.setRowSorter(null); // keep the report items in their logical order

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.add(createSection("Hostel Summary", summaryTable), gridCell(0, 0.35, new Insets(0, 0, 0, 8)));
        body.add(createSection("Room Occupancy", occupancyTable), gridCell(1, 0.65, new Insets(0, 8, 0, 0)));
        add(body, BorderLayout.CENTER);
    }

    private JPanel createSection(String title, JTable table) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        section.add(UIUtil.sectionHeading(title), BorderLayout.NORTH);
        section.add(UIUtil.wrapTable(table), BorderLayout.CENTER);
        return section;
    }

    private GridBagConstraints gridCell(int x, double weightX, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = 0;
        c.weightx = weightX;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = insets;
        return c;
    }

    /** Reloads the report from MySQL. */
    public void refreshData() {
        try {
            HostelStats s = reportDAO.getStatistics();
            summaryModel.setRowCount(0);
            summaryModel.addRow(new Object[] {"Total Students", s.getTotalStudents()});
            summaryModel.addRow(new Object[] {"Total Rooms", s.getTotalRooms()});
            summaryModel.addRow(new Object[] {"Total Capacity", s.getTotalCapacity()});
            summaryModel.addRow(new Object[] {"Occupied Beds", s.getOccupiedBeds()});
            summaryModel.addRow(new Object[] {"Available Beds", s.getAvailableBeds()});
            summaryModel.addRow(new Object[] {"Full Rooms", s.getFullRooms()});
            summaryModel.addRow(new Object[] {"Empty Rooms", s.getEmptyRooms()});
            summaryModel.addRow(new Object[] {"Active Allocations", s.getActiveAllocations()});
            summaryModel.addRow(new Object[] {"Vacated Allocations", s.getVacatedAllocations()});

            List<Room> rooms = roomDAO.getRooms("ALL", "");
            occupancyModel.setRowCount(0);
            for (Room r : rooms) {
                occupancyModel.addRow(new Object[] {
                    r.getRoomNumber(), r.getBlock(), r.getFloor(), r.getRoomType(),
                    r.getCapacity(), r.getOccupied(), r.getAvailable(), r.getStatus()
                });
            }
        } catch (HostelException e) {
            UIUtil.showError(this, e.getMessage());
        }
    }
}
