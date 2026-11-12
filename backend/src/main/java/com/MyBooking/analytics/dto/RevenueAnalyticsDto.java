package com.MyBooking.analytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public class RevenueAnalyticsDto {
    private BigDecimal roomRevenue;
    private BigDecimal eventRevenue;
    private BigDecimal totalRevenue;
    private Map<String, BigDecimal> revenueByService;
    private BigDecimal avgRevenuePerCustomer;
    private BigDecimal adr;
    private BigDecimal revPar;
    private Double occupancyRate;

    // Constructors
    public RevenueAnalyticsDto() {}

    public RevenueAnalyticsDto(BigDecimal roomRevenue, BigDecimal eventRevenue, BigDecimal totalRevenue,
                              Map<String, BigDecimal> revenueByService, BigDecimal avgRevenuePerCustomer) {
        this(roomRevenue, eventRevenue, totalRevenue, revenueByService, avgRevenuePerCustomer,
             BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
    }

    public RevenueAnalyticsDto(BigDecimal roomRevenue, BigDecimal eventRevenue, BigDecimal totalRevenue,
                              Map<String, BigDecimal> revenueByService, BigDecimal avgRevenuePerCustomer,
                              BigDecimal adr, BigDecimal revPar, Double occupancyRate) {
        this.roomRevenue = roomRevenue;
        this.eventRevenue = eventRevenue;
        this.totalRevenue = totalRevenue;
        this.revenueByService = revenueByService;
        this.avgRevenuePerCustomer = avgRevenuePerCustomer;
        this.adr = adr;
        this.revPar = revPar;
        this.occupancyRate = occupancyRate;
    }

    // Getters and Setters
    public BigDecimal getRoomRevenue() {
        return roomRevenue;
    }

    public void setRoomRevenue(BigDecimal roomRevenue) {
        this.roomRevenue = roomRevenue;
    }

    public BigDecimal getEventRevenue() {
        return eventRevenue;
    }

    public void setEventRevenue(BigDecimal eventRevenue) {
        this.eventRevenue = eventRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Map<String, BigDecimal> getRevenueByService() {
        return revenueByService;
    }

    public void setRevenueByService(Map<String, BigDecimal> revenueByService) {
        this.revenueByService = revenueByService;
    }

    public BigDecimal getAvgRevenuePerCustomer() {
        return avgRevenuePerCustomer;
    }

    public void setAvgRevenuePerCustomer(BigDecimal avgRevenuePerCustomer) {
        this.avgRevenuePerCustomer = avgRevenuePerCustomer;
    }

    public BigDecimal getAdr() {
        return adr;
    }

    public void setAdr(BigDecimal adr) {
        this.adr = adr;
    }

    public BigDecimal getRevPar() {
        return revPar;
    }

    public void setRevPar(BigDecimal revPar) {
        this.revPar = revPar;
    }

    public Double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(Double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}
