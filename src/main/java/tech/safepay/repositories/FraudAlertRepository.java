package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.safepay.entities.FraudAlert;

import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID>,
        JpaSpecificationExecutor<FraudAlert> {
}