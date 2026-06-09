package testSuite.classes.resultset_forward_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"onlyForward", "allowsBackward"})

@ExternalRefinementsFor("java.sql.ResultSet")
public interface ResultSetRefinements {

    boolean next();

    float getFloat(String columnIndex);

    @StateRefinement(from = "allowsBackward()")
    void beforeFirst();

}
