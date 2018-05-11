package code4goal.antony.resumeparser;

import gate.*;
import gate.util.GateException;
import gate.util.Out;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import static gate.Utils.stringFor;

public class ResumeParserProgram {

    private static final HashMap<String, String> WORK_EXPERIENCES = createMap();
    private static HashMap<String, String> createMap()
    {
        HashMap<String,String> myMap = new HashMap<>();
        myMap.put("organization", "company");
        myMap.put("date_start", "start");
        myMap.put("date_end", "end");
        myMap.put("text", "description");
        myMap.put("jobtitle", "title");
        myMap.put("c", "d");
        return myMap;
    }

	private static File parseToHTMLUsingApacheTikka(String file)
			throws IOException, SAXException, TikaException {
		// determine extension
		String ext = FilenameUtils.getExtension(file);
		String outputFileFormat = "";
		// ContentHandler handler;
		if (ext.equalsIgnoreCase("html") | ext.equalsIgnoreCase("pdf")
				| ext.equalsIgnoreCase("doc") | ext.equalsIgnoreCase("docx")) {
			outputFileFormat = ".html";
			// handler = new ToXMLContentHandler();
		} else if (ext.equalsIgnoreCase("txt") | ext.equalsIgnoreCase("rtf")) {
			outputFileFormat = ".txt";
		} else {
			System.out.println("Input format of the file " + file
					+ " is not supported.");
			return null;
		}
		String OUTPUT_FILE_NAME = FilenameUtils.removeExtension(file)
				+ outputFileFormat;
		ContentHandler handler = new ToXMLContentHandler();
		// ContentHandler handler = new BodyContentHandler();
		// ContentHandler handler = new BodyContentHandler(
		// new ToXMLContentHandler());
		InputStream stream = new FileInputStream(file);
		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		try {
			parser.parse(stream, handler, metadata);
			FileWriter htmlFileWriter = new FileWriter(OUTPUT_FILE_NAME);
			htmlFileWriter.write(handler.toString());
			htmlFileWriter.flush();
			htmlFileWriter.close();
			return new File(OUTPUT_FILE_NAME);
		} finally {
			stream.close();
		}
	}

