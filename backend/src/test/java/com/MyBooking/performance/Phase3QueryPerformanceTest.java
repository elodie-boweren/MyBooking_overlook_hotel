package com.MyBooking.performance;

import com.MyBooking.analytics.service.AnalyticsService;
import com.MyBooking.announcement.domain.Announcement;
import com.MyBooking.announcement.domain.AnnouncementPriority;
import com.MyBooking.announcement.domain.AnnouncementReply;
import com.MyBooking.announcement.domain.AnnouncementStatus;
import com.MyBooking.announcement.repository.AnnouncementReplyRepository;
import com.MyBooking.announcement.repository.AnnouncementRepository;
import com.MyBooking.announcement.service.AnnouncementService;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.employee.domain.Department;
import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventBooking;
import com.MyBooking.event.domain.EventBookingStatus;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.event.repository.EventBookingRepository;
import com.MyBooking.event.repository.EventRepository;
import com.MyBooking.event.service.EventService;
import com.MyBooking.feedback.domain.Feedback;
import com.MyBooking.feedback.domain.FeedbackReply;
import com.MyBooking.feedback.repository.FeedbackReplyRepository;
import com.MyBooking.feedback.repository.FeedbackRepository;
import com.MyBooking.feedback.service.FeedbackService;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.loyalty.domain.LoyaltyAccount;
import com.MyBooking.loyalty.domain.LoyaltyTransaction;
import com.MyBooking.loyalty.domain.LoyaltyTxType;
import com.MyBooking.loyalty.repository.LoyaltyAccountRepository;
import com.MyBooking.loyalty.repository.LoyaltyTransactionRepository;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.dto.ReservationSearchCriteriaDto;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.reservation.service.ReservationService;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.repository.RoomRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3-T11: Runtime SQL query-count & N+1 verification using Hibernate Statistics.
 */
@SpringBootTest(classes = HotelManagementApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false"
})
class Phase3QueryPerformanceTest {

    @Autowired private EntityManagerFactory entityManagerFactory;

    @Autowired private FeedbackService feedbackService;
    @Autowired private AnnouncementService announcementService;
    @Autowired private ReservationService reservationService;
    @Autowired private EventService eventService;
    @Autowired private LoyaltyService loyaltyService;
    @Autowired private AnalyticsService analyticsService;

    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private FeedbackReplyRepository feedbackReplyRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private AnnouncementReplyRepository announcementReplyRepository;
    @Autowired private InstallationRepository installationRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventBookingRepository eventBookingRepository;
    @Autowired private LoyaltyAccountRepository loyaltyAccountRepository;
    @Autowired private LoyaltyTransactionRepository loyaltyTransactionRepository;

    private Statistics stats;
    private User primaryClient;
    private User adminUser;
    private boolean seeded = false;

