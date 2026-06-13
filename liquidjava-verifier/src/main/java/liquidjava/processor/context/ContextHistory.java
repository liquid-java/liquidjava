package liquidjava.processor.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.DebugLog;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

public class ContextHistory {
    private static ContextHistory instance;

    private Map<String, Set<String>> fileScopes;
    private Set<RefinedVariable> localVars;
    private Set<GhostState> ghosts;
    private Set<AliasWrapper> aliases;
    private Set<RefinedVariable> globalVars;
    private Set<RefinedFunction> methods;

    private ContextHistory() {
        fileScopes = new HashMap<>();
        localVars = new HashSet<>();
        globalVars = new HashSet<>();
        ghosts = new HashSet<>();
        aliases = new HashSet<>();
        methods = new HashSet<>();
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
        methods.clear();
    }

    public void saveContext(CtElement element, Context context) {
        if (!CommandLineLauncher.cmdArgs.lspMode && !CommandLineLauncher.cmdArgs.all)
            return;

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
        methods.addAll(context.getCtxFunctions());

        // Gate lives inside DebugLog.contextAtElement (-a / --all); no-op unless that flag is set.
        DebugLog.contextAtElement(element, prettyPrint());
    }

    private String getScopePosition(CtElement element) {
        SourcePosition startPos = Utils.getRealPosition(element);
        SourcePosition endPos = element.getPosition();
        return String.format("%d:%d-%d:%d", startPos.getLine(), startPos.getColumn(), endPos.getEndLine(),
                endPos.getEndColumn() - 1);
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

    public Set<RefinedFunction> getMethods() {
        return methods;
    }

    public Map<String, Set<String>> getFileScopes() {
        return fileScopes;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder("ContextHistory:\n");

        // FileScopes are intentionally omitted here — they drive LSP hover ranges (see getFileScopes), not debug
        // output.
        appendSection(sb, "LocalVars", localVars);
        appendSection(sb, "GlobalVars", globalVars);
        appendSection(sb, "Ghosts", ghosts);
        appendSection(sb, "Aliases", aliases);
        appendSection(sb, "Methods", methods);

        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, String label, Iterable<?> items) {
        sb.append(label).append(":\n");
        for (Object item : items) {
            sb.append("  - ").append(item).append("\n");
        }
    }

}
