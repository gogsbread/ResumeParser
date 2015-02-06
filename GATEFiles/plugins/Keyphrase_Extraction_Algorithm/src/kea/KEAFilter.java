/*
 *    KEAFilter.java
 *    Copyright (C) 2000, 2001 Eibe Frank
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package kea;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

/**
 * This filter converts the incoming data into data appropriate for
 * keyphrase classification. It assumes that the dataset contains two
 * string attributes. The first attribute should contain the text of a
 * document. The second attribute should contain the keyphrases
 * associated with that document (if present). 
 *
 * The filter converts every instance (i.e. document) into a set of
 * instances, one for each word-based n-gram in the document. The
 * string attribute representing the document is replaced by some
 * numeric features, the estimated probability of each n-gram being a
 * keyphrase, and the rank of this phrase in the document according to
 * the probability.  Each new instances also has a class value
 * associated with it. The class is "true" if the n-gram is a true
 * keyphrase, and "false" otherwise. Of course, if the input document
 * doesn't come with author-assigned keyphrases, the class values for
 * that document will be missing.  
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
  */
@SuppressWarnings({"serial","rawtypes","unchecked","cast","unused"})
public class KEAFilter extends Filter implements OptionHandler {

  /** Index of attribute containing the documents */
  private int m_DocumentAtt = 0;

  /** Index of attribute containing the keyphrases */
  private int m_KeyphrasesAtt = 1;

  /** The maximum length of phrases */
  private int m_MaxPhraseLength = 3;

  /** The minimum length of phrases */
  private int m_MinPhraseLength = 1;

  /** Is keyphrase frequency attribute being used? */
  private boolean m_KFused = false;

  /** Flag for debugging mode */
  private boolean m_Debug = false;

  /** Determines whether internal periods are allowed */
  private boolean m_DisallowInternalPeriods = false;

  /** The minimum number of occurences of a phrase */
  private int m_MinNumOccur = 2;

  /** The number of features describing a phrase */
  private int m_NumFeatures = 2;

  /* Indices of attributes in m_ClassifierData */
  private int m_TfidfIndex = 0;
  private int m_FirstOccurIndex = 1;
  private int m_KeyFreqIndex = 2;

  /** The punctuation filter used by this filter */
  private KEAPhraseFilter m_PunctFilter = null;

  /** The numbers filter used by this filter */
  private NumbersFilter m_NumbersFilter = null;
 
  /** The actual classifier used to compute probabilities */
  private Classifier m_Classifier = null;

  /** The dictionary containing the document frequencies */
  private HashMap m_Dictionary = null;

  /** The dictionary containing the keyphrases */
  private HashMap m_KeyphraseDictionary = null;

  /** The number of documents in the global frequencies corpus */
  private int m_NumDocs = 0;

  /** Template for the classifier data */
  private Instances m_ClassifierData = null;

  /** The stemmer to be used */
  private Stemmer m_Stemmer = new IteratedLovinsStemmer();

  /** The list of stop words to be used */
  private Stopwords m_Stopwords = new StopwordsEnglish();

  /** Determines whether check for proper nouns is performed */
  private boolean m_CheckForProperNouns = true;

  /**
   * Get the M_CheckProperNouns value.
   * @return the M_CheckProperNouns value.
   */
  public boolean getCheckForProperNouns() {
    return m_CheckForProperNouns;
  }

  /**
   * Set the M_CheckProperNouns value.
   * @param newM_CheckProperNouns The new M_CheckProperNouns value.
   */
  public void setCheckForProperNouns(boolean newM_CheckProperNouns) {
    this.m_CheckForProperNouns = newM_CheckProperNouns;
  }
  
  /**
   * Get the M_Stopwords value.
   * @return the M_Stopwords value.
   */
  public Stopwords getStopwords() {
    return m_Stopwords;
  }

  /**
   * Set the M_Stopwords value.
   * @param newM_Stopwords The new M_Stopwords value.
   */
  public void setStopwords(Stopwords newM_Stopwords) {
    this.m_Stopwords = newM_Stopwords;
  }

  
  /**
   * Get the Stemmer value.
   * @return the Stemmer value.
   */
  public Stemmer getStemmer() {

    return m_Stemmer;
  }

  /**
   * Set the Stemmer value.
   * @param newStemmer The new Stemmer value.
   */
  public void setStemmer(Stemmer newStemmer) {

    this.m_Stemmer = newStemmer;
  }

  /**
   * Get the value of MinNumOccur.
   *
   * @return Value of MinNumOccur.
   */
  public int getMinNumOccur() {
    
    return m_MinNumOccur;
  }
  
  /**
   * Set the value of MinNumOccur.
   *
   * @param newMinNumOccur Value to assign to MinNumOccur.
   */
  public void setMinNumOccur(int newMinNumOccur) {
    
    m_MinNumOccur = newMinNumOccur;
  }
  
  /**
   * Get the value of MaxPhraseLength.
   *
   * @return Value of MaxPhraseLength.
   */
  public int getMaxPhraseLength() {
    
    return m_MaxPhraseLength;
  }
  
  /**
   * Set the value of MaxPhraseLength.
   *
   * @param newMaxPhraseLength Value to assign to MaxPhraseLength.
   */
  public void setMaxPhraseLength(int newMaxPhraseLength) {
    
    m_MaxPhraseLength = newMaxPhraseLength;
  }
  
