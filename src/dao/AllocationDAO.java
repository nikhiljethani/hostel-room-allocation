package dao;

import db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Allocation;
import util.HostelException;

/** Database access for the allocations table (allocate, vacate, history). */
public class AllocationDAO {

    private static final String SELECT_ALLOCATIONS =
            "SELECT a.allocation_id, a.student_id, a.room_id, s.name, s.enrollment_no, "
            + "r.room_number, r.block, a.allocation_date, a.vacate_date, a.status "
            + "FROM allocations a "
            + "JOIN students s ON s.student_id = a.student_id "
            + "JOIN rooms r ON r.room_id = a.room_id ";

    /**
     * Allocates a room to a student inside ONE transaction:
     * lock room -> lock student -> check student has no ACTIVE allocation
     * -> check room still has a free bed -> insert allocation -> commit.
     * Any failure rolls everything back.
     */
    public void allocateRoom(int studentId, int roomId, LocalDate allocationDate) throws HostelException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getTransactionConnection();

            // 1. Lock the room row (always room first, then student, to avoid deadlocks).
            String roomNumber;
            int capacity;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT room_number, capacity FROM rooms WHERE room_id = ? FOR UPDATE")) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new HostelException("The selected room no longer exists.");
                    }
                    roomNumber = rs.getString("room_number");
                    capacity = rs.getInt("capacity");
                }
            }

            // 2. Lock the student row.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT student_id FROM students WHERE student_id = ? FOR UPDATE")) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new HostelException("The selected student no longer exists.");
                    }
                }
            }

            // 3. Rule: a student can have only one ACTIVE allocation.
            int studentActive = count(conn,
                    "SELECT COUNT(*) FROM allocations WHERE student_id = ? AND status = 'ACTIVE'", studentId);
            if (studentActive > 0) {
                throw new HostelException("Student already has an active room allocation.");
            }

            // 4. Rule: a full room cannot receive another student.
            int occupied = count(conn,
                    "SELECT COUNT(*) FROM allocations WHERE room_id = ? AND status = 'ACTIVE'", roomId);
            if (occupied >= capacity) {
                throw new HostelException("Room " + roomNumber + " is already full.");
            }

            // 5. Insert the allocation.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO allocations (student_id, room_id, allocation_date, vacate_date, status) "
                    + "VALUES (?, ?, ?, NULL, 'ACTIVE')")) {
                ps.setInt(1, studentId);
                ps.setInt(2, roomId);
                ps.setObject(3, allocationDate);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[AllocationDAO] Student " + studentId + " allocated to room " + roomNumber
                    + " (occupied now " + (occupied + 1) + "/" + capacity + ").");
        } catch (HostelException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw DatabaseConnection.wrap(e, "allocate the room");
        } finally {
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Vacates an allocation: status becomes VACATED and vacate_date is stored.
     * The row is kept as history. An already VACATED allocation is rejected.
     */
    public void vacateAllocation(int allocationId, LocalDate vacateDate) throws HostelException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getTransactionConnection();

            String status;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM allocations WHERE allocation_id = ? FOR UPDATE")) {
                ps.setInt(1, allocationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new HostelException("This allocation record no longer exists.");
                    }
                    status = rs.getString("status");
                }
            }
            if (!Allocation.STATUS_ACTIVE.equals(status)) {
                throw new HostelException("This allocation has already been vacated.");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE allocations SET status = 'VACATED', vacate_date = ? WHERE allocation_id = ?")) {
                ps.setObject(1, vacateDate);
                ps.setInt(2, allocationId);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[AllocationDAO] Allocation " + allocationId + " vacated on " + vacateDate + ".");
        } catch (HostelException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw DatabaseConnection.wrap(e, "vacate the room");
        } finally {
            DatabaseConnection.close(conn);
        }
    }

    /** All allocation records (ACTIVE and VACATED) matching the keyword, newest first. */
    public List<Allocation> searchAllocations(String keyword) throws HostelException {
        String sql = SELECT_ALLOCATIONS
                + "WHERE s.name LIKE ? OR s.enrollment_no LIKE ? OR r.room_number LIKE ? "
                + "OR r.block LIKE ? OR a.status LIKE ? "
                + "ORDER BY a.allocation_id DESC";
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Allocation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "load allocation records");
        }
        System.out.println("[AllocationDAO] Loaded " + list.size() + " allocation record(s).");
        return list;
    }

    /** The most recent allocations (used on the dashboard). */
    public List<Allocation> getRecentAllocations(int limit) throws HostelException {
        String sql = SELECT_ALLOCATIONS + "ORDER BY a.allocation_id DESC LIMIT ?";
        List<Allocation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "load recent allocations");
        }
        return list;
    }

    private int count(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Allocation mapRow(ResultSet rs) throws SQLException {
        Allocation a = new Allocation();
        a.setAllocationId(rs.getInt("allocation_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setRoomId(rs.getInt("room_id"));
        a.setStudentName(rs.getString("name"));
        a.setEnrollmentNo(rs.getString("enrollment_no"));
        a.setRoomNumber(rs.getString("room_number"));
        a.setBlock(rs.getString("block"));
        a.setAllocationDate(rs.getObject("allocation_date", LocalDate.class));
        a.setVacateDate(rs.getObject("vacate_date", LocalDate.class));
        a.setStatus(rs.getString("status"));
        return a;
    }
}
