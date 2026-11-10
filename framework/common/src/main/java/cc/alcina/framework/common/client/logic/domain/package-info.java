/**
 * <p>
 * Base classes for entities - java representations of persistent objects
 * 
 * <h2>Notes</h2>
 * <ul>
 * <li>
 * <p>
 * Persistent entities are normally created via {@link Domain#create}, which in
 * turn calls the responsible (possibly threaded) {@link TransformManager}.
 * <p>
 * Entities *can* be created directly, in which case they're not observed by the
 * TransformManager - and not persisted. Use negative id values in that case.
 * <p>
 * TODO - explain entity uniqueness and resolution - localId, id,
 * clientInstance, class and MvccObject
 * </ul>
 */
package cc.alcina.framework.common.client.logic.domain;
