/*
 *  EntityAnnotationJobBuilder.java
 *
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 3, June 2007 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id: EntityAnnotationJobBuilder.java 17968 2014-05-11 16:37:34Z ian_roberts $
 */
package gate.crowdsource.ne;

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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

@CreoleResource(name = "Entity Annotation Job Builder",
    comment = "Build a CrowdFlower job asking users to annotate entities "
       + "within a snippet of text",
    helpURL = "http://gate.ac.uk/userguide/sec:crowd:annotation")
public class EntityAnnotationJobBuilder extends AbstractLanguageAnalyser
                                                                        implements
                                                                        ActionsPublisher {

  private static final long serialVersionUID = -1584716901194104888L;

  private String apiKey;

  private Long jobId;

  private String snippetAnnotationType;

  private String snippetASName;

  private String tokenAnnotationType;

  private String tokenASName;

  private String detailFeatureName;

  private String goldFeatureName;

  private String goldFeatureValue;

  private String goldReasonFeatureName;

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

  public String getSnippetAnnotationType() {
    return snippetAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Sentence", comment = "Annotation type for the \"snippet\" annotations.  "
          + "One snippet = one CrowdFlower unit")
  public void setSnippetAnnotationType(String contextAnnotationType) {
    this.snippetAnnotationType = contextAnnotationType;
  }

  public String getSnippetASName() {
    return snippetASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "Annotation set where snippet annotations can be found")
  public void setSnippetASName(String contextASName) {
    this.snippetASName = contextASName;
  }

  public String getTokenAnnotationType() {
    return tokenAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Token", comment = "Annotation type "
          + "representing the \"tokens\" - the atomic units that "
          + "workers will have to select to mark entity annotations.")
  public void setTokenAnnotationType(String tokenAnnotationType) {
    this.tokenAnnotationType = tokenAnnotationType;
  }

  public String getTokenASName() {
    return tokenASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "Annotation set where tokens can be found")
  public void setTokenASName(String tokenASName) {
    this.tokenASName = tokenASName;
  }

  public String getDetailFeatureName() {
    return detailFeatureName;
  }

  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "detail", comment = "Feature on the "
          + "snippet annotations containing additional details to be shown "
          + "to the annotators.  This is interpreted as HTML, and can be "
          + "used for example to show a list of clickable links extracted "
          + "from the snippet.")
  public void setDetailFeatureName(String detailFeatureName) {
    this.detailFeatureName = detailFeatureName;
  }

  public String getEntityAnnotationType() {
    return entityAnnotationType;
  }

  @RunTime
  @CreoleParameter(comment = "Annotation type representing the gold "
          + "standard annotations, i.e. the kind of entities that you want "
          + "workers to find.")
  public void setEntityAnnotationType(String entityAnnotationType) {
    this.entityAnnotationType = entityAnnotationType;
  }

  public String getEntityASName() {
    return entityASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "Annotation set where gold entities can be found")
  public void setEntityASName(String entityASName) {
    this.entityASName = entityASName;
  }

  public String getGoldFeatureName() {
    return goldFeatureName;
  }

  @RunTime
  @CreoleParameter(defaultValue = "gold", comment = "Name of a feature that marks a snippet as \"gold\"")
  public void setGoldFeatureName(String goldFeatureName) {
    this.goldFeatureName = goldFeatureName;
  }

  public String getGoldFeatureValue() {
    return goldFeatureValue;
  }

  @RunTime
  @CreoleParameter(defaultValue = "yes", comment = "Value of the feature that marks a snippet as \"gold\"")
  public void setGoldFeatureValue(String goldFeatureValue) {
    this.goldFeatureValue = goldFeatureValue;
  }

  public String getGoldReasonFeatureName() {
    return goldReasonFeatureName;
  }

  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "reason", comment = "Feature on gold snippet annotations explaining "
          + "why the snippet's entities are correct")
  public void setGoldReasonFeatureName(String goldReasonFeatureName) {
    this.goldReasonFeatureName = goldReasonFeatureName;
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

      AnnotationSet tokens =
              getDocument().getAnnotations(tokenASName)
                      .get(tokenAnnotationType);
      AnnotationSet snippetAnnotations =
              getDocument().getAnnotations(snippetASName).get(
                      snippetAnnotationType);
      AnnotationSet goldAS =
              getDocument().getAnnotations(entityASName).get(
                      entityAnnotationType);

      List<Annotation> allSnippets = Utils.inDocumentOrder(snippetAnnotations);
      fireStatusChanged("Creating CrowdFlower units for " + allSnippets.size()
              + " " + snippetAnnotationType + " annotations for "
              + entityAnnotationType + " annotation task");

      int snippetIdx = 0;
      for(Annotation snippet : allSnippets) {
        fireProgressChanged((100 * snippetIdx++) / allSnippets.size());
        if(isInterrupted()) throw new ExecutionInterruptedException();
        AnnotationSet snippetTokens =
                Utils.getContainedAnnotations(tokens, snippet);
        String detail = null;
        if(detailFeatureName != null) {
          Object detailObj = snippet.getFeatures().get(detailFeatureName);
          if(detailObj != null) {
            detail = detailObj.toString();
          }
        }
        AnnotationSet goldAnnots = null;
        String goldReason = null;
        if(goldFeatureValue.equals(snippet.getFeatures().get(goldFeatureName))) {
          goldAnnots = Utils.getContainedAnnotations(goldAS, snippet);
          if(goldReasonFeatureName != null) {
            Object goldReasonValue =
                    snippet.getFeatures().get(goldReasonFeatureName);
            if(goldReasonValue != null)
              goldReason = goldReasonValue.toString();
          }
        }

        long unitId =
                crowdFlowerClient.createAnnotationUnit(jobId, getDocument(),
                        snippetASName, snippet, detail, snippetTokens,
                        goldAnnots, goldReason);
        // store the unit ID - we use the entity annotation type as part
        // of this feature
        // name so the same sentences can hold units for different
        // annotation types
        // e.g. Person, Location, Organization
        snippet.getFeatures().put(entityAnnotationType + "_unit_id",
                Long.valueOf(unitId));
      }
      fireProcessFinished();
      fireStatusChanged(allSnippets.size() + " units created");
    } finally {
      interrupted = false;
    }
  }

  private List<Action> actions = null;

  public List<Action> getActions() {
    if(actions == null) {
      actions = new ArrayList<Action>();
      actions.add(new NewAnnotationJobAction(this));
    }
    return actions;
  }
}
