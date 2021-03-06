package cc.alcina.framework.common.client.logic.domain;

/**
 * Marker interface to allow entities to be keys of tx maps (since vacuum of
 * entities created in failed transactions won't normally work because the
 * entity is not visible to the vacuuming transaction)
 */
public interface InvariantOnceCreated {
}
