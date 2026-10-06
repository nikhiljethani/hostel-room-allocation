package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import util.HostelException;

/**
 * Central place for creating JDBC connections to MySQL and for turning
 * raw SQLExceptions into friendly messages.
 */
public final class DatabaseConnection {

    // =====================================================================
    //  CONFIGURATION  -  change these values to match your MySQL setup
    // =====================================================================
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "hostel_db";
    private static final String DB_USER = "root";

    // >>>>>>>>>>  PUT YOUR MYSQL ROOT PASSWORD HERE  <<<<<<<<<<
    private static final String DB_PASSWORD = "root";
    // =====================================================================

    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?allowPublicKeyRetrieval=true";

    private DatabaseConnection() {
    }

    /** Opens a new connection (use with try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Opens a connection prepared for a manual transaction:
     * auto-commit is OFF and every plain SELECT sees the latest committed data.
     */
    public static Connection getTransactionConnection() throws SQLException {
        Connection conn = getConnection();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        conn.setAutoCommit(false);
        return conn;
    }

    /** Rolls back quietly (safe to call with null). */
    public static void rollback(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
            System.out.println("[DB] Transaction rolled back.");
        } catch (SQLException e) {
            System.err.println("[DB] Rollback failed: " + e.getMessage());
        }
    }

    /** Restores auto-commit and closes the connection quietly (safe with null). */
    public static void close(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            // ignore - the connection is being closed anyway
        }
        try {
            conn.close();
        } catch (SQLException e) {
            System.err.println("[DB] Could not close connection: " + e.getMessage());
        }
    }

    /** MySQL error 1062 = duplicate entry for a UNIQUE key. */
    public static boolean isDuplicateKey(SQLException e) {
        return e.getErrorCode() == 1062;
    }

    /**
     * Logs the technical details to the console and returns an exception
     * with a message that is suitable for the user.
     */
    public static HostelException wrap(SQLException e, String action) {
        System.err.println("[DB ERROR] Failed to " + action + ": " + e.getMessage()
                + " (SQLState=" + e.getSQLState() + ", errorCode=" + e.getErrorCode() + ")");

        String text = e.getMessage() == null ? "" : e.getMessage();
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        int code = e.getErrorCode();

        if (text.contains("No suitable driver")) {
            return new HostelException("MySQL Connector/J library was not found.\n"
                    + "Add the mysql-connector-j JAR to the project Libraries and run again.");
        }
        if (code == 1045) {
            return new HostelException("MySQL rejected the username or password.\n"
                    + "Set the correct password in db/DatabaseConnection.java (DB_PASSWORD).");
        }
        if (code == 1049) {
            return new HostelException("Database 'hostel_db' was not found.\n"
                    + "Run database/hostel_management.sql in MySQL Workbench first.");
        }
        if (state.startsWith("08")) {
            return new HostelException("Cannot connect to the MySQL database.\n"
                    + "Make sure MySQL Server is running on localhost:3306.");
        }
        if (code == 1451 || code == 1452) {
            return new HostelException("This operation conflicts with related records in the database.");
        }
        if (code == 3819 || code == 4025) {
            return new HostelException("The entered data is not allowed by the database rules. Please check the values.");
        }
        return new HostelException("A database error occurred while trying to " + action + ". Please try again.");
    }
}
