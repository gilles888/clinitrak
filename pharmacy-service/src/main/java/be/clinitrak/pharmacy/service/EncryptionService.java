package be.clinitrak.pharmacy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service utilitaire de chiffrement AES/ECB/PKCS5 pour les données sensibles.
 *
 * <p>Utilisé pour chiffrer les codes de randomisation des médicaments en aveugle.
 * En cas d'échec du chiffrement, les données sont stockées en clair avec un avertissement.
 */
@Slf4j
@Service
public class EncryptionService {

    @Value("${clinitrak.pharmacy.encryption-key:0123456789abcdef0123456789abcdef}")
    private String encryptionKey;

    /**
     * Chiffre une chaîne de caractères en AES/ECB/PKCS5 et retourne le résultat en Base64.
     *
     * @param plainText texte en clair à chiffrer
     * @return texte chiffré encodé en Base64, ou le texte en clair en cas d'erreur
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.warn("Encryption failed, storing plaintext: {}", e.getMessage());
            return plainText;
        }
    }

    /**
     * Déchiffre une chaîne encodée en Base64 (AES/ECB/PKCS5).
     *
     * @param cipherText texte chiffré encodé en Base64
     * @return texte déchiffré, ou le texte chiffré en cas d'erreur
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Decryption failed: {}", e.getMessage());
            return cipherText;
        }
    }
}
