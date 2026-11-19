package com.google.gwt.place.shared;

/**
 * <p>
 * Represents a bookmarkable location in an app. Implementations are expected to
 * provide correct {@link Object#equals(Object)} and {@link Object#hashCode()}
 * methods.
 * 
 * <p>
 * The type in the gwt trunk is an abstract class rather than an interface
 */
public interface Place {
	/**
	 * The null place.
	 */
	public static final Place NOWHERE = new Place() {
	};
}
