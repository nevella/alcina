package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.widget.SimpleWidget;

public class LeafNodeRenderer extends DirectedNodeRenderer {
	public static final Object OBJECT_INSTANCE = new Object();

	@Override
	public Widget render(Node node) {
		String tag = getTag(node);
		Preconditions.checkArgument(Ax.notBlank(tag));
		return new SimpleWidget(tag);
	}

	protected String getTag(Node node) {
		if (node.model instanceof HasTag) {
			return ((HasTag) node.model).provideTag();
		}
		return node.directed.tag();
	}

	protected String getTagPossiblyFromFieldName(Node node,
			String defaultValue) {
		if (node.parent != null && node.parent.has(PropertyNameTags.class)
				&& node.property.getName() != null) {
			return CommonUtils.deInfixCss(node.property.getName());
		}
		if (node.model instanceof HasTag) {
			return ((HasTag) node.model).provideTag();
		}
		return Ax.blankTo(node.directed.tag(), defaultValue);
	}
}
