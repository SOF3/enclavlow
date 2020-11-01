class TraditionalFalsePositive{
	Bar bar;
	Qux qux;

	Qux foo(int y) {
		this.bar.x = y;
		return this.qux;
	}

	static class Bar {
		int x;
	}

	static class Qux {
		int x;
	}
}
