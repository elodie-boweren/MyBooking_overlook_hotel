package com.MyBooking.installation.service;

import com.MyBooking.event.domain.Event;
import com.MyBooking.event.repository.EventRepository;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallationServiceTest {

    @Mock
    private InstallationRepository installationRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private InstallationService installationService;

    private Installation testInstallation;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testInstallation = new Installation();
        testInstallation.setId(1L);
        testInstallation.setName("Grand Ballroom");
        testInstallation.setDescription("Elegant ballroom for special events");
        testInstallation.setInstallationType(InstallationType.CONFERENCE_ROOM);
        testInstallation.setCapacity(100);
        testInstallation.setHourlyRate(new BigDecimal("150.00"));
        testInstallation.setCurrency("USD");
        testInstallation.setEquipment("Projector, Sound System, Stage");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Corporate Meeting");
        testEvent.setStartAt(LocalDateTime.of(2024, 6, 15, 10, 0));
        testEvent.setEndAt(LocalDateTime.of(2024, 6, 15, 12, 0));
        testEvent.setInstallation(testInstallation);
    }

    // ==================== INSTALLATION MANAGEMENT TESTS ====================

    @Test
    void createInstallation_WithValidData_ShouldCreateInstallation() {
        // Given
        String name = "Spa Room 1";
        String description = "Relaxing spa room";
        InstallationType type = InstallationType.SPA_ROOM;
        Integer capacity = 10;
        BigDecimal hourlyRate = new BigDecimal("80.00");
        String currency = "USD";
        String equipment = "Massage table, Aromatherapy";

        when(installationRepository.existsByNameIgnoreCase(name)).thenReturn(false);
        when(installationRepository.save(any(Installation.class))).thenReturn(testInstallation);

        // When
        Installation result = installationService.createInstallation(name, description, type, capacity, hourlyRate, currency, equipment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Grand Ballroom"); // From mock return
        verify(installationRepository).existsByNameIgnoreCase(name);
        verify(installationRepository).save(any(Installation.class));
    }

    @Test
    void createInstallation_WithDuplicateName_ShouldThrowBusinessRuleException() {
        // Given
        String name = "Grand Ballroom";
        when(installationRepository.existsByNameIgnoreCase(name)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> installationService.createInstallation(name, "Description", InstallationType.CONFERENCE_ROOM, 50, new BigDecimal("100.00"), "USD", "Equipment"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Installation with name 'Grand Ballroom' already exists");
    }

    @Test
    void createInstallation_WithInvalidData_ShouldThrowBusinessRuleException() {
        // Given
        String name = "";
        InstallationType type = InstallationType.CONFERENCE_ROOM;
        Integer capacity = 0; // Invalid capacity
        BigDecimal hourlyRate = new BigDecimal("0.00"); // Invalid rate
        String currency = "US"; // Invalid currency length

        // When & Then
        assertThatThrownBy(() -> installationService.createInstallation(name, "Description", type, capacity, hourlyRate, currency, "Equipment"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateInstallation_WithValidData_ShouldUpdateInstallation() {
        // Given
        Long installationId = 1L;
        String newName = "Updated Ballroom";
        String newDescription = "Updated description";
        InstallationType newType = InstallationType.WEDDING_ROOM;
        Integer newCapacity = 150;
        BigDecimal newHourlyRate = new BigDecimal("200.00");
        String newCurrency = "EUR";
        String newEquipment = "Updated equipment";

        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));
        when(installationRepository.existsByNameIgnoreCase(newName)).thenReturn(false);
        when(installationRepository.save(any(Installation.class))).thenReturn(testInstallation);

        // When
        Installation result = installationService.updateInstallation(installationId, newName, newDescription, newType, newCapacity, newHourlyRate, newCurrency, newEquipment);

        // Then
        assertThat(result).isNotNull();
        verify(installationRepository).findByIdWithoutEvents(installationId);
        verify(installationRepository).existsByNameIgnoreCase(newName);
        verify(installationRepository).save(any(Installation.class));
    }

    @Test
    void updateInstallation_WithNonExistentId_ShouldThrowNotFoundException() {
        // Given
        Long installationId = 999L;
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> installationService.updateInstallation(installationId, "Name", "Description", InstallationType.CONFERENCE_ROOM, 50, new BigDecimal("100.00"), "USD", "Equipment"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Installation not found with ID: 999");
    }

    @Test
    void deleteInstallation_WithNoEvents_ShouldDeleteInstallation() {
        // Given
        Long installationId = 1L;
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallation(testInstallation)).thenReturn(Collections.emptyList());

        // When
        installationService.deleteInstallation(installationId);

        // Then
        verify(installationRepository).findByIdWithoutEvents(installationId);
        verify(eventRepository).findByInstallation(testInstallation);
        verify(installationRepository).delete(testInstallation);
    }

    @Test
    void deleteInstallation_WithExistingEvents_ShouldThrowBusinessRuleException() {
        // Given
        Long installationId = 1L;
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallation(testInstallation)).thenReturn(Arrays.asList(testEvent));

        // When & Then
        assertThatThrownBy(() -> installationService.deleteInstallation(installationId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete installation with existing events");
    }

    @Test
    void getInstallationById_WithValidId_ShouldReturnInstallation() {
        // Given
        Long installationId = 1L;
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));

        // When
        Installation result = installationService.getInstallationById(installationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Grand Ballroom");
        verify(installationRepository).findByIdWithoutEvents(installationId);
    }

    @Test
    void getInstallationById_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long installationId = 999L;
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> installationService.getInstallationById(installationId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Installation not found with ID: 999");
    }

    @Test
    void getAllInstallations_ShouldReturnPageOfInstallations() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Installation> installations = Arrays.asList(testInstallation);
        Page<Installation> page = new PageImpl<>(installations);
        when(installationRepository.findAllWithoutEvents(pageable)).thenReturn(page);

        // When
        Page<Installation> result = installationService.getAllInstallations(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Grand Ballroom");
        verify(installationRepository).findAllWithoutEvents(pageable);
    }

    // ==================== AVAILABILITY & CAPACITY TESTS ====================

    @Test
    void isInstallationAvailable_WithNoOverlappingEvents_ShouldReturnTrue() {
        // Given
        Long installationId = 1L;
        LocalDateTime startDateTime = LocalDateTime.of(2024, 6, 16, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 6, 16, 12, 0);

        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallation(testInstallation)).thenReturn(Collections.emptyList());
        when(eventRepository.checkEventAvailability(startDateTime, endDateTime)).thenReturn(Collections.emptyList());

        // When
        boolean result = installationService.isInstallationAvailable(installationId, startDateTime, endDateTime);

        // Then
        assertThat(result).isTrue();
        verify(installationRepository).findByIdWithoutEvents(installationId);
        verify(eventRepository).checkEventAvailability(startDateTime, endDateTime);
    }

    @Test
    void isInstallationAvailable_WithOverlappingEvents_ShouldReturnFalse() {
        // Given
        Long installationId = 1L;
        LocalDateTime startDateTime = LocalDateTime.of(2024, 6, 15, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 6, 15, 12, 0);

        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallation(testInstallation)).thenReturn(Arrays.asList(testEvent));
        when(eventRepository.checkEventAvailability(startDateTime, endDateTime)).thenReturn(Arrays.asList(testEvent));

        // When
        boolean result = installationService.isInstallationAvailable(installationId, startDateTime, endDateTime);

        // Then
        assertThat(result).isFalse();
        verify(installationRepository).findByIdWithoutEvents(installationId);
        verify(eventRepository).checkEventAvailability(startDateTime, endDateTime);
    }

    @Test
    void canAccommodateParticipants_WithSufficientCapacity_ShouldReturnTrue() {
        // Given
        Long installationId = 1L;
        Integer participants = 50;
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));

        // When
        boolean result = installationService.canAccommodateParticipants(installationId, participants);

        // Then
        assertThat(result).isTrue();
        verify(installationRepository).findByIdWithoutEvents(installationId);
    }

    @Test
    void canAccommodateParticipants_WithInsufficientCapacity_ShouldReturnFalse() {
        // Given
        Long installationId = 1L;
        Integer participants = 150; // More than capacity of 100
        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));

        // When
        boolean result = installationService.canAccommodateParticipants(installationId, participants);

        // Then
        assertThat(result).isFalse();
        verify(installationRepository).findByIdWithoutEvents(installationId);
    }

    @Test
    void findAvailableInstallations_WithValidCriteria_ShouldReturnAvailableInstallations() {
        // Given
        LocalDateTime startDateTime = LocalDateTime.of(2024, 6, 16, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 6, 16, 12, 0);
        Integer participants = 50;
        InstallationType installationType = InstallationType.CONFERENCE_ROOM;

        List<Installation> suitableInstallations = Arrays.asList(testInstallation);
        when(installationRepository.findSuitableForParticipants(participants)).thenReturn(suitableInstallations);
        when(installationRepository.findByIdWithoutEvents(testInstallation.getId())).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallation(testInstallation)).thenReturn(Collections.emptyList());
        when(eventRepository.checkEventAvailability(startDateTime, endDateTime)).thenReturn(Collections.emptyList());

        // When
        List<Installation> result = installationService.findAvailableInstallations(startDateTime, endDateTime, participants, installationType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Grand Ballroom");
        verify(installationRepository).findSuitableForParticipants(participants);
    }

    // ==================== PRICING & COST CALCULATION TESTS ====================

    @Test
    void calculateEventCost_WithValidDuration_ShouldCalculateCorrectCost() {
        // Given
        Long installationId = 1L;
        LocalDateTime startDateTime = LocalDateTime.of(2024, 6, 15, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 6, 15, 12, 0); // 2 hours
        BigDecimal expectedCost = new BigDecimal("300.00"); // 2 hours * $150/hour

        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));

        // When
        BigDecimal result = installationService.calculateEventCost(installationId, startDateTime, endDateTime);

        // Then
        assertThat(result).isEqualByComparingTo(expectedCost);
        verify(installationRepository).findByIdWithoutEvents(installationId);
    }

    @Test
    void calculateEventCost_WithMinimumDuration_ShouldCalculateMinimumCost() {
        // Given
        Long installationId = 1L;
        LocalDateTime startDateTime = LocalDateTime.of(2024, 6, 15, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 6, 15, 10, 30); // 30 minutes, should be 1 hour minimum
        BigDecimal expectedCost = new BigDecimal("150.00"); // 1 hour * $150/hour

        when(installationRepository.findByIdWithoutEvents(installationId)).thenReturn(Optional.of(testInstallation));

        // When
        BigDecimal result = installationService.calculateEventCost(installationId, startDateTime, endDateTime);

        // Then
        assertThat(result).isEqualByComparingTo(expectedCost);
        verify(installationRepository).findByIdWithoutEvents(installationId);
    }

    @Test
    void calculateEventCost_WithInstallationObject_ShouldCalculateCorrectCost() {
        // Given
        LocalDateTime startDateTime = LocalDateTime.of(2024, 6, 15, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 6, 15, 14, 0); // 4 hours
        BigDecimal expectedCost = new BigDecimal("600.00"); // 4 hours * $150/hour

        // When
        BigDecimal result = installationService.calculateEventCost(testInstallation, startDateTime, endDateTime);

        // Then
        assertThat(result).isEqualByComparingTo(expectedCost);
    }

    // ==================== SEARCH & FILTERING TESTS ====================

    @Test
    void findInstallationsByType_ShouldReturnInstallationsOfType() {
        // Given
        InstallationType type = InstallationType.CONFERENCE_ROOM;
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByInstallationType(type)).thenReturn(installations);

        // When
        List<Installation> result = installationService.findInstallationsByType(type);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstallationType()).isEqualTo(type);
        verify(installationRepository).findByInstallationType(type);
    }

    @Test
    void findInstallationsByCapacityRange_ShouldReturnInstallationsInRange() {
        // Given
        Integer minCapacity = 50;
        Integer maxCapacity = 150;
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByCapacityBetween(minCapacity, maxCapacity)).thenReturn(installations);

        // When
        List<Installation> result = installationService.findInstallationsByCapacityRange(minCapacity, maxCapacity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findByCapacityBetween(minCapacity, maxCapacity);
    }

    @Test
    void findInstallationsByPriceRange_ShouldReturnInstallationsInPriceRange() {
        // Given
        BigDecimal minRate = new BigDecimal("100.00");
        BigDecimal maxRate = new BigDecimal("200.00");
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByHourlyRateBetween(minRate, maxRate)).thenReturn(installations);

        // When
        List<Installation> result = installationService.findInstallationsByPriceRange(minRate, maxRate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findByHourlyRateBetween(minRate, maxRate);
    }

    @Test
    void searchInstallationsByName_ShouldReturnMatchingInstallations() {
        // Given
        String name = "Ballroom";
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByNameContainingIgnoreCase(name)).thenReturn(installations);

        // When
        List<Installation> result = installationService.searchInstallationsByName(name);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findByNameContainingIgnoreCase(name);
    }

    @Test
    void searchInstallationsByEquipment_ShouldReturnMatchingInstallations() {
        // Given
        String equipment = "Projector";
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByEquipmentContainingIgnoreCase(equipment)).thenReturn(installations);

        // When
        List<Installation> result = installationService.searchInstallationsByEquipment(equipment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findByEquipmentContainingIgnoreCase(equipment);
    }

    // ==================== STATISTICS & ANALYTICS TESTS ====================

    @Test
    void getTotalInstallationsCount_ShouldReturnTotalCount() {
        // Given
        long expectedCount = 5L;
        when(installationRepository.count()).thenReturn(expectedCount);

        // When
        long result = installationService.getTotalInstallationsCount();

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(installationRepository).count();
    }

    @Test
    void getInstallationsCountByType_ShouldReturnCountForType() {
        // Given
        InstallationType type = InstallationType.CONFERENCE_ROOM;
        long expectedCount = 3L;
        when(installationRepository.countByInstallationType(type)).thenReturn(expectedCount);

        // When
        long result = installationService.getInstallationsCountByType(type);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(installationRepository).countByInstallationType(type);
    }

    @Test
    void getInstallationsCountByType_ShouldReturnMapOfCounts() {
        // Given
        when(installationRepository.countByInstallationType(InstallationType.SPA_ROOM)).thenReturn(2L);
        when(installationRepository.countByInstallationType(InstallationType.CONFERENCE_ROOM)).thenReturn(3L);
        when(installationRepository.countByInstallationType(InstallationType.GYM)).thenReturn(1L);
        when(installationRepository.countByInstallationType(InstallationType.POOL)).thenReturn(1L);
        when(installationRepository.countByInstallationType(InstallationType.TENNIS_COURT)).thenReturn(2L);
        when(installationRepository.countByInstallationType(InstallationType.WEDDING_ROOM)).thenReturn(1L);

        // When
        Map<InstallationType, Long> result = installationService.getInstallationsCountByType();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get(InstallationType.SPA_ROOM)).isEqualTo(2L);
        assertThat(result.get(InstallationType.CONFERENCE_ROOM)).isEqualTo(3L);
        assertThat(result.get(InstallationType.GYM)).isEqualTo(1L);
        assertThat(result.get(InstallationType.POOL)).isEqualTo(1L);
        assertThat(result.get(InstallationType.TENNIS_COURT)).isEqualTo(2L);
        assertThat(result.get(InstallationType.WEDDING_ROOM)).isEqualTo(1L);
    }

    @Test
    void getMostUsedInstallations_ShouldReturnUsedInstallations() {
        // Given
        List<Installation> usedInstallations = Arrays.asList(testInstallation);
        when(installationRepository.findUsedInstallations()).thenReturn(usedInstallations);

        // When
        List<Installation> result = installationService.getMostUsedInstallations();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findUsedInstallations();
    }

    @Test
    void getUnusedInstallations_ShouldReturnUnusedInstallations() {
        // Given
        List<Installation> unusedInstallations = Arrays.asList(testInstallation);
        when(installationRepository.findUnusedInstallations()).thenReturn(unusedInstallations);

        // When
        List<Installation> result = installationService.getUnusedInstallations();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findUnusedInstallations();
    }

    @Test
    void getHighCapacityInstallations_ShouldReturnHighCapacityInstallations() {
        // Given
        Integer minCapacity = 100;
        List<Installation> highCapacityInstallations = Arrays.asList(testInstallation);
        when(installationRepository.findHighCapacityInstallations(minCapacity)).thenReturn(highCapacityInstallations);

        // When
        List<Installation> result = installationService.getHighCapacityInstallations(minCapacity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findHighCapacityInstallations(minCapacity);
    }

    @Test
    void getPremiumInstallations_ShouldReturnPremiumInstallations() {
        // Given
        BigDecimal minRate = new BigDecimal("100.00");
        List<Installation> premiumInstallations = Arrays.asList(testInstallation);
        when(installationRepository.findPremiumInstallations(minRate)).thenReturn(premiumInstallations);

        // When
        List<Installation> result = installationService.getPremiumInstallations(minRate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findPremiumInstallations(minRate);
    }

    @Test
    void getAverageCapacityByType_ShouldReturnAverageCapacityData() {
        // Given
        List<Object[]> averageData = Arrays.<Object[]>asList(new Object[]{InstallationType.CONFERENCE_ROOM, 75.5});
        when(installationRepository.getAverageCapacityByType()).thenReturn(averageData);

        // When
        List<Object[]> result = installationService.getAverageCapacityByType();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).getAverageCapacityByType();
    }

    @Test
    void getAverageHourlyRateByType_ShouldReturnAverageRateData() {
        // Given
        List<Object[]> averageData = Arrays.<Object[]>asList(new Object[]{InstallationType.CONFERENCE_ROOM, 125.75});
        when(installationRepository.getAverageHourlyRateByType()).thenReturn(averageData);

        // When
        List<Object[]> result = installationService.getAverageHourlyRateByType();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).getAverageHourlyRateByType();
    }

    // ==================== UTILITY METHODS TESTS ====================

    @Test
    void existsByName_WithExistingName_ShouldReturnTrue() {
        // Given
        String name = "Grand Ballroom";
        when(installationRepository.existsByNameIgnoreCase(name)).thenReturn(true);

        // When
        boolean result = installationService.existsByName(name);

        // Then
        assertThat(result).isTrue();
        verify(installationRepository).existsByNameIgnoreCase(name);
    }

    @Test
    void existsByName_WithNonExistingName_ShouldReturnFalse() {
        // Given
        String name = "Non-existent Room";
        when(installationRepository.existsByNameIgnoreCase(name)).thenReturn(false);

        // When
        boolean result = installationService.existsByName(name);

        // Then
        assertThat(result).isFalse();
        verify(installationRepository).existsByNameIgnoreCase(name);
    }

    @Test
    void findByName_WithExistingName_ShouldReturnInstallation() {
        // Given
        String name = "Grand Ballroom";
        when(installationRepository.findByNameIgnoreCase(name)).thenReturn(Optional.of(testInstallation));

        // When
        Installation result = installationService.findByName(name);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Grand Ballroom");
        verify(installationRepository).findByNameIgnoreCase(name);
    }

    @Test
    void findByName_WithNonExistingName_ShouldThrowNotFoundException() {
        // Given
        String name = "Non-existent Room";
        when(installationRepository.findByNameIgnoreCase(name)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> installationService.findByName(name))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Installation not found with name: Non-existent Room");
    }

    @Test
    void findInstallationsByCurrency_ShouldReturnInstallationsWithCurrency() {
        // Given
        String currency = "USD";
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByCurrency(currency)).thenReturn(installations);

        // When
        List<Installation> result = installationService.findInstallationsByCurrency(currency);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrency()).isEqualTo(currency);
        verify(installationRepository).findByCurrency(currency);
    }

    @Test
    void findInstallationsCreatedAfter_ShouldReturnInstallationsCreatedAfterDate() {
        // Given
        LocalDateTime date = LocalDateTime.of(2024, 1, 1, 0, 0);
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByCreatedAtAfter(date)).thenReturn(installations);

        // When
        List<Installation> result = installationService.findInstallationsCreatedAfter(date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findByCreatedAtAfter(date);
    }

    @Test
    void findInstallationsCreatedBetween_ShouldReturnInstallationsInDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);
        List<Installation> installations = Arrays.asList(testInstallation);
        when(installationRepository.findByCreatedAtBetween(startDate, endDate)).thenReturn(installations);

        // When
        List<Installation> result = installationService.findInstallationsCreatedBetween(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(installationRepository).findByCreatedAtBetween(startDate, endDate);
    }
}
