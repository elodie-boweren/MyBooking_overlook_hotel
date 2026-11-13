package com.MyBooking.installation.service;

import com.MyBooking.event.domain.Event;
import com.MyBooking.event.repository.EventRepository;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.dto.InstallationResponseDto;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InstallationService {

    private final InstallationRepository installationRepository;
    private final EventRepository eventRepository;

    @Autowired
    public InstallationService(InstallationRepository installationRepository, EventRepository eventRepository) {
        this.installationRepository = installationRepository;
        this.eventRepository = eventRepository;
    }

    // ==================== INSTALLATION MANAGEMENT ====================

    @Transactional
    public Installation createInstallation(String name, String description, InstallationType installationType, 
                                         Integer capacity, BigDecimal hourlyRate, String currency, String equipment) {
        validateInstallationData(name, installationType, capacity, hourlyRate, currency);
        
        if (installationRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("Installation with name '" + name + "' already exists.");
        }

        Installation installation = new Installation(name, installationType, capacity, hourlyRate, currency);
        installation.setDescription(description);
        installation.setEquipment(equipment);
        
        return installationRepository.save(installation);
    }

    @Transactional
    public Installation updateInstallation(Long installationId, String name, String description, 
                                         InstallationType installationType, Integer capacity, 
                                         BigDecimal hourlyRate, String currency, String equipment) {
        Installation installation = getInstallationById(installationId);
        validateInstallationData(name, installationType, capacity, hourlyRate, currency);
        
        // Check for name conflicts (excluding current installation)
        if (!installation.getName().equalsIgnoreCase(name) && 
            installationRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("Installation with name '" + name + "' already exists.");
        }

        installation.setName(name);
        installation.setDescription(description);
        installation.setInstallationType(installationType);
        installation.setCapacity(capacity);
        installation.setHourlyRate(hourlyRate);
        installation.setCurrency(currency);
        installation.setEquipment(equipment);
        
        return installationRepository.save(installation);
    }

    @Transactional
    public void deleteInstallation(Long installationId) {
        Installation installation = getInstallationById(installationId);
        
        // Check if installation has any events
        List<Event> events = eventRepository.findByInstallation(installation);
        if (!events.isEmpty()) {
            throw new BusinessRuleException("Cannot delete installation with existing events. Delete events first.");
        }
        
        installationRepository.delete(installation);
    }

    @Transactional(readOnly = true)
    public Installation getInstallationById(Long installationId) {
        return installationRepository.findByIdWithoutEvents(installationId)
                .orElseThrow(() -> new NotFoundException("Installation not found with ID: " + installationId));
    }

    @Transactional(readOnly = true)
    public InstallationResponseDto getInstallationByIdForApi(Long installationId) {
        return installationRepository.findByIdAsProjection(installationId)
                .orElseThrow(() -> new NotFoundException("Installation not found with ID: " + installationId));
    }

    @Transactional(readOnly = true)
    public Page<Installation> getAllInstallations(Pageable pageable) {
        return installationRepository.findAllWithoutEvents(pageable);
    }

    @Transactional(readOnly = true)
    public List<InstallationResponseDto> getAllInstallationsForApi() {
        return installationRepository.findAllAsProjection();
    }

    public InstallationResponseDto convertToResponseDto(Installation installation) {
        return new InstallationResponseDto(
            installation.getId(),
            installation.getName(),
            installation.getDescription(),
            installation.getInstallationType(),
            installation.getCapacity(),
            installation.getHourlyRate(),
            installation.getCurrency(),
            installation.getEquipment(),
            installation.getCreatedAt(),
            installation.getUpdatedAt()
        );
    }

    // ==================== AVAILABILITY & CAPACITY MANAGEMENT ====================

    @Transactional(readOnly = true)
    public boolean isInstallationAvailable(Long installationId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        getInstallationById(installationId);
        return !eventRepository.existsOverlappingEventForInstallation(installationId, startDateTime, endDateTime);
    }

    @Transactional(readOnly = true)
    public boolean canAccommodateParticipants(Long installationId, Integer participants) {
        Installation installation = getInstallationById(installationId);
        return installation.getCapacity() >= participants;
    }

    @Transactional(readOnly = true)
    public List<Installation> findAvailableInstallations(LocalDateTime startDateTime, LocalDateTime endDateTime, 
                                                        Integer participants, InstallationType installationType) {
        return installationRepository.findAvailableInstallations(startDateTime, endDateTime, participants, installationType);
    }

    @Transactional(readOnly = true)
    public List<Installation> findAvailableInstallations(LocalDateTime startDateTime, LocalDateTime endDateTime, 
                                                        Integer participants) {
        return findAvailableInstallations(startDateTime, endDateTime, participants, null);
    }

    // ==================== PRICING & COST CALCULATION ====================

    @Transactional(readOnly = true)
    public BigDecimal calculateEventCost(Long installationId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Installation installation = getInstallationById(installationId);
        
        // Calculate duration in hours (minimum 1 hour)
        long durationHours = java.time.Duration.between(startDateTime, endDateTime).toHours();
        if (durationHours < 1) {
            durationHours = 1;
        }
        
        return installation.getHourlyRate().multiply(BigDecimal.valueOf(durationHours));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateEventCost(Installation installation, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        long durationHours = java.time.Duration.between(startDateTime, endDateTime).toHours();
        if (durationHours < 1) {
            durationHours = 1;
        }
        
        return installation.getHourlyRate().multiply(BigDecimal.valueOf(durationHours));
    }

    // ==================== SEARCH & FILTERING ====================

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsByType(InstallationType installationType) {
        return installationRepository.findByInstallationType(installationType);
    }

    @Transactional(readOnly = true)
    public Page<Installation> findInstallationsByType(InstallationType installationType, Pageable pageable) {
        return installationRepository.findByInstallationType(installationType, pageable);
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsByCapacityRange(Integer minCapacity, Integer maxCapacity) {
        return installationRepository.findByCapacityBetween(minCapacity, maxCapacity);
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsByPriceRange(BigDecimal minRate, BigDecimal maxRate) {
        return installationRepository.findByHourlyRateBetween(minRate, maxRate);
    }

    @Transactional(readOnly = true)
    public List<Installation> searchInstallationsByName(String name) {
        return installationRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Installation> searchInstallationsByEquipment(String equipment) {
        return installationRepository.findByEquipmentContainingIgnoreCase(equipment);
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsByTypeAndCapacity(InstallationType installationType, Integer capacity) {
        return installationRepository.findByInstallationTypeAndCapacity(installationType, capacity);
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsByTypeAndCapacityRange(InstallationType installationType, 
                                                                     Integer minCapacity, Integer maxCapacity) {
        return installationRepository.findByInstallationTypeAndCapacityBetween(installationType, minCapacity, maxCapacity);
    }

    // ==================== STATISTICS & ANALYTICS ====================

    @Transactional(readOnly = true)
    public long getTotalInstallationsCount() {
        return installationRepository.count();
    }

    @Transactional(readOnly = true)
    public long getInstallationsCountByType(InstallationType installationType) {
        return installationRepository.countByInstallationType(installationType);
    }

    @Transactional(readOnly = true)
    public Map<InstallationType, Long> getInstallationsCountByType() {
        return Map.of(
                InstallationType.SPA_ROOM, installationRepository.countByInstallationType(InstallationType.SPA_ROOM),
                InstallationType.CONFERENCE_ROOM, installationRepository.countByInstallationType(InstallationType.CONFERENCE_ROOM),
                InstallationType.GYM, installationRepository.countByInstallationType(InstallationType.GYM),
                InstallationType.POOL, installationRepository.countByInstallationType(InstallationType.POOL),
                InstallationType.TENNIS_COURT, installationRepository.countByInstallationType(InstallationType.TENNIS_COURT),
                InstallationType.WEDDING_ROOM, installationRepository.countByInstallationType(InstallationType.WEDDING_ROOM)
        );
    }

    @Transactional(readOnly = true)
    public List<Installation> getMostUsedInstallations() {
        return installationRepository.findUsedInstallations();
    }

    @Transactional(readOnly = true)
    public long getUsedInstallationsCount() {
        return installationRepository.countUsedInstallations();
    }

    @Transactional(readOnly = true)
    public List<Installation> getUnusedInstallations() {
        return installationRepository.findUnusedInstallations();
    }

    @Transactional(readOnly = true)
    public List<Installation> getHighCapacityInstallations(Integer minCapacity) {
        return installationRepository.findHighCapacityInstallations(minCapacity);
    }

    @Transactional(readOnly = true)
    public List<Installation> getPremiumInstallations(BigDecimal minRate) {
        return installationRepository.findPremiumInstallations(minRate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getAverageCapacityByType() {
        return installationRepository.getAverageCapacityByType();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getAverageHourlyRateByType() {
        return installationRepository.getAverageHourlyRateByType();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCapacityStatisticsByType() {
        return installationRepository.getCapacityStatisticsByType();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getHourlyRateStatisticsByType() {
        return installationRepository.getHourlyRateStatisticsByType();
    }

    // ==================== VALIDATION METHODS ====================

    private void validateInstallationData(String name, InstallationType installationType, 
                                        Integer capacity, BigDecimal hourlyRate, String currency) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleException("Installation name is required.");
        }
        
        if (installationType == null) {
            throw new BusinessRuleException("Installation type is required.");
        }
        
        if (capacity == null || capacity < 1 || capacity > 200) {
            throw new BusinessRuleException("Capacity must be between 1 and 200.");
        }
        
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Hourly rate must be greater than 0.");
        }
        
        if (currency == null || currency.length() != 3) {
            throw new BusinessRuleException("Currency must be exactly 3 characters (e.g., USD, EUR).");
        }
    }

    // ==================== UTILITY METHODS ====================

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return installationRepository.existsByNameIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public Installation findByName(String name) {
        return installationRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException("Installation not found with name: " + name));
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsByCurrency(String currency) {
        return installationRepository.findByCurrency(currency);
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsCreatedAfter(LocalDateTime date) {
        return installationRepository.findByCreatedAtAfter(date);
    }

    @Transactional(readOnly = true)
    public List<Installation> findInstallationsCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return installationRepository.findByCreatedAtBetween(startDate, endDate);
    }

}

