/*
 * @(#)ThreadLocal.java	1.42 06/06/23
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package java.lang;

/**
 * Thanks to the java dudes
 */
public class ThreadLocal<T> {
	/**
	 * Creates a thread local variable.
	 */
	public ThreadLocal() {
		setInitialValue();
	}

	private T value;

	public T get() {
		return value;
	}

	/**
	 */
	private void setInitialValue() {
		value = initialValue();
	}

	public void set(T value) {
		this.value = value;
	}

	public void remove() {
		value = initialValue();
	}

	protected T initialValue() {
		return null;
	}
}
