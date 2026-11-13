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
        Map<String, Object> analytics = new HashMap<>();
        
        // Room revenue (prorated per night, eliminating cross-month double counting: INV-ANL-002)
        BigDecimal roomRevenue = calculateRoomRevenue(startDate, endDate);
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
        long occupiedRoomNights = calculateOccupiedRoomNights(startDate, endDate);
        long totalRooms = roomService.getRoomStatistics().getTotalRooms();
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
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        for (int i = 0; i < months; i++) {
            LocalDate monthStart = startDate.plusMonths(i);
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
        Map<String, Object> metrics = new HashMap<>();
        
        // Room occupancy
        long totalRooms = roomService.getRoomStatistics().getTotalRooms();
        long occupiedRoomNights = calculateOccupiedRoomNights(startDate, endDate);
        long totalRoomNights = totalRooms * calculateDaysBetween(startDate, endDate);
        
        double roomOccupancyRate = totalRoomNights > 0 ? 
            (double) occupiedRoomNights / totalRoomNights * 100 : 0.0;
        
        metrics.put("roomOccupancyRate", roomOccupancyRate);
        metrics.put("occupiedRoomNights", occupiedRoomNights);
        metrics.put("totalRoomNights", totalRoomNights);
        
        // USALI metrics (ADR & RevPAR)
        BigDecimal roomRevenue = calculateRoomRevenue(startDate, endDate);
        BigDecimal adr = occupiedRoomNights > 0 ?
            roomRevenue.divide(BigDecimal.valueOf(occupiedRoomNights), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        BigDecimal revPar = totalRoomNights > 0 ?
            roomRevenue.divide(BigDecimal.valueOf(totalRoomNights), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        metrics.put("adr", adr);
        metrics.put("revPar", revPar);
        
        // Installation utilization
        long totalInstallations = installationService.getTotalInstallationsCount();
        long usedInstallations = installationService.getMostUsedInstallations().size();
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
        Map<String, Object> performance = new HashMap<>();
        
        // Task completion rates
        Map<String, Object> taskMetrics = getTaskCompletionMetrics();
        performance.put("taskMetrics", taskMetrics);
        
        // Training completion statistics
        Map<String, Object> trainingMetrics = getTrainingCompletionMetrics();
        performance.put("trainingMetrics", trainingMetrics);
        
        // Leave request patterns
        Map<String, Object> leaveMetrics = getLeaveRequestMetrics();
        performance.put("leaveMetrics", leaveMetrics);
        
        return performance;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeProductivityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Active employees
        long totalEmployees = getTotalEmployeesCount();
        long activeEmployees = getActiveEmployeesCount();
        double activeEmployeeRate = totalEmployees > 0 ? 
            (double) activeEmployees / totalEmployees * 100 : 0.0;
        
        metrics.put("totalEmployees", totalEmployees);
        metrics.put("activeEmployees", activeEmployees);
        metrics.put("activeEmployeeRate", activeEmployeeRate);
        
        // Task productivity
        Map<String, Object> taskProductivity = getTaskProductivityMetrics();
        metrics.put("taskProductivity", taskProductivity);
        
        return metrics;
    }

    // ==================== OPERATIONAL KPIs ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getOperationalKPIs() {
        Map<String, Object> kpis = new HashMap<>();
        
        // System-wide metrics
        kpis.put("totalRooms", roomService.getRoomStatistics().getTotalRooms());
        kpis.put("totalInstallations", installationService.getTotalInstallationsCount());
        kpis.put("totalEmployees", getTotalEmployeesCount());
        kpis.put("totalCustomers", getTotalActiveCustomers());
        
        // Service metrics
        Map<String, Object> serviceMetrics = getServiceMetrics();
        kpis.put("serviceMetrics", serviceMetrics);
        
        // Resource utilization
        Map<String, Object> resourceUtilization = getResourceUtilization();
        kpis.put("resourceUtilization", resourceUtilization);
        
        return kpis;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Booking success rates
        double bookingSuccessRate = calculateBookingSuccessRate();
        metrics.put("bookingSuccessRate", bookingSuccessRate);
        
        // Service response metrics
        Map<String, Object> responseMetrics = getServiceResponseMetrics();
        metrics.put("responseMetrics", responseMetrics);
        
        // System health indicators
        Map<String, Object> healthIndicators = getSystemHealthIndicators();
        metrics.put("healthIndicators", healthIndicators);
        
        return metrics;
    }

    // ==================== DASHBOARD DATA ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Current period (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        // Revenue summary
        Map<String, Object> revenueSummary = getRevenueAnalytics(startDate, endDate);
        dashboard.put("revenueSummary", revenueSummary);
        
        // Occupancy summary
        Map<String, Object> occupancySummary = getOccupancyMetrics(startDate, endDate);
        dashboard.put("occupancySummary", occupancySummary);
        
        // Customer insights
        Map<String, Object> customerInsights = getCustomerInsights();
        dashboard.put("customerInsights", customerInsights);
        
        // Employee performance
        Map<String, Object> employeePerformance = getEmployeePerformance();
        dashboard.put("employeePerformance", employeePerformance);
        
        // Operational KPIs
        Map<String, Object> operationalKPIs = getOperationalKPIs();
        dashboard.put("operationalKPIs", operationalKPIs);
        
        // System performance
        Map<String, Object> systemPerformance = getSystemPerformanceMetrics();
        dashboard.put("systemPerformance", systemPerformance);
        
        return dashboard;
    }

    // ==================== HELPER METHODS ====================

    private BigDecimal calculateRoomRevenue(LocalDate startDate, LocalDate endDate) {
        // Push-down database query fetching only reservations active in date range (INV-ANL-001)
        List<Reservation> reservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        
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
        // Push-down database query fetching only reservations active in date range (INV-ANL-001)
        List<Reservation> reservations = reservationService.getOverlappingReservationsInDateRange(
            startDate, endDate, ReservationStatus.CONFIRMED);
        
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
        
        // Total feedback count
        long totalFeedbacks = getTotalFeedbackCount();
        analytics.put("totalFeedbacks", totalFeedbacks);
        
        // Average rating (1-5 scale)
        double averageRating = getAverageRating();
        analytics.put("averageRating", averageRating);
        
        // Rating distribution
        Map<String, Long> ratingDistribution = getRatingDistribution();
        analytics.put("ratingDistribution", ratingDistribution);
        
        // High-rated feedbacks (4-5 stars)
        long highRatedFeedbacks = getHighRatedFeedbackCount();
        analytics.put("highRatedFeedbacks", highRatedFeedbacks);
        
        // Low-rated feedbacks (1-2 stars)
        long lowRatedFeedbacks = getLowRatedFeedbackCount();
        analytics.put("lowRatedFeedbacks", lowRatedFeedbacks);
        
        // Feedback with comments
        long feedbacksWithComments = getFeedbackWithCommentsCount();
        analytics.put("feedbacksWithComments", feedbacksWithComments);
        
        // Customer satisfaction rate (4-5 stars as percentage)
        double satisfactionRate = totalFeedbacks > 0 ? 
            (double) highRatedFeedbacks / totalFeedbacks * 100 : 0.0;
        analytics.put("satisfactionRate", satisfactionRate);
        
        return analytics;
    }

    // ==================== FEEDBACK ANALYTICS HELPER METHODS ====================

    private long getTotalFeedbackCount() {
        // Get total feedback count from FeedbackService
        return feedbackService.getFeedbackStatistics().getTotalFeedbacks();
    }

    private double getAverageRating() {
        // Get average rating from FeedbackService
        Double avgRating = feedbackService.getOverallAverageRating();
        return avgRating != null ? avgRating : 0.0;
    }

    private Map<String, Long> getRatingDistribution() {
        // Get rating distribution from FeedbackService
        Map<String, Long> distribution = new HashMap<>();
        com.MyBooking.feedback.dto.FeedbackStatisticsDto stats = feedbackService.getFeedbackStatistics();
        
        Map<Integer, Long> ratingMap = stats.getRatingDistribution();
        for (Map.Entry<Integer, Long> entry : ratingMap.entrySet()) {
            distribution.put(entry.getKey() + "_stars", entry.getValue());
        }
        
        return distribution;
    }

    private long getHighRatedFeedbackCount() {
        // Get high-rated feedback count (4-5 stars) from FeedbackService
        return feedbackService.getFeedbackStatistics().getHighRatedFeedbacks();
    }

    private long getLowRatedFeedbackCount() {
        // Get low-rated feedback count (1-2 stars) from FeedbackService
        return feedbackService.getFeedbackStatistics().getLowRatedFeedbacks();
    }

    private long getFeedbackWithCommentsCount() {
        // Get feedback with comments count from FeedbackService
        return feedbackService.getFeedbackStatistics().getFeedbacksWithComments();
    }

    // ==================== EMPLOYEE ANALYTICS HELPER METHODS ====================

    private Map<String, Object> getTaskCompletionMetrics() {
        // Get task completion metrics from EmployeeService
        Map<String, Object> metrics = new HashMap<>();
        com.MyBooking.employee.dto.EmployeeStatisticsDto stats = employeeService.getEmployeeStatisticsAsDto();
        
        // For now, we'll use basic counts - can be enhanced with more detailed task metrics
        metrics.put("totalTasks", stats.getTotalEmployees() * 3); // Estimate: 3 tasks per employee
        metrics.put("completedTasks", (long)(stats.getTotalEmployees() * 3 * 0.8)); // 80% completion rate
        metrics.put("completionRate", 80.0);
        metrics.put("averageCompletionTime", 2.5);
        return metrics;
    }

    private Map<String, Object> getTrainingCompletionMetrics() {
        // Get training completion metrics from EmployeeService
        Map<String, Object> metrics = new HashMap<>();
        com.MyBooking.employee.dto.EmployeeStatisticsDto stats = employeeService.getEmployeeStatisticsAsDto();
        
        metrics.put("totalTrainings", stats.getInProgressTrainings() + stats.getCompletedTrainings());
        metrics.put("completedTrainings", stats.getCompletedTrainings());
        metrics.put("inProgressTrainings", stats.getInProgressTrainings());
        
        long totalTrainings = stats.getInProgressTrainings() + stats.getCompletedTrainings();
        double completionRate = totalTrainings > 0 ? 
            (double) stats.getCompletedTrainings() / totalTrainings * 100 : 0.0;
        metrics.put("completionRate", completionRate);
        
        return metrics;
    }

    private Map<String, Object> getLeaveRequestMetrics() {
        // Get leave request metrics from EmployeeService
        Map<String, Object> metrics = new HashMap<>();
        com.MyBooking.employee.dto.EmployeeStatisticsDto stats = employeeService.getEmployeeStatisticsAsDto();
        
        metrics.put("totalRequests", stats.getPendingLeaveRequests() + stats.getApprovedLeaveRequests());
        metrics.put("approvedRequests", stats.getApprovedLeaveRequests());
        metrics.put("pendingRequests", stats.getPendingLeaveRequests());
        metrics.put("rejectedRequests", 0L); // Not tracked in current statistics
        return metrics;
    }

    private long getTotalEmployeesCount() {
        // Get total employee count from EmployeeService
        return employeeService.getEmployeeStatisticsAsDto().getTotalEmployees();
    }

    private long getActiveEmployeesCount() {
        // Get active employee count from EmployeeService
        return employeeService.getEmployeeStatisticsAsDto().getActiveEmployees();
    }

    private Map<String, Object> getTaskProductivityMetrics() {
        // This would integrate with EmployeeService to get task productivity metrics
        // For now, returning placeholder data
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("tasksPerEmployee", 3.0);
        metrics.put("averageTaskDuration", 2.5);
        metrics.put("productivityScore", 85.0);
        return metrics;
    }

    // ==================== OPERATIONAL KPI HELPER METHODS ====================

    private Map<String, Object> getServiceMetrics() {
        // This would integrate with various services to get service metrics
        // For now, returning placeholder data
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalReservations", 200);
        metrics.put("totalEvents", 50);
        metrics.put("totalAnnouncements", 15);
        metrics.put("totalFeedbacks", 120);
        return metrics;
    }

    private Map<String, Object> getResourceUtilization() {
        // This would integrate with various services to get resource utilization
        // For now, returning placeholder data
        Map<String, Object> utilization = new HashMap<>();
        utilization.put("roomUtilization", 75.0);
        utilization.put("installationUtilization", 60.0);
        utilization.put("employeeUtilization", 90.0);
        return utilization;
    }

    private double calculateBookingSuccessRate() {
        // This would integrate with ReservationService to calculate booking success rate
        // For now, returning a placeholder
        return 95.5;
    }

    private Map<String, Object> getServiceResponseMetrics() {
        // This would integrate with various services to get response time metrics
        // For now, returning placeholder data
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("averageResponseTime", 1.2);
        metrics.put("responseTime95thPercentile", 3.5);
        metrics.put("serviceAvailability", 99.8);
        return metrics;
    }

    private Map<String, Object> getSystemHealthIndicators() {
        // This would integrate with system monitoring to get health indicators
        // For now, returning placeholder data
        Map<String, Object> indicators = new HashMap<>();
        indicators.put("systemHealth", "HEALTHY");
        indicators.put("databaseHealth", "HEALTHY");
        indicators.put("serviceHealth", "HEALTHY");
        indicators.put("lastHealthCheck", LocalDateTime.now());
        return indicators;
    }
}

