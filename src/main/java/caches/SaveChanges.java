package caches;

import caches.changes.AddChange;
import caches.changes.DeleteChange;
import caches.changes.ModifyChange;
import caches.changes.RenameChange;
import caches.records.FilePointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class SaveChanges {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ModifyChangeFromJSON {

        long timestamp;
        String uri;
        String oldText;
        String newText;

    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class CreateFileChangeFromJSON {

        long timestamp;
        String uri;
        String text;

    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DeleteFileChangeFromJSON {

        long timestamp;
        String uri;
        String text;

    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class RenameFileChangeFromJSON {

        long timestamp;
        String oldUri;
        String newUri;
        String text;

    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Changes {

        List<ModifyChangeFromJSON> modifyChanges;
        List<CreateFileChangeFromJSON> createChanges;
        List<DeleteFileChangeFromJSON> deleteChanges;
        List<RenameFileChangeFromJSON> renameChanges;

    }

    public static void main(String[] args) throws JsonProcessingException {
        System.out.println(args[0]);
        ObjectMapper objectMapper = new ObjectMapper();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals("changes")) {
                line = scanner.nextLine();
                Changes changes = objectMapper.readValue(line, Changes.class);
                for (ModifyChangeFromJSON modifyChangeFromJSON : changes.modifyChanges) {
                    ModifyChange modifyChange = new ModifyChange(modifyChangeFromJSON.timestamp,
                            () -> modifyChangeFromJSON.oldText,
                            () -> modifyChangeFromJSON.newText,
                            new File(modifyChangeFromJSON.uri),
                            new File(modifyChangeFromJSON.uri));
                }
                for (CreateFileChangeFromJSON createFileChangeFromJSON : changes.createChanges) {
                    AddChange addChange = new AddChange(createFileChangeFromJSON.timestamp,
                            new FilePointer(new File(createFileChangeFromJSON.uri), 0),
                            createFileChangeFromJSON.text);
                }
                for (DeleteFileChangeFromJSON deleteFileChangeFromJSON : changes.deleteChanges) {
                    DeleteChange deleteChange = new DeleteChange(deleteFileChangeFromJSON.timestamp,
                            new FilePointer(new File(deleteFileChangeFromJSON.uri), 0),
                            deleteFileChangeFromJSON.text);
                }
                for (RenameFileChangeFromJSON renameFileChangeFromJSON : changes.renameChanges) {
                    RenameChange renameChange = new RenameChange(renameFileChangeFromJSON.timestamp,
                            () -> renameFileChangeFromJSON.text,
                            () -> renameFileChangeFromJSON.text,
                            new File(renameFileChangeFromJSON.oldUri),
                            new File(renameFileChangeFromJSON.newUri));
                }
            } else if (line.equals("search")) {
                line = scanner.nextLine();
            }
        }
    }
}