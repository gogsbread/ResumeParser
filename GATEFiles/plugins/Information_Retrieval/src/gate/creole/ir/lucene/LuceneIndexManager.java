/*
 *  LuceneIndexManager.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gaI enjoy seeing
the occasional update on Facebook :te.ac.uk/gate/licence.html).
 *
 *  Rosen Marinov, 19/Apr/2002
 *
 */

package gate.creole.ir.lucene;

import gate.Corpus;
import gate.Document;
import gate.creole.ir.IndexDefinition;
import gate.creole.ir.IndexException;
import gate.creole.ir.IndexField;
import gate.creole.ir.IndexManager;
import gate.util.GateRuntimeException;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** This class represents Lucene implementation of IndexManeager interface.*/
public class LuceneIndexManager implements IndexManager{

  /** used in Lucene Documents as a key for gate document ID value. */
  public final static String DOCUMENT_ID = "DOCUMENT_ID";

  /** IndexDefinition - location, type, fields, etc.*/
  private IndexDefinition indexDefinition;

  /** An corpus for indexing*/
  private Corpus corpus;

  /* Niraj */
  /** constant that ensures that corpus is indexed with IR plugin */
  public final static String CORPUS_INDEX_FEATURE = "CorpusIndexFeature";
  public final static String CORPUS_INDEX_FEATURE_VALUE = "IR";
  /* End */

  /** Constructor of the class. */
  public LuceneIndexManager(){
  }

  /** Creates index directory and indexing all
   *  documents in the corpus. */
  @Override
  public void createIndex() throws IndexException{
    if(indexDefinition == null)
      throw new GateRuntimeException("Index definition is null!");
    if(corpus == null)
      throw new GateRuntimeException("Corpus is null!");

    String location = indexDefinition.getIndexLocation();
    try {
      File file = new File(location);
      if (file.exists()){
        if (file.isDirectory() && file.listFiles().length>0) {
          throw new IndexException(location+ " is not empty directory");
        }
        if (!file.isDirectory()){
          throw new IndexException("Only empty directory can be index path");
        }
      }

      /* Niraj */
      // ok so lets put the corpus index feature
      corpus.getFeatures().put(CORPUS_INDEX_FEATURE, CORPUS_INDEX_FEATURE_VALUE);
      /* End */

      IndexWriter writer =
              new IndexWriter(FSDirectory.open(new File(location)),
                      new IndexWriterConfig(Version.LUCENE_31,
                              new SimpleAnalyzer(Version.LUCENE_30))
                              .setOpenMode(OpenMode.CREATE));
      
      /*IndexWriter writer = new IndexWriter(
              FSDirectory.open(new File(location)),
              new SimpleAnalyzer(Version.LUCENE_30), 
              true,
              new IndexWriter.MaxFieldLength(IndexWriter.DEFAULT_MAX_FIELD_LENGTH)
              );*/

      for(int i = 0; i<corpus.size(); i++) {
        boolean isLoaded = corpus.isDocumentLoaded(i);
        gate.Document gateDoc = corpus.get(i);
        writer.addDocument(getLuceneDoc(gateDoc));
        if (!isLoaded) {
          corpus.unloadDocument(gateDoc);
          gate.Factory.deleteResource(gateDoc);
        }
      }//for (all documents)

      writer.commit();
      writer.close();
      corpus.sync();
    } catch (java.io.IOException ioe){
      throw new IndexException(ioe.getMessage());
    } catch (gate.persist.PersistenceException pe){
      pe.printStackTrace();
    }
  }

  /** Optimize existing index. */
  @Override
  public void optimizeIndex() throws IndexException{
    if(indexDefinition == null)
      throw new GateRuntimeException("Index definition is null!");
    try {

      IndexWriter writer =
              new IndexWriter(FSDirectory.open(new File(indexDefinition.getIndexLocation())),
                      new IndexWriterConfig(Version.LUCENE_31,
                              new SimpleAnalyzer(Version.LUCENE_30))
                              .setOpenMode(OpenMode.APPEND));
      
      /*IndexWriter writer = new IndexWriter(
              FSDirectory.open(new File(indexDefinition.getIndexLocation())),
              new SimpleAnalyzer(Version.LUCENE_30), 
              false,
              new IndexWriter.MaxFieldLength(IndexWriter.DEFAULT_MAX_FIELD_LENGTH));*/
      
      //writer.optimize();
      writer.forceMerge(1, true);
      
      writer.commit();
      writer.close();
    } catch (java.io.IOException ioe){
      throw new IndexException(ioe.getMessage());
    }
  }

