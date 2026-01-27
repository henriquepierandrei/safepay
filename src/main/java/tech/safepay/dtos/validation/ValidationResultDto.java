package tech.safepay.dtos.validation;

import tech.safepay.Enums.AlertType;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO responsável por armazenar o resultado das validações antifraude
 * executadas durante o processamento de uma transação.
 * <p>
 * Atua como um agregador de estado, acumulando:
 * <ul>
 *   <li>Score total gerado pelas regras aplicadas</li>
 *   <li>Alertas disparados ao longo do pipeline</li>
 * </ul>
 *
 * Normalmente é instanciado no início do pipeline e enriquecido
 * progressivamente por cada etapa de validação.
 *
 * Não contém regras de negócio, apenas estado.
 *
 * @author SafePay Team
 * @version 1.0
 */
public class ValidationResultDto {

    /** Score acumulado após a execução das validações */
    private int score;

    /** Lista de alertas disparados pelas regras antifraude */
    private List<AlertType> triggeredAlerts = new ArrayList<>();

    /**
     * Construtor padrão.
     * Inicializa score com zero e lista de alertas vazia.
     */
    public ValidationResultDto() {}

    /**
     * Construtor completo.
     *
     * @param score score inicial da validação
     * @param triggeredAlerts lista de alertas já disparados
     */
    public ValidationResultDto(int score, List<AlertType> triggeredAlerts) {
        this.score = score;
        this.triggeredAlerts = triggeredAlerts;
    }

    /**
     * Retorna o score acumulado da validação.
     *
     * @return score total
     */
    public int getScore() {
        return score;
    }

    /**
     * Incrementa o score atual.
     *
     * @param s valor a ser somado ao score
     */
    public void addScore(int s) {
        this.score += s;
    }

    /**
     * Define explicitamente o score.
     *
     * @param score novo valor do score
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Adiciona um alerta à lista de alertas disparados.
     *
     * @param alert tipo de alerta antifraude
     */
    public void addAlert(AlertType alert) {
        this.triggeredAlerts.add(alert);
    }

    /**
     * Retorna a lista de alertas disparados.
     *
     * @return lista de alertas
     */
    public List<AlertType> getTriggeredAlerts() {
        return triggeredAlerts;
    }

    /**
     * Define a lista de alertas disparados.
     *
     * @param triggeredAlerts nova lista de alertas
     */
    public void setTriggeredAlerts(List<AlertType> triggeredAlerts) {
        this.triggeredAlerts = triggeredAlerts;
    }
}
