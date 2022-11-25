package org.textprocessors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DocumentUtil {
    public static Map<String, String> readFromFolders(String directoryInfo) {
        String dataDirs = "";
        try {
            dataDirs = Files.readString(Path.of(directoryInfo), StandardCharsets.UTF_8);

        } catch (IOException e) {
            System.err.println(e);
        }
        //https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java
        Map<String, String> documents = new HashMap<>();
        for (String dataDir : dataDirs.split("\n")) {
            walkFolder(dataDir, documents);
        }
        return documents;
    }
    public static void walkFolder(String dataDir, Map<String, String> documents) {
        try (Stream<Path> paths = Files.walk(Paths.get(dataDir))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach((p) -> {
                        try {
                            //reading entire file might pose big issue for large files but ignoring for now
                            documents.put(p.toString(), new String(Files.readAllBytes(p), StandardCharsets.UTF_8).toLowerCase());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error while walking folder: " + e.getMessage());
        }
    }
}
