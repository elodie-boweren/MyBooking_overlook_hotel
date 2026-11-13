package com.MyBooking.performance;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.event.service.EventService;
import com.MyBooking.feedback.domain.Feedback;
import com.MyBooking.feedback.domain.FeedbackReply;
import com.MyBooking.feedback.dto.FeedbackResponseDto;
import com.MyBooking.feedback.dto.FeedbackStatisticsDto;
import com.MyBooking.feedback.service.FeedbackService;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.installation.service.InstallationService;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HotelManagementApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class QueryPerformanceBenchmarkTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private InstallationService installationService;

    @Autowired
    private EventService eventService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("P3-T09 & P3-T06: Availability & Event Statistics execute with O(1) SQL queries on 50 installations x 500 events")
    void benchmarkAvailabilityAndEventStatisticsQueryCount() {
        LocalDateTime base = LocalDateTime.now().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Installation targetFreeInstallation = null;

        for (int i = 0; i < 50; i++) {
            Installation inst = new Installation(
                    "Bench Hall " + i,
                    i % 2 == 0 ? InstallationType.CONFERENCE_ROOM : InstallationType.SPA_ROOM,
                    100,
                    new BigDecimal("250.00"),
                    "USD"
            );
            entityManager.persist(inst);
            if (i == 49) {
                targetFreeInstallation = inst;
            }
            for (int j = 0; j < 10; j++) {
                // Leave installation 49 free during [base, base+2h), occupy others in slot 0
                LocalDateTime start = (i < 49 && j == 0) ? base : base.plusDays(j + 1);
                Event ev = new Event(
                        "Bench Event " + i + "-" + j,
                        EventType.CONFERENCE,
                        start,
                        start.plusHours(2),
                        20,
                        new BigDecimal("100.00"),
                        "USD",
                        inst
                );
                entityManager.persist(ev);
            }
        }
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        // 1. InstallationService.findAvailableInstallations
        stats.clear();
        long t1 = System.nanoTime();
        List<Installation> freeInstallations = installationService.findAvailableInstallations(
                base, base.plusHours(2), 50, InstallationType.SPA_ROOM);
        long dt1Ms = (System.nanoTime() - t1) / 1_000_000;
        long q1 = stats.getPrepareStatementCount();
        assertThat(q1).isEqualTo(1L);
        assertThat(freeInstallations).isNotEmpty();

        // 2. EventService.getAvailableInstallations
        stats.clear();
        long t2 = System.nanoTime();
        List<Installation> freeFromEventSvc = eventService.getAvailableInstallations(
                base, base.plusHours(2), 50, InstallationType.SPA_ROOM);
        long dt2Ms = (System.nanoTime() - t2) / 1_000_000;
        long q2 = stats.getPrepareStatementCount();
        assertThat(q2).isEqualTo(1L);
        assertThat(freeFromEventSvc).extracting(Installation::getId).contains(targetFreeInstallation.getId());

        // 3. EventService.isInstallationAvailable
        stats.clear();
        long t3 = System.nanoTime();
        boolean available = eventService.isInstallationAvailable(targetFreeInstallation.getId(), base, base.plusHours(2));
        long dt3Ms = (System.nanoTime() - t3) / 1_000_000;
        long q3 = stats.getPrepareStatementCount();
        assertThat(available).isTrue();
        assertThat(q3).isEqualTo(1L);

        // 4. EventService.getEventStatistics
        stats.clear();
        long t4 = System.nanoTime();
        EventService.EventStatistics eventStats = eventService.getEventStatistics();
        long dt4Ms = (System.nanoTime() - t4) / 1_000_000;
        long q4 = stats.getPrepareStatementCount();
        assertThat(eventStats.getTotalEvents()).isGreaterThanOrEqualTo(500L);
        assertThat(q4).isEqualTo(3L);

        System.out.printf("[BENCHMARK] findAvailableInstallations: %d SQL (%d ms) | getAvailableInstallations: %d SQL (%d ms) | isInstallationAvailable: %d SQL (%d ms) | getEventStatistics: %d SQL (%d ms)%n",
                q1, dt1Ms, q2, dt2Ms, q3, dt3Ms, q4, dt4Ms);
    }

    @Test
    @DisplayName("Batch 1: @EntityGraph pagination & FeedbackService batch reply loading execute in O(1) SQL queries")
    void benchmarkBatch1EntityGraphAndFeedbackPaginationQueryCount() {
        User admin = new User("Admin", "User", "bench-admin@hotel.test", "Pass123!", "+15559990000", "Hotel St", LocalDate.of(1985, 1, 1), Role.ADMIN);
        entityManager.persist(admin);

        Room room = new Room("BENCH-101", RoomType.SINGLE, 2, new BigDecimal("120.00"), "USD", RoomStatus.AVAILABLE);
        entityManager.persist(room);

        for (int i = 0; i < 20; i++) {
            User client = new User("Guest" + i, "Bench", "bench-guest-" + i + "@hotel.test", "Pass123!", String.format("+1555000%04d", i), "Addr " + i, LocalDate.of(1992, 1, 1), Role.CLIENT);
            entityManager.persist(client);

            Reservation res = new Reservation(
                    LocalDate.now().plusDays(10 + i),
                    LocalDate.now().plusDays(12 + i),
                    2,
                    new BigDecimal("240.00"),
                    "USD",
                    ReservationStatus.CONFIRMED,
                    client,
                    room
            );
            entityManager.persist(res);

            Feedback fb = new Feedback(res, client, (i % 5) + 1, "Comment " + i);
            entityManager.persist(fb);

            FeedbackReply reply = new FeedbackReply(fb, admin, "Reply " + i);
            entityManager.persist(reply);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        // 1. ReservationRepository.findAll(Pageable) with @EntityGraph (client + room fetched in 1-2 queries, no N+1)
        stats.clear();
        Page<Reservation> resPage = reservationRepository.findAll(PageRequest.of(0, 20));
        resPage.getContent().forEach(r -> {
            assertThat(r.getClient().getEmail()).isNotBlank();
            assertThat(r.getRoom().getNumber()).isNotBlank();
        });
        long resQueries = stats.getPrepareStatementCount();
        assertThat(resPage.getContent()).hasSize(20);
        assertThat(resQueries).isLessThanOrEqualTo(2L);

        entityManager.clear();

        // 2. FeedbackService.getAllFeedbacksAsDto(Pageable) with @EntityGraph + batch replies (O(1) <= 6 SQL queries for 20 feedbacks + replies)
        stats.clear();
        Page<FeedbackResponseDto> fbPage = feedbackService.getAllFeedbacksAsDto(PageRequest.of(0, 20));
        long fbQueries = stats.getPrepareStatementCount();
        assertThat(fbPage.getContent()).hasSize(20);
        assertThat(fbPage.getContent()).allSatisfy(dto -> assertThat(dto.getReplies()).hasSize(1));
        assertThat(fbQueries).isLessThanOrEqualTo(6L);

        // 3. FeedbackService.getFeedbackStatistics() with single GROUP BY query (6 SQL queries: count + avg + distribution + withComments + highRated + lowRated, down from 10)
        stats.clear();
        FeedbackStatisticsDto fbStats = feedbackService.getFeedbackStatistics();
        long fbStatQueries = stats.getPrepareStatementCount();
        assertThat(fbStats.getTotalFeedbacks()).isGreaterThanOrEqualTo(20L);
        assertThat(fbStatQueries).isEqualTo(6L);

        System.out.printf("[BENCHMARK BATCH 1] Reservation findAll(20): %d SQL | Feedback findAll DTO(20+replies): %d SQL | FeedbackStats: %d SQL%n",
                resQueries, fbQueries, fbStatQueries);
    }
}

