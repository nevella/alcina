package cc.alcina.framework.common.client.logic.reflection.jvm;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;

@RegistryLocation(registryPoint = AlcinaBeanSerializer.class, implementationType = ImplementationType.INSTANCE, priority = 15)
@ClientInstantiable
// FIXME - mvcc.4 - use gwt.elemental to make one single version (bar
// classloader refs)
// hack for some classloader issues causing AlcinaBeanSerializerS to be unusable
public class ReflectiveSerializer {
	// extends AlcinaBeanSerializer {
	// private static boolean useContextClassloader;
	//
	// public static boolean isUseContextClassloader() {
	// return useContextClassloader;
	// }
	//
	// public static void setUseContextClassloader(boolean
	// useContextClassloader) {
	// ReflectiveSerializer.useContextClassloader = useContextClassloader;
	// }
	//
	// private ClassLoader classLoader;
	//
	// private boolean pretty;
	//
	// IdentityHashMap seenOut = new IdentityHashMap();
	//
	// Map seenIn = new LinkedHashMap();
	//
	// private int depth = 0;
	//
	// protected Map<String, Class> resolvedClassLookup = new LinkedHashMap<>();
	//
	// public ReflectiveSerializer() {
	// propertyFieldName = PROPERTIES;
	// }
	//
	// @Override
	// public <T> T deserialize(String jsonString) {
	// try {
	// JSONObject obj = new JSONObject(jsonString);
	// if (GWT.isClient() && !useContextClassloader) {
	// // devmode
	// classLoader = getClass().getClassLoader().getParent();
	// } else {
	// classLoader = Thread.currentThread().getContextClassLoader();
	// }
	// return (T) deserializeObject(obj);
	// } catch (Exception e) {
	// throw new WrappedRuntimeException(e);
	// }
	// }
	//
	// public ReflectiveSerializer pretty() {
	// this.pretty = true;
	// return this;
	// }
	//
	// @Override
	// public String serialize(Object bean) {
	// try {
	// JSONObject jsonObject = serializeObject(bean);
	// return pretty ? jsonObject.toString(3) : jsonObject.toString();
	// } catch (Exception e) {
	// throw new WrappedRuntimeException(e);
	// }
	// }
	//
	// private Object deserializeField(Object o, Class type) throws Exception {
	// if (o == null || JSONObject.NULL.equals(o)) {
	// return null;
	// }
	// if (type == Long.class || type == long.class) {
	// return Long.valueOf(o.toString());
	// }
	// if (type == String.class) {
	// return o.toString();
	// }
	// if (type == Date.class || type == Timestamp.class) {
	// String s = o.toString();
	// if (s.contains(",")) {
	// String[] parts = s.split(",");
	// Timestamp timestamp = new Timestamp(Long.parseLong(parts[0]));
	// timestamp.setNanos(Integer.parseInt(parts[1]));
	// return timestamp;
	// } else {
	// if (type == Date.class) {
	// return new Date(Long.parseLong(s));
	// } else {
	// return new Timestamp(Long.parseLong(s));
	// }
	// }
	// }
	// if (type.isEnum()) {
	// return Enum.valueOf(type, o.toString());
	// }
	// if (type == Integer.class || type == int.class) {
	// return (int) ((Number) o).doubleValue();
	// }
	// if (type == Double.class || type == double.class) {
	// return ((Number) o).doubleValue();
	// }
	// if (type == Boolean.class || type == boolean.class) {
	// return ((Boolean) o).booleanValue();
	// }
	// if (type == Class.class) {
	// return getClassMaybeAbbreviated(o.toString());
	// }
	// if (type.isArray() && type.getComponentType() == byte.class) {
	// return Base64.decode(o.toString());
	// }
	// Collection c = null;
	// if (type == Set.class || type == LinkedHashSet.class) {
	// c = new LinkedHashSet();
	// }
	// if (type == HashSet.class) {
	// c = new HashSet();
	// }
	// if (type == ArrayList.class || type == List.class
	// || type == Collection.class) {
	// c = new ArrayList();
	// }
	// if (type == ConcurrentLinkedQueue.class || type == Queue.class) {
	// c = new ConcurrentLinkedQueue();
	// }
	// if (c != null) {
	// deserializeCollection(o, c);
	// return c;
	// }
	// Map m = null;
	// if (type == Multimap.class) {
	// return deserializeMultimap(o, new Multimap());
	// }
	// if (type == Map.class || type == LinkedHashMap.class
	// || type == ConcurrentHashMap.class) {
	// m = new LinkedHashMap();
	// }
	// if (type == HashMap.class) {
	// m = new HashMap();
	// }
	// if (type == CountingMap.class) {
	// m = new CountingMap();
	// }
	// if (m != null) {
	// return deserializeMap(o, m);
	// }
	// if (CommonUtils.isOrHasSuperClass(type, BasePlace.class)) {
	// return RegistryHistoryMapper.get().getPlace(o.toString());
	// }
	// return deserializeObject((JSONObject) o);
	// }
	//
	// private Object deserializeObject(JSONObject jsonObj) throws Exception {
	// if (jsonObj == null) {
	// return null;
	// }
	// String cn = (String) jsonObj.get(CLASS_NAME);
	// Class clazz = null;
	// try {
	// clazz = getClassMaybeAbbreviated(cn);
	// } catch (Exception e1) {
	// if (isThrowOnUnrecognisedProperty()) {
	// throw new Exception(Ax.format("class not found - %s", cn));
	// } else {
	// return null;
	// }
	// }
	// if (CommonUtils.isStandardJavaClassOrEnum(clazz)
	// || clazz == Class.class) {
	// return deserializeField(jsonObj.get(LITERAL), clazz);
	// }
	// if (jsonObj.has(REF)) {
	// Object index = jsonObj.get(REF);
	// Object object = seenIn.get(index);
	// return object;
	// }
	// JSONObject props = (JSONObject) jsonObj
	// .get(getPropertyFieldName(jsonObj));
	// Object object = Reflections.newInstance(clazz);
	// int index = seenIn.size();
	// seenIn.put(index, object);
	// String[] names = JSONObject.getNames(props);
	// if (names != null) {
	// for (String propertyName : names) {
	// Object jsonValue = props.get(propertyName);
	// PropertyDescriptor pd = SEUtilities
	// .getPropertyDescriptorByName(clazz, propertyName);
	// if (pd == null) {
	// if (isThrowOnUnrecognisedProperty()) {
	// throw new Exception(
	// Ax.format("property not found - %s.%s",
	// clazz.getSimpleName(), propertyName));
	// }
	// // ignore (we are graceful...)
	// } else {
	// Object value2 = deserializeField(jsonValue,
	// pd.getPropertyType());
	// if (value2 == null && Reflections.classLookup()
	// .isPrimitive(pd.getPropertyType())) {
	// // use default, probably a refactoring issue
	// } else {
	// try {
	// SEUtilities.setPropertyValue(object, propertyName,
	// value2);
	// } catch (NoSuchPropertyException e) {
	// }
	// }
	// }
	// }
	// }
	// return object;
	// }
	//
	// private String getPropertyFieldName(JSONObject jsonObj) {
	// return jsonObj.has(PROPERTIES_SHORT) ? PROPERTIES_SHORT : PROPERTIES;
	// }
	//
	// /**
	// * Arrays, maps, primitive collections not supported for the mo'
	// *
	// * @param value
	// * @param type
	// * @return
	// * @throws Exception
	// */
	// private Object serializeField(Object value, Class type) throws Exception
	// {
	// if (value == null) {
	// return JSONObject.NULL;
	// }
	// if (type == Object.class) {
	// type = value.getClass();
	// }
	// if (type == Long.class || type == long.class || type == String.class
	// || type.isEnum() || (type.getSuperclass() != null
	// && type.getSuperclass().isEnum())) {
	// return value.toString();
	// }
	// if (type == Double.class || type == double.class
	// || type == Integer.class || type == int.class) {
	// return (((Number) value).doubleValue());
	// }
	// if (type == Boolean.class || type == boolean.class) {
	// return ((Boolean) value);
	// }
	// if (type == Class.class) {
	// return ((Class) value).getName();
	// }
	// if (type == Date.class) {
	// return String.valueOf(((Date) value).getTime());
	// }
	// if (type == Timestamp.class) {
	// return Ax.format("%s,%s",
	// String.valueOf(((Timestamp) value).getTime()),
	// String.valueOf(((Timestamp) value).getNanos()));
	// }
	// if (type.isArray() && type.getComponentType() == byte.class) {
	// return Base64.encodeBytes((byte[]) value);
	// }
	// if (value instanceof Multimap) {
	// Multimap m = (Multimap) value;
	// return serializeMultimap(m);
	// }
	// if (value instanceof Map) {
	// Map m = (Map) value;
	// return serializeMap(m);
	// }
	// if (value instanceof Collection) {
	// Collection c = (Collection) value;
	// return serializeCollection(c);
	// }
	// if (value instanceof BasePlace) {
	// return ((BasePlace) value).toTokenString();
	// }
	// return serializeObject(value);
	// }
	//
	// private JSONObject serializeObject(Object object) throws Exception {
	// try {
	// depth++;
	// if (depth > 10) {
	// int debug = 4;
	// }
	// return serializeObject0(object);
	// } finally {
	// depth--;
	// }
	// }
	//
	// private JSONObject serializeObject0(Object object) throws Exception {
	// if (object == null) {
	// return null;
	// }
	// if (object != null
	// && !CommonUtils.isStandardJavaClassOrEnum(object.getClass())) {
	// // should implement as a refererer/referee map, otherwise those're
	// // invalid cycles
	// // if (serialized.containsKey(object)) {
	// // throwIfNonZeroFields = true;
	// // } else {
	// // serialized.put(object, object);
	// // }
	// }
	// JSONObject jo = new JSONObject();
	// Class<? extends Object> type = object.getClass();
	// if (!type.isEnum() && type.getSuperclass() != null
	// && type.getSuperclass().isEnum()) {
	// type = type.getSuperclass();
	// }
	// String typeName = type.getName();
	// typeName = normaliseReverseAbbreviation(type, typeName);
	// jo.put(CLASS_NAME, typeName);
	// Class<? extends Object> clazz = type;
	// if (CommonUtils.isStandardJavaClassOrEnum(clazz)
	// || clazz == Class.class) {
	// jo.put(LITERAL, serializeField(object, clazz));
	// return jo;
	// }
	// if (seenOut.containsKey(object)) {
	// jo.put(REF, seenOut.get(object));
	// return jo;
	// } else {
	// int index = seenOut.size();
	// seenOut.put(object, index);
	// }
	// List<PropertyDescriptor> unsortedPropertyDescriptors = SEUtilities
	// .getSortedPropertyDescriptors(clazz);
	// List<PropertyDescriptor> propertyDescriptors =
	// unsortedPropertyDescriptors
	// .stream()
	// .sorted(Comparator.comparing(PropertyDescriptor::getName))
	// .collect(Collectors.toList());
	// JSONObject props = new JSONObject();
	// jo.put(propertyFieldName, props);
	// Object template = clazz.newInstance();
	// for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
	// if (propertyDescriptor.getWriteMethod() == null
	// || propertyDescriptor.getReadMethod() == null) {
	// continue;
	// }
	// String name = propertyDescriptor.getName();
	// if (propertyDescriptor.getPropertyType()
	// .getAnnotation(AlcinaTransient.class) != null
	// || propertyDescriptor.getReadMethod()
	// .getAnnotation(AlcinaTransient.class) != null) {
	// continue;
	// }
	// Object value = propertyDescriptor.getReadMethod().invoke(object);
	// if (!CommonUtils.equalsWithNullEquality(value,
	// propertyDescriptor.getReadMethod().invoke(template))) {
	// props.put(name, serializeField(value,
	// propertyDescriptor.getPropertyType()));
	// }
	// }
	// return jo;
	// }
	//
	// protected void deserializeCollection(Object o, Collection c)
	// throws JSONException, Exception {
	// if (o instanceof JSONArray) {
	// JSONArray array = (JSONArray) o;
	// int size = array.length();
	// for (int i = 0; i < size; i++) {
	// Object jv = array.get(i);
	// if (jv == JSONObject.NULL) {
	// c.add(null);
	// } else {
	// c.add(deserializeObject((JSONObject) jv));
	// }
	// }
	// }
	// }
	//
	// protected Object deserializeMap(Object o, Map m)
	// throws JSONException, Exception {
	// JSONArray array = (JSONArray) o;
	// int size = array.length();
	// for (int i = 0; i < size; i += 2) {
	// JSONObject jv = (JSONObject) array.get(i);
	// JSONObject jv2 = (JSONObject) array.get(i + 1);
	// m.put(deserializeObject(jv), deserializeObject(jv2));
	// }
	// return m;
	// }
	//
	// protected Object deserializeMultimap(Object o, Multimap m)
	// throws JSONException, Exception {
	// JSONArray array = (JSONArray) o;
	// int size = array.length();
	// for (int i = 0; i < size; i += 2) {
	// JSONObject jv = (JSONObject) array.get(i);
	// Object o2 = array.get(i + 1);
	// ArrayList c = new ArrayList();
	// deserializeCollection(o2, c);
	// m.put(deserializeObject(jv), c);
	// }
	// return m;
	// }
	//
	// @Override
	// protected Class getClassMaybeAbbreviated(String className) {
	// try {
	// Class clazz = null;
	// if (abbrevLookup.containsKey(className)) {
	// return abbrevLookup.get(className);
	// } else {
	// Class resolved = resolvedClassLookup.get(className);
	// if (resolved == null) {
	// clazz = CommonUtils.stdAndPrimitivesMap.get(className);
	// if (clazz == null) {
	// if (GWT.isClient()) {
	// Reflections.forName(className);
	// // throw if not reflectively accessible
	// }
	// try {
	// clazz = classLoader.loadClass(className);
	// } catch (Exception e) {
	// clazz = Reflections.forName(className);
	// }
	// }
	// resolvedClassLookup.put(className, clazz);
	// return clazz;
	// } else {
	// return resolved;
	// }
	// }
	// } catch (Exception e) {
	// Ax.simpleExceptionOut(e);
	// throw new WrappedRuntimeException(e);
	// }
	// }
	//
	// protected Object serializeCollection(Collection c)
	// throws JSONException, Exception {
	// JSONArray arr = new JSONArray();
	// int i = 0;
	// for (Object o : c) {
	// arr.put(i++, serializeObject(o));
	// }
	// return arr;
	// }
	//
	// protected Object serializeMap(Map m) throws JSONException, Exception {
	// JSONArray arr = new JSONArray();
	// int i = 0;
	// for (Object o : m.entrySet()) {
	// Entry e = (Entry) o;
	// arr.put(i++, serializeObject(e.getKey()));
	// arr.put(i++, serializeObject(e.getValue()));
	// }
	// return arr;
	// }
	//
	// protected Object serializeMultimap(Multimap m)
	// throws JSONException, Exception {
	// JSONArray arr = new JSONArray();
	// int i = 0;
	// for (Object o : m.entrySet()) {
	// Entry e = (Entry) o;
	// arr.put(i++, serializeObject(e.getKey()));
	// arr.put(i++, serializeCollection((Collection) e.getValue()));
	// }
	// return arr;
	// }
}