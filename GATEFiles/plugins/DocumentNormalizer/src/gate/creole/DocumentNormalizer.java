/*
 * DocumentNoramlizaer.java
 *
 * Copyright (c) 2011-2013, The University of Sheffield.
 * 
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * Licensed under the GNU Library General Public License, Version 3, June 2007
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 *
 * Mark A. Greenwood, 10/11/2011
 */

package gate.creole;

import gate.Resource;
import gate.corpora.DocumentContentImpl;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.util.InvalidOffsetException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CreoleResource(name = "Document normalizer",
    comment = "Normalize document content to remove \"smart quotes\" etc.",
    helpURL = "http://gate.ac.uk/userguide/sec:misc-creole:doc-normalizer")
public class DocumentNormalizer extends AbstractLanguageAnalyser {

  private static final long serialVersionUID = -6780562970645480555L;

  private List<Replacement> replacements = new ArrayList<Replacement>();

  private URL listURL;

  private String encoding;

  @CreoleParameter(defaultValue = "resources/replacements.lst",
    comment = "the file controlling the replacements to be made")
  public void setReplacementsURL(URL listURL) {
    this.listURL = listURL;
  }

  public URL getReplacementsURL() {
    return listURL;
  }

  @CreoleParameter(defaultValue = "UTF-8",
    comment = "The encoding of the replacements file")
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public String getEncoding() {
    return encoding;
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
    if(encoding == null)
      throw new ResourceInstantiationException("Encoding must be specified!");
    if(listURL == null)
      throw new ResourceInstantiationException(
              "URL of replacements file must be specified!");

    replacements.clear();

    try {
      BufferedReader in =
              new BufferedReader(new InputStreamReader(listURL.openStream(),
                      encoding));
      String from = in.readLine();
      while(from != null) {
        String to = in.readLine();

        if(to == null)
          throw new ResourceInstantiationException("Non-Matching Replacement!");

        replacements.add(new Replacement(Pattern.compile(from), to));

        from = in.readLine();
      }
    } catch(Exception e) {
      throw new ResourceInstantiationException(e);
    }

    return this;
  }

  @Override
  public void execute() throws ExecutionException {

    try {
      for(Replacement r : replacements) {

        String docContent = document.getContent().toString();

        Matcher m = r.from.matcher(docContent);

        String replacement = r.to;
        int rl = replacement.length();

        long offset = 0;

        while(m.find()) {

          long start = m.start() + offset;
          long end = m.end() + offset;

          document.edit(start, end, new DocumentContentImpl(replacement));

          offset += rl - (m.end() - m.start());

        }
      }
    } catch(InvalidOffsetException e) {
      throw new ExecutionException(e);
    }
  }

  private static class Replacement {
    protected Pattern from;

    protected String to;

    public Replacement(Pattern from, String to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public String toString() {
      return from + " --> " + to;
    }
  }
}
