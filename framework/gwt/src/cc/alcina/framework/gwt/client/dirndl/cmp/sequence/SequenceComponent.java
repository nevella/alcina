package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceArea.Service;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.NavigateToNewSequencePlace;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 */
@TypedProperties
@DirectedContextResolver
public class SequenceComponent extends Model.Fields
		implements Binding.TabIndexZero, SequenceArea.Service.Provider,
		SequenceEvents.NavigateToNewSequencePlace.Handler,
		HasFilteredSequenceElements {
	class SequenceAreaServiceImpl implements SequenceArea.Service {
		SequenceAreaServiceImpl() {
		}

		@Override
		public Model getSequenceDefinitionHeader() {
			return header;
		}

		@Override
		public InstanceProperty<?, SequencePlace> getPlaceProperty() {
			return sequencePlaceProperty;
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

	SequenceSettings sequenceSettings = new SequenceSettings();

	SequenceAreaServiceImpl serviceImpl = new SequenceAreaServiceImpl();

	Model header;

	InstanceProperty<?, SequencePlace> sequencePlaceProperty;

	public SequenceComponent(Model header,
			InstanceProperty<?, SequencePlace> sequencePlaceProperty) {
		this.header = header;
		this.sequencePlaceProperty = sequencePlaceProperty;
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
		sequencePlaceProperty.set(event.getModel());
	}

	@Override
	public List<?> provideFiltereedSequenceElements() {
		return sequenceArea.provideFiltereedSequenceElements();
	}
}
