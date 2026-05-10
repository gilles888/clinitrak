package be.clinitrak.study.dto;

import java.util.List;

/**
 * Enveloppe générique pour les réponses paginées.
 *
 * <p>Utilisée comme alternative à {@link org.springframework.data.domain.Page}
 * pour exposer des DTOs JSON cohérents et immuables.
 *
 * @param <T>          type des éléments de la page
 * @param content      liste des éléments de la page courante
 * @param page         numéro de la page courante (0-based)
 * @param size         nombre d'éléments par page
 * @param totalElements nombre total d'éléments toutes pages confondues
 * @param totalPages   nombre total de pages
 * @param last         indique si c'est la dernière page
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {
    /**
     * Crée un {@code PageResponse} depuis une {@link org.springframework.data.domain.Page} Spring.
     *
     * @param springPage page Spring Data
     * @param <T>        type des éléments
     * @return PageResponse correspondant
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> springPage) {
        return new PageResponse<>(
            springPage.getContent(),
            springPage.getNumber(),
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages(),
            springPage.isLast()
        );
    }
}
