package com.google.gwt.dom.client;

/*
 * for compatibility with localdom2
 */
public class LocalDom {

	public static void flush() {
		LocalDomBridge.get().flush();
	}

	public static boolean fastRemoveAll = true;
}
