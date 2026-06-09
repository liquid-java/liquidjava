package testSuite.classes.resultset_forward_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"onlyForward", "allowsBackward"})
@StateSet({"beforeRow", "onRow", "endRows"})

@ExternalRefinementsFor("java.sql.ResultSet")
public interface ResultSetRefinements {

    @StateRefinement(to = "_ ? onRow(this) : endRows(this)")
    boolean next();

    @StateRefinement(from = "onRow(this)")
    float getFloat(String columnIndex);

    @StateRefinement(from = "allowsBackward()")
    void beforeFirst();

}
