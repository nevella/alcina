package cc.alcina.framework.common.client.util;

public class ProcessCounter {
	private int visited;

	private int modified;

	public void modified() {
		modified++;
	}

	@Override
	public String toString() {
		return Ax.format("%s/%s", modified, visited);
	}

	public void visited() {
		visited++;
	}
}
