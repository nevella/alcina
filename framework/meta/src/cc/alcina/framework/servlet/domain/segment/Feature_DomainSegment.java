package cc.alcina.framework.servlet.domain.segment;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature.Type;

/**
 * <p>
 * This feature provides support for local load of a remote domain segment. It
 * comprises a sync system, which syncs a local json file containing the remote
 * segment entity property breakdowns with the server graph, and a loader
 * variant which loads the local domain from the remote file
 * 
 * <p>
 * The implementation is outlined in the servlet DomainSegmentRpc package
 */
@Type.Ref(Type.Backend_model.class)
public interface Feature_DomainSegment extends Feature {
}
