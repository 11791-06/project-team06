package Team6;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.kb.Triple;
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;
/*
 * Project 0 
 * Ranking Results : TypeSystem :: SearchResult
 * Document, ConceptSearchResult, TripleSearchResult
 * Assume the previous Ranking gives a question with N documents
 * */
public class Evaluator extends CasConsumer_ImplBase{
  ArrayList<Double> docAPList, conAPList, triAPList, sniAPList;
  double docRecall, conRecall, triRecall, sniRecall;
  double docPrec, conPrec, triPrec, sniPrec;
  
  public void initialize() throws ResourceInitializationException {
    docAPList = new ArrayList<Double>();
    conAPList = new ArrayList<Double>();
    triAPList = new ArrayList<Double>();
    sniAPList = new ArrayList<Double>();
  }
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas aJCas;
    try {
      aJCas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
    FSIterator<?> qit = aJCas.getAnnotationIndex(Question.type).iterator();
    Question question = null;
    if (qit.hasNext()) {
      question = (Question) qit.next();
    }
    processDoc(aJCas);
    processTri(aJCas);
    processCon(aJCas);
    processSni(aJCas);
  }
  /*
   * Documents
   * */
  public void processDoc(JCas aJCas) throws AnalysisEngineProcessException {
    HashSet<String> groundtruthDoc = new HashSet<String>();
    ArrayList<Document> documents = new ArrayList<Document>();
    FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Document.type);
    while (iter.hasNext()) {
      Document doc = (Document) iter.next();
      if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
        groundtruthDoc.add(doc.getUri());
      } else {
        documents.add(doc);
      }
    }
    
    int mini = 100000000, maxi = -1;
    for (Document doc : documents) {
      maxi = Math.max(maxi, doc.getRank());
      mini = Math.min(mini, doc.getRank());
    }
    //System.err.println("# docs in eval = " + (maxi - mini + 1));
    String[] docs = new String[Math.max(0, maxi - mini + 1)];
    for (Document doc : documents) {
      docs[doc.getRank() - mini] = doc.getUri();
    }
    docAPList.add(calcAP(docs, groundtruthDoc));
    docRecall = calcRecall(docs, groundtruthDoc);
    docPrec = calcPrecision(docs, groundtruthDoc);
  }
  /*
   * Concepts
   * */
  private void processCon(JCas aJCas) {
    HashSet<String> groundtruthDoc = new HashSet<String>();
    ArrayList<ConceptSearchResult> documents = new ArrayList<ConceptSearchResult>();
    FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(
        ConceptSearchResult.type);
    while (iter.hasNext()) {
      ConceptSearchResult doc = (ConceptSearchResult) iter.next();
      if (doc.getSearchId() != null
          && doc.getSearchId().equals("__gold__")) {
        groundtruthDoc.add(doc.getUri());
      } else {
        documents.add(doc);
      }
    }
    
    String[] docs = new String[documents.size()];
    for (ConceptSearchResult doc : documents) {
      docs[doc.getRank()] = doc.getUri();
    }
    conAPList.add(calcAP(docs, groundtruthDoc));
    conRecall = calcRecall(docs, groundtruthDoc);
    conPrec = calcPrecision(docs, groundtruthDoc);
  }
  /*
   * Triple
   * */
  private void processTri(JCas aJCas) {
    HashSet<String> groundtruthDoc = new HashSet<String>();
    ArrayList<TripleSearchResult> documents = new ArrayList<TripleSearchResult>();
    FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(
        TripleSearchResult.type);
    while (iter.hasNext()) {
      TripleSearchResult doc = (TripleSearchResult) iter.next();
      if (doc.getSearchId() != null
          && doc.getSearchId().equals("__gold__")) {
        groundtruthDoc.add(triple2String(doc));
      } else {
        documents.add(doc);
      }
    }
    String[] docs = new String[documents.size()];
    for (TripleSearchResult doc : documents) {
      docs[doc.getRank()] = triple2String(doc);
    }
    triAPList.add(calcAP(docs, groundtruthDoc));
    triRecall = calcRecall(docs, groundtruthDoc);
    triPrec = calcPrecision(docs, groundtruthDoc);
  }
  
  /*
   * Snippets
   * */
  private void processSni(JCas aJCas) {
    HashSet<String> groundtruthDoc = new HashSet<String>();
    ArrayList<Passage> documents = new ArrayList<Passage>();
    FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(
        Passage.type);
    while (iter.hasNext()) {
      Passage doc = (Passage) iter.next();
      if (doc.getSearchId() != null
          && doc.getSearchId().equals("__gold__")) {
        groundtruthDoc.add(Sni2String(doc));
      } else {
        documents.add(doc);
      }
    }
    String[] docs = new String[documents.size()];
    for (Passage doc : documents) {
      docs[doc.getRank()] = Sni2String(doc);
    }
    sniAPList.add(calcAP(docs, groundtruthDoc));
    sniRecall = calcRecall(docs, groundtruthDoc);
    sniPrec = calcPrecision(docs, groundtruthDoc);
  }

  
  private String Sni2String(Passage doc) {
    String uri = doc.getUri();
    String text = doc.getDocId();
    int begin = doc.getOffsetInBeginSection();
    int end = doc.getOffsetInEndSection();
    String beginS = doc.getBeginSection();
    String endS = doc.getEndSection();
    
    return uri + "$" + text + "$" + begin + "$" + end + "$" + beginS + "$" + endS;
  }
  
  private String triple2String(TripleSearchResult tsr) {
    Triple triple = tsr.getTriple();
    return triple.getSubject() + "$" + triple.getObject() + "$"
        + triple.getPredicate();
  }
  
  private double calcAP(String[] docs, HashSet<String> groundtruthDoc) {
    double sum = 0, correct = 0;
    for (int i = 0; i < docs.length; ++ i) {
      if (groundtruthDoc.contains(docs[i])) {
        correct += 1;
        sum += correct / (i + 1);
      }
    }
    if (correct > 0) {
      sum /= correct;
    }
    return sum;
  }
  /*
   * Recall
   * */
  
  private double calcRecall(String[] docs, HashSet<String> groundtruthDoc) {
    double correct = 0, size = 0, sum = 0;
    size = groundtruthDoc.size();
    
    for (int i = 0; i < docs.length; ++ i) {
      if (groundtruthDoc.contains(docs[i])) {
        correct += 1;
        //sum += correct / (i + 1);
      }
    }
    if (size > 0) {
      sum = correct/size;
    }
    return sum;
  }
  
  /*
   * Precision
   * */
  
  private double calcPrecision(String[] docs, HashSet<String> groundtruthDoc) {
    double correct = 0, size = 0, sum = 0;
    size = docs.length;
    
    for (int i = 0; i < docs.length; ++ i) {
      if (groundtruthDoc.contains(docs[i])) {
        correct += 1;
        //sum += correct / (i + 1);
      }
    }
    if (size > 0) {
      sum = correct/size;
    }
    return sum;
  }
  
  /*
   * MAP
   * */
  private double APList2MAP(ArrayList<Double> APList) {
    double sum = 0;
    for (Double AP : APList) {
      sum += AP;
    }
    if (docAPList.size() > 0) {
      sum /= APList.size();
    }
    return sum;
  }
  /*
   * GMAP
   * */
  private double APList2GMAP(ArrayList<Double> APList) {
    double eps = 0.01;
    double sum = 0;
    for (Double AP : APList) {
      sum *= (AP + eps);
    }
    if (docAPList.size() > 0) {
      sum =  Math.pow(sum, 1.0 / APList.size());
    }
    return sum;
  }
  public void collectionProcessComplete(ProcessTrace arg0)
          throws ResourceProcessException, IOException {
        /*doc*/
        System.err.println("MAP@Doc = " + APList2MAP(docAPList));
        System.err.println("GMAP@Doc = " + APList2GMAP(docAPList));
        System.err.println("Recall@Doc = " + docRecall);
        System.err.println("Precision@Doc = " + docPrec);
        double docF1 = 2*docRecall*docPrec/(docRecall + docPrec);
        System.err.println("F1@Doc = " + docF1);
        
        /*concept*/
        System.err.println("MAP@Con = " + APList2MAP(conAPList));
        System.err.println("GMAP@Con = " + APList2GMAP(conAPList));
        System.err.println("Recall@Con = " + conRecall);
        System.err.println("Precision@Con = " + conPrec);
        double conF1 = 2*conRecall*conPrec/(conRecall + conPrec);
        System.err.println("F1@Con = " + conF1);
        

         /*tri*/
        System.err.println("MAP@Tri = " + APList2MAP(triAPList));
        System.err.println("GMAP@Tri = " + APList2GMAP(triAPList));
        System.err.println("Recall@Tri = " + triRecall);
        System.err.println("Precision@Tri = " + triPrec);
        double triF1 = 2*triRecall*triPrec/(triRecall + triPrec);
        System.err.println("F1@Tri = " + triF1);
        
        /*snippts*/
        System.err.println("MAP@sni = " + APList2MAP(sniAPList));
        System.err.println("GMAP@sni  = " + APList2GMAP(sniAPList));
        System.err.println("Recall@sni  = " + sniRecall);
        System.err.println("Precision@sni  = " + sniPrec);
        double sniF1 = 2*sniRecall*sniPrec/(sniRecall + sniPrec);
        System.err.println("F1@sni = " + sniF1);
        
        System.err.println("[done]");
      }


}