package ui;

import dao.AllocationDAO;
import dao.RoomDAO;
import dao.StudentDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import model.Allocation;
import model.Room;
import model.Student;
import util.HostelException;
import util.UIUtil;

/**
 * Allocations screen: the allocation form (top) and the allocation records / history (bottom).
 * Only rooms that still have a free bed are offered in the room dropdown.
 */
public class AllocationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final StudentDAO studentDAO = new StudentDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();

    // allocation form
    private final JComboBox<Student> studentCombo = new JComboBox<>();
    private final JComboBox<Room> roomCombo = new JComboBox<>();
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JLabel roomInfoLabel = UIUtil.mutedLabel(" ");

    // records table
    private final DefaultTableModel tableModel = UIUtil.createReadOnlyModel(
            "Allocation ID", "Student Name", "Enrollment No", "Room Number", "Block",
            "Allocation Date", "Vacate Date", "Status");
    private final JTable table = UIUtil.createTable(tableModel);
    private final JTextField searchField = UIUtil.createTextField(18);
    private final JLabel countLabel = UIUtil.mutedLabel(" ");

    /** The records currently shown, in the same order as the table model rows. */
    private List<Allocation> currentRecords = new ArrayList<>();

    public AllocationPanel() {
        setLayout(new BorderLayout(0, 14));
        UIUtil.stylePage(this);

        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UIUtil.pageTitle("Room Allocation"), BorderLayout.WEST);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(12));
        top.add(buildAllocationForm());
        add(top, BorderLayout.NORTH);

        add(buildRecordsSection(), BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    //  Allocation form
    // ------------------------------------------------------------------
    private JPanel buildAllocationForm() {
        JPanel card = UIUtil.createCard();
        card.setLayout(new GridBagLayout());

        JButton allocateButton = UIUtil.primaryButton("Allocate Room");
        allocateButton.addActionListener(e -> allocateRoom());

        studentCombo.setFont(UIUtil.NORMAL_FONT);
        roomCombo.setFont(UIUtil.NORMAL_FONT);
        dateSpinner.setFont(UIUtil.NORMAL_FONT);

        // row 0: student and room
        card.add(UIUtil.normalLabel("Student"), cell(0, 0, 0.0, GridBagConstraints.NONE, 1, new Insets(0, 0, 8, 10)));
        card.add(studentCombo, cell(1, 0, 1.0, GridBagConstraints.HORIZONTAL, 1, new Insets(0, 0, 8, 24)));
        card.add(UIUtil.normalLabel("Room"), cell(2, 0, 0.0, GridBagConstraints.NONE, 1, new Insets(0, 0, 8, 10)));
        card.add(roomCombo, cell(3, 0, 1.0, GridBagConstraints.HORIZONTAL, 1, new Insets(0, 0, 8, 0)));

        // row 1: date and allocate button
        card.add(UIUtil.normalLabel("Allocation Date"), cell(0, 1, 0.0, GridBagConstraints.NONE, 1, new Insets(0, 0, 0, 10)));
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dateRow.setOpaque(false);
        dateRow.add(dateSpinner);
        card.add(dateRow, cell(1, 1, 1.0, GridBagConstraints.HORIZONTAL, 1, new Insets(0, 0, 0, 24)));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(allocateButton);
        card.add(buttonRow, cell(2, 1, 0.0, GridBagConstraints.NONE, 2, new Insets(0, 0, 0, 0)));

        // row 2: information line
        card.add(roomInfoLabel, cell(0, 2, 1.0, GridBagConstraints.HORIZONTAL, 4, new Insets(10, 0, 0, 0)));
        return card;
    }

    private GridBagConstraints cell(int x, int y, double weightX, int fill, int width, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = weightX;
        c.fill = fill;
        c.gridwidth = width;
        c.anchor = GridBagConstraints.WEST;
        c.insets = insets;
        return c;
    }

    // ------------------------------------------------------------------
    //  Records section
    // ------------------------------------------------------------------
    private JPanel buildRecordsSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIUtil.sectionHeading("Allocation Records / History"), BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton searchButton = UIUtil.secondaryButton("Search");
        JButton refreshButton = UIUtil.secondaryButton("Refresh");
        left.add(UIUtil.normalLabel("Search:"));
        left.add(searchField);
        left.add(searchButton);
        left.add(refreshButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton vacateButton = UIUtil.dangerButton("Vacate Room");
        right.add(vacateButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        header.add(toolbar, BorderLayout.CENTER);

        UIUtil.applyStatusRenderer(table, 7);

        section.add(header, BorderLayout.NORTH);
        section.add(UIUtil.wrapTable(table), BorderLayout.CENTER);
        section.add(countLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> loadRecords());
        searchField.addActionListener(e -> loadRecords());
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            refreshData();
        });
        vacateButton.addActionListener(e -> vacateRoom());
        return section;
    }

    // ------------------------------------------------------------------
    //  Loading data from MySQL
    // ------------------------------------------------------------------

    /** Reloads the dropdowns and the records table from MySQL. */
    public void refreshData() {
        loadDropdowns();
        loadRecords();
        dateSpinner.setValue(new Date());
    }

    private void loadDropdowns() {
        try {
            List<Student> students = studentDAO.getAllStudents();
            List<Room> availableRooms = roomDAO.getAvailableRooms();

            studentCombo.removeAllItems();
            for (Student s : students) {
                studentCombo.addItem(s);
            }
            roomCombo.removeAllItems();
            for (Room r : availableRooms) {
                roomCombo.addItem(r);
            }

            roomInfoLabel.setText(availableRooms.size() + " room(s) with free beds. "
                    + "Full rooms are not listed. A student can have only one active allocation.");
        } catch (HostelException e) {
            UIUtil.showError(this, e.getMessage());
        }
    }

    private void loadRecords() {
        try {
            currentRecords = allocationDAO.searchAllocations(searchField.getText());
        } catch (HostelException e) {
            UIUtil.showError(this, e.getMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (Allocation a : currentRecords) {
            tableModel.addRow(new Object[] {
                a.getAllocationId(), a.getStudentName(), a.getEnrollmentNo(), a.getRoomNumber(), a.getBlock(),
                UIUtil.formatDate(a.getAllocationDate()), UIUtil.formatDate(a.getVacateDate()), a.getStatus()
            });
        }
        countLabel.setText("Records shown: " + currentRecords.size());
    }

    // ------------------------------------------------------------------
    //  Allocate
    // ------------------------------------------------------------------
    private void allocateRoom() {
        Student student = (Student) studentCombo.getSelectedItem();
        Room room = (Room) roomCombo.getSelectedItem();

        if (student == null) {
            UIUtil.showWarning(this, "Please select a student. Add students in the Students page first.");
            return;
        }
        if (room == null) {
            UIUtil.showWarning(this, "No room with a free bed is available to select.");
            return;
        }

        LocalDate date = readAllocationDate();
        if (date == null) {
            return;
        }

        try {
            allocationDAO.allocateRoom(student.getStudentId(), room.getRoomId(), date);
            UIUtil.showInfo(this, "Room " + room.getRoomNumber() + " allocated to " + student.getName()
                    + " successfully.");
        } catch (HostelException e) {
            UIUtil.showWarning(this, e.getMessage());
        }
        // Reload in both cases: after a failure the lists may be out of date (e.g. the room just became full).
        refreshData();
    }

    /** Reads and validates the date spinner. Returns null (after a message) if it is not valid. */
    private LocalDate readAllocationDate() {
        try {
            dateSpinner.commitEdit();
        } catch (ParseException e) {
            UIUtil.showWarning(this, "Please enter a valid allocation date (yyyy-MM-dd).");
            return null;
        }
        Date value = (Date) dateSpinner.getValue();
        LocalDate date = value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (date.isAfter(LocalDate.now())) {
            UIUtil.showWarning(this, "Allocation date cannot be in the future.");
            return null;
        }
        return date;
    }

    // ------------------------------------------------------------------
    //  Vacate
    // ------------------------------------------------------------------
    private void vacateRoom() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UIUtil.showWarning(this, "Please select an allocation record from the table first.");
            return;
        }
        Allocation selected = currentRecords.get(table.convertRowIndexToModel(viewRow));

        if (!Allocation.STATUS_ACTIVE.equals(selected.getStatus())) {
            UIUtil.showWarning(this, "This allocation has already been vacated.");
            return;
        }

        String question = "Vacate " + selected.getStudentName() + " (" + selected.getEnrollmentNo()
                + ") from room " + selected.getRoomNumber() + "?";
        if (!UIUtil.confirm(this, question)) {
            return;
        }

        try {
            allocationDAO.vacateAllocation(selected.getAllocationId(), LocalDate.now());
            UIUtil.showInfo(this, selected.getStudentName() + " has vacated room "
                    + selected.getRoomNumber() + " successfully.");
        } catch (HostelException e) {
            UIUtil.showWarning(this, e.getMessage());
        }
        refreshData();
    }
}
