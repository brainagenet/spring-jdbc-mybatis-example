package net.brainage.example.core.tx;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * Programatic Transaction 처리를 위한 추상화 Interface 를 정의합니다.
 *
 * Transaction tx = Transaction.Builder.of(transactionManager)
 *     .pro
 *     .build();
 */
public interface Transaction {

    void start();

    void commit();

    void rollback();

    void end();

    public static class Builder {

        private TransactionImpl transaction;

        private Builder(PlatformTransactionManager transactionManager) {
            this.transaction = new TransactionImpl(transactionManager);
        }

        public static Builder of(PlatformTransactionManager transactionManager) {
            return new Builder(transactionManager);
        }

        public Builder propagationBehavior(int propagationBehavior) {
            this.transaction.setPropagationBehavior(propagationBehavior);
            return this;
        }

        public Builder isolationLevel(int isolationLevel) {
            this.transaction.setIsolationLevel(isolationLevel);
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.transaction.setReadOnly(readOnly);
            return this;
        }

        public Transaction build() {
            return this.transaction;
        }
    }

}
