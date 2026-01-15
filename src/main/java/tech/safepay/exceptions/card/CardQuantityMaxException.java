package tech.safepay.exceptions.card;

public class CardQuantityMaxException extends RuntimeException {
    public CardQuantityMaxException(String s) {
        super(s);
    }
}
