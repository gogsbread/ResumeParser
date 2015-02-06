/*
 *    Stemmer.java
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

import java.io.Serializable;

/**
 * Abstract class for stemmers.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
@SuppressWarnings("serial")
public abstract class Stemmer implements Serializable {

  /**
   * Iterated stemming of the given word.
   */
  public abstract String stem(String str);

  /**
   * Stems everything in the given string.
   */
  public String stemString(String str) {

    StringBuffer result = new StringBuffer();
    int start = -1;
    for (int j = 0; j < str.length(); j++) {
      char c = str.charAt(j);
      if (Character.isLetterOrDigit(c)) {
	if (start == -1) {
	  start = j;
	}
      } else if (c == '\'') {
	if (start == -1) {
	  result.append(c);
	}
      } else {
	if (start != -1) {
	  result.append(stem(str.substring(start, j)));
	  start = -1;
	}
	result.append(c);
      }
    }
    if (start != -1) {
      result.append(stem(str.substring(start, str.length())));
    }
    return result.toString();  
  }
}
    

