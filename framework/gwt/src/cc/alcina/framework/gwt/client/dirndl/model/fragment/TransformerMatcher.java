package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;

/**
 * Match a transformer to a domnode (to create a FragmentModel from a DomNode),
 * or to a FragmentModel (to create a Dirndl.Node/DomNode from a FragmentModel)
 */
class TransformerMatcher {
	FragmentModel fragmentModel;

	List<NodeTransformer> matcherTransformers;

	TransformerMatcher(FragmentModel fragmentModel) {
		this.fragmentModel = fragmentModel;
	}

	/**
	 * @return the appropriate transformer for the node
	 */
	NodeTransformer createNodeTransformer(DomNode node) {
		ensureMatcherTransformers();
		NodeTransformer matchingTransformer = this.matcherTransformers.stream()
				.filter(transformer -> transformer.appliesTo(node)).findFirst()
				.get();
		NodeTransformer transformer = createTransformerForType(
				matchingTransformer.getFragmentNodeType());
		transformer.setNode(node);
		return transformer;
	}

	NodeTransformer createTransformerForType(
			Class<? extends FragmentNode> fragmentNodeType) {
		Class<? extends NodeTransformer> transformerClass = Reflections
				.at(fragmentNodeType).annotation(FragmentNode.Transformer.class)
				.value();
		NodeTransformer transformer = Reflections.newInstance(transformerClass);
		transformer.setFragmentNodeType(fragmentNodeType);
		transformer.setFragmentModel(fragmentModel);
		return transformer;
	}

	void ensureMatcherTransformers() {
		if (matcherTransformers == null) {
			matcherTransformers = this.fragmentModel.modelledTypes.stream()
					.map(this::createTransformerForType)
					.collect(Collectors.toList());
		}
	}
}