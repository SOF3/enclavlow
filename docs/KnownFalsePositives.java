class KnownFalsePositives {
	int foo(int x) {
		boolean secret = sourceMarker(1);
		if (secret) {
			return x;
		} else {
			return x;
		}
	}

	static class Ref<T> {
		T t;
	}
}
