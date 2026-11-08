package web;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
    private static final int ITERATIONS = 210000;
    private Passwords() {}
    public static String hash(String password) throws Exception {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return "pbkdf2:" + ITERATIONS + ":" + Base64.getEncoder().withoutPadding().encodeToString(salt) + ":" + Base64.getEncoder().withoutPadding().encodeToString(derive(password, salt, ITERATIONS));
    }
    public static boolean verify(String password, String hash) {
        try {
            String[] parts = hash.split(":");
            if (parts.length != 4 || !parts[0].equals("pbkdf2")) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100000 || iterations > 1000000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]), derive(password, Base64.getDecoder().decode(parts[2]), iterations));
        } catch (Exception e) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
