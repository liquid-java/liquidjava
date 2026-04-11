package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class ErrorEnumNegation {
    enum Status {
        Active, Inactive, Pending
    }

    void process(@Refinement("status != Status.Inactive") Status status) {}

    public static void main(String[] args) {
        ErrorEnumNegation e = new ErrorEnumNegation();
        e.process(Status.Active);
        e.process(Status.Inactive); // Refinement Error
    }
}
