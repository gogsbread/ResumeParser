/*
 *  LuceneIndexStatistics.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Rosen Marinov, 19/Apr/2002
 *
 */

package gate.creole.ir.lucene;

import gate.creole.ir.IndexStatistics;

import java.util.Map;

public class LuceneIndexStatistics implements IndexStatistics {

  public LuceneIndexStatistics(){
  }

  @Override
  public Long getTermCount(){
    //NOT IMPLEMENTED YET
    return null;
  }

  @Override
  public Long getUniqueTermCount(){
    //NOT IMPLEMENTED YET
    return null;
  }

  @Override
  public Long getExhaustivity(Long docID, String fieldName){
    //NOT IMPLEMENTED YET
    return null;
  }

  @Override
  public Long getSpecificity(String term){
    //NOT IMPLEMENTED YET
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Map getTermFrequency(Long docID, String fieldName){
    //NOT IMPLEMENTED YET
    return null;
  }

}