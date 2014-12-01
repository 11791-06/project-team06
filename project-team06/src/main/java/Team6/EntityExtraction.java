package Team6;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.type.answer.Answer;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class EntityExtraction extends JCasAnnotator_ImplBase {
  Chunker model;
  
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    // TODO Auto-generated method stub
    try {
      File f = new File(this.getClass().getClassLoader().getResource((String)aContext.getConfigParameterValue("ModelName")).getFile());
      model = (Chunker) AbstractExternalizable.readObject(f);
    } catch(Exception e) {
      throw new UIMARuntimeException(e);
    }
  }
  
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    
    Properties props = new Properties();
    props.put("annotators", "tokenize");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    HashMap<String, Double> entityScore = new HashMap<String, Double>();
    
    int ranklimit = 100; 
    FSIterator iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Document.type);
    
    HashMap<String, HashSet<String>> entities_df = new HashMap<String, HashSet<String>>();
    int N = 0;
    while(iter.hasNext()) {
      Document doc = (Document) iter.next();
      if(doc.getRank() <= ranklimit) {
        N++;
        String text = doc.getText();
        if(text == null)
          continue;
        Annotation doc_annotated = new Annotation(text);
        pipeline.annotate(doc_annotated);
        List<CoreLabel> tokens = doc_annotated.get(TokensAnnotation.class);
        for(CoreLabel token : tokens) {
          String entityName = token.word().toLowerCase();
          if(entityName.length() < 3)
            continue;
          if(entities_df.containsKey(entityName)) {
            entities_df.get(entityName).add(doc.getUri());
          }
          else {
            HashSet<String> temp = new HashSet<String>();
            temp.add(doc.getUri());
            entities_df.put(entityName, temp);
          }
        }
      }
    }
    
    iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Document.type);
    while(iter.hasNext()) {
      HashMap<String, Integer> entities = new HashMap<String, Integer>();
      Document doc = (Document) iter.next();
      int max_f = 0;
      if(doc.getRank() <= ranklimit) {
        String text = doc.getText();
        if(text == null)
          continue;
        /*Set<Chunk> namedEntities = model.chunk(text).chunkSet();
        for(Chunk c : namedEntities) {
          String entityName = text.substring(c.start(), c.end());
          if(entities.containsKey(entityName)) {
            entities.put(entityName,  entities.get(entityName) + 1);
          }
          else {
            entities.put(entityName,  1);
          }
        }*/
        
        Annotation doc_annotated = new Annotation(text);
        pipeline.annotate(doc_annotated);
        List<CoreLabel> tokens = doc_annotated.get(TokensAnnotation.class);
        for(CoreLabel token : tokens) {
          String entityName = token.word().toLowerCase();
          if(entityName.length() < 3)
            continue;
          if(entities.containsKey(entityName)) {
            entities.put(entityName,  entities.get(entityName) + 1);
            max_f = Math.max(max_f,  entities.get(entityName));
            entities_df.get(entityName).add(doc.getUri());
          }
          else {
            entities.put(entityName,  1);
            max_f = Math.max(max_f,  entities.get(entityName));
            HashSet<String> temp = new HashSet<String>();
            temp.add(doc.getUri());
            entities_df.put(entityName, temp);
          }
        }
      }
      for(String entityName : entities.keySet()) {
        double tf = 0.5 + (0.5 * entities.get(entityName)) /((double) max_f);
        double idf = Math.log(N/Math.max(1.0,  entities_df.get(entityName).size()));
        double score = tf * idf;
        if(entityScore.containsKey(entityName))
          entityScore.put(entityName, entityScore.get(entityName) + score);
        else
          entityScore.put(entityName, score);
      }
    }
    List<String> entityNames = new ArrayList<String>();
    //System.out.println(entities.keySet());
    entityNames.addAll(entityScore.keySet());
    Collections.sort(entityNames,  new Comparator<String>() {
      public int compare(String o1, String o2) {
        return entityScore.get(o1).compareTo(entityScore.get(o2));
      }
    });
    
    Collections.reverse(entityNames);
    
    int rank = 0;
    for(String entityName : entityNames) {
      Answer ans = new Answer(aJCas);
      ans.setRank(++rank);
      ans.setText(entityName);
      //System.out.println("Entity : " + entityName + " rank : " + rank + " freq : " + entityScore.get(entityName));
      ans.addToIndexes();
    }
  }
}
