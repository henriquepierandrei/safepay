package tech.safepay.exceptions.device;

public class DeviceNotFoundException extends RuntimeException{
    public DeviceNotFoundException(String message){
        super(message);
    }
}
