# Hostel Room Allocation System

**Advanced Java Programming Lab - Mini Project**
Java Swing desktop application using JDBC and MySQL.

## 1. Project description
A desktop application that maintains student and hostel room information and allocates
available rooms to students. All data is stored permanently in MySQL (`hostel_db`).
Room occupancy, available beds and room status are **never stored**; they are calculated by SQL
from the active allocation records every time they are shown.

## 2. Objective
Replace manual hostel registers with a centralized digital system that
prevents wrong allocations and always shows correct room availability.

## 3. Features
- Admin login (checked against the `admins` table, password stored as SHA-256 hash)
- Dashboard with live statistics, recent allocations and room occupancy summary
- Student management: add, update, delete, search
- Room management: add, update, delete, search, filter by status (All / Available / Full / Empty)
- Room allocation with only rooms that still have a free bed in the dropdown
- Vacate room (status becomes VACATED, vacate date stored, history is kept)
- Allocation records / history with search
- Reports: totals, full/empty rooms, active/vacated allocations, room occupancy table
- Logout back to the login screen

### Business rules enforced
1. A student cannot have more than one ACTIVE allocation.
2. A FULL room cannot receive another student.
3. Only rooms with free beds appear in the allocation dropdown.
4. Enrollment number and room number are unique.
5. Required fields, phone, email, capacity and date are validated.
6. A VACATED allocation cannot be vacated again.
7. A student or room with an ACTIVE allocation cannot be deleted.
8. Room capacity cannot be reduced below the number of current occupants.
9. Allocation and vacation run in JDBC transactions (commit / rollback).

## 4. Technology stack
Java (JDK 26), Swing/AWT, JDBC, MySQL Server 8.4.x, MySQL Connector/J,
Apache NetBeans 31, MySQL Workbench. No other libraries or frameworks.

## 5. System requirements
- JDK 26 installed and configured in NetBeans
- Apache NetBeans IDE 31
- MySQL Server 8.4.x running on `localhost:3306`
- MySQL Workbench
- MySQL Connector/J JAR (`mysql-connector-j-<version>.jar`)

## 6. Project structure
```
HostelRoomAllocationSystem/
|-- database/hostel_management.sql
|-- README.md
`-- src/
    |-- Main.java
    |-- db/DatabaseConnection.java
    |-- model/   Admin, Student, Room, Allocation, HostelStats
    |-- dao/     AdminDAO, StudentDAO, RoomDAO, AllocationDAO, ReportDAO
    |-- ui/      LoginFrame, DashboardFrame, DashboardPanel, StudentPanel,
    |            RoomPanel, AllocationPanel, ReportsPanel
    `-- util/    ValidationUtil, UIUtil, HostelException
```
Packages sit directly under `src/` because that is the source root of a NetBeans
"Java with Ant" project (so `src/main/java` is not used).

Architecture: **Swing UI -> DAO -> JDBC -> MySQL**. UI classes contain no SQL.

## 7. Database setup (MySQL Workbench)
1. Open MySQL Workbench and connect to your local server (user `root`).
2. `File -> Open SQL Script...` and choose `database/hostel_management.sql`.
3. Click the lightning-bolt icon (Execute) to run the whole script.
   The last statement shows a room occupancy table in the result grid.
4. Verify: in the Navigator press Refresh. `hostel_db` must show the tables
   `admins`, `students`, `rooms`, `allocations`. You can also run:
   ```sql
   USE hostel_db;
   SELECT COUNT(*) FROM students;      -- 10
   SELECT COUNT(*) FROM rooms;         -- 6
   SELECT COUNT(*) FROM allocations;   -- 7
   ```
Note: the script drops and re-creates the tables, so running it again resets the demo data.

### Demo data loaded
| Room | Capacity | Active | Status |
|------|----------|--------|--------|
| 101 (Block A) | 4 | 3 | AVAILABLE (1 free) |
| 102 (Block A) | 2 | 2 | FULL |
| 103 (Block A) | 2 | 0 | EMPTY (has one VACATED record) |
| 201 (Block B) | 3 | 1 | AVAILABLE (2 free) |
| 202 (Block B) | 1 | 0 | EMPTY |
| 203 (Block B) | 4 | 0 | EMPTY |

Students 8, 9 and 10 (Sneha Agarwal, Rahul Meena, Pooja Choudhary) and student 7 have no active room.

## 8. NetBeans setup
1. Unzip the project somewhere (e.g. `D:\Projects\HostelRoomAllocationSystem`).
2. In NetBeans: `File -> New Project -> Java with Ant -> Java Application -> Next`.
3. Project Name: `HostelRoomAllocationSystem`. **Untick "Create Main Class"**. Finish.
4. Open the new project's `src` folder in File Explorer and copy into it
   `Main.java` and the folders `db`, `model`, `dao`, `ui`, `util` from the unzipped project.
5. Back in NetBeans the Projects tab shows them under *Source Packages*
   (if not, switch to the Files tab or close and reopen the project).
6. Check the JDK: right-click project -> `Properties -> Libraries -> Java Platform` = JDK 26,
   and `Sources -> Source/Binary Format` = JDK 26 (any 17+ also works).

## 9. Connector/J setup
1. Extract the downloaded Connector/J ZIP. Inside is `mysql-connector-j-<version>.jar`
   (use this one, not the `-sources` file).
2. In NetBeans: right-click the project -> **Properties -> Libraries**.
3. On the **Compile** tab (Classpath) click **Add JAR/Folder** and select that JAR. Click OK.
4. The JAR now appears under the project's *Libraries* node.

