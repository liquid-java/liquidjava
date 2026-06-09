package testSuite.classes.resultset_forward_correct;

import java.sql.ResultSet;
import java.sql.SQLException;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;


@Ghost("boolean setBackwards")
@ExternalRefinementsFor("java.sql.PreparedStatement")
public interface PreparedStatementRefinements {

    // The ResultSet inherits the statement's scrollability: backward-capable statements
    // yield an allowsBackward ResultSet, forward-only statements yield an onlyForward one.
    @Refinement("setBackwards(this) ? allowsBackward(_) : onlyForward(_)")
    ResultSet executeQuery();
}