package com.MyBooking.concurrency;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.employee.domain.Employee;
import com.MyBooking.employee.domain.EmployeeStatus;
import com.MyBooking.employee.repository.EmployeeRepository;
import com.MyBooking.employee.repository.ShiftRepository;
import com.MyBooking.employee.service.EmployeeService;
import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.event.dto.EventBookingCreateRequestDto;
import com.MyBooking.event.repository.EventBookingRepository;
import com.MyBooking.event.repository.EventRepository;
import com.MyBooking.event.service.EventService;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.loyalty.domain.LoyaltyAccount;
import com.MyBooking.loyalty.repository.LoyaltyAccountRepository;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.reservation.service.ReservationService;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HotelManagementApplication.class)
@ActiveProfiles("test")
class Phase3ConcurrencyStressTest {

    @Autowired private UserRepository userRepository;
    @Autowired private InstallationRepository installationRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventBookingRepository eventBookingRepository;
    @Autowired private EventService eventService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationService reservationService;
    @Autowired private LoyaltyAccountRepository loyaltyAccountRepository;
    @Autowired private LoyaltyService loyaltyService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private EmployeeService employeeService;

    private User persistUser(String prefix, Role role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User(
                prefix, "Stress",
                prefix + "." + suffix + "@hotel.com",
                "encodedPassword123", "+15550001111", "Hotel Ave",
                LocalDate.of(1990, 1, 1), role);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("INV-EVT-001: 12 concurrent threads booking a 5-spot event never exceed capacity")
    void concurrentEventBookings_ShouldEnforceCapacityWithoutOverbooking() throws Exception {
        Installation inst = installationRepository.save(new Installation(
                "Stress Conf " + UUID.randomUUID(), InstallationType.CONFERENCE_ROOM,
                50, new BigDecimal("200.00"), "USD"));
        Event event = eventRepository.save(new Event(
                "Limited Workshop", EventType.CONFERENCE,
                LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(10).plusHours(2),
                5, new BigDecimal("50.00"), "USD", inst));

        int threads = 12;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            users.add(persistUser("evtuser" + i, Role.CLIENT));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final Long userId = users.get(i).getId();
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    eventService.createEventBookingAsDto(
                            new EventBookingCreateRequestDto(event.getId(), null, 1, "Stress test"),
                            userId);
                    successes.incrementAndGet();
                } catch (Exception ex) {
                    failures.incrementAndGet();
                }
            }));
        }

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        int confirmedParticipants = eventBookingRepository.countConfirmedParticipantsByEventId(event.getId());
        assertThat(successes.get()).isEqualTo(5);
        assertThat(failures.get()).isEqualTo(7);
        assertThat(confirmedParticipants).isEqualTo(5);
    }

    @Test
    @DisplayName("INV-RES-001: 8 concurrent threads booking overlapping dates on the same room allow at most 1")
    void concurrentRoomReservations_ShouldAllowAtMostOneOverlappingBooking() throws Exception {
        Room room = roomRepository.save(new Room(
                "S-" + UUID.randomUUID().toString().substring(0, 6),
                RoomType.DELUXE, 2, new BigDecimal("180.00"), "USD", RoomStatus.AVAILABLE));

        int threads = 8;
        List<User> clients = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            clients.add(persistUser("resclient" + i, Role.CLIENT));
        }

        LocalDate checkIn = LocalDate.now().plusDays(14);
        LocalDate checkOut = checkIn.plusDays(3);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final Long clientId = clients.get(i).getId();
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    reservationService.createReservation(room.getId(), clientId, checkIn, checkOut, 2, "USD");
                    successes.incrementAndGet();
                } catch (Exception ex) {
                    failures.incrementAndGet();
                }
            }));
        }

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(7);
        assertThat(reservationRepository.findByRoomId(room.getId())).hasSize(1);
    }

    @Test
    @DisplayName("INV-LOY-001: 10 concurrent loyalty redemptions never overdraw balance or lose updates")
    void concurrentLoyaltyRedemptions_ShouldNeverOverdrawBalance() throws Exception {
        User user = persistUser("loyuser", Role.CLIENT);
        loyaltyAccountRepository.save(new LoyaltyAccount(user, 100));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    loyaltyService.redeemPoints(user.getId(), 30, "Concurrent redeem #" + idx);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // Expected optimistic lock or insufficient balance failure
                }
            }));
        }

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        LoyaltyAccount updated = loyaltyAccountRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updated.getBalance()).isGreaterThanOrEqualTo(0);
        assertThat(100 - updated.getBalance()).isEqualTo(successes.get() * 30);
    }

    @Test
    @DisplayName("INV-SHF-001: 8 concurrent overlapping shift assignments allow at most 1")
    void concurrentShiftCreation_ShouldPreventOverlappingShifts() throws Exception {
        User empUser = persistUser("shfemp", Role.EMPLOYEE);
        Employee emp = employeeService.createEmployee(empUser.getId(), "Front Desk Agent");

        LocalDateTime shiftStart = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime shiftEnd = shiftStart.plusHours(8);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    employeeService.createShift(emp.getUserId(), shiftStart, shiftEnd);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // Expected overlap conflict
                }
            }));
        }

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(shiftRepository.findByEmployeeId(emp.getUserId())).hasSize(1);
    }
}
