### plan
	* the proxy should be split into two endpoints, with a transport
		* proxy maintains a per-
		* debugger endpoint
			* stream interceptor
			* loop returns "packets" object to send to debuggee endpoint
				* packets has 'doHandshake' (so we don't pollute packet)
				* packets needs a debuggeeId - transport receiver assigns
			* assign logical category to packets (e.g. initial_all_threads)(packetCategories)
			* respond with cached packets if possible (packetCache)
				* packetCache will translate command ids (it has access to all packets from both endpoints) 
			* generally send as soon as we have one packet in the queue - but have a queue (endpoint decides) 
		 * debuggee endpoint
			* stream interceptor
			* packetCache translates commandids here
			* loop returns "packets" object to send to debugger endpoint
				* logical_category_handler - maybe get a lot of extra 
		* packetTransport (interface)
			* __sameProcess
			* __http
			
### eclipse timeouts
	<workspace>/.metadata/.plugins/org.eclipse.core.runtime/.settings			