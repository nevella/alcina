package cc.alcina.framework.entity.persistence.cache.descriptor;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.entity.persistence.cache.DomainStoreDescriptor;

public class DomainDescriptorJob {
	private Class<? extends Job> jobImplClass;

	public void configureDescriptor(DomainStoreDescriptor descriptor) {
		jobImplClass = AlcinaPersistentEntityImpl.getImplementation(Job.class);
		descriptor.addClassDescriptor(jobImplClass, "key");
		descriptor.addClassDescriptor(AlcinaPersistentEntityImpl
				.getImplementation(JobRelation.class));
	}
}
