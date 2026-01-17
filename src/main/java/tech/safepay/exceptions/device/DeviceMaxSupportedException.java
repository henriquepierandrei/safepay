package tech.safepay.exceptions.device;

public class DeviceMaxSupportedException extends RuntimeException{
    public DeviceMaxSupportedException(String message){
        super(message);
    }
}
