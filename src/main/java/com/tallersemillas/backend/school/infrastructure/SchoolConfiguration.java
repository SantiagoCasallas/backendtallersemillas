package com.tallersemillas.backend.school.infrastructure;

import com.tallersemillas.backend.school.application.*;
import com.tallersemillas.backend.school.application.port.*;
import org.springframework.context.annotation.*;
import java.time.Clock;
@Configuration
public class SchoolConfiguration {
 @Bean SchoolManagement schoolManagement(SchoolRepository repository,Transactions transactions) { return new SchoolService(repository,transactions,Clock.systemUTC()); }
 @Bean AdmissionsWorkflow admissionsWorkflow(SchoolRepository repository,SchoolManagement school,Transactions transactions,LinkTokens tokens) { return new AdmissionsWorkflow(repository,school,transactions,tokens,Clock.systemUTC()); }
}