  /**
   * Get the value of MinPhraseLength.
   *
   * @return Value of MinPhraseLength.
   */
  public int getMinPhraseLength() {
    
    return m_MinPhraseLength;
  }
  
  /**
   * Set the value of MinPhraseLength.
   *
   * @param newMinPhraseLength Value to assign to MinPhraseLength.
   */
  public void setMinPhraseLength(int newMinPhraseLength) {
    
    m_MinPhraseLength = newMinPhraseLength;
  }

  /**
   * Returns the index of the stemmed phrases in the output ARFF file.
   */
  public int getStemmedPhraseIndex() {

    return m_DocumentAtt;
  }

  /**
   * Returns the index of the unstemmed phrases in the output ARFF file.
   */
  public int getUnstemmedPhraseIndex() {

    return m_DocumentAtt + 1;
  }

  /**
   * Returns the index of the phrases' probabilities in the output ARFF file.
   */
  public int getProbabilityIndex() {

    int index = m_DocumentAtt + 4;

    if (m_Debug) {
      if (m_KFused) {
	index++;
      }
    }
    return index;
  }

  /**
   * Returns the index of the phrases' ranks in the output ARFF file.
   */
  public int getRankIndex() {

    return getProbabilityIndex() + 1;
  }

  /**
   * Get the value of DocumentAtt.
   *
   * @return Value of DocumentAtt.
   */
  public int getDocumentAtt() {
    
    return m_DocumentAtt;
  }
  
  /**
   * Set the value of DocumentAtt.
   *
   * @param newDocumentAtt Value to assign to DocumentAtt.
   */
  public void setDocumentAtt(int newDocumentAtt) {
    
    m_DocumentAtt = newDocumentAtt;
  }
  
  /**
   * Get the value of KeyphraseAtt.
   *
   * @return Value of KeyphraseAtt.
   */
  public int getKeyphrasesAtt() {
    
    return m_KeyphrasesAtt;
  }
  
  /**
   * Set the value of KeyphrasesAtt.
   *
   * @param newKeyphrasesAtt Value to assign to KeyphrasesAtt.
   */
  public void setKeyphrasesAtt(int newKeyphrasesAtt) {
    
    m_KeyphrasesAtt = newKeyphrasesAtt;
  }
  
  
  /**
   * Get the value of Debug.
   *
   * @return Value of Debug.
   */
  public boolean getDebug() {
    
    return m_Debug;
  }
  
  /**
   * Set the value of Debug.
   *
   * @param newDebug Value to assign to Debug.
   */
  public void setDebug(boolean newDebug) {
    
    m_Debug = newDebug;
  }

  /**
   * Sets whether keyphrase frequency attribute is used.
   */
  public void setKFused(boolean flag) {

    m_KFused = flag;
    if (flag) {
      m_NumFeatures = 3;
    } else {
      m_NumFeatures = 2;
    }
  }

  /**
   * Gets whether keyphrase frequency attribute is used.
   */
  public boolean getKFused() {

    return m_KFused;
  }

  /**
   * Get whether the supplied columns are to be processed
   *
   * @return true if the supplied columns won't be processed
   */
  public boolean getDisallowInternalPeriods() {

    return m_DisallowInternalPeriods;
  }

  /**
   * Set whether selected columns should be processed. If true the 
   * selected columns won't be processed.
   *
   * @param invert the new invert setting
   */
  public void setDisallowInternalPeriods(boolean disallow) {

    m_DisallowInternalPeriods = disallow;
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -K<br>
   * Specifies whether keyphrase frequency statistic is used.<p>
   *
   * -M length<br>
   * Sets the maximum phrase length (default: 3).<p>
   *
   * -L length<br>
   * Sets the minimum phrase length (default: 1).<p>
   *
   * -D<br>
   * Turns debugging mode on.<p>
   *
   * -I index<br>
   * Sets the index of the attribute containing the documents (default: 0).<p>
   *
   * -J index<br>
   * Sets the index of the attribute containing the keyphrases (default: 1).<p>
   *
   * -P<br>
   * Disallow internal periods <p>
   *
   * -O number<br>
   * The minimum number of times a phrase needs to occur (default: 2). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setKFused(Utils.getFlag('K', options));
    setDebug(Utils.getFlag('D', options));
    String docAttIndexString = Utils.getOption('I', options);
    if (docAttIndexString.length() > 0) {
      setDocumentAtt(Integer.parseInt(docAttIndexString) - 1);
    } else {
      setDocumentAtt(0);
    }
    String keyphraseAttIndexString = Utils.getOption('J', options);
    if (keyphraseAttIndexString.length() > 0) {
      setKeyphrasesAtt(Integer.parseInt(keyphraseAttIndexString) - 1);
    } else {
      setKeyphrasesAtt(1);
    }
    String maxPhraseLengthString = Utils.getOption('M', options);
    if (maxPhraseLengthString.length() > 0) {
      setMaxPhraseLength(Integer.parseInt(maxPhraseLengthString));
    } else {
      setMaxPhraseLength(3);
    }
    String minPhraseLengthString = Utils.getOption('M', options);
    if (minPhraseLengthString.length() > 0) {
      setMinPhraseLength(Integer.parseInt(minPhraseLengthString));
    } else {
      setMinPhraseLength(1);
    }
    String minNumOccurString = Utils.getOption('O', options);
    if (minNumOccurString.length() > 0) {
      setMinNumOccur(Integer.parseInt(minNumOccurString));
    } else {
      setMinNumOccur(2);
    }
    setDisallowInternalPeriods(Utils.getFlag('P', options));
   }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [13];
    int current = 0;

