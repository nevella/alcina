package com.google.gwt.regexp.shared;

import java.util.ArrayList;
import java.util.List;

public class MatchResult_Jvm implements IMatchResult {
	private final List<String> groups;

	private final int index;

	private final String input;

	public MatchResult_Jvm(int index, String input, List<String> groups) {
		this.index = index;
		this.input = input;
		this.groups = new ArrayList<String>(groups);
	}

	/**
	 * Retrieves the matched string or the given matched group.
	 *
	 * @param index
	 *            the index of the group to return, 0 to return the whole
	 *            matched string; must be between 0 and
	 *            {@code getGroupCount() - 1} included
	 * @return The matched string if {@code index} is zero, else the given
	 *         matched group. If the given group was optional and did not match,
	 *         the behavior is browser-dependent: this method will return
	 *         {@code null} or an empty string.
	 */
	@Override
	public String getGroup(int index) {
		return groups.get(index);
	}

	/**
	 * Returns the number of groups, including the matched string hence greater
	 * or equal than 1.
	 */
	@Override
	public int getGroupCount() {
		return groups.size();
	}

	/**
	 * Returns the zero-based index of the match in the input string.
	 */
	@Override
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the original input string.
	 */
	@Override
	public String getInput() {
		return input;
	}
}
