package com.MyBooking.analytics.dto;

import java.math.BigDecimal;

public class OccupancyMetricsDto {
    private double roomOccupancyRate;
    private long occupiedRoomNights;
    private long totalRoomNights;
    private double installationUtilizationRate;
    private long usedInstallations;
    private long totalInstallations;
    private BigDecimal adr;
    private BigDecimal revPar;

    // Constructors
    public OccupancyMetricsDto() {}

    public OccupancyMetricsDto(double roomOccupancyRate, long occupiedRoomNights, long totalRoomNights,
                              double installationUtilizationRate, long usedInstallations, long totalInstallations) {
        this(roomOccupancyRate, occupiedRoomNights, totalRoomNights,
             installationUtilizationRate, usedInstallations, totalInstallations,
             BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public OccupancyMetricsDto(double roomOccupancyRate, long occupiedRoomNights, long totalRoomNights,
                              double installationUtilizationRate, long usedInstallations, long totalInstallations,
                              BigDecimal adr, BigDecimal revPar) {
        this.roomOccupancyRate = roomOccupancyRate;
        this.occupiedRoomNights = occupiedRoomNights;
        this.totalRoomNights = totalRoomNights;
        this.installationUtilizationRate = installationUtilizationRate;
        this.usedInstallations = usedInstallations;
        this.totalInstallations = totalInstallations;
        this.adr = adr;
        this.revPar = revPar;
    }

    // Getters and Setters
    public double getRoomOccupancyRate() {
        return roomOccupancyRate;
    }

    public void setRoomOccupancyRate(double roomOccupancyRate) {
        this.roomOccupancyRate = roomOccupancyRate;
    }

    public long getOccupiedRoomNights() {
        return occupiedRoomNights;
    }

    public void setOccupiedRoomNights(long occupiedRoomNights) {
        this.occupiedRoomNights = occupiedRoomNights;
    }

    public long getTotalRoomNights() {
        return totalRoomNights;
    }

    public void setTotalRoomNights(long totalRoomNights) {
        this.totalRoomNights = totalRoomNights;
    }

    public double getInstallationUtilizationRate() {
        return installationUtilizationRate;
    }

    public void setInstallationUtilizationRate(double installationUtilizationRate) {
        this.installationUtilizationRate = installationUtilizationRate;
    }

    public long getUsedInstallations() {
        return usedInstallations;
    }

    public void setUsedInstallations(long usedInstallations) {
        this.usedInstallations = usedInstallations;
    }

    public long getTotalInstallations() {
        return totalInstallations;
    }

    public void setTotalInstallations(long totalInstallations) {
        this.totalInstallations = totalInstallations;
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
}
