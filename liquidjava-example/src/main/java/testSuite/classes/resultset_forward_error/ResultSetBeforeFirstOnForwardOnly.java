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

public class ResultSetBeforeFirstOnForwardOnly {

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
}
