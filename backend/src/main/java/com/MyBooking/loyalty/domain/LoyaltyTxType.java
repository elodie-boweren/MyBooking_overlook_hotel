package com.MyBooking.loyalty.domain;

public enum LoyaltyTxType {
    EARN("Earn points"),
    REDEEM("Redeem points"),
    REFUND("Refund redeemed points"),
    CLAWBACK("Clawback earned points");
    
    private final String description;
    
    LoyaltyTxType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}