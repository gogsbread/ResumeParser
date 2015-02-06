/*
 *    Counter.java
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

import java.io.*;

/**
 * Class that implements a simple counter.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
@SuppressWarnings("serial")
public class Counter implements Serializable {
  
  /** Integer value stored */
    private int m_val = 1;
  
  /**
   * Initializes the counter to 1
   */
  public Counter() {
    
    m_val = 1;
  }
  
  /**
   * Initializes the counter to the given value
   */
  public Counter(int val) {
    
    m_val = val;
  }
  
  /**
   * Increments the counter.
   */
  public void increment() {
    
    m_val++;
  }
  
  /**
   * Gets the value.
   * @return the value
   */
  public int value() {
    
    return m_val;
  }
  
  /**
   * Returns string containing value.
   */
  public String toString() {
    
    return String.valueOf(m_val);
  }
}
