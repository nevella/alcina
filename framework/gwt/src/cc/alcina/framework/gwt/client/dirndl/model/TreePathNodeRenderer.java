package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.TextNodeRenderer;

@Registration({ DirectedNodeRenderer.class, TreePath.class })
public class TreePathNodeRenderer extends TextNodeRenderer {
	public TreePathNodeRenderer() {
		int debug = 3;
	}
}