    if (getKFused()) {
      options[current++] = "-K";
    }
    if (getDebug()) {
      options[current++] = "-D";
    }
    options[current++] = "-I"; 
    options[current++] = "" + (getDocumentAtt() + 1);
    options[current++] = "-J"; 
    options[current++] = "" + (getKeyphrasesAtt() + 1);
    options[current++] = "-M"; 
    options[current++] = "" + (getMaxPhraseLength());
    options[current++] = "-L"; 
    options[current++] = "" + (getMinPhraseLength());
    options[current++] = "-O"; 
    options[current++] = "" + (getMinNumOccur());

    if (getDisallowInternalPeriods()) {
      options[current++] = "-P";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(7);

    newVector.addElement(new Option(
	      "\tSpecifies whether keyphrase frequency statistic is used.",
              "K", 0, "-K"));
    newVector.addElement(new Option(
	      "\tSets the maximum phrase length (default: 3).",
              "M", 1, "-M <length>"));
    newVector.addElement(new Option(
	      "\tSets the minimum phrase length (default: 1).",
              "L", 1, "-L <length>"));
    newVector.addElement(new Option(
	      "\tTurns debugging mode on.",
              "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tSets the index of the document attribute (default: 0).",
              "I", 1, "-I"));
    newVector.addElement(new Option(
	      "\tSets the index of the keyphrase attribute (default: 1).",
              "J", 1, "-J"));
    newVector.addElement(new Option(
	      "\tDisallow internal periods.",
              "P", 0, "-P"));
    newVector.addElement(new Option(
	      "\tSet the minimum number of occurences (default: 2).",
              "O", 1, "-O"));

    return newVector.elements();
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Converts incoming data into data appropriate for " +
      "keyphrase classification.";
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input
   * instance structure (any instances contained in the object are
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately 
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    if (instanceInfo.classIndex() >= 0) {
      throw new Exception("Don't know what do to if class index set!");
    }
    if (!instanceInfo.attribute(m_KeyphrasesAtt).isString() ||
	!instanceInfo.attribute(m_DocumentAtt).isString()) {
      throw new Exception("Keyphrase attribute and document attribute " +
			  "need to be string attributes.");
    }
    m_PunctFilter = new KEAPhraseFilter();
    int[] arr = new int[1];
    arr[0] = m_DocumentAtt;
    m_PunctFilter.setAttributeIndicesArray(arr);
    m_PunctFilter.setInputFormat(instanceInfo);
    m_PunctFilter.setDisallowInternalPeriods(getDisallowInternalPeriods());
    m_NumbersFilter = new NumbersFilter();
    m_NumbersFilter.setInputFormat(m_PunctFilter.getOutputFormat());
    super.setInputFormat(m_NumbersFilter.getOutputFormat());
    return false;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (m_Debug) {
      System.err.println("-- Reading instance");
    }

    m_PunctFilter.input(instance);
    m_PunctFilter.batchFinished();
    instance = m_PunctFilter.output();

    m_NumbersFilter.input(instance);
    m_NumbersFilter.batchFinished();
    instance = m_NumbersFilter.output();
    
    if (m_Dictionary == null) {
      bufferInput(instance);
      return false;
    } else {
      FastVector vector = convertInstance(instance, false);
      Enumeration enumeration = vector.elements();
      while (enumeration.hasMoreElements()) {
	Instance inst = (Instance)enumeration.nextElement();
	push(inst);
      }
      return true;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_Dictionary == null) {
      buildGlobalDictionaries();
      buildClassifier();
      convertPendingInstances();
    } 
    flushInput();
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Builds the global dictionaries.
   */
  private void buildGlobalDictionaries() throws Exception {

    if (m_Debug) {
      System.err.println("--- Building global dictionaries");
    }
    
    // Build dictionary of n-grams with associated
    // document frequencies
    m_Dictionary = new HashMap();
    for (int i = 0; i < getInputFormat().numInstances(); i++) {
      String str = getInputFormat().instance(i).stringValue(m_DocumentAtt);
      HashMap hash = getPhrasesForDictionary(str);
      Iterator it = hash.keySet().iterator();
      while (it.hasNext()) {
	String phrase = (String)it.next();
	Counter counter = (Counter)m_Dictionary.get(phrase);
	if (counter == null) {
	  m_Dictionary.put(phrase, new Counter());
	} else {
	  counter.increment();
	}
      }
    }
    
    if (m_KFused) {
      
      // Build dictionary of n-grams that occur as keyphrases
      // with associated keyphrase frequencies
      m_KeyphraseDictionary = new HashMap();
      for (int i = 0; i < getInputFormat().numInstances(); i++) {
	String str = getInputFormat().instance(i).stringValue(m_KeyphrasesAtt);
	HashMap hash = getGivenKeyphrases(str, false);
	if (hash != null) {
	  Iterator it = hash.keySet().iterator();
	  while (it.hasNext()) {
	    String phrase = (String)it.next();
	    Counter counter = (Counter)m_KeyphraseDictionary.get(phrase);
	    if (counter == null) {
	      m_KeyphraseDictionary.put(phrase, new Counter());
	    } else {
	      counter.increment();
	    }
	  }
	}
      }
    } else {
      m_KeyphraseDictionary = null;
    }
    
    // Set the number of documents in the global corpus
    m_NumDocs = getInputFormat().numInstances();
  }
 
  /**
   * Builds the classifier.
   */
  private void buildClassifier() throws Exception {
    
    // Generate input format for classifier
    FastVector atts = new FastVector();
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (i == m_DocumentAtt) {
	atts.addElement(new Attribute("TFxIDF"));
	atts.addElement(new Attribute("First_occurrence"));
	if (m_KFused) {
	  atts.addElement(new Attribute("Keyphrase_frequency"));
	}
      } else if (i == m_KeyphrasesAtt) {
	FastVector vals = new FastVector(2);
	vals.addElement("False");
	vals.addElement("True");
	atts.addElement(new Attribute("Keyphrase?", vals));
      } 
    }
    m_ClassifierData = new Instances("ClassifierData", atts, 0);
    m_ClassifierData.setClassIndex(m_NumFeatures);
    
    if (m_Debug) {
      System.err.println("--- Converting instances for classifier");
    }
    
    // Convert pending input instances into data for classifier
    for(int i = 0; i < getInputFormat().numInstances(); i++) {
      Instance current = getInputFormat().instance(i);

      // Get the key phrases for the document
      String keyphrases = current.stringValue(m_KeyphrasesAtt);
      HashMap hashKeyphrases = getGivenKeyphrases(keyphrases, false);
      HashMap hashKeysEval = getGivenKeyphrases(keyphrases, true);

      // Get the phrases for the document
      HashMap hash = new HashMap();
      int length = getPhrases(hash, current.stringValue(m_DocumentAtt));

      // Compute the feature values for each phrase and
      // add the instance to the data for the classifier
      Iterator it = hash.keySet().iterator();
      while (it.hasNext()) {
	String phrase = (String)it.next();
	FastVector phraseInfo = (FastVector)hash.get(phrase);
	double[] vals =  featVals(phrase, phraseInfo, true,
				  hashKeysEval, hashKeyphrases, length);
	Instance inst = new Instance(current.weight(), vals);
	m_ClassifierData.add(inst);
      }
    }
    
    if (m_Debug) {
      System.err.println("--- Building classifier");
    }
    
    // Build classifier
    FilteredClassifier fclass = new FilteredClassifier();
    fclass.setClassifier(new NaiveBayesSimple());
    fclass.setFilter(new Discretize());
    m_Classifier = fclass;
    m_Classifier.buildClassifier(m_ClassifierData);

    if (m_Debug) {
      System.err.println(m_Classifier);
    }

    // Save space
    m_ClassifierData = new Instances(m_ClassifierData, 0);
  }

  /** 
   * Conmputes the feature values for a given phrase.
   */
  private double[] featVals(String phrase, FastVector phraseInfo, 
			    boolean training, HashMap hashKeysEval,
			    HashMap hashKeyphrases, int length) {

    // Compute feature values
    Counter counterLocal = (Counter)phraseInfo.elementAt(1);
    double[] newInst = new double[m_NumFeatures + 1];
	  
    // Compute TFxIDF
    Counter counterGlobal = (Counter)m_Dictionary.get(phrase);
    double localVal = counterLocal.value(), globalVal = 0;
    if (counterGlobal != null) {
      globalVal = counterGlobal.value();
      if (training) {
	globalVal = globalVal - 1;
      }
    }
    
    // Just devide by length to get approximation of probability
    // that phrase in document is our phrase
    newInst[m_TfidfIndex] = (localVal / ((double)length)) *
      (-Math.log((globalVal + 1)/ ((double)m_NumDocs + 1)));
	  
    // Compute first occurrence
    Counter counterFirst = (Counter)phraseInfo.elementAt(0);
    newInst[m_FirstOccurIndex] = (double)counterFirst.value() /
      (double)length;
    
    // Is keyphrase frequency attribute being used?
    if (m_KFused) {
      Counter keyphraseC = (Counter)m_KeyphraseDictionary.get(phrase);
      if ((training) && (hashKeyphrases != null) &&
	  (hashKeyphrases.containsKey(phrase))) {
	newInst[m_KeyFreqIndex] = keyphraseC.value() - 1;
      } else {
	if (keyphraseC != null) {
	  newInst[m_KeyFreqIndex] = keyphraseC.value();
	} else {
	  newInst[m_KeyFreqIndex] = 0;
	}
      }
    }

    // Compute class value
    String phraseInEvalFormat = evalFormat((String)phraseInfo.elementAt(2));
    if (hashKeysEval == null) { // no author-assigned keyphrases
      newInst[m_NumFeatures] = Instance.missingValue();
    } else if (!hashKeysEval.containsKey(phraseInEvalFormat)) {
      newInst[m_NumFeatures] = 0; // No keyphrase
    } else {
      hashKeysEval.remove(phraseInEvalFormat);
      newInst[m_NumFeatures] = 1; // Keyphrase
    }
    return newInst;
  }

  /**
   * Sets output format and converts pending input instances.
   */
  private void convertPendingInstances() throws Exception {

    if (m_Debug) {
      System.err.println("--- Converting pending instances");
    }
    
    // Create output format for filter
    FastVector atts = new FastVector();
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (i == m_DocumentAtt) {
	atts.addElement(new Attribute("N-gram", (FastVector)null));
	atts.addElement(new Attribute("N-gram-original", (FastVector)null));
	atts.addElement(new Attribute("TFxIDF"));
	atts.addElement(new Attribute("First_occurrence"));
	if (m_Debug) {
	  if (m_KFused) {
	    atts.addElement(new Attribute("Keyphrase_frequency"));
	  }
	}
	atts.addElement(new Attribute("Probability"));
	atts.addElement(new Attribute("Rank"));
      } else if (i == m_KeyphrasesAtt) {
	FastVector vals = new FastVector(2);
	vals.addElement("False");
	vals.addElement("True");
	atts.addElement(new Attribute("Keyphrase?", vals));
      } else {
	atts.addElement(getInputFormat().attribute(i));
      }
    }
    Instances outFormat = new Instances("KEAdata", atts, 0);
    setOutputFormat(outFormat);
    
    // Convert pending input instances into output data
    for(int i = 0; i < getInputFormat().numInstances(); i++) {
      Instance current = getInputFormat().instance(i);
      FastVector vector = convertInstance(current, true);
      Enumeration enumeration = vector.elements();
      while (enumeration.hasMoreElements()) {
	Instance inst = (Instance)enumeration.nextElement();
	push(inst);
      }
    }
  } 

