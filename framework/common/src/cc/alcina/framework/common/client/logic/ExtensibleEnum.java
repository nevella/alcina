package cc.alcina.framework.common.client.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.env.INameEnvironment;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.common.client.util.Multimap;

import com.totsp.gwittir.client.beans.Converter;

/**
 * <h3>Why?</h3>
 * <ul>
 * <li>Because sometimes library enums need to be extended
 * <li>Because the registry-based loose coupling requires class objects rather
 * than enum constants (previously there were parallel
 * enum::class-multiple-subclass structures with converters, which were,
 * unbelievably, even less elegant)
 * </ul>
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class ExtensibleEnum {
	private static Map<Class<? extends ExtensibleEnum>, ExtensibleEnum> instanceLookup = new LinkedHashMap<Class<? extends ExtensibleEnum>, ExtensibleEnum>();

	private static Multimap<Class<? extends ExtensibleEnum>, List<ExtensibleEnum>> superLookup = new Multimap<Class<? extends ExtensibleEnum>, List<ExtensibleEnum>>();

	private static LookupMapToMap<ExtensibleEnum> valueLookup = new LookupMapToMap<ExtensibleEnum>(
			2);

	public static <E extends ExtensibleEnum> E valueOf(Class<E> enumClass,
			String name) {
		return (E) valueLookup.get(enumClass, name);
	}

	public static <E extends ExtensibleEnum> List<E> values(Class<E> enumClass) {
		return (List<E>) superLookup.get(enumClass);
	}

	protected ExtensibleEnum() {
		instanceLookup.put(getClass(), this);
		superLookup.add((Class<? extends ExtensibleEnum>) getClass()
				.getSuperclass(), this);
		valueLookup.put(getClass().getSuperclass(), serializedForm(), this);
	}

	public String name() {
		return serializedForm();
	}

	public String serializedForm() {
		String name = CommonUtils.simpleClassName(getClass());
		return name.contains("_") ? name.substring(name.indexOf("_") + 1)
				: name;
	}

	@Override
	public String toString() {
		return serializedForm();
	}

	public static class ToSerializedFormConverter implements
			Converter<ExtensibleEnum, String> {
		public static final ToSerializedFormConverter INSTANCE = new ToSerializedFormConverter();

		@Override
		public String convert(ExtensibleEnum original) {
			return original == null ? null : original.serializedForm();
		}
	}

	public static class FromSerializedFormConverter implements
			Converter<String, ExtensibleEnum> {
		private final Class<? extends ExtensibleEnum> enumClass;

		public FromSerializedFormConverter(
				Class<? extends ExtensibleEnum> enumClass) {
			this.enumClass = enumClass;
		}

		@Override
		public ExtensibleEnum convert(String original) {
			return ExtensibleEnum.valueOf(enumClass, original);
		}
	}
}
