package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel.ModelMutation;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.AttributeMappingType;

@AttributeMappingType.Ref(AttributeMappingType.SINGLE)
@Registration.Self
public abstract class FragmentDocumentRoot extends FragmentDocumentNode
		implements ModelMutation.Handler {
	@Binding(type = Type.PROPERTY)
	public FragmentDocumentVersion version;

	@Binding(type = Type.PROPERTY)
	public String persistentId;

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		fragmentModel.onBind(event);
	}

	public void setFragmentModel(FragmentModel fragmentModel) {
		this.fragmentModel = fragmentModel;
	}

	@Override
	public void onModelMutation(ModelMutation event) {
		document.onModelMutation(event);
	}

	@Override
	protected String toStringContents() {
		FormatBuilder format = new FormatBuilder().separator(" - ");
		format.format("version: %s", version);
		DomNode domNode = domNode();
		if (domNode == null) {
			format.append("[not linked]");
		} else {
			format.append(super.toStringContents());
		}
		return format.toString();
	}

	public String provideNodeName() {
		return "doc";
	}

	public String provideCssSelector() {
		return Ax.format("%s[persistent-id='%s']", provideNodeName(),
				persistentId);
	}
}
