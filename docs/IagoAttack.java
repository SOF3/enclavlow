class IagoAttack{
	Bar bar;
	Qux qux;

	Qux foo(int x) {
		this.bar.x = x;
		return this.qux;
	}

	static class Bar {
		int x;
	}

	static class Qux {
		int x;
	}
}
