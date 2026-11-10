package cc.alcina.framework.common.client.regexp.shared;

public interface IRegExp {
	MatchResult exec(String input);

	boolean getGlobal();

	boolean getIgnoreCase();

	int getLastIndex();

	boolean getMultiline();

	String getSource();

	String replace(String input, String replacement);

	void setLastIndex(int lastIndex);

	SplitResult split(String input);

	SplitResult split(String input, int limit);

	boolean test(String input);
}
