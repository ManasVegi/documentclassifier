package org.textprocessors;

import java.util.*;

public class TextClassifier {
    public static void main(String[] args) {
        Map<String, String> labelledDocuments = DocumentUtil.readFromFolders("data.txt");
        Preprocessor preprocessor = new Preprocessor(labelledDocuments);
        preprocessor.removeStopWords();
        preprocessor.tokenizeAndCalculateTfIdf();
        Map<String, List<Map<String, Double>>> labelledTfIdf = new HashMap<>();
        for (String doc : preprocessor.getTfIdf().keySet()) {
            String label = doc.split("/")[1]; //assumption that the labels are the sub-directories within the data folder. will not work if not
            if (!labelledTfIdf.containsKey(label)) {
                labelledTfIdf.put(label, new ArrayList<>());
            }
            labelledTfIdf.get(label).add(preprocessor.getTfIdf().get(doc)); //losing file names here
        }

        //https://stats.stackexchange.com/questions/154660/tfidfvectorizer-should-it-be-used-on-train-only-or-traintest
        Map<String, String> unknownDocuments = new HashMap<>();
        DocumentUtil.walkFolder("unknown", unknownDocuments);
        Preprocessor unknownPreprocessor = new Preprocessor(unknownDocuments);
        //using the idf from the training set for better idf measure
        //cant preprocess unknown docs together with the labelled ones due to information leaking
        System.out.println(preprocessor.getDocumentFrequency());
        unknownPreprocessor.setDocumentFrequency(preprocessor.getDocumentFrequency());
        unknownPreprocessor.removeStopWords();
        unknownPreprocessor.tokenizeAndCalculateTfIdf();

        int K = 5; //HYPERPARAMETER

        System.out.println("Results from Crisp Knn Classifier:");
        KNNClassifier knnClassifierCrisp = new KNNClassifier(labelledTfIdf, K, KNNClassifier.COSINE, false);
        Map<String, String> classification = new HashMap<>();
        for (String doc : unknownPreprocessor.getTfIdf().keySet()) {
            System.out.print(doc + "'s votes: ");
            classification.put(doc, knnClassifierCrisp.classifyInputCrisp(unknownPreprocessor.getTfIdf().get(doc)));
        }
        System.out.println("Crisp classification: " + classification);
        //MEASURING PERFORMANCE
        //unknown09 doc has two articles, about airline safety and one about hoof and mouth. classifying as hoof&mouth as that is the longer article
        //unknown10 has both hoof and mouth and airline safety are almost equally present with airline safety having more lines
        HashMap<String, List<String>> labelToDocs = new HashMap<>();
        labelToDocs.put("C1", List.of("unknown/unknown01.txt","unknown/unknown02.txt","unknown/unknown03.txt","unknown/unknown04.txt","unknown/unknown10.txt"));
        labelToDocs.put("C4", List.of("unknown/unknown05.txt","unknown/unknown06.txt","unknown/unknown09.txt"));
        labelToDocs.put("C7", List.of("unknown/unknown07.txt","unknown/unknown08.txt"));
        int tp1 = 0, tp4 = 0, tp7 = 0;
        int fp1 = 0, fp4 = 0, fp7 = 0;
        int tn1 = 0, tn4 = 0, tn7 = 0;
        for (int i = 1; i <= 10; i++) {
            String doc = i >= 10 ? String.format("unknown/unknown%d.txt", i) : String.format("unknown/unknown0%d.txt", i);
            String label = classification.get(doc);
            if (labelToDocs.get(label).contains(doc)) {
                switch (label) {
                    case "C1" -> {
                        tp1++;
                        tn4++;
                        tn7++;
                    }
                    case "C4" -> {
                        tp4++;
                        tn1++;
                        tn7++;
                    }
                    case "C7" -> {
                        tp7++;
                        tn1++;
                        tn4++;
                    }
                }
            } else {
                switch (label) {
                    case "C1" -> fp1++;
                    case "C4" -> fp4++;
                    case "C7" -> fp7++;
                }
            }
        }
        System.out.println("Precision for C1: " + ((double) tp1 / (tp1 + fp1)) + ", Recall for C1: " + ((double) tp1 / labelToDocs.get("C1").size()) + ", Accuracy for C1: " + ((double) (tp1 + tn1) / classification.size()));
        System.out.println("Precision for C4: " + ((double) tp4 / (tp4 + fp4)) + ", Recall for C4: " + ((double) tp4 / labelToDocs.get("C4").size()) + ", Accuracy for C4: " + ((double) (tp4 + tn4) / classification.size()));
        System.out.println("Precision for C7: " + ((double) tp7 / (tp7 + fp7)) + ", Recall for C7: " + ((double) tp7 / labelToDocs.get("C7").size()) + ", Accuracy for C7: " + ((double) (tp7 + tn7) / classification.size()));
        System.out.println();


        boolean isFuzzy = true;
        KNNClassifier.FUZZY_M = 2; //HYPERPARAMETER
        System.out.println("Results from Fuzzy knn classifier:");
        KNNClassifier knnClassifier = new KNNClassifier(labelledTfIdf, K, KNNClassifier.COSINE, isFuzzy);
        for (String doc : unknownPreprocessor.getTfIdf().keySet()) {
            System.out.print(doc + "'s membership is: ");
            System.out.println(knnClassifier.classifyInputFuzzy(unknownPreprocessor.getTfIdf().get(doc)));
        }
    }
}
