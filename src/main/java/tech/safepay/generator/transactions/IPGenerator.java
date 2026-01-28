package tech.safepay.generator.transactions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

/**
 * Gerador de endereços IP (IPv6) para transações simuladas, incluindo IPs de VPN conhecidas.
 * <p>
 * Este componente é responsável por gerar endereços IPv6 realistas para teste do sistema
 * antifraude, incluindo:
 * <ul>
 *   <li>Endereços IPv6 normais e aleatórios (95% dos casos)</li>
 *   <li>Endereços IPv6 de ranges de VPN conhecidas (5% dos casos)</li>
 *   <li>Carregamento de blacklist de VPN de arquivo de configuração</li>
 * </ul>
 * <p>
 * <b>Estratégia de geração:</b>
 * <ul>
 *   <li><b>95%:</b> IPs IPv6 completamente aleatórios simulando conexões normais</li>
 *   <li><b>5%:</b> IPs dentro de ranges conhecidos de provedores VPN</li>
 * </ul>
 * <p>
 * <b>Propósito da blacklist de VPN:</b>
 * A inclusão de IPs de VPN permite testar regras antifraude que detectam:
 * <ul>
 *   <li>Tentativas de ocultação de localização real</li>
 *   <li>Padrões suspeitos de anonimização</li>
 *   <li>Inconsistências entre localização GPS e IP</li>
 * </ul>
 * <p>
 * <b>Fonte de dados:</b>
 * A blacklist de VPN é carregada do arquivo {@code data/vpn-ipv6-blacklist.json}
 * no classpath durante a inicialização do componente.
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>Geração de transações de teste com IPs realistas</li>
 *   <li>Simulação de tentativas de fraude com ocultação de IP</li>
 *   <li>Testes de detecção de anomalias de rede</li>
 *   <li>Validação de regras antifraude baseadas em geolocalização</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class IPGenerator {

    /**
     * Gerador de números aleatórios thread-safe para criação de endereços IP.
     */
    private static final Random RANDOM = new Random();

    /**
     * Percentual de probabilidade de gerar um IP de VPN (5%).
     * Esta taxa foi calibrada para simular a proporção realista de transações
     * originadas de conexões VPN no mercado brasileiro.
     */
    private static final int VPN_CHANCE_PERCENT = 5;

    /**
     * Lista de ranges CIDR IPv6 conhecidos de provedores VPN.
     * Carregada do arquivo de configuração durante inicialização.
     */
    private List<String> vpnRanges;

    /**
     * Record para deserialização do arquivo JSON de blacklist de VPN.
     * <p>
     * Estrutura esperada do JSON:
     * <pre>
     * {
     *   "description": "VPN IPv6 Blacklist",
     *   "list": [
     *     "2001:db8::/32",
     *     "2607:f8b0::/32",
     *     ...
     *   ]
     * }
     * </pre>
     *
     * @param description descrição textual da blacklist
     * @param list lista de ranges CIDR IPv6 de VPN
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VPNIpList(String description, List<String> list) {}

    /**
     * Carrega a blacklist de IPs de VPN do arquivo de configuração durante inicialização.
     * <p>
     * Este método é executado automaticamente após a construção do bean pelo Spring,
     * garantindo que os dados estejam disponíveis antes do primeiro uso.
     * <p>
     * <b>Arquivo de origem:</b>
     * {@code src/main/resources/data/vpn-ipv6-blacklist.json}
     * <p>
     * <b>Formato esperado:</b>
     * Arquivo JSON contendo objeto com campos:
     * <ul>
     *   <li><b>description:</b> string descritiva (opcional)</li>
     *   <li><b>list:</b> array de strings com ranges CIDR IPv6</li>
     * </ul>
     * <p>
     * <b>Tratamento de erros:</b>
     * Qualquer falha no carregamento (arquivo não encontrado, JSON inválido, etc.)
     * resulta em RuntimeException, impedindo a inicialização do componente.
     * Isso é intencional para garantir que o sistema não opere com dados incompletos.
     * <p>
     * <b>Exemplo de conteúdo do arquivo:</b>
     * <pre>
     * {
     *   "description": "Known VPN IPv6 ranges",
     *   "list": [
     *     "2001:67c:2e8::/48",
     *     "2a03:2880::/32",
     *     "2c0f:fb50::/32"
     *   ]
     * }
     * </pre>
     *
     * @throws RuntimeException se falhar ao carregar ou parsear o arquivo de blacklist
     */
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

    /**
     * Gera um endereço IPv6 seguindo distribuição probabilística entre IPs normais e de VPN.
     * <p>
     * Este é o método principal do gerador, implementando a lógica de decisão:
     * <ul>
     *   <li><b>95% de probabilidade:</b> gera IPv6 completamente aleatório</li>
     *   <li><b>5% de probabilidade:</b> gera IPv6 de range VPN conhecido</li>
     * </ul>
     * <p>
     * <b>Distribuição estatística:</b>
     * <table border="1">
     *   <tr>
     *     <th>Tipo</th>
     *     <th>Probabilidade</th>
     *     <th>Método chamado</th>
     *     <th>Exemplo</th>
     *   </tr>
     *   <tr>
     *     <td>Normal</td>
     *     <td>95%</td>
     *     <td>generateIPv6()</td>
     *     <td>2001:db8:85a3::8a2e:370:7334</td>
     *   </tr>
     *   <tr>
     *     <td>VPN</td>
     *     <td>5%</td>
     *     <td>generateVpnIPv6()</td>
     *     <td>2001:67c:2e8:1234:5678:90ab:cdef:1234</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Propósito da distribuição:</b>
     * A taxa de 5% para VPN foi calibrada para simular cenários realistas onde:
     * <ul>
     *   <li>Maioria das transações vem de IPs residenciais/corporativos normais</li>
     *   <li>Pequena parcela usa VPN (legítima ou para ocultar origem)</li>
     *   <li>Sistema antifraude pode testar detecção de VPN sem falsos positivos excessivos</li>
     * </ul>
     * <p>
     * <b>Uso em regras antifraude:</b>
     * O IP gerado pode ser usado para validações como:
     * <ul>
     *   <li>Verificação de consistência entre IP e coordenadas GPS</li>
     *   <li>Detecção de uso de VPN (alerta de anonimização)</li>
     *   <li>Análise de velocidade impossível (IP em país diferente)</li>
     *   <li>Bloqueio ou revisão manual de IPs de VPN conhecidas</li>
     * </ul>
     *
     * @return endereço IPv6 formatado como string (ex: "2001:db8:85a3::8a2e:370:7334")
     */
    public String generateIP() {
        int roll = RANDOM.nextInt(100); // 0-99
        if (roll < VPN_CHANCE_PERCENT) {
            return generateVpnIPv6();
        }
        return generateIPv6();
    }

    /**
     * Gera um endereço IPv6 completamente aleatório para simular conexão normal.
     * <p>
     * Cria um IPv6 válido gerando 8 blocos hexadecimais de 16 bits cada,
     * resultando em um endereço de 128 bits conforme especificação RFC 4291.
     * <p>
     * <b>Formato gerado:</b>
     * <pre>
     * xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx
     * </pre>
     * Onde cada 'x' é um dígito hexadecimal (0-9, a-f).
     * <p>
     * <b>Técnica de geração:</b>
     * <ul>
     *   <li>Cada bloco é um inteiro aleatório de 0 a 0xFFFF (65535)</li>
     *   <li>Convertido para hexadecimal sem padding</li>
     *   <li>Blocos unidos por dois-pontos</li>
     * </ul>
     * <p>
     * <b>Exemplos de saída:</b>
     * <ul>
     *   <li>2001:db8:85a3:0:0:8a2e:370:7334</li>
     *   <li>fe80:0:0:0:200:f8ff:fe21:67cf</li>
     *   <li>2001:0db8:0000:0042:0000:8a2e:0370:7334</li>
     * </ul>
     * <p>
     * <b>Nota sobre compressão:</b>
     * O método não aplica compressão de zeros (::) para simplicidade.
     * IPs gerados são válidos mas não necessariamente na forma mais compacta.
     * <p>
     * <b>Uso pretendido:</b>
     * Representa 95% das transações, simulando conexões de:
     * <ul>
     *   <li>ISPs residenciais</li>
     *   <li>Redes corporativas</li>
     *   <li>Conexões móveis (4G/5G)</li>
     *   <li>Qualquer origem não-VPN</li>
     * </ul>
     *
     * @return endereço IPv6 completamente aleatório
     */
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

    /**
     * Gera um endereço IPv6 pertencente a um range de VPN conhecida.
     * <p>
     * Seleciona aleatoriamente um range CIDR da blacklist de VPN e expande
     * para um endereço IPv6 completo dentro daquele range.
     * <p>
     * <b>Processo:</b>
     * <ol>
     *   <li>Seleciona CIDR aleatório da blacklist (ex: "2001:67c:2e8::/48")</li>
     *   <li>Expande o prefixo para um IPv6 completo</li>
     *   <li>Preenche blocos faltantes com valores aleatórios</li>
     * </ol>
     * <p>
     * <b>Exemplo de expansão:</b>
     * <pre>
     * CIDR: "2001:67c:2e8::/48"
     * Prefixo: "2001:67c:2e8"
     * Expandido: "2001:67c:2e8:a3f1:8d2c:4b7e:9f21:3456"
     *            └─ prefixo ─┘└─── aleatório ────┘
     * </pre>
     * <p>
     * <b>Propósito:</b>
     * IPs gerados pertencem a ranges documentados de provedores VPN,
     * permitindo que o sistema antifraude:
     * <ul>
     *   <li>Detecte uso de VPN comparando com blacklist</li>
     *   <li>Aplique regras específicas para conexões VPN</li>
     *   <li>Aumente score de risco em casos suspeitos</li>
     *   <li>Valide consistência com outras informações da transação</li>
     * </ul>
     * <p>
     * <b>Provedores VPN típicos na blacklist:</b>
     * <ul>
     *   <li>NordVPN</li>
     *   <li>ExpressVPN</li>
     *   <li>Surfshark</li>
     *   <li>ProtonVPN</li>
     *   <li>Outros serviços comerciais</li>
     * </ul>
     *
     * @return endereço IPv6 dentro de um range de VPN conhecida
     */
    private String generateVpnIPv6() {
        String cidr = vpnRanges.get(RANDOM.nextInt(vpnRanges.size()));
        return expandCidrToIPv6(cidr);
    }

    /**
     * Expande uma notação CIDR IPv6 para um endereço completo preenchendo blocos faltantes.
     * <p>
     * Converte um prefixo CIDR (ex: "2001:db8::/32") em um endereço IPv6 completo
     * adicionando blocos hexadecimais aleatórios até completar os 8 blocos necessários.
     * <p>
     * <b>Algoritmo:</b>
     * <ol>
     *   <li>Remove sufixo da máscara (tudo após "/")</li>
     *   <li>Divide o prefixo em blocos pelo delimitador ":"</li>
     *   <li>Enquanto houver menos de 8 blocos:
     *     <ul>
     *       <li>Adiciona ":" ao final</li>
     *       <li>Adiciona bloco hexadecimal aleatório (0-FFFF)</li>
     *     </ul>
     *   </li>
     * </ol>
     * <p>
     * <b>Exemplos de expansão:</b>
     * <table border="1">
     *   <tr>
     *     <th>CIDR de entrada</th>
     *     <th>Blocos iniciais</th>
     *     <th>Blocos faltantes</th>
     *     <th>Exemplo de saída</th>
     *   </tr>
     *   <tr>
     *     <td>2001:db8::/32</td>
     *     <td>2</td>
     *     <td>6</td>
     *     <td>2001:db8:a3f1:8d2c:4b7e:9f21:3456:7890</td>
     *   </tr>
     *   <tr>
     *     <td>2001:67c:2e8::/48</td>
     *     <td>3</td>
     *     <td>5</td>
     *     <td>2001:67c:2e8:1234:5678:90ab:cdef:1234</td>
     *   </tr>
     *   <tr>
     *     <td>2a03:2880::/32</td>
     *     <td>2</td>
     *     <td>6</td>
     *     <td>2a03:2880:f001:c05:face:b00c:0:25de</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Limitações:</b>
     * <ul>
     *   <li>Não valida se CIDR é sintaticamente correto</li>
     *   <li>Não respeita a máscara de bits (/xx) - apenas o prefixo</li>
     *   <li>Não lida com notação comprimida (::) no prefixo</li>
     * </ul>
     * <p>
     * <b>Nota:</b>
     * Esta é uma implementação simplificada para propósitos de teste.
     * Para uso em produção com validação rigorosa de CIDR, considere bibliotecas
     * especializadas como inet.ipaddr ou guava.
     *
     * @param cidr notação CIDR IPv6 (ex: "2001:db8::/32")
     * @return endereço IPv6 completo dentro do range especificado
     */
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