package cc.alcina.framework.servlet.component.featuretree;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.FilterAscent;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features.Entry;
import cc.alcina.framework.servlet.component.featuretree.FeatureTree.Ui;

@TypedProperties
class Properties extends Model.All {
	Heading header = new Heading("Properties");

	FeatureProperties properties;

	PackageProperties._Properties.InstanceProperties properties() {
		return PackageProperties.properties.instance(this);
	}

	Properties() {
		from(Ui.get().subtypeProperties().place())
				.map(place -> new FeatureProperties(place.feature))
				.to(properties().properties()).oneWay();
	}

	@Directed.Transform(Tables.Single.class)
	class FeatureProperties extends Model.Fields
			implements FilterAscent.Handler {
		String parent;

		String name;

		String internalName;

		String status;

		String depends;

		/*
		 * not yet - needs the full annotationresolver history kit
		 * 
		 * @Directed.TransformElements(LeafTransforms.ToNestedName.class)
		 * List<Class<? extends Point>> tests;
		 */
		String tests;

		Link link = Link.of(ModelEvents.FilterAscent.class);

		@Property.Not
		Class<? extends Feature> feature;

		FeatureProperties(Class<? extends Feature> feature) {
			this.feature = feature;
			Features features = Properties.this
					.service(FeatureTable.Service.class).getFeatures();
			Entry entry = features.entriesByFeature.get(feature);
			if (entry == null) {
				return;
			}
			parent = entry.parent == null ? null : entry.parent.displayName();
			name = entry.displayName();
			internalName = entry.feature.getSimpleName();
			status = entry.status() == null ? ""
					: entry.status().getSimpleName().toLowerCase().replace("_",
							" ");
			tests = Optional.ofNullable(features.getTestPoints(feature))
					.orElse(List.of()).stream().map(NestedName::get)
					.collect(Collectors.joining(", "));
		}

		@Override
		public void onFilterAscent(FilterAscent event) {
			FeaturePlace to = FeatureTree.place().copy();
			to.featureFilter = feature;
			to.go();
		}
	}
}
