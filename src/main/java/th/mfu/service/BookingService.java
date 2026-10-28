package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import th.mfu.BookingRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.config.Messages;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.BookingStatus;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

/**
 * Booking requests from the 3D room map: checks and saves a new request (public page and tenant app),
 * and moves a logged-in applicant into the room when the admin confirms it.
 */
@Service
public class BookingService {

    // a confirmed booking from the app gets a one-year contract
    static final int CONTRACT_MONTHS = 12;

    private final BookingRequestRepository bookingRepository;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final RoomMapService roomMapService;
    private final Clock clock;
    private final Messages messages;

    public BookingService(BookingRequestRepository bookingRepository, TenantRepository tenantRepository,
            RoomRepository roomRepository, RoomMapService roomMapService, Clock clock, Messages messages) {
        this.bookingRepository = bookingRepository;
        this.tenantRepository = tenantRepository;
        this.roomRepository = roomRepository;
        this.roomMapService = roomMapService;
        this.clock = clock;
        this.messages = messages;
    }

    /** Why the request cannot be sent (a message for the user), or empty when it is fine. The room must be set. */
    public Optional<String> problem(BookingRequest booking) {
        Room room = booking.getRoom();
        if (!StringUtils.hasText(booking.getFullName()) || !StringUtils.hasText(booking.getPhone())) {
            return Optional.of(messages.get("flash.book.missing"));
        }
        if (!roomMapService.canBook(room)) {
            return Optional.of(messages.get("flash.book.taken", room.getRoomNumber()));
        }
        LocalDate from = roomMapService.availableFrom(room);
        if (booking.getMoveInDate() == null || booking.getMoveInDate().isBefore(from)) {
            return Optional.of(messages.get("flash.book.tooEarly", room.getRoomNumber(), from.toString()));
        }
        return Optional.empty();
    }

    /** Saves a checked request, resetting the values the visitor must not choose themselves. */
    public BookingRequest submit(BookingRequest booking) {
        booking.setId(null);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now(clock));
        booking.setFullName(booking.getFullName().trim());
        booking.setPhone(booking.getPhone().trim());
        return bookingRepository.save(booking);
    }

    /**
     * After the admin confirms a request sent from the app, adds the applicant as a tenant of the room
     * so their account is linked (bills, repairs, parcels). Empty when there is nothing to do:
     * the request came from the public page, the account already has a tenant record, or the room is full.
     */
    public Optional<Tenant> moveIn(BookingRequest booking) {
        if (booking.getApplicant() == null) {
            return Optional.empty();
        }
        String email = booking.getApplicant().getEmail();
        Room room = booking.getRoom();
        if (tenantRepository.findFirstByEmailIgnoreCase(email).isPresent()
                || tenantRepository.countByRoom(room) >= room.getCapacity()) {
            return Optional.empty();
        }
        String[] name = splitName(booking.getFullName());
        Tenant tenant = new Tenant();
        tenant.setFirstName(name[0]);
        tenant.setLastName(name[1]);
        tenant.setPhone(booking.getPhone());
        tenant.setEmail(email);
        LocalDate checkIn = booking.getMoveInDate() != null ? booking.getMoveInDate() : LocalDate.now(clock);
        tenant.setCheckInDate(checkIn);
        tenant.setContractEndDate(checkIn.plusMonths(CONTRACT_MONTHS));
        tenant.setRoom(room);
        tenantRepository.save(tenant);
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
        }
        return Optional.of(tenant);
    }

    /** "สมชาย ใจดี" -> ["สมชาย", "ใจดี"]; a single word becomes the first name with an empty last name. */
    static String[] splitName(String fullName) {
        String name = fullName == null ? "" : fullName.trim();
        int space = name.indexOf(' ');
        return space < 0 ? new String[] { name, "" } : new String[] { name.substring(0, space), name.substring(space + 1).trim() };
    }
}
