#!/bin/bash
#===============================================================================
#
#          FILE:  search-code.sh
# 
#         USAGE:  search-code.sh pattern
# 
#   DESCRIPTION:  Do a grep on all the java and groovy files
#===============================================================================

grep $* `find . -type f | egrep -v '\.svn|\.swp$|CVS' \
  |egrep 'groovy$|java$|gsp$|jjt$|sh$|xml$'`

exit


SUFFIXES="java groovy sh xml jjt"

CODEDIRS=`for s in ${SUFFIXES}
do
  find . -name '*.'${s}'' |sed 's,[A-Za-z0-9_-]*\.'${s}',,g'
done |sort|uniq`

echo $CODEDIRS

