package cc.alcina.framework.servlet.component.console.rcs;

import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.gwt.client.dirndl.model.HasClassNames;

@TypedProperties
public class RomcomSessionEntry extends Bindable.Fields
		implements TreeSerializable, Comparable<RomcomSessionEntry>,
		HasClassNames, HasStringRepresentation {
	public RomcomSessionEntry() {
	}

	@Override
	public String provideStringRepresentation() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'provideStringRepresentation'");
	}

	@Override
	public List<String> provideClassNames() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'provideClassNames'");
	}

	@Override
	public int compareTo(RomcomSessionEntry o) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'compareTo'");
	}
}
