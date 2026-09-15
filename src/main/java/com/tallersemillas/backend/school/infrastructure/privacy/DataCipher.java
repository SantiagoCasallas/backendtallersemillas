package com.tallersemillas.backend.school.infrastructure.privacy;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Component
public class DataCipher {
 private final byte[] key; private final SecureRandom random=new SecureRandom();
 public DataCipher(@Value("${app.data-key}") String encoded) {
  try { key=Base64.getDecoder().decode(encoded); } catch(IllegalArgumentException ex) { throw new IllegalStateException("DATA_ENCRYPTION_KEY debe estar en base64"); }
  if(key.length!=32) throw new IllegalStateException("DATA_ENCRYPTION_KEY debe contener 32 bytes");
 }
 public String encrypt(String plain) {
  if(plain==null) return null;
  try { byte[] iv=new byte[12]; random.nextBytes(iv); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));
   byte[] encrypted=cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
   return "enc:v1:"+Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(encrypted);
  } catch(GeneralSecurityException ex) { throw new IllegalStateException("No se pudo cifrar",ex); }
 }
 public String decrypt(String encrypted) {
  if(encrypted==null) return null;
  if(!encrypted.startsWith("enc:v1:")) throw new IllegalStateException("Registro sin migrar a cifrado");
  try { String[] parts=encrypted.split(":",4); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[2])));
   return new String(cipher.doFinal(Base64.getDecoder().decode(parts[3])),StandardCharsets.UTF_8);
  } catch(GeneralSecurityException | IllegalArgumentException ex) { throw new IllegalStateException("No se pudo descifrar; verifica la clave configurada",ex); }
 }
 public String index(String value) {
  try { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256")); return HexFormat.of().formatHex(mac.doFinal(("lookup:v1:"+value).getBytes(StandardCharsets.UTF_8))); }
  catch(GeneralSecurityException ex) { throw new IllegalStateException(ex); }
 }
}
