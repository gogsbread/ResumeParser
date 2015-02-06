/*
 *    Kea.java
 * 
 *    Copyright (c) 1998-2005, The University of Sheffield.
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 3/Feb/2005
 *
 *  $Id: Kea.java 6558 2005-02-03 17:28:52 +0000 (Thu, 03 Feb 2005) valyt $
 */
 
package gate.creole.kea;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.gui.ActionsPublisher;
import gate.gui.MainFrame;
import gate.util.Err;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import kea.KEAFilter;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This is wrapper for using the KEA Keyphrase extractor
 * (<a href="http://www.nzdl.org/Kea/">http://www.nzdl.org/Kea/</a>)
 * within the GATE Language Engineering
 * architecture (<a href="http://gate.ac.uk">http://gate.ac.uk</a>).
 * It exposes KEA as a GATE Processing Resource that has two functioning modes:
 * <UL>
 *  <LI>Training mode: when keyphrases (marked as annotations on documents) are
 *  collected and a model is built.
 *  <LI>Application mode: when a built model is applied on documents to the end
 *  of extracting keyphrases.
 * </UL>
 */
@SuppressWarnings("serial")
public class Kea extends AbstractLanguageAnalyser implements ActionsPublisher{

  private static final long serialVersionUID = 8297240873486868105L;

  /**
   * Anonymous constructor, required by GATE. Does nothing.
   */
  public Kea() {
  }

  /**
   * Gets the list of GUI actions available from this PR. Currently Load and
   * Save model.
   * @return
   */
  public List<Action> getActions() {
    return actions;
  }

  /**
   * Executes this PR. Depeding on the state of the {@link #trainingMode} switch
   * it will either train a model or apply it over the documents.<br>
   * Trainig consists of collecting keyphrase annotations from the input
   * annotation set of the input documents. The first time a trained model is
   * required (either application mode has started or the model is being saved)
   * the actual model ({link @ #keaModel}) will be constructed.<br>
   * The application mode consists of using a trained model to generate
   * keyphrase annotations on the output annotation set of the input documents.
   * @throws ExecutionException
   */
  public void execute() throws gate.creole.ExecutionException {
    //reinitialise the KEA filter if already trained
    if(trainingMode.booleanValue() && trainingFinished){
      //retrainig started with a used model
      System.out.println("Reinitialising KEA model...");
      try{
        initModel();
      }catch(Exception e){
        throw new ExecutionException(e);
      }
    }

    //get the clear text from the document.
    String text = document.getContent().toString();
    //generate the first attribute: the text
    //this will be used for both training and application modes.
    double[] newInst = new double[2];
    newInst[0] = data.attribute(0).addStringValue(text);

    if(trainingMode.booleanValue()){
      //training mode -> we need to collect the keyphrases
      //find the input annotation set.
      AnnotationSet annSet = inputAS == null || inputAS.length() == 0 ?
                             document.getAnnotations() :
                             document.getAnnotations(inputAS);
      //extract the keyphrase annotations
      AnnotationSet kpSet = annSet.get(keyphraseAnnotationType);
      if(kpSet != null && kpSet.size() > 0){
        //use a set to avoid repetitions
        Set<String> keyPhrases = new HashSet<String>();

        Iterator<Annotation> keyPhraseIter = kpSet.iterator();
        //initialise the string for the second attribute
        String keyPhrasesStr = "";
        while(keyPhraseIter.hasNext()){
          //get one keyphrase annotation
          Annotation aKeyPhrase = keyPhraseIter.next();
          try{
            //get the string for the keyphrase annotation
            String keyPhraseStr = document.getContent().
                                  getContent(aKeyPhrase.getStartNode().getOffset(),
                                             aKeyPhrase.getEndNode().getOffset()).
                                  toString();
            //if the keyphrase has not been seen before add to the string for
            //the second attribute
            if(keyPhrases.add(keyPhraseStr)) keyPhrasesStr +=
                                                keyPhrasesStr.length() > 0 ?
                                                "\n" + keyPhraseStr :
                                                keyPhraseStr;
          }catch(InvalidOffsetException ioe){
            throw new ExecutionException(ioe);
          }
        }
        //all the keyphrases have been enumerated -> create the second attribute
        newInst[1] = data.attribute(1).addStringValue(keyPhrasesStr);
      }else{
        //no keyphrase annotations
        newInst[1] = Instance.missingValue();
        System.out.println("No keyphrases in document: " + document.getName());
      }
      //add the new instance to the dataset
      data.add(new Instance(1.0, newInst));
      try{
        keaFilter.input(data.instance(0));
      }catch(Exception e){
        throw new ExecutionException(e);
      }
      data = data.stringFreeStructure();
    }else{
      //application mode -> we need to generate keyphrases
      //build the model if not already done
      if(!trainingFinished) finishTraining();

      newInst[1] = Instance.missingValue();
      data.add(new Instance(1.0, newInst));
      try{
        keaFilter.input(data.instance(0));
      }catch(Exception e){
        throw new ExecutionException(e);
      }
      data = data.stringFreeStructure();
      //extract the output from the model
      Instance[] topRankedInstances = new Instance[phrasesToExtract.intValue()];
      Instance inst;
      while ((inst = keaFilter.output()) != null) {
        int index = (int)inst.value(keaFilter.getRankIndex()) - 1;
        if (index < phrasesToExtract.intValue()) {
          topRankedInstances[index] = inst;
        }
      }
      //annotate the document with the results -> create a list with all the
      //keyphrases found by KEA
      List<String> phrases = new ArrayList<String>();
      for(int i = 0; i < topRankedInstances.length; i ++){
        if(topRankedInstances[i] != null){
          phrases.add(topRankedInstances[i].
                      stringValue(keaFilter.getUnstemmedPhraseIndex()));
        }
      }
      try{
        //add the actiul annotations on the document
        annotateKeyPhrases(phrases);
      }catch(Exception e){
        throw new ExecutionException(e);
      }
    }//application mode
  }//execute

  /**
   * Annnotates the document with all the occurences of keyphrases from a List.
   * Uses the java.util.regex package to search for ocurences of keyphrases.
   * @param phrases the list of keyphrases.
   * @throws Exception
   */
  protected void annotateKeyPhrases(List<String> phrases) throws Exception{
    if(phrases == null || phrases.isEmpty()) return;
    //create a pattern
    String patternStr = "";
    Iterator<String> phraseIter = phrases.iterator();
    while(phraseIter.hasNext()){
      String phrase = phraseIter.next();
      patternStr += patternStr.length() == 0 ?
                 "\\Q" + phrase + "\\E" :
                 "|\\Q" + phrase + "\\E";
    }

    Pattern pattern = Pattern.compile(patternStr,
                                      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    Matcher matcher = pattern.matcher(document.getContent().toString());
    //find the output annotation set
    AnnotationSet outputSet = outputAS == null || outputAS.length() == 0 ?
                              document.getAnnotations() :
                              document.getAnnotations(outputAS);
    while(matcher.find()){
      int start = matcher.start();
      int end = matcher.end();
      //add the new annotation
      outputSet.add(new Long(start), new Long(end), keyphraseAnnotationType,
                    Factory.newFeatureMap());
    }
    document.getFeatures().put("KEA matched keyphrases", phrases);
  }//protected void annotateKeyPhrases(List phrases)


  /**
   * Action used to save a trained model. If the model is not built yet it will
   * be built before being saved.
   */
  protected class SaveModelAction extends javax.swing.AbstractAction{
    public SaveModelAction(){
      super("Save model");
      putValue(SHORT_DESCRIPTION, "Saves the KEA model to a file");
    }

    public void actionPerformed(java.awt.event.ActionEvent evt){
      //we need to use a new thread to avoid blocking the GUI
      Runnable runnable = new Runnable(){
        public void run(){
          //get the file to save to
          JFileChooser fileChooser = MainFrame.getFileChooser();
          fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
          fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fileChooser.setMultiSelectionEnabled(false);
          if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            ObjectOutputStream oos = null;
            try{
              MainFrame.lockGUI("Saving KEA model...");
              if(!trainingFinished) finishTraining();
              //zip and save the model
              oos = new ObjectOutputStream(new GZIPOutputStream(
                    new FileOutputStream(file.getCanonicalPath(), false)));
              oos.writeObject(keaFilter);
              oos.flush();
              oos.close();
              oos = null;
            }catch(Exception e){
              MainFrame.unlockGUI();
              JOptionPane.showMessageDialog(MainFrame.getInstance(),
                              "Error!\n"+
                               e.toString(),
                               "Gate", JOptionPane.ERROR_MESSAGE);
              e.printStackTrace(Err.getPrintWriter());
              if(oos != null) try{
                oos.close();
              }catch(IOException ioe){
                JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                "Error!\n"+
                                 ioe.toString(),
                                 "Gate", JOptionPane.ERROR_MESSAGE);
                ioe.printStackTrace(Err.getPrintWriter());
              }
            }finally{
              MainFrame.unlockGUI();
            }
          }
        }
      };
      Thread thread = new Thread(runnable, "ModelSaver(serialisation)");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  /**
   * Action for loading a saved model. Once loaded the model wil be marked as
   * trained.
   */
  protected class LoadModelAction extends javax.swing.AbstractAction{
    public LoadModelAction(){
      super("Load model");
      putValue(SHORT_DESCRIPTION, "Loads a KEA model from a file");
    }

    public void actionPerformed(java.awt.event.ActionEvent evt){
      Runnable runnable = new Runnable(){
        public void run(){
          //get the file to load from.
          JFileChooser fileChooser = MainFrame.getFileChooser();
          fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
          fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fileChooser.setMultiSelectionEnabled(false);
          if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            ObjectInputStream ois = null;
            try{
              MainFrame.lockGUI("Loading model...");
              //unzip and load the model
              ois = new ObjectInputStream(new GZIPInputStream(
                    new FileInputStream(file)));
              keaFilter = (KEAFilter)ois.readObject();
              ois.close();
              ois = null;
              //mark the model as trained.
              trainingFinished = true;
            }catch(Exception e){
              MainFrame.unlockGUI();
              JOptionPane.showMessageDialog(MainFrame.getInstance(),
                              "Error!\n"+
                               e.toString(),
                               "Gate", JOptionPane.ERROR_MESSAGE);
              e.printStackTrace(Err.getPrintWriter());
              if(ois != null) try{
                ois.close();
              }catch(IOException ioe){
                JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                "Error!\n"+
                                 ioe.toString(),
                                 "Gate", JOptionPane.ERROR_MESSAGE);
                ioe.printStackTrace(Err.getPrintWriter());
              }
            }finally{
              MainFrame.unlockGUI();
            }
          }
        }
      };
      Thread thread = new Thread(runnable, "ModelLoader(serialisation)");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  /**
   * Sets the annotation type to be used for keyphrases.
   * @param keyphraseAnnotationType
   */
  public void setKeyphraseAnnotationType(String keyphraseAnnotationType) {
    this.keyphraseAnnotationType = keyphraseAnnotationType;
  }

  /**
   * Sets the annotation type to be used for keyphrases.
   * @return
   */
  public String getKeyphraseAnnotationType() {
    return keyphraseAnnotationType;
  }

  /**
   * Sets the name for the input annotation set.
   * @param inputAS
   */
  public void setInputAS(String inputAS) {
    this.inputAS = inputAS;
  }

  /**
   * Gets the name for the input annotation set.
   * @return
   */
  public String getInputAS() {
    return inputAS;
  }

  /**
   * Sets the name for the output annotation set.
   * @param outputAS
   */
  public void setOutputAS(String outputAS) {
    this.outputAS = outputAS;
  }

  /**
   * Gets the name for the output annotation set.
   * @return
   */
  public String getOutputAS() {
    return outputAS;
  }

  /**
   * Initialises this KEA Processing Resource.
   * @return
   * @throws ResourceInstantiationException
   */
  public Resource init() throws gate.creole.ResourceInstantiationException {
    System.out.println("\nThis is KEA (automatic keyphrase extraction)");
    System.out.println("Details at http://www.nzdl.org/Kea/\n");
    super.init();
    try{
      initModel();
    }catch(Exception e){
      throw new ResourceInstantiationException(e);
    }
    actions = new ArrayList<Action>();
    actions.add(new SaveModelAction());
    actions.add(new LoadModelAction());
    return this;
  }

  /**
   * Initialises the KEA model.
   * @throws Exception
   */
  protected void initModel()throws Exception{
    keaFilter = new KEAFilter();

    atts = new FastVector(2);
    atts.addElement(new Attribute("doc", (FastVector)null));
    atts.addElement(new Attribute("keyphrases", (FastVector)null));
    data = new Instances("keyphrase_training_data", atts, 0);

    keaFilter.setDisallowInternalPeriods(getDisallowInternalPeriods().booleanValue());
    keaFilter.setKFused(getUseKFrequency().booleanValue());
    keaFilter.setMaxPhraseLength(getMaxPhraseLength().intValue());
    keaFilter.setMinPhraseLength(getMinPhraseLength().intValue());
    keaFilter.setMinNumOccur(getMinNumOccur().intValue());
    keaFilter.setInputFormat(data);
    trainingFinished = false;
  }

  /**
   * Stops the training phase and builds the actual model.
   * @throws ExecutionException
   */
  protected void finishTraining() throws ExecutionException{
    if(trainingFinished) return;
    try{
      keaFilter.batchFinished();
    }catch(Exception e){
      throw new ExecutionException(e);
    }
    // Get rid of instances in filter
    @SuppressWarnings("unused")
    Instance dummy;
    while ((dummy = keaFilter.output()) != null) {};
    trainingFinished = true;
  }


  public void setMaxPhraseLength(Integer maxPhraseLength) {
    this.maxPhraseLength = maxPhraseLength;
  }
  public Integer getMaxPhraseLength() {
    return maxPhraseLength;
  }
  public void setMinPhraseLength(Integer minPhraseLength) {
    this.minPhraseLength = minPhraseLength;
  }
  public Integer getMinPhraseLength() {
    return minPhraseLength;
  }
  public void setDisallowInternalPeriods(Boolean dissallowInternalPeriods) {
    this.disallowInternalPeriods = dissallowInternalPeriods;
  }
  public Boolean getDisallowInternalPeriods() {
    return disallowInternalPeriods;
  }
  public void setUseKFrequency(Boolean useKFrequency) {
    this.useKFrequency = useKFrequency;
  }
  public Boolean getUseKFrequency() {
    return useKFrequency;
  }
  public void setMinNumOccur(Integer minNumOccur) {
    this.minNumOccur = minNumOccur;
  }
  public Integer getMinNumOccur() {
    return minNumOccur;
  }
  public Boolean getTrainingMode() {
    return trainingMode;
  }
  public void setTrainingMode(Boolean trainingMode) {
    this.trainingMode = trainingMode;
  }
  public void setPhrasesToExtract(Integer phrasesToExtract) {
    this.phrasesToExtract = phrasesToExtract;
  }
  public Integer getPhrasesToExtract() {
    return phrasesToExtract;
  }

  /**
   * If <tt>true</tt> then the PR is in training mode and will collect
   * keyphrases from the input documents.
   * If <tt>false</tt> then the PR is in application mode and will generate
   * keyphrases on the input documents.
   */
  private Boolean trainingMode;

  /**
   * The annotation type used for the keyphrase annotations.
   */
  private String keyphraseAnnotationType;

  /**
   * The name of the input annotation set.
   */
  private String inputAS;

  /**
   * The name for the output annotation set.
   */
  private String outputAS;

  /**
   * The maximum length for a keyphrase (default 3).
   */
  private Integer maxPhraseLength;

  /**
   * The minimum length for a keyphrase (default 1).
   */
  private Integer minPhraseLength;

  /**
   * Should periods be disallowed inside keyphrases?
   */
  private Boolean disallowInternalPeriods;

  /**
   * Use keyphrase frequency statistic.
   */
  private Boolean useKFrequency;

  /**
   * The minimum number of times a phrase needs to occur (default: 2).
   */
  private Integer minNumOccur;

  /**
   * How many keyphrases should be extracted for each input document?
   */
  private Integer phrasesToExtract;

  /**
   * This flag is used to determine whether the model has been constructed or
   * not. During training mode the training data is simply collected and this
   * flag is set to <tt>false</tt>. The first time when the traied model is
   * required (which could be either the first time the application mode is
   * started or when the model is being saved) the model is built from the
   * collected instances and this flag is set to <tt>true</tt>.<br>
   * If this flag is found to be <tt>true</tt> during training phase (i.e.
   * there is an attempt to train an already triend model) then the current
   * model will be discarded and a new one will be created. The traininig will
   * be performed using the newly created model.
   */
  protected boolean trainingFinished;

  /**
   * The KEA filter object which incorporates the actual model.
   */
  protected  KEAFilter keaFilter = null;

  /**
   * Data structure used internally to define the dataset.
   */
  protected FastVector atts;

  /**
   * The dataset.
   */
  protected Instances data;

  /**
   * The list of GUI actions available from this PR on popup menus.
   */
  protected List<Action> actions;
}