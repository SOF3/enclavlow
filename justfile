test-plugin:
	./gradlew publishToMavenLocal
	cd example && ./gradlew enclavlow --debug --stacktrace 2>&1 | grep -P 'enclavlow\.plugin|BuildExceptionReporter'
