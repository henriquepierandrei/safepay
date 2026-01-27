package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.dtos.fraudalert.FraudAlertFilterRequestDTO;
import tech.safepay.repositories.FraudAlertRepository;
import tech.safepay.services.FraudAlertService;

@RestController
@RequestMapping("/api/v1/fraud-alerts")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    private final FraudAlertRepository fraudAlertRepository;


    public FraudAlertController(FraudAlertService fraudAlertService, FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertService = fraudAlertService;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(
            @RequestBody FraudAlertFilterRequestDTO request,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "size") int size) {


        var response = fraudAlertService.getWithFilters(
                request.recentAlerts(),
                request.severity(),
                request.startFraudScore(),
                request.endFraudScore(),
                request.alertTypeList(),
                request.createdAtFrom(),
                request.transactionId(),
                request.deviceId(),
                request.cardId(),
                page,
                size
        );
        System.out.println("=== DEBUG FILTERS ===");
        System.out.println("recentAlerts: " + request.recentAlerts());
        System.out.println("severity: " + request.severity());
        System.out.println("alertTypeList: " + request.alertTypeList());
        System.out.println("Total alerts in DB: " + fraudAlertRepository.count());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<?> testAll(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size) {
        var response = fraudAlertService.getAllWithoutFilters(page, size);
        return ResponseEntity.ok(response);
    }

}
