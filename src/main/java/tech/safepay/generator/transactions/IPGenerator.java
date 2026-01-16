package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class IPGenerator {
    private static final Random RANDOM = new Random();


    // Gerar IPV6
    public String generateIPv6() {
        return String.format(
                "%x:%x:%x:%x:%x:%x:%x:%x",
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000),
                RANDOM.nextInt(0x10000)
        );
    }


}
