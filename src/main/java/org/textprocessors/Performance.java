package org.textprocessors;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Performance {
    private int[] actualLabelFrequencies = {8,8,8}; //need to change this appropriately for different number of documents, magic number rigth now
    //need tp for each cluster
    private List<String> labels;
    private Map<Integer, List<String>> clusters;
    private Map<Integer, String> clusterLabelling;

    public Map<String, Map<String, Integer>> getConfusionMatrix() {
        return confusionMatrix;
    }

    private Map<String, Map<String, Integer>> confusionMatrix;
    public Performance(List<String> labels, Map<Integer, List<String>> clusters) {
        this.labels = labels;
        this.clusters = clusters;
        this.clusterLabelling();
        confusionMatrix = new HashMap<>(labels.size());
        for (String label : labels) {
            confusionMatrix.put(label, new HashMap<>());
        }
        createConfusionMatrix();
    }
    private void clusterLabelling() {
        Map<String, int[]> labelFreqs = new HashMap<>();
        for (String label : labels) {
            int[] freqs = new int[clusters.size()];
            for (int i = 0; i < clusters.size(); i++) {
                int folderFreq = 0;
                for (String fileName : clusters.get(i)) {
                    String folder = fileName.substring(0, fileName.lastIndexOf(File.separator));
                    if (folder.equals(label))
                        folderFreq++;
                }
                freqs[i] = folderFreq;
            }
            labelFreqs.put(label, freqs);
        }
        Map<Integer, String> result = new HashMap<>();
        for (String label : labels) {
            int bestFreq = -1, bestCluster = -1;
            int[] freqs = labelFreqs.get(label);
            for (int i = 0; i < clusters.size(); i++) {
                if (freqs[i] > bestFreq && !result.containsKey(i)) {
                    bestFreq = freqs[i];
                    bestCluster = i;
                }
            }
            result.put(bestCluster, label);
        }
        this.clusterLabelling = result;
    }
    private double getPrecision(int cluster) {
        String label = this.clusterLabelling.get(cluster);
        return (double) confusionMatrix.get(label).getOrDefault(label, 0) / clusters.get(cluster).size();
    }
    private double getRecall(int cluster) {
        String label = this.clusterLabelling.get(cluster);
        return (double) confusionMatrix.get(label).getOrDefault(label, 0) / actualLabelFrequencies[cluster];
    }
    public double getF1(int cluster) {
        double prec = getPrecision(cluster), rec = getRecall(cluster);
        double f1 = 2 * (prec * rec ) / (prec + rec);
        System.out.println("F1 score for: " + clusterLabelling.get(cluster) + ": " + f1);
        return f1;
    }
    private void createConfusionMatrix() {
        for (int i = 0; i < labels.size(); i++) {
            String clLabel = this.clusterLabelling.get(i);
            for (String fileName : clusters.get(i)) {
                String folder = fileName.substring(0, fileName.lastIndexOf(File.separator));
                Map<String, Integer> numAssignments = confusionMatrix.get(clLabel);
                numAssignments.put(folder, numAssignments.getOrDefault(folder, 0) + 1);
            }
        }
        for (String label : labels) {
            System.out.println("True positives for cluster " + label + ": " + confusionMatrix.get(label).get(label));
        }
    }
}
