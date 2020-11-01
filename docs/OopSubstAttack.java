class OopSubstAttack {
	@JECall
	public void foo(CharSequence cs) {
		@Source byte[] secret = getSecret();
		writeEncrypted(cs.substr(secret.length()));
	}
}
