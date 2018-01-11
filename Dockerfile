FROM java:8

RUN wget -O get_pip.py "https://bootstrap.pypa.io/get-pip.py"

RUN python get_pip.py

RUN rm get_pip.py

RUN pip install flask

RUN git clone "https://github.com/antonydeepak/ResumeParser"

WORKDIR ResumeParser/ResumeTransducer

ENV GATE_HOME=/ResumeParser/GATEFiles

RUN wget -O 'parser_api.py' 'https://gist.githubusercontent.com/arpitgoyalhtmedia/1ebda74744bf0076c872b7d92b723bd6/raw/406b82dd0400f1769fe970b11b7cd218e77dd19a/parser_api.py'

RUN mkdir service_resumes

CMD python parser_api.py
