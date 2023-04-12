/**
 * An implementation of the GWT typemodel backed by a running JVM typemodel
 *
 * I would prefer something different - the isXXX methods returning a non
 * boolean, then JPrimitive enum (not interface) and a few other features of the
 * GWT abstract typemodel are...less than ideal, IMO, but it *is* an abstract
 * typemodel.
 *
 * Note that all Alcina usages of the typemodel eschew JPrimitiveType, all JVM
 * types are regarded as JClassTypes
 */
package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;
