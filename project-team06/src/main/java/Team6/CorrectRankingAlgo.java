package Team6;


 

  import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

  import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
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
    
   
  ArrayList<String>  docList;
  ArrayList<Double> ScoreList;
  ArrayList<String>  QuestionList;

@Override
  public void initialize() throws ResourceInitializationException {
    
    docList = new ArrayList<String>();
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
    if (docList == null){
      System.out.println("DOCLIST IS EMPTY HERE");
    }
    while (iter.hasNext()) {
         Document doc = (Document) iter.next(); 
       
       
       Answer = doc.getText();
   
       double val = 0.0;
       if (Answer == null){
        
       }
       else
       {
       
      // System.out.println("Now writing answer");
       //System.out.println(Answer);
       Map<String, Integer> q_vector = createTermFreqVector(query_string);
       Map<String, Integer> a_vector = createTermFreqVector(Answer);
       val = computeCosineSimilarity(a_vector,q_vector);
       doc.setScore(val);
       docList.add(Answer);
       }
   /***UPLOAD SCORE TO CAS ***/
      
    }
    }
     
   

  
  public void GetTripleScores(JCas jcas)
  {
    FSIterator iter2 = jcas.getJFSIndexRepository().getAllIndexedFS(TripleSearchResult.type);

    while (iter2.hasNext()) {
      TripleSearchResult doc = (TripleSearchResult) iter2.next(); 
       
       String query_string = doc.getQueryString();
       String Answer = doc.getText();
       
       if (query_string != null && Answer != null) 
       {
         
         String svo = new String();
         svo = doc.getTriple().getSubject() + ' ' + doc.getTriple().getPredicate() + ' ' + doc.getTriple().getObject();
         docList.add(svo);
         ScoreList.add(doc.getScore());
       
        
       
         } 
      
    }
     
   

  }
  public void GetConceptScores(JCas jcas)
  {
  FSIterator iter3 = jcas.getJFSIndexRepository().getAllIndexedFS(SearchResult.type);
  
  // FSIterator iter3 = jcas.getAnnotationIndex(SearchResult.type).iterator();
   while (iter3.hasNext()) {

       SearchResult doc = (SearchResult) iter3.next();  // SHOULD ACTUALLY BE CONCEPT SEARCH RESULT
       String query_string = doc.getQueryString();
       String Answer = doc.getText();
      // System.out.println(query_string);
       if (query_string != null && Answer != null) 
       {
     
     docList.add(doc.getUri());
     ScoreList.add(doc.getScore());

     
       }
   }
  }
  
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas =aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
 
    String query_string= "rheumatoid arthritis common man woman";
    System.out.println("Into Process");
    System.out.println("Get ALL Questions");
   // query_string = GetAllQuestions(jcas);
    System.out.println("Get ALL Documents");
    GetDocumentScores(jcas,query_string);
    System.out.println("Got All Documents");
    GetTripleScores(jcas);
    System.out.println("Got All Triples");
    GetConceptScores(jcas);
    System.out.println("Got All Concepts");
    System.out.println(QuestionList);
/** 
 GetDocumentScores(jcas);
  System.out.println("Got One Documents");
  
  //System.out.println(docList.size());
  GetTripleScores(jcas);
  System.out.println("Got One Triples");
  
//  System.out.println(docList.size());
  GetConceptScores(jcas);
  System.out.println("Got One Concepts");
  
  //System.out.println(docList.size());
  
  //**/
    

  }
    
public void collectionProcessComplete(ProcessTrace arg0)
          throws ResourceProcessException, IOException {
  System.out.println("Into Collection process Complete");
    double val;
    
    
    /**
    GetDocumentScores(jcas);
    System.out.println("Got All Documents");
    GetTripleScores(jcas);
    System.out.println("Got All Triples");
    GetConceptScores(jcas);
    System.out.println("Got All Concepts");
    // Need to create a Answer List.
    **/
    
    
   
    Integer[] ranks = new Integer[ScoreList.size()];
    Comparator<Integer> gc = new ScoreComparator(ScoreList);
    Arrays.sort(ranks, gc);
    System.out.println("######################################");
   // System.out.println(ranks);
    System.out.println("######################################"); 
    //http://stackoverflow.com/questions/14186529/java-array-of-sorted-indexes
   
    
    
  }
  public class ScoreComparator implements Comparator<Integer> {
    private ArrayList<Double> scores;
    public ScoreComparator(ArrayList<Double> scoreList) {
        scores = scoreList;
    }
    public int compare(Integer i, Integer j) {
        return Double.compare(scores.get(j), scores.get(i));
    }
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

  
  

