package com.tallersemillas.backend.school.infrastructure.privacy;

import jakarta.persistence.*;
@Converter
public class EncryptedText implements AttributeConverter<String,String> {
 private final DataCipher cipher;
 public EncryptedText(DataCipher cipher) { this.cipher=cipher; }
 public String convertToDatabaseColumn(String value) { return cipher.encrypt(value); }
 public String convertToEntityAttribute(String value) { return cipher.decrypt(value); }
}
