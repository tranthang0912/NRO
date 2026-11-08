package web;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordsTest {
    @Test void saltedPasswordsAndIncorrectCredentials() throws Exception {
        String a=Passwords.hash("test-password"), b=Passwords.hash("test-password");
        assertNotEquals(a,b);assertTrue(a.length()<=100);
        assertTrue(Passwords.verify("test-password",a));assertFalse(Passwords.verify("incorrect",a));
        assertFalse(Passwords.verify("test-password","test-password"));assertFalse(Passwords.verify("x","pbkdf2:999999999:x:y"));
    }
}
