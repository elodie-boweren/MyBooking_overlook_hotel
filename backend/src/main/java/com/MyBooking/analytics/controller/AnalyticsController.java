package com.MyBooking.analytics.controller;

import com.MyBooking.analytics.dto.*;
import com.MyBooking.analytics.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ==================== REVENUE ANALYTICS ====================

    @GetMapping("/revenue")
    public ResponseEntity<RevenueAnalyticsDto> getRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> analytics = analyticsService.getRevenueAnalytics(startDate, endDate);
        
        RevenueAnalyticsDto response = new RevenueAnalyticsDto(
            (java.math.BigDecimal) analytics.get("roomRevenue"),
            (java.math.BigDecimal) analytics.get("eventRevenue"),
            (java.math.BigDecimal) analytics.get("totalRevenue"),
            (Map<String, java.math.BigDecimal>) analytics.get("revenueByService"),
            (java.math.BigDecimal) analytics.get("avgRevenuePerCustomer"),
            (java.math.BigDecimal) analytics.get("adr"),
            (java.math.BigDecimal) analytics.get("revPar"),
            (Double) analytics.get("occupancyRate")
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue/trends")
    public ResponseEntity<RevenueTrendsDto> getRevenueTrends(
            @RequestParam(defaultValue = "12") int months) {
        
        Map<String, Object> trends = analyticsService.getRevenueTrends(months);
        
        RevenueTrendsDto response = new RevenueTrendsDto(
            (java.util.List<Map<String, Object>>) trends.get("monthlyRevenue"),
            (java.math.BigDecimal) trends.get("totalPeriodRevenue")
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== OCCUPANCY ANALYTICS ====================

    @GetMapping("/occupancy")
    public ResponseEntity<OccupancyMetricsDto> getOccupancyMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> metrics = analyticsService.getOccupancyMetrics(startDate, endDate);
        
        OccupancyMetricsDto response = new OccupancyMetricsDto(
            (Double) metrics.get("roomOccupancyRate"),
            (Long) metrics.get("occupiedRoomNights"),
            (Long) metrics.get("totalRoomNights"),
            (Double) metrics.get("installationUtilizationRate"),
            (Long) metrics.get("usedInstallations"),
            (Long) metrics.get("totalInstallations"),
            (java.math.BigDecimal) metrics.get("adr"),
            (java.math.BigDecimal) metrics.get("revPar")
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== CUSTOMER ANALYTICS ====================

    @GetMapping("/customers")
    public ResponseEntity<CustomerInsightsDto> getCustomerInsights() {
        Map<String, Object> insights = analyticsService.getCustomerInsights();
        
        CustomerInsightsDto response = new CustomerInsightsDto(
            (Long) insights.get("totalCustomers"),
            (Long) insights.get("activeLoyaltyUsers"),
            (Double) insights.get("loyaltyEngagementRate"),
            (Map<String, Object>) insights.get("customerFeedback")
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== EMPLOYEE ANALYTICS ====================

    @GetMapping("/employees/performance")
    public ResponseEntity<EmployeePerformanceDto> getEmployeePerformance() {
        Map<String, Object> performance = analyticsService.getEmployeePerformance();
        
        EmployeePerformanceDto response = new EmployeePerformanceDto(
            (Map<String, Object>) performance.get("taskMetrics"),
            (Map<String, Object>) performance.get("trainingMetrics"),
            (Map<String, Object>) performance.get("leaveMetrics")
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employees/productivity")
    public ResponseEntity<EmployeeProductivityDto> getEmployeeProductivity() {
        Map<String, Object> metrics = analyticsService.getEmployeeProductivityMetrics();
        
        EmployeeProductivityDto response = new EmployeeProductivityDto(
            (Long) metrics.get("totalEmployees"),
            (Long) metrics.get("activeEmployees"),
            (Double) metrics.get("activeEmployeeRate"),
            (Map<String, Object>) metrics.get("taskProductivity")
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== OPERATIONAL KPIs ====================

    @GetMapping("/kpis")
    public ResponseEntity<OperationalKPIsDto> getOperationalKPIs() {
        Map<String, Object> kpis = analyticsService.getOperationalKPIs();
        
        OperationalKPIsDto response = new OperationalKPIsDto(
            (Long) kpis.get("totalRooms"),
            (Long) kpis.get("totalInstallations"),
            (Long) kpis.get("totalEmployees"),
            (Long) kpis.get("totalCustomers"),
            (Map<String, Object>) kpis.get("serviceMetrics"),
            (Map<String, Object>) kpis.get("resourceUtilization")
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/system-performance")
    public ResponseEntity<SystemPerformanceDto> getSystemPerformance() {
        Map<String, Object> metrics = analyticsService.getSystemPerformanceMetrics();
        
        SystemPerformanceDto response = new SystemPerformanceDto(
            (Double) metrics.get("bookingSuccessRate"),
            (Map<String, Object>) metrics.get("responseMetrics"),
            (Map<String, Object>) metrics.get("healthIndicators")
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDataDto> getDashboardData() {
        Map<String, Object> dashboard = analyticsService.getDashboardData();
        
        DashboardDataDto response = new DashboardDataDto(
            (Map<String, Object>) dashboard.get("revenueSummary"),
            (Map<String, Object>) dashboard.get("occupancySummary"),
            (Map<String, Object>) dashboard.get("customerInsights"),
            (Map<String, Object>) dashboard.get("employeePerformance"),
            (Map<String, Object>) dashboard.get("operationalKPIs"),
            (Map<String, Object>) dashboard.get("systemPerformance")
        );
        
        return ResponseEntity.ok(response);
    }
}
