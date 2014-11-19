package Team6;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.SearchResult;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;

public class GoPubMedServiceCall extends JCasAnnotator_ImplBase {

  private void getDocText(edu.cmu.lti.oaqa.type.retrieval.Document doc, String pmid) {
    try {
      URL url = new URL("http://metal.lti.cs.cmu.edu:30002/pmc/" + pmid);
      URLConnection conn = url.openConnection();
      BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line = null, text = "";
      while((line = br.readLine()) != null) {
        text += line;
      }
      doc.setText(text);
      //System.out.println(text);
    } catch(MalformedURLException e) {
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
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
    String query = null;
    FSIterator<Annotation> iter = aJCas.getAnnotationIndex().iterator();
    try {
      if (iter.isValid()) {
        Question question = (Question) iter.get();
        query = question.getText();
        System.out.println("Query = " + query);
        PubMedSearchServiceResponse.Result pubMedResult = service.findPubMedCitations(query, 0);
        System.out.println("# of retrieved documents = " + pubMedResult.getDocuments().size());
        for(Document d : pubMedResult.getDocuments()) {
          edu.cmu.lti.oaqa.type.retrieval.Document doc = new edu.cmu.lti.oaqa.type.retrieval.Document(aJCas);
          doc.setText(d.getDocumentAbstract());
          doc.setTitle(d.getTitle());
          if(d.isFulltextAvailable()==true) {
            String pmid = d.getPmid();
            getDocText(doc, pmid);
            //query here and add snippets once the service is online 
          }
          //System.out.println("Document full text : " + (d.isFulltextAvailable()));
          
          // need to set this - Diyi
          doc.setUri("http://www.ncbi.nlm.nih.gov/pubmed/" + d.getPmid());
          
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
            tsr.setQueryString(query);
            tsr.setScore(entity.getScore());
            tsr.addToIndexes();
            
          }
        }
        
        OntologyServiceResponse.Result uniprotResult = service.findUniprotEntitiesPaged(query, 0);
        for (OntologyServiceResponse.Finding finding : uniprotResult.getFindings()) {
          // change it to conceptSearchResult
          // not so sure about using searchresult
          ConceptSearchResult c = new ConceptSearchResult(aJCas);
          //SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.setQueryString(query);
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result jochemResult = service.findJochemEntitiesPaged(query, 0);
        for (OntologyServiceResponse.Finding finding : jochemResult.getFindings()) {
          ConceptSearchResult c = new ConceptSearchResult(aJCas);
          //SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.setQueryString(query);
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result goResult = service.findGeneOntologyEntitiesPaged(query,  0);
        for (OntologyServiceResponse.Finding finding : goResult.getFindings()) {
          ConceptSearchResult c = new ConceptSearchResult(aJCas);
          //SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.setQueryString(query);
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result doResult = service.findDiseaseOntologyEntitiesPaged(query,  0);
        for (OntologyServiceResponse.Finding finding : doResult.getFindings()) {
          ConceptSearchResult c = new ConceptSearchResult(aJCas);
          //SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.setQueryString(query);
          c.addToIndexes();
        }
        
        OntologyServiceResponse.Result meshResult = service.findMeshEntitiesPaged(query, 0);
        // for (OntologyServiceResponse.Finding finding : goResult.getFindings()) {
        for (OntologyServiceResponse.Finding finding : meshResult.getFindings()) {
          ConceptSearchResult c = new ConceptSearchResult(aJCas);
          //SearchResult c = new SearchResult(aJCas);
          c.setUri(finding.getConcept().getUri());
          c.setScore(finding.getScore());
          c.setQueryString(query);
          c.addToIndexes();
        }
      }
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (IllegalStateException e) {
      e.printStackTrace();
    }

  }

}
