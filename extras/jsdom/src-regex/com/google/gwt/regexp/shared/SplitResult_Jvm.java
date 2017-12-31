package com.google.gwt.regexp.shared;

public class SplitResult_Jvm implements ISplitResult {
	private final String[] result;

	public SplitResult_Jvm(String[] result) {
		this.result = result;
	}

	/**
	 * Returns one the strings split off.
	 *
	 * @param index
	 *            the index of the string to be returned.
	 * @return The index'th string resulting from the split.
	 */
	@Override
	public String get(int index) {
		return result[index];
	}

	/**
	 * Returns the number of strings split off.
	 */
	@Override
	public int length() {
		return result.length;
	}

	/**
	 * Sets (overrides) one of the strings split off.
	 *
	 * @param index
	 *            the index of the string to be set.
	 */
	@Override
	public void set(int index, String value) {
		result[index] = value;
	}
}
