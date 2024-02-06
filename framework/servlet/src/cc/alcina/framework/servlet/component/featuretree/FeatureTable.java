package cc.alcina.framework.servlet.component.featuretree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature.ReleaseVersion.Ref;
import cc.alcina.framework.common.client.meta.Feature.Status;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

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
	Table table;

	Features features;

	FeatureTable() {
		features = new Features();
		features.generate();
		table = new Table(features.entries);
	}

	static class Features {
		Map<Class<? extends Feature>, Entry> entriesByFeature;

		List<Entry> entries = new ArrayList<>();

		void generate() {
			entriesByFeature = Registry.query(Feature.class).registrations()
					.filter(c -> c != Feature.class).map(Entry::new)
					.collect(AlcinaCollectors.toKeyMap(e -> e.feature));
			entriesByFeature.values().forEach(Entry::addToParent);
			List<Entry> roots = entriesByFeature.values().stream()
					.filter(e -> e.parent == null).sorted()
					.collect(Collectors.toList());
			for (Entry entry : roots) {
				new DepthFirstTraversal<>(entry, Entry::sortedChildren).stream()
						.forEach(entries::add);
			}
		}

		class Entry implements Comparable<Entry> {
			Class<? extends Feature> feature;

			Entry parent;

			List<Entry> children = new ArrayList<>();

			Entry(Class<? extends Feature> feature) {
				this.feature = feature;
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

			@Override
			public int compareTo(Entry o) {
				return treeName().compareTo(o.treeName());
			}

			int depth() {
				int depth = 0;
				Entry cursor = this;
				while (cursor.parent != null) {
					depth++;
					cursor = cursor.parent;
				}
				return depth;
			}

			String displayName() {
				String name = treeName();
				if (parent != null) {
					String parentName = FeatureTable.treeName(parent.feature);
					if (name.startsWith(parentName)) {
						name = name.substring(parentName.length());
					}
				}
				name = name.replaceFirst("^_", "");
				String[] parts = name.split("_");
				if (parts.length > 2) {
					name = parts[0] + "..." + parts[parts.length - 1];
				}
				name = name.replace("_", "");
				name = CommonUtils.deInfix(name);
				return name;
			}

			Class<? extends Feature> parentClass() {
				Feature.Parent ann = Reflections.at(feature)
						.annotation(Feature.Parent.class);
				return ann == null ? null : ann.value();
			}

			Class<? extends Feature.ReleaseVersion> releaseVersion() {
				Ref ref = Reflections.at(feature)
						.annotation(Feature.ReleaseVersion.Ref.class);
				return ref == null ? null : ref.value();
			}

			List<Entry> sortedChildren() {
				return children.stream().collect(Collectors.toList());
			}

			Class<? extends Status> status() {
				Feature.Status.Ref ann = Reflections.at(feature)
						.annotation(Feature.Status.Ref.class);
				return ann == null ? null : ann.value();
			}

			String treeName() {
				return FeatureTable.treeName(feature);
			}
		}
	}
}
