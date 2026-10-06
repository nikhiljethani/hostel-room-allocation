package model;

/**
 * A room together with its CALCULATED occupancy values.
 * occupied / available / status are never stored in the database;
 * they are filled in by the room summary query in RoomDAO.
 */
public class Room {

    public static final String STATUS_EMPTY = "EMPTY";
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_FULL = "FULL";

    private int roomId;
    private String roomNumber;
    private String block;
    private int floor;
    private String roomType;
    private int capacity;
    private int occupied;
    private int available;
    private String status;

    public Room() {
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = occupied;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** Text shown in the allocation room dropdown, e.g. "101 - Block A - Available: 2/4". */
    @Override
    public String toString() {
        return roomNumber + " - Block " + block + " - Available: " + available + "/" + capacity;
    }
}
