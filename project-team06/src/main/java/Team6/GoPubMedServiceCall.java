package Team6;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.oaqa.bio.bioasq.services.GoPubMedService;
import edu.cmu.lti.oaqa.bio.bioasq.services.LinkedLifeDataServiceResponse;
import edu.cmu.lti.oaqa.bio.bioasq.services.OntologyServiceResponse;
import edu.cmu.lti.oaqa.bio.bioasq.services.OntologyServiceResponse.Concept;
import edu.cmu.lti.oaqa.bio.bioasq.services.PubMedSearchServiceResponse;
import edu.cmu.lti.oaqa.bio.bioasq.services.PubMedSearchServiceResponse.Document;
import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.kb.Triple;
import edu.cmu.lti.oaqa.type.retrieval.SearchResult;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;

public class GoPubMedServiceCall extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    GoPubMedService service = null;
    try {
      service = new GoPubMedService("project.properties");
    } catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    FSIterator<Annotation> iter = aJCas.getAnnotationIndex().iterator();
    try {
      if (iter.isValid()) {
        iter.moveToNext();
        Question question = (Question) iter.get();
        String query = question.getText();
        
        PubMedSearchServiceResponse.Result pubMedResult = service.findPubMedCitations(query, 0);
        for(Document d : pubMedResult.getDocuments()) {
          edu.cmu.lti.oaqa.type.retrieval.Document doc = new edu.cmu.lti.oaqa.type.retrieval.Document(aJCas);
          doc.setText(d.getDocumentAbstract());
          doc.setTitle(d.getTitle());
          doc.addToIndexes();
        }
        
        LinkedLifeDataServiceResponse.Result linkedLifeDataResult = service.findLinkedLifeDataEntitiesPaged(query, 0);
        for (LinkedLifeDataServiceResponse.Entity entity : linkedLifeDataResult.getEntities()) {
          for (LinkedLifeDataServiceResponse.Relation relation : entity.getRelations()) {
            Triple t = new Triple(aJCas);
            t.setObject(relation.getObj());
            t.setPredicate(relation.getPred());
            t.setSubject(relation.getSubj());
            TripleSearchResult tsr = new TripleSearchResult(aJCas);
            tsr.setTriple(t);
            tsr.setScore(entity.getScore());
            tsr.addToIndexes();
          }
        }
        
        OntologyServiceResponse.Result uniprotResult = service.findUniprotEntitiesPaged(query, 0);
        for (OntologyServiceResponse.Finding finding : uniprotResult.getFindings()) {
          SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result jochemResult = service.findJochemEntitiesPaged(query, 0);
        for (OntologyServiceResponse.Finding finding : jochemResult.getFindings()) {
          SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result goResult = service.findGeneOntologyEntitiesPaged(query,  0);
        for (OntologyServiceResponse.Finding finding : goResult.getFindings()) {
          SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result doResult = service.findDiseaseOntologyEntitiesPaged(query,  0);
        for (OntologyServiceResponse.Finding finding : doResult.getFindings()) {
          SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result meshResult = service.findMeshEntitiesPaged(query, 0);
        for (OntologyServiceResponse.Finding finding : goResult.getFindings()) {
          SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.addToIndexes();
        }
        
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}