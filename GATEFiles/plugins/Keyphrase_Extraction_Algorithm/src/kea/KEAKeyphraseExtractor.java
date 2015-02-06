/*
 *    KEAKeyphraseExtractor.java
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

import gate.util.BomStrippingInputStreamReader;

import java.io.*;
import java.util.*;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.FastVector;
import weka.core.Option;

/**
 * Extracts keyphrases from the documents in a given directory.
 * Assumes that the file names for the documents end with ".txt".
 * Puts extracted keyphrases into corresponding files ending with
 * ".key" (if those are not already present). Optionally an encoding
 * for the documents/keyphrases can be defined (e.g. for Chinese
 * text). Documents for which ".key" exists, are used for evaluation.
 *
 * Valid options are:<p>
 *
 * -l "directory name"<br>
 * Specifies name of directory.<p>
 *
 * -m "model name"<br>
 * Specifies name of model.<p>
 *
 * -e "encoding"<br>
 * Specifies encoding.<p>
 *
 * -n <br>
 * Specifies number of phrases to be output (default: 5).<p>
 *
 * -d<br>
 * Turns debugging mode on.<p>
 *
 * -a<br>
 * Also write stemmed phrase and score into ".key" file.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
@SuppressWarnings({"rawtypes","unchecked","cast","resource"})
public class KEAKeyphraseExtractor implements OptionHandler {

  /** Name of directory */
  String m_dirName = null;

  /** Name of model */
  String m_modelName = null;

  /** Encoding */
  String m_encoding = "default";

  /** Debugging mode? */
  boolean m_debug = false;

  /** The KEA filter object */
  KEAFilter m_KEAFilter = null;

  /** The number of phrases to extract. */
  int m_numPhrases = 5;

  /** Also write stemmed phrase and score into .key file. */
  boolean m_AdditionalInfo = false;

  /**
   * Get the value of AdditionalInfo.
   *
   * @return Value of AdditionalInfo.
   */
  public boolean getAdditionalInfo() {

    return m_AdditionalInfo;
  }

  /**
   * Set the value of AdditionalInfo.
   *
   * @param newAdditionalInfo Value to assign to AdditionalInfo.
   */
  public void setAdditionalInfo(boolean newAdditionalInfo) {

    m_AdditionalInfo = newAdditionalInfo;
  }

  /**
   * Get the value of numPhrases.
   *
   * @return Value of numPhrases.
   */
  public int getNumPhrases() {

    return m_numPhrases;
  }

  /**
   * Set the value of numPhrases.
   *
   * @param newnumPhrases Value to assign to numPhrases.
   */
  public void setNumPhrases(int newnumPhrases) {

    m_numPhrases = newnumPhrases;
  }

  /**
   * Get the value of debug.
   *
   * @return Value of debug.
   */
  public boolean getDebug() {

    return m_debug;
  }

  /**
   * Set the value of debug.
   *
   * @param newdebug Value to assign to debug.
   */
  public void setDebug(boolean newdebug) {

    m_debug = newdebug;
  }

  /**
   * Get the value of encoding.
   *
   * @return Value of encoding.
   */
  public String getEncoding() {

    return m_encoding;
  }

  /**
   * Set the value of encoding.
   *
   * @param newencoding Value to assign to encoding.
   */
  public void setEncoding(String newencoding) {

    m_encoding = newencoding;
  }

  /**
   * Get the value of modelName.
   *
   * @return Value of modelName.
   */
  public String getModelName() {

    return m_modelName;
  }

  /**
   * Set the value of modelName.
   *
   * @param newmodelName Value to assign to modelName.
   */
  public void setModelName(String newmodelName) {

    m_modelName = newmodelName;
  }

  /**
   * Get the value of dirName.
   *
   * @return Value of dirName.
   */
  public String getDirName() {

    return m_dirName;
  }

  /**
   * Set the value of dirName.
   *
   * @param newdirName Value to assign to dirName.
   */
  public void setDirName(String newdirName) {

    m_dirName = newdirName;
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -l "directory name"<br>
   * Specifies name of directory.<p>
   *
   * -m "model name"<br>
   * Specifies name of model.<p>
   *
   * -e "encoding"<br>
   * Specifies encoding.<p>
   *
   * -n<br>
   * Specifies number of phrases to be output (default: 5).<p>
   *
   * -d<br>
   * Turns debugging mode on.<p>
   *
   * -a<br>
   * Also write stemmed phrase and score into ".key" file.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String dirName = Utils.getOption('l', options);
    if (dirName.length() > 0) {
      setDirName(dirName);
    } else {
      setDirName(null);
      throw new Exception("Name of directory required argument.");
    }
    String modelName = Utils.getOption('m', options);
    if (modelName.length() > 0) {
      setModelName(modelName);
    } else {
      setModelName(null);
      throw new Exception("Name of model required argument.");
    }
    String encoding = Utils.getOption('e', options);
    if (encoding.length() > 0) {
      setEncoding(encoding);
    } else {
      setEncoding("default");
    }
    String numPhrases = Utils.getOption('n', options);
    if (numPhrases.length() > 0) {
      setNumPhrases(Integer.parseInt(numPhrases));
    } else {
      setNumPhrases(5);
    }
    setDebug(Utils.getFlag('d', options));
    setAdditionalInfo(Utils.getFlag('a', options));
    Utils.checkForRemainingOptions(options);
   }

  /**
   * Gets the current option settings.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;

    options[current++] = "-l";
    options[current++] = "" + (getDirName());
    options[current++] = "-m";
    options[current++] = "" + (getModelName());
    options[current++] = "-e";
    options[current++] = "" + (getEncoding());
    options[current++] = "-n";
    options[current++] = "" + (getNumPhrases());
    if (getDebug()) {
      options[current++] = "-d";
    }
    if (getAdditionalInfo()) {
      options[current++] = "-a";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	      "\tSpecifies name of directory.",
              "l", 1, "-l <directory name>"));
    newVector.addElement(new Option(
	      "\tSpecifies name of model.",
              "m", 1, "-m <model name>"));
    newVector.addElement(new Option(
	      "\tSpecifies encoding.",
              "e", 1, "-e <encoding>"));
    newVector.addElement(new Option(
	      "\tSpecifies number of phrases to be output (default: 5).",
              "n", 1, "-n"));
    newVector.addElement(new Option(
	      "\tTurns debugging mode on.",
              "d", 0, "-d"));
    newVector.addElement(new Option(
	      "\tAlso write stemmed phrase and score into \".key\" file.",
              "a", 0, "-a"));

    return newVector.elements();
  }

  /**
   * Collects the stems of the file names.
   */
  public Hashtable collectStems() throws Exception {

    Hashtable stems = new Hashtable();

    try {
      File dir = new File(m_dirName);
      String[] files = dir.list();
      for (int i = 0; i < files.length; i++) {
	if (files[i].endsWith(".txt")) {
	  String stem = files[i].substring(0, files[i].length() - 4);
	  if (!stems.containsKey(stem)) {
	    stems.put(stem, new Double(0));
	  }
	}
      }
    } catch (Exception e) {
      throw new Exception("Problem opening directory " + m_dirName);
    }
    return stems;
  }

  /**
   * Builds the model from the files
   */
  public void extractKeyphrases(Hashtable stems) throws Exception {

    Vector stats = new Vector();

    // Check whether there is actually any data
    if (stems.size() == 0) {
      throw new Exception("Couldn't find any data!");
    }

    FastVector atts = new FastVector(2);
    atts.addElement(new Attribute("doc", (FastVector)null));
    atts.addElement(new Attribute("keyphrases", (FastVector)null));
    Instances data = new Instances("keyphrase_training_data", atts, 0);

    // Extract keyphrases
    Enumeration elem = stems.keys();
    while (elem.hasMoreElements()) {
      String str = (String)elem.nextElement();
      double[] newInst = new double[2];
      try {
	File txt = new File(m_dirName + "/" + str + ".txt");
	Reader is;
	if (!m_encoding.equals("default")) {
	  is = new BomStrippingInputStreamReader(new FileInputStream(txt), m_encoding);
	} else {
	  is = new BomStrippingInputStreamReader(new FileInputStream(txt));
	}
	StringBuffer txtStr = new StringBuffer();
	int c;
	while ((c = is.read()) != -1) {
	  txtStr.append((char)c);
	}
	newInst[0] = (double)data.attribute(0).addStringValue(txtStr.toString());
      } catch (Exception e) {
	if (m_debug) {
	  System.err.println("Can't read document " + str + ".txt");
	}
	newInst[0] = Instance.missingValue();
      }
      try {
	File key = new File(m_dirName + "/" + str + ".key");
	Reader is;
	if (!m_encoding.equals("default")) {
	  is = new BomStrippingInputStreamReader(new FileInputStream(key), m_encoding);
	} else {
	  is = new BomStrippingInputStreamReader(new FileInputStream(key));
	}
	StringBuffer keyStr = new StringBuffer();
	int c;
	while ((c = is.read()) != -1) {
	  keyStr.append((char)c);
	}
	newInst[1] = (double)data.attribute(1).addStringValue(keyStr.toString());
      } catch (Exception e) {
	if (m_debug) {
	  System.err.println("No keyphrases for stem " + str + ".");
	}
	newInst[1] = Instance.missingValue();
      }
      data.add(new Instance(1.0, newInst));
      m_KEAFilter.input(data.instance(0));
      data = data.stringFreeStructure();
      if (m_debug) {
	System.err.println("-- Document: " + str);
      }
      Instance[] topRankedInstances = new Instance[m_numPhrases];
      Instance inst;
      while ((inst = m_KEAFilter.output()) != null) {
	int index = (int)inst.value(m_KEAFilter.getRankIndex()) - 1;
	if (index < m_numPhrases) {
	  topRankedInstances[index] = inst;
	}
      }
      if (m_debug) {
	System.err.println("-- Keyphrases and feature values:");
      }
      FileOutputStream out = null;
      PrintWriter printer = null;
      File key = new File(m_dirName + "/" + str + ".key");
      if (!key.exists()) {
	out = new FileOutputStream(m_dirName + "/" + str + ".key");
	if (!m_encoding.equals("default")) {
	  printer = new PrintWriter(new OutputStreamWriter(out, m_encoding));
	} else {
	  printer = new PrintWriter(out);
	}
      }
      double numExtracted = 0, numCorrect = 0;
      for (int i = 0; i < m_numPhrases; i++) {
	if (topRankedInstances[i] != null) {
	  if (!topRankedInstances[i].
	      isMissing(topRankedInstances[i].numAttributes() - 1)) {
	    numExtracted += 1.0;
	  }
	  if ((int)topRankedInstances[i].
	      value(topRankedInstances[i].numAttributes() - 1) ==
	      topRankedInstances[i].
	      attribute(topRankedInstances[i].numAttributes() - 1).
	      indexOfValue("True")) {
	    numCorrect += 1.0;
	  }
	  if (printer != null) {
	    printer.print(topRankedInstances[i].
			  stringValue(m_KEAFilter.getUnstemmedPhraseIndex()));
	    if (m_AdditionalInfo) {
	      printer.print("\t");
	      printer.print(topRankedInstances[i].
			  stringValue(m_KEAFilter.getStemmedPhraseIndex()));
	      printer.print("\t");
	      printer.print(Utils.
			    doubleToString(topRankedInstances[i].
					   value(m_KEAFilter.
						 getProbabilityIndex()), 4));
	    }
	    printer.println();
	  }
	  if (m_debug) {
	    System.err.println(topRankedInstances[i]);
	  }
	}
      }
      if (numExtracted > 0) {
	if (m_debug) {
	  System.err.println("-- " + numCorrect + " correct");
	}
	stats.addElement(new Double(numCorrect));
      }
      if (printer != null) {
	printer.flush();
	printer.close();
	out.close();
      }
    }
    double[] st = new double[stats.size()];
    for (int i = 0; i < stats.size(); i++) {
      st[i] = ((Double)stats.elementAt(i)).doubleValue();
    }
    double avg = Utils.mean(st);
    double stdDev = Math.sqrt(Utils.variance(st));
    System.err.println("Avg. number of correct keyphrases: " +
		       Utils.doubleToString(avg, 2) + " +/- " +
		       Utils.doubleToString(stdDev, 2));
    System.err.println("Based on " + stats.size() + " documents");
    m_KEAFilter.batchFinished();
  }

  /**
   * Loads the extraction model from the file.
   */
  public void loadModel() throws Exception {

    BufferedInputStream inStream =
      new BufferedInputStream(new FileInputStream(m_modelName));
    ObjectInputStream in = new ObjectInputStream(inStream);
    m_KEAFilter = (KEAFilter)in.readObject();
    in.close();
  }

  /**
   * The main method.
   */
  public static void main(String[] ops) {

    KEAKeyphraseExtractor kmb = new KEAKeyphraseExtractor();
    try {
      kmb.setOptions(ops);
      System.err.print("Extracting keyphrases with options: ");
      String[] optionSettings = kmb.getOptions();
      for (int i = 0; i < optionSettings.length; i++) {
	System.err.print(optionSettings[i] + " ");
      }
      System.err.println();
      kmb.loadModel();
      kmb.extractKeyphrases(kmb.collectStems());
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
      System.err.println("\nOptions:\n");
      Enumeration enumeration = kmb.listOptions();
      while (enumeration.hasMoreElements()) {
	Option option = (Option) enumeration.nextElement();
	System.err.println(option.synopsis());
	System.err.println(option.description());
      }
    }
  }
}

