package be.clinitrak.notification.domain.enums;

/**
 * Types de notifications supportés par le notification-service CliniTrak.
 * Chaque type est associé à un template email Thymeleaf correspondant.
 */
public enum NotificationType {

    /** Notification lors d'un changement de statut d'une étude clinique. */
    STUDY_STATUS_CHANGE,

    /** Confirmation de réception d'une soumission via le portail Exchange. */
    SUBMISSION_RECEIVED,

    /** Rappel pour soumettre le rapport annuel d'une étude. */
    ANNUAL_REPORT_DUE,

    /** Alerte de stock bas dans le module Pharmacie. */
    STOCK_ALERT,

    /** Rappel de réunion (Comité d'Éthique, etc.). */
    MEETING_REMINDER,

    /** Notification d'affectation d'une tâche à un utilisateur. */
    TASK_ASSIGNED,

    /** Email d'invitation d'un nouvel utilisateur sur la plateforme. */
    USER_INVITED,

    /** Alerte système générique (maintenance, incident, etc.). */
    SYSTEM_ALERT
}
