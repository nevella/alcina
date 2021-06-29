package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Bean;

@Bean
public class KnownsDelta implements Serializable {
	private List<KnownRenderableNode> added = new ArrayList<>();

	private List<KnownRenderableNode> removed = new ArrayList<>();

	private long timeStamp;

	private boolean clearAll;

	public List<KnownRenderableNode> getAdded() {
		return added;
	}

	public List<KnownRenderableNode> getRemoved() {
		return removed;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public boolean isClearAll() {
		return clearAll;
	}

	public void setAdded(List<KnownRenderableNode> added) {
		this.added = added;
	}

	public void setClearAll(boolean clearAll) {
		this.clearAll = clearAll;
	}

	public void setRemoved(List<KnownRenderableNode> removed) {
		this.removed = removed;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
}
