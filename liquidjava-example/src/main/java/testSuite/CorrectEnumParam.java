package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class CorrectEnumParam {
    enum Status {
        Active, Inactive, Pending
    }

    Status process(@Refinement("status == Status.Inactive") Status status) {
        return Status.Active;
    }

    public static void main(String[] args) {
        CorrectEnumParam cep = new CorrectEnumParam();
        cep.process(Status.Inactive); // correct
    }
}