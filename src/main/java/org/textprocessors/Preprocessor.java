package org.textprocessors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;

public class Preprocessor {
    public static final int NGRAMS_THRESHOLD = 4;
    private final List<String> stopWords = initializeStopWords();
    private Properties props = new Properties();
    Map<ComparableLabel, Integer> grams2, grams3;
    Map<String, Map<String, Double>> tfIdf;
    Map<String, String> documents;
    public Preprocessor(Map<String, String> documents) {
        this.documents = documents;
    }

    public void removeStopWords() {
        for (String document : documents.keySet()) {
            String content = documents.get(document);
            //try finding a way to reduce the complexity of this step
            for (String stopWord : stopWords) {
                content = content.replaceAll("\\b" + stopWord + "\\b", "");
            }
            content = content.replaceAll("\\p{P}", ""); //removing punctuations
            documents.put(document, content);
        }
    }
    private void createNGrams(Map<String, List<CoreLabel>> docuTokens) {
        Map<ComparableLabel, Integer> grams2 = new HashMap<>();
        Map<ComparableLabel, Integer> grams3 = new HashMap<>();
        //assuming length of strings is larger than 3
        for (String document : docuTokens.keySet()) {
            List<CoreLabel> tokens = docuTokens.get(document);
            ArrayDeque<CoreLabel> word2 = new ArrayDeque<>(List.of(tokens.get(0), tokens.get(1)));
            ArrayDeque<CoreLabel> word3 = new ArrayDeque<>(List.of(tokens.get(0), tokens.get(1), tokens.get(2)));
            for (int i = 2; i < tokens.size() - 1; i++) {
                String w2 = word2.stream().map(CoreLabel::word).collect(Collectors.joining(" "));
                String w3 = word3.stream().map(CoreLabel::word).collect(Collectors.joining(" "));
                CoreLabel cl2 = new CoreLabel(word2.peekFirst()), cl3 = new CoreLabel(word3.peekFirst());
                cl2.setWord(w2);
                cl2.setValue(w2);
                cl2.setNER("2_GRAM");
                cl3.setWord(w3);
                cl3.setValue(w3);
                cl3.setNER("3_GRAM");
                ComparableLabel comparableLabel2 = new ComparableLabel(cl2), comparableLabel3 = new ComparableLabel(cl3);
                grams2.put(comparableLabel2, grams2.getOrDefault(comparableLabel2, 0) + 1);
                grams3.put(comparableLabel3, grams3.getOrDefault(comparableLabel3, 0) + 1);
                word2.removeFirst();
                word2.addLast(tokens.get(i));
                word3.removeFirst();
                word3.addLast(tokens.get(i + 1));
            }
                    //ngrams = StringUtils.getNgramsString(documents.get(document), 2, 3).stream().toList();
        }
        Iterator<Map.Entry<ComparableLabel, Integer>> it2 = grams2.entrySet().iterator(), it3 = grams3.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<ComparableLabel, Integer> ent = it2.next();
            int freq = ent.getValue();
            if (freq < NGRAMS_THRESHOLD) {
                it2.remove();
            }
        }
        while (it3.hasNext()) {
            Map.Entry<ComparableLabel, Integer> ent = it3.next();
            int freq = ent.getValue();
            if (freq < NGRAMS_THRESHOLD) {
                it3.remove();
            }
        }
        this.grams2 = grams2;
        this.grams3 = grams3;
    }

    public void tokenizeAndCalculateTfIdf() {
        props.setProperty("annotators", "tokenize,pos,lemma,ner");
        props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
        StanfordCoreNLP tokenizingPipeline = new StanfordCoreNLP(props);
//        for (String document : documents.keySet()) {
//            ngrams = StringUtils.getNgramsString(documents.get(document), 2, 3).stream().toList();
//        }
        Map<String, List<CoreLabel>> docuTokens = new HashMap<>();
        Map<String, Set<ComparableLabel>> docuNerTokens = new HashMap<>();
        Map<String, List<CoreLabel>> allTokens = new HashMap<>();
        for (String documentName : documents.keySet()) {
            CoreDocument doc = tokenizingPipeline.processToCoreDocument(documents.get(documentName));
            // annotate
            //tokenizingPipeline.annotate(doc);
            // display tokens
            allTokens.put(documentName, doc.tokens());
            for (CoreLabel tok : doc.tokens()) {
                //System.out.println(String.format("%s\t%s\t%s", tok.word(), tok.lemma(), tok.ner()));

                if (tok.ner().equals("O")) {
                    List<CoreLabel> toks = docuTokens.getOrDefault(documentName, new ArrayList<>());
                    toks.add(tok);
                    docuTokens.put(documentName, toks);
                } else {
                    Set<ComparableLabel> toks = docuNerTokens.getOrDefault(documentName, new HashSet<>());
                    toks.add(new ComparableLabel(tok));
                    docuNerTokens.put(documentName, toks);
                }

            }
        }
        this.createNGrams(docuTokens);
        Map<String, Map<String, Double>> termFrequency = new HashMap<>();
        for (String docName : allTokens.keySet()) {
            Map<String, Double> docTF = new HashMap<>();
            for (int i = 0; i < allTokens.get(docName).size(); i++) {
                CoreLabel token = allTokens.get(docName).get(i);
                CoreLabel cl2gram = new CoreLabel(token), cl3gram = new CoreLabel(token);
                if (i < allTokens.get(docName).size() - 1)
                    cl2gram.setWord(cl2gram.word() + " " + allTokens.get(docName).get(i + 1).word());
                if (i < allTokens.get(docName).size() - 2)
                    cl3gram.setWord(cl2gram.word() + " " + allTokens.get(docName).get(i + 1).word() + " " + allTokens.get(docName).get(i + 2).word());
                if (!token.ner().equals("O")) {
                    docTF.put(token.word(), docTF.getOrDefault(token.word(), 0d) + 1);
                } else if (i < allTokens.get(docName).size() - 2 && grams3.containsKey(new ComparableLabel(cl3gram))) {
                    docTF.put(cl3gram.word(), docTF.getOrDefault(cl3gram.word(), 0d) + 1);
                    i += 2;
                } else if (i < allTokens.size() - 1 && grams2.containsKey(new ComparableLabel(cl2gram))) {
                    docTF.put(cl2gram.word(), docTF.getOrDefault(cl2gram.word(), 0d) + 1);
                    i += 1;
                } else {
                    docTF.put(token.lemma(), docTF.getOrDefault(token.lemma(), 0d) + 1);
                }
            }
            termFrequency.put(docName, docTF);
        }
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (String docName : termFrequency.keySet()) {
            for (String tok : termFrequency.get(docName).keySet()) {
                documentFrequency.put(tok, documentFrequency.getOrDefault(tok, 0) + 1);
            }
        }
        for (String docName : termFrequency.keySet()) {
            for (String tok : termFrequency.get(docName).keySet()) {
                double idf = Math.log10((double) documents.size() / documentFrequency.get(tok));
                termFrequency.get(docName).put(tok, termFrequency.get(docName).get(tok) * idf);
            }
        }
        tfIdf = termFrequency;
    }
    private List<String> initializeStopWords() {
        Path stopWordFile = Path.of("princeton_stopwords.txt");
        List<String> stopWords = new ArrayList<>();
        try {
            stopWords.addAll(Files.readAllLines(stopWordFile));
        } catch (IOException e) {
            System.err.println("Error while reading stopwords");
        }
        return stopWords;
    }

    public Map<String, Map<String, Double>> getTfIdf() {
        return tfIdf;
    }

    public Map<String, Map<String, Double>> generateFolderTopics() {
        Map<String, Map<String, Double>> folderTopics = new HashMap<>();
        for (String fileName : tfIdf.keySet()) {
            String[] path = fileName.split(File.separator);
            String folder = path[0] + File.separator + path[1];
            if (!folderTopics.containsKey(folder)) {
                folderTopics.put(folder, new HashMap<>(tfIdf.get(fileName)));
            } else {
                Map<String, Double> cumulativeTfIdf = folderTopics.get(folder);
                for (String keyword : tfIdf.get(fileName).keySet()) {
                    cumulativeTfIdf.put(keyword,
                            cumulativeTfIdf.getOrDefault(keyword, 0.0) + tfIdf.get(fileName).get(keyword));
                }
            }
        }
        return folderTopics;
    }
}
