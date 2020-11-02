try {
	thisMethodThrowsSensitiveExceptions();
} catch(SensitiveException e) {
	throw sinkMarker(e);
}
