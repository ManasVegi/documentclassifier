package org.textprocessors;

import java.util.Map;
import java.util.Set;

public class SimilarityUtil {
    public static double getEuclideanDist(Map<String, Double> vec1, Map<String, Double> vec2) {
        Set<String> keys1 = vec1.keySet(), keys2 = vec2.keySet();
        Double dist = 0.0;
        for (String key : keys1) {
            if (!keys2.contains(key)) {
                dist += Math.pow(vec1.get(key), 2);
            } else {
                dist += Math.pow(vec1.get(key) - vec2.get(key), 2);
            }
        }
        for (String key : keys2) {
            if (!keys1.contains(key)) {
                dist += Math.pow(vec2.get(key), 2);
            }
        }
        return Math.sqrt(dist);
    }
    private static double getMagnitude(Map<String, Double> vec) {
        double mag = 0.0;
        for (String key : vec.keySet()) {
            mag += Math.pow(vec.get(key), 2);
        }
        return Math.sqrt(mag);
    }
    public static double getCosineSim(Map<String, Double> vec1, Map<String, Double> vec2) {
        Set<String> keys1 = vec1.keySet(), keys2 = vec2.keySet();
        double dist = 0.0;
        for (String key : keys1) {
            if (keys2.contains(key)) {
                dist += vec1.get(key) * vec2.get(key);
            }
        }
        return dist / (getMagnitude(vec1) * getMagnitude(vec2));
    }

    //vec2 is added to vec1
    public static void addVec(Map<String, Double> vec1, Map<String, Double> vec2) {
        for (String key : vec2.keySet()) {
            vec1.put(key, vec1.getOrDefault(key, 0.0) + vec2.get(key));
        }
    }
}
