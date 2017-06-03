# ResumeParser
Parser that extracts information from any resume and converts into a structured .json format to be used by internal systems. The parser uses a **rule-based** approach that focuses on semantic rather than syntactic parsing. The parser can handle document types in .pdf, .txt, .doc and .docx (Microsoft word). In its current form, this application is a console based application.

## System
* Windows 8.1 (tested). Should also run in Windows 7 & 10)
* Mac OSX

## Framework
* GATE (https://gate.ac.uk/) - Open source language processing framework.
* Apache Tikka (http://tika.apache.org/) - Open source format handling framework

## Windows

### Pre-requisites
* Windows
* Powershell
* git
* Latest Java (jre8 tested)

### Installation
Open powershell in windows (run->powershell)

1. Git clone https://github.com/antonydeepak/ResumeParser.git
2. cd ResumeParser
3. cd ResumeTransducer
4. $env:GATE_HOME="..\GATEFiles"  *(beware: you are giving a relative path for ease.)*

### Run/Test
	Run syntax:
	> java -cp '.\bin\*;..\GATEFiles\lib\*;..\GATEFILES\bin\gate.jar;.\lib\*' code4goal.antony.resumeparser.ResumeParserProgram <input_file> [output_file]

	Test:
	> java -cp '.\bin\*;..\GATEFiles\lib\*;..\GATEFILES\bin\gate.jar;.\lib\*' code4goal.antony.resumeparser.ResumeParserProgram .\UnitTests\AntonyDeepakThomas.pdf antony_thomas.json

## Mac OSX / Linux

### Installation
Open terminal

1. `git clone https://github.com/antonydeepak/ResumeParser.git`
2. `cd ResumeParser/ResumeTransducer`
3. `export GATE_HOME="..\GATEFiles"`

### Run
```bash
java -cp 'bin/*:../GATEFiles/lib/*:../GATEFiles/bin/gate.jar:lib/*' code4goal.antony.resumeparser.ResumeParserProgram <input_file> [output_file]
```

## Parser Capabilities

* *Supported formats*: PDF, doc, docx, rtf, html, txt
* *Supported Resume Language*: English
* *Output JSON format*:

```json
{  
   "title":"",
   "gender":"",
   "name":{  
      "first":"Antony",
      "middle":"Deepak",
      "last":"Thomas"
   },
   "email":[  

   ],
   "address":[  

   ],
   "phone":[  

   ],
   "url":[  

   ],
   "work_experience":[  
      {  
         "date_start":"",
         "jobtitle":"",
         "organization":"",
         "date_end":"",
         "text":""
      },
      {  
         "<section_title>":""
      }
   ],
   "skills":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ],
   "education_and_training":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ],
   "accomplishments":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ],
   "awards":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ],
   "credibility":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ],
   "extracurricular":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ],
   "misc":[  
      {  
         "<section_title_from_resume>":"text"
      }
   ]
}
```

## Pros
1. Very powerful semantic parsing of resumes. I did not syntactically parse based on common styles or appearances of sections because these approaches do not scale.
2. Relies on proven grammar engines (GATE) and open source projects.

## Everything is not perfect
I tried my best to not blow in the face of user, but these are some gotchas:

1. The file should have an extension in one of the supported format. I simply use the extension to determine the parser and unknown formats will be returned with error. I did not have time for MIME-type evaluation.
2. The engine has a one-time initilization cost and technically I should be faster for subsequent files, however, I did not expose the capability to process corpus data, so it will incur the same cost for every run.
3. There is a log4j warning at the start. Did not have time to fix that :)
4. Page numbers are part of PDF files. Hence you would see page 1, page 2, page n every now and then. This will improve as Apache Tika improves.
5. Some grammar parsing especially in identifying adjectives is not on par. I did not have time to try out other NL parsers such as Stanford NLP but this is just a matter of improvement of the fundamental engine overtime.

## SourceCode structure
```
\ResumeParser
	-\ANNIEGazetterFiles
		Contains all the compiled lists for common resume section titles
	-\GATEFiles
		Contains all the GATE libraries needed for NL processing
	-\JAPEGrammars
		Contains all the JAPE grammars for resume parsing.
	-\ResumeTransducer<br/>
		Console application written in JAVA
```		
## How does the parse work?
Parse uses the Engligh grammar engine provided by GATE through its ANNIE framework. The output is then transduced using the grammar rules and lists specifically written for resume parsing. The JAPE grammar defines a generic set of rules that complies with popular ways of resume writing. It takes Proper nouns from lists and applies them to rules to identify entities. Explore the source code and read about GATE for more details. Also, feel free to pose questions.
