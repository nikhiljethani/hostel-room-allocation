package ui;

import dao.RoomDAO;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import model.Room;
import util.HostelException;
import util.UIUtil;
import util.ValidationUtil;

/**
 * Room Management screen. Occupied, Available and Status are calculated by SQL
 * from the ACTIVE allocations and are read-only here.
 */
public class RoomPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final RoomDAO roomDAO = new RoomDAO();

    private final DefaultTableModel tableModel = UIUtil.createReadOnlyModel(
            "Room Number", "Block", "Floor", "Room Type", "Capacity", "Occupied", "Available", "Status");
    private final JTable table = UIUtil.createTable(tableModel);
    private final JComboBox<String> filterCombo = new JComboBox<>(new String[] {"All", "Available", "Full", "Empty"});
    private final JTextField searchField = UIUtil.createTextField(16);
    private final JLabel countLabel = UIUtil.mutedLabel(" ");

    /** The rooms currently shown, in the same order as the table model rows. */
    private List<Room> currentRooms = new ArrayList<>();

    public RoomPanel() {
        setLayout(new BorderLayout(0, 12));
        UIUtil.stylePage(this);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UIUtil.pageTitle("Room Management"), BorderLayout.WEST);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(12));
        top.add(buildToolbar());
        add(top, BorderLayout.NORTH);

        UIUtil.applyStatusRenderer(table, 7);
        add(UIUtil.wrapTable(table), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        filterCombo.setFont(UIUtil.NORMAL_FONT);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton searchButton = UIUtil.secondaryButton("Search");
        JButton clearButton = UIUtil.secondaryButton("Clear");
        left.add(UIUtil.normalLabel("Status:"));
        left.add(filterCombo);
        left.add(UIUtil.normalLabel("Search:"));
        left.add(searchField);
        left.add(searchButton);
        left.add(clearButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton addButton = UIUtil.primaryButton("Add Room");
        JButton updateButton = UIUtil.secondaryButton("Update Selected");
        JButton deleteButton = UIUtil.dangerButton("Delete Selected");
        right.add(addButton);
        right.add(updateButton);
        right.add(deleteButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        filterCombo.addActionListener(e -> loadRooms());
        searchButton.addActionListener(e -> loadRooms());
        searchField.addActionListener(e -> loadRooms());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            filterCombo.setSelectedIndex(0); // also reloads through the combo listener
            loadRooms();
        });
        addButton.addActionListener(e -> addRoom());
        updateButton.addActionListener(e -> updateRoom());
        deleteButton.addActionListener(e -> deleteRoom());
        return toolbar;
    }

    /** Reloads the room table from MySQL (uses the current filter and search text). */
    public void refreshData() {
        loadRooms();
    }

    private void loadRooms() {
        String filter = String.valueOf(filterCombo.getSelectedItem());
        try {
            currentRooms = roomDAO.getRooms(filter, searchField.getText());
        } catch (HostelException e) {
            UIUtil.showError(this, e.getMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (Room r : currentRooms) {
            tableModel.addRow(new Object[] {
                r.getRoomNumber(), r.getBlock(), r.getFloor(), r.getRoomType(),
                r.getCapacity(), r.getOccupied(), r.getAvailable(), r.getStatus()
            });
        }
        countLabel.setText("Rooms shown: " + currentRooms.size());
    }

    private Room getSelectedRoom() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UIUtil.showWarning(this, "Please select a room from the table first.");
            return null;
        }
        return currentRooms.get(table.convertRowIndexToModel(viewRow));
    }

    private void addRoom() {
        RoomDialog dialog = new RoomDialog(SwingUtilities.getWindowAncestor(this), roomDAO, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadRooms();
        }
    }

    private void updateRoom() {
        Room selected = getSelectedRoom();
        if (selected == null) {
            return;
        }
        RoomDialog dialog = new RoomDialog(SwingUtilities.getWindowAncestor(this), roomDAO, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadRooms();
        }
    }

    private void deleteRoom() {
        Room selected = getSelectedRoom();
        if (selected == null) {
            return;
        }
        String question = "Delete room " + selected.getRoomNumber() + "?\n"
                + "Any old (vacated) allocation history of this room will also be removed.\n"
                + "This cannot be undone.";
        if (!UIUtil.confirm(this, question)) {
            return;
        }
        try {
            roomDAO.deleteRoom(selected.getRoomId());
            UIUtil.showInfo(this, "Room deleted successfully.");
            loadRooms();
        } catch (HostelException e) {
            UIUtil.showWarning(this, e.getMessage());
        }
    }

    // =====================================================================
    //  Add / Update dialog (no occupied / available / status fields)
    // =====================================================================
    private static class RoomDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        private final RoomDAO dao;
        private final Room existing; // null = adding a new room
        private boolean saved = false;

        private final JTextField roomNumberField = UIUtil.createTextField(20);
        private final JTextField blockField = UIUtil.createTextField(20);
        private final JTextField floorField = UIUtil.createTextField(20);
        private final JComboBox<String> typeCombo =
                new JComboBox<>(new String[] {"Single", "Double", "Triple", "Four Sharing"});
        private final JTextField capacityField = UIUtil.createTextField(20);

        RoomDialog(Window owner, RoomDAO dao, Room existing) {
            super(owner, existing == null ? "Add Room" : "Update Room", Dialog.ModalityType.APPLICATION_MODAL);
            this.dao = dao;
            this.existing = existing;
            typeCombo.setEditable(true);
            buildForm();
            if (existing != null) {
                fillForm(existing);
            }
            pack();
            setMinimumSize(new Dimension(420, getHeight()));
            setLocationRelativeTo(owner);
        }

        boolean isSaved() {
            return saved;
        }

        private void buildForm() {
            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(UIUtil.SURFACE);
            form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
            UIUtil.addFormRow(form, 0, "Room Number *", roomNumberField);
            UIUtil.addFormRow(form, 1, "Block *", blockField);
            UIUtil.addFormRow(form, 2, "Floor *", floorField);
            UIUtil.addFormRow(form, 3, "Room Type *", typeCombo);
            UIUtil.addFormRow(form, 4, "Capacity *", capacityField);

            JButton saveButton = UIUtil.primaryButton(existing == null ? "Add Room" : "Save Changes");
            JButton cancelButton = UIUtil.secondaryButton("Cancel");
            saveButton.addActionListener(e -> save());
            cancelButton.addActionListener(e -> dispose());

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setBackground(UIUtil.SURFACE);
            buttons.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
            buttons.add(cancelButton);
            buttons.add(saveButton);

            JPanel note = new JPanel(new BorderLayout());
            note.setBackground(UIUtil.SURFACE);
            note.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            note.add(UIUtil.mutedLabel("* required. Occupied, available and status are calculated automatically."),
                    BorderLayout.WEST);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(UIUtil.SURFACE);
            bottom.add(note, BorderLayout.NORTH);
            bottom.add(buttons, BorderLayout.SOUTH);

            getContentPane().setBackground(UIUtil.SURFACE);
            setLayout(new BorderLayout());
            add(form, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
            getRootPane().setDefaultButton(saveButton);
        }

        private void fillForm(Room r) {
            roomNumberField.setText(r.getRoomNumber());
            blockField.setText(r.getBlock());
            floorField.setText(String.valueOf(r.getFloor()));
            typeCombo.setSelectedItem(r.getRoomType());
            capacityField.setText(String.valueOf(r.getCapacity()));
        }

        private void save() {
            String roomNumber = roomNumberField.getText().trim();
            String block = blockField.getText().trim();
            Object typeValue = typeCombo.getSelectedItem();
            String roomType = typeValue == null ? "" : typeValue.toString().trim();

            if (ValidationUtil.isBlank(roomNumber)) {
                fail("Please enter the room number.", roomNumberField);
                return;
            }
            if (!ValidationUtil.isValidRoomNumber(roomNumber)) {
                fail("Room number may contain only letters, digits and '-' (maximum 10 characters).",
                        roomNumberField);
                return;
            }
            if (ValidationUtil.isBlank(block)) {
                fail("Please enter the block.", blockField);
                return;
            }
            if (block.length() > 20) {
                fail("Block name is too long (maximum 20 characters).", blockField);
                return;
            }

            Integer floor = ValidationUtil.parseInteger(floorField.getText());
            if (floor == null) {
                fail("Please enter a valid floor number (0 or more).", floorField);
                return;
            }
            if (floor < 0 || floor > 50) {
                fail("Floor must be between 0 and 50.", floorField);
                return;
            }

            if (roomType.isEmpty()) {
                UIUtil.showWarning(this, "Please select or enter the room type.");
                typeCombo.requestFocusInWindow();
                return;
            }
            if (roomType.length() > 30) {
                UIUtil.showWarning(this, "Room type is too long (maximum 30 characters).");
                typeCombo.requestFocusInWindow();
                return;
            }

            Integer capacity = ValidationUtil.parseInteger(capacityField.getText());
            if (capacity == null) {
                fail("Please enter a valid capacity (a whole number).", capacityField);
                return;
            }
            if (capacity <= 0) {
                fail("Capacity must be greater than zero.", capacityField);
                return;
            }
            if (capacity > 20) {
                fail("Capacity cannot be more than 20 students per room.", capacityField);
                return;
            }

            Room r = new Room();
            if (existing != null) {
                r.setRoomId(existing.getRoomId());
            }
            r.setRoomNumber(roomNumber);
            r.setBlock(block);
            r.setFloor(floor);
            r.setRoomType(roomType);
            r.setCapacity(capacity);

            try {
                if (existing == null) {
                    dao.addRoom(r);
                    UIUtil.showInfo(this, "Room added successfully.");
                } else {
                    dao.updateRoom(r);
                    UIUtil.showInfo(this, "Room updated successfully.");
                }
                saved = true;
                dispose();
            } catch (HostelException e) {
                UIUtil.showError(this, e.getMessage());
            }
        }

        private void fail(String message, JTextField fieldToFocus) {
            UIUtil.showWarning(this, message);
            fieldToFocus.requestFocusInWindow();
        }
    }
}
