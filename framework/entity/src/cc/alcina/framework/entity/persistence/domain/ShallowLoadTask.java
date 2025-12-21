package cc.alcina.framework.entity.persistence.domain;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.Loader;

public class ShallowLoadTask<T extends Entity> {
	final static Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	public List<T> loadTable(Class<T> clazz, String sqlFilter) {
		DomainStore domainStore = DomainStore.stores().storeFor(clazz);
		Preconditions.checkState(
				domainStore.loader instanceof DomainStoreLoaderDatabase);
		Loader loader = ((DomainStoreLoaderDatabase) domainStore.loader)
				.loader();
		loader.withClazz(clazz).withSqlFilter(sqlFilter)
				.withReturnResults(true);
		try {
			return loader.loadEntities();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