    @BeforeEach
    void setUp() {
        stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        if (!seeded) {
            seedDataset(10);
            seeded = true;
        } else {
            primaryClient = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.CLIENT)
                    .findFirst()
                    .orElseThrow();
            adminUser = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .findFirst()
                    .orElseThrow();
        }
        stats.clear();
    }

    @Test
    @DisplayName("P3-T11: Paginated DTO queries and Analytics dashboard execute O(1) SQL statements for N=10 records")
    void verifyBoundedQueryCountsAcrossServices() {
        PageRequest page = PageRequest.of(0, 10);

        stats.clear();
        var feedbacks = feedbackService.getAllFeedbacksAsDto(page);
        long feedbackQueries = stats.getPrepareStatementCount();
        assertThat(feedbacks.getContent()).hasSize(10);
        assertThat(feedbackQueries).isLessThanOrEqualTo(4);

        stats.clear();
        var announcements = announcementService.getPublishedAnnouncements(page);
        long announcementQueries = stats.getPrepareStatementCount();
        assertThat(announcements.getContent()).hasSize(10);
        assertThat(announcementQueries).isLessThanOrEqualTo(3);

        stats.clear();
        var visibleAnnouncements = announcementService.getVisibleAnnouncementsForUserAsDto(primaryClient.getId(), page);
        long visibleAnnQueries = stats.getPrepareStatementCount();
        assertThat(visibleAnnouncements.getContent()).hasSize(10);
        assertThat(visibleAnnQueries).isLessThanOrEqualTo(4);

        stats.clear();
        var reservations = reservationService.searchAllReservationsAsDto(new ReservationSearchCriteriaDto(), page);
        long reservationQueries = stats.getPrepareStatementCount();
        assertThat(reservations.getContent()).hasSize(10);
        assertThat(reservationQueries).isLessThanOrEqualTo(3);

        stats.clear();
        var events = eventService.getAllEventsAsDto(page);
        long eventQueries = stats.getPrepareStatementCount();
        assertThat(events.getContent()).hasSize(10);
        assertThat(eventQueries).isLessThanOrEqualTo(3);

        stats.clear();
        var bookings = eventService.getEventBookingsByUserAsDto(primaryClient.getId(), page);
        long bookingQueries = stats.getPrepareStatementCount();
        assertThat(bookings.getContent()).hasSize(10);
        assertThat(bookingQueries).isLessThanOrEqualTo(4);

        stats.clear();
        var loyaltyAccounts = loyaltyService.getAllLoyaltyAccountsAsDto(page);
        long loyaltyAccQueries = stats.getPrepareStatementCount();
        assertThat(loyaltyAccounts.getContent()).hasSize(10);
        assertThat(loyaltyAccQueries).isLessThanOrEqualTo(3);

        stats.clear();
        var loyaltyTxs = loyaltyService.getTransactionsByUserIdAsDto(primaryClient.getId(), page);
        long loyaltyTxQueries = stats.getPrepareStatementCount();
        assertThat(loyaltyTxs.getContent()).hasSize(10);
        assertThat(loyaltyTxQueries).isLessThanOrEqualTo(4);

        stats.clear();
        var dashboard = analyticsService.getDashboardData();
        long dashboardQueries = stats.getPrepareStatementCount();

        System.out.printf(
                "P3-T11 QUERY COUNTS (N=10): feedback=%d, announcement=%d, visibleAnn=%d, reservation=%d, event=%d, eventBooking=%d, loyaltyAcc=%d, loyaltyTx=%d, dashboard=%d%n",
                feedbackQueries, announcementQueries, visibleAnnQueries, reservationQueries,
                eventQueries, bookingQueries, loyaltyAccQueries, loyaltyTxQueries, dashboardQueries
        );

        assertThat(dashboard).containsKeys("revenueSummary", "occupancySummary", "customerInsights", "operationalKPIs");
        assertThat(dashboardQueries).isLessThanOrEqualTo(40);
    }

    @Transactional
    void seedDataset(int count) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        adminUser = saveUser("perf-admin-" + suffix + "@test.com", Role.ADMIN);

        Installation inst = new Installation();
        inst.setName("Perf Hall " + suffix);
        inst.setInstallationType(InstallationType.CONFERENCE_ROOM);
        inst.setCapacity(100);
        inst.setHourlyRate(new BigDecimal("100.00"));
        inst.setCurrency("USD");
        inst = installationRepository.save(inst);

        for (int i = 0; i < count; i++) {
            User client = saveUser("perf-client-" + i + "-" + suffix + "@test.com", Role.CLIENT);
            if (i == 0) {
                primaryClient = client;
            }

            Room room = new Room();
            room.setNumber("PF-" + suffix + "-" + i);
            room.setRoomType(RoomType.SINGLE);
            room.setCapacity(2);
            room.setPrice(new BigDecimal("120.00"));
            room.setCurrency("USD");
            room.setStatus(RoomStatus.AVAILABLE);
            room = roomRepository.save(room);

            Reservation res = new Reservation();
            res.setClient(client);
            res.setRoom(room);
            res.setCheckIn(LocalDate.now().minusDays(10 + i));
            res.setCheckOut(LocalDate.now().minusDays(8 + i));
            res.setNumberOfGuests(1);
            res.setTotalPrice(new BigDecimal("240.00"));
            res.setCurrency("USD");
            res.setStatus(ReservationStatus.CONFIRMED);
            res = reservationRepository.save(res);

            Feedback fb = new Feedback(res, client, 5, "Great stay " + i);
            fb = feedbackRepository.save(fb);
            FeedbackReply fbReply = new FeedbackReply(fb, adminUser, "Thank you " + i);
            feedbackReplyRepository.save(fbReply);

            Announcement ann = new Announcement();
            ann.setTitle("Perf Announcement " + i);
            ann.setContent("Content " + i);
            ann.setPriority(AnnouncementPriority.HIGH);
            ann.setStatus(AnnouncementStatus.PUBLISHED);
            ann.setTargetDepartment(Department.ALL);
            ann.setCreatedBy(adminUser);
            ann = announcementRepository.save(ann);
            AnnouncementReply annReply = new AnnouncementReply(ann, client, "Noted " + i);
            announcementReplyRepository.save(annReply);

            Event ev = new Event();
            ev.setName("Perf Event " + i);
            ev.setDescription("Desc " + i);
            ev.setEventType(EventType.CONFERENCE);
            ev.setStartAt(LocalDateTime.now().plusDays(10 + i));
            ev.setEndAt(LocalDateTime.now().plusDays(10 + i).plusHours(2));
            ev.setCapacity(50);
            ev.setPrice(new BigDecimal("50.00"));
            ev.setCurrency("USD");
            ev.setInstallation(inst);
            ev = eventRepository.save(ev);

            EventBooking eb = new EventBooking(
                    ev, primaryClient, ev.getStartAt(), 2, ev.getStartAt().minusDays(1), 1,
                    new BigDecimal("50.00"), EventBookingStatus.CONFIRMED);
            eventBookingRepository.save(eb);

            LoyaltyAccount acc = new LoyaltyAccount();
            acc.setUser(client);
            acc.setBalance(500);
            acc = loyaltyAccountRepository.save(acc);

            LoyaltyTransaction tx = new LoyaltyTransaction(
                    i == 0 ? acc : loyaltyAccountRepository.findByUserId(primaryClient.getId()).orElse(acc),
                    LoyaltyTxType.EARN, 100);
            loyaltyTransactionRepository.save(tx);
        }
    }

    private User saveUser(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO5rN8U1C4C4C4C4C4C4C4C4C4C4C4C4C");
        u.setFirstName("Perf");
        u.setLastName("User");
        u.setPhone("1234567890");
        u.setAddress("123 Perf St");
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        u.setRole(role);
        return userRepository.save(u);
    }
}
