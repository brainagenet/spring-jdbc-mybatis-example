package net.brainage.example.core.tx;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Created by ms29.seo on 2016-08-18.
 */
public class TransactionImpl implements Transaction {

    private final PlatformTransactionManager transactionManager;

    private DefaultTransactionDefinition transactionDefinition;

    private TransactionStatus status;

    public TransactionImpl(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        transactionDefinition = new DefaultTransactionDefinition();
    }

    public void setPropagationBehavior(int propagationBehavior) {
        transactionDefinition.setPropagationBehavior(propagationBehavior);
    }

    public void setIsolationLevel(int isolationLevel) {
        transactionDefinition.setIsolationLevel(isolationLevel);
    }

    public void setReadOnly(boolean readOnly) {
        transactionDefinition.setReadOnly(readOnly);
    }

    @Override
    public void start() {
        status = transactionManager.getTransaction(transactionDefinition);
    }

    @Override
    public void commit() {
        if (!status.isCompleted()) {
            transactionManager.commit(status);
        }
    }

    @Override
    public void rollback() {
        if (!status.isCompleted()) {
            transactionManager.rollback(status);
        }
    }

    @Override
    public void end() {
        rollback();
    }
}
