int controlFlowLeak(int secret) {
	int result = 0;
	for(int i = 0; i < secret; i++) {
		result++;
	}
	return result;
}
