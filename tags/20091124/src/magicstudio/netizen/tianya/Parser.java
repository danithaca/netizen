/**
 * Obsolete, new scripts to parse HTML file is in py_scripts
 */

package magicstudio.netizen.tianya;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    public static enum Status {
        OK (1000),
        UNEXPECTED_PARSING_ERROR(1200),
        NO_FIRST_POST(1201),
        REPLY_ERROR(1202),
        HTML_CODE (1100);
        private final int value;
        Status(int value) {
            this.value = value;
        }
    }

    private static class ParsingException extends Exception {
        public Status getCode() {
            return errCode;
        }
        private Status errCode;
        public ParsingException(Status code) {
            errCode = code;
        }
    }

    public void parse() throws Exception {
        Logger logger = Main.getLogger();
        Connection conn = Main.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select fid from file where sid=4 and status=200 order by fid");
        List<Integer> fileIds = new ArrayList<Integer>();
        while (rs.next()) {
            fileIds.add(rs.getInt(1));
        }
        conn.close();
        logger.info("Total items to process: "+fileIds.size());
        for (int fid : fileIds) {
            String content = getContent(fid);
            Status code;
            logger.info("Processing "+fid);
            try {
                code = parseFile(fid, content);
            } catch (ParsingException e) {
                e.printStackTrace();
                code = e.getCode();
            } catch (Exception e) {
                e.printStackTrace();
                code = Status.UNEXPECTED_PARSING_ERROR;
            }
            updateStatus(fid, code);
        }
    }

    private String getContent(int fid) throws SQLException {
        Connection conn = Main.getConnection();
        Statement contentSql = conn.createStatement();
        ResultSet contentRs = contentSql.executeQuery("select content from file where fid="+fid);
        contentRs.first();
        String content = contentRs.getString(1);
        conn.close();
        return content;
    }

    private void updateStatus(int fid, Status code) throws Exception{
        Connection conn = Main.getConnection();
        PreparedStatement stmt = conn.prepareStatement("update file set status=? where fid=?");
        stmt.setInt(2, fid);
        stmt.setInt(1, code.value);
        stmt.executeUpdate();
        conn.close();
    }

    private Status parseFile(int fid, String content) throws Exception {
        if (content.startsWith("<!DOCTYPE") && content.indexOf("<title>ERROR:</title>")!=0
                && content.indexOf("抱歉，您所访问的页面不存在，该页面可能已被删除、更名或暂时不可用。")!=0) {
            return Status.HTML_CODE;
        }

        Pattern pattern;
        Matcher matcher;
        int position;

        pattern = Pattern.compile("<TITLE>.*?</TITLE>.*?<!-- 天涯百宝箱 -->.*?var intArticleId = \"(\\d+)\".*?<!-- 天涯百宝箱 -->", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        matcher.find();
        int thread_id = new Integer(matcher.group(1));
        position = matcher.end();

        pattern = Pattern.compile("<table .*>』?\\w*(.*?)\\w*</font></TD></TR></table></td></table>", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        matcher.find(position);
        String title = matcher.group(1);
        position = matcher.end();

        pattern = Pattern.compile("提交日期：([0-9 \\-:　]*?)访问：(\\d+?) 回复：(\\d+)");
        matcher = pattern.matcher(content);
        matcher.region(position, position+5000);
        if(!matcher.find()) throw new ParsingException(Status.NO_FIRST_POST);

        pattern = Pattern.compile("<TABLE .*?作者：<a.*?>(.+?)</a>.*?提交日期：([0-9 \\-:　]*?)访问：(\\d+?) 回复：(\\d+).*?</table>", Pattern.DOTALL);
        matcher = pattern.matcher(content);
        matcher.find(position);   // Note: this line sometimes causes lots of time if there's no 访问，回复。
        String author = matcher.group(1);
        String submit_date = matcher.group(2);
        int visit_no = new Integer(matcher.group(3));
        int reply_no = new Integer(matcher.group(4));
        position = matcher.end();

        pattern = Pattern.compile("^<div .*>$\\s*^(.*?)$", Pattern.MULTILINE);
        matcher = pattern.matcher(content);
        matcher.find(position);
        String body = stripHtml(matcher.group(1));
        position = matcher.end(1);

        saveToDatabase(fid, title, author, formatDatetime(submit_date), body, thread_id, -1, visit_no, reply_no);

        // skip the ad table block
        /*pattern = Pattern.compile("<TABLE (.*?)$\\s*", Pattern.MULTILINE);
        matcher = pattern.matcher(content);
        matcher.find(position);
        position = matcher.end();*/

        pattern = Pattern.compile("^(<TABLE .*?)$\\s*^(.*?)$", Pattern.MULTILINE);
        matcher = pattern.matcher(content);
        Pattern responseTitlePattern = Pattern.compile("<TABLE .*?作者：<a.*?>(.+?)</a>.*?回复日期：([0-9 \\-:　]*)");
        while (true) {
            if (!matcher.find(position)) break; // only exit of the loop
            String responseTitle = matcher.group(1);
            String responseBody = matcher.group(2);
            Matcher m = responseTitlePattern.matcher(responseTitle);
            m.find();
            String responseAuthor = m.group(1);
            String responseSubmit = m.group(2);
            int replyTo = findReply(thread_id, responseBody);
            saveToDatabase(fid, null, responseAuthor, formatDatetime(responseSubmit), responseBody, thread_id, replyTo, 0, 0);
            position = matcher.end();
        }
        return Status.OK;
    }

    private int findReply(int threadId, String body) throws Exception {
        int replyTo = 0;
        Pattern pattern = Pattern.compile("[　 ]*作者：(.+?)[　 ]+回复日期：([0-9 \\-:　]*)");
        Matcher matcher = pattern.matcher(body);
        if (matcher.lookingAt()) {
            String author = matcher.group(1);
            Timestamp submit = formatDatetime(matcher.group(2));
            Connection conn = Main.getConnection();
            PreparedStatement stmt = conn.prepareStatement("select pid from post where thread_id=? and author=? and submit_date=?");
            stmt.setInt(1, threadId);
            stmt.setString(2, author);
            stmt.setTimestamp(3, submit);
            ResultSet rs = stmt.executeQuery();
            if(!rs.first()) {
                conn.close();
                throw new ParsingException(Status.REPLY_ERROR); // assume only one results
            }
            replyTo = rs.getInt(1);
            conn.close();
        }
        return replyTo;
    }

    private String stripHtml(String html) {
        // note that it does not string the same number of leading and trailing <>s
        Pattern pattern = Pattern.compile("(?:\\s*<.*?>\\s*)*(.*?)(?:\\s*</.*?>\\s*)*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (!matcher.matches()) throw new RuntimeException("Can't find match");
        String strip = matcher.group(1);
        return strip.trim();
    }

    private Timestamp formatDatetime(String datetime) throws ParseException {
        datetime = datetime.replace('　', ' ').trim();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new Timestamp(df.parse(datetime).getTime());
    }

    private void saveToDatabase(int fid, String title, String author, Timestamp submitDate, String body,
                                int threadId, int replyTo, int viewNo, int replyNo) throws SQLException {
        Connection conn = Main.getConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO post(fid, title, author, submit_date, body, thread_id, reply_to, view_no, reply_no) " +
                "VALUE(?, ?, ?, ?, ?, ?, ?, ?, ?)");
        stmt.setInt(1, fid);
        stmt.setString(2, title);
        stmt.setString(3, author);
        stmt.setTimestamp(4, submitDate);
        stmt.setString(5, body);
        stmt.setInt(6, threadId);
        stmt.setInt(7, replyTo);
        stmt.setInt(8, viewNo);
        stmt.setInt(9, replyNo);
        stmt.executeUpdate();
        conn.close();
    }

}
