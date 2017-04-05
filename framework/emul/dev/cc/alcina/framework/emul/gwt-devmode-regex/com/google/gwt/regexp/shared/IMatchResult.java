package com.google.gwt.regexp.shared;

public interface IMatchResult {

	String getInput();

	int getIndex();

	int getGroupCount();

	String getGroup(int index);
}
