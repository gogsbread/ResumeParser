/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 17/05/2002
 *
 *  $Id: LuceneIREngine.java 17841 2014-04-16 12:35:41Z markagreenwood $
 *
 */
package gate.creole.ir.lucene;

import gate.Gate;
import gate.creole.AbstractResource;
import gate.creole.ir.IREngine;
import gate.creole.ir.IndexManager;
import gate.creole.ir.Search;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;

/**
 * The lucene IR engine.
 * Packages a {@link LuceneIndexManager} and a {@link LuceneSearch}.
 */
@CreoleResource(name = "Lucene IR Engine", tool = true, autoinstances = @AutoInstance)
public class LuceneIREngine extends AbstractResource implements IREngine{

  private static final long serialVersionUID = -152880506664125169L;
  
  static {
    try {
      Gate.registerIREngine(LuceneIREngine.class.getName());
    }
    catch(Exception cnfe) {
      throw new RuntimeException(cnfe);
    }
  }

  public LuceneIREngine() {
    search = new LuceneSearch();
    indexManager = new LuceneIndexManager();
  }

  @Override
  public Search getSearch() {
    return search;
  }

  @Override
  public IndexManager getIndexmanager() {
    return indexManager;
  }

  @Override
  public String getName(){
    return "Lucene IR engine";
  }

  Search search;
  IndexManager indexManager;

}