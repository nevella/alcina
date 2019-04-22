###init docs
	* Eventually, linker should copy jscodeserver folder to output war
	* Initially use symlink
	* devmode adds __gwt_js_plugin
	* work with remote console
	
###list of lists
	* js plugin comms with bridge (xframe xhr)
	* bridge comms with gwt-hosted (must understand message enough to flush when complete)
	* js plugin implementation of messages
	* js plugin implementation of message handlers
		... at which point we go hoora
	* in js impl, copy structure of npapi and common
	
