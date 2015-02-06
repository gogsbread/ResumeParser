/*
 *  EntityClassificationJobBuilder.java
 *
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 3, June 2007 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id: EntityClassificationJobBuilder.java 17968 2014-05-11 16:37:34Z ian_roberts $
 */

package gate.crowdsource.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Resource;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.crowdsource.rest.CrowdFlowerClient;
import gate.gui.ActionsPublisher;

import org.apache.log4j.Logger;

@CreoleResource(name = "Entity Classification Job Builder",
   comment = "Build a CrowdFlower job asking users to select the right label for entities",
   helpURL = "http://gate.ac.uk/userguide/sec:crowd:classification")
public class EntityClassificationJobBuilder extends AbstractLanguageAnalyser implements ActionsPublisher {

  private static final Logger log = Logger.getLogger(EntityClassificationJobBuilder.class);

  private static final long serialVersionUID = -1584716901194104888L;

  private String apiKey;
  
  private Long jobId;
  
  private String contextAnnotationType;
  
  private String contextASName;
  
  private String entityAnnotationType;
  
  private String entityASName;
  
  protected CrowdFlowerClient crowdFlowerClient;

  public String getApiKey() {
    return apiKey;
  }

  @CreoleParameter(comment = "CrowdFlower API key")
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public Long getJobId() {
    return jobId;
  }

  @RunTime
  @CreoleParameter
  public void setJobId(Long jobId) {
    this.jobId = jobId;
  }

  public String getContextAnnotationType() {
    return contextAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Sentence")
  public void setContextAnnotationType(String contextAnnotationType) {
    this.contextAnnotationType = contextAnnotationType;
  }

  public String getContextASName() {
    return contextASName;
  }

  @Optional
  @RunTime
  @CreoleParameter
  public void setContextASName(String contextASName) {
    this.contextASName = contextASName;
  }

  public String getEntityAnnotationType() {
    return entityAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Mention")
  public void setEntityAnnotationType(String entityAnnotationType) {
    this.entityAnnotationType = entityAnnotationType;
  }

  public String getEntityASName() {
    return entityASName;
  }

  @Optional
  @RunTime
  @CreoleParameter
  public void setEntityASName(String entityASName) {
    this.entityASName = entityASName;
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
    if(apiKey == null || "".equals(apiKey)) {
      throw new ResourceInstantiationException("API Key must be set");
    }
    crowdFlowerClient = new CrowdFlowerClient(apiKey);
    return this;
  }

  @Override
  public void execute() throws ExecutionException {
    if(isInterrupted()) throw new ExecutionInterruptedException();
    interrupted = false;
    try {
      if(jobId == null || jobId.longValue() <= 0) {
        throw new ExecutionException("Job ID must be provided");
      }
      
      AnnotationSet entityAS = getDocument().getAnnotations(entityASName);
      AnnotationSet contextAnnotations = getDocument().getAnnotations(contextASName)
              .get(contextAnnotationType);
      
      List<Annotation> allEntities = Utils.inDocumentOrder(entityAS.get(entityAnnotationType));
      fireStatusChanged("Creating CrowdFlower units for " + allEntities.size() + " "
              + entityAnnotationType + " annotations for classification task ");

      int entityIdx = 0;
      for(Annotation entity : allEntities) {
        fireProgressChanged((100 * entityIdx++) / allEntities.size());
        if(isInterrupted()) throw new ExecutionInterruptedException();
        AnnotationSet thisEntityContext = Utils.getCoveringAnnotations(contextAnnotations, entity);
        if(thisEntityContext.isEmpty()) {
          log.warn(entityAnnotationType + " with ID " + entity.getId() +
              " at offsets (" + Utils.start(entity) + ":" + Utils.end(entity) +
              ") in document " + getDocument().getName() + 
              " has no surrounding " + contextAnnotationType + " - ignored");
        } else {
          // get the "closest" context, i.e. the shortest annotation in the covering set.
          // usually we'd expect this set to contain just one annotation
          Annotation context = Collections.min(thisEntityContext, ANNOTATION_LENGTH_COMPARATOR);
          crowdFlowerClient.createClassificationUnit(jobId, getDocument(), entityASName, context, entity);
        }
      }
      fireProcessFinished();
      fireStatusChanged(entityIdx + " units created");
    } finally {
      interrupted = false;
    }
  }
  
  private static final Comparator<Annotation> ANNOTATION_LENGTH_COMPARATOR = new Comparator<Annotation>() {
    public int compare(Annotation a1, Annotation a2) {
      return Utils.length(a1) - Utils.length(a2);
    }
  };
  
  private List<Action> actions = null;
  
  public List<Action> getActions() {
    if(actions == null) {
      actions = new ArrayList<Action>();
      actions.add(new NewClassificationJobAction(this));
    }
    return actions;
  }
}