  /** Delete index. */
  @Override
  public void deleteIndex() throws IndexException{
    if(indexDefinition == null)
      throw new GateRuntimeException("Index definition is null!");
    boolean isDeleted = true;
    File dir = new File(indexDefinition.getIndexLocation());
    if (dir.exists() && dir.isDirectory()) {
      File[] files = dir.listFiles();
      for (int i =0; i<files.length; i++){
        File f = files[i];
        isDeleted = f.delete();
      }
    }
    dir.delete();
    if (!isDeleted) {
      throw new IndexException("Can't delete directory"
                               + indexDefinition.getIndexLocation());
    }
  }

  /** Reindexing changed documents, removing removed documents and
   *  add to the index new corpus documents. */
  @Override
  public void sync(List<Document> added, List<String> removedIDs, List<Document> changed) throws IndexException{
    String location = indexDefinition.getIndexLocation();
    try {

      IndexReader reader = IndexReader.open(FSDirectory.open(new File(location)),false);
      
      for (int i = 0; i<removedIDs.size(); i++) {
        String id = removedIDs.get(i).toString();
        org.apache.lucene.index.Term term =
                               new org.apache.lucene.index.Term(DOCUMENT_ID,id);
        reader.deleteDocuments(term);
      }//for (remove all removed documents)

      for (int i = 0; i<changed.size(); i++) {
        gate.Document gateDoc = changed.get(i);
        String id = gateDoc.getLRPersistenceId().toString();
        org.apache.lucene.index.Term term =
                               new org.apache.lucene.index.Term(DOCUMENT_ID,id);
        reader.deleteDocuments(term);
      }//for (remove all changed documents)

      reader.close();

      /*IndexWriter writer = new IndexWriter(
              FSDirectory.open(new File(location)),
              new SimpleAnalyzer(Version.LUCENE_30), 
              false,
              new IndexWriter.MaxFieldLength(IndexWriter.DEFAULT_MAX_FIELD_LENGTH)
              );*/
      IndexWriter writer =
              new IndexWriter(FSDirectory.open(new File(location)),
                      new IndexWriterConfig(Version.LUCENE_31,
                              new SimpleAnalyzer(Version.LUCENE_30))
                              .setOpenMode(OpenMode.APPEND));      

      for(int i = 0; i<added.size(); i++) {
        gate.Document gateDoc = added.get(i);
        writer.addDocument(getLuceneDoc(gateDoc));
      }//for (add all added documents)

      for(int i = 0; i<changed.size(); i++) {
        gate.Document gateDoc = changed.get(i);
        writer.addDocument(getLuceneDoc(gateDoc));
      }//for (add all changed documents)

      writer.close();
    } catch (java.io.IOException ioe) {
      throw new IndexException(ioe.getMessage());
    }
  }

  private org.apache.lucene.document.Document getLuceneDoc(gate.Document gateDoc){
    org.apache.lucene.document.Document luceneDoc =
                                     new org.apache.lucene.document.Document();
    
    Iterator<IndexField> fields = indexDefinition.getIndexFields();

//    luceneDoc.add(Field.Keyword(DOCUMENT_ID,
//                                gateDoc.getLRPersistenceId().toString()));

    // update version of Lucene
    luceneDoc.add(new Field(DOCUMENT_ID,gateDoc.getLRPersistenceId().toString(),Field.Store.YES,Field.Index.NOT_ANALYZED));
    
    while (fields.hasNext()) {
      IndexField field = fields.next();
      String valueForIndexing;

      if (field.getReader() == null){
        valueForIndexing = gateDoc.getFeatures().get(field.getName()).toString();
      } else {
        valueForIndexing = field.getReader().getPropertyValue(gateDoc);
      } //if-else reader or feature

      if (field.isPreseved()) {
        luceneDoc.add(new Field(field.getName(),valueForIndexing,Field.Store.YES,Field.Index.NOT_ANALYZED));
        // luceneDoc.add(Field.Keyword(field.getName(),valueForIndexing));
      } else {
        luceneDoc.add(new Field(field.getName(),valueForIndexing,Field.Store.NO,Field.Index.ANALYZED));
        // luceneDoc.add(Field.UnStored(field.getName(),valueForIndexing));
      } // if-else keyword or text

    }// while (add all fields)

    return luceneDoc;
  }

  @Override
  public Corpus getCorpus() {
    return corpus;
  }
  @Override
  public void setCorpus(Corpus corpus) {
    this.corpus = corpus;
  }
  @Override
  public IndexDefinition getIndexDefinition() {
    return indexDefinition;
  }
  @Override
  public void setIndexDefinition(IndexDefinition indexDefinition) {
    this.indexDefinition = indexDefinition;
  }

}