package net.brainage.example.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Created by ms29.seo on 2016-08-17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BasicBatchUpdateTest {

    @Configuration
    static class TestConfig {

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("oracle.jdbc.OracleDriver");
            ds.setUrl("jdbc:oracle:thin:@localhost:1521:XE");
            ds.setUsername("scott");
            ds.setPassword("tiger");
            return ds;
        }

    }

    @Autowired
    DataSource dataSource;

    @Test
    public void test_jdbc_batch_update() throws SQLException {
        long start = System.currentTimeMillis();
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement ups = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement("SELECT BOOK_ID FROM TEST_BOOK");
            rs = ps.executeQuery();

            ups = conn.prepareStatement("UPDATE TEST_BOOK SET ORIGIN_PRICE = 0 WHERE BOOK_ID = ?");
            int updateCount = 0;
            while (rs.next()) {
                ups.clearParameters();
                ups.setString(1, rs.getString("BOOK_ID"));
                ups.addBatch();
                updateCount++;

                if (updateCount == 100) {
                    ups.executeBatch();
                    updateCount = 0;
                }
            }
            ups.executeBatch();

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            System.err.println(ex);
        } finally {
            if (rs != null) {
                rs.close();
                rs = null;
            }

            if (ups != null) {
                ups.close();
                ups = null;
            }

            if (ps != null) {
                ps.close();
                ps = null;
            }

            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed time: " + elapsed + "ms");
    }


}
