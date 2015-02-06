/*
 *  EntityAnnotationResultsImporter.java
 *
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 3, June 2007 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id: EntityAnnotationResultsImporter.java 17968 2014-05-11 16:37:34Z ian_roberts $
 */
package gate.crowdsource.ne;

import static gate.crowdsource.CrowdFlowerConstants.*;

import java.util.List;

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
import gate.util.InvalidOffsetException;

@CreoleResource(name = "Entity Annotation Results Importer",
comment = "Import judgments from a CrowdFlower job created by "
        + "the Entity Annotation Job Builder as GATE annotations.",
helpURL = "http://gate.ac.uk/userguide/sec:crowd:annotation:import")
public class EntityAnnotationResultsImporter
                                                extends
                                                  AbstractLanguageAnalyser {

  private static final long serialVersionUID = 3424823295729835240L;

  private static final Logger log = Logger
          .getLogger(EntityAnnotationResultsImporter.class);

  private String apiKey;

  private Long jobId;

  private String resultAnnotationType;

  private String resultASName;
  
  private String snippetAnnotationType;
  
  private String snippetASName;

  private String tokenAnnotationType;
  
  private String tokenASName;
  
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

  public String getResultAnnotationType() {
    return resultAnnotationType;
  }

  @RunTime
  @CreoleParameter
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

  public String getSnippetAnnotationType() {
    return snippetAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Sentence", comment = "Annotation type " +
  		"representing the snippets (one snippet = one unit)")
  public void setSnippetAnnotationType(String snippetAnnotationType) {
    this.snippetAnnotationType = snippetAnnotationType;
  }

  public String getSnippetASName() {
    return snippetASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "Annotation set where the snippets can be found")
  public void setSnippetASName(String snippetASName) {
    this.snippetASName = snippetASName;
  }

  public String getTokenAnnotationType() {
    return tokenAnnotationType;
  }

  @RunTime
  @CreoleParameter(defaultValue = "Token",
          comment = "Annotation type representing the \"tokens\" - the atomic " +
              "units that workers have selected to mark entity annotations.")
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

      AnnotationSet tokens = getDocument().getAnnotations(tokenASName).get(tokenAnnotationType);
      AnnotationSet snippetAnnotations = getDocument().getAnnotations(snippetASName)
              .get(snippetAnnotationType);
      AnnotationSet resultAS = getDocument().getAnnotations(resultASName);
      List<Annotation> allSnippets = Utils.inDocumentOrder(snippetAnnotations);
      
      for(Annotation snippet : allSnippets) {
        if(isInterrupted()) throw new ExecutionInterruptedException();
        Object unitId = snippet.getFeatures().get(resultAnnotationType + "_unit_id");
        if(unitId != null) {
          if(!(unitId instanceof Long)) {
            unitId = Long.valueOf(unitId.toString());
          }
          // find any existing result annotations within the span of this snippet
          // so we can avoid creating another annotation from this judgment if
          // one already exists
          AnnotationSet existingResults =
                  Utils.getContainedAnnotations(resultAS, snippet,
                          resultAnnotationType);
          // tokens under this snippet
          List<Annotation> snippetTokens = Utils.inDocumentOrder(
                  Utils.getContainedAnnotations(tokens, snippet));
          
          JsonArray judgments =
                  crowdFlowerClient.getJudgments(jobId,
                          ((Long)unitId).longValue());
          
          if(judgments != null) {
            for(JsonElement judgmentElt : judgments) {
              JsonObject judgment = judgmentElt.getAsJsonObject();
              JsonArray answer =
                      judgment.getAsJsonObject("data").get("answer")
                              .getAsJsonArray();
              Long judgmentId = judgment.get("id").getAsLong();
              Double trust = judgment.get("trust").getAsDouble();
              Long workerId = judgment.get("worker_id").getAsLong();
              if(answer.size() > 0) {
                // judgment says there are some entities to annotate.  Look for
                // sequences of consecutive token indices and create one result
                // annotation for each such sequence
                int startTok = 0;
                int curTok = startTok;
                while(curTok < answer.size()) {
                  // we've reached the end of a consecutive sequence if either
                  // (a) we're on the last element of answer or
                  // (b) the next element is not this+1
                  if(curTok == answer.size() - 1
                          || answer.get(curTok).getAsInt() != answer.get(curTok + 1).getAsInt()) {
                    Long startOffset = snippetTokens.get(answer.get(startTok).getAsInt()).getStartNode().getOffset();
                    Long endOffset = snippetTokens.get(answer.get(curTok).getAsInt()).getEndNode().getOffset();
                    startTok = curTok + 1;
                    // check whether there's already an annotation at this location for this judgment
                    AnnotationSet existingEntities = existingResults.getContained(startOffset, endOffset);
                    boolean found = false;
                    for(Annotation a : existingEntities) {
                      if(judgmentId.equals(a.getFeatures().get(JUDGMENT_ID_FEATURE_NAME))) {
                        found = true;
                        break;
                      }
                    }
                    if(!found) {
                      // no existing annotation found, create one
                      try {
                        resultAS.add(startOffset, endOffset, resultAnnotationType, Utils.featureMap(
                                JUDGMENT_ID_FEATURE_NAME, judgmentId,
                                "trust", trust,
                                "worker_id", workerId));
                      } catch(InvalidOffsetException e) {
                        throw new ExecutionException("Invalid offset obtained from existing annotation!", e);
                      }
                    }
                  }
                  curTok++;
                }
              }
            }
          } else {
            log.warn("Unit " + unitId + " has no judgments");
          }

        } else {
          log.warn("Found " + snippetAnnotationType + " annotation with no "
                  + UNIT_ID_FEATURE_NAME + " feature, ignoring");
        }
      }

    } finally {
      interrupted = false;
    }
  }

}
