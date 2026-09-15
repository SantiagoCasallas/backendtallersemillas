package com.tallersemillas.backend.infrastructure.adapter.outbound.persistence;

import com.tallersemillas.backend.domain.*;
import jakarta.persistence.*;
import com.tallersemillas.backend.school.infrastructure.privacy.EncryptedText;
import java.util.UUID;
@Entity @Table(name="accounts")
public class AccountEntity {
    @Id UUID id;
    @Convert(converter=EncryptedText.class) @Column(nullable=false,columnDefinition="text") String name;
    @Convert(converter=EncryptedText.class) @Column(nullable=false,columnDefinition="text") String email;
    @Column(name="email_lookup",length=64) String emailLookup;
    @Column(name="password_hash",nullable=false,length=255) String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Role role;
    protected AccountEntity() {}
    AccountEntity(Account a) { id=a.id(); name=a.name(); email=a.email(); passwordHash=a.passwordHash(); role=a.role(); }
    Account domain() { return new Account(id,name,email,passwordHash,role); }
}
