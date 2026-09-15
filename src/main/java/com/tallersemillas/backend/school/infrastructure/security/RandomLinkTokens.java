package com.tallersemillas.backend.school.infrastructure.security;

import com.tallersemillas.backend.school.application.port.LinkTokens;
import com.tallersemillas.backend.school.infrastructure.privacy.DataCipher;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;
@Component public class RandomLinkTokens implements LinkTokens {
 private final SecureRandom random=new SecureRandom(); private final DataCipher cipher;
 public RandomLinkTokens(DataCipher cipher) { this.cipher=cipher; }
 public String generate() { byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
 public String hash(String value) { return cipher.index("invitation:"+value); }
}
