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


###Installation:###<br />
Open powershell in windows (run->powershell) <br />
1) Git clone https://github.com/antonydeepak/ResumeParser.git <br />
2) cd ResumeParser <br />
3) cd ResumeTransducer <br />
4) $env:GATE_HOME="..\GATEFiles" (beware: you are giving a relative path for ease.)<br />

###Run\Test:###<br />
	Syntax:
	> java -cp '.\bin\*;..\GATEFiles\lib\*;..\GATEFILES\bin\gate.jar;.\lib\*' code4goal.antony.resumeparser.ResumeParserProgram <input_file> [output_file]
	
	> java -cp '.\bin\\\*;..\GATEFiles\lib\\\*;..\GATEFILES\bin\gate.jar;.\lib\\\*' code4goal.antony.resumeparser.ResumeParserProgram .\UnitTests\AntonyDeepakThomas.pdf antony_thomas.json

###Parser Capabilities:###

  *Supported formats*: PDF, doc, docx, rtf, html, txt
	*Supported Resume Language*: English

	*Output JSON format*:
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
  	misc" : [
  	  {"<section_title_from_resume>":"text"}
  	],
  }
  
###How does the parse work?###<br/>

