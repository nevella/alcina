package cc.alcina.framework.gwt.client.util;

public class ClassNames {
	public static String addClassName(String existingClassNames,
			String className) {
		className = trimClassName(className);
		// Get the current style string.
		int idx = indexOfName(existingClassNames, className);
		// Only add the style if it's not already present.
		if (idx == -1) {
			if (existingClassNames.length() > 0) {
				return existingClassNames + " " + className;
			} else {
				return className;
			}
		} else {
			return existingClassNames;
		}
	}

	public static String removeClassName(String existingClassNames,
			String className) {
		className = trimClassName(className);
		// Get the current style string.
		int idx = indexOfName(existingClassNames, className);
		// Only add the style if it's not already present.
		if (idx != -1) {
			// Get the leading and trailing parts, without the removed name.
			String begin = existingClassNames.substring(0, idx).trim();
			String end = existingClassNames.substring(idx + className.length())
					.trim();
			// Some contortions to make sure we don't leave extra spaces.
			String newClassName;
			if (begin.length() == 0) {
				newClassName = end;
			} else if (end.length() == 0) {
				newClassName = begin;
			} else {
				newClassName = begin + " " + end;
			}
			return newClassName;
		} else {
			return existingClassNames;
		}
	}

	static String trimClassName(String className) {
		assert (className != null) : "Unexpectedly null class name";
		className = className.trim();
		assert !className.isEmpty() : "Unexpectedly empty class name";
		return className;
	}

	/**
	 * Returns the index of the first occurrence of name in a space-separated
	 * list of names, or -1 if not found.
	 *
	 * @param nameList
	 *            list of space delimited names
	 * @param name
	 *            a non-empty string. Should be already trimmed.
	 */
	public static int indexOfName(String nameList, String name) {
		int idx = nameList.indexOf(name);
		// Calculate matching index.
		while (idx != -1) {
			if (idx == 0 || nameList.charAt(idx - 1) == ' ') {
				int last = idx + name.length();
				int lastPos = nameList.length();
				if ((last == lastPos) || ((last < lastPos)
						&& (nameList.charAt(last) == ' '))) {
					break;
				}
			}
			idx = nameList.indexOf(name, idx + 1);
		}
		return idx;
	}
}
