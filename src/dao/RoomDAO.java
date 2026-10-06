package dao;

import db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Room;
import util.HostelException;

/**
 * Database access for the rooms table.
 *
 * Occupied / available / status are NEVER stored. They are calculated by
 * ROOM_SUMMARY_SQL from the ACTIVE rows of the allocations table:
 *   occupied  = number of ACTIVE allocations of the room
 *   available = capacity - occupied
 *   status    = EMPTY (0 occupied), AVAILABLE (some beds free), FULL (no beds free)
 */
public class RoomDAO {

    /** The single query that defines occupancy. It is reused by the dashboard and reports. */
    public static final String ROOM_SUMMARY_SQL =
            "SELECT r.room_id, r.room_number, r.block, r.floor, r.room_type, r.capacity, "
            + "COUNT(a.allocation_id) AS occupied, "
            + "r.capacity - COUNT(a.allocation_id) AS available, "
            + "CASE WHEN COUNT(a.allocation_id) = 0 THEN 'EMPTY' "
            + "WHEN COUNT(a.allocation_id) < r.capacity THEN 'AVAILABLE' "
            + "ELSE 'FULL' END AS status "
            + "FROM rooms r "
            + "LEFT JOIN allocations a ON a.room_id = r.room_id AND a.status = 'ACTIVE' "
            + "GROUP BY r.room_id, r.room_number, r.block, r.floor, r.room_type, r.capacity";

    /**
     * Returns rooms with calculated occupancy.
     *
     * @param statusFilter ALL, AVAILABLE, FULL or EMPTY (case-insensitive)
     * @param keyword      text searched in room number, block and room type ("" = no search)
     */
    public List<Room> getRooms(String statusFilter, String keyword) throws HostelException {
        String sql = "SELECT * FROM (" + ROOM_SUMMARY_SQL + ") rs "
                + "WHERE (? = 'ALL' OR rs.status = ?) "
                + "AND (rs.room_number LIKE ? OR rs.block LIKE ? OR rs.room_type LIKE ?) "
                + "ORDER BY rs.room_number";
        String filter = (statusFilter == null ? "ALL" : statusFilter.trim().toUpperCase());
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";

        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filter);
            ps.setString(2, filter);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "load rooms");
        }
        System.out.println("[RoomDAO] Loaded " + rooms.size() + " room(s), filter=" + filter + ".");
        return rooms;
    }

    /** Only rooms that still have at least one free bed (used by the allocation dropdown). */
    public List<Room> getAvailableRooms() throws HostelException {
        String sql = "SELECT * FROM (" + ROOM_SUMMARY_SQL + ") rs WHERE rs.available > 0 ORDER BY rs.room_number";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "load available rooms");
        }
        System.out.println("[RoomDAO] Rooms with free beds: " + rooms.size());
        return rooms;
    }

    public void addRoom(Room room) throws HostelException {
        String sql = "INSERT INTO rooms (room_number, block, `floor`, room_type, capacity) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getBlock());
            ps.setInt(3, room.getFloor());
            ps.setString(4, room.getRoomType());
            ps.setInt(5, room.getCapacity());
            ps.executeUpdate();
            System.out.println("[RoomDAO] Room added: " + room.getRoomNumber());
        } catch (SQLException e) {
            if (DatabaseConnection.isDuplicateKey(e)) {
                throw new HostelException("Room number " + room.getRoomNumber() + " already exists.");
            }
            throw DatabaseConnection.wrap(e, "add the room");
        }
    }

    /** Updates a room. The capacity cannot be reduced below the number of active occupants. */
    public void updateRoom(Room room) throws HostelException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getTransactionConnection();

            // Lock the room row so no allocation can happen while we check the capacity.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT room_id FROM rooms WHERE room_id = ? FOR UPDATE")) {
                ps.setInt(1, room.getRoomId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new HostelException("This room record no longer exists.");
                    }
                }
            }

            int occupied = countActive(conn, room.getRoomId());
            if (room.getCapacity() < occupied) {
                throw new HostelException("Capacity cannot be less than the number of students currently allocated ("
                        + occupied + ").");
            }

            String sql = "UPDATE rooms SET room_number = ?, block = ?, `floor` = ?, room_type = ?, capacity = ? "
                    + "WHERE room_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, room.getRoomNumber());
                ps.setString(2, room.getBlock());
                ps.setInt(3, room.getFloor());
                ps.setString(4, room.getRoomType());
                ps.setInt(5, room.getCapacity());
                ps.setInt(6, room.getRoomId());
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println("[RoomDAO] Room updated: " + room.getRoomNumber());
        } catch (HostelException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            if (DatabaseConnection.isDuplicateKey(e)) {
                throw new HostelException("Room number " + room.getRoomNumber() + " already exists.");
            }
            throw DatabaseConnection.wrap(e, "update the room");
        } finally {
            DatabaseConnection.close(conn);
        }
    }

    /**
     * Deletes a room. A room with ACTIVE allocations cannot be deleted.
     * Old VACATED history rows of that room are removed with the room
     * (ON DELETE CASCADE in the database).
     */
    public void deleteRoom(int roomId) throws HostelException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getTransactionConnection();

            String roomNumber;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT room_number FROM rooms WHERE room_id = ? FOR UPDATE")) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new HostelException("This room record no longer exists.");
                    }
                    roomNumber = rs.getString("room_number");
                }
            }

            int occupied = countActive(conn, roomId);
            if (occupied > 0) {
                throw new HostelException("Room " + roomNumber + " has " + occupied
                        + " active allocation(s).\nVacate all students first, then delete the room.");
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE room_id = ?")) {
                ps.setInt(1, roomId);
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println("[RoomDAO] Room deleted: " + roomNumber);
        } catch (HostelException e) {
            DatabaseConnection.rollback(conn);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw DatabaseConnection.wrap(e, "delete the room");
        } finally {
            DatabaseConnection.close(conn);
        }
    }

    private int countActive(Connection conn, int roomId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM allocations WHERE room_id = ? AND status = 'ACTIVE'")) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setRoomId(rs.getInt("room_id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setBlock(rs.getString("block"));
        room.setFloor(rs.getInt("floor"));
        room.setRoomType(rs.getString("room_type"));
        room.setCapacity(rs.getInt("capacity"));
        room.setOccupied(rs.getInt("occupied"));
        room.setAvailable(rs.getInt("available"));
        room.setStatus(rs.getString("status"));
        return room;
    }
}
