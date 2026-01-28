package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.DeviceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um dispositivo utilizado em transações no SafePay.
 * <p>
 * O dispositivo é identificado por um fingerprint único e
 * utilizado pelo motor antifraude para análise de risco,
 * correlação de eventos e detecção de comportamento suspeito.
 *
 * Responsabilidades:
 * <ul>
 *   <li>Identificar tecnicamente o ambiente do usuário</li>
 *   <li>Armazenar informações de sistema e navegador</li>
 *   <li>Registrar primeira e última utilização</li>
 *   <li>Manter vínculo com cartões associados</li>
 * </ul>
 *
 * Atua como entidade de apoio para decisões antifraude,
 * não contendo regras de negócio próprias.
 *
 * @author SafePay Team
 * @version 1.0
 */
@Entity
@Table(name = "devices_tb")
public class Device {

    /**
     * Identificador único do dispositivo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Fingerprint único do dispositivo.
     */
    @Column(nullable = false, unique = true)
    private String fingerPrintId;

    /**
     * Tipo do dispositivo (ex: MOBILE, DESKTOP, TERMINAL).
     */
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;
    private LocalDateTime lastFingerPrintChangedAt;

    /**
     * Sistema operacional do dispositivo.
     */
    @Column(length = 50)
    private String os;

    /**
     * Navegador utilizado no dispositivo.
     */
    @Column(length = 50)
    private String browser;

    /**
     * Timestamp da primeira detecção do dispositivo.
     */
    private Instant firstSeenAt = Instant.now();

    /**
     * Timestamp da última atividade registrada.
     */
    private Instant lastSeenAt = Instant.now();

    /**
     * Cartões associados ao dispositivo.
     */
    @ManyToMany(mappedBy = "devices")
    private List<Card> cards = new ArrayList<>();

    /** Construtor padrão exigido pelo JPA */
    public Device() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFingerPrintId() {
        return fingerPrintId;
    }

    public void setFingerPrintId(String fingerPrintId) {
        this.fingerPrintId = fingerPrintId;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public LocalDateTime getLastFingerPrintChangedAt() {
        return lastFingerPrintChangedAt;
    }

    public void setLastFingerPrintChangedAt(LocalDateTime lastFingerPrintChangedAt) {
        this.lastFingerPrintChangedAt = lastFingerPrintChangedAt;
    }
}
