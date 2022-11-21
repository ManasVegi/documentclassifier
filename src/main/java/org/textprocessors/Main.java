package org.textprocessors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        //Read files and store in List
        String dataDirs = "";
        try {
            dataDirs = Files.readString(Path.of("data.txt"), StandardCharsets.UTF_8);

        } catch (IOException e) {
            System.err.println(e);
        }
        //https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java
        Map<String, String> documents = new HashMap<>();
        for (String dataDir : dataDirs.split("\n")) {
            try (Stream<Path> paths = Files.walk(Paths.get(dataDir))) {
                paths
                    .filter(Files::isRegularFile)
                    .forEach((p) -> {
                        try {
                            //reading entire file might pose big issue for large files but ignoring for now
                            documents.put(p.toString(), Files.readString(p).toLowerCase());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            } catch (IOException e) {
                System.err.println("Error while walking folder: " + e.getMessage());
            }
        }
        Preprocessor preprocessor = new Preprocessor(documents);
        preprocessor.removeStopWords();
        preprocessor.tokenizeAndCalculateTfIdf();
        Map<String, Map<String, Double>> folderTopics = preprocessor.generateFolderTopics();
        for (String folder : folderTopics.keySet()) {
            List<String> folderKeys = new ArrayList<>(folderTopics.get(folder).keySet());
            Collections.sort(folderKeys, Comparator.comparing(k -> folderTopics.get(folder).get(k)));
            System.out.println("Top 15 keywords in " + folder + ": " + folderKeys.subList(0,15));
        }
        int ANSWER_TO_EVERYTHING = 43;

        System.out.println("Results from KMeans with Cosine:");
        KMeansClusterer kmeans = new KMeansClusterer(preprocessor.getTfIdf(), KMeansClusterer.COSINE, 3, ANSWER_TO_EVERYTHING, false);
        Map<String, Integer> clusterAss = kmeans.generateClusters();
        Map<Integer, List<String>> clusters = new HashMap<>();
        for (String fileName : clusterAss.keySet()) {
            int clust = clusterAss.get(fileName);
            if (clusters.containsKey(clust)) {
                clusters.get(clust).add(fileName);
            } else {
                List<String> list = new ArrayList<>();
                list.add(fileName);
                clusters.put(clust, list);
            }
        }
        System.out.println(clusters);
        Performance perfCalculator = new Performance(Arrays.asList("data/C7", "data/C4", "data/C1"), clusters);
        System.out.println("Confusion matrix with predicted label as rows and actual as keys within");
        System.out.println(perfCalculator.getConfusionMatrix());
//        System.out.println(perfCalculator.clusterLabelling());
        for (int i = 0; i < 3; i++) {
            perfCalculator.getF1(i);
        }
        System.out.println("\nResults from KMeans++ with Cosine:");
        KMeansClusterer kmeansPP = new KMeansClusterer(preprocessor.getTfIdf(), KMeansClusterer.COSINE, 3, ANSWER_TO_EVERYTHING, true);
        Map<String, Integer> clusterAssPP = kmeansPP.generateClusters();
        Map<Integer, List<String>> clustersPP = new HashMap<>();
        for (String fileName : clusterAssPP.keySet()) {
            int clust = clusterAssPP.get(fileName);
            if (clustersPP.containsKey(clust)) {
                clustersPP.get(clust).add(fileName);
            } else {
                List<String> list = new ArrayList<>();
                list.add(fileName);
                clustersPP.put(clust, list);
            }
        }
        System.out.println(clustersPP);
        Performance perfCalculatorPP = new Performance(Arrays.asList("data/C7", "data/C4", "data/C1"), clustersPP);
        System.out.println("Confusion matrix with predicted label as rows and actual as keys within");
        System.out.println(perfCalculatorPP.getConfusionMatrix());
//        System.out.println(perfCalculator.clusterLabelling());
        for (int i = 0; i < 3; i++) {
            perfCalculatorPP.getF1(i);
        }
    }
}