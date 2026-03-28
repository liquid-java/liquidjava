package liquidjava.rj_language.opt.derivation_node;

public class IteDerivationNode extends DerivationNode {

    private final ValDerivationNode condition;
    private final ValDerivationNode thenBranch;
    private final ValDerivationNode elseBranch;

    public IteDerivationNode(ValDerivationNode condition, ValDerivationNode thenBranch, ValDerivationNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public ValDerivationNode getCondition() {
        return condition;
    }

    public ValDerivationNode getThenBranch() {
        return thenBranch;
    }

    public ValDerivationNode getElseBranch() {
        return elseBranch;
    }
}