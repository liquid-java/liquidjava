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

    private Map<String, Set<String>> fileScopes;
    private Set<RefinedVariable> localVars;
    private Set<GhostState> ghosts;
    private Set<AliasWrapper> aliases;
    private Set<RefinedVariable> globalVars;

    private ContextHistory() {
        fileScopes = new HashMap<>();
        localVars = new HashSet<>();
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
        fileScopes.clear();
        localVars.clear();
        globalVars.clear();
        ghosts.clear();
        aliases.clear();
    }

    public void saveContext(CtElement element, Context context) {
        String file = Utils.getFile(element);
        if (file == null)
            return;

        // add scope
        String scope = getScopePosition(element);
        fileScopes.putIfAbsent(file, new HashSet<>());
        fileScopes.get(file).add(scope);

        // add variables, ghosts and aliases
        localVars.addAll(context.getCtxVars());
        localVars.addAll(context.getCtxInstanceVars());
        globalVars.addAll(context.getCtxGlobalVars());
        ghosts.addAll(context.getGhostStates());
        aliases.addAll(context.getAliases());
    }

    private String getScopePosition(CtElement element) {
        CtElement startElement = element instanceof CtParameter<?> ? element.getParent() : element;
        SourcePosition annPosition = Utils.getFirstLJAnnotationPosition(startElement);
        SourcePosition pos = element.getPosition();
        return String.format("%d:%d-%d:%d", annPosition.getLine(), annPosition.getColumn(), pos.getEndLine(),
                pos.getEndColumn());
    }

    public Set<RefinedVariable> getLocalVars() {
        return localVars;
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

    public Map<String, Set<String>> getFileScopes() {
        return fileScopes;
    }
}
