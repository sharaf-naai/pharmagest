package pharmacie.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hachage des mots de passe (RG02). */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /** Retourne l'empreinte SHA-256 du mot de passe, en hexadécimal (64 caractères). RG02 */
    public static String hacher(String motDePasse) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] empreinte = md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(empreinte);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est obligatoire dans toute JVM : ce cas ne doit jamais arriver
            throw new IllegalStateException("Algorithme SHA-256 indisponible", e);
        }
    }
}
