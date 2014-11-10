package Team6;
import java.io.FileNotFoundException;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.oaqa.type.input.Question;

public class QuestionNormalization extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {

	FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
	if (iter.isValid()) {
	    iter.moveToNext();
	    Question question = (Question) iter.get(); //here is where I get the Question type object from the previous stage
	    try {
		normalization(question); //this functin will directly modify the text in Question object
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	}
    }

    /**
     * perform basic normalization
     * @param str
     * @return
     */
    public String normalizeCaseStem(String str) {
	StringBuilder answer = new StringBuilder();
	
	for (String s : str.split("\\s+")) {
	    // normalization
	    String tmp = s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
	    // stemming
	    String stemword = StanfordLemmatizer.stemWord(tmp);
	    answer.append(stemword).append(" ");
	}
	return answer.substring(0,answer.length()-1);
    }

    private void normalization(Question question)
	    throws FileNotFoundException {
	String text = question.getText();
	question.setText(normalizeCaseStem(text));
    }

}
