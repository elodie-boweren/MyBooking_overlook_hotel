package com.MyBooking.performance;

import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.event.service.EventService;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.installation.service.InstallationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HotelManagementApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class QueryPerformanceBenchmarkTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private InstallationService installationService;

    @Autowired
    private EventService eventService;

    @Test
    @DisplayName("P3-T09 & P3-T06: Availability & Event Statistics execute with O(1) SQL queries on 50 installations x 500 events")
    void benchmarkAvailabilityAndEventStatisticsQueryCount() {
        LocalDateTime base = LocalDateTime.now().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Installation targetFreeInstallation = null;

        for (int i = 0; i < 50; i++) {
            Installation inst = new Installation(
                    "Bench Hall " + i,
                    i % 2 == 0 ? InstallationType.CONFERENCE_ROOM : InstallationType.SPA_ROOM,
                    100,
                    new BigDecimal("250.00"),
                    "USD"
            );
            entityManager.persist(inst);
            if (i == 49) {
                targetFreeInstallation = inst;
            }
            for (int j = 0; j < 10; j++) {
                // Leave installation 49 free during [base, base+2h), occupy others in slot 0
                LocalDateTime start = (i < 49 && j == 0) ? base : base.plusDays(j + 1);
                Event ev = new Event(
                        "Bench Event " + i + "-" + j,
                        EventType.CONFERENCE,
                        start,
                        start.plusHours(2),
                        20,
                        new BigDecimal("100.00"),
                        "USD",
                        inst
                );
                entityManager.persist(ev);
            }
        }
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        // 1. InstallationService.findAvailableInstallations
        stats.clear();
        long t1 = System.nanoTime();
        List<Installation> freeInstallations = installationService.findAvailableInstallations(
                base, base.plusHours(2), 50, InstallationType.SPA_ROOM);
        long dt1Ms = (System.nanoTime() - t1) / 1_000_000;
        long q1 = stats.getPrepareStatementCount();
        assertThat(q1).isEqualTo(1L);
        assertThat(freeInstallations).isNotEmpty();

        // 2. EventService.getAvailableInstallations
        stats.clear();
        long t2 = System.nanoTime();
        List<Installation> freeFromEventSvc = eventService.getAvailableInstallations(
                base, base.plusHours(2), 50, InstallationType.SPA_ROOM);
        long dt2Ms = (System.nanoTime() - t2) / 1_000_000;
        long q2 = stats.getPrepareStatementCount();
        assertThat(q2).isEqualTo(1L);
        assertThat(freeFromEventSvc).extracting(Installation::getId).contains(targetFreeInstallation.getId());

        // 3. EventService.isInstallationAvailable
        stats.clear();
        long t3 = System.nanoTime();
        boolean available = eventService.isInstallationAvailable(targetFreeInstallation.getId(), base, base.plusHours(2));
        long dt3Ms = (System.nanoTime() - t3) / 1_000_000;
        long q3 = stats.getPrepareStatementCount();
        assertThat(available).isTrue();
        assertThat(q3).isEqualTo(1L);

        // 4. EventService.getEventStatistics
        stats.clear();
        long t4 = System.nanoTime();
        EventService.EventStatistics eventStats = eventService.getEventStatistics();
        long dt4Ms = (System.nanoTime() - t4) / 1_000_000;
        long q4 = stats.getPrepareStatementCount();
        assertThat(eventStats.getTotalEvents()).isGreaterThanOrEqualTo(500L);
        assertThat(q4).isEqualTo(3L);

        System.out.printf("[BENCHMARK] findAvailableInstallations: %d SQL (%d ms) | getAvailableInstallations: %d SQL (%d ms) | isInstallationAvailable: %d SQL (%d ms) | getEventStatistics: %d SQL (%d ms)%n",
                q1, dt1Ms, q2, dt2Ms, q3, dt3Ms, q4, dt4Ms);
    }
}

