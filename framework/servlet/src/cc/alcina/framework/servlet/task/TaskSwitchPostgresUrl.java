package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskSwitchPostgresUrl extends PerformerTask {
	private String newUrl;

	private String descriptorClassName;

	public String getDescriptorClassName() {
		return this.descriptorClassName;
	}

	public String getNewUrl() {
		return this.newUrl;
	}

	@Override
	public void run() throws Exception {
		DomainStore store = DomainStore.stores().storeFor(descriptorClassName);
		store.setConnectionUrl(newUrl);
		if (store == DomainStore.writableStore()) {
			CommonPersistenceProvider.get().getCommonPersistence()
					.changeJdbcConnectionUrl(newUrl);
		}
		logger.info("Connection url changed to: {}", newUrl);
	}

	public void setDescriptorClassName(String descriptorClassName) {
		this.descriptorClassName = descriptorClassName;
	}

	public void setNewUrl(String newUrl) {
		this.newUrl = newUrl;
	}
}
