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
	
###debugging weird f() objects in browser (exceptions)
//com.google.gwt.dev.shell.BrowserChannel.SessionHandler.ExceptionOrReturnValue.ExceptionOrReturnValue(boolean, Value)


### dev
rsync -av /private/var/local/git/alcina/framework/jscodeserver/src/cc/alcina/framework/jscodeserver/js/ /private/var/local/build/opsol/dev/app0/staging/opsol_server.ear/opsol_server.war/jscodeserver/
turn packed on/off via injectJsCodeServerFiles CrossSiteIframeLinker

519  cd /g/alcina
535  ant make-gwt-dev-jar
536  cp /private/var/local/git/alcina/dist-extras/gwt-dev-patch.jar /private/var/local/git/barnet-common-java/lib/alcina/framework/gwt/gwt-dev-patch.jar

bpx web opsol

docker cp /private/var/local/git/alcina/framework/jscodeserver/src/cc/alcina/framework/jscodeserver/js/GwtJsPlugin.js opsol.app.dev:/opt/jboss/wildfly/standalone/deployments/opsol_server.ear/opsol_server.war/jscodeserver/GwtJsPlugin.js &&
docker cp /private/var/local/git/alcina/framework/jscodeserver/src/cc/alcina/framework/jscodeserver/js/common/ opsol.app.dev:/opt/jboss/wildfly/standalone/deployments/opsol_server.ear/opsol_server.war/jscodeserver/ &&
docker cp /private/var/local/git/alcina/framework/jscodeserver/src/cc/alcina/framework/jscodeserver/js/impl/ opsol.app.dev:/opt/jboss/wildfly/standalone/deployments/opsol_server.ear/opsol_server.war/jscodeserver/

cd /private/var/local/git/alcina/framework/jscodeserver/src/cc/alcina/framework/jscodeserver/js && \
js-beautify -r * && js-beautify -r ../GwtJsPlugin.js && js-beautify -r ../impl/*