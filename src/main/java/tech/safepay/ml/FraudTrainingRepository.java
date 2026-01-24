package tech.safepay.ml;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FraudTrainingRepository
        extends JpaRepository<FraudTraining, UUID> {
}
