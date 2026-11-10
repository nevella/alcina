package cc.alcina.framework.common.client.regexp.shared;

public interface IMatchResult {
	String getGroup(int index);

	int getGroupCount();

	int getIndex();

	String getInput();
}
