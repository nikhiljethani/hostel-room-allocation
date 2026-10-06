package model;

/** A logged-in administrator (the password is never kept in memory). */
public class Admin {

    private int adminId;
    private String username;

    public Admin() {
    }

    public Admin(int adminId, String username) {
        this.adminId = adminId;
        this.username = username;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
