package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.safepay.services.ResetDataService;
import tech.safepay.websocket.CleanupScheduler;

@RestController
@RequestMapping("/api/v1")
public class CleanupController {
    private final ResetDataService resetDataService;

    public CleanupController(ResetDataService resetDataService) {
        this.resetDataService = resetDataService;
    }

    @PostMapping("/admin/reset")
    public ResponseEntity<String> resetManual() {
        resetDataService.resetAllData();
        return ResponseEntity.ok("Reset executado com sucesso");
    }


}
