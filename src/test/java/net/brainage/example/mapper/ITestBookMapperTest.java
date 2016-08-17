package net.brainage.example.mapper;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

/**
 * Created by ms29.seo on 2016-08-17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ITestBookMapperTest {

    @Configuration
    @MapperScan(basePackages = {"net.brainage.example.mapper"})
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
        public SqlSessionFactory sessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean ssfb = new SqlSessionFactoryBean();
            ssfb.setDataSource(dataSource);
            ssfb.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
            return ssfb.getObject();
        }

        @Bean
        public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.REUSE);
            return sqlSessionTemplate;
        }

        @Bean
        public SqlSessionTemplate batchSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
            return sqlSessionTemplate;
        }

    }

}
