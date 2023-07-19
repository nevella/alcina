package cc.alcina.framework.servlet.component.featuretree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.TreePath.Walker;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features.Entry;

class FeatureTable extends Model.Fields {
	static String treeName(Class<? extends Feature> clazz) {
		if (clazz == null) {
			return "0";
		} else {
			String simpleName = clazz.getSimpleName();
			return simpleName.replaceFirst("^Feature_?", "");
		}
	}

	@Directed
	FeatureTreeModel tree = new FeatureTreeModel();

	static class FeatureNode extends Tree.AbstractPathNode<FeatureNode> {
		Class<? extends Feature> feature;

		Entry entry;

		FeatureNode(FeatureNode parent, Features.Entry entry) {
			super(parent, entry == null ? "0"
					: parent.treePath + "." + treeName(entry.feature));
			if (entry != null) {
				this.feature = entry.feature;
				this.entry = entry;
				entry.children.stream().sorted().forEach(e -> {
					new FeatureNode(this, e);
				});
				getLabel().setLabel(treeName(feature));
			}
		}
	}

	static class Features {
		Map<Class<? extends Feature>, Entry> entriesByFeature;

		void generate() {
			entriesByFeature = Registry.query(Feature.class).registrations()
					.filter(c -> c != Feature.class).map(Entry::new)
					.collect(AlcinaCollectors.toKeyMap(e -> e.feature));
			entriesByFeature.values().forEach(Entry::addToParent);
		}

		class Entry implements Comparable<Entry> {
			Class<? extends Feature> feature;

			Entry parent;

			List<Entry> children = new ArrayList<>();

			Entry(Class<? extends Feature> feature) {
				this.feature = feature;
			}

			@Override
			public int compareTo(Entry o) {
				return treeName(this.feature).compareTo(treeName(o.feature));
			}

			void addToParent() {
				Class<? extends Feature> parentClass = parentClass();
				if (parentClass == null) {
				} else {
					Entry parent = entriesByFeature.get(parentClass);
					this.parent = parent;
					parent.children.add(this);
				}
			}

			Class<? extends Feature> parentClass() {
				Feature.Parent ann = Reflections.at(feature)
						.annotation(Feature.Parent.class);
				return ann == null ? null : ann.value();
			}
		}
	}

	static class FeatureTreeModel extends Tree<FeatureNode> {
		FeatureTreeModel() {
			FeatureNode root = new FeatureNode(null, null);
			setRoot(root);
			setRootHidden(true);
			generate();
		}

		void generate() {
			Features features = new Features();
			features.generate();
			List<Entry> roots = features.entriesByFeature.values().stream()
					.filter(e -> e.parent == null).sorted()
					.collect(Collectors.toList());
			roots.forEach(e -> new FeatureNode(getRoot(), e));
			Walker<? extends Tree.AbstractPathNode> walker = getRoot()
					.getTreePath().walker();
			walker.stream().forEach(n -> n.setOpen(true));
		}
	}
}
