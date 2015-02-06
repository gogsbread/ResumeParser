/*
 *  NewAnnotationJobAction.java
 *
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 3, June 2007 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id: NewAnnotationJobAction.java 17412 2014-02-24 17:30:09Z ian_roberts $
 */
package gate.crowdsource.ne;

import gate.gui.MainFrame;
import gate.util.GateRuntimeException;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.jeta.forms.components.panel.FormPanel;

/**
 * Action to present a dialog prompting the user for the information required to
 * create a new CrowdFlower job, and then attempt to actually create it.
 */
public class NewAnnotationJobAction extends AbstractAction {
  private static final long serialVersionUID = -5631249026928008860L;

  private EntityAnnotationJobBuilder jobBuilder;
  
  public NewAnnotationJobAction(EntityAnnotationJobBuilder jobBuilder) {
    super("Create a new CrowdFlower job");
    putValue(SHORT_DESCRIPTION,
            "Create a new job on CrowdFlower to be populated by this PR");
    this.jobBuilder = jobBuilder;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      InputStream formInput = this.getClass().getResourceAsStream("newAnnotationJobForm.xml");
      final FormPanel panel;
      try {
        panel = new FormPanel(formInput);
      } finally {
        formInput.close();
      }
      
      panel.setPreferredSize(new Dimension(500, 400));
      
      // attach listeners etc.
      final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
      final JDialog dialog  = new JDialog(MainFrame.getInstance(), "New annotation job", true);
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          optionPane.setValue(Integer.valueOf(JOptionPane.CLOSED_OPTION));
        }
      });
      dialog.setContentPane(optionPane);
      optionPane.addPropertyChangeListener(new PropertyChangeListener() {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if(JOptionPane.VALUE_PROPERTY.equals(evt.getPropertyName())) {
            Object value = optionPane.getValue();
            if (value == JOptionPane.UNINITIALIZED_VALUE) {
              //ignore reset
              return;
            }
 
            // Reset the JOptionPane's value.
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            if(Integer.valueOf(JOptionPane.OK_OPTION).equals(value)) {
              boolean valid = verify(panel);
              if(valid) {
                // hide the dialog
                dialog.setVisible(false);
                // do the job in a background thread
                new Thread(new Runnable() {
                  public void run() {
                    try {
                      MainFrame.lockGUI("Creating CrowdFlower job");
                      try {
                        jobBuilder.setJobId(jobBuilder.crowdFlowerClient.createAnnotationJob(
                                panel.getTextField("title").getText().trim(),
                                panel.getTextComponent("instructions").getText(),
                                panel.getTextField("caption").getText().trim(),
                                panel.getTextField("noEntitiesCaption").getText().trim()));
                        // if the creation was successful we can dispose the dialog
                        SwingUtilities.invokeLater(new Runnable() {
                          public void run() {
                            dialog.dispose();
                          }
                        });
                      } finally {
                        MainFrame.unlockGUI();
                      }
                    } catch(Exception ex) {
                      ex.printStackTrace();
                      SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                          JOptionPane.showMessageDialog(dialog,
                                  "Error creating the crowdflower job, see the messages pane for details",
                                  "Error creating job",
                                  JOptionPane.ERROR_MESSAGE);
                          dialog.setVisible(true);
                        }
                      });
                    }
                  }
                }).start();
              }
            } else {
              // cancel or close
              dialog.setVisible(false);
              dialog.dispose();
            }
          }
        }
      });
      dialog.setResizable(true);
      dialog.pack();
      dialog.setLocationRelativeTo(dialog.getParent());
      dialog.setVisible(true);
    } catch(Exception ex) {
      throw new GateRuntimeException(ex);
    }
  }

  protected boolean verify(FormPanel panel) {
    boolean valid = true;
    JLabel statusLabel = panel.getLabel("status");
    if("".equals(panel.getTextField("title").getText().trim())) {
      statusLabel.setText("<html><p>A job title is required</p></html>");
      statusLabel.setIcon(MainFrame.getIcon("Invalid"));
      valid = false;
    } else if("".equals(panel.getTextField("caption").getText().trim())) {
      statusLabel.setText("<html><p>A task caption is required</p></html>");
      statusLabel.setIcon(MainFrame.getIcon("Invalid"));
      valid = false;
    } else if("".equals(panel.getTextField("noEntitiesCaption").getText().trim())) {
      statusLabel.setText("<html><p>A caption for the \"no entities\" checkbox is required</p></html>");
      statusLabel.setIcon(MainFrame.getIcon("Invalid"));
      valid = false;
    }
    return valid;
  }

}
