package org.textprocessors;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class KMeansClusterer {
    public static int EUCLIDEAN = 0;
    public static int COSINE = 1;
    Map<String, Map<String, Double>> tfIdf;
    private int similarityMeasure;
    private int K;
    private Random rand;
    private boolean isPlusPlus = false;

    public KMeansClusterer(Map<String, Map<String, Double>> tfIdf, int similarityMeasure, int K, int centroidSeed, boolean isPP) {
        this.tfIdf = tfIdf;
        this.similarityMeasure = similarityMeasure;
        this.K = K;
        this.rand = new Random(centroidSeed);
        isPlusPlus = isPP;
    }

    private List<String> pickCentroidsRandomly() {
        List<String> fileNames = new ArrayList<>(tfIdf.keySet());
        Set<String> centroids = new HashSet<>();
        for (int i = 0; i < this.K; i++) {
            String cent = "";
            while (cent.isEmpty() || centroids.contains(cent))
                cent = fileNames.get((int) (rand.nextDouble() * (fileNames.size())));
            centroids.add(cent);
        }
        return new ArrayList<>(centroids);
    }

    private List<String> pickCentroidsPlusPlus() {
        List<String> fileNames = new ArrayList<>(tfIdf.keySet());
        List<String> centroids = new ArrayList<>();
        String cent = fileNames.get((int) (Math.random() * (fileNames.size())));
        centroids.add(cent);
        for (int i = 1; i < K; i++) {
            double maxDist = Double.MIN_VALUE, minSim = Double.MAX_VALUE;
            String bestCent = "";
            for (String fileName : fileNames) {
                Map<String, Double> docVec = tfIdf.get(fileName);
                for (int j = 0; j < centroids.size(); j++) {
                    if (fileName.equals(centroids.get(j))) //must choose unique centroids
                        continue;
                    if (similarityMeasure == EUCLIDEAN) {
                        double dist = Math.pow(SimilarityUtil.getEuclideanDist(docVec, tfIdf.get(centroids.get(j))), 2);
                        if (dist > maxDist) {
                            maxDist = dist;
                            bestCent = fileName;
                        }
                    } else if (similarityMeasure == COSINE) {
                        double sim = SimilarityUtil.getCosineSim(docVec, tfIdf.get(centroids.get(j)));
                        if (sim < minSim) {
                            minSim = sim;
                            bestCent = fileName;
                        }
                    }
                }
            }
            centroids.add(bestCent);
        }
        return centroids;
    }

    public List<Map<String, Double>> computeCentroids(Map<String, Integer> clusterAssignment, List<Map<String, Double>> centroids) {
        int[] clusterSizes = new int[K];
        for (int i = 0; i < K; i++) {
            clusterSizes[i] = 1; //one for the centroid itself
        }
        for (String fileName : tfIdf.keySet()) {
            int clust = clusterAssignment.get(fileName);
            clusterSizes[clust] += 1;
            SimilarityUtil.addVec(centroids.get(clust), tfIdf.get(fileName));
        }
        for (int i = 0; i < K; i++) {
            Map<String, Double> vec = centroids.get(i);
            for (String keyword : vec.keySet()) {
                vec.put(keyword, vec.get(keyword) / clusterSizes[i]);
            }
        }
        return centroids;
    }

    public Map<String, Integer> generateClusters() {
        Map<String, Integer> clusterAssignment = new HashMap<>();
        for (String fileName : tfIdf.keySet()) {
            clusterAssignment.put(fileName, -1);
        }
        List<String> centroids = isPlusPlus ? pickCentroidsPlusPlus() : pickCentroidsRandomly();
        List<Map<String, Double>> centroidVecs = new ArrayList<>();
        for (String cent : centroids) {
            centroidVecs.add(tfIdf.get(cent));
        }
        if (similarityMeasure == EUCLIDEAN) {
            while (true) {
                //System.out.println(centroidVecs);
                boolean membersChanged = false;
                for (String fileName : tfIdf.keySet()) {
                    double minDist = Double.MAX_VALUE;
                    int cluster = -1;
                    for (int i = 0; i < K; i++) {
                        Map<String, Double> centVec = centroidVecs.get(i);
                        double eucDist = SimilarityUtil.getEuclideanDist(centVec, tfIdf.get(fileName));
                        if (eucDist < minDist) {
                            minDist = eucDist;
                            cluster = i;
                        }
                    }
                    int prevCluster = clusterAssignment.get(fileName);
                    if (prevCluster != cluster) {
                        membersChanged = true;
                        clusterAssignment.put(fileName, cluster);
                    }
                }
                centroidVecs = computeCentroids(clusterAssignment, centroidVecs);
                if (!membersChanged) //Stopping condition
                    break;
            }
        }
        if (similarityMeasure == COSINE) {
            while (true) {
                boolean membersChanged = false;
                for (String fileName : tfIdf.keySet()) {
                    double maxSim = -1.0;
                    int cluster = -1;
                    for (int i = 0; i < K; i++) {
                        Map<String, Double> centVec = centroidVecs.get(i);
                        double cosSim = SimilarityUtil.getCosineSim(centVec, tfIdf.get(fileName));
                        if (cosSim > maxSim) {
                            maxSim = cosSim;
                            cluster = i;
                        }
                    }
                    int prevCluster = clusterAssignment.get(fileName);
                    if (prevCluster != cluster) {
                        membersChanged = true;
                        clusterAssignment.put(fileName, cluster);
                    }
                }
                centroidVecs = computeCentroids(clusterAssignment, centroidVecs);
                if (!membersChanged)
                    break;
            }

        }
        return clusterAssignment;
    }
}
