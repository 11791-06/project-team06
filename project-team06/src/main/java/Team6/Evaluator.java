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

import edu.cmu.lti.oaqa.type.answer.Answer;
import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.kb.Entity;
import edu.cmu.lti.oaqa.type.kb.Triple;
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.Document;
import edu.cmu.lti.oaqa.type.retrieval.Passage;
import edu.cmu.lti.oaqa.type.retrieval.SearchResult;
import edu.cmu.lti.oaqa.type.retrieval.TripleSearchResult;

/*
 * Project 0 
 * Ranking Results : TypeSystem :: SearchResult
 * Document, ConceptSearchResult, TripleSearchResult
 * Assume the previous Ranking gives a question with N documents
 * */
public class Evaluator extends CasConsumer_ImplBase {
    ArrayList<Double> docAPList, conAPList, triAPList, sniAPList;

    ArrayList<Double> docRecall, conRecall, triRecall, sniRecall;

    ArrayList<Double> docPrec, conPrec, triPrec, sniPrec;
    
    ArrayList<Double> factoidAcc1, factoidAcc5, factoidMRR;

    public void initialize() throws ResourceInitializationException {
        docAPList = new ArrayList<Double>();
        conAPList = new ArrayList<Double>();
        triAPList = new ArrayList<Double>();
        sniAPList = new ArrayList<Double>();

        docRecall = new ArrayList<Double>();
        conRecall = new ArrayList<Double>();
        triRecall = new ArrayList<Double>();
        sniRecall = new ArrayList<Double>();

        docPrec = new ArrayList<Double>();
        conPrec = new ArrayList<Double>();
        triPrec = new ArrayList<Double>();
        sniPrec = new ArrayList<Double>();
        
        factoidAcc1 = new ArrayList<Double>();
        factoidAcc5 = new ArrayList<Double>();
        factoidMRR = new ArrayList<Double>();
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
        processCon(aJCas);
        processTri(aJCas);
        processSni(aJCas);
        processFactoid(aJCas);
    }

    public void processFactoid(JCas aJCas)throws ResourceProcessException {
        // TODO Auto-generated method stub
        FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Answer.type);
        String answer = "NOT_FACT";
        ArrayList<Answer> answerList = new ArrayList<Answer>();
        while (iter.hasNext()) {
            Answer ans = (Answer) iter.next();
            if (ans.getRank() == -11791) {
                answer = ans.getText();
                //System.err.println("[Debug] Gold Standard Answer = " + ans.getText() + " " + ans.getRank());
            } else {
                answerList.add(ans);
                //System.err.println("[Debug] Retrieval Factoid Answer  = " + ans.getText() + " " + ans.getRank());
            }
        }
        if (answer.equals("NOT_FACT")) {
            return;
        }
        
