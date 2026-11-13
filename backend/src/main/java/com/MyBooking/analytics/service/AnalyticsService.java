package com.MyBooking.analytics.service;

import com.MyBooking.announcement.service.AnnouncementService;
import com.MyBooking.auth.service.AuthService;
import com.MyBooking.employee.service.EmployeeService;
import com.MyBooking.event.service.EventService;
import com.MyBooking.feedback.service.FeedbackService;
import com.MyBooking.installation.service.InstallationService;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.reservation.service.ReservationService;
import com.MyBooking.room.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.event.domain.EventBooking;
import com.MyBooking.event.domain.EventBookingStatus;
import com.MyBooking.auth.domain.Role;

@Service
public class AnalyticsService {

    private final AuthService authService;
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final EventService eventService;
    private final InstallationService installationService;
    private final LoyaltyService loyaltyService;
    private final EmployeeService employeeService;
    private final AnnouncementService announcementService;
    private final FeedbackService feedbackService;

    @Autowired
    public AnalyticsService(AuthService authService, RoomService roomService, ReservationService reservationService,
                           EventService eventService, InstallationService installationService, LoyaltyService loyaltyService,
                           EmployeeService employeeService, AnnouncementService announcementService, FeedbackService feedbackService) {
        this.authService = authService;
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.eventService = eventService;
        this.installationService = installationService;
        this.loyaltyService = loyaltyService;
        this.employeeService = employeeService;
        this.announcementService = announcementService;
        this.feedbackService = feedbackService;
    }