	public static JSONObject loadGateAndAnnie(File file) throws GateException,
			IOException {
//		Out.prln("Initialising basic system...");
//		Gate.init();
//		Out.prln("...basic system initialised");

		// initialise ANNIE (this may take several minutes)
		Annie annie = new Annie();
		annie.initAnnie();

		// create a GATE corpus and add a document for each command-line
		// argument
		Corpus corpus = Factory.newCorpus("Annie corpus");
		String current = new File(".").getAbsolutePath();
		URL u = file.toURI().toURL();
		FeatureMap params = Factory.newFeatureMap();
		params.put("sourceUrl", u);
		params.put("preserveOriginalContent", new Boolean(true));
		params.put("collectRepositioningInfo", new Boolean(true));
		Out.prln("Creating doc for " + u);
		Document resume = (Document) Factory.createResource(
				"gate.corpora.DocumentImpl", params);
		corpus.add(resume);

		// tell the pipeline about the corpus and run it
		annie.setCorpus(corpus);
		annie.execute();

		Iterator iter = corpus.iterator();
		JSONObject parsedJSON = new JSONObject();
		Out.prln("Started parsing...");
		// while (iter.hasNext()) {
		if (iter.hasNext()) { // should technically be while but I am just
								// dealing with one document
			JSONObject profileJSON = new JSONObject();
			Document doc = (Document) iter.next();
			AnnotationSet defaultAnnotSet = doc.getAnnotations();

			AnnotationSet curAnnSet;
			Iterator it;
			Annotation currAnnot;

			// Name
			curAnnSet = defaultAnnotSet.get("NameFinder");
			if (curAnnSet.iterator().hasNext()) { // only one name will be
													// found.
				currAnnot = (Annotation) curAnnSet.iterator().next();
				String gender = (String) currAnnot.getFeatures().get("gender");
				if (gender != null && gender.length() > 0) {
					profileJSON.put("gender", gender);
				}

				// Needed name Features
				JSONObject nameJson = new JSONObject();
				String[] nameFeatures = new String[] { "firstName",
						"middleName", "surname" };
				for (String feature : nameFeatures) {
					String s = (String) currAnnot.getFeatures().get(feature);
					if (s != null && s.length() > 0) {
						nameJson.put(feature, s);
					}
				}
				profileJSON.put("name", nameJson);
				String firstName = nameJson.get("firstName") == null ? "" : nameJson.get("firstName").toString()+" ";
				String middleName = nameJson.get("middleName") == null ? "" : nameJson.get("middleName").toString()+" ";
				String surname = nameJson.get("surname") == null ? "" : nameJson.get("surname").toString();
				profileJSON.put("cand_name",(firstName + middleName + surname).trim());
			} // name

			// title
			curAnnSet = defaultAnnotSet.get("TitleFinder");
			if (curAnnSet.iterator().hasNext()) { // only one title will be
													// found.
				currAnnot = (Annotation) curAnnSet.iterator().next();
				String title = stringFor(doc, currAnnot);
				if (title != null && title.length() > 0) {
					profileJSON.put("title", title);
				}
			}// title

			// email,address,phone,url
			String[] annSections = new String[] { "EmailFinder",
					"AddressFinder", "PhoneFinder", "URLFinder" };
			String[] annKeys = new String[] { "email", "address", "phone",
					"url" };
			for (short i = 0; i < annSections.length; i++) {
				String annSection = annSections[i];
				curAnnSet = defaultAnnotSet.get(annSection);
				it = curAnnSet.iterator();
				JSONArray sectionArray = new JSONArray();
				while (it.hasNext()) { // extract all values for each
										// address,email,phone etc..
					currAnnot = (Annotation) it.next();
					String s = stringFor(doc, currAnnot);
					if (s != null && s.length() > 0) {
						sectionArray.add(s.trim());
					}
				}
				if (sectionArray.size() > 0) {
					profileJSON.put(annKeys[i], sectionArray);
				}
			}
			if (!profileJSON.isEmpty()) {
				parsedJSON.put("basics", profileJSON);
				parsedJSON.put("email", profileJSON.get("email"));
				parsedJSON.put("phone", profileJSON.get("phone"));
				parsedJSON.put("cand_name", profileJSON.get("cand_name"));
			}

			// awards,credibility,education_items,extracurricular,misc,skills,summary
			String[] otherSections = new String[] { "summary",
					"education_items", "skills", "accomplishments",
					"awards", "credibility", "extracurricular", "misc" };
			for (String otherSection : otherSections) {
				curAnnSet = defaultAnnotSet.get(otherSection);
				it = curAnnSet.iterator();
				JSONArray subSections = new JSONArray();
				while (it.hasNext()) {
					JSONObject subSection = new JSONObject();
					currAnnot = (Annotation) it.next();
					String key = (String) currAnnot.getFeatures().get(
							"sectionHeading");
					String value = stringFor(doc, currAnnot);
					if (!StringUtils.isBlank(key)
							&& !StringUtils.isBlank(value)) {
						subSection.put(key, value.trim());
					}
					if (!subSection.isEmpty()) {
						subSections.add(subSection);
					}
				}
				if (!subSections.isEmpty()) {
					parsedJSON.put(otherSection, subSections);
					if("summary".equalsIgnoreCase(otherSection)){
						parsedJSON.put("objective", ((JSONObject)subSections.get(0)).get("SUMMARY"));
					}
				}
			}

			// work_experience
			curAnnSet = defaultAnnotSet.get("work_experience");
			it = curAnnSet.iterator();
			JSONArray workExperiences = new JSONArray();
			while (it.hasNext()) {
				JSONObject workExperience = new JSONObject();
				currAnnot = (Annotation) it.next();
				String key = (String) currAnnot.getFeatures().get(
						"sectionHeading");
				if (key.equals("work_experience_marker")) {
					// JSONObject details = new JSONObject();
					String[] annotations = new String[] { "date_start",
							"date_end", "jobtitle", "organization" };
					for (String annotation : annotations) {
						String v = (String) currAnnot.getFeatures().get(
								annotation);
						if (!StringUtils.isBlank(v)) {
							// details.put(annotation, v);
                            if("annotation".equalsIgnoreCase(annotation)){
                                annotation = "company";
                            }
							workExperience.put(WORK_EXPERIENCES.get(annotation), v.trim());
						}
					}
					// if (!details.isEmpty()) {
					// workExperience.put("work_details", details);
					// }
					key = "text";

				}
				String value = stringFor(doc, currAnnot);
				if (!StringUtils.isBlank(key) && !StringUtils.isBlank(value)) {
					workExperience.put(WORK_EXPERIENCES.get(key), value.trim());
				}
				if (!workExperience.isEmpty()) {
					workExperiences.add(workExperience);
				}

			}
			if (!workExperiences.isEmpty()) {
				parsedJSON.put("work_items", workExperiences);
			}

		}// if
		Out.prln("Completed parsing...");
        corpus.clear();
        corpus.cleanup();
        annie = null;
		return parsedJSON;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err
					.println("USAGE: java ResumeParser <inputfile> <outputfile>");
			return;
		}
		String inputFileDir = args[0];
		String outputFileDir = (args.length == 2) ? args[1]
				: "output";
		File inputDir = new File(inputFileDir);
		File outDir = new File(outputFileDir);
		if(!inputDir.exists()){
			System.err
					.println("Can not find input file dir: " + inputFileDir);
			return;
		}
		if(!outDir.exists()){
			outDir.mkdirs();
		}
        Out.prln("Initialising basic system...");
        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
        Out.prln("...basic system initialised");
		for (File file : inputDir.listFiles()){
            parseResume(file.getAbsolutePath(), outDir.getAbsolutePath()+ "/" +file.getName()+".json");
        }
	}

	private static void parseResume(String inputFileName, String outputFileName){
		try {
			File tikkaConvertedFile = parseToHTMLUsingApacheTikka(inputFileName);
			if (tikkaConvertedFile != null) {
				JSONObject parsedJSON = loadGateAndAnnie(tikkaConvertedFile);

				Out.prln("Writing to output...");
				FileWriter jsonFileWriter = new FileWriter(outputFileName);
				jsonFileWriter.write(parsedJSON.toJSONString());
				jsonFileWriter.flush();
				jsonFileWriter.close();
				Out.prln("Output written to file " + outputFileName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Sad Face :( .Something went wrong.");
			e.printStackTrace();
		}
	}
}
