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
  import org.apache.uima.cas.FSIterator;
  import org.apache.uima.fit.component.JCasConsumer_ImplBase;
  import org.apache.uima.jcas.JCas;
  import org.apache.uima.resource.ResourceInitializationException;
  import org.apache.uima.resource.ResourceProcessException;
  import org.apache.uima.util.ProcessTrace;

  import edu.cmu.lti.oaqa.type.retrieval.Document;
  import edu.cmu.lti.oaqa.type.retrieval.SearchResult;
  import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;

  public class CorrectRankingAlgo extends JCasConsumer_ImplBase {
   
  ArrayList<String>  docList;
  ArrayList<Double> ScoreList;


  public void initialize(CAS aCas) throws ResourceInitializationException {
    
    docList = new ArrayList<String>();
    ScoreList = new ArrayList<Double>();
    
  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
   /*** 
    JCas jcas = null;
    try {
      jcas =aCas.getJCas();
    } catch (CASException e) 
    {
      e.printStackTrace();
    }
    **/
    
    FSIterator iter = jcas.getAnnotationIndex(Document.type).iterator();
    if (iter.isValid()) {
        iter.moveToNext();
        Document doc = (Document) iter.get(); 
      
      String query_string = doc.getQueryString();
      String Answer = doc.getText();
      double val = computeCosineSimilarity(createTermFreqVector(Answer),createTermFreqVector(query_string));
      doc.setScore(val);
      
        } 
    

  }
    
public void collectionProcessComplete(ProcessTrace arg0, JCas jcas)
          throws ResourceProcessException, IOException {
    double val;
    
    // Need to create a Answer List.
    FSIterator iter = jcas.getAnnotationIndex(Document.type).iterator();
    if (iter.isValid()) {
        iter.moveToNext();
        Document doc = (Document) iter.get(); 
      
      String query_string = doc.getQueryString();
      
    val = doc.getScore();
    docList.add(doc.getText());
    ScoreList.add(val);
    
        } 
    FSIterator iter2 = jcas.getAnnotationIndex(TripleSearchResult.type).iterator();
    if (iter2.isValid()) {
        iter2.moveToNext();
        TripleSearchResult doc = (TripleSearchResult) iter2.get(); 
      
      String svo = new String();
   
      svo = doc.getTriple().getSubject() + ' ' + doc.getTriple().getPredicate() + ' ' + doc.getTriple().getObject();
      docList.add(svo);
      ScoreList.add(doc.getScore());
      
        } 
    

    FSIterator iter3 = jcas.getAnnotationIndex(SearchResult.type).iterator();
    if (iter3.isValid()) {
        iter3.moveToNext();
        SearchResult doc = (SearchResult) iter3.get();  // SHOULD ACTUALLY BE CONCEPT SEARCH RESULT
      
      String query_string = doc.getQueryString();
      docList.add(doc.getUri());
      ScoreList.add(doc.getScore());
      
        }
    Integer[] ranks = new Integer[ScoreList.size()];
    Comparator<Integer> gc = new ScoreComparator(ScoreList);
    Arrays.sort(ranks, gc);
    
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

  
  

