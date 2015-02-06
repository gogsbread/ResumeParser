#!/usr/bin/perl

# Script to read all the creole.xml files for every plugin
# to produce a summary HTML page (GATE/doc/plugins.html)
# by Andrew Golightly
#
# DO NOT RUN THIS SCRIPT FROM THE COMMAND LINE, use "ant plugins.html" in the
# top-level GATE directory instead.

use strict;
use warnings;
use XML::Simple;
use XML::XPath;
use XML::XPath::XMLParser;
use File::Find;

if(!@ARGV || $ARGV[0] ne "runningFromAnt") {
  print "This script should not be run directly.  Instead, you should do\n";
  print "\"ant plugins.html\" in the top-level GATE directory.\n";
  exit 1;
}

# ********** Some constants **********
my $internalPluginsTitle = "Plugins included in the GATE distribution";
my $externalPluginsTitle = "Other contributed plugins";

# Grab all the creole filenames for all the plugins
my @creoleFileList = ();
File::Find::find(
    sub {
      push (@creoleFileList, $File::Find::name) if $_ eq 'creole.xml';
    },
    qw(../build/plugins));

# Sort alphabetically, case insensitive
@creoleFileList = sort {uc($a) cmp uc($b)} @creoleFileList;

# **************************************************

print "Extracting information on GATE plugins\n";
print "--------------------------------------\n\n";

# ********** Write HTML for the top of the plugins page **********
# Open file handle to the HTML file we are creating
my $htmlFilename = '../doc/plugins.html';
open(HTMLFILE , ">:utf8", $htmlFilename) || die("Cannot Open File $htmlFilename");

print HTMLFILE <<ENDHTML;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!--
****** This page is generated automatically by plugins-info-to-HTML.pl. ******
****** Do not edit it manually.  To modify the external plugins list at ******
****** the bottom of this page edit external-plugins.html and run       ******
****** "ant plugins.html" from the top-level GATE directory.            ******

-->
<html>

<head>
<title>List of plugins available to GATE</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<link rel="stylesheet" type="text/css" href="gate.css">
<style type="text/css">
	a img {border: none;}
	th {background-color: #A0D0F0;}
</style>
</head>
<body>
<center>
	<a href="http://gate.ac.uk/"><img src="http://www.gate.ac.uk/gateHeader.gif" alt="GATE" height="76" width="356"/></a>
</center>
<br/>
<!-- top banner ****************************************************** -->
<div class="banner">
	<p>Plugins for GATE</p>
</div>

<p>This page lists some of the plugins that are currently available with GATE:</p>
<ul>
	<li><a href="#internal-plugins">
ENDHTML

print HTMLFILE "$internalPluginsTitle";

print HTMLFILE <<ENDHTML;
	</a></li>
	<li><a href="#external-plugins">
ENDHTML

print HTMLFILE $externalPluginsTitle,
	<<ENDHTML;
	
	</a></li>
</ul>

<p>For more information on how the plugins work, see the online user guide "<a href="http://gate.ac.uk/sale/tao/#sec:howto:plugins">Developing Language Processing Components with GATE</a>".</p>
<p>To submit a plugin, please contact us via the <a
href="http://www.gate.ac.uk/mail/index.html">gate-users mailing
list</a>.</p>
<hr/>
ENDHTML
# **************************************************

# ********** Write internal plugin information to the HTML file **********
print "Extracting internal plugins information..\n";
print HTMLFILE "<a name='internal-plugins'></a>\n",
				"<h2>$internalPluginsTitle</h2>\n",
				"<ul type='circle'>";

my @creoleFileData = ();

foreach my $creoleFileName (@creoleFileList)
{
	$creoleFileName =~ /plugins\/(.+)\/creole.xml/;
   	my $xp = XML::XPath->new(filename => $creoleFileName); # parse the XML file
    my $nodeset = $xp->find('//RESOURCE'); 	# find all resources in this creole.xml file..
        my @nodes = $nodeset->get_nodelist;
        # Ignore plugins that do not define any RESOURCEs
        if(@nodes) {
                print HTMLFILE "<li><a href='#$1'>$1</a></li>\n";
                push @creoleFileData, { NAME => $1,
                                        DATA => $nodeset,
                                        XPATH => $xp };
        }
}

print HTMLFILE "</ul>\n",
				"<table border='1'>\n";

# foreach plugin creole.xml file...
foreach my $creoleFile (@creoleFileData)
{
	my $creoleFileName = $creoleFile->{NAME};
        print "$creoleFileName\n";
        print HTMLFILE "\t<tr>\n\t\t<th colspan='3'><a name='$creoleFileName'>$creoleFileName</a></th>\n\t</tr>\n";
        foreach my $node ($creoleFile->{DATA}->get_nodelist) 
        {
                my $creoleFragment = XML::XPath::XMLParser::as_string($node);
                print HTMLFILE "\t<tr>\n";
                
                # NAME
                print HTMLFILE "\t\t<td>", $creoleFile->{XPATH}->findvalue('NAME', $node), "&nbsp;</td>\n";

                # COMMENT and HELPURL
                print HTMLFILE "\t\t<td>", $creoleFile->{XPATH}->findvalue('COMMENT', $node);
                if($creoleFile->{XPATH}->exists('HELPURL', $node)) {
                        print HTMLFILE " (<a href=\"", $creoleFile->{XPATH}->findvalue('HELPURL', $node), "\">docs</a>)";
                }

                print HTMLFILE "&nbsp;</td>\n";

                # CLASS
                print HTMLFILE "\t\t<td>", $creoleFile->{XPATH}->findvalue('CLASS', $node), "</td>\n";

                print HTMLFILE "\t</tr>\n";
        }
}

print HTMLFILE "</table>\n",
				"<hr/>\n";    
print ".. all internal plugin information extracted.\n\n";
# **************************************************

# ********** Include external-plugins.html page **********
print "Importing external plugins information ... ";
print HTMLFILE "<a name='external-plugins'></a>\n",
	"<h2>$externalPluginsTitle</h2>\n";
my $externalPluginsFilename = '../doc/external-plugins.html';
open(EXTERNALHTMLFILE , "<$externalPluginsFilename") || die("Cannot Open File $externalPluginsFilename");
while(<EXTERNALHTMLFILE>)
{
	print HTMLFILE;
}
close(EXTERNALHTMLFILE);
print "done!\n";
# **************************************************

# ********** Write the footer images of the plugins page **********
print HTMLFILE <<ENDHTML;
<div class="banner">| <a href="../../index.html">gate home</a> |</div>
<table width="100%">
	<tr>
		<td>
			<a href="http://nlp.shef.ac.uk/"><img src="http://www.gate.ac.uk/revNlpLogo.jpg" width="164" height="60" alt="NLP group"/></a>
		</td>
		<td align="right">
			<img src="http://www.gate.ac.uk/nlpTitle.gif" width="250" height="18" alt="NLP Group"/>
			<br/>
			<img src="http://www.gate.ac.uk/redline.jpg" width="500" height="17" alt="kewl red line"/>
		</td>
	</tr>
</table>	
</body>
</html>

ENDHTML

close(HTMLFILE);
# **************************************************

print "\nAll done. ($htmlFilename created)\n";
