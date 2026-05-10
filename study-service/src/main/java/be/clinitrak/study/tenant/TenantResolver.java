package be.clinitrak.study.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Stratégies de résolution du tenant depuis l'URL.
 *
 * <p>Supporte la résolution via sous-domaine :
 * {@code saintluc.clinitrak.be} → slug {@code saintluc}.
 */
@Component
public class TenantResolver {

    private final String rootDomain;

    /**
     * Crée un nouveau {@code TenantResolver} avec le domaine racine configuré.
     *
     * @param rootDomain domaine racine de la plateforme (ex: {@code clinitrak.be})
     */
    public TenantResolver(@Value("${clinitrak.tenant.root-domain:clinitrak.be}") String rootDomain) {
        this.rootDomain = rootDomain;
    }

    /**
     * Extrait le slug du tenant depuis le hostname.
     *
     * <p>Ex : {@code saintluc.clinitrak.be} avec rootDomain {@code clinitrak.be} → {@code saintluc}.
     *
     * @param hostname hostname complet de la requête
     * @return slug du tenant ou null si hostname ne correspond pas à la convention
     */
    public String resolveFromHost(String hostname) {
        if (!StringUtils.hasText(hostname)) {
            return null;
        }

        String suffix = "." + rootDomain;
        if (hostname.endsWith(suffix)) {
            String subdomain = hostname.substring(0, hostname.length() - suffix.length());
            // On ne prend que le premier niveau de sous-domaine
            if (!subdomain.contains(".") && StringUtils.hasText(subdomain)) {
                return subdomain.toLowerCase();
            }
        }

        return null;
    }
}
