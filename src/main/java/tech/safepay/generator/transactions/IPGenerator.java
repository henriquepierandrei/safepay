package tech.safepay.generator.transactions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

@Component
public class IPGenerator {

    private static final Random RANDOM = new Random();
    private static final int VPN_CHANCE_PERCENT = 5;

    private List<String> vpnRanges;

    @JsonIgnoreProperties(ignoreUnknown = true)
    // record para mapear JSON
    public record VPNIpList(String description, List<String> list) {}

    @PostConstruct
    void loadVpnIps() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("data/vpn-ipv6-blacklist.json");

            VPNIpList data = mapper.readValue(is, VPNIpList.class);
            this.vpnRanges = data.list();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load VPN IP list", e);
        }
    }

    // Método principal
    public String generateIP() {
        int roll = RANDOM.nextInt(100); // 0-99
        if (roll < VPN_CHANCE_PERCENT) {
            return generateVpnIPv6();
        }
        return generateIPv6();
    }

    // 95% IP "normal"
    private String generateIPv6() {
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

    // 5% IP de VPN
    private String generateVpnIPv6() {
        String cidr = vpnRanges.get(RANDOM.nextInt(vpnRanges.size()));
        return expandCidrToIPv6(cidr);
    }

    // simples: preenche blocos faltantes aleatoriamente
    private String expandCidrToIPv6(String cidr) {
        String prefix = cidr.split("/")[0];
        String[] blocks = prefix.split(":");
        StringBuilder ip = new StringBuilder(prefix);

        while (blocks.length < 8) {
            ip.append(":").append(Integer.toHexString(RANDOM.nextInt(0x10000)));
            blocks = ip.toString().split(":");
        }

        return ip.toString();
    }
}
