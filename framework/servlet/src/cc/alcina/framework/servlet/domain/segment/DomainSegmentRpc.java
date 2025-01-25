package cc.alcina.framework.servlet.domain.segment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.entity.persistence.domain.segment.DomainSegment;
import cc.alcina.framework.entity.persistence.domain.segment.DomainSegmentRemoteLoader;
import cc.alcina.framework.servlet.servlet.remote.RemoteServletInvoker;

/**
 * <p>
 * Main handler for DomainSegment rpc calls
 * <p>
 * Overview of the full DomainSegment flow
 * <ul>
 * <li>console starts
 * <li>console domain begins loading
 * <li>console domain routes to segmentloader.init
 * <li>segmentloader.init [optional] calls refresh from remote
 * (segmentloader.refresh)
 * <li>segmentloader.refresh gets list of existing locators/moddates for segment
 * def
 * <li>segmentloader.refresh sends locators + segment def name to remote
 * <li>segmentprojector queries (segment def) + projects
 * (segmentdef/projectionfilter)
 * <li>segmentprojector creates a DomainSegment for the response (filtered by
 * remote segment mod timestamps)
 * <li>return segment
 * <li>segmentloader merges, persists
 * <li>segmentloader creates a ConnRs
 * 
 * </ul>
 */
@Feature.Ref(Feature_DomainSegment.class)
public class DomainSegmentRpc {
	static Logger logger = LoggerFactory.getLogger(DomainSegmentRpc.class);

	public static class RemoteLoaderImpl
			implements DomainSegmentRemoteLoader.RemoteLoader {
		@Override
		public DomainSegment load(DomainSegment.Definition definition,
				DomainSegment localState) {
			TaskGetDomainSegment task = new TaskGetDomainSegment();
			task.definition = definition;
			task.localState = localState;
			task.serialize();
			logger.info(
					"Loading remote segment -- definition: {} - local state: {}",
					definition.asString(), localState);
			return Registry.impl(RemoteServletInvoker.class)
					.invokeRemoteTaskReturnResult(task);
		}
	}
}
