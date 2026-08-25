package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.AdminKpis;
import com.stuttgarttaxi.taxihub.dto.BookingCreateForm;
import com.stuttgarttaxi.taxihub.dto.PublicBookingRequest;
import com.stuttgarttaxi.taxihub.dto.PublicLegRequest;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.BookingStatus;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.LegType;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.exception.BookingAccessDeniedException;
import com.stuttgarttaxi.taxihub.exception.BookingConflictException;
import com.stuttgarttaxi.taxihub.exception.BookingNotFoundException;
import com.stuttgarttaxi.taxihub.repository.BookingRepository;
import com.stuttgarttaxi.taxihub.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.ASSIGNED, BookingStatus.IN_PROGRESS);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerService customerService;

    // ==================== ADMIN ====================

    @Transactional(readOnly = true)
    public AdminKpis getAdminKpis() {
        long totalBookingsToday = bookingRepository.countByBookingDate(LocalDate.now());
        // "Active" = currently assigned to a driver or already underway. There's no
        // separate "start trip" action in this flow, so ASSIGNED bookings are just as
        // active as IN_PROGRESS ones - counting IN_PROGRESS alone would under-report.
        long activeTrips = bookingRepository.countByStatusIn(ACTIVE_STATUSES);
        long pendingApprovals = bookingRepository.countByStatus(BookingStatus.PENDING);

        // TODO: replace with a real value once employee presence/heartbeat tracking exists.
        int employeesOnline = 4;

        return new AdminKpis(totalBookingsToday, activeTrips, employeesOnline, pendingApprovals);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookingsForAdmin() {
        return bookingRepository.findAllByOrderByBookingDateDescBookingTimeDesc();
    }

    // ==================== EMPLOYEE ====================

    @Transactional(readOnly = true)
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatusOrderByBookingDateAscBookingTimeAsc(BookingStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Booking> getActiveBookingForEmployee(Long employeeId) {
        return bookingRepository.findFirstByAssignedEmployeeIdAndStatusIn(employeeId, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public long getTodayCompletedCountForEmployee(Long employeeId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        return bookingRepository.countByAssignedEmployeeIdAndStatusAndCompletedAtBetween(
                employeeId, BookingStatus.COMPLETED, startOfDay, startOfNextDay);
    }

    /**
     * "Assign to Me": rejects if the employee already has an unfinished booking,
     * and rejects if the booking was already taken (checked both eagerly and via
     * the @Version optimistic lock, which is what actually protects against the
     * race where two employees click at the same instant).
     */
    @Transactional
    public Booking assignToSelf(Long bookingId, Employee employee) {
        if (getActiveBookingForEmployee(employee.getId()).isPresent()) {
            throw new BookingConflictException("Zaten aktif bir rezervasyonunuz var");
        }
        return assignBookingToEmployee(bookingId, employee);
    }

    /**
     * Dispatcher-initiated assignment: an admin assigns a PENDING booking to a
     * specific driver from the dispatch map (drag-and-drop or click-to-assign),
     * rather than the driver taking it themselves. Same conflict rules as
     * "Assign to Me" - just parameterized on which employee.
     */
    @Transactional
    public Booking assignToEmployee(Long bookingId, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BookingNotFoundException("Çalışan bulunamadı"));

        if (getActiveBookingForEmployee(employee.getId()).isPresent()) {
            throw new BookingConflictException("Bu çalışanın zaten aktif bir rezervasyonu var");
        }

        return assignBookingToEmployee(bookingId, employee);
    }

    private Booking assignBookingToEmployee(Long bookingId, Employee employee) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Rezervasyon bulunamadı"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Bu rezervasyon başka biri tarafından alındı");
        }

        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setAssignedEmployee(employee);
        booking.setAssignedAt(LocalDateTime.now());

        try {
            // saveAndFlush forces the version-checked UPDATE to run now, so a
            // losing concurrent request fails here instead of at commit time.
            return bookingRepository.saveAndFlush(booking);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BookingConflictException("Bu rezervasyon başka biri tarafından alındı");
        }
    }

    /**
     * "Complete Trip": only the employee the booking is assigned to may complete it.
     */
    @Transactional
    public Booking completeTrip(Long bookingId, Employee employee) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Rezervasyon bulunamadı"));

        Employee assignedEmployee = booking.getAssignedEmployee();
        if (assignedEmployee == null || !assignedEmployee.getId().equals(employee.getId())) {
            throw new BookingAccessDeniedException("Bu rezervasyon size ait değil");
        }

        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new BookingConflictException("Bu rezervasyon zaten tamamlanmış veya iptal edilmiş");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    // ==================== TRACKING ====================

    @Transactional(readOnly = true)
    public Booking getBookingById(Long bookingId) {
        // assignedEmployee is lazy and open-in-view is off, so the tracking
        // page (which reads assignedEmployee.firstName/currentLat/...) needs
        // it fetched eagerly here - otherwise Thymeleaf hits a
        // LazyInitializationException once the transaction has closed.
        return bookingRepository.findWithAssignedEmployeeById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Rezervasyon bulunamadı"));
    }

    // ==================== CREATE BOOKING ====================

    /**
     * Employees currently free of an active job - the pool offered in the
     * "Optional Driver Assignment" dropdown on the create-booking form.
     */
    @Transactional(readOnly = true)
    public List<Employee> getAvailableEmployeesForAssignment() {
        List<Long> busyEmployeeIds = bookingRepository.findAssignedEmployeeIdsByStatusIn(ACTIVE_STATUSES);

        return employeeRepository.findAll().stream()
                .filter(employee -> employee.getRole() == Role.EMPLOYEE)
                .filter(employee -> !busyEmployeeIds.contains(employee.getId()))
                .toList();
    }

    /**
     * Creates a booking from the dispatcher-facing form. If a driver was
     * chosen, the booking starts life already ASSIGNED; otherwise it lands
     * in the PENDING queue just like a booking coming from the public site.
     */
    @Transactional
    public Booking createBooking(BookingCreateForm form) {
        Employee assignedEmployee = null;

        if (form.getAssignedEmployeeId() != null) {
            assignedEmployee = employeeRepository.findById(form.getAssignedEmployeeId())
                    .orElseThrow(() -> new BookingNotFoundException("Seçilen çalışan bulunamadı"));

            if (getActiveBookingForEmployee(assignedEmployee.getId()).isPresent()) {
                throw new BookingConflictException(
                        "Seçilen çalışanın zaten aktif bir rezervasyonu var, lütfen listeyi yenileyin");
            }
        }

        Booking.BookingBuilder builder = Booking.builder()
                .bookingCode(generateBookingCode())
                .lastName(form.getLastName())
                .firstName(form.getFirstName())
                .email(form.getEmail())
                .phoneNumber(form.getPhoneNumber())
                .pickupAddress(form.getPickupAddress())
                .dropoffAddress(form.getDropoffAddress())
                .pickupLat(form.getPickupLat())
                .pickupLng(form.getPickupLng())
                .bookingDate(form.getBookingDate())
                .bookingTime(form.getBookingTime())
                .vehicleType(form.getVehicleType())
                .passengerCount(form.getPassengerCount())
                .luggageCount(form.getLuggageCount())
                .customerMessage(form.getCustomerMessage())
                .paymentMethod(form.getPaymentMethod());

        if (assignedEmployee != null) {
            builder.status(BookingStatus.ASSIGNED)
                    .assignedEmployee(assignedEmployee)
                    .assignedAt(LocalDateTime.now());
        } else {
            builder.status(BookingStatus.PENDING);
        }

        Booking savedBooking = bookingRepository.save(builder.build());

        // Keeps the Customer Database an accurate record of everyone who's
        // actually booked, without a dispatcher having to add them by hand.
        customerService.ensureCustomerExists(
                savedBooking.getEmail(), savedBooking.getFirstName(),
                savedBooking.getLastName(), savedBooking.getPhoneNumber());

        return savedBooking;
    }

    /**
     * Creates one leg (outbound or return) of a public-site booking. Called
     * once per leg by PublicBookingController - a round trip calls this
     * twice with the same groupId and opposite legType, producing two fully
     * independent Booking rows (own status, own driver assignment, own
     * tracking) that dispatch/reports/tracking handle with no changes since
     * they already operate per-row. pickupLat/pickupLng come from the same
     * RouteService geocode the controller used to price the leg, so the
     * dispatch map gets a pin immediately, same as the admin-created flow.
     */
    @Transactional
    public Booking createPublicBooking(PublicBookingRequest request, PublicLegRequest leg,
                                        String groupId, LegType legType,
                                        Double pickupLat, Double pickupLng) {
        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .lastName(request.getLastName())
                .firstName(request.getFirstName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .pickupAddress(leg.getPickupAddress())
                .dropoffAddress(leg.getDropoffAddress())
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .bookingDate(leg.getDate())
                .bookingTime(leg.getTime())
                .vehicleType(request.getVehicleType())
                .passengerCount(request.getPassengerCount())
                .luggageCount(request.getLuggageCount())
                .customerMessage(request.getCustomerMessage())
                .paymentMethod(request.getPaymentMethod())
                .babySeatCount(request.getBabySeatCount())
                .childSeatCount(request.getChildSeatCount())
                .boosterSeatCount(request.getBoosterSeatCount())
                .waterQty(leg.getWaterQty())
                .colaQty(leg.getColaQty())
                .sodaLemonadeQty(leg.getSodaLemonadeQty())
                .orangeJuiceQty(leg.getOrangeJuiceQty())
                .groupId(groupId)
                .legType(legType)
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        customerService.ensureCustomerExists(
                savedBooking.getEmail(), savedBooking.getFirstName(),
                savedBooking.getLastName(), savedBooking.getPhoneNumber());

        return savedBooking;
    }

    // ==================== SHARED ====================

    /**
     * Generates a unique PNR-style booking code, e.g. "PNR4821". Not an IATA/
     * ISO-governed format (no such standard exists for PNRs - that's each
     * airline/reservation system's own convention) - just a short, easy to
     * read-aloud reference customers can quote to customer service.
     */
    public String generateBookingCode() {
        String code;
        do {
            code = "PNR" + String.format("%04d", RANDOM.nextInt(10_000));
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }
}
