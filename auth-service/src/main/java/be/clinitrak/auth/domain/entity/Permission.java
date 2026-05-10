package be.clinitrak.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Permission atomique représentant une action sur une ressource.
 *
 * <p>Format recommandé : {@code RESOURCE:ACTION} (ex: {@code STUDY:READ}, {@code ETHICS:SUBMIT}).
 */
@Entity
@Table(name = "permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_permission_name", columnNames = "name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

    /** Nom unique de la permission (ex: "STUDY:READ"). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Description humaine de la permission. */
    @Column(name = "description", length = 500)
    private String description;

    /** Module fonctionnel auquel appartient cette permission. */
    @Column(name = "module", length = 50)
    private String module;
}
