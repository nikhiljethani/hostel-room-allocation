package model;

/** Summary numbers shown on the Dashboard and in Reports (all read from MySQL). */
public class HostelStats {

    private final int totalStudents;
    private final int totalRooms;
    private final int totalCapacity;
    private final int occupiedBeds;
    private final int availableBeds;
    private final int fullRooms;
    private final int emptyRooms;
    private final int activeAllocations;
    private final int vacatedAllocations;

    public HostelStats(int totalStudents, int totalRooms, int totalCapacity, int occupiedBeds,
            int availableBeds, int fullRooms, int emptyRooms, int activeAllocations, int vacatedAllocations) {
        this.totalStudents = totalStudents;
        this.totalRooms = totalRooms;
        this.totalCapacity = totalCapacity;
        this.occupiedBeds = occupiedBeds;
        this.availableBeds = availableBeds;
        this.fullRooms = fullRooms;
        this.emptyRooms = emptyRooms;
        this.activeAllocations = activeAllocations;
        this.vacatedAllocations = vacatedAllocations;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getOccupiedBeds() {
        return occupiedBeds;
    }

    public int getAvailableBeds() {
        return availableBeds;
    }

    public int getFullRooms() {
        return fullRooms;
    }

    public int getEmptyRooms() {
        return emptyRooms;
    }

    public int getActiveAllocations() {
        return activeAllocations;
    }

    public int getVacatedAllocations() {
        return vacatedAllocations;
    }
}
