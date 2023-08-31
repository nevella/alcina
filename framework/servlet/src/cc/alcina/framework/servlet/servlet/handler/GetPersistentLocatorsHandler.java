package cc.alcina.framework.servlet.servlet.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.transform.EntityLocatorMap;

public class GetPersistentLocatorsHandler {
	Map<Long, EntityLocatorMap> locatorMaps = new LinkedHashMap<>();

	public Map<EntityLocator, EntityLocator>
			handle(Set<EntityLocator> locators) {
		Map<EntityLocator, EntityLocator> result = new LinkedHashMap<>();
		locators.forEach(locator -> {
			EntityLocatorMap locatorMap = locatorMaps.computeIfAbsent(
					locator.clientInstanceId,
					clientInstanceId -> CommonPersistenceProvider.get()
							.getCommonPersistence()
							.getLocatorMap(clientInstanceId));
			EntityLocator persistentLocator = locatorMap
					.getForLocalId(locator.localId);
			result.put(locator, persistentLocator);
		});
		return result;
	}
}