        //System.err.println("[Debug] answer list size = " + answerList.size());
        double acc1 = 0, acc5 = 0;
        double mrr = 0;
        for (Answer ans : answerList) {
            if ((answer.toLowerCase()).equals((ans.getText()).toLowerCase())) {
                System.err.println("[Debug] Factoid GoldStandard and Retrieval Matched!!!!");
                //while(1);
                // mrr = 1.0 / (ans.getRank() + 1);
                /* rank from 1 here*/
                mrr = 1.0 / (ans.getRank());
                //acc5 += 1;
                
                //if (ans.getRank() < answerList.size()) {
                if (ans.getRank() < 5) {
                    acc5 += 1;
                }
                if (ans.getRank() < 1) {
                    acc1 += 1;
                }
            }
        }
        factoidAcc1.add(acc1);
        factoidAcc5.add(acc5);
        factoidMRR.add(mrr);
    }

    /*
     * Documents
     */
    public void processDoc(JCas aJCas) throws AnalysisEngineProcessException {
        HashSet<String> groundtruthDoc = new HashSet<String>();
        ArrayList<Document> documents = new ArrayList<Document>();
        FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Document.type);
        while (iter.hasNext()) {
            Document doc = (Document) iter.next();
            if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
                groundtruthDoc.add(doc.getUri());
               // System.err.println("[Debug] + Doc " + doc.getUri());
            } else {
                documents.add(doc);
            }
        }

        int mini = 100000000, maxi = -1;
        for (Document doc : documents) {
            maxi = Math.max(maxi, doc.getRank());
            mini = Math.min(mini, doc.getRank());
        }

        String[] docs = new String[Math.max(0, maxi - mini + 1)];
        for (Document doc : documents) {
            //System.err.println("DocRank   =    " + doc.getRank());
            docs[doc.getRank() - mini] = doc.getUri();
        }
        // if( calcAP(docs, groundtruthDoc) > 0) {
        docAPList.add(calcAP(docs, groundtruthDoc));
        // }

        // if(calcRecall(docs, groundtruthDoc) > 0) {
        docRecall.add(calcRecall(docs, groundtruthDoc));
        // }
        // if (calcPrecision(docs, groundtruthDoc) > 0){
        docPrec.add(calcPrecision(docs, groundtruthDoc));
        // }

    }

    /*
     * Concepts
     */
    private void processCon(JCas aJCas) {
        HashSet<String> groundtruthDoc = new HashSet<String>();
        // ArrayList<ConceptSearchResult> documents = new ArrayList<ConceptSearchResult>();
        ArrayList<ConceptSearchResult> documents = new ArrayList<ConceptSearchResult>();
        FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(
                        ConceptSearchResult.type);
        while (iter.hasNext()) {
            ConceptSearchResult doc = (ConceptSearchResult) iter.next();
            /*
             * if (doc instanceof ConceptSearchResult){ System.err.println("Yes!!!"); }
             */
            if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
                // System.err.println("Gold Standard == " + doc.getUri());
                groundtruthDoc.add(doc.getUri());
            } else {
                documents.add(doc);
                // System.err.println("Doc == " + doc.getUri());
            }
        }

        String[] docs = new String[documents.size()];
        for (ConceptSearchResult doc : documents) {
            // System.err.println("ConceptDoc ===" + doc.getUri());

            // System.err.println("ConceptRank   =    " + doc.getRank());
            docs[doc.getRank()] = doc.getUri();
        }
        // if( calcAP(docs, groundtruthDoc) > 0) {
        conAPList.add(calcAP(docs, groundtruthDoc));
        // }
        // conRecall = calcRecall(docs, groundtruthDoc);
        // conPrec = calcPrecision(docs, groundtruthDoc);

        // if(calcRecall(docs, groundtruthDoc) > 0) {
        conRecall.add(calcRecall(docs, groundtruthDoc));
        // }
        // if (calcPrecision(docs, groundtruthDoc) > 0){
        conPrec.add(calcPrecision(docs, groundtruthDoc));
        // }
    }

    /*
     * Triple
     */
    private void processTri(JCas aJCas) {
        HashSet<String> groundtruthDoc = new HashSet<String>();
        ArrayList<TripleSearchResult> documents = new ArrayList<TripleSearchResult>();
        FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(
                        TripleSearchResult.type);
        while (iter.hasNext()) {
            TripleSearchResult doc = (TripleSearchResult) iter.next();
            if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
                groundtruthDoc.add(triple2String(doc));
                System.err.println(triple2String(doc));
            } else {
                documents.add(doc);
            }
        }
        System.err.println("# Triple Results = " + documents.size());
        String[] docs = new String[documents.size()];
        for (TripleSearchResult doc : documents) {
            docs[doc.getRank()] = triple2String(doc);
        }
        triAPList.add(calcAP(docs, groundtruthDoc));
        triRecall.add(calcRecall(docs, groundtruthDoc));
        triPrec.add(calcPrecision(docs, groundtruthDoc));
    }

    /*
     * Snippets
     */
    private void processSni(JCas aJCas) {
        HashSet<String> groundtruthDoc = new HashSet<String>();
        ArrayList<Passage> documents = new ArrayList<Passage>();
        FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Passage.type);
        while (iter.hasNext()) {
            Passage doc = (Passage) iter.next();
            if (doc.getSearchId() != null && doc.getSearchId().equals("__gold__")) {
                groundtruthDoc.add(Sni2String(doc));
            } else {
                documents.add(doc);
            }
        }
        String[] docs = new String[documents.size()];
        for (Passage doc : documents) {
            docs[doc.getRank()] = Sni2String(doc);
        }
        //if (calcAP(docs, groundtruthDoc) > 0) {
            sniAPList.add(calcAP(docs, groundtruthDoc));
        //}

        // sniRecall = calcRecall(docs, groundtruthDoc);
        // sniPrec = calcPrecision(docs, groundtruthDoc);

        //if (calcRecall(docs, groundtruthDoc) > 0) {
            sniRecall.add(calcRecall(docs, groundtruthDoc));
        //}
        //if (calcPrecision(docs, groundtruthDoc) > 0) {
            sniPrec.add(calcPrecision(docs, groundtruthDoc));
        //}
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

    private String triple2String(TripleSearchResult doc) {
        doc = (TripleSearchResult) doc;
        Triple triple = ((TripleSearchResult) doc).getTriple();
        // System.err.println(triple);
        return triple.getSubject() + "$" + triple.getObject() + "$" + triple.getPredicate();
    }

    private double calcAP(String[] docs, HashSet<String> groundtruthDoc) {
        double sum = 0, correct = 0;
        for (int i = 0; i < docs.length; ++i) {
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
     */

    /**
     * problems existed in how to compute those metrics
     * */

    private double calcRecall(String[] docs, HashSet<String> groundtruthDoc) {
        double correct = 0, size = 0, sum = 0;
        size = groundtruthDoc.size();

        for (int i = 0; i < docs.length; ++i) {
            if (groundtruthDoc.contains(docs[i])) {
                correct += 1;
                // sum += correct / (i + 1);
            }
        }
        if (size > 0) {
            sum = correct / size;
        }
        // if (size == 0) return -1;
        return sum;
    }

    /*
     * Precision
     */

    private double calcPrecision(String[] docs, HashSet<String> groundtruthDoc) {
        double correct = 0, size = 0, sum = 0;
        size = docs.length;

        for (int i = 0; i < docs.length; ++i) {
            if (groundtruthDoc.contains(docs[i])) {
                correct += 1;
                // sum += correct / (i + 1);
            }
        }
        if (size > 0) {
            sum = correct / size;
        }
        // if (size == 0) return -1;
        return sum;
    }

    /*
     * MAP
     */
    private double List2Value(ArrayList<Double> APList) {
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
     */
    private double APList2GMAP(ArrayList<Double> APList) {
        double eps = 0.01;
        double sum = 1;
        for (Double AP : APList) {
            sum *= (AP + eps);
        }
        if (docAPList.size() > 0) {
            sum = Math.pow(sum, 1.0 / APList.size());
        }
        return sum;
    }

    public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
                    IOException {
        /* doc */
        System.err.println("MAP@Doc = " + List2Value(docAPList));
        System.err.println("GMAP@Doc = " + APList2GMAP(docAPList));
        System.err.println("Recall@Doc = " + List2Value(docRecall));
        System.err.println("Precision@Doc = " + List2Value(docPrec));
        double docF1 = 2 * List2Value(docRecall) * List2Value((docPrec))
                        / (List2Value(docRecall) + List2Value(docPrec));
        System.err.println("F1@Doc = " + docF1);

        /* concept */
        System.err.println("MAP@Con = " + List2Value(conAPList));
        System.err.println("GMAP@Con = " + APList2GMAP(conAPList));
        System.err.println("Recall@Con = " + List2Value(conRecall));
        System.err.println("Precision@Con = " + List2Value(conPrec));
        // double conF1 = 2*conRecall*conPrec/(conRecall + conPrec);
        double conF1 = 2 * List2Value(conRecall) * List2Value((conPrec))
                        / (List2Value(conRecall) + List2Value(conPrec));
        System.err.println("F1@Con = " + conF1);

        System.err.println("Con TripleList Size====" + triAPList.size());

        /* tri */
        System.err.println("MAP@Tri = " + List2Value(triAPList));
        System.err.println("GMAP@Tri = " + APList2GMAP(triAPList));
        double triR = List2Value(triRecall);
        double triP = List2Value(triPrec);
        System.err.println("Recall@Tri = " + triR);
        System.err.println("Precision@Tri = " + triP);
        double triF1 = 2 * triR * triP / (triR + triP);
        System.err.println("F1@Tri = " + triF1);

        /* snippts */
        System.err.println("MAP@sni = " + List2Value(sniAPList));
        System.err.println("GMAP@sni  = " + APList2GMAP(sniAPList));
        double sniR = List2Value(sniRecall);
        double sniP = List2Value(sniPrec);
        System.err.println("Recall@sni  = " + sniR);
        System.err.println("Precision@sni  = " + sniP);
        double sniF1 = 2 * sniR * sniP / (sniR + sniP);
        System.err.println("F1@sni = " + sniF1);
        
        System.err.println("SAcc@Factoid = " + List2Value(factoidAcc1));
        System.err.println("LAcc@Factoid = " + List2Value(factoidAcc5));
        System.err.println("MRR@Factoid = " + List2Value(factoidMRR));

        System.err.println("[done]");
    }

}
