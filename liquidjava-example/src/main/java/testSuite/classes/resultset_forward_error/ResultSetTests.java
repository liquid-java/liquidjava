package testSuite.classes.resultset_forward_error;


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
                con.prepareStatement("select typeid from users where username=? and password=?");
        ResultSet rs = pstat.executeQuery();

        // The default ResultSet is TYPE_FORWARD_ONLY, so the next line cannot rewind:
        // beforeFirst() requires a scrollable (allowsBackward) ResultSet.
        // FIX (accepted answers): request a scrollable result set, e.g.
        //   con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
        //                             ResultSet.CONCUR_READ_ONLY);
        // or drop the rewind and read typeID inside the single forward pass.
        rs.beforeFirst(); // State Refinement Error

        return typeID;
    }

    int login2(Connection con, String username, String password) throws SQLException {
        int typeID = 0;
        PreparedStatement pstat =
                con.prepareStatement("select typeid from users where username=? and password=?");
        pstat.setString(1, username);
        pstat.setString(2, password);
        ResultSet rs = pstat.executeQuery();
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
        }
        // VIOLATION: beforeFirst() scrolls backward, illegal on a TYPE_FORWARD_ONLY
        // result set -> SQLException: Result set type is TYPE_FORWARD_ONLY.
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
        float avgsum = parentMessage.getFloat("IMPAVG"); // State Refinement Error
        return avgsum;
    }

        // Branch-sensitive: legal in the then-branch (onRow), illegal in the else-branch (endRows).
    float readAverageVarElse(Connection conn) throws SQLException {
        Statement parentstmt = conn.createStatement();
        ResultSet parentMessage = parentstmt.executeQuery("SELECT SUM(IMPORTANCE) AS IMPAVG FROM MAIL");
        float avgsum = 0.0f;
        boolean b = parentMessage.next();
        if (b) {
            avgsum = parentMessage.getFloat("IMPAVG");
        } else {
            avgsum = parentMessage.getFloat("IMPAVG"); // State Refinement Error
        }
        return avgsum;
    }
}
