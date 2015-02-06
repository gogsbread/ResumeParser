/*
 *    NumbersFilter.java
 *    Copyright (C) 2000 Eibe Frank
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


import weka.core.*;
import weka.filters.*;

import java.util.*;

/**
 * Removes all numbers from all the string attributes in the given
 * dataset. Assumes that words are separated by whitespace.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
@SuppressWarnings({"serial","cast"})
public class NumbersFilter extends Filter {

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
      return "Removes all numbers from all the string attributes in " +
      "the given dataset. Assumes that words are separated by whitespace.";
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
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new NumbersFilter(), argv);
      } else {
	Filter.filterFile(new NumbersFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
 
  /** 
   * Converts an instance. A phrase boundary is inserted where
   * a number is found.
   */
  private void convertInstance(Instance instance) throws Exception {
  
    double[] instVals = new double[instance.numAttributes()];

    for (int i = 0; i < instance.numAttributes(); i++) {
      if ((!instance.attribute(i).isString()) || instance.isMissing(i)) {
	instVals[i] = instance.value(i);
      } else {
	String str = instance.stringValue(i);
	StringBuffer resultStr = new StringBuffer();
	StringTokenizer tok = new StringTokenizer(str, " \t\n", true);
	while (tok.hasMoreTokens()) {
	  String token = tok.nextToken();

	  // Everything that doesn't contain at least
	  // one letter is considered to be a number
	  boolean isNumber = true;
	  for (int j = 0; j < token.length(); j++) {
	    if (Character.isLetter(token.charAt(j))) {
	      isNumber = false;
	      break;
	    }
	  }
	  if (!isNumber) {
	    resultStr.append(token);
	  } else {
	    if (token.equals(" ") || token.equals("\t") ||
		token.equals("\n")) {
	      resultStr.append(token);
	    } else {
	      resultStr.append(" \n ");
	    }
	  }
	}
	int index = getOutputFormat().attribute(i).addStringValue(resultStr.toString());
	instVals[i] = (double)index;
      }
    }
    Instance inst = new Instance(instance.weight(), instVals);
    inst.setDataset(getOutputFormat());
    push(inst);
  }
}











