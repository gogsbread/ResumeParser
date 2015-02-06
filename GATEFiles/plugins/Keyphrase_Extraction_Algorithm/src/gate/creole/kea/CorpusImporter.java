/*
 *    CorpusImporter.java
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
 *  $Id: CorpusImporter.java 7479 2006-06-29 19:21:29 +0000 (Thu, 29 Jun 2006) valyt $
 */

package gate.creole.kea;

import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Executable;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Resource;
import gate.creole.AbstractVisualResource;
import gate.creole.ExecutionException;
import gate.gui.MainFrame;
import gate.util.BomStrippingInputStreamReader;
import gate.util.Err;
import gate.util.ExtensionFileFilter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.io.IOUtils;

/**
 * A simple utility to import KEA style corpora into GATE.
 * It is registered as a GATE viewer for the KEA Processing Resource and it is
 * available from the main pane.
 */
@SuppressWarnings("serial")
public class CorpusImporter extends AbstractVisualResource {

  private static final long serialVersionUID = 5318018411995211792L;

  public CorpusImporter() {
  }
  /**
   * Does nothing as this is not really a viewer.
   */
  public void setTarget(Object target){}

  public Resource init() throws gate.creole.ResourceInstantiationException {
    super.init();
    initLocalData();
    initGUIComponents();
    initListeners();
    return this;
  }

  protected void initLocalData(){
  }

