class KnownFalsePositives {
	/**
	 * This is a false positive due to multiple return arms.
	 *
	 * The CFG contains an edge (secret -> Return).
	 * But in fact, Return is always 1.
	 * This is considered a minor use case not to be fixed,
	 * because duplicated code in multiple return arms
	 * is typically regarded as an antipattern anyway.
	 *
	 * However, this pattern is checked as a special case
	 * if multiple arms return the same literal,
	 * including string literals, int literals and boolean literals,
	 * but not for class constant literals.
	 */
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
