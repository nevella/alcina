package cc.alcina.framework.jvmclient.domaintransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

/**
 * 
 * @author Nick Reddel
 * 
 *         <h2>Thread safety</h2>
 *         <p>
 *         This class can be used by a JVM client, with a non-threaded
 *         TransformManager (there will be a single instance in that case).
 *         </p>
 *         <p>
 *         The logic (if in JVM) is that mutator methods are synchronized, and
 *         collection fields are Collection.synchronized versions
 *         </p>
 */
public class CommitToStorageTransformListenerJvm
		extends CommitToStorageTransformListener {
	@Override
	protected void init() {
		priorRequestsWithoutResponse = Collections
				.synchronizedList(new ArrayList<DomainTransformRequest>());
		eventIdsToIgnore = Collections.synchronizedSet(new HashSet<Long>());
	}

	@Override
	protected synchronized void resetQueue() {
		transformQueue = Collections
				.synchronizedList(new ArrayList<DomainTransformEvent>());
	}
}
