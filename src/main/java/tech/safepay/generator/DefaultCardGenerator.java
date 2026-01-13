package tech.safepay.generator;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.CardBrand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Component
public class DefaultCardGenerator {

    private static final Random RANDOM = new Random();

    // Gera a brand do cartão
    public CardBrand choiceCardBrand() {
        CardBrand[] brands = CardBrand.values();
        int index = RANDOM.nextInt(brands.length);
        return brands[index];
    }

    // Gera a expiração do cartão
    public LocalDate generateExpirationDate() {
        int yearsToAdd = RANDOM.nextInt(4) + 2; // 2 a 5 anos
        return LocalDate.now()
                .plusYears(yearsToAdd)
                .withMonth(RANDOM.nextInt(12) + 1)
                .withDayOfMonth(1);


    }

    // Gera o limite de crédito
    public BigDecimal generateCreditLimit() {
        int steps = RANDOM.nextInt(10) + 1; // 1 a 10
        return BigDecimal.valueOf(steps * 1000L);
    }


    public Integer generateRiskScore(){
        return RANDOM.nextInt(30);
    }


    public String generateNumber(CardBrand cardBrand) {
        String prefix;
        int length;

        switch (cardBrand) {
            case VISA -> {
                prefix = "4";
                length = 16;
            }
            case MASTERCARD -> {
                prefix = "5" + (RANDOM.nextInt(5) + 1);
                length = 16;
            }
            case AMERICAN_EXPRESS -> {
                prefix = RANDOM.nextBoolean() ? "34" : "37";
                length = 15;
            }
            case ELO -> {
                prefix = "6362";
                length = 16;
            }
            default -> throw new IllegalArgumentException("Brand não suportada");
        }

        String baseNumber = prefix + generateRandomDigits(length - prefix.length() - 1);
        int checkDigit = calculateLuhnCheckDigit(baseNumber);
        return baseNumber + checkDigit;
    }

    private String generateRandomDigits(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

}
