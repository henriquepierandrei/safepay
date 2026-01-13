package tech.safepay.exceptions.card;

public class CardNotFoundException extends RuntimeException{
    public CardNotFoundException(String message){
        super(message);
    }
}
