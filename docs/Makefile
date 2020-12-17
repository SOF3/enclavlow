report.pdf: report.tex \
	title.tex abstract.tex abbr.tex intro.tex objective.tex \
	methodology.tex methodology-design.tex discussion.tex conclusion.tex \
	cite.bib \
	KnownFalsePositives.java \
	OopSubstAttack.java \
	sootOutput/IagoAttack.jimple \
	# ../doc-fig/build/IagoAttack.dot
	true | xelatex -halt-on-error -shell-escape report
	true | bibtex report
	true | xelatex -halt-on-error -shell-escape report
	true | xelatex -halt-on-error -shell-escape report

prst.pdf: prst.tex \
	cite.bib
	true | pdflatex -halt-on-error -shell-escape prst
	true | bibtex prst
	true | pdflatex -halt-on-error -shell-escape prst
	true | pdflatex -halt-on-error -shell-escape prst

IagoAttack.class: IagoAttack.java
	javac IagoAttack.java

sootOutput/IagoAttack.jimple: IagoAttack.class soot.jar
	java -jar soot.jar -f jimple -cp VIRTUAL_FS_FOR_JDK:. IagoAttack
	sed -i '/^$$/d' sootOutput/IagoAttack.jimple

soot.jar:
	wget https://repo1.maven.org/maven2/org/soot-oss/soot/4.2.1/soot-4.2.1-jar-with-dependencies.jar -O soot.jar
