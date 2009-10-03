/**
 * Obsolete, new code would be Groovy scripts (if to use Java), or Python for general tasks
 */

package magicstudio.netizen.tianya;

import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Control the retrieving/parsing process
 */
public class Main {

    public static void main(String[] args) {
        Harvester harvester = new Harvester(4);
        Parser parser = new Parser();
        try {
            //harvester.harvest();
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDataSource();
    }

    private static DataSource dataSource = null;
    private static Logger logger = null;

    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("tianya");
        }
        return logger;
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            BasicDataSource ds = new BasicDataSource();
            //SharedPoolDataSource ds = new SharedPoolDataSource();
            ds.setDriverClassName("com.mysql.jdbc.Driver");
            ds.setUrl("jdbc:mysql://141.211.184.206/netizen?user=remote&password=mercyme&useUnicode=True&characterEncoding=utf8");
            ds.setMaxWait(120000);
            ds.setNumTestsPerEvictionRun(-1);
            ds.setTimeBetweenEvictionRunsMillis(180000);
            ds.setMinEvictableIdleTimeMillis(120000);
            ds.setTestWhileIdle(true);
            dataSource = ds;
        }
        return dataSource.getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null) {
            BasicDataSource ds = (BasicDataSource) dataSource;
            try {
                ds.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dataSource = null;
        }
    }
}
