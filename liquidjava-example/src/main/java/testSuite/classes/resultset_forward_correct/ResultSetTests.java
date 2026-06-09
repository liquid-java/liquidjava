package testSuite.classes.resultset_forward_correct;


// SO 6367737 — "ResultSet: Exception: set type is TYPE_FORWARD_ONLY -- why?"
// https://stackoverflow.com/questions/6367737
// Counting rows by iterating, then calling rs.beforeFirst() to rewind and iterate
// again. A default ResultSet is TYPE_FORWARD_ONLY, so the backward scroll throws:
//     java.sql.SQLException: Result set type is TYPE_FORWARD_ONLY
//         at ...JdbcOdbcResultSet.beforeFirst(...)
// Kept as close to the question as possible; the SO lines are wrapped in a method
// taking a Connection. Pure java.sql; compiles with no JDBC driver present.
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ResultSetTests {

     int login(Connection con, String username, String password) throws SQLException {
        int typeID = 0;

        PreparedStatement pstat =
                con.prepareStatement("select typeid from users where username=? and password=?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = pstat.executeQuery();

        rs.beforeFirst(); // State Refinement Error

        return typeID;
    }

    int login2(Connection con, String username, String password) throws SQLException {
        int typeID = 0;
        PreparedStatement pstat =
                con.prepareStatement("select typeid from users where username=? and password=?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        pstat.setString(1, username);
        pstat.setString(2, password);
        ResultSet rs = pstat.executeQuery();
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
        }
        rs.beforeFirst(); // State Refinement Error
        if (rowCount >= 1) {
            while (rs.next()) {
                typeID = rs.getInt(1);
            }
        }
        return typeID;
    }

    float readAverage(Connection conn) throws SQLException {
        Statement parentstmt = conn.createStatement();
        ResultSet parentMessage =
                parentstmt.executeQuery("SELECT SUM(IMPORTANCE) AS IMPAVG FROM MAIL");
        // FIX (from accepted answer): parentMessage.next();
        // VIOLATION: cursor is before the first row; getFloat() with no next().
        if( parentMessage.next()) {
            float avgsum = parentMessage.getFloat("IMPAVG");
            return avgsum;
        } else {
            return 0.0f;
        }
    }
}
