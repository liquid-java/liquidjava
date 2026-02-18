package liquidjava.processor.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

public class ContextHistory {
    private static ContextHistory instance;
    
    private Map<String, Set<RefinedVariable>> vars; // scope -> variables in scope
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
    
        String scope = String.format("%s:%d:%d", pos.getFile().getName(), pos.getLine(), pos.getColumn());
        vars.put(scope, new HashSet<>(context.getCtxVars()));
        instanceVars.addAll(context.getCtxInstanceVars());
        globalVars.addAll(context.getCtxGlobalVars());
        ghosts.addAll(context.getGhostStates());
        aliases.addAll(context.getAliases());
    }

    public Map<String, Set<RefinedVariable>> getVars() {
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