  /**
   * Converts an instance.
   */
  private FastVector convertInstance(Instance instance, boolean training) 
    throws Exception {

    FastVector vector = new FastVector();

    if (m_Debug) {
      System.err.println("-- Converting instance");
    }

    // Get the key phrases for the document
    HashMap hashKeyphrases = null;
    HashMap hashKeysEval = null;
    if (!instance.isMissing(m_KeyphrasesAtt)) {
      String keyphrases = instance.stringValue(m_KeyphrasesAtt);
      hashKeyphrases = getGivenKeyphrases(keyphrases, false);
      hashKeysEval = getGivenKeyphrases(keyphrases, true);
    }

    // Get the phrases for the document
    HashMap hash = new HashMap();
    int length = getPhrases(hash, instance.stringValue(m_DocumentAtt));

    // Compute number of extra attributes
    int numFeatures = 5;
    if (m_Debug) {
      if (m_KFused) {
	numFeatures = numFeatures + 1;
      }
    } 

    // Set indices of key attributes
    int phraseAttIndex = m_DocumentAtt;
    int tfidfAttIndex = m_DocumentAtt + 2;
    int distAttIndex = m_DocumentAtt + 3;
    int probsAttIndex = m_DocumentAtt + numFeatures - 1;

    // Go through the phrases and convert them into instances
    Iterator it = hash.keySet().iterator();
    while (it.hasNext()) {
      String phrase = (String)it.next();
      FastVector phraseInfo = (FastVector)hash.get(phrase);
      double[] vals =  featVals(phrase, phraseInfo, training,
				hashKeysEval, hashKeyphrases, length);
      Instance inst = new Instance(instance.weight(), vals);
      inst.setDataset(m_ClassifierData);

      // Get probability of phrase being key phrase
      double[] probs = m_Classifier.distributionForInstance(inst);
      double prob = probs[1];

      // Compute attribute values for final instance
      double[] newInst = 
	new double[instance.numAttributes() + numFeatures];
      int pos = 0;
      for (int i = 0; i < instance.numAttributes(); i++) {
	if (i == m_DocumentAtt) {
	    
	  // Add phrase
	  int index = outputFormatPeek().attribute(pos).
	    addStringValue(phrase);
	  newInst[pos++] = index;
	  
	  // Add original version
	  index = outputFormatPeek().attribute(pos).
	    addStringValue((String)phraseInfo.elementAt(2));
	  newInst[pos++] = index;

	  // Add TFxIDF
	  newInst[pos++] = inst.value(m_TfidfIndex);
	    
	  // Add distance
	  newInst[pos++] = inst.value(m_FirstOccurIndex);

	    // Add other features
	  if (m_Debug) {
	    if (m_KFused) {
	      newInst[pos++] = inst.value(m_KeyFreqIndex);
	    }
	  }

	  // Add probability 
	  probsAttIndex = pos;
	  newInst[pos++] = prob;

	  // Set rank to missing (computed below)
	  newInst[pos++] = Instance.missingValue();
	} else if (i == m_KeyphrasesAtt) {
	  newInst[pos++] = inst.classValue();
	} else {
	  newInst[pos++] = instance.value(i);
	}
      }
      Instance ins = new Instance(instance.weight(), newInst);
      ins.setDataset(outputFormatPeek());
      vector.addElement(ins);
    }

    // Add dummy instances for keyphrases that don't occur
    // in the document
    if (hashKeysEval != null) {
      Iterator phrases = hashKeysEval.keySet().iterator();
      while (phrases.hasNext()) {
	String phrase = (String)phrases.next();
	double[] newInst = 
	  new double[instance.numAttributes() + numFeatures];
	int pos = 0;
	for (int i = 0; i < instance.numAttributes(); i++) {
	  if (i == m_DocumentAtt) {
	    
	    // Add phrase
	    int index = outputFormatPeek().attribute(pos).
	      addStringValue(phrase);
	    newInst[pos++] = (double)index;
	    
	    // Add original version
	    index = outputFormatPeek().attribute(pos).
	      addStringValue((String)hashKeysEval.get(phrase));
	    newInst[pos++] = (double)index;
	    
	    // Add TFxIDF
	    newInst[pos++] = Instance.missingValue();
	    
	    // Add distance
	    newInst[pos++] = Instance.missingValue();
	      
	    // Add other features
	    if (m_Debug) {
	      if (m_KFused) {
		newInst[pos++] = Instance.missingValue();
	      }
	    }

	    // Add probability and rank
	    newInst[pos++] = -Double.MAX_VALUE;
	    newInst[pos++] = Instance.missingValue();
	  } else if (i == m_KeyphrasesAtt) {
	    newInst[pos++] = 1; // Keyphrase
	  } else {
	    newInst[pos++] = instance.value(i);
	  } 
	}
	Instance inst = new Instance(instance.weight(), newInst);
	inst.setDataset(outputFormatPeek());
	vector.addElement(inst);
      }
    }

    // Sort phrases according to their distance (stable sort)
    double[] vals = new double[vector.size()];
    for (int i = 0; i < vals.length; i++) {
      vals[i] = ((Instance)vector.elementAt(i)).value(distAttIndex);
    }
    FastVector newVector = new FastVector(vector.size());
    int[] sortedIndices = Utils.stableSort(vals);
    for (int i = 0; i < vals.length; i++) {
      newVector.addElement(vector.elementAt(sortedIndices[i]));
    }
    vector = newVector;

    // Sort phrases according to their tfxidf value (stable sort)
    for (int i = 0; i < vals.length; i++) {
      vals[i] = -((Instance)vector.elementAt(i)).value(tfidfAttIndex);
    }
    newVector = new FastVector(vector.size());
    sortedIndices = Utils.stableSort(vals);
    for (int i = 0; i < vals.length; i++) {
      newVector.addElement(vector.elementAt(sortedIndices[i]));
    }
    vector = newVector;

    // Sort phrases according to their probability (stable sort)
    for (int i = 0; i < vals.length; i++) {
      vals[i] = 1 - ((Instance)vector.elementAt(i)).value(probsAttIndex);
    }
    newVector = new FastVector(vector.size());
    sortedIndices = Utils.stableSort(vals);
    for (int i = 0; i < vals.length; i++) {
      newVector.addElement(vector.elementAt(sortedIndices[i]));
    }
    vector = newVector;

    // Compute rank of phrases. Check for subphrases that are ranked
    // lower than superphrases and assign probability -1 and set the
    // rank to Integer.MAX_VALUE
    int rank = 1;
    for (int i = 0; i < vals.length; i++) {
      Instance currentInstance = (Instance)vector.elementAt(i);

      // Short cut: if phrase very unlikely make rank very low and continue
      if (Utils.grOrEq(vals[i], 1.0)) {
	currentInstance.setValue(probsAttIndex + 1, Integer.MAX_VALUE);
	continue;
      }

      // Otherwise look for super phrase starting with first phrase
      // in list that has same probability, TFxIDF value, and distance as
      // current phrase. We do this to catch all superphrases
      // that have same probability, TFxIDF value and distance as current phrase.
      int startInd = i;
      while (startInd < vals.length) {
	Instance inst = (Instance)vector.elementAt(startInd);
	if ((inst.value(tfidfAttIndex) != 
	     currentInstance.value(tfidfAttIndex)) ||
	    (inst.value(probsAttIndex) != 
	     currentInstance.value(probsAttIndex)) ||
	    (inst.value(distAttIndex) !=
	     currentInstance.value(distAttIndex))) {
	  break;
	}
	startInd++;
      }
      String val = currentInstance.stringValue(phraseAttIndex);
      boolean foundSuperphrase = false;
      for (int j = startInd - 1; j >= 0; j--) {
	if (j != i) {
	  Instance candidate = (Instance)vector.elementAt(j);
	  String potSuperphrase = candidate.stringValue(phraseAttIndex);
	  if (val.length() <= potSuperphrase.length()) {
	    if (KEAFilter.contains(val, potSuperphrase)) {
	      foundSuperphrase = true;
	      break;
	    }
	  }
	}
      }
      if (foundSuperphrase) {
	currentInstance.setValue(probsAttIndex + 1, Integer.MAX_VALUE);
      } else {
	currentInstance.setValue(probsAttIndex + 1, rank++);
      }
    }
    return vector;
  }
    
