package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.TypeTag;
import cc.alcina.framework.common.client.dom.DomNode.TypeTagClassName;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;

/**
 * Match a transformer to a domnode (to create a FragmentModel from a DomNode),
 * or to a FragmentModel (to create a Dirndl.Node/DomNode from a FragmentModel)
 */
class TransformerMatcher {
	FragmentModel fragmentModel;

	List<NodeTransformer> matcherTransformers;

	Map<DomNode.TypeTagClassName, NodeTransformer> tagClassnameTransformer;

	Map<DomNode.TypeTag, NodeTransformer> tagTransformer;

	Map<DomNodeType, NodeTransformer> typeTransformer;

	TransformerMatcher(FragmentModel fragmentModel) {
		this.fragmentModel = fragmentModel;
	}

	/**
	 * @return the appropriate transformer for the node
	 */
	NodeTransformer createNodeTransformer(DomNode node) {
		ensureMatcherTransformers();
		/*
		 * NodeTransformer matchingTransformer =
		 * this.matcherTransformers.stream() .filter(transformer ->
		 * transformer.appliesTo(node)).findFirst() .get();
		 * 
		 * Performance optimisation:
		 */
		NodeTransformer matchingTransformer = tagClassnameTransformer
				.get(new TypeTagClassName(node));
		if (matchingTransformer == null) {
			matchingTransformer = tagTransformer.get(new TypeTag(node));
		}
		if (matchingTransformer == null) {
			matchingTransformer = typeTransformer.get(node.getDomNodeType());
		}
		NodeTransformer matchingTransformer2 = this.matcherTransformers.stream()
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
			tagClassnameTransformer = matcherTransformers.stream()
					.collect(AlcinaCollectors
							.toKeyMap(NodeTransformer::getTypeTagClassName));
			tagTransformer = matcherTransformers.stream().collect(
					AlcinaCollectors.toKeyMap(NodeTransformer::getTypeTag));
			typeTransformer = matcherTransformers.stream().collect(
					AlcinaCollectors.toKeyMap(NodeTransformer::getType));
			tagClassnameTransformer.remove(null);
			tagTransformer.remove(null);
		}
	}
}