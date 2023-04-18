/**
 * <p>
 * An implementation of the GWT typemodel backed by a running JVM typemodel
 *
 * <p>
 * I would prefer something different - the isXXX methods returning a non
 * boolean, then JPrimitive enum (not interface) and a few other features of the
 * GWT abstract typemodel are...less than ideal, IMO, but it *is* an abstract
 * typemodel.
 *
 * <p>
 * Note that all Alcina usages of the typemodel eschew JPrimitiveType, all JVM
 * types are regarded as JClassTypes
 *
 * <h3>GWT typemodel notes and examples</h3>
 *
 *
 *
 * <pre>
 * {@code
 * Example types:
 *
public static class TypeModel1<A extends List> {

 public A fld;

 public A get() {
  return null;
 }
}

public static class TypeModel2<A extends ArrayList> extends TypeModel1<A> {
}

public static class TypeModel2a extends TypeModel1<ArrayList> {
}

public static class TypeModel3 {
 TypeModel1<LinkedList> field1;

 TypeModel2 method1() {
  return null;
 }

 TypeModel<ArrayList> method2() {
  return null;
 }
}
 *

 * TypeModel3 is represented by a JRealClassType instance
 *
 * TypeModel1, TypeModel2 are represented by JGenericType instances
 *
 * JGenericType:
 * has a JRawType field ('a generic type with no type arguments')
 *
 * has a List<TypeParameter> field representing the type parameters-
 *
 * * for TypeModel1, the singleton JTypeParameter is [class A extends java.util.ArrayList]
 *
 * The fields of JTypeParameter are:
 *
 * private JClassType[] bounds;//first is class-or-interface, subsequent are interface - e.g. A extends ArrayList & Serializable
  private JGenericType declaringClass;//no docs required...
  private final int ordinal;
  private final String typeName;
 *
 * ---------------------------------------------------------------------------
 *
 * Field TypeModel3.field1 has type JParameterizedType - baseType is the JGenericType representing TypeModel1,
 * typeArg is JRealType LinkedList
 *
 * Method TypeModel1.get has return type JTypeParameter (from tye generic type)
 *
 * Note that the key for properties is that the superclass (of say TypeModel2a) is of type  TypeModel1<ArrayList> -
 * not TypeModel1<A extends List> -- which are distinct -- the latter is the *base type* of the former
 * }
 * </pre>
 *
 * ---------------------------------------------------------------------------
 *
 * <p>
 * Goals:
 *
 * <ul>
 * <li>Get properties of non-parameterized types working (used by alcina class
 * reflection) -- complete
 * <li>Get properties of parameterized types working (used by alcina class
 * reflection) -- in progress
 * <li>Completeness - see FIXME - reflection - typemodel
 * </ul>
 */
package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;
