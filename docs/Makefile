report.pdf: report.tex \
	Makefile \
	title.tex abstract.tex abbr.tex intro.tex objective.tex \
	methodology.tex methodology-design.tex discussion.tex conclusion.tex \
	KnownFalsePositives.java \
	OopSubstAttack.java \
	sootOutput/TraditionalFalsePositive.jimple
	true | xelatex -halt-on-error -shell-escape report
	true | bibtex report
	true | xelatex -halt-on-error -shell-escape report
	true | xelatex -halt-on-error -shell-escape report

TraditionalFalsePositive.class: TraditionalFalsePositive.java
	javac TraditionalFalsePositive.java

sootOutput/TraditionalFalsePositive.jimple: TraditionalFalsePositive.class soot.jar
	java -jar soot.jar -f jimple -cp VIRTUAL_FS_FOR_JDK:. TraditionalFalsePositive

soot.jar:
	wget https://repo1.maven.org/maven2/org/soot-oss/soot/4.2.1/soot-4.2.1-jar-with-dependencies.jar -O soot.jar
