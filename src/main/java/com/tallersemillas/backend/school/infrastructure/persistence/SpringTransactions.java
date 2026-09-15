package com.tallersemillas.backend.school.infrastructure.persistence;

import com.tallersemillas.backend.school.application.port.Transactions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.function.Supplier;
@Component
public class SpringTransactions implements Transactions {
 private final TransactionTemplate template;
 public SpringTransactions(PlatformTransactionManager manager) { template=new TransactionTemplate(manager); }
 public <T> T required(Supplier<T> work) { return template.execute(status->work.get()); }
}
