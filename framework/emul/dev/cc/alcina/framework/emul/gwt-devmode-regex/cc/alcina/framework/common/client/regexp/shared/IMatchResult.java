package cc.alcina.framework.common.client.regexp.shared;

public interface IMatchResult {
	String getInput();

	int getIndex();

	int getGroupCount();

	String getGroup(int index);
}
