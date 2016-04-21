package cc.alcina.framework.entity.util;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PropertyMapper.NoSuchVariantPropertyException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.entity.domaintransform.MethodIndividualPropertyAccessor;

public class JsonPropertyAccessor implements PropertyAccessor {
	public static final String TOPIC_NOTIFICATION_MULTIPLE_JSON_SINGLE_JAVA = JsonPropertyAccessor.class
			.getName() + ".TOPIC_NOTIFICATION_MULTIPLE_JSON_SINGLE_JAVA";

	public static final String CONTEXT_IGNORE_MISSING_JSON_VALUES = JsonPropertyAccessor.class
			.getName() + ".CONTEXT_IGNORE_MISSING_JSON_VALUES";

	private boolean ignoreNullWrites;

	private String defaultObjectPrefix;

	private boolean returnNullIfNotFound;

	private boolean returnJsonArray;

	public JsonPropertyAccessor() {
		this(true);
	}

	public JsonPropertyAccessor(boolean ignoreNullWrites) {
		this.ignoreNullWrites = ignoreNullWrites;
	}

	public JsonPropertyAccessor(String defaultObjectPrefix) {
		this.defaultObjectPrefix = defaultObjectPrefix;
	}

	@Override
	public IndividualPropertyAccessor cachedAccessor(Class clazz,
			String propertyName) {
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	public <T> T get(Object bean, String propertyName) {
		return (T) getPropertyValue(bean, propertyName);
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Class getPropertyType(Class clazz, String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Object getPropertyValue(Object bean, String propertyName) {
		try {
			ResolvedJson resolved = new ResolvedJson(bean, propertyName, false);
			if (resolved.invalid) {
				return maybeThrow(propertyName);
			}
			JSONObject jsonObject = resolved.leaf;
			propertyName = resolved.resolvedPropertyName;
			if (!jsonObject.has(propertyName)) {
				return maybeThrow(propertyName);
			}
			Object value = jsonObject.get(propertyName);
			if (value instanceof JSONArray &&! returnJsonArray) {
				if (((JSONArray) value).length() == 1) {
					return ((JSONArray) value).get(0);
				} else if (((JSONArray) value).length() == 0) {
					return null;
				} else {
					GlobalTopicPublisher.get().publishTopic(
							TOPIC_NOTIFICATION_MULTIPLE_JSON_SINGLE_JAVA,
							new Object[] { jsonObject, bean, propertyName });
					return ((JSONArray) value).get(0);
				}
			}
			if (value == JSONObject.NULL) {
				value = null;
			}
			return (value instanceof String && ((String) value).isEmpty())
					? null : value;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrapIfNotRuntime(e);
		}
	}

	private Object maybeThrow(String propertyName) {
		if (LooseContext.is(CONTEXT_IGNORE_MISSING_JSON_VALUES)
				|| returnNullIfNotFound) {
			return null;
		} else {
			throw new NoSuchVariantPropertyException(propertyName);
		}
	}

	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		try {
			ResolvedJson resolved = new ResolvedJson(bean, propertyName, true);
			if (resolved.invalid) {
				throw new NoSuchVariantPropertyException(propertyName);
			}
			JSONObject jsonObject = resolved.leaf;
			propertyName = resolved.resolvedPropertyName;
			if (ignoreNullWrites && value == null) {
				return;
			}
			jsonObject.put(propertyName, value);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public JsonPropertyAccessor returnNullIfNotFound() {
		this.returnNullIfNotFound = true;
		return this;
	}
	public JsonPropertyAccessor returnJsonArray() {
		this.returnJsonArray = true;
		return this;
	}

	class ResolvedJson {
		JSONObject leaf;

		String resolvedPropertyName;

		boolean invalid = false;

		public ResolvedJson(Object bean, String propertyName, boolean create)
				throws JSONException {
			leaf = (JSONObject) bean;
			resolvedPropertyName = propertyName;
			Pattern arrayPattern = Pattern.compile("(.+)\\[(\\d+)\\]");
			while (resolvedPropertyName.contains(".")) {
				int idx = resolvedPropertyName.indexOf(".");
				String part = resolvedPropertyName.substring(0, idx);
				Matcher m = arrayPattern.matcher(part);
				int arrayIndex = -1;
				if (m.matches()) {
					part = m.group(1);
					arrayIndex = Integer.parseInt(m.group(2));
				}
				if (leaf.has(part)) {
					if (leaf.get(part) instanceof JSONObject) {
						leaf = leaf.getJSONObject(part);
						resolvedPropertyName = resolvedPropertyName
								.substring(idx + 1);
					} else if (leaf.get(part) instanceof JSONArray) {
						if (arrayIndex == -1) {
							invalid = true;
							return;
						}
						JSONArray array = leaf.getJSONArray(part);
						if (array.length() == arrayIndex && create) {
							leaf = new JSONObject();
							array.put(leaf);
							resolvedPropertyName = resolvedPropertyName
									.substring(idx + 1);
						} else if (array.length() > arrayIndex) {
							leaf = array.getJSONObject(arrayIndex);
							resolvedPropertyName = resolvedPropertyName
									.substring(idx + 1);
						} else {
							invalid = true;
							return;
						}
					}
				} else if (create && defaultObjectPrefix == null) {
					if (arrayIndex == -1) {
						leaf.put(part, new JSONObject());
						leaf = leaf.getJSONObject(part);
					} else {
						JSONArray array = new JSONArray();
						leaf.put(part, array);
						leaf = new JSONObject();
						array.put(leaf);
					}
					resolvedPropertyName = resolvedPropertyName
							.substring(idx + 1);
				} else {
					if (defaultObjectPrefix != null
							&& defaultObjectPrefix.equals(part)) {
						resolvedPropertyName = resolvedPropertyName
								.substring(idx + 1);
						break;
					}
					break;
				}
			}
		}
	}
}
