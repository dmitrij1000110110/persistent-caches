package com.github.SuduIDE.persistentidecaches;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.SuduIDE.persistentidecaches.changes.AddChange;
import com.github.SuduIDE.persistentidecaches.changes.Change;
import com.github.SuduIDE.persistentidecaches.changes.DeleteChange;
import com.github.SuduIDE.persistentidecaches.changes.ModifyChange;
import com.github.SuduIDE.persistentidecaches.changes.RenameChange;
import com.github.SuduIDE.persistentidecaches.records.FilePointer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VsCodeClient {

    public static final String SEARCH = "search";
    public static final String CHANGES = "changes";

    public static final String CHECKOUT = "checkout";

    public static final String CC_SEARCH = "ccsearch";
    public static final int BUSY_WAITING_MILLIS = 500;
    private static final char[] BUFFER = new char[16384];

    @SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
    public static void main(final String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            throw new RuntimeException("Needs path to repository as first arg");
        }
        try (final IndexesManager manager = new IndexesManager(true)) {
            final var trigramHistoryIndex = manager.addTrigramIndex();
            final var trigramIndexUtils = trigramHistoryIndex.getTrigramIndexUtils();
            final var repPath = Path.of(args[0]);
            manager.parseGitRepository(repPath);
            manager.getFileCache().forEach((it, iti) -> System.err.println(it + " " + iti));

            final ObjectMapper objectMapper = new ObjectMapper();
            final BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = scanner.readLine();
                Thread.sleep(BUSY_WAITING_MILLIS);
                if (line.equals(CHANGES)) {
                    final var read = scanner.read(BUFFER);
                    line = new String(BUFFER, 0, read);
                    final Changes changes = objectMapper.readValue(line, Changes.class);
                    final List<Change> processedChangesList = new ArrayList<>();
                    for (final ModifyChangeFromJSON modifyChangeFromJSON : changes.modifyChanges) {
                        final Path path = repPath.relativize(Path.of(modifyChangeFromJSON.uri));
                        final ModifyChange modifyChange = new ModifyChange(changes.timestamp,
                                () -> modifyChangeFromJSON.oldText,
                                () -> modifyChangeFromJSON.newText,
                                path,
                                path);
                        processedChangesList.add(modifyChange);
                    }
                    for (final CreateFileChangeFromJSON createFileChangeFromJSON : changes.addChanges) {
                        final AddChange addChange = new AddChange(changes.timestamp,
                                new FilePointer(repPath.relativize(Path.of(createFileChangeFromJSON.uri)), 0),
                                createFileChangeFromJSON.text);
                        processedChangesList.add(addChange);
                    }
                    for (final DeleteFileChangeFromJSON deleteFileChangeFromJSON : changes.deleteChanges) {
                        final DeleteChange deleteChange = new DeleteChange(changes.timestamp,
                                new FilePointer(repPath.relativize(Path.of(deleteFileChangeFromJSON.uri)), 0),
                                deleteFileChangeFromJSON.text);
                        processedChangesList.add(deleteChange);
                    }
                    for (final RenameFileChangeFromJSON renameFileChangeFromJSON : changes.renameChanges) {
                        final RenameChange renameChange = new RenameChange(changes.timestamp,
                                () -> renameFileChangeFromJSON.text,
                                () -> renameFileChangeFromJSON.text,
                                repPath.relativize(Path.of(renameFileChangeFromJSON.oldUri)),
                                repPath.relativize(Path.of(renameFileChangeFromJSON.newUri)));
                        processedChangesList.add(renameChange);
                    }
                    manager.nextRevision();
                    manager.applyChanges(processedChangesList);
                } else if (line.equals(SEARCH)) {
                    final var read = scanner.read(BUFFER);
                    line = new String(BUFFER, 0, read);
                    System.out.println(trigramIndexUtils.filesForString(line));
                } else if (line.equals(CHECKOUT)) {
                    final var read = scanner.read(BUFFER);
                    line = new String(BUFFER, 0, read);
                } else if (line.equals(CC_SEARCH)) {
                    final var read = scanner.read(BUFFER);
                    line = new String(BUFFER, 0, read);
                }
            }
        }
    }

    private record ModifyChangeFromJSON(String uri, String oldText, String newText) {

    }

    private record CreateFileChangeFromJSON(String uri, String text) {

    }

    private record DeleteFileChangeFromJSON(String uri, String text) {

    }

    private record RenameFileChangeFromJSON(String oldUri, String newUri, String text) {

    }

    private record Changes(List<ModifyChangeFromJSON> modifyChanges,
                           List<CreateFileChangeFromJSON> addChanges,
                           List<DeleteFileChangeFromJSON> deleteChanges,
                           List<RenameFileChangeFromJSON> renameChanges,
                           long timestamp) {

    }
}
