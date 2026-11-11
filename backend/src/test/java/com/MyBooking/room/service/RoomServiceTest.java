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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    
    @Mock
    private EquipmentRepository equipmentRepository;
    
    @Mock
    private RoomPhotoRepository roomPhotoRepository;
    
    @Mock
    private RoomStatusUpdateRepository roomStatusUpdateRepository;
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private RoomService roomService;
    
    private Room testRoom;
    private Equipment testEquipment;
    private RoomPhoto testPhoto;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Setup test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber("101");
        testRoom.setRoomType(RoomType.DELUXE);
        testRoom.setPrice(BigDecimal.valueOf(150.00));
        testRoom.setCurrency("USD");
        testRoom.setCapacity(2);
        testRoom.setDescription("Deluxe room with ocean view");
        testRoom.setStatus(RoomStatus.AVAILABLE);
        
        // Setup test equipment
        testEquipment = new Equipment("TV", EquipmentType.ELECTRONICS, 1, "Samsung", "Smart TV");
        testEquipment.setId(1L);
        testEquipment.setDescription("55-inch Smart TV");
        
        // Setup test photo
        testPhoto = new RoomPhoto("https://example.com/room101.jpg", testRoom, 1, "Deluxe room view");
        testPhoto.setId(1L);
        testPhoto.setIsPrimary(true);
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@hotel.com");
        testUser.setFirstName("Admin");
        testUser.setLastName("User");
        testUser.setRole(Role.ADMIN);
    }

    // ========== ROOM MANAGEMENT TESTS ==========
    
    @Test
    void createRoom_WithValidData_ShouldCreateAndReturnRoom() {
        // Given
        String number = "102";
        RoomType roomType = RoomType.SINGLE;
        BigDecimal price = BigDecimal.valueOf(100.00);
        String currency = "USD";
        Integer capacity = 1;
        String description = "Single room";
        
        when(roomRepository.existsByNumber(number)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        
        // When
        Room result = roomService.createRoom(number, roomType, price, currency, capacity, description);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo("101");
        assertThat(result.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        
        verify(roomRepository).existsByNumber(number);
        verify(roomRepository).save(any(Room.class));
    }
    
    @Test
    void createRoom_WithExistingNumber_ShouldThrowBusinessRuleException() {
        // Given
        String number = "101";
        when(roomRepository.existsByNumber(number)).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> roomService.createRoom(number, RoomType.SINGLE, 
            BigDecimal.valueOf(100.00), "USD", 1, "Description"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Room number already exists: " + number);
        
        verify(roomRepository).existsByNumber(number);
        verify(roomRepository, never()).save(any(Room.class));
    }
    
    @Test
    void getRoomById_WithValidId_ShouldReturnRoom() {
        // Given
        Long roomId = 1L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        // When
        Room result = roomService.getRoomById(roomId);
        
        // Then
        assertThat(result).isEqualTo(testRoom);
        verify(roomRepository).findById(roomId);
    }
    
    @Test
    void getRoomById_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long roomId = 999L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> roomService.getRoomById(roomId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Room not found with ID: " + roomId);
    }
    
    @Test
    void updateRoom_WithValidData_ShouldUpdateAndReturnRoom() {
        // Given
        Long roomId = 1L;
        String newNumber = "102";
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.existsByNumber(newNumber)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        
        // When
        Room result = roomService.updateRoom(roomId, newNumber, RoomType.SINGLE, 
            BigDecimal.valueOf(120.00), "USD", 1, "Updated description", RoomStatus.AVAILABLE);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).existsByNumber(newNumber);
        verify(roomRepository).save(testRoom);
    }
    
    @Test
    void deleteRoom_ShouldSetStatusToOutOfService() {
        // Given
        Long roomId = 1L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        
        // When
        roomService.deleteRoom(roomId);
        
        // Then
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
    }

    // ========== ROOM AVAILABILITY TESTS ==========
    
    @Test
    void isRoomAvailable_WithAvailableRoom_ShouldReturnTrue() {
        // Given
        Long roomId = 1L;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(reservationRepository.checkRoomAvailability(roomId, checkIn, checkOut))
            .thenReturn(Arrays.asList());
        
        // When
        boolean result = roomService.isRoomAvailable(roomId, checkIn, checkOut);
        
        // Then
        assertThat(result).isTrue();
        verify(roomRepository).findById(roomId);
        verify(reservationRepository).checkRoomAvailability(roomId, checkIn, checkOut);
    }
    
    @Test
    void isRoomAvailable_WithOutOfServiceRoom_ShouldReturnFalse() {
        // Given
        Long roomId = 1L;
        testRoom.setStatus(RoomStatus.OUT_OF_SERVICE);
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        
        // When
        boolean result = roomService.isRoomAvailable(roomId, checkIn, checkOut);
        
        // Then
        assertThat(result).isFalse();
        verify(roomRepository).findById(roomId);
        verify(reservationRepository, never()).checkRoomAvailability(anyLong(), any(), any());
    }

    @Test
    void isRoomAvailable_WithOccupiedRoom_ShouldReturnTrueWhenNoOverlappingReservations() {
        // Given - Room physically occupied today, but no overlapping reservation on requested future dates
        Long roomId = 1L;
        testRoom.setStatus(RoomStatus.OCCUPIED);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(10);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(reservationRepository.checkRoomAvailability(roomId, checkIn, checkOut))
            .thenReturn(List.of());
        
        // When
        boolean result = roomService.isRoomAvailable(roomId, checkIn, checkOut);
        
        // Then - Decoupled dynamic availability allows booking
        assertThat(result).isTrue();
        verify(roomRepository).findById(roomId);
        verify(reservationRepository).checkRoomAvailability(roomId, checkIn, checkOut);
    }
    
    @Test
    void isRoomAvailable_WithOverlappingReservations_ShouldReturnFalse() {
        // Given
        Long roomId = 1L;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        Reservation overlappingReservation = new Reservation();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(reservationRepository.checkRoomAvailability(roomId, checkIn, checkOut))
            .thenReturn(Arrays.asList(overlappingReservation));
        
        // When
        boolean result = roomService.isRoomAvailable(roomId, checkIn, checkOut);
        
        // Then
        assertThat(result).isFalse();
        verify(roomRepository).findById(roomId);
        verify(reservationRepository).checkRoomAvailability(roomId, checkIn, checkOut);
    }

    // ========== ROOM STATUS MANAGEMENT TESTS ==========
    
    @Test
    void updateRoomStatus_WithValidStatus_ShouldUpdateRoomAndLogChange() {
        // Given
        Long roomId = 1L;
        RoomStatus newStatus = RoomStatus.OCCUPIED;
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomStatusUpdateRepository.save(any(RoomStatusUpdate.class))).thenReturn(new RoomStatusUpdate());
        
        // When
        Room result = roomService.updateRoomStatus(roomId, newStatus, testUser);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
        verify(roomStatusUpdateRepository).save(any(RoomStatusUpdate.class));
    }
    
    @Test
    void markRoomAsAvailable_ShouldSetStatusToAvailable() {
        // Given
        Long roomId = 1L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomStatusUpdateRepository.save(any(RoomStatusUpdate.class))).thenReturn(new RoomStatusUpdate());
        
        // When
        Room result = roomService.markRoomAsAvailable(roomId, testUser);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
        verify(roomStatusUpdateRepository).save(any(RoomStatusUpdate.class));
    }
    
    @Test
    void markRoomAsOutOfService_ShouldSetStatusToOutOfServiceAndLogReason() {
        // Given
        Long roomId = 1L;
        String reason = "Maintenance required";
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomStatusUpdateRepository.save(any(RoomStatusUpdate.class))).thenReturn(new RoomStatusUpdate());
        
        // When
        Room result = roomService.markRoomAsOutOfService(roomId, reason, testUser);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
        verify(roomStatusUpdateRepository).save(any(RoomStatusUpdate.class));
    }

    // ========== AUTOMATIC STATUS UPDATE TESTS ==========
    
    @Test
    void updateRoomStatusAutomatically_ShouldUpdateRoomAndLogAsAutomatic() {
        // Given
        Long roomId = 1L;
        RoomStatus newStatus = RoomStatus.OCCUPIED;
        String reason = "Guest checked in";
        
        User systemUser = new User();
        systemUser.setId(999L);
        systemUser.setEmail("system@hotel.com");
        systemUser.setRole(Role.ADMIN);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(userRepository.findByEmail("system@hotel.com")).thenReturn(Optional.of(systemUser));
        when(roomStatusUpdateRepository.save(any(RoomStatusUpdate.class))).thenReturn(new RoomStatusUpdate());
        
        // When
        Room result = roomService.updateRoomStatusAutomatically(roomId, newStatus, reason);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
        verify(userRepository).findByEmail("system@hotel.com");
        verify(roomStatusUpdateRepository).save(any(RoomStatusUpdate.class));
    }
    
    @Test
    void markRoomAsOccupiedAutomatically_ShouldUpdateRoomStatus() {
        // Given
        Long roomId = 1L;
        String reason = "Guest checked in - Reservation ID: 123";
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomStatusUpdateRepository.save(any(RoomStatusUpdate.class))).thenReturn(new RoomStatusUpdate());
        
        // When
        Room result = roomService.markRoomAsOccupiedAutomatically(roomId, reason);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
        verify(roomStatusUpdateRepository).save(any(RoomStatusUpdate.class));
    }
    
    @Test
    void markRoomAsAvailableAutomatically_ShouldUpdateRoomStatus() {
        // Given
        Long roomId = 1L;
        String reason = "Guest checked out - Reservation ID: 123";
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomStatusUpdateRepository.save(any(RoomStatusUpdate.class))).thenReturn(new RoomStatusUpdate());
        
        // When
        Room result = roomService.markRoomAsAvailableAutomatically(roomId, reason);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(testRoom);
        verify(roomStatusUpdateRepository).save(any(RoomStatusUpdate.class));
    }

    // ========== EQUIPMENT MANAGEMENT TESTS ==========
    
    @Test
    void addEquipmentToRoom_WithValidData_ShouldCreateEquipment() {
        // Given
        Long roomId = 1L;
        String name = "Mini Bar";
        EquipmentType type = EquipmentType.APPLIANCES;
        String description = "Stocked mini bar";
        Integer quantity = 1;
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(testEquipment);
        
        // When
        Equipment result = roomService.addEquipmentToRoom(roomId, name, type, description, quantity);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(equipmentRepository).save(any(Equipment.class));
    }
    
    @Test
    void getRoomEquipment_WithValidRoom_ShouldReturnEquipmentList() {
        // Given
        Long roomId = 1L;
        List<Equipment> equipmentList = Arrays.asList(testEquipment);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(equipmentRepository.findByIsActiveTrue()).thenReturn(equipmentList);
        
        // When
        List<Equipment> result = roomService.getRoomEquipment(roomId);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(testEquipment);
        verify(roomRepository).findById(roomId);
        verify(equipmentRepository).findByIsActiveTrue();
    }

    // ========== PHOTO MANAGEMENT TESTS ==========
    
    @Test
    void addRoomPhoto_WithValidData_ShouldCreatePhoto() {
        // Given
        Long roomId = 1L;
        String imageUrl = "https://example.com/photo.jpg";
        String caption = "Room view";
        boolean isPrimary = true;
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomPhotoRepository.findByRoomAndIsPrimaryTrue(testRoom)).thenReturn(Optional.empty());
        when(roomPhotoRepository.save(any(RoomPhoto.class))).thenReturn(testPhoto);
        
        // When
        RoomPhoto result = roomService.addRoomPhoto(roomId, imageUrl, caption, isPrimary);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).findById(roomId);
        verify(roomPhotoRepository).save(any(RoomPhoto.class));
    }
    
    @Test
    void setPrimaryPhoto_ShouldUnsetOtherPrimaryPhotos() {
        // Given
        Long photoId = 1L;
        RoomPhoto existingPrimary = new RoomPhoto();
        existingPrimary.setIsPrimary(true);
        
        when(roomPhotoRepository.findById(photoId)).thenReturn(Optional.of(testPhoto));
        when(roomPhotoRepository.findByRoomAndIsPrimaryTrue(testRoom)).thenReturn(Optional.of(existingPrimary));
        when(roomPhotoRepository.save(any(RoomPhoto.class))).thenReturn(testPhoto);
        
        // When
        RoomPhoto result = roomService.setPrimaryPhoto(photoId);
        
        // Then
        assertThat(result).isNotNull();
        verify(roomPhotoRepository).findById(photoId);
        verify(roomPhotoRepository, times(2)).save(any(RoomPhoto.class)); // Unset old + set new
    }

    // ========== SEARCH AND FILTERING TESTS ==========
    
    @Test
    void getRoomsByType_ShouldReturnRoomsOfSpecifiedType() {
        // Given
        RoomType roomType = RoomType.DELUXE;
        List<Room> rooms = Arrays.asList(testRoom);
        
        when(roomRepository.findByRoomType(roomType)).thenReturn(rooms);
        
        // When
        List<Room> result = roomService.getRoomsByType(roomType);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(testRoom);
        verify(roomRepository).findByRoomType(roomType);
    }
    
    @Test
    void getRoomsByStatus_ShouldReturnRoomsOfSpecifiedStatus() {
        // Given
        RoomStatus status = RoomStatus.AVAILABLE;
        List<Room> rooms = Arrays.asList(testRoom);
        
        when(roomRepository.findByStatus(status)).thenReturn(rooms);
        
        // When
        List<Room> result = roomService.getRoomsByStatus(status);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(testRoom);
        verify(roomRepository).findByStatus(status);
    }

    // ========== STATISTICS TESTS ==========
    
    @Test
    void getRoomStatistics_ShouldReturnCorrectStatistics() {
        // Given
        when(roomRepository.count()).thenReturn(10L);
        when(roomRepository.countByStatus(RoomStatus.AVAILABLE)).thenReturn(5L);
        when(roomRepository.countByStatus(RoomStatus.OCCUPIED)).thenReturn(3L);
        when(roomRepository.countByStatus(RoomStatus.OUT_OF_SERVICE)).thenReturn(2L);
        
        // When
        RoomService.RoomStatistics result = roomService.getRoomStatistics();
        
        // Then
        assertThat(result.getTotalRooms()).isEqualTo(10L);
        assertThat(result.getAvailableRooms()).isEqualTo(5L);
        assertThat(result.getOccupiedRooms()).isEqualTo(3L);
        assertThat(result.getOutOfServiceRooms()).isEqualTo(2L);
        
        verify(roomRepository).count();
        verify(roomRepository).countByStatus(RoomStatus.AVAILABLE);
        verify(roomRepository).countByStatus(RoomStatus.OCCUPIED);
        verify(roomRepository).countByStatus(RoomStatus.OUT_OF_SERVICE);
    }
}