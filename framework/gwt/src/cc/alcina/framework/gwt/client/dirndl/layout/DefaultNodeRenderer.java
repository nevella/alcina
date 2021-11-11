package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@RegistryLocation(registryPoint = DirectedNodeRenderer.class)
public class DefaultNodeRenderer extends ContainerNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node),
				() -> node.getModel() == null ? "div"
						: CommonUtils.deInfixCss(
								node.getModel().getClass().getSimpleName()));
	}
}
