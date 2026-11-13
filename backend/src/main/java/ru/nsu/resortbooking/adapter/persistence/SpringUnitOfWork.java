package ru.nsu.resortbooking.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.nsu.resortbooking.application.port.UnitOfWork;

import java.util.function.Supplier;

@Component
public class SpringUnitOfWork implements UnitOfWork {
    private final TransactionTemplate transactions;

    public SpringUnitOfWork(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }

    @Override
    public void onRollback(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }
}
