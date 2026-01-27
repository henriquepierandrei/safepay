package tech.safepay.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.configs.FraudAlertSpecifications;
import tech.safepay.dtos.fraudalert.FraudAlertResponseDTO;
import tech.safepay.entities.FraudAlert;
import tech.safepay.repositories.FraudAlertRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    public FraudAlertService(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public Page<FraudAlertResponseDTO> getWithFilters(
            Boolean recentAlerts,
            Severity severity,
            Integer startFraudScore,
            Integer endFraudScore,
            List<AlertType> alertTypeList,
            LocalDateTime createdAtFrom,
            UUID transactionId,
            UUID deviceId,
            UUID cardId,
            int page,
            int size
    ) {
        Specification<FraudAlert> spec = FraudAlertSpecifications.withFilters(
                recentAlerts,
                severity,
                startFraudScore,
                endFraudScore,
                alertTypeList,
                createdAtFrom,
                transactionId,
                deviceId,
                cardId
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<FraudAlert> fraudAlerts = fraudAlertRepository.findAll(spec, pageable);

        return fraudAlerts.map(FraudAlertResponseDTO::from);
    }

    public Page<FraudAlertResponseDTO> getAllWithoutFilters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FraudAlert> fraudAlerts = fraudAlertRepository.findAll(pageable);

        return fraudAlerts.map(FraudAlertResponseDTO::from);
    }
}