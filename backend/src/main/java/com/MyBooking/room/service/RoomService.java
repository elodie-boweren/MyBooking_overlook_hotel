//Room Management - CRUD operations for rooms
//Availability Calculation - Check room availability for dates
//Status Management - Update room status (clean, dirty, out of service)
//Equipment Management - Manage room equipment
//Photo Management - Handle room photos
//Search & Filtering - Find rooms by criteria

package com.MyBooking.room.service;

import com.MyBooking.room.domain.*;
import com.MyBooking.room.repository.*;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private RoomPhotoRepository roomPhotoRepository;
    
    @Autowired
    private RoomStatusUpdateRepository roomStatusUpdateRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private UserRepository userRepository;

    // ========== ROOM MANAGEMENT ==========

    /**
     * Create a new room
     */
    public Room createRoom(String number, RoomType roomType, BigDecimal price, 
                          String currency, Integer capacity, String description) {
        // Validate room number uniqueness
        if (roomRepository.existsByNumber(number)) {
            throw new BusinessRuleException("Room number already exists: " + number);
        }
        
        Room room = new Room();
        room.setNumber(number);
        room.setRoomType(roomType);
        room.setPrice(price);
        room.setCurrency(currency);
        room.setCapacity(capacity);
        room.setDescription(description);
        room.setStatus(RoomStatus.AVAILABLE);
        
        return roomRepository.save(room);
    }

    /**
     * Get room by ID
     */
    @Transactional(readOnly = true)
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));
    }

    /**
     * Get room by room number
     */
    @Transactional(readOnly = true)
    public Room getRoomByNumber(String number) {
        return roomRepository.findByNumber(number)
            .orElseThrow(() -> new NotFoundException("Room not found with number: " + number));
    }

    /**
     * Update room information
     */
    public Room updateRoom(Long roomId, String number, RoomType roomType, 
                          BigDecimal price, String currency, Integer capacity, String description, RoomStatus status) {
        Room room = getRoomById(roomId);
        
        // Check if room number is being changed and if it's unique
        if (!room.getNumber().equals(number) && roomRepository.existsByNumber(number)) {
            throw new BusinessRuleException("Room number already exists: " + number);
        }
        
        room.setNumber(number);
        room.setRoomType(roomType);
        room.setPrice(price);
        room.setCurrency(currency);
        room.setCapacity(capacity);
        room.setDescription(description);
        room.setStatus(status);
        
        return roomRepository.save(room);
    }

    /**
     * Delete room (soft delete by setting status to OUT_OF_SERVICE)
     */
    public void deleteRoom(Long roomId) {
        Room room = getRoomById(roomId);
        room.setStatus(RoomStatus.OUT_OF_SERVICE);
        roomRepository.save(room);
    }

    /**
     * Get all rooms with pagination
     */
    @Transactional(readOnly = true)
    public Page<Room> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    // ========== ROOM AVAILABILITY ==========

    /**
     * Check if room is available for given dates
     */
    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = getRoomById(roomId);
        
        // Check if room is out of service or occupied
        if (room.getStatus() == RoomStatus.OUT_OF_SERVICE || room.getStatus() == RoomStatus.OCCUPIED) {
            return false;
        }
        
        // Check for overlapping reservations
        List<Reservation> overlappingReservations = reservationRepository
            .checkRoomAvailability(roomId, checkIn, checkOut);
        
        return overlappingReservations.isEmpty();
    }

    /**
     * Get available rooms for given dates
     */
    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms(LocalDate checkIn, LocalDate checkOut, 
                                       RoomType roomType, Integer minCapacity) {
        return roomRepository.findAvailableRooms(checkIn, checkOut, roomType, minCapacity);
    }

    /**
     * Get room availability status
     */
    @Transactional(readOnly = true)
    public String getRoomAvailabilityStatus(Long roomId) {
        Room room = getRoomById(roomId);
        
        if (room.getStatus() == RoomStatus.OUT_OF_SERVICE) {
            return "OUT_OF_SERVICE";
        }
        
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            return "OCCUPIED";
        }
        
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            return "AVAILABLE";
        }
        
        return "UNKNOWN";
    }

    // ========== ROOM STATUS MANAGEMENT ==========

    /**
     * Update room status
     */
    @Transactional
    public Room updateRoomStatus(Long roomId, RoomStatus newStatus, User updatedBy) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found with id: " + roomId));
        
        RoomStatus oldStatus = room.getStatus();
        room.setStatus(newStatus);
        Room savedRoom = roomRepository.save(room);
        
        // Log status update
        logRoomStatusUpdate(room, oldStatus, newStatus, "Status updated", updatedBy);
        
        return savedRoom;
    }

    /**
     * Update room status with notes and reason
     */
    public Room updateRoomStatus(Long roomId, RoomStatus newStatus, User updatedBy, String notes, String updateReason) {
        Room room = getRoomById(roomId);
        RoomStatus oldStatus = room.getStatus();
        
        room.setStatus(newStatus);
        Room savedRoom = roomRepository.save(room);
        
        // Log status change with user context, notes, and reason
        logRoomStatusUpdateWithDetails(room, oldStatus, newStatus, updateReason, updatedBy, notes);
        
        return savedRoom;
    }

    /**
     * Mark room as available
     */
    public Room markRoomAsAvailable(Long roomId, User updatedBy) {
        return updateRoomStatus(roomId, RoomStatus.AVAILABLE, updatedBy);
    }

    /**
     * Mark room as occupied
     */
    public Room markRoomAsOccupied(Long roomId, User updatedBy) {
        return updateRoomStatus(roomId, RoomStatus.OCCUPIED, updatedBy);
    }

    /**
     * Mark room as out of service
     */
    public Room markRoomAsOutOfService(Long roomId, String reason, User updatedBy) {
        Room room = getRoomById(roomId);
        RoomStatus oldStatus = room.getStatus();
        
        room.setStatus(RoomStatus.OUT_OF_SERVICE);
        Room savedRoom = roomRepository.save(room);
        
        // Log status change with reason and user context
        logRoomStatusUpdate(room, oldStatus, RoomStatus.OUT_OF_SERVICE, reason, updatedBy);
        
        return savedRoom;
    }

    // ========== AUTOMATIC STATUS UPDATES (SYSTEM-TRIGGERED) ==========

    /**
     * Update room status automatically (system-triggered)
     * Used for reservation lifecycle events, check-in/check-out, etc.
     */
    public Room updateRoomStatusAutomatically(Long roomId, RoomStatus newStatus, String reason) {
        Room room = getRoomById(roomId);
        RoomStatus oldStatus = room.getStatus();
        
        room.setStatus(newStatus);
        Room savedRoom = roomRepository.save(room);
        
        // Log automatic status change
        logAutomaticStatusUpdate(room, oldStatus, newStatus, reason);
        
        return savedRoom;
    }

    /**
     * Mark room as occupied automatically (check-in)
     */
    public Room markRoomAsOccupiedAutomatically(Long roomId, String reason) {
        return updateRoomStatusAutomatically(roomId, RoomStatus.OCCUPIED, reason);
    }

    /**
     * Mark room as available automatically (check-out)
     */
    public Room markRoomAsAvailableAutomatically(Long roomId, String reason) {
        return updateRoomStatusAutomatically(roomId, RoomStatus.AVAILABLE, reason);
    }

    // ========== EQUIPMENT MANAGEMENT ==========

    /**
     * Add equipment to room
     */
    public Equipment addEquipmentToRoom(Long roomId, String name, EquipmentType type, 
                                      String description, Integer quantity) {
        Room room = getRoomById(roomId);
        
        Equipment equipment = new Equipment(name, type, quantity, null, null);
        equipment.setDescription(description);
        
        return equipmentRepository.save(equipment);
    }

    /**
     * Get room equipment
     * Note: Equipment is not tied to specific rooms in this design
     * This method returns all available equipment
     */
    @Transactional(readOnly = true)
    public List<Equipment> getRoomEquipment(Long roomId) {
        // Verify room exists
        getRoomById(roomId);
        // Return all available equipment since equipment is not room-specific
        return equipmentRepository.findByIsActiveTrue();
    }

    /**
     * Update equipment
     */
    public Equipment updateEquipment(Long equipmentId, String name, EquipmentType type, 
                                   String description, Integer quantity) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
            .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + equipmentId));
        
        equipment.setName(name);
        equipment.setEquipmentType(type);
        equipment.setDescription(description);
        equipment.setQuantity(quantity);
        
        return equipmentRepository.save(equipment);
    }

    /**
     * Remove equipment from room
     */
    public void removeEquipment(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
            .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + equipmentId));
        
        equipmentRepository.delete(equipment);
    }

    // ========== PHOTO MANAGEMENT ==========

    /**
     * Add photo to room
     */
    public RoomPhoto addRoomPhoto(Long roomId, String imageUrl, String caption, boolean isPrimary) {
        Room room = getRoomById(roomId);
        
        // If this is set as primary, unset other primary photos
        if (isPrimary) {
            Optional<RoomPhoto> existingPrimary = roomPhotoRepository.findByRoomAndIsPrimaryTrue(room);
            if (existingPrimary.isPresent()) {
                RoomPhoto primaryPhoto = existingPrimary.get();
                primaryPhoto.setIsPrimary(false);
                roomPhotoRepository.save(primaryPhoto);
            }
        }
        
        // Get next display order
        Integer nextOrder = roomPhotoRepository.findNextDisplayOrderForRoom(room);
        if (nextOrder == null) {
            nextOrder = 1;
        }
        
        RoomPhoto photo = new RoomPhoto(imageUrl, room, nextOrder, caption);
        photo.setIsPrimary(isPrimary);
        
        return roomPhotoRepository.save(photo);
    }

    /**
     * Get room photos
     */
    @Transactional(readOnly = true)
    public List<RoomPhoto> getRoomPhotos(Long roomId) {
        Room room = getRoomById(roomId);
        return roomPhotoRepository.findByRoomOrderByDisplayOrderAsc(room);
    }

    /**
     * Set primary photo
     */
    public RoomPhoto setPrimaryPhoto(Long photoId) {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
            .orElseThrow(() -> new NotFoundException("Photo not found with ID: " + photoId));
        
        // Unset other primary photos for this room
        Optional<RoomPhoto> existingPrimary = roomPhotoRepository.findByRoomAndIsPrimaryTrue(photo.getRoom());
        if (existingPrimary.isPresent()) {
            RoomPhoto primaryPhoto = existingPrimary.get();
            primaryPhoto.setIsPrimary(false);
            roomPhotoRepository.save(primaryPhoto);
        }
        
        // Set this photo as primary
        photo.setIsPrimary(true);
        return roomPhotoRepository.save(photo);
    }

    /**
     * Remove room photo
     */
    public void removeRoomPhoto(Long photoId) {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
            .orElseThrow(() -> new NotFoundException("Photo not found with ID: " + photoId));
        
        roomPhotoRepository.delete(photo);
    }

    // ========== SEARCH AND FILTERING ==========

    /**
     * Search rooms by criteria
     */
    @Transactional(readOnly = true)
    public Page<Room> searchRooms(RoomType roomType, BigDecimal minPrice, BigDecimal maxPrice, 
                                 Integer minCapacity, RoomStatus status, Pageable pageable) {
        return roomRepository.findByCriteria(roomType, minCapacity, maxPrice, status, pageable);
    }

    /**
     * Get rooms by type
     */
    @Transactional(readOnly = true)
    public List<Room> getRoomsByType(RoomType roomType) {
        return roomRepository.findByRoomType(roomType);
    }

    /**
     * Get rooms by status
     */
    @Transactional(readOnly = true)
    public List<Room> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status);
    }

    /**
     * Get rooms by price range
     */
    @Transactional(readOnly = true)
    public List<Room> getRoomsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return roomRepository.findByPriceBetween(minPrice, maxPrice);
    }

    // ========== STATISTICS AND REPORTING ==========

    /**
     * Get room statistics
     */
    @Transactional(readOnly = true)
    public RoomStatistics getRoomStatistics() {
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.countByStatus(RoomStatus.AVAILABLE);
        long occupiedRooms = roomRepository.countByStatus(RoomStatus.OCCUPIED);
        long outOfServiceRooms = roomRepository.countByStatus(RoomStatus.OUT_OF_SERVICE);
        
        return new RoomStatistics(totalRooms, availableRooms, occupiedRooms, outOfServiceRooms);
    }

    /**
     * Get room status history
     */
    @Transactional(readOnly = true)
    public List<RoomStatusUpdate> getRoomStatusHistory(Long roomId) {
        Room room = getRoomById(roomId);
        return roomStatusUpdateRepository.findByRoomOrderByUpdatedAtDesc(room);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Log room status update with user context
     */
    private void logRoomStatusUpdate(Room room, RoomStatus oldStatus, RoomStatus newStatus, String reason, User updatedBy) {
        RoomStatusUpdate statusUpdate = new RoomStatusUpdate();
        statusUpdate.setRoom(room);
        statusUpdate.setPreviousStatus(oldStatus);
        statusUpdate.setNewStatus(newStatus);
        statusUpdate.setUpdateReason(reason);
        statusUpdate.setUpdatedBy(updatedBy);
        statusUpdate.setIsAutomatic(false); // Manual update
        
        roomStatusUpdateRepository.save(statusUpdate);
    }

    private void logRoomStatusUpdateWithDetails(Room room, RoomStatus oldStatus, RoomStatus newStatus, String reason, User updatedBy, String notes) {
        RoomStatusUpdate statusUpdate = new RoomStatusUpdate();
        statusUpdate.setRoom(room);
        statusUpdate.setPreviousStatus(oldStatus);
        statusUpdate.setNewStatus(newStatus);
        statusUpdate.setUpdateReason(reason);
        statusUpdate.setUpdatedBy(updatedBy);
        statusUpdate.setNotes(notes);
        statusUpdate.setIsAutomatic(false); // Manual update
        
        roomStatusUpdateRepository.save(statusUpdate);
    }

    /**
     * Log automatic room status update (system-triggered)
     */
    private void logAutomaticStatusUpdate(Room room, RoomStatus oldStatus, RoomStatus newStatus, String reason) {
        // Get or create system user for automatic updates
        User systemUser = getSystemUser();
        
        RoomStatusUpdate statusUpdate = new RoomStatusUpdate();
        statusUpdate.setRoom(room);
        statusUpdate.setPreviousStatus(oldStatus);
        statusUpdate.setNewStatus(newStatus);
        statusUpdate.setUpdateReason(reason);
        statusUpdate.setUpdatedBy(systemUser);
        statusUpdate.setIsAutomatic(true); // Mark as automatic update
        
        roomStatusUpdateRepository.save(statusUpdate);
    }

    /**
     * Get or create system user for automatic updates
     */
    private User getSystemUser() {
        // Try to find existing system user
        Optional<User> systemUser = userRepository.findByEmail("system@hotel.com");
        
        if (systemUser.isPresent()) {
            return systemUser.get();
        }
        
        // Create system user if it doesn't exist
        User newSystemUser = new User();
        newSystemUser.setEmail("system@hotel.com");
        newSystemUser.setFirstName("System");
        newSystemUser.setLastName("User");
        newSystemUser.setPhone("+1234567890"); // System user phone
        newSystemUser.setAddress("Hotel System"); // System user address
        newSystemUser.setBirthDate(java.time.LocalDate.of(2000, 1, 1)); // System user birth date
        newSystemUser.setRole(Role.ADMIN);
        newSystemUser.setPassword("SystemUser123@"); // Valid password for system user
        
        return userRepository.save(newSystemUser);
    }

    // ========== INNER CLASSES ==========

    /**
     * Room statistics data class
     */
    public static class RoomStatistics {
        private final long totalRooms;
        private final long availableRooms;
        private final long occupiedRooms;
        private final long outOfServiceRooms;

        public RoomStatistics(long totalRooms, long availableRooms, long occupiedRooms, 
                            long outOfServiceRooms) {
            this.totalRooms = totalRooms;
            this.availableRooms = availableRooms;
            this.occupiedRooms = occupiedRooms;
            this.outOfServiceRooms = outOfServiceRooms;
        }

        // Getters
        public long getTotalRooms() { return totalRooms; }
        public long getAvailableRooms() { return availableRooms; }
        public long getOccupiedRooms() { return occupiedRooms; }
        public long getOutOfServiceRooms() { return outOfServiceRooms; }
    }

    // ========== ADDITIONAL METHODS FOR CONTROLLER ==========

    /**
     * Get available rooms for given dates with pagination
     */
    @Transactional(readOnly = true)
    public Page<Room> getAvailableRoomsForDateRange(LocalDate checkIn, LocalDate checkOut, Pageable pageable) {
        return roomRepository.findAvailableRoomsForDateRange(checkIn, checkOut, pageable);
    }

    /**
     * Get available rooms for given dates with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<Room> getAvailableRoomsForDateRangeWithFilters(LocalDate checkIn, LocalDate checkOut, 
                                                          RoomType roomType, Integer minCapacity, Pageable pageable) {
        return roomRepository.findAvailableRoomsForDateRangeWithFilters(checkIn, checkOut, roomType, minCapacity, pageable);
    }

    /**
     * Get rooms by criteria with pagination
     */
    @Transactional(readOnly = true)
    public Page<Room> getRoomsByCriteria(RoomType roomType, Integer minCapacity, 
                                   BigDecimal maxPrice, RoomStatus status, Pageable pageable) {
        return roomRepository.findByCriteria(roomType, minCapacity, maxPrice, status, pageable);
    }

    /**
     * Get rooms by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<Room> getRoomsByStatus(RoomStatus status, Pageable pageable) {
        return roomRepository.findByStatus(status, pageable);
    }

    /**
     * Get rooms by room type with pagination
     */
    @Transactional(readOnly = true)
    public Page<Room> getRoomsByType(RoomType roomType, Pageable pageable) {
        return roomRepository.findByRoomType(roomType, pageable);
    }

    /**
     * Get user by username for audit trails
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByEmail(username)
            .orElseThrow(() -> new NotFoundException("User not found: " + username));
    }
}