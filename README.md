# ResumeParser
Resume Parser using a hybrid **machine-learning** and **rule-based** approach that focuses on semantic rather than syntactic parsing. This is a console based application

###System:###
Windows 8.1 (tested) . Should also run in Windows 7)

###Framework:###
GATE (https://gate.ac.uk/) - Open source language processing framework.<br/>
Apache Tikka (http://tika.apache.org/) - Open source format handling framework<br/>

###Pre-requisites:###
Windows<br/>
Powershell<br/>
git<br/>
Latest Java (jre8 tested)<br/>


###Installation:###
Open powershell in windows (run->powershell) <br />
1) Git clone https://github.com/antonydeepak/ResumeParser.git <br />
2) cd ResumeParser <br />
3) cd ResumeTransducer <br />
4) $env:GATE_HOME="..\GATEFiles"  *(beware: you are giving a relative path for ease.)*<br />

###Run\Test:###
	Run syntax:
	> java -cp '.\bin\*;..\GATEFiles\lib\*;..\GATEFILES\bin\gate.jar;.\lib\*' code4goal.antony.resumeparser.ResumeParserProgram <input_file> [output_file]
	
	Test:
	> java -cp '.\bin\*;..\GATEFiles\lib\*;..\GATEFILES\bin\gate.jar;.\lib\*' code4goal.antony.resumeparser.ResumeParserProgram .\UnitTests\AntonyDeepakThomas.pdf antony_thomas.json

###Parser Capabilities:###

  *Supported formats*: PDF, doc, docx, rtf, html, txt<br />
   *Supported Resume Language*: English<br />

   *Output JSON format*:
  
	>	
	{
  	"title":""
  	"gender":"",
  	"name":{
  		"first": "Antony"
  		"middle":"Deepak",
  		"last" " "Thomas"
  	}
  	"email":[],
  	"address":[]
  	"phone":[]
  	"url":[]
  	"work_experience":[{
  	  "date_start" : "",
			"jobtitle" : "",
			"organization" : "",
			"date_end" : "",
			"text" : ""
		},{
		  <section_title>:""
  	}
  	],
  	"skills":[
  		{"<section_title_from_resume>":"text"}
  	],
  	"education_and_training":[
  		{"<section_title_from_resume>":"text"}
  	],
  	"accomplishments":[
  		{"<section_title_from_resume>":"text"}
  	],
  	"awards" : [
  	  {"<section_title_from_resume>":"text"}
  	],
  	"credibility" : [
  	  {"<section_title_from_resume>":"text"}
  	],
  	"extracurricular" : [
  	  {"<section_title_from_resume>":"text"}
  	],
  	"misc" : [
  	  {"<section_title_from_resume>":"text"}
  	],
  	}
  	
###Pros###
a) Very powerful semantic parsing of resumes. I did not syntactically parse based on common styles or appearances of sections because these approaches do not scale.</br>
b) Relies on proven grammar engines (GATE) and open source projects.<br/>
		
###Everything is not perfect###
I tried my best to not blow in the face of user, but these are some gotchas:<br/>
	1) The file should have an extension in one of the supported format. I simply use the extension to determine the parser and unknown formats will be returned with error. I did not have time for MIME-type evaluation.<br/>
	2) The engine has a one-time initilization cost and technically I should be faster for subsequent files, however, I did not expose the capability to process corpus data, so it will incur the same cost for every run. <br/>
	3) There is a log4j warning at the start. Did not have time to fix that :)<br/>
	4) Page numbers are part of PDF files. Hence you would see page 1, page 2, page n every now and then. This will improve as Apache Tikka improves.<br/>
	5) Some grammar parsing especially in identifying adjectives is not on par. I did not have time to try out other NL parsers such as Stanford NLP but this is just a matter of improvement of the fundamental engine overtime.<br/>

###SourceCode structure:###
\ResumeParser<br/>
	-\ANNIEGazetterFiles<br/>
		Contains all the compiled lists for common resume section titles<br/>
	-\GATEFiles<br/>
		Contains all the GATE libraries needed for NL processing<br/>
	-\JAPEGrammars<br/>
		Contains all the JAPE grammars for resume parsing.<br/>
	-\ResumeTransducer<br/>
		Console application written in JAVA	<br/>
		
###How does the parse work?###
Parse uses the Engligh grammar engine provided by GATE through its ANNIE framework. The output is then transduced using the grammar rules and lists specifically written for resume parsing. The JAPE grammar defines a generic set of rules that complies with popular ways of resume writing. It takes Proper nouns from lists and applies them to rules to identify entities. Explore the source code and read about GATE for more details. Also, feel free to pose questions.