    // ==================== REVENUE ANALYTICS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueAnalytics(LocalDate startDate, LocalDate endDate) {
        List<Reservation> overlappingReservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        return buildRevenueAnalytics(startDate, endDate, overlappingReservations,
            roomService.getRoomStatistics().getTotalRooms());
    }

    private Map<String, Object> buildRevenueAnalytics(LocalDate startDate, LocalDate endDate,
                                                      List<Reservation> overlappingReservations,
                                                      long totalRooms) {
        Map<String, Object> analytics = new HashMap<>();

        // Room revenue (prorated per night, eliminating cross-month double counting: INV-ANL-002)
        BigDecimal roomRevenue = calculateRoomRevenue(overlappingReservations, startDate, endDate);
        analytics.put("roomRevenue", roomRevenue);

        // Event revenue (real confirmed bookings from database: INV-ANL-002)
        BigDecimal eventRevenue = calculateEventRevenue(startDate, endDate);
        analytics.put("eventRevenue", eventRevenue);

        // Total revenue
        BigDecimal totalRevenue = roomRevenue.add(eventRevenue);
        analytics.put("totalRevenue", totalRevenue);

        // Revenue by service type
        Map<String, BigDecimal> revenueByService = new HashMap<>();
        revenueByService.put("rooms", roomRevenue);
        revenueByService.put("events", eventRevenue);
        analytics.put("revenueByService", revenueByService);

        // Average revenue per customer
        long totalCustomers = getTotalActiveCustomers();
        BigDecimal avgRevenuePerCustomer = totalCustomers > 0 ?
            totalRevenue.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        analytics.put("avgRevenuePerCustomer", avgRevenuePerCustomer);

        // USALI Hospitality Standard Metrics: ADR, RevPAR, Occupancy Rate
        long occupiedRoomNights = calculateOccupiedRoomNights(overlappingReservations, startDate, endDate);
        long totalRoomNights = totalRooms * calculateDaysBetween(startDate, endDate);
        
        BigDecimal adr = occupiedRoomNights > 0 ?
            roomRevenue.divide(BigDecimal.valueOf(occupiedRoomNights), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        analytics.put("adr", adr);
        
        BigDecimal revPar = totalRoomNights > 0 ?
            roomRevenue.divide(BigDecimal.valueOf(totalRoomNights), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        analytics.put("revPar", revPar);
        
        double occupancyRate = totalRoomNights > 0 ?
            (double) occupiedRoomNights / totalRoomNights * 100 : 0.0;
        analytics.put("occupancyRate", occupancyRate);
        
        return analytics;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueTrends(int months) {
        Map<String, Object> trends = new HashMap<>();
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        if (months <= 0) {
            trends.put("monthlyRevenue", monthlyData);
            trends.put("totalPeriodRevenue", 0.0);
            return trends;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate firstMonthStart = endDate.withDayOfMonth(1).minusMonths(months - 1L);
        
        for (int i = 0; i < months; i++) {
            LocalDate monthStart = firstMonthStart.plusMonths(i);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.getMonth().name());
            monthData.put("year", monthStart.getYear());
            
            BigDecimal monthRevenue = calculateRoomRevenue(monthStart, monthEnd)
                .add(calculateEventRevenue(monthStart, monthEnd));
            monthData.put("revenue", monthRevenue);
            
            monthlyData.add(monthData);
        }
        
        trends.put("monthlyRevenue", monthlyData);
        trends.put("totalPeriodRevenue", monthlyData.stream()
            .mapToDouble(data -> ((BigDecimal) data.get("revenue")).doubleValue())
            .sum());
        
        return trends;
    }

    // ==================== OCCUPANCY & UTILIZATION METRICS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getOccupancyMetrics(LocalDate startDate, LocalDate endDate) {
        long totalRooms = roomService.getRoomStatistics().getTotalRooms();
        List<Reservation> overlappingReservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        return buildOccupancyMetrics(startDate, endDate, overlappingReservations, totalRooms,
            installationService.getTotalInstallationsCount(), installationService.getUsedInstallationsCount());
    }

    private Map<String, Object> buildOccupancyMetrics(LocalDate startDate, LocalDate endDate,
                                                      List<Reservation> overlappingReservations,
                                                      long totalRooms,
                                                      long totalInstallations,
                                                      long usedInstallations) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Room occupancy
        long occupiedRoomNights = calculateOccupiedRoomNights(overlappingReservations, startDate, endDate);
        long totalRoomNights = totalRooms * calculateDaysBetween(startDate, endDate);
        
        double roomOccupancyRate = totalRoomNights > 0 ? 
            (double) occupiedRoomNights / totalRoomNights * 100 : 0.0;
        
        metrics.put("roomOccupancyRate", roomOccupancyRate);
        metrics.put("occupiedRoomNights", occupiedRoomNights);
        metrics.put("totalRoomNights", totalRoomNights);
        
        // USALI metrics (ADR & RevPAR)
        BigDecimal roomRevenue = calculateRoomRevenue(overlappingReservations, startDate, endDate);
        BigDecimal adr = occupiedRoomNights > 0 ?
            roomRevenue.divide(BigDecimal.valueOf(occupiedRoomNights), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        BigDecimal revPar = totalRoomNights > 0 ?
            roomRevenue.divide(BigDecimal.valueOf(totalRoomNights), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        metrics.put("adr", adr);
        metrics.put("revPar", revPar);
        
        // Installation utilization
        double installationUtilizationRate = totalInstallations > 0 ? 
            (double) usedInstallations / totalInstallations * 100 : 0.0;
        
        metrics.put("installationUtilizationRate", installationUtilizationRate);
        metrics.put("usedInstallations", usedInstallations);
        metrics.put("totalInstallations", totalInstallations);
        
        return metrics;
    }

    // ==================== CUSTOMER ANALYTICS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerInsights() {
        Map<String, Object> insights = new HashMap<>();
        
        // Customer counts
        long totalCustomers = getTotalActiveCustomers();
        
        // Active loyalty users (customers who have earned/redeemed points)
        long activeLoyaltyUsers = getActiveLoyaltyUsersCount();
        double loyaltyEngagementRate = totalCustomers > 0 ? 
            (double) activeLoyaltyUsers / totalCustomers * 100 : 0.0;
        
        insights.put("totalCustomers", totalCustomers);
        insights.put("activeLoyaltyUsers", activeLoyaltyUsers);
        insights.put("loyaltyEngagementRate", loyaltyEngagementRate);
        
        // Customer feedback and ratings
        Map<String, Object> customerFeedback = getCustomerFeedbackAnalytics();
        insights.put("customerFeedback", customerFeedback);
        
        return insights;
    }

    // ==================== EMPLOYEE PERFORMANCE ANALYTICS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeePerformance() {
        com.MyBooking.employee.dto.EmployeeStatisticsDto empStats = employeeService.getEmployeeStatisticsAsDto();
        return buildEmployeePerformance(empStats);
    }

    private Map<String, Object> buildEmployeePerformance(com.MyBooking.employee.dto.EmployeeStatisticsDto empStats) {
        Map<String, Object> performance = new HashMap<>();
        performance.put("taskMetrics", getTaskCompletionMetrics());
        performance.put("trainingMetrics", getTrainingCompletionMetrics(empStats));
        performance.put("leaveMetrics", getLeaveRequestMetrics(empStats));
        return performance;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeProductivityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        com.MyBooking.employee.dto.EmployeeStatisticsDto empStats = employeeService.getEmployeeStatisticsAsDto();
        long totalEmployees = empStats.getTotalEmployees();
        long activeEmployees = empStats.getActiveEmployees();
        double activeEmployeeRate = totalEmployees > 0 ? 
            (double) activeEmployees / totalEmployees * 100 : 0.0;
        
        metrics.put("totalEmployees", totalEmployees);
        metrics.put("activeEmployees", activeEmployees);
        metrics.put("activeEmployeeRate", activeEmployeeRate);
        metrics.put("taskProductivity", getTaskProductivityMetrics(empStats));
        
        return metrics;
    }

    // ==================== OPERATIONAL KPIs ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getOperationalKPIs() {
        com.MyBooking.employee.dto.EmployeeStatisticsDto empStats = employeeService.getEmployeeStatisticsAsDto();
        long totalRooms = roomService.getRoomStatistics().getTotalRooms();
        long totalInstallations = installationService.getTotalInstallationsCount();
        long usedInstallations = installationService.getUsedInstallationsCount();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        List<Reservation> overlapping = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        return buildOperationalKPIs(empStats, totalRooms, totalInstallations, usedInstallations, overlapping, startDate, endDate);
    }

    private Map<String, Object> buildOperationalKPIs(com.MyBooking.employee.dto.EmployeeStatisticsDto empStats,
                                                     long totalRooms,
                                                     long totalInstallations,
                                                     long usedInstallations,
                                                     List<Reservation> overlappingReservations,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        Map<String, Object> kpis = new HashMap<>();
        
        // System-wide metrics
        kpis.put("totalRooms", totalRooms);
        kpis.put("totalInstallations", totalInstallations);
        kpis.put("totalEmployees", empStats.getTotalEmployees());
        kpis.put("totalCustomers", getTotalActiveCustomers());
        
        // Service metrics
        kpis.put("serviceMetrics", getServiceMetrics());
        
        // Resource utilization
        kpis.put("resourceUtilization", getResourceUtilization(
            empStats, totalRooms, totalInstallations, usedInstallations, overlappingReservations, startDate, endDate));
        
        return kpis;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        com.MyBooking.reservation.service.ReservationService.ReservationStatistics resStats =
            reservationService.getReservationStatistics();
        com.MyBooking.employee.dto.EmployeeStatisticsDto empStats =
            employeeService.getEmployeeStatisticsAsDto();
        long totalRooms = roomService.getRoomStatistics().getTotalRooms();

        double bookingSuccessRate = calculateBookingSuccessRate(resStats);
        metrics.put("bookingSuccessRate", bookingSuccessRate);
        metrics.put("responseMetrics", getServiceResponseMetrics(bookingSuccessRate, empStats));
        metrics.put("healthIndicators", getSystemHealthIndicators(totalRooms, empStats));
        
        return metrics;
    }

    // ==================== DASHBOARD DATA ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Current period (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<Reservation> overlappingReservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        long totalRooms = roomService.getRoomStatistics().getTotalRooms();
        long totalInstallations = installationService.getTotalInstallationsCount();
        long usedInstallations = installationService.getUsedInstallationsCount();
        com.MyBooking.employee.dto.EmployeeStatisticsDto empStats = employeeService.getEmployeeStatisticsAsDto();
        
        // Revenue summary
        dashboard.put("revenueSummary", buildRevenueAnalytics(startDate, endDate, overlappingReservations, totalRooms));
        
        // Occupancy summary
        dashboard.put("occupancySummary", buildOccupancyMetrics(
            startDate, endDate, overlappingReservations, totalRooms, totalInstallations, usedInstallations));
        
        // Customer insights
        dashboard.put("customerInsights", getCustomerInsights());
        
        // Employee performance
        dashboard.put("employeePerformance", buildEmployeePerformance(empStats));
        
        // Operational KPIs
        dashboard.put("operationalKPIs", buildOperationalKPIs(
            empStats, totalRooms, totalInstallations, usedInstallations, overlappingReservations, startDate, endDate));
        
        // System performance
        dashboard.put("systemPerformance", getSystemPerformanceMetrics());
        
        return dashboard;
    }

    // ==================== HELPER METHODS ====================

    private BigDecimal calculateRoomRevenue(LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        return calculateRoomRevenue(reservations, startDate, endDate);
    }

    private BigDecimal calculateRoomRevenue(List<Reservation> reservations, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalRoomRevenue = BigDecimal.ZERO;
        for (Reservation r : reservations) {
            totalRoomRevenue = totalRoomRevenue.add(calculateProratedRevenueForReservation(r, startDate, endDate));
        }
        return totalRoomRevenue;
    }

    private BigDecimal calculateProratedRevenueForReservation(Reservation r, LocalDate startDate, LocalDate endDate) {
        if (r.getTotalPrice() == null || r.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        long totalStayNights = ChronoUnit.DAYS.between(r.getCheckIn(), r.getCheckOut());
        if (totalStayNights <= 0) {
            return r.getTotalPrice();
        }
        
        // Effective interval within queried period: [effectiveStart, effectiveEnd)
        LocalDate effectiveStart = r.getCheckIn().isBefore(startDate) ? startDate : r.getCheckIn();
        LocalDate effectiveEnd = r.getCheckOut().isAfter(endDate.plusDays(1)) ? endDate.plusDays(1) : r.getCheckOut();
        
        long nightsInPeriod = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd);
        if (nightsInPeriod <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (nightsInPeriod >= totalStayNights) {
            return r.getTotalPrice();
        }
        
        BigDecimal nightlyRate = r.getTotalPrice().divide(BigDecimal.valueOf(totalStayNights), 4, RoundingMode.HALF_UP);
        return nightlyRate.multiply(BigDecimal.valueOf(nightsInPeriod)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEventRevenue(LocalDate startDate, LocalDate endDate) {
        // Push-down database query aggregating actual confirmed event booking revenue (INV-ANL-002)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return eventService.getTotalEventRevenueBetweenDates(startDateTime, endDateTime);
    }

    private long getTotalActiveCustomers() {
        // Get count of users with CLIENT role without loading entities into heap
        return authService.countUsersByRole(Role.CLIENT);
    }

    private long getActiveLoyaltyUsersCount() {
        // Get count of loyalty accounts (users who have earned/redeemed points)
        return loyaltyService.getLoyaltyStatistics().getTotalAccounts();
    }

    private long calculateOccupiedRoomNights(LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        return calculateOccupiedRoomNights(reservations, startDate, endDate);
    }

    private long calculateOccupiedRoomNights(List<Reservation> reservations, LocalDate startDate, LocalDate endDate) {
        long occupiedRoomNights = 0;
        for (Reservation r : reservations) {
            LocalDate effectiveStart = r.getCheckIn().isBefore(startDate) ? startDate : r.getCheckIn();
            LocalDate effectiveEnd = r.getCheckOut().isAfter(endDate.plusDays(1)) ? endDate.plusDays(1) : r.getCheckOut();
            long nights = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd);
            if (nights > 0) {
                occupiedRoomNights += nights;
            }
        }
        return occupiedRoomNights;
    }

    private long calculateDaysBetween(LocalDate startDate, LocalDate endDate) {
        return endDate.toEpochDay() - startDate.toEpochDay() + 1;
    }

    private Map<String, Object> getCustomerFeedbackAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        com.MyBooking.feedback.dto.FeedbackStatisticsDto stats = feedbackService.getFeedbackStatistics();

        long totalFeedbacks = stats != null ? stats.getTotalFeedbacks() : 0L;
        analytics.put("totalFeedbacks", totalFeedbacks);

        Double avgRating = feedbackService.getOverallAverageRating();
        analytics.put("averageRating", avgRating != null ? avgRating : 0.0);

        Map<String, Long> ratingDistribution = new HashMap<>();
        if (stats != null && stats.getRatingDistribution() != null) {
            for (Map.Entry<Integer, Long> entry : stats.getRatingDistribution().entrySet()) {
                ratingDistribution.put(entry.getKey() + "_stars", entry.getValue());
            }
        }
        analytics.put("ratingDistribution", ratingDistribution);

        long highRatedFeedbacks = stats != null ? stats.getHighRatedFeedbacks() : 0L;
        analytics.put("highRatedFeedbacks", highRatedFeedbacks);

        long lowRatedFeedbacks = stats != null ? stats.getLowRatedFeedbacks() : 0L;
        analytics.put("lowRatedFeedbacks", lowRatedFeedbacks);

        long feedbacksWithComments = stats != null ? stats.getFeedbacksWithComments() : 0L;
        analytics.put("feedbacksWithComments", feedbacksWithComments);

        double satisfactionRate = totalFeedbacks > 0 ?
            (double) highRatedFeedbacks / totalFeedbacks * 100 : 0.0;
        analytics.put("satisfactionRate", satisfactionRate);

        return analytics;
    }

    // ==================== EMPLOYEE ANALYTICS HELPER METHODS ====================

    private Map<String, Object> getTaskCompletionMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Map<String, Long> taskCounts = employeeService.getTaskStatusCounts();
        long totalTasks = taskCounts != null ? taskCounts.getOrDefault("total", 0L) : 0L;
        long completedTasks = taskCounts != null ? taskCounts.getOrDefault("done", 0L) : 0L;
        long inProgressTasks = taskCounts != null ? taskCounts.getOrDefault("inProgress", 0L) : 0L;
        long todoTasks = taskCounts != null ? taskCounts.getOrDefault("todo", 0L) : 0L;
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0.0;

        metrics.put("totalTasks", totalTasks);
        metrics.put("completedTasks", completedTasks);
        metrics.put("inProgressTasks", inProgressTasks);
        metrics.put("todoTasks", todoTasks);
        metrics.put("completionRate", completionRate);
        return metrics;
    }

    private Map<String, Object> getTrainingCompletionMetrics(com.MyBooking.employee.dto.EmployeeStatisticsDto stats) {
        Map<String, Object> metrics = new HashMap<>();
        long inProgress = stats != null ? stats.getInProgressTrainings() : 0L;
        long completed = stats != null ? stats.getCompletedTrainings() : 0L;
        long totalTrainings = inProgress + completed;
        double completionRate = totalTrainings > 0 ? (double) completed / totalTrainings * 100 : 0.0;

        metrics.put("totalTrainings", totalTrainings);
        metrics.put("completedTrainings", completed);
        metrics.put("inProgressTrainings", inProgress);
        metrics.put("completionRate", completionRate);
        return metrics;
    }

    private Map<String, Object> getLeaveRequestMetrics(com.MyBooking.employee.dto.EmployeeStatisticsDto stats) {
        Map<String, Object> metrics = new HashMap<>();
        long pending = stats != null ? stats.getPendingLeaveRequests() : 0L;
        long approved = stats != null ? stats.getApprovedLeaveRequests() : 0L;
        long rejected = employeeService.getRejectedLeaveRequestsCount();
        long total = pending + approved + rejected;

        metrics.put("totalRequests", total);
        metrics.put("approvedRequests", approved);
        metrics.put("pendingRequests", pending);
        metrics.put("rejectedRequests", rejected);
        return metrics;
    }

    private Map<String, Object> getTaskProductivityMetrics(com.MyBooking.employee.dto.EmployeeStatisticsDto stats) {
        Map<String, Object> metrics = new HashMap<>();
        Map<String, Long> taskCounts = employeeService.getTaskStatusCounts();
        long totalTasks = taskCounts != null ? taskCounts.getOrDefault("total", 0L) : 0L;
        long completedTasks = taskCounts != null ? taskCounts.getOrDefault("done", 0L) : 0L;
        long totalEmployees = stats != null ? stats.getTotalEmployees() : 0L;

        double tasksPerEmployee = totalEmployees > 0 ? (double) totalTasks / totalEmployees : 0.0;
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0.0;

        metrics.put("tasksPerEmployee", tasksPerEmployee);
        metrics.put("totalTasks", totalTasks);
        metrics.put("completedTasks", completedTasks);
        metrics.put("completionRate", completionRate);
        return metrics;
    }

    // ==================== OPERATIONAL KPI HELPER METHODS ====================

    private Map<String, Object> getServiceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        com.MyBooking.reservation.service.ReservationService.ReservationStatistics resStats =
            reservationService.getReservationStatistics();
        com.MyBooking.event.service.EventService.EventStatistics eventStats =
            eventService.getEventStatistics();
        com.MyBooking.feedback.dto.FeedbackStatisticsDto feedbackStats =
            feedbackService.getFeedbackStatistics();

        metrics.put("totalReservations", resStats != null ? resStats.getTotalReservations() : 0L);
        metrics.put("totalEvents", eventStats != null ? eventStats.getTotalEvents() : 0L);
        metrics.put("totalAnnouncements", announcementService.getTotalAnnouncementsCount());
        metrics.put("totalFeedbacks", feedbackStats != null ? feedbackStats.getTotalFeedbacks() : 0L);
        return metrics;
    }

    private Map<String, Object> getResourceUtilization(com.MyBooking.employee.dto.EmployeeStatisticsDto empStats,
                                                       long totalRooms,
                                                       long totalInstallations,
                                                       long usedInstallations,
                                                       List<Reservation> overlappingReservations,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {
        Map<String, Object> utilization = new HashMap<>();
        long totalRoomNights = totalRooms * calculateDaysBetween(startDate, endDate);
        long occupiedRoomNights = calculateOccupiedRoomNights(overlappingReservations, startDate, endDate);
        double roomUtilization = totalRoomNights > 0 ? (double) occupiedRoomNights / totalRoomNights * 100 : 0.0;
        double installationUtilization = totalInstallations > 0 ?
            (double) usedInstallations / totalInstallations * 100 : 0.0;
        long totalEmployees = empStats != null ? empStats.getTotalEmployees() : 0L;
        long activeEmployees = empStats != null ? empStats.getActiveEmployees() : 0L;
        double employeeUtilization = totalEmployees > 0 ? (double) activeEmployees / totalEmployees * 100 : 0.0;

        utilization.put("roomUtilization", roomUtilization);
        utilization.put("installationUtilization", installationUtilization);
        utilization.put("employeeUtilization", employeeUtilization);
        return utilization;
    }

    private double calculateBookingSuccessRate(com.MyBooking.reservation.service.ReservationService.ReservationStatistics resStats) {
        if (resStats == null || resStats.getTotalReservations() <= 0) {
            return 0.0;
        }
        return (double) resStats.getConfirmedReservations() / resStats.getTotalReservations() * 100;
    }

    private Map<String, Object> getServiceResponseMetrics(double bookingSuccessRate,
                                                          com.MyBooking.employee.dto.EmployeeStatisticsDto empStats) {
        Map<String, Object> metrics = new HashMap<>();
        long pendingLeave = empStats != null ? empStats.getPendingLeaveRequests() : 0L;
        long approvedLeave = empStats != null ? empStats.getApprovedLeaveRequests() : 0L;
        long rejectedLeave = employeeService.getRejectedLeaveRequestsCount();
        long totalLeave = pendingLeave + approvedLeave + rejectedLeave;
        double leaveResolutionRate = totalLeave > 0 ? (double) (approvedLeave + rejectedLeave) / totalLeave * 100 : 100.0;

        long totalEmployees = empStats != null ? empStats.getTotalEmployees() : 0L;
        long activeEmployees = empStats != null ? empStats.getActiveEmployees() : 0L;
        double activeEmployeeRate = totalEmployees > 0 ? (double) activeEmployees / totalEmployees * 100 : 0.0;

        metrics.put("bookingSuccessRate", bookingSuccessRate);
        metrics.put("leaveResolutionRate", leaveResolutionRate);
        metrics.put("activeEmployeeRate", activeEmployeeRate);
        return metrics;
    }

    private Map<String, Object> getSystemHealthIndicators(long totalRooms,
                                                          com.MyBooking.employee.dto.EmployeeStatisticsDto empStats) {
        Map<String, Object> indicators = new HashMap<>();
        long activeEmployees = empStats != null ? empStats.getActiveEmployees() : 0L;
        long totalEmployees = empStats != null ? empStats.getTotalEmployees() : 0L;
        boolean staffHealthy = totalEmployees == 0 || activeEmployees > 0;
        boolean inventoryHealthy = totalRooms >= 0;

        indicators.put("systemHealth", (staffHealthy && inventoryHealthy) ? "HEALTHY" : "DEGRADED");
        indicators.put("databaseHealth", "HEALTHY");
        indicators.put("serviceHealth", staffHealthy ? "HEALTHY" : "DEGRADED");
        indicators.put("lastHealthCheck", LocalDateTime.now());
        return indicators;
    }
}

