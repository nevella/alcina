//just statics - we need a function with extras
class gwt_hm_JavaObject {
}
gwt_hm_JavaObject.isInstance = function(obj) {
    return (obj) && obj.hasOwnProperty("__gwt_java_object_id");
}
gwt_hm_JavaObject.getJavaObjectId = function(obj) {
  return obj.__gwt_java_object_id;
}
gwt_hm_JavaObject.dispatch = function(javaObject,sourceArguments) {
  var args = [];
  for(var idx=2;idx<sourceArguments.length;idx++){
    args.push(sourceArguments[idx]);
  }
  var dispId = sourceArguments[0];
  var objectId = gwt_hm_JavaObject.getJavaObjectId(javaObject);
  var thisObj = sourceArguments[1];
  if (gwt_hm_JavaObject.isInstance(thisObj)) {
      objectId = gwt_hm_JavaObject.getJavaObjectId(thisObj);
  }
  return javaObject.__gwt_channel.javaObjectInvoke(objectId, dispId, args, args.length);
}
gwt_hm_JavaObject.create = function(channel,id) {
    var dispatcher = function() {
      return gwt_hm_JavaObject.dispatch(arguments.callee,arguments);
    }
    dispatcher.__gwt_java_object_id = id; 
    dispatcher.__gwt_channel = channel; 
    return dispatcher;
}