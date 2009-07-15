package magicstudio.netizen.tianya;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;


/**
 * Harvest conversation files and save to database
 */
public class Harvester {

    private String urlTemplate = "http://www.tianya.cn/publicforum/content/news/1/%index.shtml";
    private int sourceId;

    public Harvester(int sourceId) {
        this.sourceId = sourceId;
    }

    public void harvest() throws Exception {
        Logger logger = Main.getLogger();
        HttpClient httpclient = new DefaultHttpClient();
        // this is all 2008 posts
        int startIndex = 94143; //91217;
        int endIndex = 94143; //113745;
        //for (int index=startIndex; index<endIndex; index++) {
        int index = startIndex;
        while (true) {
            try {
                if (index > endIndex) break;
                String url = urlTemplate.replace("%index", Integer.toString(index));
                logger.info("Processing " + url);
                HttpGet httpget = new HttpGet(url);
                HttpResponse response = null;
                response = httpclient.execute(httpget);
                int status = response.getStatusLine().getStatusCode();

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "GBK"));
                    int ch;
                    StringBuffer sb = new StringBuffer();
                    while ((ch = reader.read()) != -1) {
                        sb.append((char)ch);
                    }
                    instream.close();
                    saveToDatabase(url, status, sb);
                }
                index++;
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(120000);
            }
        }

    }

    private void saveToDatabase(String url, int status, StringBuffer sb) throws SQLException {
        Connection conn = Main.getConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO `file`(sid, url, content, `status`) VALUE(?, ?, ?, ?)");
        stmt.setInt(1, sourceId);
        stmt.setString(2, url);
        stmt.setString(3, sb.toString());
        stmt.setInt(4, status);
        stmt.executeUpdate();
        stmt.close();
        conn.close();        
    }
}
