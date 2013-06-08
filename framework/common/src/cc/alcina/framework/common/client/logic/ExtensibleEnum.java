package cc.alcina.framework.common.client.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * <h3>Example</h3> <br>
 * <div><h4>Before</h4>
 * 
 * <pre>
 * public enum PublicationFontOptions {
 * 	ARIAL, TIMES_NEW_ROMAN, COURIER, GEORGIA, ATHELAS
 * }
 * 
 * </pre>
 * 
 * <h4>After</h4>
 * 
 * <pre>
 * public abstract class PublicationFontOptions extends ExtensibleEnum {
 * 	public static final PublicationFontOptions ARIAL = new PublicationFontOptions_ARIAL();
 * 
 * 	public static final PublicationFontOptions TIMES_NEW_ROMAN = new PublicationFontOptions_TIMES_NEW_ROMAN();
 * 
 * 	public static final PublicationFontOptions COURIER = new PublicationFontOptions_COURIER();
 * 
 * 	public static final PublicationFontOptions GEORGIA = new PublicationFontOptions_GEORGIA();
 * 
 * 	public static final PublicationFontOptions ATHELAS = new PublicationFontOptions_ATHELAS();
 * 
 * 	public static class PublicationFontOptions_ARIAL extends
 * 			PublicationFontOptions {
 * 	}
 * 
 * 	public static class PublicationFontOptions_TIMES_NEW_ROMAN extends
 * 			PublicationFontOptions {
 * 	}
 * ...
 * }
 * 
 * </pre>
 * <p>
 * Yep, definition is much more verbose. <b>But...</b>
 * </p>
 * <ul>
 * <li>Usage is exactly the same (replace Class with ExtensibleEnum) - you can
 * use == rather than equals(), since there's guaranteed only one instance per
 * vm (protected constructor)<br>
 * 
 * <pre>
 * - acr.putPublicationFontOptions(PublicationFontOptions.ARIAL)
 * - ExtensibleEnum.valueOf(ContentDeliveryType.class, deliveryMode);
 * - List options = new ArrayList(ExtensibleEnum.values(clazz));
 * - if (fontOptions == PublicationFontOptions.ARIAL) {
 * </pre>
 * 
 * <li>Serialization (xml/gwt) is the only pain, you need to maintain a string
 * property, and a put/provide surrogate pair - using the FromSerializedForm and
 * ToSerializedForm converters for bindings:<br>
 * 
 * <pre>
 * ContentRequestBase
 * private String outputFormat = FormatConversionTarget.HTML.serializedForm();
 * ...
 * 
 * 	public ContentDeliveryType provideContentDeliveryType() {
 * 		return ExtensibleEnum.valueOf(ContentDeliveryType.class, deliveryMode);
 * 	}
 * 	public void putContentDeliveryType(ContentDeliveryType type) {
 * 		setDeliveryMode(type == null ? null : type.name());
 * 	}
 * 	...(binding)
 * 	binding.getChildren().add(
 * 		new Binding(form.deliveryrbl, "value",
 * 			ToSerializedFormConverter.INSTANCE, crb,
 * 			"deliveryMode", new FromSerializedFormConverter(
 * 			ContentDeliveryType.class)));
 * </pre>
 * 
 * </ul>
 * 
 * </div>
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class ExtensibleEnum {
	private static Multimap<Class<? extends ExtensibleEnum>, List<ExtensibleEnum>> superLookup = new Multimap<Class<? extends ExtensibleEnum>, List<ExtensibleEnum>>();

	private static LookupMapToMap<ExtensibleEnum> valueLookup = new LookupMapToMap<ExtensibleEnum>(
			2);

	private String key;

	public static <E extends ExtensibleEnum> E valueOf(Class<E> enumClass,
			String name) {
		return (E) valueLookup.get(enumClass, name);
	}

	public static <E extends ExtensibleEnum> List<E> values(Class<E> enumClass) {
		return (List<E>) superLookup.get(enumClass);
	}

	public ExtensibleEnum(String key) {
		this.key = key;
		Class<? extends ExtensibleEnum> registryPoint = getClass();
		if (registryPoint.getSuperclass() != ExtensibleEnum.class) {
			registryPoint = (Class<? extends ExtensibleEnum>) registryPoint
					.getSuperclass();
		}
		ExtensibleEnum existing = valueLookup.get(registryPoint,
				serializedForm());
		if (existing != null) {
			throw new RuntimeException("Duplicate xtensible enum - "
					+ serializedForm());
		}
		valueLookup.put(registryPoint, serializedForm(), this);
		superLookup.add(registryPoint, this);
	}

	protected ExtensibleEnum() {
		this(null);
	}

	public String name() {
		return serializedForm();
	}

	public String serializedForm() {
		String name = CommonUtils.simpleClassName(getClass());
		name = name.contains("_") ? name.substring(name.indexOf("_") + 1)
				: name;
		name += key == null ? "" : "$" + name;
		return name;
	}

	@Override
	public String toString() {
		return serializedForm();
	}

	// due to double-triple conversions, be lenient
	public static class ToSerializedFormConverter implements
			Converter<Object, String> {
		public static final ToSerializedFormConverter INSTANCE = new ToSerializedFormConverter();

		@Override
		public String convert(Object original) {
			return original == null ? null
					: original instanceof ExtensibleEnum ? ((ExtensibleEnum) original)
							.serializedForm() : (String) original;
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
