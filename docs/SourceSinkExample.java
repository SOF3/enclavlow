class SourceSinkExample {
	@JECall
	int getSum(byte[] encrypted) {
		List<Integer> raw = parse(encrypted);
		return sinkMarker(computeSum(raw));
	}

	List<Integer> parse(byte[] encrypted) {
		byte[] buf = sourceMarker(PRIVATE_KEY.decrypt(encrypted));
		List<Integer> result = new ArrayList<>();
		for(byte i : buf) {
			result.add((int) i);
		}
		return result;
	}

	int computeSum(List<Integer> integers) {
		int sum = 0;
		for(int i : integers) {
			sum += i;
		}
		return sum;
	}
}
