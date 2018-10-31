package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class KnownsDelta implements Serializable {
	public List<KnownRenderableNode> added = new ArrayList<>();

	public List<KnownRenderableNode> removed = new ArrayList<>();

	public long timeStamp;

	public boolean clearAll;
}
