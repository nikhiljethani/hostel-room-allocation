package dao;

import db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Admin;
import util.HostelException;

/** Database access for the admins table (login). */
public class AdminDAO {

    /**
     * Checks the username and password against the admins table.
     * The password is compared as a SHA-256 hash, exactly as stored by the SQL script.
     *
     * @return the Admin if the credentials are correct, otherwise null
     */
    public Admin login(String username, String password) throws HostelException {
        String sql = "SELECT admin_id, username FROM admins WHERE username = ? AND `password` = SHA2(?, 256)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("[AdminDAO] Login successful for user: " + username);
                    return new Admin(rs.getInt("admin_id"), rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            throw DatabaseConnection.wrap(e, "log in");
        }
        System.out.println("[AdminDAO] Login failed for user: " + username);
        return null;
    }
}
