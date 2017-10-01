package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

public abstract class DelegateMapCreator implements Serializable {
	static final transient long serialVersionUID = -1L;

	public abstract Map createDelegateMap(int depthFromRoot, int depth);

	public boolean isSorted(Map m) {
		return m instanceof SortedMap;
	}
}