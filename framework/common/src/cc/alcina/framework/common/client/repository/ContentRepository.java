package cc.alcina.framework.common.client.repository;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;

public abstract class ContentRepository {
	protected RepositoryConnection connection;

	public ContentRepository
			withConnection(RepositoryConnection repositoryConnection) {
		this.connection = repositoryConnection;
		return this;
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());

	public String computePath(String template, Object... args) {
		String path = SEUtilities.Paths
				.ensureSlashTerminated(connection.getRepositoryPath());
		return path + Ax.format(template, args);
	}

	public abstract void put(String path, InputStream stream) throws Exception;

	public static synchronized ContentRepository
			forConnection(RepositoryConnection connection) {
		return Registry.query(ContentRepository.class)
				.addKeys(connection.getType()).impl()
				.withConnection(connection);
	}
}
