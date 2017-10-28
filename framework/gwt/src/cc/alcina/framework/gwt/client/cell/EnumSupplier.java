package cc.alcina.framework.gwt.client.cell;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

public class EnumSupplier<E extends Enum> implements Supplier<Collection> {
	private Class<E> clazz;

	public EnumSupplier(Class<E> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Collection get() {
		return Arrays.asList(clazz.getEnumConstants());
	}
}
