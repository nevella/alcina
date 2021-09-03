package cc.alcina.framework.common.client.logic.reflection.jvm;

/**
 * Intermediate base class for all problems encountered when processing
 * (parsing, generating) JSON content that are not pure I/O problems. Regular
 * {@link java.io.IOException}s will be passed through as is. Sub-class of
 * {@link java.io.IOException} for convenience.
 */
public class JsonProcessingException extends java.io.IOException {
	public JsonProcessingException(Throwable rootCause) {
		super();
	}
}