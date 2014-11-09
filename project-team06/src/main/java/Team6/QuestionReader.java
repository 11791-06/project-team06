package Team6;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import json.JsonCollectionReaderHelper;
import json.gson.QuestionType;
import json.gson.TestQuestion;
import json.gson.TestSet;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;

import com.google.common.collect.Lists;

import edu.cmu.lti.oaqa.type.input.*;

public class QuestionReader extends CollectionReader_ImplBase {
  List<TestQuestion> inputs;
  int ptr;
  
  @Override
  public void initialize() {
  String filePath = "/BioASQ-SampleData1B.json";
  inputs = Lists.newArrayList();
  /*inputs = TestSet
      .load(new FileInputStream(filePath)).stream()
      .collect(toList());*/
  Object value = filePath;
  inputs = TestSet
          .load(getClass().getResourceAsStream(
              String.class.cast(value))).stream()
          .collect(toList());
  // trim question texts
  inputs.stream()
      .filter(input -> input.getBody() != null)
      .forEach(
          input -> input.setBody(input.getBody().trim()
              .replaceAll("\\s+", " ")));
  ptr = 0;
  }

  @Override
  public void getNext(CAS aCAS) throws IOException, CollectionException {
  JCas jcas = null;
  try {
    jcas = aCAS.getJCas();
  } catch (CASException e) {
    e.printStackTrace();
  }
  TestQuestion input = inputs.get(ptr ++);
  JsonCollectionReaderHelper.addQuestionToIndex(input, "", jcas);
  }

  @Override
  public boolean hasNext() throws IOException, CollectionException {
    return ptr < inputs.size();
  }

  @Override
  public Progress[] getProgress() {
    return null;
  }

  @Override
  public void close() throws IOException {
  }
}