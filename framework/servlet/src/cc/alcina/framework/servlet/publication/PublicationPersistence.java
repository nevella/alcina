package cc.alcina.framework.servlet.publication;

import cc.alcina.framework.common.client.logic.permissions.IUser;

/**
 * Implemented by Jade to persist publications
 * 
 * @author nick@alcina.cc
 * 
 */
public interface PublicationPersistence {
	public long getNextPublicationIdForUser(IUser user);
}
