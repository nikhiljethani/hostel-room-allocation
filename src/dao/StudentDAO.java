package dao;

import db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Student;
import util.HostelException;

/** Database access for the students table. */
public class StudentDAO {

    private static final String SELECT_STUDENTS =
            "SELECT student_id, enrollment_no, name, gender, course, `year`, phone, email, address FROM students ";

    /** Returns all students whose enrollment no, name, course or phone contains the keyword. */
    public List<Student> searchStudents(String keyword) throws HostelException {
        String sql = SELECT_STUDENTS
                + "WHERE enrollment_no LIKE ? OR name LIKE ? OR course LIKE ? OR phone LIKE ? "
                + "ORDER BY student_id";
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Student> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "load students");
        }
        System.out.println("[StudentDAO] Loaded " + students.size() + " student(s) for search '" + keyword + "'.");
        return students;
    }

    /** Returns every student (used by the allocation dropdown). */
    public List<Student> getAllStudents() throws HostelException {
        return searchStudents("");
    }

    public void addStudent(Student s) throws HostelException {
        String sql = "INSERT INTO students (enrollment_no, name, gender, course, `year`, phone, email, address) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getEnrollmentNo());
            ps.setString(2, s.getName());
            ps.setString(3, s.getGender());
            ps.setString(4, s.getCourse());
            ps.setInt(5, s.getYear());
            ps.setString(6, s.getPhone());
            ps.setString(7, emptyToNull(s.getEmail()));
            ps.setString(8, emptyToNull(s.getAddress()));
            ps.executeUpdate();
            System.out.println("[StudentDAO] Student added: " + s.getEnrollmentNo());
        } catch (SQLException e) {
            if (DatabaseConnection.isDuplicateKey(e)) {
                throw new HostelException("Enrollment number already exists.");
            }
            throw DatabaseConnection.wrap(e, "add the student");
        }
    }

    public void updateStudent(Student s) throws HostelException {
        String sql = "UPDATE students SET enrollment_no = ?, name = ?, gender = ?, course = ?, `year` = ?, "
                + "phone = ?, email = ?, address = ? WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getEnrollmentNo());
            ps.setString(2, s.getName());
            ps.setString(3, s.getGender());
            ps.setString(4, s.getCourse());
            ps.setInt(5, s.getYear());
            ps.setString(6, s.getPhone());
            ps.setString(7, emptyToNull(s.getEmail()));
            ps.setString(8, emptyToNull(s.getAddress()));
            ps.setInt(9, s.getStudentId());
            if (ps.executeUpdate() == 0) {
                throw new HostelException("This student record no longer exists.");
            }
            System.out.println("[StudentDAO] Student updated: " + s.getEnrollmentNo());
        } catch (SQLException e) {
            if (DatabaseConnection.isDuplicateKey(e)) {
                throw new HostelException("Enrollment number already exists.");
            }
            throw DatabaseConnection.wrap(e, "update the student");
        }
    }

    /**
     * Deletes a student. A student with an ACTIVE allocation cannot be deleted.
     * Old VACATED history rows of that student are removed with the student
     * (ON DELETE CASCADE in the database).
     */
    public void deleteStudent(int studentId) throws HostelException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getTransactionConnection();

            // Lock the student row so nobody can allocate this student while we check.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT student_id FROM students WHERE student_id = ? FOR UPDATE")) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new HostelException("This student record no longer exists.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM allocations WHERE student_id = ? AND status = 'ACTIVE'")) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new HostelException("This student has an active room allocation.\n"
                                + "Vacate the room first, then delete the student.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println("[StudentDAO] Student deleted, id = " + studentId);
        } catch (HostelException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw DatabaseConnection.wrap(e, "delete the student");
        } finally {
            DatabaseConnection.close(conn);
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setEnrollmentNo(rs.getString("enrollment_no"));
        s.setName(rs.getString("name"));
        s.setGender(rs.getString("gender"));
        s.setCourse(rs.getString("course"));
        s.setYear(rs.getInt("year"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        return s;
    }

    private static String emptyToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
