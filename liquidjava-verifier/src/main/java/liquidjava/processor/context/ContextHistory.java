package liquidjava.processor.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

public class ContextHistory {
    private static ContextHistory instance;
    
    private Map<String, Map<String, Set<RefinedVariable>>> vars; // file -> (scope -> variables in scope)
    private Set<RefinedVariable> instanceVars;
    private Set<RefinedVariable> globalVars;
    private Set<GhostState> ghosts;
    private Set<AliasWrapper> aliases;

    private ContextHistory() {
        vars = new HashMap<>();
        instanceVars = new HashSet<>();
        globalVars = new HashSet<>();
        ghosts = new HashSet<>();
        aliases = new HashSet<>();
    }

    public static ContextHistory getInstance() {
        if (instance == null)
            instance = new ContextHistory();
        return instance;
    }

    public void clearHistory() {
        vars.clear();
        instanceVars.clear();
        globalVars.clear();
        ghosts.clear();
        aliases.clear();
    }

    public void saveContext(CtElement element, Context context) {
        SourcePosition pos = element.getPosition();
        if (pos == null || pos.getFile() == null)
            return;

        // add variables in scope for this position
        String file = pos.getFile().getAbsolutePath();
        String scope = getScopePosition(element);
        System.out.println("Saving context for " + file + " in scope " + scope);
        vars.putIfAbsent(file, new HashMap<>());
        vars.get(file).put(scope, new HashSet<>(context.getCtxVars()));
    
        // add other elements in context
        instanceVars.addAll(context.getCtxInstanceVars());
        globalVars.addAll(context.getCtxGlobalVars());
        ghosts.addAll(context.getGhostStates());
        aliases.addAll(context.getAliases());
    }

    public String getScopePosition(CtElement element) {
        SourcePosition pos = element.getPosition();
        SourcePosition innerPosition = element instanceof CtExecutable ? ((CtExecutable<?>) element).getBody().getPosition() : pos;
        return String.format("%d:%d-%d:%d", innerPosition.getLine(), innerPosition.getColumn() + 1, pos.getEndLine(), pos.getEndColumn());
    }

    public Map<String, Map<String, Set<RefinedVariable>>> getVars() {
        return vars;
    }

    public Set<RefinedVariable> getInstanceVars() {
        return instanceVars;
    }

    public Set<RefinedVariable> getGlobalVars() {
        return globalVars;
    }

    public Set<GhostState> getGhosts() {
        return ghosts;
    }

    public Set<AliasWrapper> getAliases() {
        return aliases;
    }
}
