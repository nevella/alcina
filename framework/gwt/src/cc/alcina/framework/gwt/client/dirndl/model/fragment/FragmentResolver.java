package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;

public class FragmentResolver extends ContextResolver
		implements NodeTransformer.Provider {
	FragmentModel fragmentModel;

	@Override
	public NodeTransformer createNodeTransformer(org.w3c.dom.Node w3cNode) {
		return fragmentModel.createNodeTransformer(w3cNode);
	}

	@Override
	protected void init(ContextResolver parent, DirectedLayout layout,
			Object rootModel) {
		super.init(parent, layout, rootModel);
		fragmentModel = ((FragmentModel.Has) rootModel).provideFragmentModel();
	}
}
