package com.google.gwt.regexp.shared;

public interface IMatchResult {
	String getGroup(int index);

	int getGroupCount();

	int getIndex();

	String getInput();
}
