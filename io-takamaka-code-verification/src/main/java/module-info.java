module io.takamaka.code.verification {
	exports io.takamaka.code.verification;
	exports io.takamaka.code.verification.issues;
	requires io.takamaka.code.constants;
	requires transitive io.takamaka.code.whitelisting;
	requires transitive org.apache.bcel;
}