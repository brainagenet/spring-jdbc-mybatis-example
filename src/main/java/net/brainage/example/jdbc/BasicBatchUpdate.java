package net.brainage.example.jdbc;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by ms29.seo on 2016-08-17.
 */
public class BasicBatchUpdate {

    public static void main(String... args) throws SQLException {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("oracle.jdbc.OracleDriver");
        ds.setUrl("jdbc:oracle:thin:@localhost:1521:XE");
        ds.setUsername("scott");
        ds.setPassword("tiger");

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement("INSERT INTO TEST_BOOK (BOOK_ID, BOOK_NAME, ORIGIN_PRICE) VALUES (?, ?, ?)");
            for (int i = 1, l = 100_000; i <= l; i++) {
                ps.clearParameters();
                ps.setInt(1, i);
                ps.setString(2, "BOOK_" + i);
                ps.setInt(3, 10000);
                ps.addBatch();
                if ((i % 1000) == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            System.err.println(ex);
        } finally {
            if (ps != null) {
                ps.close();
                ps = null;
            }

            if (conn != null) {
                conn.close();
                conn = null;
            }
        }

    }

}
