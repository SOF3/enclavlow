@JECall
static void implicit(int a) {
	byte[] buffer = new byte[a];
	new Random().nextBytes(buffer);

	int index = sourceMarker(5);
	byte selection = buffer[index];

	saveData(sinkMarker(selection));
}
