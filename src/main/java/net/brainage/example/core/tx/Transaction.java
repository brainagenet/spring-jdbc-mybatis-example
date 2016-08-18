package net.brainage.example.core.tx;

/**
 * Created by ms29.seo on 2016-08-18.
 */
public interface Transaction {

    void start();

    void commit();

    void rollback();

    void end();

}
