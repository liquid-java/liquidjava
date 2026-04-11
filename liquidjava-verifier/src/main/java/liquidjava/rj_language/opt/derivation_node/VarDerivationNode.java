package liquidjava.rj_language.opt.derivation_node;

public class VarDerivationNode extends DerivationNode {

    private final String var;
    private final DerivationNode origin;

    public VarDerivationNode(String var) {
        this.var = var;
        this.origin = null;
    }

    public VarDerivationNode(String var, DerivationNode origin) {
        this.var = var;
        this.origin = origin;
    }

    public String getVar() {
        return var;
    }

    public DerivationNode getOrigin() {
        return origin;
    }
}
