package be.clinitrak.document.domain.enums;

/**
 * Types de documents supportés par le document-service CliniTrak.
 */
public enum DocumentType {

    /** Document au format PDF. */
    PDF,

    /** Classeur Microsoft Excel. */
    XLSX,

    /** Document Microsoft Word. */
    DOCX,

    /** Document au format RTF (Rich Text Format). */
    RTF,

    /** Fichier image (JPEG, PNG, TIFF, etc.). */
    IMAGE
}
