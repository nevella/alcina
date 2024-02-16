package com.google.gwt.regexp.shared;

public interface ISplitResult {
	String get(int index);

	int length();

	void set(int index, String value);
}
