package be.clinitrak.ethics.service;

import be.clinitrak.ethics.domain.entity.EthicsSequence;
import be.clinitrak.ethics.domain.repository.EthicsSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

/**
 * Service de génération des numéros CE (Comité d'Éthique).
 *
 * <p>Chaque numéro CE est unique par tenant et suit le format {@code AAAA/NNNN}
 * (ex: {@code 2026/0042}). Un verrou pessimiste (PESSIMISTIC_WRITE) combiné
 * à l'isolation SERIALIZABLE garantit l'unicité même en cas de soumissions simultanées.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final EthicsSequenceRepository sequenceRepository;

    /**
     * Génère un numéro CE unique au format {ANNEE}/{SEQUENCE_04d}.
     *
     * <p>Utilise un verrou pessimiste (PESSIMISTIC_WRITE) pour éviter les doublons
     * en cas de soumissions simultanées. Si aucune séquence n'existe pour l'année
     * et le tenant courants, une nouvelle séquence est créée à partir de 1.
     *
     * @param tenantId UUID du tenant pour lequel générer le numéro
     * @return numéro CE formaté, ex: {@code "2026/0042"}
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String generateEthicsNumber(UUID tenantId) {
        int currentYear = Year.now().getValue();

        EthicsSequence sequence = sequenceRepository
            .findByYearAndTenantIdWithLock(currentYear, tenantId)
            .orElseGet(() -> {
                EthicsSequence newSeq = new EthicsSequence();
                newSeq.setYear(currentYear);
                newSeq.setTenantId(tenantId);
                newSeq.setLastValue(0L);
                return sequenceRepository.save(newSeq);
            });

        sequence.setLastValue(sequence.getLastValue() + 1);
        EthicsSequence saved = sequenceRepository.save(sequence);

        String generated = String.format("%d/%04d", currentYear, saved.getLastValue());
        log.debug("Numéro CE généré : {} (tenant: {})", generated, tenantId);
        return generated;
    }
}
