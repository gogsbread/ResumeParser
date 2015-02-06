/*
 *  LuceneSearch.java
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

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import gate.creole.ir.*;

/** This class represents Lucene implementation of serching in index. */
public class LuceneSearch implements Search {

  /** Default number of maximum results when no limit is specified
   * in a search method call 
   */
  private static int DEFAULTMAXRESULTS = 1000000;
  
  /** An instance of indexed corpus*/
  private IndexedCorpus indexedCorpus;

  /** Set the indexed corpus resource for searching. */
  @Override
  public void setCorpus(IndexedCorpus ic){
    this.indexedCorpus = ic;
  }

  /** Search in corpus with this query. 
   * Result length is limited by DEFAULTMAXRESULTS */
  @Override
  public QueryResultList search(String query)
                                         throws IndexException, SearchException{
    return search(query, DEFAULTMAXRESULTS);
  }

  /** Search in corpus with this query.
   *  Size of the result list is limited. */
  @Override
  public QueryResultList search(String query, int limit)
                                         throws IndexException, SearchException{
    return search(query, limit, null);
  }

  /** Search in corpus with this query.
   *  In each QueryResult will be added values of theise fields.
   *  Result length is limited by DEFAULTMAXRESULTS. */
  @Override
  public QueryResultList search(String query, List<String> fieldNames)
                                         throws IndexException, SearchException{
    return search(query, DEFAULTMAXRESULTS, fieldNames);
  }

  /** Search in corpus with this query.
   *  In each QueryResult will be added values of these fields.
   *  Result length is limited. */
  @Override
  public QueryResultList search(String query, int limit, List<String> fieldNames)
                                         throws IndexException, SearchException{
    
    List<QueryResult> result = new Vector<QueryResult>();

    try {
      IndexSearcher searcher =
          new IndexSearcher(IndexReader.open(FSDirectory.open(new File(
              indexedCorpus.getIndexDefinition().getIndexLocation())), true));
      QueryParser parser = new QueryParser(
              Version.LUCENE_30,
              "body", 
              new SimpleAnalyzer(Version.LUCENE_30));
      Query luceneQuery = parser.parse(query);

      // JP was for lucene 2.2
      // Hits hits = searcher.search(luceneQuery);
      //int resultlength = hits.length();
      //if (limit>-1) {
      //  resultlength = Math.min(limit,resultlength);
      //}
      TopDocs topDocs = searcher.search(luceneQuery, limit);
      ScoreDoc[] hits = topDocs.scoreDocs;
      int resultlength = hits.length;


      Vector<Term> fieldValues = null;
      for (int i=0; i<resultlength; i++) {

        if (fieldNames != null){
          fieldValues = new Vector<Term>();
          for (int j=0; j<fieldNames.size(); j++){
            fieldValues.add(new Term( 
                    fieldNames.get(j), 
                    searcher.doc(hits[i].doc).get(fieldNames.get(j)))
            );
          }
        }

        result.add(new QueryResult(
                searcher.doc(hits[i].doc).get(LuceneIndexManager.DOCUMENT_ID),
                  hits[i].score,fieldValues));
      }// for (all search hints)

      searcher.close();

      return new QueryResultList(query, indexedCorpus, result);
    }
    catch (java.io.IOException ioe) {
      throw new IndexException(ioe.getMessage());
    }
    catch (org.apache.lucene.queryParser.ParseException pe) {
      throw new SearchException(pe.getMessage());
    }
  }
}