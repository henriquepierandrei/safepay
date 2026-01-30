package tech.safepay.websocket;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.safepay.repositories.*;
import tech.safepay.services.CardService;
import tech.safepay.services.DeviceService;
import tech.safepay.services.ResetDataService;

@Component
public class CleanupScheduler {


    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);
    private final ResetDataService resetAllData;

    public CleanupScheduler(ResetDataService resetAllData) {
        this.resetAllData = resetAllData;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledReset() {
        resetAllData.resetAllData();
    }



}
