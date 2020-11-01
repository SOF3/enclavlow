class SourceSinkExample {
	@JECall
	@Sink
	int getSum(byte[] encrypted) {
		List<Integer> raw = parse(encrypted);
		return computeSum(raw);
	}

	List<Integer> parse(byte[] encrypted) {
		@Source byte[] buf = PRIVATE_KEY.decrypt(encrypted);
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
