package cc.alcina.framework.servlet.environment;

import java.util.Map;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations;
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations.MutationId;

class MutationConflictResolver {
	Map<MutationId, Mutations> uncommittedRemoteMutationMessages = AlcinaCollections
			.newLinkedHashMap();
}