  /** 
   * Checks whether one phrase is a subphrase of another phrase.
   */
  private static boolean contains(String sub, String sup) {

    int i = 0;
    while (i + sub.length() - 1 < sup.length()) {
      int j;
      for (j = 0; j < sub.length(); j++) {
	if (sub.charAt(j) != sup.charAt(i + j)) {
	  break;
	}
      }
      if (j == sub.length()) {
	if ((i + j) < sup.length()) {
	  if (sup.charAt(i + j) == ' ') {
	    return true;
	  } else {
	    return false;
	  }
	} else {
	  return true;
	}
      }

      // Skip forward to next space
      do {
	i++;
      } while ((i < sup.length()) && (sup.charAt(i) != ' '));
      i++;
    }
    return false;
  }

  /**
   * Returns a hashtable. Fills the hashtable
   * with the stemmed n-grams occuring in the given string
   * (as keys) and the number of times it occurs.
   */
  private HashMap getPhrasesForDictionary(String str) {

    String[] buffer = new String[m_MaxPhraseLength];
    HashMap hash = new HashMap();

    StringTokenizer tok = new StringTokenizer(str, "\n");
    while (tok.hasMoreTokens()) {
      String phrase = tok.nextToken();
      int numSeen = 0;
      StringTokenizer wordTok = new StringTokenizer(phrase, " ");
      while (wordTok.hasMoreTokens()) {
	String word = wordTok.nextToken();
	
	// Store word in buffer
	for (int i = 0; i < m_MaxPhraseLength - 1; i++) {
	  buffer[i] = buffer[i + 1];
	}
	buffer[m_MaxPhraseLength - 1] = word;
	
	// How many are buffered?
	numSeen++;
	if (numSeen > m_MaxPhraseLength) {
	  numSeen = m_MaxPhraseLength;
	}
	
	// Don't consider phrases that end with a stop word
	if (m_Stopwords.isStopword(buffer[m_MaxPhraseLength - 1])) {
	  continue;
	}
	
	// Loop through buffer and add phrases to hashtable
	StringBuffer phraseBuffer = new StringBuffer();
	for (int i = 1; i <= numSeen; i++) {
	  if (i > 1) {
	    phraseBuffer.insert(0, ' ');
	  }
	  phraseBuffer.insert(0, buffer[m_MaxPhraseLength - i]);
	  
	  // Don't consider phrases that begin with a stop word
	  if ((i > 1) && 
	      (m_Stopwords.isStopword(buffer[m_MaxPhraseLength - i]))) {
	    continue;
	  }
	  
	  // Only consider phrases with minimum length
	  if (i >= m_MinPhraseLength) {
	    
	    // Stem string
	    String orig = phraseBuffer.toString();
	    String internal = internalFormat(orig);
	    Counter count = (Counter)hash.get(internal);
	    if (count == null) {
	      hash.put(internal, new Counter());
	    } else {
	      count.increment();
	    }
	    
	    // Add components if phrase is single word before
	    // conversion into internal format (i.e. error-correcting)
	    /*if ((orig.indexOf(' ') == -1) &&
	      (internal.indexOf(' ') != -1)) {
	      StringTokenizer tokW = new StringTokenizer(internal, " ");
	      while (tokW.hasMoreTokens()) {
	      String comp = (String)tokW.nextToken();
	      Counter countW = (Counter)hash.get(comp);
	      if (countW == null) {
	      hash.put(comp, new Counter());
	      } else {
	      countW.increment();
	      }
	      }
	      }*/
	  }
	}
      }
    }
    return hash;
  }

