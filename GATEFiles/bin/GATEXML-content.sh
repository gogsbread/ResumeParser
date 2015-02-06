#! /bin/sh

# keep only the document content part
# remove XML tags
# remove empty lines
# remove spaces starting a line
# remove spaces ending a line

cat "$1" |
tr '\n\r' ' ' |
sed -r \
 -e 's/^.*<TextWithNodes>//g' \
 -e 's/<\/TextWithNodes>.*$//g' |
sed -r \
 -e 's/<[^>]+>//g' \
 -e '/^\s*$/d' \
 -e 's/^\s+//g'\
 -e 's/\s+$//g'
