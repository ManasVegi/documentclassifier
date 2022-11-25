package org.textprocessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        //SETTING HYPERPARAMETERS
        boolean isFuzzy = true;
        KNNClassifier.FUZZY_M = 2;
        int K = 5;

        KNNClassifier knnClassifier = new KNNClassifier(labelledTfIdf, K, KNNClassifier.COSINE, isFuzzy);
        if (isFuzzy) {
            for (String doc : unknownPreprocessor.getTfIdf().keySet()) {
                System.out.println(doc + "'s membership is");
                System.out.println(knnClassifier.classifyInputFuzzy(unknownPreprocessor.getTfIdf().get(doc)));
            }
        } else {
            Map<String, String> classification = new HashMap<>();
            for (String doc : unknownPreprocessor.getTfIdf().keySet()) {
                System.out.println(doc + "'s votes");
                classification.put(doc, knnClassifier.classifyInputCrisp(unknownPreprocessor.getTfIdf().get(doc)));
            }
            System.out.println(classification);
        }
    }
}