  /**
   * Expects an empty hashtable. Fills the hashtable
   * with the stemmed n-grams occuring in the given string
   * (as keys). Stores the position, the number of occurences,
   * and the most commonly occurring orgininal version of
   * each n-gram.
   *
   * N-grams that occur less than m_MinNumOccur are not used.
   *
   * Returns the total number of words (!) in the string.
   */
  private int getPhrases(HashMap hash, String str) {

    String[] buffer = new String[m_MaxPhraseLength];
    
    StringTokenizer tok = new StringTokenizer(str, "\n");
    int pos = 1; 
    while (tok.hasMoreTokens()) {
      String phrase = tok.nextToken();
      int numSeen = 0;
      StringTokenizer wordTok = new StringTokenizer(phrase, " ");
      while (wordTok.hasMoreTokens()) {
	String word = wordTok.nextToken();
	
	// Store word in buffer
	for (int i = 0; i < m_MaxPhraseLength - 1; i++) {
	  buffer[i] = buffer[i + 1];
	}
	buffer[m_MaxPhraseLength - 1] = word;
	
	// How many are buffered?
	numSeen++;
	if (numSeen > m_MaxPhraseLength) {
	  numSeen = m_MaxPhraseLength;
	}
	
	// Don't consider phrases that end with a stop word
	if (m_Stopwords.isStopword(buffer[m_MaxPhraseLength - 1])) {
	  pos++;
	  continue;
	}
	
	// Loop through buffer and add phrases to hashtable
	StringBuffer phraseBuffer = new StringBuffer();
	for (int i = 1; i <= numSeen; i++) {
	  if (i > 1) {
	    phraseBuffer.insert(0, ' ');
	  }
	  phraseBuffer.insert(0, buffer[m_MaxPhraseLength - i]);
	  
	  // Don't consider phrases that begin with a stop word
	  if ((i > 1) && 
	      (m_Stopwords.isStopword(buffer[m_MaxPhraseLength - i]))) {
	    continue;
	  }
	  
	  // Only consider phrases with minimum length
	  if (i >= m_MinPhraseLength) {
	    
	    // Stem string
	    String phrStr = phraseBuffer.toString();
	    String internal = internalFormat(phrStr);
	    FastVector vec = (FastVector)hash.get(internal);
	    if (vec == null) {
	      vec = new FastVector(3);
	      
	      // HashMap for storing all versions
	      HashMap secHash = new HashMap();
	      secHash.put(phrStr, new Counter());
	      
	      // Update hashtable with all the info
	      vec.addElement(new Counter(pos + 1 - i));
	      vec.addElement(new Counter());
	      vec.addElement(secHash);
	      hash.put(internal, vec);
	    } else {
	      
	      // Update number of occurrences
	      ((Counter)((FastVector)vec).elementAt(1)).increment();
	      
	      // Update hashtable storing different versions
	      HashMap secHash = (HashMap)vec.elementAt(2);
	      Counter count = (Counter)secHash.get(phrStr);
	      if (count == null) {
		secHash.put(phrStr, new Counter());
	      } else {
		count.increment();
	      }
	    }
	  }
	}
	pos++;
      }
    }
    
    // Replace secondary hashtables with most commonly occurring
    // version of each phrase (canonical) form. Delete all words
    // that are proper nouns.
    Iterator phrases = hash.keySet().iterator();
    while (phrases.hasNext()) {
      String phrase = (String)phrases.next();
      FastVector info = (FastVector)hash.get(phrase);

      // Occurring less than m_MinNumOccur?
      if (((Counter)((FastVector)info).elementAt(1)).value() < m_MinNumOccur) {
	phrases.remove();
	continue;
      }

      // Get canonical form
      String canForm = canonicalForm((HashMap)info.elementAt(2));
      if (canForm == null) {
	phrases.remove();
      } else {
	info.setElementAt(canForm, 2);
      }
    }
    return pos;
  }

