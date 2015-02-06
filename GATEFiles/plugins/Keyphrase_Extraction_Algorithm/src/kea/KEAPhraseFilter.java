/*
 *    KEAPhraseFilter.java
 *    Copyright (C) 2001 Eibe Frank
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
import java.util.Vector;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.Utils;
import weka.filters.Filter;

/**
 * This filter splits the text in selected string
 * attributes into phrases. The resulting
 * string attributes contain these phrases
 * separated by '\n' characters.
 *
 * Phrases are identified according to the
 * following definitions:
 * 
 * A phrase is a sequence of words interrupted
 * only by sequences of whitespace characters,
 * where each sequence of whitespace characters
 * contains at most one '\n'.
 *
 * A word is a sequence of letters or digits
 * that contains at least one letter, with
 * the following exceptions:
 *
 * a) '.', '@', '_', '&', '/', '-' are allowed
 * if surrounded by letters or digits,
 *
 * b) '\'' is allowed if preceeded by a letter
 * or digit,
 * 
 * c) '-', '/' are also allowed if succeeded by
 * whitespace characters followed by another
 * word. In that case the whitespace characters
 * will be deleted.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
@SuppressWarnings({"serial","rawtypes","unchecked","cast"})
public class KEAPhraseFilter extends Filter implements OptionHandler {

  /** Stores which columns to select as a funky range */
  protected Range m_SelectCols = new Range();

  /** Determines whether internal periods are allowed */
  protected boolean m_DisallowInternalPeriods = false;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "This filter splits the text contained " +
      "by the selected string attributes into phrases.";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
              "\tSpecify list of attributes to process. First and last are valid\n"
	      +"\tindexes. (default none)",
              "R", 1, "-R <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tInvert matching sense",
              "V", 0, "-V"));
    newVector.addElement(new Option(
	      "\tDisallow internal periods",
              "P", 0, "-P"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -R index1,index2-index4,...<br>
   * Specify list of attributes to process. First and last are valid indexes.
   * (default none)<p>
   *
   * -V<br>
   * Invert matching sense <p>
   *
   * -P<br>
   * Disallow internal periods <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String list = Utils.getOption('R', options);
    if (list.length() != 0) {
      setAttributeIndices(list);
    }
    setInvertSelection(Utils.getFlag('V', options));

    setDisallowInternalPeriods(Utils.getFlag('P', options));
    
    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [4];
    int current = 0;

    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (getDisallowInternalPeriods()) {
      options[current++] = "-P";
    }
    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
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

    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    m_SelectCols.setUpper(instanceInfo.numAttributes() - 1);

    return true;
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
    convertInstance(instance);
    return true;
  }

  /**
   * Signify that this batch of input to the filter is finished. If
   * the filter requires all instances prior to filtering, output()
   * may now be called to retrieve the filtered instances. Any
   * subsequent instances filtered should be filtered based on setting
   * obtained from the first batch (unless the inputFormat has been
   * re-assigned or new options have been set). This default
   * implementation assumes all instance processing occurs during
   * inputFormat() and input().
   *
   * @return true if there are instances pending output
   * @exception NullPointerException if no input structure has been defined,
   * @exception Exception if there was a problem finishing the batch.
   */
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new NullPointerException("No input instance format defined");
    }
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new KEAPhraseFilter(), argv);
      } else {
	Filter.filterFile(new KEAPhraseFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
 
  /** 
   * Converts an instance by removing all non-alphanumeric characters
   * from its string attribute values.
   */
  private void convertInstance(Instance instance) throws Exception {
  
    double[] instVals = new double[instance.numAttributes()];

    for (int i = 0; i < instance.numAttributes(); i++) {
      if (!instance.attribute(i).isString() || 
	  instance.isMissing(i)) {
	instVals[i] = instance.value(i);
      } else {
	if (!m_SelectCols.isInRange(i)) {
	  int index = getOutputFormat().attribute(i).
	    addStringValue(instance.stringValue(i));
	  instVals[i] = (double)index;
	  continue;
	}
	String str = instance.stringValue(i);
	StringBuffer resultStr = new StringBuffer();
	int j = 0;
	boolean phraseStart = true;
	boolean seenNewLine = false;
	boolean haveSeenHyphen = false;
	boolean haveSeenSlash = false;
	while (j < str.length()) {
	  boolean isWord = false;
	  boolean potNumber = false;
	  int startj = j;
	  while (j < str.length()) {
	    char ch = str.charAt(j);
	    if (Character.isLetterOrDigit(ch)) {
	      potNumber = true;
	      if (Character.isLetter(ch)) {
		isWord = true;
	      }
	      j++;
	    } else if ((!m_DisallowInternalPeriods && (ch == '.')) ||
		       (ch == '@') ||
		       (ch == '_') ||
		       (ch == '&') ||
		       (ch == '/') ||
		       (ch == '-')) {
	      if ((j > 0) && (j  + 1 < str.length()) &&
		  Character.isLetterOrDigit(str.charAt(j - 1)) &&
		  Character.isLetterOrDigit(str.charAt(j + 1))) {
		j++;
	      } else {
		break;
	      }
	    } else if (ch == '\'') {
	      if ((j > 0) &&
		  Character.isLetterOrDigit(str.charAt(j - 1))) {
		j++;
	      } else {
		break;
	      }
	    } else {
	      break;
	    }
	  }
	  if (isWord == true) {
	    if (!phraseStart) {
	      if (haveSeenHyphen) {
		resultStr.append('-');
	      } else if (haveSeenSlash) {
		resultStr.append('/');
	      } else {
		resultStr.append(' ');
	      }
	    }
	    resultStr.append(str.substring(startj, j));
	    if (j == str.length()) {
	      break;
	    }
	    phraseStart = false;
	    seenNewLine = false;
	    haveSeenHyphen = false;
	    haveSeenSlash = false;
	    if (Character.isWhitespace(str.charAt(j))) {
	      if (str.charAt(j) == '\n') {
		seenNewLine = true;
	      } 
	    } else if (str.charAt(j) == '-') {
	      haveSeenHyphen = true;
	    } else if (str.charAt(j) == '/') {
	      haveSeenSlash = true;
	    } else {
	      phraseStart = true;
	      resultStr.append('\n');
	    }
	    j++;
	  } else if (j == str.length()) {
	    break;
	  } else if (str.charAt(j) == '\n') {
	    if (seenNewLine) {
	      if (phraseStart == false) {
		resultStr.append('\n');
		phraseStart = true;
	      }
	    } else if (potNumber) {
	      if (phraseStart == false) {
		phraseStart = true;
		resultStr.append('\n');
	      }
	    }
	    seenNewLine = true;
	    j++;
	  } else if (Character.isWhitespace(str.charAt(j))) {
	    if (potNumber) {
	      if (phraseStart == false) {
		phraseStart = true;
		resultStr.append('\n');
	      }
	    }
	    j++;
	  } else {
	    if (phraseStart == false) {
	      resultStr.append('\n');
	      phraseStart = true;
	    }
	    j++;
	  }
	}
	int index = getOutputFormat().attribute(i).
	  addStringValue(resultStr.toString());
	instVals[i] = (double)index;
      }
    }
    Instance inst = new Instance(instance.weight(), instVals);
    inst.setDataset(getOutputFormat());
    push(inst);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "If set to false, the specified attributes will be processed;"
      + " If set to true, specified attributes won't be processed.";
  }

  /**
   * Get whether the supplied columns are to be processed
   *
   * @return true if the supplied columns won't be processed
   */
  public boolean getInvertSelection() {

    return m_SelectCols.getInvert();
  }

  /**
   * Set whether selected columns should be processed. If true the 
   * selected columns won't be processed.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_SelectCols.setInvert(invert);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String disallowInternalPeriodsTipText() {

    return "If set to false, internal periods are allowed.";
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
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeIndicesTipText() {

    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Get the current range selection.
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_SelectCols.getRanges();
  }

  /**
   * Set which attributes are to be processed
   *
   * @param rangeList a string representing the list of attributes.  Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   */
  public void setAttributeIndices(String rangeList) {

    m_SelectCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be processed
   *
   * @param attributes an array containing indexes of attributes to select.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   */
  public void setAttributeIndicesArray(int [] attributes) {
    
    setAttributeIndices(Range.indicesToRangeList(attributes));
  }
}
