package dao;

import db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.HostelStats;
import util.HostelException;

/** Summary statistics for the Dashboard and Reports screens. Everything comes from SQL. */
public class ReportDAO {

    public HostelStats getStatistics() throws HostelException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int totalStudents = queryCount(conn, "SELECT COUNT(*) FROM students");
            int totalRooms = queryCount(conn, "SELECT COUNT(*) FROM rooms");
            int totalCapacity = queryCount(conn, "SELECT COALESCE(SUM(capacity), 0) FROM rooms");
            int activeAllocations = queryCount(conn,
                    "SELECT COUNT(*) FROM allocations WHERE status = 'ACTIVE'");
            int vacatedAllocations = queryCount(conn,
                    "SELECT COUNT(*) FROM allocations WHERE status = 'VACATED'");
            int fullRooms = queryCount(conn,
                    "SELECT COUNT(*) FROM (" + RoomDAO.ROOM_SUMMARY_SQL + ") rs WHERE rs.status = 'FULL'");
            int emptyRooms = queryCount(conn,
                    "SELECT COUNT(*) FROM (" + RoomDAO.ROOM_SUMMARY_SQL + ") rs WHERE rs.status = 'EMPTY'");

            // Occupied beds = active allocations; available beds = capacity - occupied.
            int occupiedBeds = activeAllocations;
            int availableBeds = totalCapacity - occupiedBeds;

            System.out.println("[ReportDAO] Statistics loaded: students=" + totalStudents + ", rooms=" + totalRooms
                    + ", capacity=" + totalCapacity + ", occupied=" + occupiedBeds + ", available=" + availableBeds);
            return new HostelStats(totalStudents, totalRooms, totalCapacity, occupiedBeds, availableBeds,
                    fullRooms, emptyRooms, activeAllocations, vacatedAllocations);
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "load statistics");
        }
    }

    private int queryCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
