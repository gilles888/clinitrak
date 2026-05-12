package be.clinitrak.document.mapper;

import be.clinitrak.document.domain.entity.Document;
import be.clinitrak.document.dto.DocumentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper MapStruct pour la conversion entre l'entité {@link Document}
 * et le DTO {@link DocumentResponse}.
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper {

    /**
     * Convertit une entité Document en DocumentResponse.
     * Le champ {@code uploadedAt} est mappé depuis {@code createdAt}.
     * Le champ {@code downloadUrl} doit être renseigné manuellement après le mapping.
     *
     * @param document entité source
     * @return DTO de réponse (downloadUrl sera null, à compléter)
     */
    @Mapping(source = "createdAt", target = "uploadedAt")
    @Mapping(target = "downloadUrl", ignore = true)
    DocumentResponse toResponse(Document document);

    /**
     * Convertit une liste d'entités Document en liste de DocumentResponse.
     *
     * @param documents liste d'entités source
     * @return liste de DTOs de réponse
     */
    List<DocumentResponse> toResponseList(List<Document> documents);
}
