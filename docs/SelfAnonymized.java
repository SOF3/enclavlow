@JECall
static int foo(int a) {
	int secret = getSecret();
	a += secret;
	doSomethingWith(a);
	a -= secret;
	return a;
}
