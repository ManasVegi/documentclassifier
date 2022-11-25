package org.textprocessors;

import edu.stanford.nlp.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class KNNClassifier {

    public static int EUCLIDEAN = 0;
    public static int FUZZY_M = 2; //determines how heavily distance is weighted
    public static int COSINE = 1;
    Map<String, List<Map<String, Double>>> labelledTfIdf;
    private int similarityMeasure;
    private int K;
    boolean isFuzzy = false;

    public KNNClassifier(Map<String, List<Map<String, Double>>> labelledTfIdf, int k, int similarityMeasure, boolean isFuzzy) {
        this.labelledTfIdf = labelledTfIdf;
        this.K = k;
        this.similarityMeasure = similarityMeasure;
        this.isFuzzy = isFuzzy;
    }
    private PriorityQueue<Pair<Map<String, Double>, String>> getNearestNeighbors(Map<String, Double> inputTfIdf) {
        //priority queue as a max heap
        PriorityQueue<Pair<Map<String, Double>, String>> nearestNeighbors = new PriorityQueue<>(this.K, (m1, m2) -> {
            if (this.similarityMeasure == KNNClassifier.COSINE) {
                return Double.compare(SimilarityUtil.getCosineSim(inputTfIdf, m1.first), SimilarityUtil.getCosineSim(inputTfIdf, m2.first));
            } else {
                return Double.compare(SimilarityUtil.getEuclideanDist(inputTfIdf, m2.first), SimilarityUtil.getEuclideanDist(inputTfIdf, m1.first));
            }
        });
        for (String label : labelledTfIdf.keySet()) {
            List<Map<String, Double>> vectors = labelledTfIdf.get(label);
            for (Map<String, Double> vec : vectors) {
                nearestNeighbors.add(new Pair<>(vec, label));
                if (nearestNeighbors.size() > K) {
                    nearestNeighbors.poll();
                }
            }
        }
        return nearestNeighbors;
    }
    public String classifyInputCrisp(Map<String, Double> inputTfIdf) {
        PriorityQueue<Pair<Map<String, Double>, String>> nearestNeighbors = getNearestNeighbors(inputTfIdf);
        Map<String, Integer> votes = new HashMap<>();
        for (Pair<Map<String, Double>, String> pair : nearestNeighbors) {
            votes.put(pair.second, votes.getOrDefault(pair.second, 0) + 1);
        }
        System.out.println(votes);
        int max = 0;
        String winner = "";
        for (String vote : votes.keySet()) {
            if (votes.get(vote) > max) {
                max = votes.get(vote);
                winner = vote;
            }
        }
        return winner;
    }
    public Map<String, Double> classifyInputFuzzy(Map<String, Double> inputTfIdf) {
        PriorityQueue<Pair<Map<String, Double>, String>> nearestNeighbors = getNearestNeighbors(inputTfIdf);
        Map<String, Double> membership = new HashMap<>();
        //full membership for the labelled instances
        double sumDistances = 0.0;
        for (Pair<Map<String, Double>, String> neigh : nearestNeighbors) {
            double dist = similarityMeasure == KNNClassifier.COSINE
                    ? SimilarityUtil.getCosineSim(inputTfIdf, neigh.first)
                    : SimilarityUtil.getEuclideanDist(inputTfIdf, neigh.first);
            dist = Math.pow(dist, 2.0 / (FUZZY_M - 1));
            String label = neigh.second;
            if (similarityMeasure == KNNClassifier.COSINE) {
                sumDistances += dist;
                membership.put(label, membership.getOrDefault(label, 0.0) + dist);
            } else {
                sumDistances += (1 / dist);
                membership.put(label, membership.getOrDefault(label, 0.0) + (1 / dist));
            }
        }
        for (String label : membership.keySet()) {
            membership.put(label, membership.get(label) / sumDistances);
        }
        return membership;
    }
}
