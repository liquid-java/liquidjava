package testSuite.classes.car_correct;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@RefinementAlias("Positive(int x) { x > 0}")
@RefinementAlias("CarAcceptableYears(int x) { x > 1800 && x < 2050}")
@RefinementAlias("GreaterThan(int x, int y) {x > y}")
public class Car {

    @Refinement("CarAcceptableYears(year)")
    private int year;

    @Refinement("Positive(_)")
    private int seats;

    public void setYear(@Refinement("CarAcceptableYears(_)") int year) {
        this.year = year;
    }

    @Refinement("CarAcceptableYears(_)")
    public int getYear() {
        return year;
    }

    //	@Refinement("_ == GreaterThan(year, y)")
    //	public boolean isOlderThan(int y) {
    //		return this.year > y;
    //	}

}
