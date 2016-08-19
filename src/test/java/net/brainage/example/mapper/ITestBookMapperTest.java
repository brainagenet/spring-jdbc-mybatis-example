package net.brainage.example.mapper;

import net.brainage.example.core.tx.Transaction;
import net.brainage.example.core.tx.TransactionImpl;
import net.brainage.example.model.TestBook;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Created by ms29.seo on 2016-08-17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
public class ITestBookMapperTest {

    @Configuration
    @MapperScan(basePackages = {"net.brainage.example.mapper"}, sqlSessionTemplateRef = "sqlSessionTemplate")
    static class TestConfig {

        @Bean
        public DataSource dataSource() {
            String driverClass = "oracle.jdbc.OracleDriver";
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverClass);
            dataSource.setUrl("jdbc:oracle:thin:@localhost:1521:XE");
            dataSource.setUsername("scott");
            dataSource.setPassword("tiger");
            return dataSource;
        }

        @Bean
        public SqlSessionFactory sqlSessionFactory() throws Exception {
            SqlSessionFactoryBean ssfb = new SqlSessionFactoryBean();
            ssfb.setDataSource(dataSource());
            ssfb.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
            // ssfb.setPlugins();
            return ssfb.getObject();
        }

        @Bean(name = "sqlSessionTemplate")
        public SqlSessionTemplate sqlSessionTemplate() throws Exception {
            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory(), ExecutorType.REUSE);
            return sqlSessionTemplate;
        }

        @Bean
        public SqlSessionTemplate batchSqlSessionTemplate() throws Exception {
            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory(), ExecutorType.BATCH);
            return sqlSessionTemplate;
        }

        @Bean
        public SqlMapMapper sqlMapMapper(@Qualifier("sqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) throws Exception {
            SqlMapMapper sqlMapMapper = new SqlMapMapper();
            sqlMapMapper.setSqlSessionTemplate(sqlSessionTemplate());
            return sqlMapMapper;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dataSource);
            return dataSourceTransactionManager;
        }


        @Bean
        public SqlMapMapper batchSqlMapMapper(@Qualifier("batchSqlSessionTemplate") SqlSessionTemplate batchSqlSessionTemplate) {
            SqlMapMapper sqlMapMapper = new SqlMapMapper();
            sqlMapMapper.setSqlSessionTemplate(batchSqlSessionTemplate);
            return sqlMapMapper;
        }

    }

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    SqlMapMapper sqlMapMapper;

    @Autowired
    TestBookBackupMapper testBookBackupMapper;

    @Autowired
    @Qualifier("batchSqlMapMapper")
    SqlMapMapper batchSqlMapMapper;

    @Test
    public void test_for_backup_book() {

        final Transaction transaction = Transaction.Builder.of(transactionManager)
                .propagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED)
                .isolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED)
                .readOnly(false)
                .build();

        /*
        final TransactionImpl transaction = new TransactionImpl(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transaction.setReadOnly(false);
        */

        try {
            transaction.start();

            final List<TestBook> books = new ArrayList<TestBook>();

            sqlMapMapper.select("net.brainage.example.mapper.TestBookMapper.getAll", new ResultHandler() {
                @Override
                public void handleResult(ResultContext resultContext) {
                    TestBook book = (TestBook) resultContext.getResultObject();
                    books.add(book);

                    if (books.size() == 100) {
                        testBookBackupMapper.insertBatch(books);
                        books.clear();
                    }
                }
            });

            if (books.size() > 0) {
                testBookBackupMapper.insertBatch(books);
                books.clear();
            }

            transaction.rollback();
        } finally {
            transaction.end();
        }

    }


    @Test
    public void test_for_bulk_update_with_batch_mode() {

        final Transaction transaction = Transaction.Builder.of(transactionManager)
                .propagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED)
                .isolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED)
                .readOnly(false)
                .build();

        try {
            transaction.start();

            batchSqlMapMapper.select("net.brainage.example.mapper.TestBookMapper.getAll", new ResultHandler() {
                @Override
                public void handleResult(ResultContext resultContext) {
                    TestBook book = (TestBook) resultContext.getResultObject();

                    TestBook newBook = new TestBook();
                    newBook.setId(book.getId());


                    newBook.setOriginPrice(book.getOriginPrice() + (book.getOriginPrice() * ((resultContext.getResultCount() % 5) / 100)));

                    batchSqlMapMapper.update("net.brainage.example.mapper.TestBookMapper.updateOriginPrice", newBook);
                    if (resultContext.getResultCount() % 100 == 0) {
                        batchSqlMapMapper.getSqlSession().flushStatements();
                    }
                    if (resultContext.isStopped()) {
                        batchSqlMapMapper.getSqlSession().flushStatements();
                    }
                }
            });

            transaction.commit();
        } finally {
            transaction.end();
        }

    }

//    @Test
//    public void test_for_backup_book() {
//
//        final TransactionImpl transaction = new TransactionImpl(transactionManager);
//        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
//        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
//        transaction.setReadOnly(false);
//
//        try {
//            transaction.start();
//            sqlMapMapper.select("net.brainage.example.mapper.TestBookMapper.getAll", new ResultHandler() {
//                @Override
//                public void handleResult(ResultContext resultContext) {
//                    TestBook book = (TestBook) resultContext.getResultObject();
//                    testBookBackupMapper.insert(book);
//                }
//            });
//            transaction.rollback();
//        } finally {
//            transaction.end();
//        }
//    }

    @Test
    public void some() {
        BigDecimal price = new BigDecimal(10000);
        BigDecimal discount = price.multiply(new BigDecimal((3 % 5) / 100));
        BigDecimal newPrice = price.subtract(discount);

        System.out.println(">>>>>> " + (3 % 5));

        System.out.println(price + " / " + discount + " / " + newPrice);
    }

}
