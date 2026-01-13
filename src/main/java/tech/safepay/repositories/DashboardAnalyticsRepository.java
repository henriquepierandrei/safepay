package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.DashboardAnalytics;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardAnalyticsRepository extends JpaRepository<DashboardAnalytics, UUID> {
    Optional<DashboardAnalytics> findByDate(LocalDate date);
}
