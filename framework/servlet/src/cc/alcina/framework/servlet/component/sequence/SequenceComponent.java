package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.sequence.SequenceArea.Service;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.NavigateToNewSequencePlace;

/*
 */
@TypedProperties
@DirectedContextResolver
public class SequenceComponent extends Model.Fields
		implements Binding.TabIndexZero, SequenceArea.Service.Provider,
		SequenceEvents.NavigateToNewSequencePlace.Handler {
	class SequenceAreaServiceImpl implements SequenceArea.Service {
		LeafModel.DivLabel header = new LeafModel.DivLabel("header", null);

		SequenceAreaServiceImpl() {
		}

		@Override
		public Model getSequenceDefinitionHeader() {
			return header;
		}

		@Override
		public InstanceProperty<?, SequencePlace> getPlaceProperty() {
			return properties().place();
		}

		@Override
		public InstanceQuery getInstanceQuery() {
			// the query comes from the SequencePlace
			throw new UnsupportedOperationException();
		}

		@Override
		public SequenceSettings getSettings() {
			return sequenceSettings;
		}

		@Override
		public long getElementLimit() {
			return 50;
		}
	}

	@Directed
	SequenceArea sequenceArea = new SequenceArea();

	SequencePlace place;

	SequenceSettings sequenceSettings = new SequenceSettings();

	SequenceAreaServiceImpl serviceImpl = new SequenceAreaServiceImpl();

	public SequenceComponent(SequencePlace place) {
		this.place = place;
	}

	SequenceComponent() {
		this.serviceImpl = new SequenceAreaServiceImpl();
	}

	@Override
	public Service getSequenceAreaService() {
		return serviceImpl;
	}

	PackageProperties._SequenceComponent.InstanceProperties properties() {
		return PackageProperties.sequenceComponent.instance(this);
	}

	@Override
	public void onNavigateToNewSequencePlace(NavigateToNewSequencePlace event) {
		properties().place().set(event.getModel());
	}
}
