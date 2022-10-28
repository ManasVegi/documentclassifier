package org.textprocessors;

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
    Map<String, String> documents;
    public Preprocessor(Map<String, String> documents) {
        this.documents = documents;
    }

    public Map<String, String> getDocuments() {
        return documents;
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
                System.out.println(cl2 + ", " + cl3);
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

    public void tokenize() {
        props.setProperty("annotators", "tokenize,pos,lemma,ner");
        props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
        StanfordCoreNLP tokenizingPipeline = new StanfordCoreNLP(props);
//        for (String document : documents.keySet()) {
//            ngrams = StringUtils.getNgramsString(documents.get(document), 2, 3).stream().toList();
//        }
        Map<String, List<CoreLabel>> docuTokens = new HashMap<>();
        Map<String, List<CoreLabel>> docuNerTokens = new HashMap<>();
        for (String documentName : documents.keySet()) {
            CoreDocument doc = tokenizingPipeline.processToCoreDocument(documents.get(documentName));
            // annotate
            //tokenizingPipeline.annotate(doc);
            // display tokens
            System.out.println("Tokens for file: " + documentName);
            System.out.println("Size: " + documents.get(documentName).length() + " #ner: " + doc.entityMentions().size());
            for (CoreLabel tok : doc.tokens()) {
                //System.out.println(String.format("%s\t%s\t%s", tok.word(), tok.lemma(), tok.ner()));

                if (tok.ner().equals("O")) {
                    List<CoreLabel> toks = docuTokens.getOrDefault(documentName, new ArrayList<>());
                    toks.add(tok);
                    docuTokens.put(documentName, toks);
                } else {
                    List<CoreLabel> toks = docuNerTokens.getOrDefault(documentName, new ArrayList<>());
                    toks.add(tok);
                    docuNerTokens.put(documentName, toks);
                }

            }
            //docuTokens.put(document, doc.tokens());
        }
        this.createNGrams(docuTokens);
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
}
