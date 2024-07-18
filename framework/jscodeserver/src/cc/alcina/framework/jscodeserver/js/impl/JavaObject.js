//just statics - we need a function with extras
class gwt_hm_JavaObject { }
gwt_hm_JavaObject.isInstance = function (javaObject) {
	return (javaObject) && javaObject.hasOwnProperty("__gwt_java_object_id");
}
gwt_hm_JavaObject.getJavaObjectId = function (javaObject) {
	return javaObject.__gwt_java_object_id;
}
gwt_hm_JavaObject.dispatch = function (javaObject, sourceArguments) {
	var args = [];
	for (var idx = 2; idx < sourceArguments.length; idx++) {
		args.push(sourceArguments[idx]);
	}
	var dispId = sourceArguments[0];
	var objectId = gwt_hm_JavaObject.getJavaObjectId(javaObject);
	var thisObj = sourceArguments[1];
	if (gwt_hm_JavaObject.isInstance(thisObj)) {
		objectId = gwt_hm_JavaObject.getJavaObjectId(thisObj);
	}
	return javaObject.__gwt_plugin.javaObjectInvoke(objectId, dispId, args, args.length);
}
gwt_hm_JavaObject.propertyDispatcher = {
	set: function (javaObject, prop, value) {
		var objectId = javaObject.__gwt_java_object_id;
		var dispId = prop;
		if (isNaN(parseInt(dispId))) {
			// string-valued -- e.g. __gwt_java_js_object_list
			javaObject[prop] = value;
			return;
		}
		return javaObject.__gwt_plugin.javaObjectSet(objectId, dispId, value);
	},
	get: function (javaObject, prop) {
		var objectId = javaObject.__gwt_java_object_id;
		var dispId = prop;
		if (isNaN(parseInt(dispId))) {
			// string-valued -- e.g. hasOwnProperty
			return javaObject[prop];
		}
		return javaObject.__gwt_plugin.javaObjectGet(objectId, dispId);
	},
	has: function (javaObject, prop) {
		throw "nope";
		return javaObject.hasOwnProperty(prop);
	}
};
gwt_hm_JavaObject.create = function (plugin, id, type) {
	var dispatcher = function () {
		// we use a function rather than an object because the original NPAPI impl
		// expects a "default" call target:
		//
		// e.g. __static(55) and not __static.callRemote(55)
		//
		// we then use power-of-js to add properties to the function (and then
		// proxy...lordy)
		//
		// works though. could rewrite without function/proxy by rewriting
		// generated js in hostedmode -
		// but no need, that would be for pre-proxy js engines
		return gwt_hm_JavaObject.dispatch(arguments.callee, arguments);
	}
	dispatcher.__gwt_java_object_id = id;
	dispatcher.__gwt_plugin = plugin;
	switch (type) {
		case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT_LIST:
			//note that the value of the property can be replaced - its presence is what determines the JAVA_OBJECT subtype
			dispatcher.__gwt_java_js_object_list = [];
			break;
		case gwt_hm_BrowserChannel.VALUE_TYPE_JS_INT_LIST:
			//note that the value of the property can be replaced - its presence is what determines the JAVA_OBJECT subtype
			dispatcher.__gwt_java_js_int_list = [];
			break;
	}
	return new Proxy(dispatcher, gwt_hm_JavaObject.propertyDispatcher);
}
gwt_hm_JavaObject.wrapJavaObject = function (valueObj, javaObject, scriptableInstance) {
	valueObj.setJavaObjectId(gwt_hm_JavaObject.getJavaObjectId(javaObject));
	if (javaObject.hasOwnProperty("__gwt_java_js_object_list")) {
		var arr = javaObject.__gwt_java_js_object_list;
		valueObj.setJavaObjectLength(arr.length);
		for (var idx = 0; idx < arr.length; idx++) {
			var obj = arr[idx];
			valueObj.addJsObjectId(scriptableInstance.getLocalObjectRef(obj));
		}
	} else if (javaObject.hasOwnProperty("__gwt_java_js_int_list")) {
		var arr = javaObject.__gwt_java_js_int_list;
		valueObj.setJavaObjectLength(arr.length);
		for (var idx = 0; idx < arr.length; idx++) {
			var iValue = arr[idx];
			valueObj.addJsInt(iValue);
		}
	}
}
gwt_hm_JavaObject.unwrapJavaObject = function (valueObj, javaObject, scriptableInstance) {
	ret.setJavaObjectId(gwt_hm_JavaObject.getJavaObjectId(javaObject));
	if (javaObject.hasOwnProperty("__gwt_java_js_object_list")) {
		javaObject.__gwt_java_js_object_list = [];
		var intArray = valueObj.getArray();
		for (var idx = 0; idx < ret.length; idx++) {
			var refId = arr[idx];
			javaObject.__gwt_java_js_object_list.push(scriptableInstance.getLocalObject(refId));
		}
	} else if (javaObject.hasOwnProperty("__gwt_java_js_int_list")) {
		javaObject.__gwt_java_js_object_list = valueObj.getArray();
	}
}