  protected void initGUIComponents(){
    setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridy = 0;
    constraints.gridx = GridBagConstraints.RELATIVE;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.BOTH;

    JPanel inputPanel = new JPanel(new GridBagLayout());
    inputPanel.setBorder(BorderFactory.createTitledBorder("Input"));
    GridBagConstraints constraints2 = new GridBagConstraints();
    constraints2.gridy = 0;
    constraints2.gridx = GridBagConstraints.RELATIVE;
    constraints2.weighty = 0;
    constraints2.weightx = 0;
    constraints2.fill = GridBagConstraints.BOTH;
    constraints2.insets = new Insets(2, 2, 2, 2);

    JLabel label = new JLabel("Source directory:");
    inputPanel.add(label, constraints2);
    sourceDirTField = new JTextField(30);
    inputPanel.add(sourceDirTField, constraints2);
    JButton openButton = new JButton(new SelectDirectoryAction());
    inputPanel.add(openButton, constraints2);

    constraints2.gridy = 1;
    label = new JLabel("Extension for text files:");
    inputPanel.add(label, constraints2);
    constraints2.gridwidth = 2;
    textExtensionTField = new JTextField(".txt");
    inputPanel.add(textExtensionTField, constraints2);
    constraints2.gridwidth = 1;

    constraints2.gridy = 2;
    label = new JLabel("Extension for keyphrase files:");
    inputPanel.add(label, constraints2);
    constraints2.gridwidth = 2;
    keyExtensionTField = new JTextField(".key");
    inputPanel.add(keyExtensionTField, constraints2);
    constraints2.gridwidth = 1;

    constraints2.gridy = 3;
    label = new JLabel("Encoding for input files:");
    inputPanel.add(label, constraints2);
    constraints2.gridwidth = 2;
    encodingTField = new JTextField("");
    inputPanel.add(encodingTField, constraints2);
    constraints2.gridwidth = 1;

    add(inputPanel, constraints);
    constraints.weightx = 1;
    add(Box.createHorizontalGlue(), constraints);
    constraints.weightx = 0;

    JPanel outputPanel = new JPanel();
    outputPanel.setLayout(new GridBagLayout());
    outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));

    constraints2.gridy = 0;
    label = new JLabel("Corpus name:");
    outputPanel.add(label, constraints2);
    constraints2.weightx = 1;
    corpusNameTField = new JTextField("KEA Corpus");
    constraints2.weightx = 0;
    outputPanel.add(corpusNameTField, constraints2);

    constraints2.gridy = 1;
    label = new JLabel("Output annotation set:");
    outputPanel.add(label, constraints2);
    constraints2.weightx = 1;
    annotationSetTField = new JTextField("Key");
    constraints2.weightx = 0;
    outputPanel.add(annotationSetTField, constraints2);

    constraints2.gridy = 2;
    label = new JLabel("Keyphrase annotation type:");
    outputPanel.add(label, constraints2);
    constraints2.weightx = 1;
    annotationTypeTField = new JTextField("Keyphrase");
    constraints2.weightx = 0;
    outputPanel.add(annotationTypeTField, constraints2);


    constraints.gridy = 1;
    add(outputPanel, constraints);

    constraints.gridy = 2;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.CENTER;
    add(new JButton(new ImportCorpusAction()), constraints);

    constraints.gridy = 3;
    constraints.weighty = 1;
    add(Box.createVerticalGlue(), constraints);
  }

  protected void initListeners(){
  }

  protected class SelectDirectoryAction extends AbstractAction{
    public SelectDirectoryAction(){
      super("");
      putValue(SHORT_DESCRIPTION, "Opens a file chooser");
      putValue(SMALL_ICON, MainFrame.getIcon("open-file"));
    }

    public void actionPerformed(ActionEvent evt){
      JFileChooser chooser = MainFrame.getFileChooser();
      chooser.setMultiSelectionEnabled(false);
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if(chooser.showOpenDialog(CorpusImporter.this) == JFileChooser.APPROVE_OPTION){
        try{
          sourceDirTField.setText(chooser.getSelectedFile().getCanonicalPath());
        }catch(IOException ioe){
          JOptionPane.showMessageDialog(MainFrame.getInstance(),
                          "Error!\n"+
                           ioe.toString(),
                           "Gate", JOptionPane.ERROR_MESSAGE);
          ioe.printStackTrace(Err.getPrintWriter());
        }
      }
    }
  }

  protected class ImportCorpusAction extends AbstractAction{
    public ImportCorpusAction(){
      super("Import!");
      putValue(SHORT_DESCRIPTION, "Creates a new GATE corpus");
    }

    public void actionPerformed(ActionEvent evt){
      Executable executable = new Executable(){
        public void execute(){
          interrupted = false;
          try{
            MainFrame.lockGUI("Importing...");

            File sourceDir = new File(sourceDirTField.getText());
            if(sourceDir.isDirectory()){
              //create a new corpus
              Corpus corpus = Factory.newCorpus(corpusNameTField.getText());
              String textExt = textExtensionTField.getText();
              String keyExt = keyExtensionTField.getText();
              String encoding = encodingTField.getText();

              //get the text files
              ExtensionFileFilter filter = new ExtensionFileFilter();
              filter.addExtension(textExt);
              File[] textFiles = sourceDir.listFiles(filter);
              if(textFiles != null){
                for(int i = 0; i < textFiles.length; i++){
                  if(textFiles[i].isDirectory()) continue;
                  FeatureMap params = Factory.newFeatureMap();
                  params.put("sourceUrl", textFiles[i].toURI().toURL());
                  if(encoding != null && encoding.length() > 0)
                    params.put("encoding", encoding);
                  Document doc = (Document)Factory.createResource(
                      "gate.corpora.DocumentImpl", params, null,
                      textFiles[i].getName());
                  corpus.add(doc);
                  //locate the key file
                  String fileName = textFiles[i].getCanonicalPath();
                  int pos = fileName.lastIndexOf(textExt);
                  fileName = fileName.substring(0, pos) + keyExt;
                  List<String> keys = new ArrayList<String>();
                  Exception e = null;
                  BufferedReader reader = null;
                  try{
                    
                    if(encoding != null && encoding.length() > 0){
                      reader = new BomStrippingInputStreamReader(
                                 new FileInputStream(fileName), encoding);
                    }else{
                      reader = new BufferedReader(new FileReader(fileName));
                    }

                    String line = reader.readLine();
                    while(line != null){
                      keys.add(line);
                      line = reader.readLine();
                    }
                  }catch(IOException ioe){
                    e = ioe;
                  }
                  finally {
                    IOUtils.closeQuietly(reader);
                  }
                  if(keys.isEmpty() || e != null){
                    MainFrame.unlockGUI();
                    int res = JOptionPane.showOptionDialog(CorpusImporter.this,
                                    "There were problems obtainig the keyphrases for " +
                                    textFiles[i].getName()  + "!",
                                    "Gate",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.ERROR_MESSAGE,
                                    null,
                                    new String[]{"Continue", "Stop importing"},
                                    "Continue");
                    if(e != null) e.printStackTrace(Err.getPrintWriter());
                    if(res == JOptionPane.NO_OPTION){
                      interrupt();
                    }else{
                      //the user wants to continue: lock the GUI
                      MainFrame.lockGUI("Importing...");
                    }
                    Factory.deleteResource(doc);
                  }else{
                    //we found some keys
                    if(!annotateKeyPhrases(doc, annotationSetTField.getText(),
                                       annotationTypeTField.getText(), keys)){
                      //none of the keys were present in the document
                      MainFrame.unlockGUI();
                      int res = JOptionPane.showOptionDialog(CorpusImporter.this,
                                      "None of the keyphrases were present in " +
                                      textFiles[i].getName(),
                                      "Gate",
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.ERROR_MESSAGE,
                                      null,
                                      new String[]{"Keep document",
                                                   "Discard document",
                                                   "Stop importing"},
                                      "Discard document");
                      if(res == JOptionPane.CANCEL_OPTION){
                        //the user gave up
                        interrupt();
                      }else{
                        MainFrame.lockGUI("Importing...");
                        if(res == JOptionPane.NO_OPTION){
                          Factory.deleteResource(doc);
                        }
                      }

                    }
                  }

                  if(isInterrupted()){
                    MainFrame.unlockGUI();
                    return;
                  }
                }
              }

            }else{
              throw new Exception(sourceDir.getCanonicalPath() +
                                  " is not a directory!");
            }
          }catch(Exception e){
            MainFrame.unlockGUI();
            JOptionPane.showMessageDialog(MainFrame.getInstance(),
                            "Error!\n"+
                             e.toString(),
                             "Gate", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(Err.getPrintWriter());
          }finally{
            MainFrame.unlockGUI();
            Gate.setExecutable(null);
          }
        }
        /**
         * Checks whether this executable has been interrupted since the last
         * time its {@link execute()} method was called.
         */
        public synchronized boolean isInterrupted(){
          return interrupted;
        }

        /**
         * Notifies this executable that it should stop its execution as soon
         * as possible.
         */
        public synchronized void interrupt(){
          interrupted = true;
        }

        protected boolean interrupted = false;
      };

      class Executor implements Runnable{
        public Executor(Executable executable){
          this.executable = executable;
        }
        public void run(){
          try{
            executable.execute();
          }catch(ExecutionException ee){
            MainFrame.unlockGUI();
            JOptionPane.showMessageDialog(MainFrame.getInstance(),
                            "Error!\n"+
                             ee.toString(),
                             "Gate", JOptionPane.ERROR_MESSAGE);
            ee.printStackTrace(Err.getPrintWriter());
          }
        }
        Executable executable;
      };

      Thread thread = new Thread(new Executor(executable));
      thread.setPriority(Thread.MIN_PRIORITY);
      Gate.setExecutable(executable);
      thread.start();
    }
  }


  protected boolean annotateKeyPhrases(Document document,
                                    String annSetName,
                                    String keyphraseAnnotationType,
                                    List<String> phrases) throws Exception{
    if(phrases == null || phrases.isEmpty()) return false;
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
    AnnotationSet outputSet = annSetName == null || annSetName.length() == 0 ?
                              document.getAnnotations() :
                              document.getAnnotations(annSetName);
    boolean result = false;
    while(matcher.find()){
      int start = matcher.start();
      int end = matcher.end();
      outputSet.add(new Long(start), new Long(end), keyphraseAnnotationType,
                    Factory.newFeatureMap());
      result = true;
    }
    document.getFeatures().put("Author assigned keyphrases", phrases);
    return result;
  }//protected void annotateKeyPhrases(List phrases)

  JTextField sourceDirTField;
  JTextField textExtensionTField;
  JTextField keyExtensionTField;
  JTextField encodingTField;

  JTextField corpusNameTField;
  JTextField annotationTypeTField;
  JTextField annotationSetTField;
}