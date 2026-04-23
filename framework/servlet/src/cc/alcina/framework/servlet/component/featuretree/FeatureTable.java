package cc.alcina.framework.servlet.component.featuretree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature.ReleaseVersion.Ref;
import cc.alcina.framework.common.client.meta.Feature.Status;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Story.Decl.Conditional.ExitOkOnFalse;
import cc.alcina.framework.gwt.client.story.Story.Point;
import cc.alcina.framework.servlet.component.featuretree.FeatureTree.Ui;

@TypedProperties
class FeatureTable extends Model.Fields {
	class Service implements ContextService {
		Features getFeatures() {
			return features;
		}
	}

	static class Features {
		class Entry implements Comparable<Entry> {
			Class<? extends Feature> feature;

			Entry parent;

			List<Entry> children = new ArrayList<>();

			Entry(Class<? extends Feature> feature) {
				this.feature = feature;
			}

			@Override
			public int compareTo(Entry o) {
				return treeName().compareTo(o.treeName());
			}

			public boolean filter(Class<? extends Feature> featureFilter) {
				if (featureFilter == null || feature == featureFilter) {
					return true;
				}
				Entry cursor = this;
				while (cursor != null) {
					if (cursor.feature == featureFilter) {
						return true;
					}
					cursor = cursor.parent;
				}
				return children.stream().anyMatch(c -> c.filter(featureFilter));
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
				List<Entry> list = children.stream()
						.sorted(Comparator.comparing(Entry::displayName))
						.collect(Collectors.toList());
				return list;
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

		Map<Class<? extends Feature>, Entry> entriesByFeature;

		List<Entry> entries = new ArrayList<>();

		Multimap<Class<? extends Feature>, List<Class<? extends Point>>> testPoints = new Multimap<>();

		Multimap<Class<? extends Feature>, List<Class<? extends Point>>> nonStandardCoveragePoints = new Multimap<>();

		public boolean hasTestCoverage(Class<? extends Feature> feature) {
			return testPoints.containsKey(feature);
		}

		public boolean
				hasNonStandardTestCoverage(Class<? extends Feature> feature) {
			return nonStandardCoveragePoints.containsKey(feature);
		}

		public List<Class<? extends Point>>
				getTestPoints(Class<? extends Feature> feature) {
			return testPoints.get(feature);
		}

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
			Registry.query(Point.class).registrations().forEach(clazz -> {
				Reflections.at(clazz).annotations(Feature.Ref.class)
						.forEach(ref -> {
							Arrays.stream(ref.value())
									.forEach(featureClass -> testPoints
											.add(featureClass, (Class) clazz));
							ExitOkOnFalse exitOk = Reflections.at(clazz)
									.annotation(
											Decl.Conditional.ExitOkOnFalse.class);
							if (exitOk != null && exitOk.value().getSimpleName()
									.equals("CheckNonStandardTestFlag")) {
								Arrays.stream(ref.value()).forEach(
										featureClass -> nonStandardCoveragePoints
												.add(featureClass,
														(Class) clazz));
							}
						});
			});
		}
	}

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

	FeaturePlace lastTablePlace;

	FeatureTable() {
		features = new Features();
		features.generate();
		bindings().from(Ui.get().subtypeProperties().place())
				.filter(this::isRequiresTableRefresh)
				.map(p -> new Table(features.entries, p.featureFilter))
				.to(properties().table()).oneWay();
	}

	@Override
	public void onNodeContext(NodeContext event) {
		event.registerService(Service.class, new Service());
	}

	PackageProperties._FeatureTable.InstanceProperties properties() {
		return PackageProperties.featureTable.instance(this);
	}

	boolean isRequiresTableRefresh(FeaturePlace place) {
		boolean result = true;
		if (lastTablePlace != null) {
			if (Objects.equals(lastTablePlace.featureFilter,
					place.featureFilter)) {
				result = false;
			}
		}
		if (result) {
			lastTablePlace = place;
		}
		return result;
	}
}
