package liquidjava.smt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Removes the copy of the Z3 native libraries that the Z3 Java bindings unpack when they load. The bindings unpack
 * about 30 MB into a fresh {@code z3-turnkey*} folder in the temporary directory on every start and delete it only when
 * the JVM exits normally, so every killed verifier (language server, timed-out run) left its folder behind. On Linux a
 * loaded library stays usable after its file is deleted, so the files can go as soon as they are loaded. Elsewhere this
 * does nothing and the folder is still removed at a normal exit
 */
final class Z3NativeLibraries {

    private static final Path PROCESS_MAPPINGS = Path.of("/proc/self/maps");
    private static final String UNPACK_FOLDER_PREFIX = "z3-turnkey";

    private static boolean attempted;

    private Z3NativeLibraries() {
    }

    /**
     * Deletes the unpacked libraries this process has loaded and their folder. Must be called after Z3 is loaded. Only
     * files this process has mapped are touched, never the folders of other running verifiers
     */
    static synchronized void deleteUnpackedFiles() {
        if (attempted || !Files.isReadable(PROCESS_MAPPINGS))
            return;
        attempted = true;
        try {
            Set<Path> libraries = unpackedLibraries(Files.readAllLines(PROCESS_MAPPINGS));
            for (Path library : libraries)
                Files.deleteIfExists(library);
            for (Path folder : libraries.stream().map(Path::getParent).collect(Collectors.toSet()))
                Files.deleteIfExists(folder);
        } catch (IOException e) {
            // keep what is left; it is still deleted when the JVM exits normally
        }
    }

    /**
     * The mapped files that lie directly in a folder unpacked by the Z3 bindings, from the lines of
     * {@code /proc/self/maps} ({@code address perms offset dev inode path})
     */
    private static Set<Path> unpackedLibraries(List<String> mappings) {
        Set<Path> libraries = new LinkedHashSet<>();
        for (String mapping : mappings) {
            String[] fields = mapping.trim().split("\\s+", 6);
            if (fields.length < 6 || !fields[5].startsWith("/"))
                continue;
            Path file = Path.of(fields[5]);
            Path folder = file.getParent();
            if (folder != null && folder.getFileName() != null
                    && folder.getFileName().toString().startsWith(UNPACK_FOLDER_PREFIX))
                libraries.add(file);
        }
        return libraries;
    }
}
