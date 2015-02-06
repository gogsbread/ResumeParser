A simple PR to allow for basic document normalization. Should usually be run as
the first PR in a pipeline after Document Reset. The PR edits the document
content and so once it has been run over a document once, future executions
will have no effect although will require processing time.

The PR works from a file of replacements. Essentially this file consists of
pairs of lines. The first line specifics the text to replace, while the second
line signifies what will be substituted in its place. The first line can be a
regular expression, but back references cannot be used within the second line.

The most common use for this PR is to normalise punctuation symbols as WYSIWYG
editors often automatically replace standard apostrophe and hyphen symbols with
more fancy versions. This makes processing text difficult as gazetteer lists,
JAPE grammars and other resources usually assume the use of the standard
symbols, i.e. the ones on the keyboard. The default config file is aimed at
normalizing such cases.
