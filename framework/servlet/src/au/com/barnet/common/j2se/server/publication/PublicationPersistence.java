package au.com.barnet.common.j2se.server.publication;

import cc.alcina.framework.common.client.logic.permissions.IUser;
/**
 * Implemented by Jade to persist publications
 * 
 * @author nreddel@barnet.com.au
 *
 */
public interface PublicationPersistence {
	public long getNextPublicationIdForUser(IUser user);

	public static class PublicationPersistenceLocator {
		private PublicationPersistenceLocator() {
			super();
		}

		private static PublicationPersistence.PublicationPersistenceLocator theInstance;

		public static PublicationPersistence.PublicationPersistenceLocator get() {
			if (theInstance == null) {
				theInstance = new PublicationPersistence.PublicationPersistenceLocator();
			}
			return theInstance;
		}

		public void appShutdown() {
			theInstance = null;
		}

		private PublicationPersistence publicationPersistence;

		public void registerPublicationPersistence(
				PublicationPersistence publicationPersistence) {
			this.publicationPersistence = publicationPersistence;
		}

		public PublicationPersistence publicationPersistence() {
			return publicationPersistence;
		}
	}
}
