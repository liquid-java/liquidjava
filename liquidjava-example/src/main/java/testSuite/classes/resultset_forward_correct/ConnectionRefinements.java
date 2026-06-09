package testSuite.classes.resultset_forward_correct;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;


@ExternalRefinementsFor("java.sql.Connection")
public interface ConnectionRefinements {

    // Default statements are forward-only: the produced ResultSet cannot scroll backwards.
    @Refinement("setBackwards(_) == false")
    PreparedStatement prepareStatement(String sql);

    // Explicit type decides scrollability: only TYPE_FORWARD_ONLY forbids backward scrolling.
    @Refinement("resultSetType == ResultSet.TYPE_FORWARD_ONLY ? setBackwards(_) == false : setBackwards(_) == true")
    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency);

 }
