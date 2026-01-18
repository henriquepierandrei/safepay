package tech.safepay.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.services.DeviceService;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateDevices(@RequestParam(name = "quantity") int quantity){
        return ResponseEntity.ok(deviceService.generateDevice(quantity));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getDeviceList(){
        return ResponseEntity.ok(deviceService.getDeviceList());
    }

    @PostMapping("/card/add")
    public ResponseEntity<?> add(@RequestBody DeviceService.AddCardDto dto){
        return ResponseEntity.ok(deviceService.addCardToDevice(dto));
    }
}
