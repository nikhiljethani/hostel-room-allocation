package ui;

import dao.StudentDAO;
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
import model.Student;
import util.HostelException;
import util.UIUtil;
import util.ValidationUtil;

/** Student Management screen: search, add, update and delete students stored in MySQL. */
public class StudentPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final StudentDAO studentDAO = new StudentDAO();

    private final DefaultTableModel tableModel = UIUtil.createReadOnlyModel(
            "ID", "Enrollment No", "Name", "Gender", "Course", "Year", "Phone", "Email");
    private final JTable table = UIUtil.createTable(tableModel);
    private final JTextField searchField = UIUtil.createTextField(22);
    private final JLabel countLabel = UIUtil.mutedLabel(" ");

    /** The students currently shown, in the same order as the table model rows. */
    private List<Student> currentStudents = new ArrayList<>();

    public StudentPanel() {
        setLayout(new BorderLayout(0, 12));
        UIUtil.stylePage(this);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UIUtil.pageTitle("Student Management"), BorderLayout.WEST);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(12));
        top.add(buildToolbar());
        add(top, BorderLayout.NORTH);

        add(UIUtil.wrapTable(table), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton searchButton = UIUtil.secondaryButton("Search");
        JButton clearButton = UIUtil.secondaryButton("Clear");
        left.add(UIUtil.normalLabel("Search:"));
        left.add(searchField);
        left.add(searchButton);
        left.add(clearButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton addButton = UIUtil.primaryButton("Add Student");
        JButton updateButton = UIUtil.secondaryButton("Update Selected");
        JButton deleteButton = UIUtil.dangerButton("Delete Selected");
        right.add(addButton);
        right.add(updateButton);
        right.add(deleteButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        searchButton.addActionListener(e -> loadStudents());
        searchField.addActionListener(e -> loadStudents());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadStudents();
        });
        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        return toolbar;
    }

    /** Reloads the student table from MySQL (uses the current search text). */
    public void refreshData() {
        loadStudents();
    }

    private void loadStudents() {
        try {
            currentStudents = studentDAO.searchStudents(searchField.getText());
        } catch (HostelException e) {
            UIUtil.showError(this, e.getMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (Student s : currentStudents) {
            tableModel.addRow(new Object[] {
                s.getStudentId(), s.getEnrollmentNo(), s.getName(), s.getGender(), s.getCourse(),
                s.getYear(), s.getPhone(), s.getEmail() == null ? "" : s.getEmail()
            });
        }
        countLabel.setText("Students shown: " + currentStudents.size());
    }

    /** Returns the student selected in the table, or null (after showing a message). */
    private Student getSelectedStudent() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UIUtil.showWarning(this, "Please select a student from the table first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return currentStudents.get(modelRow);
    }

    private void addStudent() {
        StudentDialog dialog = new StudentDialog(SwingUtilities.getWindowAncestor(this), studentDAO, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadStudents();
        }
    }

    private void updateStudent() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            return;
        }
        StudentDialog dialog = new StudentDialog(SwingUtilities.getWindowAncestor(this), studentDAO, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadStudents();
        }
    }

    private void deleteStudent() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            return;
        }
        String question = "Delete student " + selected.getName() + " (" + selected.getEnrollmentNo() + ")?\n"
                + "Any old (vacated) allocation history of this student will also be removed.\n"
                + "This cannot be undone.";
        if (!UIUtil.confirm(this, question)) {
            return;
        }
        try {
            studentDAO.deleteStudent(selected.getStudentId());
            UIUtil.showInfo(this, "Student deleted successfully.");
            loadStudents();
        } catch (HostelException e) {
            UIUtil.showWarning(this, e.getMessage());
        }
    }

    // =====================================================================
    //  Add / Update dialog
    // =====================================================================
    private static class StudentDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        private final StudentDAO dao;
        private final Student existing; // null = adding a new student
        private boolean saved = false;

        private final JTextField enrollmentField = UIUtil.createTextField(24);
        private final JTextField nameField = UIUtil.createTextField(24);
        private final JComboBox<String> genderCombo = new JComboBox<>(new String[] {"Male", "Female", "Other"});
        private final JTextField courseField = UIUtil.createTextField(24);
        private final JComboBox<Integer> yearCombo = new JComboBox<>(new Integer[] {1, 2, 3, 4, 5, 6});
        private final JTextField phoneField = UIUtil.createTextField(24);
        private final JTextField emailField = UIUtil.createTextField(24);
        private final JTextField addressField = UIUtil.createTextField(24);

        StudentDialog(Window owner, StudentDAO dao, Student existing) {
            super(owner, existing == null ? "Add Student" : "Update Student", Dialog.ModalityType.APPLICATION_MODAL);
            this.dao = dao;
            this.existing = existing;
            buildForm();
            if (existing != null) {
                fillForm(existing);
            }
            pack();
            setMinimumSize(new Dimension(460, getHeight()));
            setLocationRelativeTo(owner);
        }

        boolean isSaved() {
            return saved;
        }

        private void buildForm() {
            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(UIUtil.SURFACE);
            form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
            UIUtil.addFormRow(form, 0, "Enrollment No *", enrollmentField);
            UIUtil.addFormRow(form, 1, "Name *", nameField);
            UIUtil.addFormRow(form, 2, "Gender *", genderCombo);
            UIUtil.addFormRow(form, 3, "Course *", courseField);
            UIUtil.addFormRow(form, 4, "Year *", yearCombo);
            UIUtil.addFormRow(form, 5, "Phone *", phoneField);
            UIUtil.addFormRow(form, 6, "Email", emailField);
            UIUtil.addFormRow(form, 7, "Address", addressField);

            JButton saveButton = UIUtil.primaryButton(existing == null ? "Add Student" : "Save Changes");
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
            note.add(UIUtil.mutedLabel("* required field"), BorderLayout.WEST);

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

        private void fillForm(Student s) {
            enrollmentField.setText(s.getEnrollmentNo());
            nameField.setText(s.getName());
            genderCombo.setSelectedItem(s.getGender());
            courseField.setText(s.getCourse());
            yearCombo.setSelectedItem(s.getYear());
            phoneField.setText(s.getPhone());
            emailField.setText(s.getEmail() == null ? "" : s.getEmail());
            addressField.setText(s.getAddress() == null ? "" : s.getAddress());
        }

        /** Validates the form, then inserts/updates the student in MySQL. */
        private void save() {
            String enrollment = enrollmentField.getText().trim();
            String name = nameField.getText().trim();
            String course = courseField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();

            if (ValidationUtil.isBlank(enrollment)) {
                fail("Please enter the enrollment number.", enrollmentField);
                return;
            }
            if (!ValidationUtil.isValidEnrollmentNo(enrollment)) {
                fail("Enrollment number must be 3 to 30 characters: letters, digits, '-', '_' or '/' only.",
                        enrollmentField);
                return;
            }
            if (ValidationUtil.isBlank(name)) {
                fail("Please enter the student name.", nameField);
                return;
            }
            if (!ValidationUtil.isValidName(name)) {
                fail("Name may contain only letters, spaces, '.', ''' and '-'.", nameField);
                return;
            }
            if (ValidationUtil.isBlank(course)) {
                fail("Please enter the course.", courseField);
                return;
            }
            if (course.length() > 50) {
                fail("Course name is too long (maximum 50 characters).", courseField);
                return;
            }
            if (ValidationUtil.isBlank(phone)) {
                fail("Please enter the phone number.", phoneField);
                return;
            }
            if (!ValidationUtil.isValidPhone(phone)) {
                fail("Please enter a valid phone number (10 to 13 digits, optional leading +).", phoneField);
                return;
            }
            if (!email.isEmpty() && (!ValidationUtil.isValidEmail(email) || email.length() > 100)) {
                fail("Please enter a valid email address.", emailField);
                return;
            }
            if (address.length() > 200) {
                fail("Address is too long (maximum 200 characters).", addressField);
                return;
            }

            Student s = new Student();
            if (existing != null) {
                s.setStudentId(existing.getStudentId());
            }
            s.setEnrollmentNo(enrollment);
            s.setName(name);
            s.setGender((String) genderCombo.getSelectedItem());
            s.setCourse(course);
            s.setYear((Integer) yearCombo.getSelectedItem());
            s.setPhone(phone);
            s.setEmail(email);
            s.setAddress(address);

            try {
                if (existing == null) {
                    dao.addStudent(s);
                    UIUtil.showInfo(this, "Student added successfully.");
                } else {
                    dao.updateStudent(s);
                    UIUtil.showInfo(this, "Student updated successfully.");
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
