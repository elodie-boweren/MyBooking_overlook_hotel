package com.MyBooking.concurrency;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.auth.service.AuthService;
import com.MyBooking.feedback.repository.FeedbackRepository;
import com.MyBooking.feedback.service.FeedbackService;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.loyalty.domain.LoyaltyAccount;
import com.MyBooking.loyalty.domain.LoyaltyTxType;
import com.MyBooking.loyalty.repository.LoyaltyAccountRepository;
import com.MyBooking.loyalty.repository.LoyaltyTransactionRepository;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.reservation.service.ReservationService;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.repository.RoomRepository;
import com.MyBooking.room.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3-T10: Real PostgreSQL concurrency & locking verification via Testcontainers.
 * Container port 5432 is mapped to a random ephemeral host port (never hardcoded to 5432).
 */
@SpringBootTest(classes = HotelManagementApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class Phase3AuditConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mybooking_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired private ReservationService reservationService;
    @Autowired private LoyaltyService loyaltyService;
    @Autowired private RoomService roomService;
    @Autowired private AuthService authService;
    @Autowired private FeedbackService feedbackService;

    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private LoyaltyAccountRepository loyaltyAccountRepository;
    @Autowired private LoyaltyTransactionRepository loyaltyTransactionRepository;
    @Autowired private FeedbackRepository feedbackRepository;

    @Test
    @DisplayName("P3-T10: Concurrent createReservationWithPoints on same account prevents double-spending")
    void concurrentCreateReservationWithPoints_ShouldNotDoubleSpend() throws Exception {
        User client = createUser("pts-race-" + UUID.randomUUID() + "@test.com", Role.CLIENT);
        LoyaltyAccount account = new LoyaltyAccount();
        account.setUser(client);
        account.setBalance(1000);
        loyaltyAccountRepository.save(account);

        int threads = 4;
        List<Long> roomIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Room room = new Room();
            room.setNumber("PTS-" + UUID.randomUUID().toString().substring(0, 8));
            room.setRoomType(RoomType.SINGLE);
            room.setCapacity(2);
            room.setPrice(new BigDecimal("100.00"));
            room.setCurrency("USD");
            room.setStatus(RoomStatus.AVAILABLE);
            roomIds.add(roomRepository.save(room).getId());
        }

        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);
        runConcurrently(threads, idx -> {
            try {
                reservationService.createReservationWithPoints(
                        client.getId(), roomIds.get(idx), checkIn, checkOut, 1, "USD", 600);
                successes.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        });

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threads - 1);

        LoyaltyAccount updated = loyaltyAccountRepository.findByUserId(client.getId()).orElseThrow();
        assertThat(updated.getBalance()).isGreaterThanOrEqualTo(0);
        assertThat(loyaltyTransactionRepository.findByAccount(updated))
                .filteredOn(tx -> tx.getType() == LoyaltyTxType.REDEEM)
                .hasSize(1);
    }

    @Test
    @DisplayName("P3-T10: Concurrent processReservationPoints & refundReservationPoints are idempotent")
    void concurrentProcessAndRefundReservationPoints_ShouldBeIdempotent() throws Exception {
        User client = createUser("idem-" + UUID.randomUUID() + "@test.com", Role.CLIENT);
        LoyaltyAccount account = new LoyaltyAccount();
        account.setUser(client);
        account.setBalance(1000);
        loyaltyAccountRepository.save(account);

        Room room = createRoom("IDEM-" + UUID.randomUUID().toString().substring(0, 8));
        Reservation reservation = createReservation(client, room, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        reservation.setTotalPrice(new BigDecimal("200.00"));
        reservation.setPointsUsed(500);
        reservation = reservationRepository.save(reservation);
        Long resId = reservation.getId();

        int threads = 6;
        runConcurrently(threads, idx -> loyaltyService.processReservationPoints(resId));
        assertThat(loyaltyTransactionRepository.findByTypeAndReservationId(LoyaltyTxType.EARN, resId)).hasSize(1);

        runConcurrently(threads, idx -> loyaltyService.refundReservationPoints(resId, 500));
        assertThat(loyaltyTransactionRepository.findByTypeAndReservationId(LoyaltyTxType.REFUND, resId)).hasSize(1);
    }

    @Test
    @DisplayName("P3-T10: Concurrent createRoom with duplicate number allows only one")
    void concurrentCreateRoom_WithDuplicateNumber_ShouldAllowOnlyOne() throws Exception {
        String roomNumber = "DUP-" + UUID.randomUUID().toString().substring(0, 8);
        int threads = 6;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        runConcurrently(threads, idx -> {
            try {
                roomService.createRoom(roomNumber, RoomType.DOUBLE, new BigDecimal("150.00"), "USD", 2, "Race");
                successes.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        });

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threads - 1);
        assertThat(roomRepository.findByNumber(roomNumber)).isPresent();
    }

    @Test
    @DisplayName("P3-T10: Concurrent registerUser with duplicate email allows only one")
    void concurrentRegisterUser_WithDuplicateEmail_ShouldAllowOnlyOne() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String mixedEmail = "  Race." + unique + "@Example.COM ";
        int threads = 6;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        runConcurrently(threads, idx -> {
            try {
                authService.registerUser(mixedEmail, "Password123!", "Race", "User",
                        "1234567890", "123 St", LocalDate.of(1992, 5, 1));
                successes.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        });

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threads - 1);
        assertThat(userRepository.findByEmail("race." + unique + "@example.com")).isPresent();
    }

    @Test
    @DisplayName("P3-T10: Concurrent createFeedback for same reservation allows only one")
    void concurrentCreateFeedback_ForSameReservation_ShouldAllowOnlyOne() throws Exception {
        User client = createUser("fb-race-" + UUID.randomUUID() + "@test.com", Role.CLIENT);
        Room room = createRoom("FB-" + UUID.randomUUID().toString().substring(0, 8));
        Reservation res = createReservation(client, room, LocalDate.now().minusDays(4), LocalDate.now().minusDays(1));

        int threads = 6;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        runConcurrently(threads, idx -> {
            try {
                feedbackService.createFeedback(client.getId(), res.getId(), 5, "Excellent stay");
                successes.incrementAndGet();
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        });

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threads - 1);
        assertThat(feedbackRepository.countByReservationId(res.getId())).isEqualTo(1L);
    }

    @FunctionalInterface
    private interface IndexedTask {
        void run(int index) throws Exception;
    }

    private void runConcurrently(int threads, IndexedTask task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                task.run(idx);
                return null;
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private User createUser(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO5rN8U1C4C4C4C4C4C4C4C4C4C4C4C4C");
        u.setFirstName("Test");
        u.setLastName("User");
        u.setPhone("1234567890");
        u.setAddress("123 Test St");
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        u.setRole(role);
        return userRepository.save(u);
    }

    private Room createRoom(String number) {
        Room r = new Room();
        r.setNumber(number);
        r.setRoomType(RoomType.SINGLE);
        r.setCapacity(2);
        r.setPrice(new BigDecimal("100.00"));
        r.setCurrency("USD");
        r.setStatus(RoomStatus.AVAILABLE);
        return roomRepository.save(r);
    }

    private Reservation createReservation(User client, Room room, LocalDate checkIn, LocalDate checkOut) {
        Reservation r = new Reservation();
        r.setClient(client);
        r.setRoom(room);
        r.setCheckIn(checkIn);
        r.setCheckOut(checkOut);
        r.setNumberOfGuests(1);
        r.setTotalPrice(new BigDecimal("200.00"));
        r.setCurrency("USD");
        r.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(r);
    }
}

