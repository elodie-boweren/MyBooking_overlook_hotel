package com.MyBooking.room.repository;

import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import com.MyBooking.hotel_management.HotelManagementApplication;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;




import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@ContextConfiguration(classes = HotelManagementApplication.class)
@EntityScan({"com.MyBooking.room.domain", "com.MyBooking.reservation.domain", "com.MyBooking.auth.domain"})
@EnableJpaRepositories("com.MyBooking.room.repository")

class RoomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoomRepository roomRepository;

    private Room singleRoom;
    private Room doubleRoom;
    private Room deluxeRoom;
    private Room familyRoom;
    private Room occupiedRoom;
    private Room outOfServiceRoom;

    @BeforeEach
    @Transactional
    @Rollback 
    void setUp() {
        // Create test rooms with different types and statuses
        singleRoom = new Room("101", RoomType.SINGLE, 1, new BigDecimal("100.00"), "USD", RoomStatus.AVAILABLE);
        singleRoom.setDescription("Cozy single room");
        singleRoom.setEquipment("WiFi, TV, Mini-fridge");
        entityManager.persistAndFlush(singleRoom);

        doubleRoom = new Room("102", RoomType.DOUBLE, 2, new BigDecimal("150.00"), "USD", RoomStatus.AVAILABLE);
        doubleRoom.setDescription("Comfortable double room");
        doubleRoom.setEquipment("WiFi, TV, Mini-fridge, Balcony");
        entityManager.persistAndFlush(doubleRoom);

        deluxeRoom = new Room("201", RoomType.DELUXE, 2, new BigDecimal("250.00"), "USD", RoomStatus.AVAILABLE);
        deluxeRoom.setDescription("Luxurious deluxe room");
        deluxeRoom.setEquipment("WiFi, Smart TV, Mini-bar, Balcony, Jacuzzi");
        entityManager.persistAndFlush(deluxeRoom);

        familyRoom = new Room("301", RoomType.FAMILY, 4, new BigDecimal("300.00"), "USD", RoomStatus.AVAILABLE);
        familyRoom.setDescription("Spacious family room");
        familyRoom.setEquipment("WiFi, TV, Mini-fridge, Sofa bed");
        entityManager.persistAndFlush(familyRoom);

        occupiedRoom = new Room("103", RoomType.DOUBLE, 2, new BigDecimal("150.00"), "USD", RoomStatus.OCCUPIED);
        occupiedRoom.setDescription("Currently occupied room");
        entityManager.persistAndFlush(occupiedRoom);

        outOfServiceRoom = new Room("104", RoomType.SINGLE, 1, new BigDecimal("100.00"), "USD", RoomStatus.OUT_OF_SERVICE);
        outOfServiceRoom.setDescription("Room under maintenance");
        entityManager.persistAndFlush(outOfServiceRoom);
    }

    @Test
    void testFindByNumber() {
        // When
        Optional<Room> found = roomRepository.findByNumber("101");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRoomType()).isEqualTo(RoomType.SINGLE);
        assertThat(found.get().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void testFindByNumberNotFound() {
        // When
        Optional<Room> found = roomRepository.findByNumber("999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testExistsByNumber() {
        // When & Then
        assertThat(roomRepository.existsByNumber("101")).isTrue();
        assertThat(roomRepository.existsByNumber("999")).isFalse();
    }

    @Test
    void testFindByRoomType() {
        // When
        List<Room> singleRooms = roomRepository.findByRoomType(RoomType.SINGLE);

        // Then
        assertThat(singleRooms).hasSize(2); // singleRoom and outOfServiceRoom
        assertThat(singleRooms).extracting(Room::getNumber).containsExactlyInAnyOrder("101", "104");
    }

    @Test
    void testFindByRoomTypeWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Room> page = roomRepository.findByRoomType(RoomType.SINGLE, pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void testFindByStatus() {
        // When
        List<Room> availableRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);

        // Then
        assertThat(availableRooms).hasSize(4); // singleRoom, doubleRoom, deluxeRoom, familyRoom
        assertThat(availableRooms).extracting(Room::getStatus).containsOnly(RoomStatus.AVAILABLE);
    }

    @Test
    void testFindByStatusWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Room> page = roomRepository.findByStatus(RoomStatus.AVAILABLE, pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByCapacityGreaterThanEqual() {
        // When
        List<Room> rooms = roomRepository.findByCapacityGreaterThanEqual(2);

        // Then
        assertThat(rooms).hasSize(4); // doubleRoom, deluxeRoom, familyRoom, occupiedRoom
        assertThat(rooms).extracting(Room::getCapacity).allMatch(capacity -> capacity >= 2);
    }

    @Test
    void testFindByCapacityBetween() {
        // When
        List<Room> rooms = roomRepository.findByCapacityBetween(2, 3);

        // Then
        assertThat(rooms).hasSize(3); // doubleRoom, deluxeRoom, occupiedRoom
        assertThat(rooms).extracting(Room::getCapacity).allMatch(capacity -> capacity >= 2 && capacity <= 3);
    }

    @Test
    void testFindByPriceBetween() {
        // When
        List<Room> rooms = roomRepository.findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00"));

        // Then
        assertThat(rooms).hasSize(4); // singleRoom, doubleRoom, occupiedRoom, outOfServiceRoom
        assertThat(rooms).extracting(Room::getPrice).allMatch(price -> 
            price.compareTo(new BigDecimal("100.00")) >= 0 && 
            price.compareTo(new BigDecimal("200.00")) <= 0);
    }

    @Test
    void testFindByPriceBetweenWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Room> page = roomRepository.findByPriceBetween(
            new BigDecimal("100.00"), new BigDecimal("200.00"), pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByCurrency() {
        // When
        List<Room> rooms = roomRepository.findByCurrency("USD");

        // Then
        assertThat(rooms).hasSize(6); // All test rooms use USD
        assertThat(rooms).extracting(Room::getCurrency).containsOnly("USD");
    }

    @Test
    void testFindByEquipmentContainingIgnoreCase() {
        // When
        List<Room> rooms = roomRepository.findByEquipmentContainingIgnoreCase("balcony");

        // Then
        assertThat(rooms).hasSize(2); // doubleRoom and deluxeRoom
        assertThat(rooms).extracting(Room::getNumber).containsExactlyInAnyOrder("102", "201");
    }

    @Test
    void testFindByDescriptionContainingIgnoreCase() {
        // When
        List<Room> rooms = roomRepository.findByDescriptionContainingIgnoreCase("luxurious");

        // Then
        assertThat(rooms).hasSize(1); // deluxeRoom
        assertThat(rooms.get(0).getNumber()).isEqualTo("201");
    }

    @Test
    void testFindByRoomTypeAndStatus() {
        // When
        List<Room> rooms = roomRepository.findByRoomTypeAndStatus(RoomType.SINGLE, RoomStatus.AVAILABLE);

        // Then
        assertThat(rooms).hasSize(1); // Only singleRoom
        assertThat(rooms.get(0).getNumber()).isEqualTo("101");
    }

    @Test
    void testFindByRoomTypeAndStatusWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Room> page = roomRepository.findByRoomTypeAndStatus(RoomType.SINGLE, RoomStatus.AVAILABLE, pageable);

        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getNumber()).isEqualTo("101");
    }

    @Test
    void testFindByCriteria() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When - search for available rooms with capacity >= 2 and price <= 200
        Page<Room> page = roomRepository.findByCriteria(
            null, // any room type
            2,    // min capacity
            new BigDecimal("200.00"), // max price
            RoomStatus.AVAILABLE, // status
            pageable
        );

        // Then
        assertThat(page.getContent()).hasSize(1); // doubleRoom only (deluxeRoom price > 200)
        assertThat(page.getContent()).extracting(Room::getNumber).containsExactlyInAnyOrder("102");
    }

    @Test
    void testCountByStatus() {
        // When & Then
        assertThat(roomRepository.countByStatus(RoomStatus.AVAILABLE)).isEqualTo(4);
        assertThat(roomRepository.countByStatus(RoomStatus.OCCUPIED)).isEqualTo(1);
        assertThat(roomRepository.countByStatus(RoomStatus.OUT_OF_SERVICE)).isEqualTo(1);
    }

    @Test
    void testCountByRoomType() {
        // When & Then
        assertThat(roomRepository.countByRoomType(RoomType.SINGLE)).isEqualTo(2);
        assertThat(roomRepository.countByRoomType(RoomType.DOUBLE)).isEqualTo(2);
        assertThat(roomRepository.countByRoomType(RoomType.DELUXE)).isEqualTo(1);
        assertThat(roomRepository.countByRoomType(RoomType.FAMILY)).isEqualTo(1);
    }

    @Test
    void testFindByCreatedAtBetween() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Room> page = roomRepository.findByCreatedAtBetween(start, end, pageable);

        // Then
        assertThat(page.getContent()).hasSize(6); // All test rooms created in setUp
    }

    @Test
    void testFindByCreatedAtAfter() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Room> page = roomRepository.findByCreatedAtAfter(since, pageable);

        // Then
        assertThat(page.getContent()).hasSize(6); // All test rooms created in setUp
    }

    @Test
    void testFindCheapestAvailableRoom() {
        // When
        Optional<Room> cheapest = roomRepository.findCheapestAvailableRoom();

        // Then
        assertThat(cheapest).isPresent();
        assertThat(cheapest.get().getPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(cheapest.get().getNumber()).isEqualTo("101");
    }

    @Test
    void testFindMostExpensiveRoom() {
        // When
        Optional<Room> mostExpensive = roomRepository.findMostExpensiveRoom();

        // Then
        assertThat(mostExpensive).isPresent();
        assertThat(mostExpensive.get().getPrice()).isEqualTo(new BigDecimal("300.00"));
        assertThat(mostExpensive.get().getNumber()).isEqualTo("301");
    }

    @Test
    void testFindAvailableRoomsForDateRange() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        // When - Returns all operational rooms (status != OUT_OF_SERVICE) since there are no overlapping reservations
        List<Room> availableRooms = roomRepository.findAvailableRoomsForDateRange(checkIn, checkOut);

        // Then - Should include 4 AVAILABLE rooms + 1 OCCUPIED room (5 total), strictly excluding OUT_OF_SERVICE
        assertThat(availableRooms).hasSize(5);
        assertThat(availableRooms).extracting(Room::getStatus)
            .doesNotContain(RoomStatus.OUT_OF_SERVICE);
    }

    @Test
    void testFindAvailableRoomsForDateRangeWithPagination() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Room> page = roomRepository.findAvailableRoomsForDateRange(checkIn, checkOut, pageable);

        // Then - 5 operational rooms with page size 2 gives 3 pages
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }
}