package liquidjava.processor.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;

public class ContextHistory {
    private static ContextHistory instance;

    private Map<String, Map<String, Set<RefinedVariable>>> fileScopeVars; // file -> (scope -> variables in scope)
    private Map<String, Set<GhostState>> ghosts; // file -> ghosts

    // globals
    private Set<AliasWrapper> aliases;
    private Set<RefinedVariable> instanceVars;
    private Set<RefinedVariable> globalVars;

    private ContextHistory() {
        fileScopeVars = new HashMap<>();
        instanceVars = new HashSet<>();
        globalVars = new HashSet<>();
        ghosts = new HashMap<>();
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
        String file = getFile(element);
        if (file == null)
            return;

        // add variables in scope
        String scope = getScopePosition(element);
        fileScopeVars.putIfAbsent(file, new HashMap<>());
        fileScopeVars.get(file).put(scope, new HashSet<>(context.getCtxVars()));

        // add other elements in context (except ghosts)
        instanceVars.addAll(context.getCtxInstanceVars());
        globalVars.addAll(context.getCtxGlobalVars());
        aliases.addAll(context.getAliases());
    }

    public void saveGhost(CtElement element, GhostState ghost) {
        String file = getFile(element);
        if (file == null)
            return;
        ghosts.putIfAbsent(file, new HashSet<>());
        ghosts.get(file).add(ghost);
    }

    private String getFile(CtElement element) {
        SourcePosition pos = element.getPosition();
        if (pos == null || pos.getFile() == null)
            return null;

        return pos.getFile().getAbsolutePath();
    }

    public String getScopePosition(CtElement element) {
        CtElement startElement = element instanceof CtParameter<?> ? element.getParent() : element;
        SourcePosition annPosition = Utils.getFirstAnnotationPosition(startElement);
        SourcePosition pos = element.getPosition();
        return String.format("%d:%d-%d:%d", annPosition.getLine(), annPosition.getColumn() + 1, pos.getEndLine(),
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

    public Map<String, Set<GhostState>> getGhosts() {
        return ghosts;
    }

    public Set<AliasWrapper> getAliases() {
        return aliases;
    }
}
