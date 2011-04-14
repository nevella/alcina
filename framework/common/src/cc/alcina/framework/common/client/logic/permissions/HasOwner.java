package cc.alcina.framework.common.client.logic.permissions;

/**
 * Note,  implementations should pretty much always have the @Transient annotation on the getOwner method
 * @author nick@alcina.cc
 *
 */
public interface HasOwner {
	public IUser getOwner();

	
}
