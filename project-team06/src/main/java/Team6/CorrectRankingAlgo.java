package Team6;


 
import util.TypeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

  import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.SearchResult;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;

  public class CorrectRankingAlgo extends CasConsumer_ImplBase {
    
   
  ArrayList<Integer>  docList;
  ArrayList<Double> ScoreList;
  ArrayList<String>  QuestionList;
  int docId;

@Override
  public void initialize() throws ResourceInitializationException {
    docId = 0;
    docList = new ArrayList<Integer>();
    ScoreList = new ArrayList<Double>();
    QuestionList= new ArrayList<String>();
    
    System.out.println("Intialized all Lists");
    
  }
  
  public String GetAllQuestions(JCas jcas)
  {
    String query = null;
    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
  
      if (iter.isValid()) {
        Question question = (Question) iter.get();
        query = question.getText();
        System.out.println("Query = " + query);
        QuestionList.add(query);
        return query;
      }
      else{
        return query;
      }
}
      
  public void GetDocumentScores(JCas jcas, String query_string)
  {
    
    FSIterator iter = jcas.getJFSIndexRepository().getAllIndexedFS(Document.type);
    String Answer=null;
    while (iter.hasNext()) {
      
      Document doc = (Document) iter.next(); 
     if (doc.getSearchId() == "__gold__")
     {
       continue;
     }
     docId += 1;
     
     Answer = doc.getText();
     
     doc.setDocId(Integer.toString(docId));
     double val = 0.0;
       if (Answer == null){
        
       }
       else
       {
       Map<String, Integer> q_vector = createTermFreqVector(query_string);
       Map<String, Integer> a_vector = createTermFreqVector(Answer);
       val = computeCosineSimilarity(a_vector,q_vector);

       }
       doc.setScore(val);
       
       docList.add(docId);
       ScoreList.add(val);
      // doc.addToIndexes();
      
    }
    }
     
   

  
  public void GetTripleScores(JCas jcas)
  {
    FSIterator iter2 = jcas.getJFSIndexRepository().getAllIndexedFS(TripleSearchResult.type);

    while (iter2.hasNext()) {
      TripleSearchResult doc = (TripleSearchResult) iter2.next(); 
       String Answer = doc.getText();
     
         //Perhaps needs some kind of adjustment to account for Answer being null.
         String svo = new String();
         svo = doc.getTriple().getSubject() + ' ' + doc.getTriple().getPredicate() + ' ' + doc.getTriple().getObject();
         //docList.add(doc.getUri());
         ScoreList.add(doc.getScore());
         
  }
     
   

  }
  public void GetConceptScores(JCas jcas)
  {
  FSIterator iter3 = jcas.getJFSIndexRepository().getAllIndexedFS(SearchResult.type);
  
   while (iter3.hasNext()) {
      SearchResult doc = (SearchResult) iter3.next();  // SHOULD ACTUALLY BE CONCEPT SEARCH RESULT
      String Answer = doc.getText();
      //Perhaps needs try and catch or some kind of adjustment of Answer being null.
      // docList.add(doc.getUri());
       ScoreList.add(doc.getScore());
      
}
  }
  
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas =aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
 
    String query_string = GetAllQuestions(jcas); //Only caters to one question at a time.
    GetDocumentScores(jcas,query_string);
    System.out.println("Number of Documents Returned");
   System.out.println(docList.size());
   List<Document> DocResults = util.TypeUtil.rankedSearchResultsByScore(JCasUtil.select(jcas, Document.class),docList.size());
  int i=0;
    for(Document docr : DocResults)
    {
      docr.addToIndexes(jcas);
      
    }
    
 
  
    //NOT DOING TRIPLES AND CONCEPTS RIGHT NOW, NO DOCUMENT ID's added for them
    //GetTripleScores(jcas);
   // GetConceptScores(jcas);

   
  } 
   



  
public void collectionProcessComplete(ProcessTrace arg0)
          throws ResourceProcessException, IOException {
 // super.collectionProcessComplete(arg0);
  

 // System.out.println("Into Collection process Complete");





  
  }
    
      
      

      
      
      
      List<String> MyTokenizer(String doc) {
        List<String> res = new ArrayList<String>();
        
        for (String s: doc.split("[\\p{Punct}\\s]+"))
          res.add(s.toLowerCase());
        return res;
      }
    
    
    private double computeCosineSimilarity(Map<String, Integer> queryVector,
            Map<String, Integer> docVector) {
          double cosine_similarity=0.0;
          double qf = 0.0;
          double mag_query = 0.0;
          double mag_doc = 0.0;
          
          Map<String, Double> queryVector_L1 = L1_normalize(queryVector);
          Map<String, Double> docVector_L1 = L1_normalize(docVector);
          Map<String, Double> queryVector_L2 = L2_normalize(queryVector_L1);
          Map<String, Double> docVector_L2 = L2_normalize(docVector_L1);
          
          for(String s: queryVector_L2.keySet())
          {
            qf = queryVector_L2.get(s);
            if(docVector.containsKey(s))
            {
              Double df =  docVector_L2.get(s);
              cosine_similarity  = cosine_similarity + df*qf;
            }
          }
          
         
          return cosine_similarity;
        }
    private Map<String, Double> L2_normalize(Map<String, Double> TERMVector)
    {
     double total = 0.0;
     double freq = 0;
     Map<String, Double> termVector = new HashMap<String, Double>();
     for(String s: TERMVector.keySet())
     {
       total += TERMVector.get(s)*TERMVector.get(s);
     }
     total = Math.sqrt(total);
     for(String s: TERMVector.keySet())
     { 
       freq = TERMVector.get(s);
       termVector.put(s, freq/total);
     }
     return termVector;
    }
  /**
   * Implements L1 Normalization on the given vector.
   * @param TERMVector
   * @return termVector
   */
  private Map<String, Double> L1_normalize(Map<String, Integer> queryVector)
  {
   double total = 0.0;
   int freq = 0;
   Map<String, Double> termVector = new HashMap<String, Double>();
   for(String s: queryVector.keySet())
   {
     total += queryVector.get(s);
   }
   for(String s: queryVector.keySet())
   { 
     freq = queryVector.get(s);
     termVector.put(s, freq/total);
   }
   return termVector;
  }

  private Map<String, Integer> createTermFreqVector(String doctext) {


    List<String> tokenized = MyTokenizer(doctext);

    Map<String, Integer> tokens = new HashMap<String, Integer>();
    for(String s: tokenized)
    {
      
      if(!tokens.containsKey(s)){
        tokens.put(s, (int) 1.0);
      }
      else{
        Integer freq = tokens.get(s);
        Integer new_freq =  freq + 1;
        tokens.put(s, new_freq);
                
      }
    
      
    }
    return tokens;
  }



    }

  
  

