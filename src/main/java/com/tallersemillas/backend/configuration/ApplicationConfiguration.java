package com.tallersemillas.backend.configuration;

import com.tallersemillas.backend.application.port.inbound.*;
import com.tallersemillas.backend.application.port.outbound.*;
import com.tallersemillas.backend.application.service.*;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import java.time.Clock;
@Configuration
public class ApplicationConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean Passwords passwords(PasswordEncoder encoder) {
        // PBKDF2 permite contraseñas Unicode largas sin el límite de 72 bytes de bcrypt.
        var pbkdf2=Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        return raw -> "{pbkdf2@SpringSecurity_v5_8}" + pbkdf2.encode(raw);
    }
    @Bean RegisterAccount registerAccount(Accounts accounts, Passwords passwords) { return new AccountService(accounts,passwords); }
    @Bean ManageStudents manageStudents(Students students) { return new StudentService(students,Clock.systemUTC()); }
}
