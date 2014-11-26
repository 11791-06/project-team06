package Team6;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.nlp.Parse;
import edu.cmu.lti.oaqa.type.nlp.Token;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class QuestionNormalization extends JCasAnnotator_ImplBase {

  private Set<String> stopWords = new HashSet<String>();

  private StanfordCoreNLP pipeline;

  public QuestionNormalization() {
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos");
    pipeline = new StanfordCoreNLP(props);
    try {
      BufferedReader br = new BufferedReader(new FileReader("src/main/java/stopword"));
      String str = "";
      //initialize the stop word list
      while ((str = br.readLine()) != null) {
        stopWords.add(str);
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      Question question = (Question) iter.get(); // here is where I get the Question type object
                                                 // from the previous stage
      try {
        normalization(jcas, question); // this functin will directly modify the text in Question
                                       // object
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public void normalization(JCas jcas, Question question) throws FileNotFoundException {
      
      String text = question.getText();

      List<CoreMap> sentences = posTagging(text);
      List<String> words = new ArrayList<String>();
      List<String> posTag = new ArrayList<String>();
      for (CoreMap sentence : sentences) {
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
          String pos = token.get(PartOfSpeechAnnotation.class);
          String tokenResult = token.get(TextAnnotation.class);
          posTag.add(pos);
          words.add(tokenResult);
        }
      }
      //String result = stopRemoval(normalizeCaseStem(text));
      String result = normHelper(jcas, words, posTag, question);
      question.setRankText(result);
      question.setText(result);
      question.setOriginalText(text);
    }

  public String queryOperator(String str) {
      String [] strArr = str.split(" ");
      //use OR
      StringBuilder answer = new StringBuilder();
      if (strArr.length >= 1)
	  answer.append(strArr[0]);
      for (int i = 1; i < strArr.length; i++) {
	  answer.append(" OR ").append(strArr[i]);
      }
      
      return answer.toString();
  }
  
  public String normHelper(JCas jcas, List<String> words, List<String> posTag, Question q) {
    StringBuilder answer = new StringBuilder();
    ArrayList<Token> tokenList = new ArrayList<Token>();
    for (int i = 0; i < words.size(); i++) {
      // case normalization (not sure if we are going to use this) //TODO
      String tmp = words.get(i).replaceAll("[^a-zA-Z ]", "").toLowerCase();
      // stemming
      String stemword = StanfordLemmatizer.stemWord(tmp);
      if (stopWords.contains(stemword))
        continue;
      answer.append(stemword).append(" ");
      Token tmpToken = new Token(jcas);
      tmpToken.setPartOfSpeech(posTag.get(i));
      tmpToken.setWord(stemword);
      tokenList.add(tmpToken);
    }
    Parse parse = new Parse(jcas);
    parse.setTokens(Utils.fromCollectionToFSList(jcas, tokenList));
    parse.addToIndexes();

    return answer.substring(0, answer.length() - 1);
  }



  public List<CoreMap> posTagging(String text) {
    edu.stanford.nlp.pipeline.Annotation document = new edu.stanford.nlp.pipeline.Annotation(text);
    pipeline.annotate(document);
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    return sentences;
  }

  
  
  
  /**
   * perform basic normalization
   * 
   * @param str
   * @return
   */
  /*
  public String normalizeCaseStem(String str) {
    StringBuilder answer = new StringBuilder();

    for (String s : str.split("\\s+")) {
      // case normalization (not sure if we are going to use this) TODO
      String tmp = s.replaceAll("[^a-zA-Z ]", "").toLowerCase();

      // stemming
      String stemword = StanfordLemmatizer.stemWord(tmp);
      answer.append(stemword).append(" ");
    }
    return answer.substring(0, answer.length() - 1);
  }*/

  /*
  public String stopRemoval(String str) {
    String[] strArr = str.split("\\s+");

    StringBuilder answer = new StringBuilder();
    for (int i = 0; i < strArr.length; i++) {
      if (stopWords.contains(strArr[i]))
        continue;
      else
        answer.append(strArr[i]).append(" ");
    }
    return answer.substring(0, answer.length() - 1);
  }*/
  
  
}
