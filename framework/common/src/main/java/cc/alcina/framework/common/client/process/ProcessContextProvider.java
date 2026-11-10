package cc.alcina.framework.common.client.process;

import cc.alcina.framework.common.client.process.TreeProcess.Node;

public interface ProcessContextProvider {
	String flatPosition(Node node);
}