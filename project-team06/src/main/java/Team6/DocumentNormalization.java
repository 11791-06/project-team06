package Team6;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import edu.cmu.lti.oaqa.type.retrieval.Document;

public class DocumentNormalization extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    FSIterator iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Document.type);
    while(iter.hasNext()) {
      Document doc = (Document) iter.next();
      
      String text = doc.getText();

      if(text == null)
        continue;
      
      text = Utils.normalization(text);
      doc.setText(text);
      
    }
  }
}
