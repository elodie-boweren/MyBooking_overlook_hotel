package com.MyBooking.analytics.service;

import com.MyBooking.announcement.service.AnnouncementService;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.service.AuthService;
import com.MyBooking.employee.service.EmployeeService;
import com.MyBooking.event.service.EventService;
import com.MyBooking.feedback.service.FeedbackService;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.service.InstallationService;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.service.ReservationService;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private RoomService roomService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private EventService eventService;

    @Mock
    private InstallationService installationService;

    @Mock
    private LoyaltyService loyaltyService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testClient;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testClient = new User();
        testClient.setId(1L);
        testClient.setEmail("guest@example.com");

        testRoom = new Room("101", RoomType.DELUXE, 2, new BigDecimal("100.00"), "USD", RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("P3-T07: Revenue Analytics prorates multi-month reservations nightly and eliminates double counting (INV-ANL-002)")
    void testRevenueAnalytics_NightlyProration_EliminatesDoubleCounting() {
        // Given: Reservation for 4 nights: May 30 to June 3. Total price = $400.00 ($100/night)
        Reservation crossMonthRes = new Reservation(
            LocalDate.of(2026, 5, 30),
            LocalDate.of(2026, 6, 3),
            2,
            new BigDecimal("400.00"),
            "USD",
            ReservationStatus.CONFIRMED,
            testClient,
            testRoom
        );

        LocalDate mayStart = LocalDate.of(2026, 5, 1);
        LocalDate mayEnd = LocalDate.of(2026, 5, 31);

        when(reservationService.getOverlappingReservationsInDateRange(mayStart, mayEnd, ReservationStatus.CONFIRMED))
            .thenReturn(List.of(crossMonthRes));
        when(eventService.getTotalEventRevenueBetweenDates(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("150.00"));
        when(authService.countUsersByRole(Role.CLIENT)).thenReturn(1L);

        RoomService.RoomStatistics roomStats = new RoomService.RoomStatistics(10L, 8L, 2L, 0L);
        when(roomService.getRoomStatistics()).thenReturn(roomStats);

        // When: Query May revenue
        Map<String, Object> analytics = analyticsService.getRevenueAnalytics(mayStart, mayEnd);

        // Then:
        // Stays on May 30 and May 31 = 2 nights * $100 = $200.00 (NOT $400.00 full price)
        BigDecimal roomRevenue = (BigDecimal) analytics.get("roomRevenue");
        assertThat(roomRevenue).isEqualByComparingTo(new BigDecimal("200.00"));

        BigDecimal eventRevenue = (BigDecimal) analytics.get("eventRevenue");
        assertThat(eventRevenue).isEqualByComparingTo(new BigDecimal("150.00"));

        BigDecimal totalRevenue = (BigDecimal) analytics.get("totalRevenue");
        assertThat(totalRevenue).isEqualByComparingTo(new BigDecimal("350.00"));

        // USALI Metric: ADR = Room Revenue ($200.00) / Occupied Nights (2) = $100.00
        BigDecimal adr = (BigDecimal) analytics.get("adr");
        assertThat(adr).isEqualByComparingTo(new BigDecimal("100.00"));

        // Total available room nights in May (31 days * 10 rooms = 310 room nights)
        // RevPAR = $200.00 / 310 ≈ $0.65
        BigDecimal revPar = (BigDecimal) analytics.get("revPar");
        assertThat(revPar).isEqualByComparingTo(new BigDecimal("0.65"));

        // Occupancy Rate = 2 / 310 * 100 ≈ 0.645%
        Double occupancyRate = (Double) analytics.get("occupancyRate");
        assertThat(occupancyRate).isGreaterThan(0.64).isLessThan(0.65);
    }

    @Test
    @DisplayName("P3-T07: Revenue Analytics calculates real event revenue instead of hardcoded $25,000 placeholder (AUD-ANL-002)")
    void testRevenueAnalytics_CalculatesRealEventRevenue_NoMock() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        when(reservationService.getOverlappingReservationsInDateRange(start, end, ReservationStatus.CONFIRMED))
            .thenReturn(Collections.emptyList());
        when(eventService.getTotalEventRevenueBetweenDates(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("1234.50"));
        when(authService.countUsersByRole(Role.CLIENT)).thenReturn(0L);

        RoomService.RoomStatistics roomStats = new RoomService.RoomStatistics(5L, 5L, 0L, 0L);
        when(roomService.getRoomStatistics()).thenReturn(roomStats);

        Map<String, Object> analytics = analyticsService.getRevenueAnalytics(start, end);

        // Must equal real event revenue ($1234.50), NOT the old $25000.00 placeholder
        BigDecimal eventRevenue = (BigDecimal) analytics.get("eventRevenue");
        assertThat(eventRevenue).isEqualByComparingTo(new BigDecimal("1234.50"));
        assertThat(eventRevenue).isNotEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("P3-T07: Occupancy metrics computes USALI ADR and RevPAR alongside room occupancy")
    void testOccupancyMetrics_WithUSALIMetrics() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10); // 10 days inclusive

        Reservation res = new Reservation(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 6), // 5 nights
            1,
            new BigDecimal("750.00"), // $150/night
            "USD",
            ReservationStatus.CONFIRMED,
            testClient,
            testRoom
        );

        when(reservationService.getOverlappingReservationsInDateRange(start, end, ReservationStatus.CONFIRMED))
            .thenReturn(List.of(res));

        RoomService.RoomStatistics roomStats = new RoomService.RoomStatistics(10L, 9L, 1L, 0L);
        when(roomService.getRoomStatistics()).thenReturn(roomStats);

        when(installationService.getTotalInstallationsCount()).thenReturn(5L);
        when(installationService.getUsedInstallationsCount()).thenReturn(0L);

        Map<String, Object> metrics = analyticsService.getOccupancyMetrics(start, end);

        // 10 rooms * 10 days = 100 total room nights
        assertThat(metrics.get("totalRoomNights")).isEqualTo(100L);
        // 5 occupied room nights
        assertThat(metrics.get("occupiedRoomNights")).isEqualTo(5L);
        // 5 / 100 * 100 = 5.0%
        assertThat(metrics.get("roomOccupancyRate")).isEqualTo(5.0);

        // ADR = $750.00 / 5 = $150.00
        BigDecimal adr = (BigDecimal) metrics.get("adr");
        assertThat(adr).isEqualByComparingTo(new BigDecimal("150.00"));

        // RevPAR = $750.00 / 100 = $7.50
        BigDecimal revPar = (BigDecimal) metrics.get("revPar");
        assertThat(revPar).isEqualByComparingTo(new BigDecimal("7.50"));
    }

    @Test
    @DisplayName("P3-T07: Occupancy metrics handles zero rooms gracefully without ArithmeticException")
    void testOccupancyMetrics_ZeroRooms_SafeFromDivisionByZero() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);

        when(reservationService.getOverlappingReservationsInDateRange(start, end, ReservationStatus.CONFIRMED))
            .thenReturn(Collections.emptyList());

        RoomService.RoomStatistics roomStats = new RoomService.RoomStatistics(0L, 0L, 0L, 0L);
        when(roomService.getRoomStatistics()).thenReturn(roomStats);

        when(installationService.getTotalInstallationsCount()).thenReturn(0L);
        when(installationService.getUsedInstallationsCount()).thenReturn(0L);

        Map<String, Object> metrics = analyticsService.getOccupancyMetrics(start, end);

        assertThat(metrics.get("roomOccupancyRate")).isEqualTo(0.0);
        assertThat(metrics.get("occupiedRoomNights")).isEqualTo(0L);
        assertThat(metrics.get("totalRoomNights")).isEqualTo(0L);
        assertThat((BigDecimal) metrics.get("adr")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((BigDecimal) metrics.get("revPar")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getRevenueTrends_ShouldQueryFromFirstDayOfEachMonth() {
        when(reservationService.getOverlappingReservationsInDateRange(
                any(LocalDate.class), any(LocalDate.class), eq(ReservationStatus.CONFIRMED)))
            .thenReturn(Collections.emptyList());
        when(eventService.getTotalEventRevenueBetweenDates(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.ZERO);

        Map<String, Object> trends = analyticsService.getRevenueTrends(2);
        assertThat(trends.get("monthlyRevenue")).asList().hasSize(2);

        LocalDate expectedFirstMonthStart = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        org.mockito.Mockito.verify(reservationService).getOverlappingReservationsInDateRange(
            expectedFirstMonthStart,
            expectedFirstMonthStart.withDayOfMonth(expectedFirstMonthStart.lengthOfMonth()),
            ReservationStatus.CONFIRMED
        );
    }

    @Test
    void getOperationalKPIs_ShouldReturnRealServiceMetricsAndNotPlaceholders() {
        RoomService.RoomStatistics roomStats = new RoomService.RoomStatistics(10L, 6L, 3L, 1L);
        when(roomService.getRoomStatistics()).thenReturn(roomStats);
        when(installationService.getTotalInstallationsCount()).thenReturn(4L);
        when(installationService.getUsedInstallationsCount()).thenReturn(2L);
        when(authService.countUsersByRole(Role.CLIENT)).thenReturn(20L);
        when(reservationService.getOverlappingReservationsInDateRange(
                any(LocalDate.class), any(LocalDate.class), eq(ReservationStatus.CONFIRMED)))
            .thenReturn(Collections.emptyList());

        com.MyBooking.employee.dto.EmployeeStatisticsDto empStats =
            new com.MyBooking.employee.dto.EmployeeStatisticsDto(8L, 6L, 2L, 1L, 3L, 1L, 2L);
        when(employeeService.getEmployeeStatisticsAsDto()).thenReturn(empStats);

        com.MyBooking.reservation.service.ReservationService.ReservationStatistics resStats =
            new com.MyBooking.reservation.service.ReservationService.ReservationStatistics(
                12L, 10L, 2L, new BigDecimal("1200"), new BigDecimal("120"));
        when(reservationService.getReservationStatistics()).thenReturn(resStats);

        com.MyBooking.event.service.EventService.EventStatistics eventStats =
            new com.MyBooking.event.service.EventService.EventStatistics(7L, 5L, 2L, new BigDecimal("700"), new BigDecimal("100"));
        when(eventService.getEventStatistics()).thenReturn(eventStats);

        when(announcementService.getTotalAnnouncementsCount()).thenReturn(3L);
        when(feedbackService.getFeedbackStatistics()).thenReturn(
            new com.MyBooking.feedback.dto.FeedbackStatisticsDto(4.5, 9L, Collections.emptyMap(), 7L, 1L, 8L, 1L));

        Map<String, Object> kpis = analyticsService.getOperationalKPIs();
        @SuppressWarnings("unchecked")
        Map<String, Object> serviceMetrics = (Map<String, Object>) kpis.get("serviceMetrics");
        assertThat(serviceMetrics.get("totalReservations")).isEqualTo(12L);
        assertThat(serviceMetrics.get("totalEvents")).isEqualTo(7L);
        assertThat(serviceMetrics.get("totalAnnouncements")).isEqualTo(3L);
        assertThat(serviceMetrics.get("totalFeedbacks")).isEqualTo(9L);
    }
}