  /**
   * Create canonical form of phrase.
   */
  private String canonicalForm(HashMap secHash) {

    int max = 0; String bestVersion = null;
    boolean allFullyHyphenated = true;
    int num = 0;
    Iterator versions = secHash.keySet().iterator();
    while (versions.hasNext()) {
      num++;
      String version = (String)versions.next();

      // Are all the words joined up?
      if (!isFullyHyphenated(version)) {
	allFullyHyphenated = false;
      }

      // Check for how often this version occurs
      Counter count = (Counter)secHash.get(version);
      if (count.value() > max) {
	max = count.value();
	bestVersion = version;
      }
    }
    if ((getCheckForProperNouns()) && (num == 1) && properNoun(bestVersion)) {
    //if (allProperNouns) {
      return null;
    } else {
      if (isFullyHyphenated(bestVersion) &&
	  !allFullyHyphenated) {
	bestVersion = bestVersion.replace('-', ' ');
      }
      return bestVersion;
    }
  }

  /**
   * Checks whether the given phrase is
   * fully hyphenated.
   */
  private boolean isFullyHyphenated(String str) {
    
    return (str.indexOf(' ') == -1);
  }

  /**
   * Checks whether the given string is a 
   * proper noun.
   *
   * @return true if it is a potential proper noun
   */
  private static boolean properNoun(String str) {

    // Is it more than one word?
    if (str.indexOf(' ') != -1) {
      return false;
    }

    // Does it start with an upper-case character?
    if (Character.isLowerCase(str.charAt(0))) {
      return false;
    } 

    // Is there at least one character that's
    // not upper-case?
    for (int i = 1; i < str.length(); i++) {
      /*if (Character.isUpperCase(str.charAt(i))) {
	return false;
	}*/
      if (!Character.isUpperCase(str.charAt(i))) {
	return true;
      }
    }
    //return true;
    return false;
  }

