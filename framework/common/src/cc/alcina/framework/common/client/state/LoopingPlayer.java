package cc.alcina.framework.common.client.state;

/**
 * Marker to signify has successive self-async calls - for visualisation of
 * consort
 * 
 * @author nick@alcina.cc
 * 
 */
public interface LoopingPlayer {
	public String describeLoop();
}
