/*
 *  EntityClassificationResultsImporter.java
 *
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 3, June 2007 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id: EntityClassificationResultsImporter.java 17968 2014-05-11 16:37:34Z ian_roberts $
 */
package gate.crowdsource.classification;

import static gate.crowdsource.CrowdFlowerConstants.*;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

@CreoleResource(name = "Entity Classification Results Importer", comment = "Import judgments from a CrowdFlower job created by "
        + "the Entity Classification Job Builder as GATE annotations.",
    helpURL = "http://gate.ac.uk/userguide/sec:crowd:classification:import")
public class EntityClassificationResultsImporter
                                                extends
                                                  AbstractLanguageAnalyser {

  private static final long serialVersionUID = -4933088333206339292L;

  private static final Logger log = Logger
          .getLogger(EntityClassificationResultsImporter.class);

  private String apiKey;

  private Long jobId;

  private String entityAnnotationType;

  private String entityASName;

  private String resultAnnotationType;

  private String resultASName;

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

  public String getResultAnnotationType() {
    return resultAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Mention")
  public void setResultAnnotationType(String resultAnnotationType) {
    this.resultAnnotationType = resultAnnotationType;
  }

  public String getResultASName() {
    return resultASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "crowdResults")
  public void setResultASName(String resultASName) {
    this.resultASName = resultASName;
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

      AnnotationSet allEntities =
              getDocument().getAnnotations(entityASName).get(
                      entityAnnotationType);
      AnnotationSet resultAS = getDocument().getAnnotations(resultASName);
      for(Annotation entity : allEntities) {
        Object unitId = entity.getFeatures().get(UNIT_ID_FEATURE_NAME);
        if(unitId != null) {
          if(!(unitId instanceof Long)) {
            unitId = Long.valueOf(unitId.toString());
          }
          AnnotationSet existingJudgments =
                  Utils.getContainedAnnotations(resultAS, entity,
                          resultAnnotationType);
          JsonArray judgments =
                  crowdFlowerClient.getJudgments(jobId,
                          ((Long)unitId).longValue());
          if(judgments != null) {
            for(JsonElement judgmentElt : judgments) {
              JsonObject judgment = judgmentElt.getAsJsonObject();
              String answer =
                      judgment.getAsJsonObject("data").get("answer")
                              .getAsString();
              long judgmentId = judgment.get("id").getAsLong();
              double trust = judgment.get("trust").getAsDouble();
              long workerId = judgment.get("worker_id").getAsLong();
              Annotation judgmentAnn =
                      findOrCreate(resultAS, existingJudgments, judgmentId,
                              entity);
              judgmentAnn.getFeatures().put("answer", answer);
              judgmentAnn.getFeatures().put("trust", Double.valueOf(trust));
              judgmentAnn.getFeatures()
                      .put("worker_id", Long.valueOf(workerId));
            }
          } else {
            log.warn("Unit " + unitId + " has no judgments");
          }

        } else {
          log.warn("Found " + entityAnnotationType + " annotation with no "
                  + UNIT_ID_FEATURE_NAME + " feature, ignoring");
        }
      }

    } finally {
      interrupted = false;
    }
  }

  protected Annotation findOrCreate(AnnotationSet resultAS,
          AnnotationSet existingJudgments, Long judgmentId, Annotation entity) {
    Annotation judgment = null;
    // look for an existing judgment annotation with the right ID
    for(Annotation existing : existingJudgments) {
      if(judgmentId
              .equals(existing.getFeatures().get(JUDGMENT_ID_FEATURE_NAME))) {
        judgment = existing;
        break;
      }
    }
    // if not found, create one and return it
    if(judgment == null) {
      Integer id =
              Utils.addAnn(resultAS, entity, resultAnnotationType,
                      Utils.featureMap(JUDGMENT_ID_FEATURE_NAME, judgmentId));
      judgment = resultAS.get(id);
    }

    return judgment;
  }

}
