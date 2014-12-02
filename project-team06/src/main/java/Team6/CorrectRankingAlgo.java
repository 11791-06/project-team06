package Team6;

import util.TypeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.kb.Concept;
import edu.cmu.lti.oaqa.type.nlp.Parse;
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.cmu.lti.oaqa.type.retrieval.SearchResult;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;

public class CorrectRankingAlgo extends CasConsumer_ImplBase {

    @Override
    public void initialize() throws ResourceInitializationException {

        System.out.println("Intialized all Lists");

    }

    public String GetAllTokens(JCas jcas) {
        String query = null;
        // FSIterator<Parse> iter = jcas.getAnnotationIndex().iterator();
        FSIterator<TOP> iter = jcas.getJFSIndexRepository().getAllIndexedFS(Parse.type);
        if (iter.isValid()) {
            Question question = (Question) iter.get();

            query = question.getText();
            System.out.println("Query = " + query);
            // QuestionList.add(query);
            return query;
        } else {
            return query;
        }
    }

    public String GetAllQuestions(JCas jcas) {
        String query = null;
        FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();

        if (iter.isValid()) {
            Question question = (Question) iter.get();
            

            //query = question.getText();
              query = question.getRankText();
            System.out.println("Query = " + query);
            // QuestionList.add(query);
            return query;
        } else {
            return query;
        }
    }
    public int GetSnippetScores(JCas jcas, String query_string) {
      int passId = 0;
      FSIterator iter = jcas.getJFSIndexRepository().getAllIndexedFS(Passage.type);
      String Answer = null;
      while (iter.hasNext()) {

          Passage doc = (Passage) iter.next();
         
          //System.out.println(doc);
          
          // if (doc.getSearchId() == "__gold__")
          if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
             doc.setScore(-1.0);
              continue;
          }
          passId += 1;

          Answer = doc.getText();
          
//          System.out.println("Anser = " + Answer);
          doc.setDocId(Integer.toString(passId));
          double val = 0.0;
          if (Answer == null) {
           

          } else {
              Map<String, Integer> q_vector = createTermFreqVector(query_string);
              Map<String, Integer> a_vector = createTermFreqVector(Answer);
              val = computeCosineSimilarity(a_vector, q_vector);

          }

          System.out.println("Score 1:");
          
          System.out.println(query_string);
          System.out.println(Answer);
          System.out.println(val);
          System.out.println("Score 2:");
          

          doc.setScore(val);

          // docList.add(docId);
          // ScoreList.add(val);
          // doc.addToIndexes();

      }
      return passId;
  }
    public int GetDocumentScores(JCas jcas, String query_string) {
        int docId = 0;
        FSIterator iter = jcas.getJFSIndexRepository().getAllIndexedFS(Document.type);
        String Answer = null;
        while (iter.hasNext()) {

            Document doc = (Document) iter.next();
            // if (doc.getSearchId() == "__gold__")
            if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
                continue;
            }
            docId += 1;

            Answer = doc.getText();
            
//            System.out.println("Anser = " + Answer);
            doc.setDocId(Integer.toString(docId));
            double val = 0.0;
            if (Answer == null) {

            } else {
                Map<String, Integer> q_vector = createTermFreqVector(query_string);
                Map<String, Integer> a_vector = createTermFreqVector(Answer);
                val = computeCosineSimilarity(a_vector, q_vector);

            }
            doc.setScore(val);

            // docList.add(docId);
            // ScoreList.add(val);
            // doc.addToIndexes();

        }
        return docId;
    }

    /**
     * @author Diyi
     * @param jcas
     * @return the number of triple search results
     */
    public int GetTripleSize(JCas jcas) {
        FSIterator<?> iter = jcas.getJFSIndexRepository().getAllIndexedFS(TripleSearchResult.type);
        int ret = 0;
        while (iter.hasNext()) {
            iter.next();
            ++ret;
        }
        return ret;
    }

    /**
     * @author Diyi
     * @param jcas
     * @return the number of concept search results
     */
    public int GetConceptSize(JCas jcas) {
        FSIterator<?> iter = jcas.getJFSIndexRepository().getAllIndexedFS(ConceptSearchResult.type);
        int ret = 0;
        while (iter.hasNext()) {
            iter.next();
            ++ret;
        }
        return ret;
    }

    public void GetTripleScores(JCas jcas, String query_string) {
        FSIterator iter2 = jcas.getJFSIndexRepository().getAllIndexedFS(TripleSearchResult.type);

        while (iter2.hasNext()) {
            TripleSearchResult doc = (TripleSearchResult) iter2.next();
            String Answer = doc.getText();

            // Perhaps needs some kind of adjustment to account for Answer being null.
            String svo = new String();
            svo = doc.getTriple().getSubject() + ' ' + doc.getTriple().getPredicate() + ' '
                            + doc.getTriple().getObject();
            double val = 0.0;

            //System.out.println("Current SVO triple");

            //System.out.println(svo);

            Map<String, Integer> q_vector = createTermFreqVector(query_string);
            Map<String, Integer> a_vector = createTermFreqVector(svo);
            val = computeCosineSimilarity(a_vector, q_vector);
            doc.setScore(val);
            // docList.add(doc.getUri());
            // ScoreList.add(doc.getScore());

        }

    }

    public void GetConceptScores(JCas jcas) {
        FSIterator iter3 = jcas.getJFSIndexRepository().getAllIndexedFS(ConceptSearchResult.type);

        while (iter3.hasNext()) {
            ConceptSearchResult doc = (ConceptSearchResult) iter3.next(); // SHOULD ACTUALLY BE
                                                                          // CONCEPT SEARCH RESULT
            String Answer = doc.getText();
            // Perhaps needs try and catch or some kind of adjustment of Answer being null.
            // docList.add(doc.getUri());
            // ScoreList.add(doc.getScore());

        }
    }

    public void processCas(CAS aCas) throws ResourceProcessException {

        /**
         * ArrayList<Integer> docList = new ArrayList<Integer>(); ArrayList<Double> ScoreList = new
         * ArrayList<Double>(); ArrayList<String> QuestionList= new ArrayList<String>();
         */
        System.out.println("Now to get CAS");
        JCas jcas;
        try {
            jcas = aCas.getJCas();
        } catch (CASException e) {
            throw new ResourceProcessException(e);
        }

        String query_string = GetAllQuestions(jcas); // Only caters to one question at a time.
        System.out.println("Now to get Documents");
        int docId = GetDocumentScores(jcas, query_string);
        int passId = GetSnippetScores(jcas, query_string);
        // GetTripleScores(jcas,query_string);
        System.out.println("Number of Documents Returned");
 
        /**
         * @author Diyi
         * @comment we should remove the groudtruth when sorting and also need to use the correct size, instead of giving docId to concepts, and triples 
         * */
        List<Document> docResults = new ArrayList<Document>();
        for (Document doc : JCasUtil.select(jcas, Document.class)) {
            if (doc.getSearchId() == null || !doc.getSearchId().equals("__gold__")) {
                docResults.add(doc);
            }
        }
        docResults = util.TypeUtil.rankedSearchResultsByScore(JCasUtil.select(jcas, Document.class), docResults.size());
        
        List<Passage> passResults = new ArrayList<Passage>();
        for (Passage pass : JCasUtil.select(jcas, Passage.class)) {
            if (pass.getSearchId() == null || !pass.getSearchId().equals("__gold__")) {
                passResults.add(pass);
            
            }
        }
        passResults = util.TypeUtil.rankedSearchResultsByScore(JCasUtil.select(jcas, Passage.class), passResults.size());
      //System.out.println(passResults);
        List<ConceptSearchResult> conceptResults = new ArrayList<ConceptSearchResult>();
        for (ConceptSearchResult doc : JCasUtil.select(jcas, ConceptSearchResult.class)) {
            if (doc.getSearchId() == null || !doc.getSearchId().equals("__gold__")) {
                conceptResults.add(doc);
            }
        }
        conceptResults = util.TypeUtil.rankedSearchResultsByScore(JCasUtil.select(jcas, ConceptSearchResult.class), conceptResults.size());
        
        List<TripleSearchResult> triResults = new ArrayList<TripleSearchResult>();
        for (TripleSearchResult doc : JCasUtil.select(jcas, TripleSearchResult.class)) {
            if (doc.getSearchId() == null || !doc.getSearchId().equals("__gold__")) {
                triResults.add(doc);
            }
        }
        triResults = util.TypeUtil.rankedSearchResultsByScore(JCasUtil.select(jcas, TripleSearchResult.class), triResults.size());
       
        
        int i = 0;

        for (Document docr : docResults) {
            docr.addToIndexes(jcas);

        }
        for (Passage pass : passResults) {
          pass.addToIndexes(jcas);

      }
        
        for (ConceptSearchResult conr : conceptResults) {
            conr.addToIndexes(jcas);

        }
        for (TripleSearchResult tripr : triResults) {
            /*if (tripr.getRank() > 0) {
            
                System.out.println("Triples");
                System.out.println(tripr.getScore());
                System.out.println(tripr.getRank());
            }*/
            tripr.addToIndexes(jcas);

        }

        // NOT DOING TRIPLES AND CONCEPTS RIGHT NOW, NO DOCUMENT ID's added for them
        // GetTripleScores(jcas);
        // GetConceptScores(jcas);

    }

    public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
                    IOException {
        // super.collectionProcessComplete(arg0);

        // System.out.println("Into Collection process Complete");

    }

    List<String> MyTokenizer(String doc) {
        List<String> res = new ArrayList<String>();

        for (String s : doc.split("[\\p{Punct}\\s]+"))
            res.add(s.toLowerCase());
        return res;
    }
    private double computeJaccardSimilarity(Map<String, Integer> queryVector,
            Map<String, Integer> docVector) {
          double jaccard_similarity=0.0;
      
         
          Set union = new HashSet<String>();
          Set inter = new HashSet<String>(docVector.keySet());
          
         // Map<String, Double> queryVector_L1 = L1_normalize(queryVector);
          //Map<String, Double> docVector_L1 = L1_normalize(docVector);
         // Map<String, Double> queryVector_L2 = L2_normalize(queryVector_L1);
         // Map<String, Double> docVector_L2 = L2_normalize(docVector_L1);
          union.addAll(docVector.keySet());
          union.addAll(queryVector.keySet());
          inter.retainAll(queryVector.keySet());
          int SizeOfUnion = union.size();
          int SizeOfInter = inter.size();
        /*  System.out.println("Union");
   
          System.out.println(union);
          System.out.println(SizeOfUnion);
          System.out.println("Intersection");
          System.out.println(SizeOfInter);
          System.out.println(inter);*/
          double denom = (double) SizeOfUnion;
          double numer = (double) SizeOfInter;
         jaccard_similarity =   (numer)/(denom);
         /*
         System.out.println("Similarity");
         System.out.println(jaccard_similarity);
         System.out.println(numer);
         System.out.println(denom);*/
          return jaccard_similarity;
        }
    
    private double computeDiceSimilarity(Map<String, Integer> queryVector,
            Map<String, Integer> docVector) {
          double dice_similarity=0.0;
      
         
          Set union = new HashSet<String>();
          Set inter = new HashSet<String>(docVector.keySet());
          
         // Map<String, Double> queryVector_L1 = L1_normalize(queryVector);
          //Map<String, Double> docVector_L1 = L1_normalize(docVector);
         // Map<String, Double> queryVector_L2 = L2_normalize(queryVector_L1);
         // Map<String, Double> docVector_L2 = L2_normalize(docVector_L1);
        
          int SizeOfUnion = queryVector.size() + docVector.size();
          int SizeOfInter = inter.size();
          /*
          System.out.println("Union");
   
        
          System.out.println(SizeOfUnion);
          System.out.println("Intersection");
          System.out.println(SizeOfInter);
          System.out.println(inter);*/
          double denom = (double) SizeOfUnion;
          double numer = (double) SizeOfInter;
         dice_similarity =   (2*numer)/(denom);
         /*System.out.println("Similarity");
         System.out.println(dice_similarity);
         System.out.println(numer);
         System.out.println(denom);*/
          return dice_similarity;
        }


    private double computeCosineSimilarity(Map<String, Integer> queryVector,
                    Map<String, Integer> docVector) {
        double cosine_similarity = 0.0;
        double qf = 0.0;
        double mag_query = 0.0;
        double mag_doc = 0.0;

        Map<String, Double> queryVector_L1 = L1_normalize(queryVector);
        Map<String, Double> docVector_L1 = L1_normalize(docVector);
        Map<String, Double> queryVector_L2 = L2_normalize(queryVector_L1);
        Map<String, Double> docVector_L2 = L2_normalize(docVector_L1);

        for (String s : queryVector_L2.keySet()) {
            qf = queryVector_L2.get(s);
            if (docVector.containsKey(s)) {
                Double df = docVector_L2.get(s);
                cosine_similarity = cosine_similarity + df * qf;
            }
        }

        return cosine_similarity;
    }

    private Map<String, Double> L2_normalize(Map<String, Double> TERMVector) {
        double total = 0.0;
        double freq = 0;
        Map<String, Double> termVector = new HashMap<String, Double>();
        for (String s : TERMVector.keySet()) {
            total += TERMVector.get(s) * TERMVector.get(s);
        }
        total = Math.sqrt(total);
        for (String s : TERMVector.keySet()) {
            freq = TERMVector.get(s);
            termVector.put(s, freq / total);
        }
        return termVector;
    }

    /**
     * Implements L1 Normalization on the given vector.
     * 
     * @param TERMVector
     * @return termVector
     */
    private Map<String, Double> L1_normalize(Map<String, Integer> queryVector) {
        double total = 0.0;
        int freq = 0;
        Map<String, Double> termVector = new HashMap<String, Double>();
        for (String s : queryVector.keySet()) {
            total += queryVector.get(s);
        }
        for (String s : queryVector.keySet()) {
            freq = queryVector.get(s);
            termVector.put(s, freq / total);
        }
        return termVector;
    }

    private Map<String, Integer> createTermFreqVector(String doctext) {

        List<String> tokenized = MyTokenizer(doctext);

        Map<String, Integer> tokens = new HashMap<String, Integer>();
        for (String s : tokenized) {

            if (!tokens.containsKey(s)) {
                tokens.put(s, (int) 1.0);
            } else {
                Integer freq = tokens.get(s);
                Integer new_freq = freq + 1;
                tokens.put(s, new_freq);

            }
        }
        return tokens;
    }
}
