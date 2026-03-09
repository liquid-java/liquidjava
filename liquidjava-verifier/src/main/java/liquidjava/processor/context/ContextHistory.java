package liquidjava.processor.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtParameter;

public class ContextHistory {
    private static ContextHistory instance;

    private Map<String, Map<String, Set<RefinedVariable>>> fileScopeVars; // file -> (scope -> variables in scope)
    private Set<GhostState> ghosts;
    private Set<AliasWrapper> aliases;
    private Set<RefinedVariable> instanceVars;
    private Set<RefinedVariable> globalVars;

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
        String file = Utils.getFile(element);
        if (file == null)
            return;

        // add variables in scope
        String scope = getScopePosition(element);
        fileScopeVars.putIfAbsent(file, new HashMap<>());
        fileScopeVars.get(file).put(scope, new HashSet<>(context.getCtxVars()));

        // add other elements in context (except ghosts)
        instanceVars.addAll(context.getCtxInstanceVars());
        globalVars.addAll(context.getCtxGlobalVars());
        ghosts.addAll(context.getGhostStates());
        aliases.addAll(context.getAliases());
    }

    private String getScopePosition(CtElement element) {
        CtElement startElement = element instanceof CtParameter<?> ? element.getParent() : element;
        SourcePosition annPosition = Utils.getFirstLJAnnotationPosition(startElement);
        SourcePosition pos = element.getPosition();
        return String.format("%d:%d-%d:%d", annPosition.getLine() - 1, annPosition.getColumn() - 1,
                pos.getEndLine() - 1, pos.getEndColumn() - 1);
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
