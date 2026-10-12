package com.MyBooking.employee.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrainingStatusTest {
    
    @Test
    void testTrainingStatusEnumValues() {
        // Test that all expected enum values exist
        TrainingStatus[] values = TrainingStatus.values();
        assertEquals(3, values.length);
        
        // Test specific enum values
        assertTrue(TrainingStatus.ASSIGNED != null);
        assertTrue(TrainingStatus.IN_PROGRESS != null);
        assertTrue(TrainingStatus.COMPLETED != null);
    }
    
    @Test
    void testTrainingStatusValueOf() {
        // Test valueOf method
        assertEquals(TrainingStatus.ASSIGNED, TrainingStatus.valueOf("ASSIGNED"));
        assertEquals(TrainingStatus.IN_PROGRESS, TrainingStatus.valueOf("IN_PROGRESS"));
        assertEquals(TrainingStatus.COMPLETED, TrainingStatus.valueOf("COMPLETED"));
    }
    
    @Test
    void testTrainingStatusToString() {
        // Test toString method
        assertEquals("ASSIGNED", TrainingStatus.ASSIGNED.toString());
        assertEquals("IN_PROGRESS", TrainingStatus.IN_PROGRESS.toString());
        assertEquals("COMPLETED", TrainingStatus.COMPLETED.toString());
    }
    
    @Test
    void testTrainingStatusOrdinal() {
        // Test ordinal values
        assertEquals(0, TrainingStatus.ASSIGNED.ordinal());
        assertEquals(1, TrainingStatus.IN_PROGRESS.ordinal());
        assertEquals(2, TrainingStatus.COMPLETED.ordinal());
    }
}
