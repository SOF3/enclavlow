class IagoAttack {
	@JECall
	public void foo(CharSequence cs) {
		byte[] secret = sourceMarker(new byte[0]});
		writeEncrypted(cs.substr(secret.length()));
	}
}
