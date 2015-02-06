#!/bin/bash

# Parameters passed to the GATE process
# This array gets populated from the command line parameters given to the 
# script. If required, you can set the initial list of parameters here. 

gateparams=()


############################################################################
#        USERS SHOULD NOT NEED TO MAKE ANY CHANGES BELOW THIS LINE         #
############################################################################

#set -x
PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
GATE_HOME=`dirname "$PRG"`/..
# make it fully qualified
# When CDPATH is set, the cd command prints out the dir name. Because of this
# wee need to execute the cd command separately, and only then get the value
# via `pwd`
cd "$GATE_HOME"
export GATE_HOME="`pwd`"
export ANT_HOME=$GATE_HOME
cd "$CURDIR"

# pull in JVM settings from the Launch4J ini file so we have a single place
# where these things can be set across operarting systems which makes things
# so much easier to document/explain/teach
IFS=$'\r\n'
vmparams=($(cat $GATE_HOME/gate.l4j.ini))
unset IFS

vmparams=( "${vmparams[@]}" "-splash:$GATE_HOME/bin/splash.png" )

DEFAULTSDIR=
tmpbase="config_files_with_this_base_should_never_really_exist_so_we_can_test_for_existence_later"

## function that copies over any config files that do not already exist
## from the DEFAULTSDIR, if the DEFAULTSDIR has been specified
## Expected parms: $1 is base name of config files to create
copy_default_files() {
  base=$1
  echo copying default files base=$base
    if [ -f "$CURDIR/$base.session" ]
    then 
      echo using existing "$CURDIR/$base.session"
    else 
      if [ -f "${DEFAULTSDIR}"/default.session ]
      then
        echo copying default session from "${DEFAULTSDIR}"/default.session to "$CURDIR/$base.session"
        cp "${DEFAULTSDIR}"/default.session "$CURDIR/$base.session"
      fi
    fi
    if [ -f "$CURDIR/$base.xml" ]
    then 
      echo using existing "$CURDIR/$base.xml"
    else 
      if [ -f "${DEFAULTSDIR}"/default.xml ]
      then
        echo copying default config from "${DEFAULTSDIR}"/default.xml to "$CURDIR/$base.xml"
        cp "${DEFAULTSDIR}"/default.xml "$CURDIR/$base.xml"
      fi
    fi
}

## function that returns, for a root directory and an absolute or 
## relative path, the absolute path.
function abs_path() {
  rootdir="$1"
  path="$2"
  case "$path" in
  /*)
    ## this is already an absolute path, use it
    path="$path"
    ;;
  *)
    path="${rootdir}"/"${path}"
    ;;
  esac
  echo "$path"
}

while test "$1" != "";
do
  case "$1" in
  -h)
    cat <<EOF
Run GATE Developer
The following options can be passed immediately after the command name:
  -ld      ... create or use the GATE default configuration and session files 
               in the current directory
  -ln name ... create or use a config file name.xml and session file name.session 
               in the current directory
  -ll      ... if the current directory contains a file log4j.properties use
               this file to configure the logging
  -rh path ... set the resources home path, this is a shortcut for 
               -Druntime.gate.user.resourceshome=path               
  -d URL   ... register the plugin at URL. Can be used multiple times.
  -i path  ... use the file at path as the site configuration file
  -dc dir  ... copy default.xml and/or default.session from this dir when 
               creating a new config or session file 
               (must come before -ld,-ld,-tmp)
  -tmp     ... use temporary config and session files (-dc option works)
  -h       ... show this help
All other options will be passed on to the "java" command, for example:
  -Djava.io.tmpdir=<somedir>
  -Xmx<memorysize>
For more information see the user manual in your GATE distribution or at
http://gate.ac.uk/userguide/
EOF
    exit 0
    ;;
  -ld)
    shift
    vmparams=( "${vmparams[@]}" "-Dgate.user.config=$(abs_path $CURDIR .gate.xml)" )
    vmparams=( "${vmparams[@]}" "-Dgate.user.session=$(abs_path $CURDIR .gate.session)" )
    vmparams=( "${vmparams[@]}" "-Dgate.user.filechooser.defaultdir=$CURDIR" )
    copy_default_files ".gate"
    ;;
  -ln)
    shift
    base=$1
    shift
    vmparams=( "${vmparams[@]}" "-Dgate.user.config=$(abs_path $CURDIR $base.xml)" )
    vmparams=( "${vmparams[@]}" "-Dgate.user.session=$(abs_path $CURDIR $base.session)" )
    vmparams=( "${vmparams[@]}" "-Dgate.user.filechooser.defaultdir=$CURDIR" )
    copy_default_files "$base"
    ;;
  -ll)
    shift
    if [ -f "$CURDIR/log4j.properties" ]
    then
      vmparams=( "${vmparams[@]}" "-Dlog4j.configuration=file://$CURDIR/log4j.properties" )
    fi
    ;;
  -rh)
    shift
    resourceshome=$1
    resourceshome=`cd "$resourceshome"; pwd -P`
    shift
    vmparams=( "${vmparams[@]}" "-Dgate.user.resourceshome=$resourceshome" )
    ;;
  -d)
    shift
    gateparams=( "${gateparams[@]}" "-d" "$1" )
    shift
    ;;
  -i)
    shift
    gateparams=( "${gateparams[@]}" "-i" "$1" )
    shift
    ;;
  -tmp)
    shift
    tmpbase=GATE$$
    vmparams=( "${vmparams[@]}" "-Dgate.user.config=$CURDIR/$tmpbase.xml" )
    vmparams=( "${vmparams[@]}" "-Dgate.user.session=$CURDIR/$tmpbase.session" )
    vmparams=( "${vmparams[@]}" "-Dgate.user.filechooser.defaultdir=$CURDIR" )
    copy_default_files "$tmpbase"
    ;;
  -dc)
    shift
    DEFAULTSDIR=$1
    shift
    ;;
  *)
    vmparams=( "${vmparams[@]}" "$1" )
    shift
    ;;
  esac
done

# Locate JAVA
if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
  elif [ -x "$JAVA_HOME/jre/bin/java" ]; then
    JAVACMD="$JAVA_HOME/jre/bin/java"
  fi
elif ( which java 2>&1 > /dev/null ); then
  JAVACMD="`which java`"
elif [ -x /usr/libexec/java_home ]; then
  # Mac OS X - use /usr/libexec/java_home -R --exec java ...
  JAVACMD=/usr/libexec/java_home
  vmparams=( "-R" "--exec" "java" "${vmparams[@]}" )
else
  echo "Couldn't find java, please set JAVA_HOME"
  exit 1
fi


echo "Running GATE using Java at $JAVACMD"
echo "$JAVACMD" "${vmparams[@]}" -jar "$GATE_HOME/bin/gateLauncher.jar" "${gateparams[@]}" 
"$JAVACMD" "${vmparams[@]}" -jar "$GATE_HOME/bin/gateLauncher.jar" "${gateparams[@]}"

## clean up temporary config files if -tmp had been specified
if [ -f "$CURDIR"/"$tmpbase".xml ]
then
  rm "$CURDIR"/"$tmpbase".xml 
fi
if [ -f "$CURDIR"/"$tmpbase".session ]
then
  rm "$CURDIR"/"$tmpbase".session 
fi
