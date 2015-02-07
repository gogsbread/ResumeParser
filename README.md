# ResumeParser
Resume Parser using a hybrid **machine-learning** and **rule-based** approach that focuses on semantic rather than syntactic parsing. This is a console based application

**System:**
Windows 8.1 (tested . Should also run in Windows 7)

**Framework:**
GATE (https://gate.ac.uk/) - Open source language processing framework.
Apache Tikka (http://tika.apache.org/) - Open source format handling framework

**Pre-requisites:**
Windows
Powershell
git
Latest Java (jre8 tested)


**Installation:**
Open powershell in windows (run->powershell) 
1) Git clone https://github.com/antonydeepak/ResumeParser.git 
2) cd ResumeParser 
3) cd ResumeTransducer 
4) $env:GATE_HOME="..\GATEFiles" (beware: you are giving a relative path for ease.)

**Run\Test:**
java -cp '.\bin*;..\GATEFiles\lib*;..\GATEFILES\bin\gate.jar;.\lib*' code4goal.antony.resumeparser.ResumeParserProgram .\UnitTests\AntonyDeepakThomas.pdf antony_thomas.json

How does the parse work?

Capabilities:


