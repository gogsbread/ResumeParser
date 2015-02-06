#! /bin/sh

if [ "$1" = "" -o "$2" = "" -o "$1" = "--help" -o "$2" = "--help" ]; then
  cat << eof

  Extract statistics from a Gate XML document file.

  Usage: $0 gate_document.xml annotation_set_name [feature_name]

  To run on a directory use:
  for f in \`ls directory\`; do
    sh $0 directory/\$f annotation_set_name;
  done > results.txt

eof
  exit
fi

file=`basename $1`

# keep only the document content part
# remove XML tags
# remove empty lines
# remove spaces starting a line
# remove spaces ending a line
# calculate number of lines, words and characters
# format the result

cat "$1" |
tr '\n\r' ' ' |
sed -r \
 -e 's/^.*<TextWithNodes>//g' \
 -e 's/<\/TextWithNodes>.*$//g' |
sed -r \
 -e 's/<[^>]+>//g' \
 -e '/^\s*$/d' \
 -e 's/^\s+//g'\
 -e 's/\s+$//g' |
wc --lines --words --chars |
sed -r -e 's/^\s*([0-9]+)\s+([0-9]+)\s+([0-9]+)$/'$file' _Lines_ \1\n'$file' _Words_ \2\n'$file' _Characters_ \3/'

echo $file _AnnotationSet_ $2

# keep only the annotation set given in second parameter
# put one annotation type per line
# optionnaly get the name of the feature given in third parameter
# remove all other XML tag
# sort lines
# count each annotation type [and feature]
# format the result
cat "$1" |
tr '\n\r' ' ' |
sed -r \
 -e 's/^.*<AnnotationSet Name="'$2'">//g' \
 -e 's/<\/AnnotationSet>.*$//g' \
 -e 's/<Annotation [^>]+Type="([^"]+)" [^>]+>/\n\1/g' |
sed -r \
 -e 's/\s+<Feature>.+<Name [^>]+>('$3')<\/Name>\s+<Value [^>]+>([^<]+).+$/ \1=\2/g' \
 -e 's/\s+<Feature>.+$//g' \
 -e 's/\s+<\/Annotation>\s+//g' \
 -e 's/^<\?xml .+$/This annotation set do not exist !!!/g' \
 -e '/^\s*$/d' |
sort --field-separator=' ' --key=1,1 --key=2,2 |
uniq --count |
sort --reverse --numeric-sort |
sed -r -e 's/^\s*([0-9]+)\s+(.+)$/'$file' \2 \1/g'
