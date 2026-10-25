package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import th.mfu.BookingRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.BookingStatus;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

/** Builds the room data for the 3D room map and decides which rooms can be booked. */
@Service
public class RoomMapService {

    // a rented room counts as "ใกล้หมดสัญญา" when its contract ends within this many days
    public static final int SOON_DAYS = 60;

    public static final String AVAILABLE = "AVAILABLE";
    public static final String SOON = "SOON";
    public static final String OCCUPIED = "OCCUPIED";
    public static final String MAINTENANCE = "MAINTENANCE";

    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final BookingRequestRepository bookingRepository;
    private final Clock clock;

    public RoomMapService(RoomRepository roomRepository, TenantRepository tenantRepository,
            BookingRequestRepository bookingRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /** All rooms for the map. forAdmin = true also includes tenant names. */
    public List<RoomView> rooms(boolean forAdmin) {
        LocalDate today = LocalDate.now(clock);

        Map<Long, List<Tenant>> tenantsByRoom = new HashMap<>();
        for (Tenant t : tenantRepository.findAllByOrderByFirstNameAsc()) {
            if (t.getRoom() != null) {
                tenantsByRoom.computeIfAbsent(t.getRoom().getId(), id -> new ArrayList<>()).add(t);
            }
        }
        Map<Long, Long> pendingByRoom = new HashMap<>();
        for (BookingRequest b : bookingRepository.findByStatus(BookingStatus.PENDING)) {
            if (b.getRoom() != null) {
                pendingByRoom.merge(b.getRoom().getId(), 1L, Long::sum);
            }
        }

        List<RoomView> result = new ArrayList<>();
        for (Room room : roomRepository.findAllByOrderByRoomNumberAsc()) {
            List<Tenant> tenants = tenantsByRoom.getOrDefault(room.getId(), new ArrayList<>());
            LocalDate contractEnd = contractEnd(tenants);
            String status = mapStatus(room.getStatus(), contractEnd, today);

            RoomView v = new RoomView();
            v.setId(room.getId());
            v.setNumber(room.getRoomNumber());
            v.setBuilding(room.getBuilding() == null || room.getBuilding().isBlank() ? "A" : room.getBuilding().trim().toUpperCase());
            v.setFloor(room.getFloor() == null ? 1 : room.getFloor());
            v.setType(room.getRoomType());
            v.setRent(room.getMonthlyRent() == null ? 0 : room.getMonthlyRent());
            v.setCapacity(room.getCapacity() == null ? 1 : room.getCapacity());
            v.setOccupants(tenants.size());
            v.setStatus(status);
            v.setStatusLabel(label(status));
            v.setContractEnd(contractEnd == null ? null : contractEnd.toString());
            LocalDate from = availableFrom(status, contractEnd, today);
            v.setAvailableFrom(from == null ? null : from.toString());
            v.setPendingBookings(pendingByRoom.getOrDefault(room.getId(), 0L));
            if (forAdmin) {
                List<String> names = new ArrayList<>();
                for (Tenant t : tenants) {
                    names.add(t.getFullName());
                }
                v.setTenants(names);
            }
            result.add(v);
        }
        return result;
    }

    /** A room can be booked when it is empty now or its contract ends soon. */
    public boolean canBook(Room room) {
        LocalDate contractEnd = contractEnd(tenantRepository.findByRoomOrderByFirstNameAsc(room));
        String status = mapStatus(room.getStatus(), contractEnd, LocalDate.now(clock));
        return status.equals(AVAILABLE) || status.equals(SOON);
    }

    /** First day a new tenant could move in, or null when the room cannot be booked. */
    public LocalDate availableFrom(Room room) {
        LocalDate today = LocalDate.now(clock);
        LocalDate contractEnd = contractEnd(tenantRepository.findByRoomOrderByFirstNameAsc(room));
        return availableFrom(mapStatus(room.getStatus(), contractEnd, today), contractEnd, today);
    }

    // ---------- pure logic (unit tested in ServiceLogicTest) ----------

    /** The room is free again when the last tenant's contract ends; unknown if any tenant has no end date. */
    static LocalDate contractEnd(List<Tenant> tenants) {
        LocalDate latest = null;
        for (Tenant t : tenants) {
            if (t.getContractEndDate() == null) {
                return null;
            }
            if (latest == null || t.getContractEndDate().isAfter(latest)) {
                latest = t.getContractEndDate();
            }
        }
        return latest;
    }

    static String mapStatus(RoomStatus roomStatus, LocalDate contractEnd, LocalDate today) {
        if (roomStatus == RoomStatus.MAINTENANCE) {
            return MAINTENANCE;
        }
        if (roomStatus == RoomStatus.AVAILABLE) {
            return AVAILABLE;
        }
        if (contractEnd != null && !contractEnd.isAfter(today.plusDays(SOON_DAYS))) {
            return SOON;
        }
        return OCCUPIED;
    }

    static LocalDate availableFrom(String status, LocalDate contractEnd, LocalDate today) {
        if (status.equals(AVAILABLE)) {
            return today;
        }
        if (status.equals(SOON)) {
            LocalDate next = contractEnd.plusDays(1);
            return next.isBefore(today) ? today : next;
        }
        return null;
    }

    static String label(String status) {
        switch (status) {
            case AVAILABLE:
                return "ว่าง";
            case SOON:
                return "ใกล้หมดสัญญา";
            case MAINTENANCE:
                return "ปิดซ่อม";
            default:
                return "มีผู้เช่า";
        }
    }
}
