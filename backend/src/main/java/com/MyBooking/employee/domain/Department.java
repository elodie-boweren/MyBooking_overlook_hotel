package com.MyBooking.employee.domain;

/**
 * Operational hotel departments used for ABAC workspace adaptation,
 * announcement audience scoping (INV-ANN-001), and task routing (ADR-0017).
 */
public enum Department {
    ALL,
    FRONT_DESK,
    HOUSEKEEPING,
    MAINTENANCE,
    FOOD_AND_BEVERAGE,
    MANAGEMENT,
    SECURITY
}

