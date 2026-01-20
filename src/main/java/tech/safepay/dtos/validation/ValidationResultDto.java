package tech.safepay.dtos.validation;

import tech.safepay.Enums.AlertType;

import java.util.ArrayList;
import java.util.List;

public class ValidationResultDto {
    private int score;
    private List<AlertType> triggeredAlerts = new ArrayList<>();

    public ValidationResultDto() {}

    public ValidationResultDto(int score, List<AlertType> triggeredAlerts) {
        this.score = score;
        this.triggeredAlerts = triggeredAlerts;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int s) {
        this.score += s;
    }

    public void setScore(int score) {
        this.score = score;
    }
    public void addAlert(AlertType alert) {
        this.triggeredAlerts.add(alert);
    }


    public List<AlertType> getTriggeredAlerts() {
        return triggeredAlerts;
    }

    public void setTriggeredAlerts(List<AlertType> triggeredAlerts) {
        this.triggeredAlerts = triggeredAlerts;
    }
}
