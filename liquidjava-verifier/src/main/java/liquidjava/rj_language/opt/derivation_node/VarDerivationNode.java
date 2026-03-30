package liquidjava.rj_language.opt.derivation_node;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

import liquidjava.utils.VariableFormatter;

public class VarDerivationNode extends DerivationNode {

    @JsonAdapter(VariableNameSerializer.class)
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

    private static class VariableNameSerializer implements JsonSerializer<String> {
        @Override
        public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(VariableFormatter.formatVariable(src));
        }
    }
}