  /**
   * Generates the evaluation format of a phrase.
   */
  private String evalFormat(String str) {
    
    return m_Stemmer.stemString(str);
  }

  /** 
   * Generates the internal format of a phrase.
   */
  private String internalFormat(String str) {

    // Remove some non-alphanumeric characters
    str = str.replace('-', ' ');
    str = str.replace('/', ' ');
    str = str.replace('&', ' ');

    // Stem string
    return m_Stemmer.stemString(str);
  }

  /**
   * Gets all the phrases in the given string and puts them into the
   * hashtable.  Also stores the original version of the stemmed
   * phrase in the hash table.  
   */
  private HashMap getGivenKeyphrases(String str,
				       boolean forEval) {

    FastVector vector = new FastVector();
    HashMap hash = new HashMap();
    
    StringTokenizer tok = new StringTokenizer(str, "\n");
    while (tok.hasMoreTokens()) {
      String orig = tok.nextToken();
      orig = orig.trim();
      if (orig.length() > 0) {
	String modified;
	if (!forEval) {
	  modified = internalFormat(orig);
	} else {
	  modified = evalFormat(orig);
	}
	if (!hash.containsKey(modified)) {
	  hash.put(modified, orig);
	} else {
	  if (forEval) {
	    System.err.println("WARNING: stem of author-assigned keyphrase " +
			       orig + " matches existing stem (skipping it)!");
	  }
	}
      }
    }
    if (hash.size() == 0) {
      return null;
    } else {
      return hash;
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new KEAFilter(), argv);
      } else {
	Filter.filterFile(new KEAFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}











