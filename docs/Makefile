report.pdf: report.tex \
	title.tex abstract.tex abbr.tex intro.tex objective.tex \
	methodology.tex methodology-design.tex \
	evaluation.tex discussion.tex conclusion.tex \
	cite.bib \
	KnownFalsePositives.java \
	ImplicitException.java \
	ControlFlowLeak.java \
	SelfAnonymized.java \
	OopSubstAttack.java
	true | pdflatex -halt-on-error -shell-escape report
	true | bibtex report
	true | pdflatex -halt-on-error -shell-escape report
	true | pdflatex -halt-on-error -shell-escape report

prst.pdf: prst.tex \
	cite.bib
	true | pdflatex -halt-on-error -shell-escape prst
	true | bibtex prst
	true | pdflatex -halt-on-error -shell-escape prst
	true | pdflatex -halt-on-error -shell-escape prst

soot.jar:
	wget https://repo1.maven.org/maven2/org/soot-oss/soot/4.2.1/soot-4.2.1-jar-with-dependencies.jar -O soot.jar
