/*
 * Emulation
 */
package java.lang;

public class ClassNotFoundException extends Exception {
	public ClassNotFoundException() {
		super((Throwable) null); // Disallow initCause
	}
}
