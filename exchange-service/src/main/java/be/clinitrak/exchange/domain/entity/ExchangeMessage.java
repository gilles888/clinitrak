package be.clinitrak.exchange.domain.entity;

import be.clinitrak.exchange.domain.enums.SenderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Message échangé dans le cadre d'une demande d'échange.
 *
 * <p>Supporte la communication bidirectionnelle entre les utilisateurs internes
 * et externes. Le champ {@code senderId} contient soit un UUID d'utilisateur interne
 * soit l'identifiant d'un {@link ExternalUser}.
 */
@Getter
@Setter
@Entity
@Table(name = "exchange_messages")
public class ExchangeMessage extends BaseEntity {

    /** Demande d'échange à laquelle ce message est rattaché. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private ExchangeRequest request;

    /** UUID de l'expéditeur (utilisateur interne ou ExternalUser). */
    @Column(name = "sender_id", nullable = false, length = 255)
    private String senderId;

    /** Type d'expéditeur : interne ou externe. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    /** Contenu textuel du message. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Chemin vers une pièce jointe optionnelle. */
    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    /** Date et heure d'envoi du message. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Date et heure de lecture du message par le destinataire. */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /** Identifiant du tenant auquel appartient ce message. */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
