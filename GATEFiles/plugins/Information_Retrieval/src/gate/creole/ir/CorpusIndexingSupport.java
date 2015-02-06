/*
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Mark A. Greenwood 16/04/2014
 *
 */

package gate.creole.ir;

import gate.GateConstants;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.event.CreoleEvent;
import gate.gui.CreateIndexGUI;
import gate.gui.NameBearerHandle;
import gate.gui.OkCancelDialog;
import gate.gui.ResourceHelper;
import gate.persist.LuceneDataStoreImpl;
import gate.util.Err;

import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

@CreoleResource(name = "Corpus Indexing Support", tool = true, autoinstances = @AutoInstance)
public class CorpusIndexingSupport extends ResourceHelper {

  private static final long serialVersionUID = -968628391030653008L;

  @Override
  public void resourceLoaded(CreoleEvent e) {
    super.resourceLoaded(e);
    
    if (e.getResource() instanceof IndexedCorpus) {
      IndexedCorpus ic = (IndexedCorpus)e.getResource();
      
      IndexDefinition definition = (IndexDefinition)ic.getFeatures().get(GateConstants.CORPUS_INDEX_DEFINITION_FEATURE_KEY);
      if(definition != null) {
        ic.setIndexDefinition(definition);
      }
    }
  }
  
  @Override
  protected List<Action> buildActions(NameBearerHandle handle) {
    List<Action> actions = new ArrayList<Action>();
    
    if(handle.getTarget() instanceof IndexedCorpus) {
      IndexedCorpus ic = (IndexedCorpus)handle.getTarget();
      if(ic.getDataStore() != null
              && ic.getDataStore() instanceof LuceneDataStoreImpl) {
        // do nothing
      }
      else {
        actions.add(new CreateIndexAction(handle));
        actions.add(new OptimizeIndexAction(handle));
        actions.add(new DeleteIndexAction(handle));
      }
    }
    
    return actions;
  }
  
  class CreateIndexAction extends AbstractAction {
    private static final long serialVersionUID = -292879296310753260L;

    private IndexedCorpus target;
    final private NameBearerHandle handle;
    
    CreateIndexAction(NameBearerHandle handle) {
      super("Index Corpus");
      this.target = (IndexedCorpus)handle.getTarget();
      this.handle = handle;
      putValue(SHORT_DESCRIPTION, "Create index with documents from the corpus");
      createIndexGui = new CreateIndexGUI();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      boolean ok = OkCancelDialog.showDialog(handle.getLargeView(), createIndexGui,
              "Index \"" + target.getName() + "\" corpus");
      if(ok) {
        DefaultIndexDefinition did = new DefaultIndexDefinition();
        IREngine engine = createIndexGui.getIREngine();
        did.setIrEngineClassName(engine.getClass().getName());

        did.setIndexLocation(createIndexGui.getIndexLocation().toString());

        // add the content if wanted
        if(createIndexGui.getUseDocumentContent()) {
          did.addIndexField(new IndexField("body", new DocumentContentReader(),
                  false));
        }
        // add all the features
        Iterator<String> featIter = createIndexGui.getFeaturesList().iterator();
        while(featIter.hasNext()) {
          String featureName = featIter.next();
          did.addIndexField(new IndexField(featureName, new FeatureReader(
                  featureName), false));
        }

        target.setIndexDefinition(did);

        Thread thread = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              handle.fireProgressChanged(1);
              handle.fireStatusChanged("Indexing corpus...");
              long start = System.currentTimeMillis();
              target.getIndexManager().deleteIndex();
              handle.fireProgressChanged(10);
              target.getIndexManager().createIndex();
              handle.fireProgressChanged(100);
              handle.fireProcessFinished();
              handle.fireStatusChanged("Corpus indexed in "
                      + NumberFormat
                              .getInstance()
                              .format(
                                      (double)(System.currentTimeMillis() - start) / 1000)
                      + " seconds");
            }
            catch(IndexException ie) {
              JOptionPane.showMessageDialog(handle.getLargeView() != null
                      ? handle.getLargeView()
                      : handle.getSmallView(), "Could not create index!\n "
                      + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              ie.printStackTrace(Err.getPrintWriter());
            }
            finally {
              handle.fireProcessFinished();
            }
          }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
      }
    }

    CreateIndexGUI createIndexGui;
  }

  class OptimizeIndexAction extends AbstractAction {
    private static final long serialVersionUID = 261845730081082766L;

    private IndexedCorpus target;
    final private NameBearerHandle handle;
    
    OptimizeIndexAction(NameBearerHandle handle) {
      super("Optimize Index");
      this.handle = handle;
      this.target = (IndexedCorpus)handle.getTarget();
      putValue(SHORT_DESCRIPTION, "Optimize existing index");
    }

    @Override
    public boolean isEnabled() {
      return target.getIndexDefinition() != null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            handle.fireProgressChanged(1);
            handle.fireStatusChanged("Optimising index...");
            long start = System.currentTimeMillis();
            target.getIndexManager().optimizeIndex();
            handle.fireStatusChanged("Index optimised in "
                    + NumberFormat
                            .getInstance()
                            .format(
                                    (double)(System.currentTimeMillis() - start) / 1000)
                    + " seconds");
            handle.fireProcessFinished();
          }
          catch(IndexException ie) {
            JOptionPane.showMessageDialog(handle.getLargeView() != null
                    ? handle.getLargeView()
                    : handle.getSmallView(), "Errors during optimisation!", "GATE",
                    JOptionPane.PLAIN_MESSAGE);
            ie.printStackTrace(Err.getPrintWriter());
          }
          finally {
            handle.fireProcessFinished();
          }
        }
      });
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  class DeleteIndexAction extends AbstractAction {
    private static final long serialVersionUID = 6121632107964572415L;

    private IndexedCorpus target;
    final private NameBearerHandle handle;
    
    DeleteIndexAction(NameBearerHandle handle) {
      super("Delete Index");
      this.handle = handle;
      this.target = (IndexedCorpus)handle.getTarget();
      putValue(SHORT_DESCRIPTION, "Delete existing index");
    }

    @Override
    public boolean isEnabled() {
      return target.getIndexDefinition() != null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int answer = JOptionPane.showOptionDialog(handle.getLargeView() != null
              ? handle.getLargeView()
              : handle.getSmallView(), "Do you want to delete index?", "Gate",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              null, null);
      if(answer == JOptionPane.YES_OPTION) {
        try {
          IndexedCorpus ic = target;
          if(ic.getIndexManager() != null) {
            ic.getIndexManager().deleteIndex();
            ic.getFeatures().remove(
                    GateConstants.CORPUS_INDEX_DEFINITION_FEATURE_KEY);
          }
          else {
            JOptionPane.showMessageDialog(handle.getLargeView() != null
                    ? handle.getLargeView()
                    : handle.getSmallView(), "There is no index to delete!", "GATE",
                    JOptionPane.PLAIN_MESSAGE);
          }
        }
        catch(gate.creole.ir.IndexException ie) {
          ie.printStackTrace();
        }
      }
    }
  }

}
