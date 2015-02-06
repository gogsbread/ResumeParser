/*
 *  SearchPR.java
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

package gate.creole.ir;

import java.util.List;

import javax.swing.JOptionPane;

import gate.ProcessingResource;
import gate.Resource;
import gate.creole.*;
import gate.gui.MainFrame;
import gate.Gate;


public class SearchPR extends AbstractProcessingResource
                      implements ProcessingResource{

  private static final long serialVersionUID = 1316224997021873795L;
  
  private IndexedCorpus corpus = null;
  private String query  = null;
  private String searcherClassName = null;
  private QueryResultList resultList = null;
  private int limit = -1;
  private List<String> fieldNames = null;

  private Search searcher = null;

  /** Constructor of the class*/
  public SearchPR(){
  }

   /** Initialise this resource, and return it. */
  @Override
  public Resource init() throws ResourceInstantiationException {
    Resource result = super.init();
    return result;
  }

  /**
   * Reinitialises the processing resource. After calling this method the
   * resource should be in the state it is after calling init.
   * If the resource depends on external resources (such as rules files) then
   * the resource will re-read those resources. If the data used to create
   * the resource has changed since the resource has been created then the
   * resource will change too after calling reInit().
  */
  @Override
  public void reInit() throws ResourceInstantiationException {
    init();
  }

  /**
   * This method runs the coreferencer. It assumes that all the needed parameters
   * are set. If they are not, an exception will be fired.
   */
  @Override
  public void execute() throws ExecutionException {
    if ( corpus == null){
      throw new ExecutionException("Corpus is not initialized");
    }
    if ( query == null){
      throw new ExecutionException("Query is not initialized");
    }
    if ( searcher == null){
      throw new ExecutionException("Searcher is not initialized");
    }

    /* Niraj */
    // we need to check if this is the corpus with the specified feature
    String val = (String) corpus.getFeatures().get(gate.creole.ir.lucene.LuceneIndexManager.CORPUS_INDEX_FEATURE);
    if(!gate.creole.ir.lucene.LuceneIndexManager.CORPUS_INDEX_FEATURE_VALUE.equals(val)) {
      throw new ExecutionException("This corpus was not indexed by the specified IR");
    }
    /* End */


    try {
      if (corpus.getIndexManager() == null){
        MainFrame.unlockGUI();
        JOptionPane.showMessageDialog(MainFrame.getInstance(), "Corpus is not indexed!\n"
                                    +"Please index first this corpus!",
                       "Search Processing", JOptionPane.WARNING_MESSAGE);
        return;
      }

      fireProgressChanged(0);
      resultList = null;
      searcher.setCorpus(corpus);
      resultList = searcher.search(query, limit, fieldNames);
      fireProcessFinished();
    }

    catch (SearchException ie) {
      throw new ExecutionException(ie.getMessage());
    }
    catch (IndexException ie) {
      throw new ExecutionException(ie.getMessage());
    }
  }

  public void setCorpus(IndexedCorpus corpus) {
    this.corpus = corpus;
  }

  public IndexedCorpus getCorpus() {
    return this.corpus;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return this.query;
  }

  public void setSearcherClassName(String name){
    this.searcherClassName = name;
    try {
      // load searcher class through GATE classloader
      searcher = (Search)
        Class.forName(searcherClassName, true, Gate.getClassLoader())
        .newInstance();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  public String getSearcherClassName(){

    return this.searcher.getClass().getName();
  }

  public void setLimit(Integer limit){
    this.limit = limit.intValue();
  }

  public Integer getLimit(){
    return new Integer(this.limit);
  }

  public void setFieldNames(List<String> fieldNames){
    this.fieldNames = fieldNames;
  }

  public List<String> getFieldNames(){
    return this.fieldNames;
  }

  public QueryResultList getResult(){
    return resultList;
  }

  public void setResult(QueryResultList qr){
    throw new UnsupportedOperationException();
  }

}
