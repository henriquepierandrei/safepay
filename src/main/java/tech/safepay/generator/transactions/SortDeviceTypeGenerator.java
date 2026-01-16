package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.DeviceType;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.util.Random;

@Component
public class SortDeviceTypeGenerator {
    private final TransactionRepository transactionRepository;
    private static final Random RANDOM = new Random();

    public SortDeviceTypeGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private DeviceType randomDeviceType() {
        DeviceType[] values = DeviceType.values();
        return values[RANDOM.nextInt(values.length)];
    }


    //  Escolher dispositivo a ser utilizado.
    public DeviceType sortDeviceType(Card card) {

        var lastTransactions =
                transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        int mobile = 0;
        int desktop = 0;
        int terminal = 0;

        for (Transaction transaction : lastTransactions) {
            DeviceType type = transaction.getDeviceType();

            if (type == null) continue;

            switch (type) {
                case MOBILE -> mobile++;
                case DESKTOP -> desktop++;
                case POS_TERMINAL -> terminal++;
            }
        }

        // Se não houver histórico, escolhe aleatório
        if (mobile + desktop + terminal == 0) {
            return randomDeviceType();
        }

        // Sorteio ponderado
        int totalWeight = mobile + desktop + terminal;
        int draw = RANDOM.nextInt(totalWeight);

        if (draw < mobile) {
            return DeviceType.MOBILE;
        } else if (draw < mobile + desktop) {
            return DeviceType.DESKTOP;
        } else {
            return DeviceType.POS_TERMINAL;
        }
    }


}
