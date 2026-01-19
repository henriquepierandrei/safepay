package tech.safepay.exceptions.device;

public class DeviceNotLinkedException extends RuntimeException{
    public DeviceNotLinkedException(String message){
        super(message);
    }
}
