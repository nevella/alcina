package cc.alcina.framework.servlet.domain.segment;

import cc.alcina.framework.common.client.meta.Feature;

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
}
