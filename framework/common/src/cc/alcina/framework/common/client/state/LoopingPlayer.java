package cc.alcina.framework.common.client.state;

/**
 * Marker to signify has successive self-async calls - for visualisation of
 * consort, and to ease pre-loop initialisation for the player
 * 
 * @author nick@alcina.cc
 * 
 */
public interface LoopingPlayer {
	public String describeLoop();
	
	public void loop();
}
