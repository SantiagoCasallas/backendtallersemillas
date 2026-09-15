package com.tallersemillas.backend.school.infrastructure.privacy;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
/** Converts existing V1/V2 data before the application is ready to serve traffic. Keep database backups protected. */
@Component @Order(-100)
public class PrivacyMigration implements ApplicationRunner {
 private final JdbcTemplate jdbc; private final DataCipher cipher; private final TransactionTemplate tx; private final ObjectMapper json;
 public PrivacyMigration(JdbcTemplate jdbc,DataCipher cipher,PlatformTransactionManager manager,ObjectMapper json) { this.jdbc=jdbc;this.cipher=cipher;this.tx=new TransactionTemplate(manager);this.json=json; }
 @SuppressWarnings("unchecked") public void run(ApplicationArguments args) { tx.executeWithoutResult(status->{
  jdbc.queryForList("SELECT id,name,email,email_lookup FROM accounts").forEach(row->{
   String name=(String)row.get("name"),email=(String)row.get("email");
   String plain=email.startsWith("enc:v1:") ? cipher.decrypt(email) : email;
   jdbc.update("UPDATE accounts SET name=?,email=?,email_lookup=? WHERE id=?",name.startsWith("enc:v1:") ? name : cipher.encrypt(name),email.startsWith("enc:v1:") ? email : cipher.encrypt(email),cipher.index(plain.toLowerCase(Locale.ROOT)),row.get("id"));
  });
  jdbc.queryForList("SELECT id,full_name FROM students").forEach(row->{String value=(String)row.get("full_name"); if(!value.startsWith("enc:v1:")) jdbc.update("UPDATE students SET full_name=? WHERE id=?",cipher.encrypt(value),row.get("id"));});
  for(String table:List.of("school_students","school_guardians","school_medical_histories","school_medical_entries","school_schemas","school_slots","school_interviews","school_inscriptions")) {
   jdbc.queryForList("SELECT id,data FROM "+table).forEach(row->{String data=(String)row.get("data"); if(!data.startsWith("enc:v1:")) {
    Map<String,Object> fields=json.readValue(data,Map.class);
    if(table.equals("school_students") || table.equals("school_guardians")) jdbc.update("UPDATE "+table+" SET document_number=? WHERE id=?",cipher.index(String.valueOf(fields.get("documento_identidad"))),row.get("id"));
    if(table.equals("school_interviews")) jdbc.update("UPDATE "+table+" SET contact_email=? WHERE id=?",cipher.index(String.valueOf(fields.get("correo_form_aspirante"))),row.get("id"));
    jdbc.update("UPDATE "+table+" SET data=? WHERE id=?",cipher.encrypt(data),row.get("id"));
   }});
  }
 }); }
}