## 10. Configure the database password
Open `src/db/DatabaseConnection.java` and change:
```java
private static final String DB_PASSWORD = "your_mysql_password";
```
to your own MySQL root password. (Host, port, database and user are in the same block.)

## 11. How to run
1. Set the main class once: `Properties -> Run -> Main Class` = `Main`.
2. Press **F6** (Run Project). The Login screen opens.

## 12. Default login
| Username | Password |
|----------|----------|
| `admin` | `admin123` |

To change it: `UPDATE admins SET password = SHA2('newpassword', 256) WHERE username = 'admin';`

## 13. Database schema overview
- `admins(admin_id PK, username UNIQUE, password)`
- `students(student_id PK, enrollment_no UNIQUE, name, gender, course, year, phone, email, address)`
- `rooms(room_id PK, room_number UNIQUE, block, floor, room_type, capacity CHECK > 0)`
- `allocations(allocation_id PK, student_id FK, room_id FK, allocation_date, vacate_date, status)`
  - `status` is `ACTIVE` or `VACATED` (CHECK constraint), `ACTIVE` rows have no vacate date
  - foreign keys: `ON UPDATE CASCADE`, `ON DELETE CASCADE`

Calculated for every room (RoomDAO.ROOM_SUMMARY_SQL):
```
occupied  = COUNT of allocations of the room with status = 'ACTIVE'
available = capacity - occupied
status    = EMPTY (occupied = 0) | AVAILABLE (0 < occupied < capacity) | FULL (occupied >= capacity)
```

## 14. Application modules
| Module | Class |
|--------|-------|
| Login / Logout | `LoginFrame`, `DashboardFrame` |
| Dashboard | `DashboardPanel` |
| Student Management | `StudentPanel` |
| Room Management | `RoomPanel` |
| Room Allocation + Records / Vacate | `AllocationPanel` |
| Reports | `ReportsPanel` |

Every page reloads its data from MySQL whenever it is opened and after each of its own
operations, so values on screen are never stale.

## 15. Testing checklist
- [ ] Login with `admin` / `admin123` works; wrong password shows an error
- [ ] Dashboard: 10 students, 6 rooms, capacity 16, occupied 6, available 10 (with the demo data)
- [ ] Add a student -> appears in the table immediately; Dashboard total increases
- [ ] Add a student with an existing enrollment number -> "Enrollment number already exists."
- [ ] Invalid phone / email / empty required field -> clear message
- [ ] Update and delete a student; deleting a student with an active room is refused
- [ ] Add a room; duplicate room number is refused; capacity 0 or text is refused
- [ ] Room filter: Available / Full / Empty / All and the search box
- [ ] Allocate a student; room Occupied/Available/Status change on the Rooms page
- [ ] Allocating a student who already has an active room is refused
- [ ] A room that became FULL disappears from the allocation room dropdown
- [ ] Vacate -> status VACATED, vacate date shown, room becomes selectable again
- [ ] Vacating an already VACATED record is refused
- [ ] Reports page numbers match the Dashboard
- [ ] Close and reopen the application: all data is still there
- [ ] Logout returns to the Login screen

## 16. Live demo sequence
1. Login (`admin` / `admin123`).
2. Dashboard: show students, rooms, capacity, occupied and available beds.
3. Students -> Add Student (new enrollment no) -> it appears in the table.
4. Rooms: Room 101 shows Capacity 4, Occupied 3, Available 1, AVAILABLE.
5. Allocations: choose the new student, choose room `101 - Block A - Available: 1/4`,
   click Allocate Room.
6. Rooms: Room 101 now shows Occupied 4, Available 0, **FULL**.
7. Allocations: Room 101 is no longer in the room dropdown (full rooms are hidden).
   Pick any student who already has an active room and a free room, and click Allocate Room:
   "Student already has an active room allocation." is shown.
   (The check "Room 101 is already full." is also enforced inside the allocation transaction,
   for example if another user filled the room after the dropdown was loaded.)
8. Allocation records: select an ACTIVE record of Room 101 -> Vacate Room -> Yes.
9. Rooms: Room 101 is back to Occupied 3, Available 1, **AVAILABLE**.
10. Dashboard: all figures have updated.

## 17. Common errors and fixes
| Problem | Fix |
|---------|-----|
| "MySQL Connector/J library was not found" | Add the Connector/J JAR in Properties -> Libraries -> Compile (section 9) |
| "MySQL rejected the username or password" | Set `DB_PASSWORD` in `DatabaseConnection.java` |
| "Database 'hostel_db' was not found" | Run `hostel_management.sql` in MySQL Workbench |
| "Cannot connect to the MySQL database" | Start MySQL Server (Windows Services -> MySQL84) and check port 3306 |
| Login says invalid credentials | Re-run the SQL script; login is `admin` / `admin123` |
| "Could not find or load main class Main" | Properties -> Run -> Main Class = `Main`; make sure `Main.java` is directly in `src` |
| Red errors on `import ui...` | The folders were not copied into the project's `src` folder (section 8, step 4) |
| Window text looks different from screenshots | The application uses the Segoe UI font (Windows); other systems fall back to a default font |

## 18. For the presentation
- **Innovation:** automatic room occupancy and availability management based on active allocation records.
- **Other contributions:** centralized hostel data, prevention of full-room allocation, prevention of duplicate
  active allocation, automatic availability updates, digital allocation history, reduced manual record keeping.
- **SDG 11 - Sustainable Cities and Communities:** the system supports efficient digital management of hostel
  accommodation by maintaining centralized occupancy records, improving utilization of existing hostel capacity,
  and reducing manual record keeping.
- **Future scope:** fee/payment tracking, room change requests, printable reports, role-based users.
