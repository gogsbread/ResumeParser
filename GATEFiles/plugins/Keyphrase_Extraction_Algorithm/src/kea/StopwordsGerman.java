/*
 *    StopwordsGerman.java
 *    Copyright (C) 2001 Eibe Frank
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package kea;

import java.util.*;

/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * This list of German stop words has been obtained from
 * http://snowball.tartarus.org/german/stop.txt
 *
 * But I have deleted/changed some words that I haven't seen before.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
@SuppressWarnings({"serial","rawtypes","unchecked"})
public class StopwordsGerman extends Stopwords {
  
  /** The hashtable containing the list of stopwords */
  private static Hashtable m_Stopwords = null;

  static {
   
    if (m_Stopwords == null) {
      m_Stopwords = new Hashtable();
      Double dummy = new Double(0);

      m_Stopwords.put("aber", dummy);

      m_Stopwords.put("alle", dummy);
      m_Stopwords.put("allem", dummy);
      m_Stopwords.put("allen", dummy);
      m_Stopwords.put("aller", dummy);
      m_Stopwords.put("alles", dummy);

      m_Stopwords.put("als", dummy);
      m_Stopwords.put("also", dummy);
      m_Stopwords.put("am", dummy);
      m_Stopwords.put("an", dummy);

      m_Stopwords.put("ander", dummy);
      m_Stopwords.put("andere", dummy);
      m_Stopwords.put("anderem", dummy);
      m_Stopwords.put("anderen", dummy);
      m_Stopwords.put("anderer", dummy);
      m_Stopwords.put("anderes", dummy);
      m_Stopwords.put("anderm", dummy);
      m_Stopwords.put("andern", dummy);
      m_Stopwords.put("anders", dummy);

      m_Stopwords.put("auch", dummy);
      m_Stopwords.put("auf", dummy);
      m_Stopwords.put("aus", dummy);
      m_Stopwords.put("bei", dummy);
      m_Stopwords.put("bin", dummy);
      m_Stopwords.put("bis", dummy);
      m_Stopwords.put("bist", dummy);
      m_Stopwords.put("da", dummy);
      m_Stopwords.put("damit", dummy);
      m_Stopwords.put("dann", dummy);
      
      m_Stopwords.put("der", dummy);
      m_Stopwords.put("den", dummy);
      m_Stopwords.put("des", dummy);
      m_Stopwords.put("dem", dummy);
      m_Stopwords.put("die", dummy);
      m_Stopwords.put("das", dummy);

      m_Stopwords.put("da\u00df", dummy);

      m_Stopwords.put("derselbe", dummy);
      m_Stopwords.put("derselben", dummy);
      m_Stopwords.put("denselben", dummy);
      m_Stopwords.put("desselben", dummy);
      m_Stopwords.put("demselben", dummy);
      m_Stopwords.put("dieselbe", dummy);
      m_Stopwords.put("dieselben", dummy);
      m_Stopwords.put("dasselbe", dummy);

      m_Stopwords.put("dazu", dummy);

      m_Stopwords.put("dein", dummy);
      m_Stopwords.put("deine", dummy);
      m_Stopwords.put("deinem", dummy);
      m_Stopwords.put("deinen", dummy);
      m_Stopwords.put("deiner", dummy);
      m_Stopwords.put("deines", dummy);

      m_Stopwords.put("denn", dummy);
      
      m_Stopwords.put("derer", dummy);
      m_Stopwords.put("dessen", dummy);
      
      m_Stopwords.put("dich", dummy);
      m_Stopwords.put("dir", dummy);
      m_Stopwords.put("du", dummy);

      m_Stopwords.put("dies", dummy);
      m_Stopwords.put("diese", dummy);
      m_Stopwords.put("diesem", dummy);
      m_Stopwords.put("diesen", dummy);
      m_Stopwords.put("dieser", dummy);
      m_Stopwords.put("dieses", dummy);

      m_Stopwords.put("doch", dummy);
      m_Stopwords.put("dort", dummy);

      m_Stopwords.put("durch", dummy);

      m_Stopwords.put("ein", dummy);
      m_Stopwords.put("eine", dummy);
      m_Stopwords.put("einem", dummy);
      m_Stopwords.put("einen", dummy);
      m_Stopwords.put("einer", dummy);
      m_Stopwords.put("eines", dummy);

      m_Stopwords.put("einig", dummy);
      m_Stopwords.put("einige", dummy);
      m_Stopwords.put("einigem", dummy);
      m_Stopwords.put("einigen", dummy);
      m_Stopwords.put("einiger", dummy);
      m_Stopwords.put("einiges", dummy);

      m_Stopwords.put("einmal", dummy);

      m_Stopwords.put("er", dummy);
      m_Stopwords.put("ihn", dummy);
      m_Stopwords.put("ihm", dummy);

      m_Stopwords.put("es", dummy);
      m_Stopwords.put("etwas", dummy);

      m_Stopwords.put("euer", dummy);
      m_Stopwords.put("eure", dummy);
      m_Stopwords.put("eurem", dummy);
      m_Stopwords.put("euren", dummy);
      m_Stopwords.put("eurer", dummy);
      m_Stopwords.put("eures", dummy);

      m_Stopwords.put("f\u00fcr", dummy);
      m_Stopwords.put("gegen", dummy);
      m_Stopwords.put("gewesen", dummy);
      m_Stopwords.put("hab", dummy);
      m_Stopwords.put("habe", dummy);
      m_Stopwords.put("haben", dummy);
      m_Stopwords.put("hat", dummy);
      m_Stopwords.put("hatte", dummy);
      m_Stopwords.put("hatten", dummy);
      m_Stopwords.put("hier", dummy);
      m_Stopwords.put("hin", dummy);
      m_Stopwords.put("hinter", dummy);
      
      m_Stopwords.put("ich", dummy);
      m_Stopwords.put("mich", dummy);
      m_Stopwords.put("mir", dummy);

      m_Stopwords.put("ihr", dummy);
      m_Stopwords.put("ihre", dummy);
      m_Stopwords.put("ihrem", dummy);
      m_Stopwords.put("ihren", dummy);
      m_Stopwords.put("ihrer", dummy);
      m_Stopwords.put("ihres", dummy);
      m_Stopwords.put("euch", dummy);

      m_Stopwords.put("im", dummy);
      m_Stopwords.put("in", dummy);
      m_Stopwords.put("indem", dummy);
      m_Stopwords.put("ins", dummy);
      m_Stopwords.put("ist", dummy);

      m_Stopwords.put("jede", dummy);
      m_Stopwords.put("jedem", dummy);
      m_Stopwords.put("jeden", dummy);
      m_Stopwords.put("jeder", dummy);
      m_Stopwords.put("jedes", dummy);

      m_Stopwords.put("jene", dummy);
      m_Stopwords.put("jenem", dummy);
      m_Stopwords.put("jenen", dummy);
      m_Stopwords.put("jener", dummy);
      m_Stopwords.put("jenes", dummy);

      m_Stopwords.put("jetzt", dummy);
      m_Stopwords.put("kann", dummy);

      m_Stopwords.put("kein", dummy);
      m_Stopwords.put("keine", dummy);
      m_Stopwords.put("keinem", dummy);
      m_Stopwords.put("keinen", dummy);
      m_Stopwords.put("keiner", dummy);
      m_Stopwords.put("keines", dummy);

      m_Stopwords.put("k\u00f6nnen", dummy);
      m_Stopwords.put("k\u00f6nnte", dummy);
      m_Stopwords.put("machen", dummy);
      m_Stopwords.put("man", dummy);

      m_Stopwords.put("manche", dummy);
      m_Stopwords.put("manchem", dummy);
      m_Stopwords.put("manchen", dummy);
      m_Stopwords.put("mancher", dummy);
      m_Stopwords.put("manches", dummy);

      m_Stopwords.put("mein", dummy);
      m_Stopwords.put("meine", dummy);
      m_Stopwords.put("meinem", dummy);
      m_Stopwords.put("meinen", dummy);
      m_Stopwords.put("meiner", dummy);
      m_Stopwords.put("meines", dummy);

      m_Stopwords.put("mit", dummy);
      m_Stopwords.put("muss", dummy);
      m_Stopwords.put("musste", dummy);
      m_Stopwords.put("nach", dummy);
      m_Stopwords.put("nicht", dummy);
      m_Stopwords.put("nichts", dummy);
      m_Stopwords.put("noch", dummy);
      m_Stopwords.put("nun", dummy);
      m_Stopwords.put("nur", dummy);
      m_Stopwords.put("ob", dummy);
      m_Stopwords.put("oder", dummy);
      m_Stopwords.put("ohne", dummy);
      m_Stopwords.put("sehr", dummy);

      m_Stopwords.put("sein", dummy);
      m_Stopwords.put("seine", dummy);
      m_Stopwords.put("seinem", dummy);
      m_Stopwords.put("seinen", dummy);
      m_Stopwords.put("seiner", dummy);
      m_Stopwords.put("seines", dummy);

      m_Stopwords.put("selbst", dummy);
      m_Stopwords.put("sich", dummy);
      
      m_Stopwords.put("sie", dummy);
      m_Stopwords.put("ihnen", dummy);

      m_Stopwords.put("sind", dummy);
      m_Stopwords.put("so", dummy);

      m_Stopwords.put("solche", dummy);
      m_Stopwords.put("solchem", dummy);
      m_Stopwords.put("solchen", dummy);
      m_Stopwords.put("solcher", dummy);
      m_Stopwords.put("solches", dummy);

      m_Stopwords.put("soll", dummy);
      m_Stopwords.put("sollte", dummy);
      m_Stopwords.put("sondern", dummy);
      m_Stopwords.put("sonst", dummy);
      m_Stopwords.put("\00fcber", dummy);
      m_Stopwords.put("um", dummy);
      m_Stopwords.put("und", dummy);

      m_Stopwords.put("uns", dummy);
      m_Stopwords.put("unser", dummy);
      m_Stopwords.put("unserem", dummy);
      m_Stopwords.put("unseren", dummy);
      m_Stopwords.put("unsere", dummy);
      m_Stopwords.put("unseres", dummy);

      m_Stopwords.put("unter", dummy);
      m_Stopwords.put("viel", dummy);
      m_Stopwords.put("vom", dummy);
      m_Stopwords.put("von", dummy);
      m_Stopwords.put("vor", dummy);
      m_Stopwords.put("w\u00e4hrend", dummy);
      m_Stopwords.put("war", dummy);
      m_Stopwords.put("waren", dummy);
      m_Stopwords.put("warst", dummy);
      m_Stopwords.put("was", dummy);
      m_Stopwords.put("weg", dummy);
      m_Stopwords.put("weil", dummy);
      m_Stopwords.put("weiter", dummy);

      m_Stopwords.put("welche", dummy);
      m_Stopwords.put("welchem", dummy);
      m_Stopwords.put("welchen", dummy);
      m_Stopwords.put("welcher", dummy);
      m_Stopwords.put("welches", dummy);

      m_Stopwords.put("wenn", dummy);
      m_Stopwords.put("werde", dummy);
      m_Stopwords.put("werden", dummy);
      m_Stopwords.put("wie", dummy);
      m_Stopwords.put("wieder", dummy);
      m_Stopwords.put("will", dummy);
      m_Stopwords.put("wir", dummy);
      m_Stopwords.put("wird", dummy);
      m_Stopwords.put("wirst", dummy);
      m_Stopwords.put("wo", dummy);
      m_Stopwords.put("wollen", dummy);
      m_Stopwords.put("wollte", dummy);
      m_Stopwords.put("w\u00fcrde", dummy);
      m_Stopwords.put("w\u00fcrden", dummy);
      m_Stopwords.put("zu", dummy);
      m_Stopwords.put("zum", dummy);
      m_Stopwords.put("zur", dummy);
      m_Stopwords.put("zwar", dummy);
      m_Stopwords.put("zwischen", dummy);
    }
  }
  
  /** 
   * Returns true if the given string is a stop word.
   */
  public boolean isStopword(String str) {

    return m_Stopwords.containsKey(str.toLowerCase());
  }
}


