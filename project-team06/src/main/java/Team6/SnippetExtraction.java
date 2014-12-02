package Team6;

import java.util.List;
import java.util.Properties;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import edu.cmu.lti.oaqa.type.nlp.Parse;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.stanford.nlp.ling.CoreAnnotations.SectionAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class SnippetExtraction extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // Auto-generated method stub
    
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    
    FSIterator iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Document.type);
    while(iter.hasNext()) {
      Document doc = (Document) iter.next();
      String text = doc.getText(), docId = doc.getUri();
      if(text == null)
        continue;
      Annotation doc_annotated = new Annotation(text);
      pipeline.annotate(doc_annotated);
      List<CoreMap> sentences = doc_annotated.get(SentencesAnnotation.class);
      for(CoreMap sentence : sentences) {
        Passage p = new Passage(aJCas);
        int beginOffset = text.indexOf(sentence.toString());
        int endOffset = beginOffset + sentence.toString().length();
        if(beginOffset != -1) {
          p.setOffsetInBeginSection(beginOffset);
          p.setOffsetInEndSection(endOffset);
          p.setBeginSection("sections.0");
          p.setEndSection("sections.0");
          p.setUri(docId);
          p.setText(sentence.toString());
          //System.out.println(p.toString());
          p.addToIndexes();
        }
      }
    }
  }
}
