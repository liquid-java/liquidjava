package liquidjava.rj_language.opt.derivation_node;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralLong;
import liquidjava.rj_language.ast.LiteralNull;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.Var;

public class ValDerivationNode extends DerivationNode {

    @JsonAdapter(ExpressionSerializer.class)
    private final Expression value;
    private final DerivationNode origin;

    public ValDerivationNode(Expression exp, DerivationNode origin) {
        this.value = exp;
        this.origin = origin;
    }

    public Expression getValue() {
        return value;
    }

    public DerivationNode getOrigin() {
        return origin;
    }

    // Custom serializer to handle Expression subclasses properly
    private static class ExpressionSerializer implements JsonSerializer<Expression> {
        @Override
        public JsonElement serialize(Expression exp, Type typeOfSrc, JsonSerializationContext context) {
            if (exp == null)
                return JsonNull.INSTANCE;
            if (exp instanceof LiteralInt)
                return new JsonPrimitive(((LiteralInt) exp).getValue());
            if (exp instanceof LiteralLong)
                return new JsonPrimitive(((LiteralLong) exp).getValue());
            if (exp instanceof LiteralReal)
                return new JsonPrimitive(((LiteralReal) exp).getValue());
            if (exp instanceof LiteralBoolean)
                return new JsonPrimitive(exp.isBooleanTrue());
            if (exp instanceof LiteralNull)
                return JsonNull.INSTANCE;
            if (exp instanceof Var)
                return new JsonPrimitive(((Var) exp).getName());
            return new JsonPrimitive(exp.toString());
        }
    }
}
