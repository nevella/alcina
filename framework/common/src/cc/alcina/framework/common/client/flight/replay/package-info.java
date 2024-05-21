/*
 * @formatter:off
 * 
 * event replay (abstract)

* Replay stream
  A stream of events to replay - essentially a prior application run. The goal of event replay is to reproduce 
  application behaviour by replaying this prior run's event stream, to allow debugging of complex or 
  production event sequences.
* ReplayEventProcessor
    * The application being replayed (concretely, a service interface it provides)
* .EmissionFilter
    * filters events emitted by the ReplayEventProcessor; to prevent conflict with the dispatcher stream
* ReplayEventDispatcher
	Dispatches events from a replay stream to the ReplayEventProcessor's event queue
    * .DispatchFilter
        * filters events from the replay stream
    * .Timing
        * delays dispatch until certain conditions (e.g. ReplayEventProcessor message emissions) are met
    * dispatches events from the replay stream, 
* Note that the ReplayEventDispatcher can either be a separate process that pushes events to the ReplayEventProcessor, or 
  run as a thread on the ReplayEventProcessor

 *
 * 
 * @formatter:on
 */
package cc.alcina.framework.common.client.flight.replay;
