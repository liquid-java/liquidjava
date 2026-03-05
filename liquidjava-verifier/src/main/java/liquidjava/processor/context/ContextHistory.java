package liquidjava.processor.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

public class ContextHistory {
    private static ContextHistory instance;

    private Map<String, Map<String, Set<RefinedVariable>>> fileScopeVars; // file -> (scope -> variables in scope)
    private Set<RefinedVariable> instanceVars;
    private Set<RefinedVariable> globalVars;
    private Set<GhostState> ghosts;
    private Set<AliasWrapper> aliases;

    private ContextHistory() {
        fileScopeVars = new HashMap<>();
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
        fileScopeVars.clear();
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
        fileScopeVars.putIfAbsent(file, new HashMap<>());
        fileScopeVars.get(file).put(scope, new HashSet<>(context.getAllCtxVars()));

        // add other elements in context
        instanceVars.addAll(context.getCtxInstanceVars());
        globalVars.addAll(context.getCtxGlobalVars());
        ghosts.addAll(context.getGhostStates());
        aliases.addAll(context.getAliases());
    }

    public String getScopePosition(CtElement element) {
        SourcePosition pos = element.getPosition();
        SourcePosition innerPosition = pos;
        if (element instanceof CtExecutable<?> executable) {
            if (executable.getBody() != null)
                innerPosition = executable.getBody().getPosition();
        }
        return String.format("%d:%d-%d:%d", innerPosition.getLine(), innerPosition.getColumn() + 1, pos.getEndLine(),
                pos.getEndColumn());
    }

    public Map<String, Map<String, Set<RefinedVariable>>> getFileScopeVars() {
        return fileScopeVars;
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
