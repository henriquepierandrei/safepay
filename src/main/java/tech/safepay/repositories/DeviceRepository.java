package tech.safepay.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.safepay.Enums.DeviceType;
import tech.safepay.entities.Device;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    @EntityGraph(attributePaths = "cards")
    Optional<Device> findWithCardsById(UUID id);


    @Query("""
                SELECT d FROM Device d
                WHERE (:deviceType IS NULL OR d.deviceType = :deviceType)
                  AND (:os IS NULL OR d.os = :os)
                  AND (:browser IS NULL OR d.browser = :browser)
            """)
    Page<Device> findWithFilters(
            @Param("deviceType") DeviceType deviceType,
            @Param("os") String os,
            @Param("browser") String browser,
            Pageable pageable
    );

}
