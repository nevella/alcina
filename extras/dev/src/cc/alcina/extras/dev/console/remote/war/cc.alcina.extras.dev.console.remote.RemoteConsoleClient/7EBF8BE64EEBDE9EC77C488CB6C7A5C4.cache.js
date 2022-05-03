var $wnd = $wnd || window.parent;
var __gwtModuleFunction = $wnd.cc_alcina_extras_dev_console_remote_RemoteConsoleClient;
var $sendStats = __gwtModuleFunction.__sendStats;
$sendStats('moduleStartup', 'moduleEvalStart');
var $gwt_version = "0.0.0";
var $strongName = '7EBF8BE64EEBDE9EC77C488CB6C7A5C4';
var $gwt = {};
var $doc = $wnd.document;
var $moduleName, $moduleBase;
function __gwtStartLoadingFragment(frag) {
var fragFile = 'deferredjs/' + $strongName + '/' + frag + '.cache.js';
return __gwtModuleFunction.__startLoadingFragment(fragFile);
}
function __gwtInstallCode(code) {return __gwtModuleFunction.__installRunAsyncCode(code);}
function __gwt_isKnownPropertyValue(propName, propValue) {
return __gwtModuleFunction.__gwt_isKnownPropertyValue(propName, propValue);
}
function __gwt_getMetaProperty(name) {
return __gwtModuleFunction.__gwt_getMetaProperty(name);
}
var $stats = $wnd.__gwtStatsEvent ? function(a) {
return $wnd.__gwtStatsEvent && $wnd.__gwtStatsEvent(a);
} : null;
var $sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;
var $intern_0 = {3:1, 4:1}, $intern_1 = {3:1, 17:1, 11:1, 10:1}, $intern_2 = {19:1, 30:1}, $intern_3 = {16:1}, $intern_4 = {3:1, 54:1}, $intern_5 = {20:1}, $intern_6 = {12:1, 8:1, 7:1}, $intern_7 = {12:1, 8:1, 5:1, 7:1}, $intern_8 = {8:1, 53:1, 26:1}, $intern_9 = 65535, $intern_10 = 65536, $intern_11 = {83:1, 3:1}, $intern_12 = {6:1}, $intern_13 = {106:1, 3:1}, $intern_14 = 4194303, $intern_15 = 1048575, $intern_16 = 524288, $intern_17 = 17592186044416, $intern_18 = 4194304, $intern_19 = -17592186044416, $intern_20 = 16384, $intern_21 = 16777216, $intern_22 = 33554432, $intern_23 = 67108864, $intern_24 = {3:1, 55:1, 10:1}, $intern_25 = {3:1, 414:1}, $intern_26 = {19:1, 15:1}, $intern_27 = {3:1, 19:1, 15:1, 195:1}, $intern_28 = {3:1}, $intern_29 = {3:1, 19:1, 35:1, 30:1}, $intern_30 = {3:1, 4:1, 413:1};
var _, prototypesByTypeId_0, initFnList_0, permutationId = -1;
function setGwtProperty(propertyName, propertyValue){
  typeof window === 'object' && typeof window['$gwt'] === 'object' && (window['$gwt'][propertyName] = propertyValue);
}

function gwtOnLoad_0(errFn, modName, modBase, softPermutationId){
  ensureModuleInit();
  var initFnList = initFnList_0;
  $moduleName = modName;
  $moduleBase = modBase;
  permutationId = softPermutationId;
  function initializeModules(){
    for (var i = 0; i < initFnList.length; i++) {
      initFnList[i]();
    }
  }

  if (errFn) {
    try {
      $entry(initializeModules)();
    }
     catch (e) {
      errFn(modName, e);
    }
  }
   else {
    $entry(initializeModules)();
  }
}

function ensureModuleInit(){
  initFnList_0 == null && (initFnList_0 = []);
}

function addInitFunctions(){
  ensureModuleInit();
  var initFnList = initFnList_0;
  for (var i = 0; i < arguments.length; i++) {
    initFnList.push(arguments[i]);
  }
}

function typeMarkerFn(){
}

function toString_26(object){
  var number;
  if (Array.isArray(object) && object.typeMarker === typeMarkerFn) {
    return $getName(getClass__Ljava_lang_Class___devirtual$(object)) + '@' + (number = hashCode__I__devirtual$(object) >>> 0 , number.toString(16));
  }
  return object.toString();
}

function portableObjCreate(obj){
  function F(){
  }

  ;
  F.prototype = obj || {};
  return new F;
}

function emptyMethod(){
}

function defineClass(typeId, superTypeIdOrPrototype, castableTypeMap){
  var prototypesByTypeId = prototypesByTypeId_0, superPrototype;
  var prototype_0 = prototypesByTypeId[typeId];
  var clazz = prototype_0 instanceof Array?prototype_0[0]:null;
  if (prototype_0 && !clazz) {
    _ = prototype_0;
  }
   else {
    _ = (superPrototype = superTypeIdOrPrototype && superTypeIdOrPrototype.prototype , !superPrototype && (superPrototype = prototypesByTypeId_0[superTypeIdOrPrototype]) , portableObjCreate(superPrototype));
    _.castableTypeMap = castableTypeMap;
    !superTypeIdOrPrototype && (_.typeMarker = typeMarkerFn);
    prototypesByTypeId[typeId] = _;
  }
  for (var i = 3; i < arguments.length; ++i) {
    arguments[i].prototype = _;
  }
  clazz && (_.___clazz = clazz);
}

function bootstrap(){
  prototypesByTypeId_0 = {};
  !Array.isArray && (Array.isArray = function(vArg){
    return Object.prototype.toString.call(vArg) === '[object Array]';
  }
  );
  function now_0(){
    return (new Date).getTime();
  }

  !Date.now && (Date.now = now_0);
}

$wnd.goog = $wnd.goog || {};
$wnd.goog.global = $wnd.goog.global || $wnd;
bootstrap();
function $equals(this$static, other){
  return maskUndefined(this$static) === maskUndefined(other);
}

function Object_0(){
}

function equals_Ljava_lang_Object__Z__devirtual$(this$static, other){
  return instanceOfString(this$static)?$equals_0(this$static, other):instanceOfDouble(this$static)?($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , maskUndefined(this$static) === maskUndefined(other)):instanceOfBoolean(this$static)?($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , maskUndefined(this$static) === maskUndefined(other)):hasJavaObjectVirtualDispatch(this$static)?this$static.equals_0(other):isJavaArray(this$static)?$equals(this$static, other):maskUndefined(this$static) === maskUndefined(other);
}

function getClass__Ljava_lang_Class___devirtual$(this$static){
  return instanceOfString(this$static)?Ljava_lang_String_2_classLit:instanceOfDouble(this$static)?Ljava_lang_Double_2_classLit:instanceOfBoolean(this$static)?Ljava_lang_Boolean_2_classLit:hasJavaObjectVirtualDispatch(this$static)?this$static.___clazz:isJavaArray(this$static)?this$static.___clazz:typeof src === 'object';
}

function hashCode__I__devirtual$(this$static){
  return instanceOfString(this$static)?getHashCode_1(this$static):instanceOfDouble(this$static)?round_int(($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , this$static)):instanceOfBoolean(this$static)?($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , this$static)?1231:1237:hasJavaObjectVirtualDispatch(this$static)?this$static.hashCode_0():isJavaArray(this$static)?getHashCode_0(this$static):getHashCode_0(this$static);
}

defineClass(1, null, {}, Object_0);
_.equals_0 = function equals(other){
  return $equals(this, other);
}
;
_.getClass_0 = function getClass_0(){
  return this.___clazz;
}
;
_.hashCode_0 = function hashCode_0(){
  return getHashCode_0(this);
}
;
_.toString_0 = function toString_1(){
  var number;
  return $getName(getClass__Ljava_lang_Class___devirtual$(this)) + '@' + (number = hashCode__I__devirtual$(this) >>> 0 , number.toString(16));
}
;
_.equals = function(other){
  return this.equals_0(other);
}
;
_.hashCode = function(){
  return this.hashCode_0();
}
;
_.toString = function(){
  return this.toString_0();
}
;
function canCast(src_0, dstId){
  if (instanceOfString(src_0)) {
    return !!stringCastMap[dstId];
  }
   else if (src_0.castableTypeMap) {
    return !!src_0.castableTypeMap[dstId];
  }
   else if (instanceOfDouble(src_0)) {
    return !!doubleCastMap[dstId];
  }
   else if (instanceOfBoolean(src_0)) {
    return !!booleanCastMap[dstId];
  }
  return false;
}

function castTo(src_0, dstId){
  return $clinit_InternalPreconditions() , checkCriticalType(src_0 == null || isJsObjectOrFunction(src_0) && !(src_0.typeMarker === typeMarkerFn) || canCast(src_0, dstId), null) , src_0;
}

function castToAllowJso(src_0, dstId){
  $clinit_InternalPreconditions();
  checkCriticalType(src_0 == null || isJsObjectOrFunction(src_0) && !(src_0.typeMarker === typeMarkerFn) || canCast(src_0, dstId), null);
  return src_0;
}

function castToBoolean(src_0){
  $clinit_InternalPreconditions();
  checkCriticalType(src_0 == null || instanceOfBoolean(src_0), null);
  return src_0;
}

function castToJso(src_0){
  $clinit_InternalPreconditions();
  checkCriticalType(src_0 == null || isJsObjectOrFunction(src_0) && !(src_0.typeMarker === typeMarkerFn), null);
  return src_0;
}

function castToString(src_0){
  $clinit_InternalPreconditions();
  checkCriticalType(src_0 == null || instanceOfString(src_0), null);
  return src_0;
}

function hasJavaObjectVirtualDispatch(src_0){
  return !Array.isArray(src_0) && src_0.typeMarker === typeMarkerFn;
}

function instanceOf(src_0, dstId){
  return src_0 != null && canCast(src_0, dstId);
}

function instanceOfBoolean(src_0){
  return typeof src_0 === 'boolean';
}

function instanceOfDouble(src_0){
  return typeof src_0 === 'number';
}

function instanceOfJso(src_0){
  return src_0 != null && isJsObjectOrFunction(src_0) && !(src_0.typeMarker === typeMarkerFn);
}

function instanceOfString(src_0){
  return typeof src_0 === 'string';
}

function isJsObjectOrFunction(src_0){
  return typeof src_0 === 'object' || typeof src_0 === 'function';
}

function maskUndefined(src_0){
  return src_0 == null?null:src_0;
}

function round_int(x_0){
  return Math.max(Math.min(x_0, 2147483647), -2147483648) | 0;
}

function throwClassCastExceptionUnlessNull(o){
  $clinit_InternalPreconditions();
  checkCriticalType(o == null, null);
  return o;
}

var booleanCastMap, doubleCastMap, stringCastMap;
function $ensureNamesAreInitialized(this$static){
  if (this$static.typeName != null) {
    return;
  }
  initializeNames(this$static);
}

function $getCanonicalName(this$static){
  $ensureNamesAreInitialized(this$static);
  return this$static.canonicalName;
}

function $getName(this$static){
  $ensureNamesAreInitialized(this$static);
  return this$static.typeName;
}

function $getSimpleName(this$static){
  $ensureNamesAreInitialized(this$static);
  return this$static.simpleName;
}

function $toString_2(this$static){
  return ((this$static.modifiers & 2) != 0?'interface ':(this$static.modifiers & 1) != 0?'':'class ') + ($ensureNamesAreInitialized(this$static) , this$static.typeName);
}

function Class(){
  nextSequentialId++;
  this.typeName = null;
  this.simpleName = null;
  this.packageName = null;
  this.compoundName = null;
  this.canonicalName = null;
  this.typeId = null;
  this.arrayLiterals = null;
}

function createClassObject(packageName, compoundClassName){
  var clazz;
  clazz = new Class;
  clazz.packageName = packageName;
  clazz.compoundName = compoundClassName;
  return clazz;
}

function createForAnonymousClass(superclass, typeId){
  return createForClass(superclass.packageName, 'anon_' + (typeId.toString?typeId.toString():'[JavaScriptObject]'), typeId, superclass);
}

function createForClass(packageName, compoundClassName, typeId, superclass){
  var clazz;
  clazz = createClassObject(packageName, compoundClassName);
  maybeSetClassLiteral(typeId, clazz);
  clazz.superclass = superclass;
  return clazz;
}

function createForEnum(packageName, compoundClassName, typeId, superclass, enumConstantsFunc){
  var clazz;
  clazz = createClassObject(packageName, compoundClassName);
  maybeSetClassLiteral(typeId, clazz);
  clazz.modifiers = enumConstantsFunc?8:0;
  clazz.superclass = superclass;
  return clazz;
}

function createForPrimitive(className, primitiveTypeId){
  var clazz;
  clazz = createClassObject('', className);
  clazz.typeId = primitiveTypeId;
  clazz.modifiers = 1;
  return clazz;
}

function getClassLiteralForArray_0(leafClass, dimensions){
  var arrayLiterals = leafClass.arrayLiterals = leafClass.arrayLiterals || [];
  return arrayLiterals[dimensions] || (arrayLiterals[dimensions] = leafClass.createClassLiteralForArray(dimensions));
}

function getPrototypeForClass(clazz){
  if (clazz.isPrimitive()) {
    return null;
  }
  var typeId = clazz.typeId;
  return prototypesByTypeId_0[typeId];
}

function initializeNames(clazz){
  if (clazz.isArray_0()) {
    var componentType = clazz.componentType;
    componentType.isPrimitive()?(clazz.typeName = '[' + componentType.typeId):!componentType.isArray_0()?(clazz.typeName = '[L' + componentType.getName() + ';'):(clazz.typeName = '[' + componentType.getName());
    clazz.canonicalName = componentType.getCanonicalName() + '[]';
    clazz.simpleName = componentType.getSimpleName() + '[]';
    return;
  }
  var packageName = clazz.packageName;
  var compoundName = clazz.compoundName;
  compoundName = compoundName.split('/');
  clazz.typeName = join_2('.', [packageName, join_2('$', compoundName)]);
  clazz.canonicalName = join_2('.', [packageName, join_2('.', compoundName)]);
  clazz.simpleName = compoundName[compoundName.length - 1];
}

function join_2(separator, strings){
  var i = 0;
  while (!strings[i] || strings[i] == '') {
    i++;
  }
  var result_0 = strings[i++];
  for (; i < strings.length; i++) {
    if (!strings[i] || strings[i] == '') {
      continue;
    }
    result_0 += separator + strings[i];
  }
  return result_0;
}

function maybeSetClassLiteral(typeId, clazz){
  var proto;
  if (!typeId) {
    return;
  }
  clazz.typeId = typeId;
  var prototype_0 = getPrototypeForClass(clazz);
  if (!prototype_0) {
    prototypesByTypeId_0[typeId] = [clazz];
    return;
  }
  prototype_0.___clazz = clazz;
}

defineClass(27, 1, {27:1, 472:1}, Class);
_.createClassLiteralForArray = function createClassLiteralForArray(dimensions){
  var clazz;
  clazz = new Class;
  clazz.modifiers = 4;
  clazz.superclass = Ljava_lang_Object_2_classLit;
  dimensions > 1?(clazz.componentType = getClassLiteralForArray_0(this, dimensions - 1)):(clazz.componentType = this);
  return clazz;
}
;
_.getCanonicalName = function getCanonicalName(){
  return $getCanonicalName(this);
}
;
_.getName = function getName_0(){
  return $getName(this);
}
;
_.getSimpleName = function getSimpleName(){
  return $getSimpleName(this);
}
;
_.isArray_0 = function isArray(){
  return (this.modifiers & 4) != 0;
}
;
_.isPrimitive = function isPrimitive(){
  return (this.modifiers & 1) != 0;
}
;
_.toString_0 = function toString_30(){
  return $toString_2(this);
}
;
_.modifiers = 0;
var nextSequentialId = 1;
var Ljava_lang_Object_2_classLit = createForClass('java.lang', 'Object', 1, null);
var Lcom_google_gwt_core_client_JavaScriptObject_2_classLit = createForClass('com.google.gwt.core.client', 'JavaScriptObject$', 0, Ljava_lang_Object_2_classLit);
var Ljava_lang_Class_2_classLit = createForClass('java.lang', 'Class', 27, Ljava_lang_Object_2_classLit);
function $$init(this$static){
  this$static.stackTrace = initUnidimensionalArray(Ljava_lang_StackTraceElement_2_classLit, $intern_0, 43, 0, 0, 1);
}

function $addSuppressed(this$static, exception){
  $clinit_InternalPreconditions();
  checkCriticalNotNull_0(exception, 'Cannot suppress a null exception.');
  checkCriticalArgument_0(exception != this$static, 'Exception can not suppress itself.');
  if (this$static.disableSuppression) {
    return;
  }
  this$static.suppressedExceptions == null?(this$static.suppressedExceptions = stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Throwable_2_classLit, 1), $intern_0, 10, 0, [exception])):(this$static.suppressedExceptions[this$static.suppressedExceptions.length] = exception);
}

function $fillInStackTrace(this$static){
  if (this$static.writableStackTrace) {
    this$static.backingJsObject !== '__noinit__' && this$static.initializeBackingError();
    this$static.stackTrace = null;
  }
  return this$static;
}

function $linkBack(this$static, error){
  if (error instanceof Object) {
    try {
      error.__java$exception = this$static;
      if (navigator.userAgent.toLowerCase().indexOf('msie') != -1 && $doc.documentMode < 9) {
        return;
      }
      var throwable = this$static;
      Object.defineProperties(error, {'cause':{'get':function(){
        var cause = throwable.getCause();
        return cause && cause.getBackingJsObject();
      }
      }, 'suppressed':{'get':function(){
        return throwable.getBackingSuppressed();
      }
      }});
    }
     catch (ignored) {
    }
  }
}

function $printStackTraceImpl(this$static, out, ident){
  var t, t$array, t$index, t$max, theCause;
  $printStackTraceItems(this$static);
  for (t$array = (this$static.suppressedExceptions == null && (this$static.suppressedExceptions = initUnidimensionalArray(Ljava_lang_Throwable_2_classLit, $intern_0, 10, 0, 0, 1)) , this$static.suppressedExceptions) , t$index = 0 , t$max = t$array.length; t$index < t$max; ++t$index) {
    t = t$array[t$index];
    $printStackTraceImpl(t, out, '\t' + ident);
  }
  theCause = this$static.cause;
  !!theCause && $printStackTraceImpl(theCause, out, ident);
}

function $printStackTraceItems(this$static){
  var element$array, element$index, element$max, stackTrace;
  for (element$array = (this$static.stackTrace == null && (this$static.stackTrace = ($clinit_StackTraceCreator() , stackTrace = collector_1.getStackTrace(this$static) , dropInternalFrames(stackTrace))) , this$static.stackTrace) , element$index = 0 , element$max = element$array.length; element$index < element$max; ++element$index) {
  }
}

function $setBackingJsObject(this$static, backingJsObject){
  this$static.backingJsObject = backingJsObject;
  $linkBack(this$static, backingJsObject);
}

function $toString(this$static, message){
  var className;
  className = $getName(this$static.___clazz);
  return message == null?className:className + ': ' + message;
}

function fixIE(e){
  if (!('stack' in e)) {
    try {
      throw e;
    }
     catch (ignored) {
    }
  }
  return e;
}

defineClass(10, 1, {3:1, 10:1});
_.createError = function createError(msg){
  return new Error(msg);
}
;
_.getBackingJsObject = function getBackingJsObject(){
  return this.backingJsObject;
}
;
_.getBackingSuppressed = function getBackingSuppressed(){
  return stream_0((this.suppressedExceptions == null && (this.suppressedExceptions = initUnidimensionalArray(Ljava_lang_Throwable_2_classLit, $intern_0, 10, 0, 0, 1)) , this.suppressedExceptions)).map_1(new Throwable$lambda$0$Type).toArray();
}
;
_.getCause = function getCause(){
  return this.cause;
}
;
_.getMessage = function getMessage(){
  return this.detailMessage;
}
;
_.initializeBackingError = function initializeBackingError(){
  $setBackingJsObject(this, fixIE(this.createError($toString(this, this.detailMessage))));
  captureStackTrace(this);
}
;
_.toString_0 = function toString_2(){
  return $toString(this, this.getMessage());
}
;
_.backingJsObject = '__noinit__';
_.disableSuppression = false;
_.writableStackTrace = true;
var Ljava_lang_Throwable_2_classLit = createForClass('java.lang', 'Throwable', 10, Ljava_lang_Object_2_classLit);
defineClass(17, 10, {3:1, 17:1, 10:1});
var Ljava_lang_Exception_2_classLit = createForClass('java.lang', 'Exception', 17, Ljava_lang_Throwable_2_classLit);
function RuntimeException(){
  $$init(this);
  $fillInStackTrace(this);
  this.initializeBackingError();
}

function RuntimeException_0(message){
  $$init(this);
  this.detailMessage = message;
  $fillInStackTrace(this);
  this.initializeBackingError();
}

function RuntimeException_1(cause){
  $$init(this);
  this.detailMessage = !cause?null:$toString(cause, cause.getMessage());
  this.cause = cause;
  $fillInStackTrace(this);
  this.initializeBackingError();
}

defineClass(11, 17, $intern_1, RuntimeException, RuntimeException_0);
var Ljava_lang_RuntimeException_2_classLit = createForClass('java.lang', 'RuntimeException', 11, Ljava_lang_Exception_2_classLit);
function WrappedRuntimeException(cause){
  RuntimeException_1.call(this, cause);
}

defineClass(117, 11, $intern_1, WrappedRuntimeException);
var Lcc_alcina_framework_common_client_WrappedRuntimeException_2_classLit = createForClass('cc.alcina.framework.common.client', 'WrappedRuntimeException', 117, Ljava_lang_RuntimeException_2_classLit);
function initJs(){
  if ($wnd.AlcJsKeyableMap) {
    return;
  }
  function AlcJsKeyableMap(intLookup){
    this.length = 0;
    this.modCount = 0;
    this.values = {};
    this.keys = {};
    this.intLookup = intLookup;
  }

  AlcJsKeyableMap.prototype.get = function(key){
    return this.values[key];
  }
  ;
  AlcJsKeyableMap.prototype.put = function(key, value_0){
    var old = null;
    if (this.values[key] === undefined) {
      this.length++;
      this.modCount++;
    }
     else {
      old = this.values[key];
    }
    this.values[key] = value_0;
    this.keys[key] = key;
    return old;
  }
  ;
  AlcJsKeyableMap.prototype.remove = function(key){
    if (this.values[key] === undefined) {
      return null;
    }
     else {
      var old = this.values[key];
      delete this.values[key];
      delete this.keys[key];
      this.modCount++;
      this.length--;
      return old;
    }
  }
  ;
  AlcJsKeyableMap.prototype.containsKey = function(key){
    if (this.values[key] === undefined) {
      return false;
    }
     else {
      return true;
    }
  }
  ;
  AlcJsKeyableMap.prototype.clear = function(key){
    this.values = {};
    this.keys = {};
    this.length = 0;
    this.modCount++;
  }
  ;
  $wnd.AlcJsKeyableMap = AlcJsKeyableMap;
}

function $containsKey(this$static, key){
  return this$static.has(key);
}

function $get(this$static, key){
  return this$static.get(key);
}

function $keys(this$static){
  var map_0 = this$static;
  var v = [];
  var itr = map_0.keys();
  result = itr.next();
  while (!result.done) {
    v.push(result.value);
    result = itr.next();
  }
  return v;
}

function $put(this$static, key, value_0){
  this$static.set(key, value_0);
  return null;
}

function $remove(this$static, key){
  var v = this$static.get(key);
  this$static['delete'](key);
  return v;
}

function $computeIfAbsent(this$static, key, remappingFunction){
  var value_0;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(remappingFunction);
  value_0 = this$static.get_0(key);
  if (value_0 == null) {
    value_0 = remappingFunction.apply_0(key);
    value_0 != null && this$static.put_0(key, value_0);
  }
  return value_0;
}

function $forEach_0(this$static, consumer){
  var entry, entry$iterator;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  for (entry$iterator = this$static.entrySet().iterator(); entry$iterator.hasNext_0();) {
    entry = castTo(entry$iterator.next_1(), 16);
    consumer.accept(entry.getKey(), entry.getValue());
  }
}

function $containsEntry(this$static, entry){
  var key, ourValue, value_0;
  key = entry.getKey();
  value_0 = entry.getValue();
  ourValue = this$static.get_0(key);
  if (!(maskUndefined(value_0) === maskUndefined(ourValue) || value_0 != null && equals_Ljava_lang_Object__Z__devirtual$(value_0, ourValue))) {
    return false;
  }
  if (ourValue == null && !this$static.containsKey_0(key)) {
    return false;
  }
  return true;
}

function $implFindEntry(this$static, key, remove){
  var entry, iter, k;
  for (iter = this$static.entrySet().iterator(); iter.hasNext_0();) {
    entry = castTo(iter.next_1(), 16);
    k = entry.getKey();
    if (maskUndefined(key) === maskUndefined(k) || key != null && equals_Ljava_lang_Object__Z__devirtual$(key, k)) {
      if (remove) {
        entry = new AbstractMap$SimpleEntry(entry.getKey(), entry.getValue());
        iter.remove_2();
      }
      return entry;
    }
  }
  return null;
}

function $toString_0(this$static, o){
  return maskUndefined(o) === maskUndefined(this$static)?'(this Map)':o == null?'null':toString_26(o);
}

function getEntryValueOrNull(entry){
  return !entry?null:entry.getValue();
}

defineClass(442, 1, {54:1});
_.computeIfAbsent = function computeIfAbsent(key, remappingFunction){
  return $computeIfAbsent(this, key, remappingFunction);
}
;
_.forEach = function forEach(consumer){
  $forEach_0(this, consumer);
}
;
_.clear_0 = function clear_0(){
  this.entrySet().clear_0();
}
;
_.containsKey_0 = function containsKey(key){
  return !!$implFindEntry(this, key, false);
}
;
_.containsValue = function containsValue(value_0){
  var entry, entry$iterator, v;
  for (entry$iterator = this.entrySet().iterator(); entry$iterator.hasNext_0();) {
    entry = castTo(entry$iterator.next_1(), 16);
    v = entry.getValue();
    if (maskUndefined(value_0) === maskUndefined(v) || value_0 != null && equals_Ljava_lang_Object__Z__devirtual$(value_0, v)) {
      return true;
    }
  }
  return false;
}
;
_.equals_0 = function equals_0(obj){
  var entry, entry$iterator, otherMap;
  if (obj === this) {
    return true;
  }
  if (!instanceOf(obj, 54)) {
    return false;
  }
  otherMap = castTo(obj, 54);
  if (this.size_1() != otherMap.size_1()) {
    return false;
  }
  for (entry$iterator = otherMap.entrySet().iterator(); entry$iterator.hasNext_0();) {
    entry = castTo(entry$iterator.next_1(), 16);
    if (!$containsEntry(this, entry)) {
      return false;
    }
  }
  return true;
}
;
_.get_0 = function get_0(key){
  return getEntryValueOrNull($implFindEntry(this, key, false));
}
;
_.hashCode_0 = function hashCode_1(){
  return hashCode_17(this.entrySet());
}
;
_.isEmpty = function isEmpty(){
  return this.size_1() == 0;
}
;
_.keySet = function keySet(){
  return new AbstractMap$1(this);
}
;
_.put_0 = function put(key, value_0){
  throw toJs(new UnsupportedOperationException_0('Put not supported on this map'));
}
;
_.putAll = function putAll(map_0){
  var e, e$iterator;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(map_0);
  for (e$iterator = map_0.entrySet().iterator(); e$iterator.hasNext_0();) {
    e = castTo(e$iterator.next_1(), 16);
    this.put_0(e.getKey(), e.getValue());
  }
}
;
_.remove_0 = function remove_0(key){
  return getEntryValueOrNull($implFindEntry(this, key, true));
}
;
_.size_1 = function size_1(){
  return this.entrySet().size_1();
}
;
_.toString_0 = function toString_3(){
  var entry, entry$iterator, joiner;
  joiner = new StringJoiner(', ', '{', '}');
  for (entry$iterator = this.entrySet().iterator(); entry$iterator.hasNext_0();) {
    entry = castTo(entry$iterator.next_1(), 16);
    $add_4(joiner, $toString_0(this, entry.getKey()) + '=' + $toString_0(this, entry.getValue()));
  }
  return !joiner.builder?joiner.emptyValue:joiner.suffix.length == 0?joiner.builder.string:joiner.builder.string + ('' + joiner.suffix);
}
;
_.values_0 = function values_0(){
  return new AbstractMap$2(this);
}
;
var Ljava_util_AbstractMap_2_classLit = createForClass('java.util', 'AbstractMap', 442, Ljava_lang_Object_2_classLit);
function $get_0(this$static, key){
  return $get(this$static.map_0, key);
}

function $lambda$1(this$static, key_0, value_1){
  return $put(this$static.map_0, key_0, value_1);
}

function $put_0(this$static, key, value_0){
  return $put(this$static.map_0, key, value_0);
}

function $remove_0(this$static, key){
  return $remove(this$static.map_0, key);
}

function JsNativeMapWrapper(weak){
  this.weak = weak;
  this.map_0 = weak?new WeakMap:new Map;
}

function lambda$0(value_0, v_1){
  return maskUndefined(v_1) === maskUndefined(value_0) || v_1 != null && equals_Ljava_lang_Object__Z__devirtual$(v_1, value_0);
}

defineClass(90, 442, {54:1}, JsNativeMapWrapper);
_.clear_0 = function clear_1(){
  this.map_0.clear();
}
;
_.containsKey_0 = function containsKey_0(key){
  return $containsKey(this.map_0, key);
}
;
_.containsValue = function containsValue_0(value_0){
  return this.values_0().stream().anyMatch(new JsNativeMapWrapper$lambda$0$Type(value_0));
}
;
_.entrySet = function entrySet(){
  if (this.weak) {
    throw toJs(new UnsupportedOperationException);
  }
  return new JsNativeMapWrapper$EntrySet(this);
}
;
_.get_0 = function get_1(key){
  return $get_0(this, key);
}
;
_.isEmpty = function isEmpty_0(){
  return this.map_0.size == 0;
}
;
_.put_0 = function put_0(key, value_0){
  return $put_0(this, key, value_0);
}
;
_.putAll = function putAll_0(m){
  m.forEach(new JsNativeMapWrapper$lambda$1$Type(this));
}
;
_.remove_0 = function remove_1(key){
  return $remove_0(this, key);
}
;
_.size_1 = function size_2(){
  return this.map_0.size;
}
;
_.weak = false;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'JsNativeMapWrapper', 90, Ljava_util_AbstractMap_2_classLit);
function $forEach(this$static, action){
  var t, t$iterator;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(action);
  for (t$iterator = this$static.iterator(); t$iterator.hasNext_0();) {
    t = t$iterator.next_1();
    action.accept_0(t);
  }
}

function $removeIf(this$static, filter){
  var it, removed;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(filter);
  removed = false;
  for (it = this$static.iterator(); it.hasNext_0();) {
    if (filter.test_0(it.next_1())) {
      it.remove_2();
      removed = true;
    }
  }
  return removed;
}

function $advanceToFind(this$static, o, remove){
  var e, iter;
  for (iter = this$static.iterator(); iter.hasNext_0();) {
    e = iter.next_1();
    if (maskUndefined(o) === maskUndefined(e) || o != null && equals_Ljava_lang_Object__Z__devirtual$(o, e)) {
      remove && iter.remove_2();
      return true;
    }
  }
  return false;
}

function $containsAll(this$static, c){
  var e, e$iterator;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(c);
  for (e$iterator = c.iterator(); e$iterator.hasNext_0();) {
    e = e$iterator.next_1();
    if (!this$static.contains(e)) {
      return false;
    }
  }
  return true;
}

defineClass(433, 1, {19:1});
_.forEach_0 = function forEach_0(action){
  $forEach(this, action);
}
;
_.removeIf = function removeIf(filter){
  return $removeIf(this, filter);
}
;
_.spliterator_0 = function spliterator_0(){
  return new Spliterators$IteratorSpliterator(this, 0);
}
;
_.stream = function stream(){
  return new StreamImpl(null, this.spliterator_0());
}
;
_.add_0 = function add_0(o){
  throw toJs(new UnsupportedOperationException_0('Add not supported on this collection'));
}
;
_.addAll = function addAll(c){
  var changed, e, e$iterator;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(c);
  changed = false;
  for (e$iterator = c.iterator(); e$iterator.hasNext_0();) {
    e = e$iterator.next_1();
    changed = changed | this.add_0(e);
  }
  return changed;
}
;
_.clear_0 = function clear_2(){
  var iter;
  for (iter = this.iterator(); iter.hasNext_0();) {
    iter.next_1();
    iter.remove_2();
  }
}
;
_.contains = function contains(o){
  return $advanceToFind(this, o, false);
}
;
_.remove_1 = function remove_2(o){
  return $advanceToFind(this, o, true);
}
;
_.removeAll = function removeAll(c){
  var changed, iter, o;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(c);
  changed = false;
  for (iter = this.iterator(); iter.hasNext_0();) {
    o = iter.next_1();
    if (c.contains(o)) {
      iter.remove_2();
      changed = true;
    }
  }
  return changed;
}
;
_.toArray = function toArray(){
  return this.toArray_0(initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, this.size_1(), 5, 1));
}
;
_.toArray_0 = function toArray_0(a){
  var i, it, result_0, size_0;
  size_0 = this.size_1();
  a.length < size_0 && (a = stampJavaTypeInfo_1(new Array(size_0), a));
  result_0 = a;
  it = this.iterator();
  for (i = 0; i < size_0; ++i) {
    setCheck(result_0, i, it.next_1());
  }
  a.length > size_0 && setCheck(a, size_0, null);
  return a;
}
;
_.toString_0 = function toString_4(){
  var e, e$iterator, joiner;
  joiner = new StringJoiner(', ', '[', ']');
  for (e$iterator = this.iterator(); e$iterator.hasNext_0();) {
    e = e$iterator.next_1();
    $add_4(joiner, e === this?'(this Collection)':e == null?'null':toString_26(e));
  }
  return !joiner.builder?joiner.emptyValue:joiner.suffix.length == 0?joiner.builder.string:joiner.builder.string + ('' + joiner.suffix);
}
;
var Ljava_util_AbstractCollection_2_classLit = createForClass('java.util', 'AbstractCollection', 433, Ljava_lang_Object_2_classLit);
defineClass(443, 433, $intern_2);
_.spliterator_0 = function spliterator_1(){
  return new Spliterators$IteratorSpliterator(this, 1);
}
;
_.equals_0 = function equals_1(o){
  var other;
  if (o === this) {
    return true;
  }
  if (!instanceOf(o, 30)) {
    return false;
  }
  other = castTo(o, 30);
  if (other.size_1() != this.size_1()) {
    return false;
  }
  return $containsAll(this, other);
}
;
_.hashCode_0 = function hashCode_2(){
  return hashCode_17(this);
}
;
_.removeAll = function removeAll_0(c){
  var iter, o, o$iterator, size_0;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(c);
  size_0 = this.size_1();
  if (size_0 < c.size_1()) {
    for (iter = this.iterator(); iter.hasNext_0();) {
      o = iter.next_1();
      c.contains(o) && iter.remove_2();
    }
  }
   else {
    for (o$iterator = c.iterator(); o$iterator.hasNext_0();) {
      o = o$iterator.next_1();
      this.remove_1(o);
    }
  }
  return size_0 != this.size_1();
}
;
var Ljava_util_AbstractSet_2_classLit = createForClass('java.util', 'AbstractSet', 443, Ljava_util_AbstractCollection_2_classLit);
function JsNativeMapWrapper$EntrySet(this$0){
  this.this$01 = this$0;
}

defineClass(332, 443, $intern_2, JsNativeMapWrapper$EntrySet);
_.clear_0 = function clear_3(){
  this.this$01.map_0.clear();
}
;
_.iterator = function iterator_0(){
  return new JsNativeMapWrapper$TypedEntryIterator(this.this$01);
}
;
_.size_1 = function size_3(){
  return this.this$01.map_0.size;
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper$EntrySet_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'JsNativeMapWrapper/EntrySet', 332, Ljava_util_AbstractSet_2_classLit);
function $forEachRemaining(this$static, consumer){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  while (this$static.hasNext_0()) {
    consumer.accept_0(this$static.next_1());
  }
}

function $remove_8(){
  throw toJs(new UnsupportedOperationException);
}

function $hasNext(this$static){
  return this$static.idx + 1 < this$static.keysSnapshot.length;
}

function $next(this$static){
  if (this$static.idx + 1 == this$static.keysSnapshot.length) {
    throw toJs(new NoSuchElementException);
  }
  if (this$static.itrModCount != 0) {
    throw toJs(new ConcurrentModificationException);
  }
  this$static.nextCalled = true;
  this$static.key = this$static.keysSnapshot[++this$static.idx];
  return this$static.key;
}

function $remove_1(this$static){
  if (!this$static.nextCalled) {
    throw toJs(new UnsupportedOperationException);
  }
  $remove_0(this$static.this$01, this$static.key);
  this$static.nextCalled = false;
}

function JsNativeMapWrapper$KeyIterator(this$0){
  this.this$01 = this$0;
  this.keysSnapshot = $keys(this$0.map_0);
  this.itrModCount = 0;
}

defineClass(333, 1, {}, JsNativeMapWrapper$KeyIterator);
_.forEachRemaining = function forEachRemaining(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext(){
  return $hasNext(this);
}
;
_.next_1 = function next_0(){
  return $next(this);
}
;
_.remove_2 = function remove_3(){
  $remove_1(this);
}
;
_.idx = -1;
_.itrModCount = 0;
_.nextCalled = false;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper$KeyIterator_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'JsNativeMapWrapper/KeyIterator', 333, Ljava_lang_Object_2_classLit);
function JsNativeMapWrapper$TypedEntryIterator(this$0){
  this.this$01 = this$0;
  this.keyIterator = new JsNativeMapWrapper$KeyIterator(this$0);
}

defineClass(331, 1, {}, JsNativeMapWrapper$TypedEntryIterator);
_.forEachRemaining = function forEachRemaining_0(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_1(){
  var key;
  return key = $next(this.keyIterator) , new JsNativeMapWrapper$TypedEntryIterator$JsMapEntry(this, key, $get_0(this.this$01, key));
}
;
_.hasNext_0 = function hasNext_0(){
  return $hasNext(this.keyIterator);
}
;
_.remove_2 = function remove_4(){
  $remove_1(this.keyIterator);
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper$TypedEntryIterator_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'JsNativeMapWrapper/TypedEntryIterator', 331, Ljava_lang_Object_2_classLit);
function JsNativeMapWrapper$TypedEntryIterator$JsMapEntry(this$1, key, value_0){
  this.this$11 = this$1;
  this.key = key;
  this.value_0 = value_0;
}

defineClass(133, 1, $intern_3, JsNativeMapWrapper$TypedEntryIterator$JsMapEntry);
_.equals_0 = function equals_2(other){
  return maskUndefined(this) === maskUndefined(other);
}
;
_.hashCode_0 = function hashCode_3(){
  return getHashCode_0(this);
}
;
_.getKey = function getKey(){
  return this.key;
}
;
_.getValue = function getValue(){
  return this.value_0;
}
;
_.setValue = function setValue(value_0){
  var old;
  old = $put_0(this.this$11.this$01, this.key, value_0);
  this.value_0 = value_0;
  return old;
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper$TypedEntryIterator$JsMapEntry_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'JsNativeMapWrapper/TypedEntryIterator/JsMapEntry', 133, Ljava_lang_Object_2_classLit);
function JsNativeMapWrapper$lambda$0$Type(value_0){
  this.value_0 = value_0;
}

defineClass(334, 1, {}, JsNativeMapWrapper$lambda$0$Type);
_.negate = function negate(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_0(arg0){
  return lambda$0(this.value_0, arg0);
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper$lambda$0$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 334, Ljava_lang_Object_2_classLit);
function JsNativeMapWrapper$lambda$1$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(335, 1, {}, JsNativeMapWrapper$lambda$1$Type);
_.accept = function accept(arg0, arg1){
  $lambda$1(this.$$outer_0, arg0, arg1);
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_JsNativeMapWrapper$lambda$1$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 335, Ljava_lang_Object_2_classLit);
function $get_1(this$static, key){
  var idx;
  idx = $getIndex(this$static, key);
  return idx == -1?null:this$static.elementData[idx * 2 + 1];
}

function $getIndex(this$static, key){
  var idx;
  for (idx = 0; idx < this$static.size_0; idx++) {
    if (equals_18(key, this$static.elementData[idx * 2])) {
      return idx;
    }
  }
  return -1;
}

function $put_1(this$static, key, value_0){
  var degenerate, idx, newData;
  idx = $getIndex(this$static, key);
  if (idx != -1) {
    this$static.elementData[idx * 2 + 1] = value_0;
    return value_0;
  }
   else {
    if (this$static.size_0 == 12) {
      degenerate = ($clinit_Registry() , throwClassCastExceptionUnlessNull($impl(new Registry$Query(provider.getRegistry(), Lcc_alcina_framework_common_client_domain_DomainCollections_2_classLit)))).$_nullMethod();
      null.$_nullMethod();
      this$static.elementData = null;
      return null.$_nullMethod();
    }
    ++this$static.size_0;
    ++this$static.modCount_0;
    idx = this$static.elementData.length;
    newData = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, idx + 2, 5, 1);
    arraycopy(this$static.elementData, 0, newData, 0, idx);
    newData[idx] = key;
    newData[idx + 1] = value_0;
    this$static.elementData = newData;
    return value_0;
  }
}

function $remove_2(this$static, key){
  var idx, newData, result_0;
  idx = $getIndex(this$static, key);
  if (idx != -1) {
    --this$static.size_0;
    ++this$static.modCount_0;
    newData = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, this$static.size_0 * 2, 5, 1);
    result_0 = this$static.elementData[idx * 2 + 1];
    arraycopy(this$static.elementData, 0, newData, 0, idx * 2);
    arraycopy(this$static.elementData, (idx + 1) * 2, newData, idx * 2, (this$static.size_0 - idx) * 2);
    this$static.elementData = newData;
    return result_0;
  }
   else {
    return null;
  }
}

function LightMap(){
  this.elementData = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, 0, 5, 1);
}

defineClass(92, 1, $intern_4, LightMap);
_.computeIfAbsent = function computeIfAbsent_0(key, remappingFunction){
  return $computeIfAbsent(this, key, remappingFunction);
}
;
_.forEach = function forEach_1(consumer){
  $forEach_0(this, consumer);
}
;
_.containsKey_0 = function containsKey_1(key){
  return $getIndex(this, key) != -1;
}
;
_.entrySet = function entrySet_0(){
  return new LightMap$EntrySet(this);
}
;
_.get_0 = function get_2(key){
  return $get_1(this, key);
}
;
_.keySet = function keySet_0(){
  return new LightMap$1(this);
}
;
_.put_0 = function put_1(key, value_0){
  return $put_1(this, key, value_0);
}
;
_.remove_0 = function remove_5(key){
  return $remove_2(this, key);
}
;
_.size_1 = function size_4(){
  return this.size_0;
}
;
_.toString_0 = function toString_5(){
  var e, i, key, sb, value_0;
  i = (new LightMap$EntrySet(this)).iterator();
  if (!i.hasNext_0())
    return '{}';
  sb = new StringBuilder;
  sb.string += '{';
  for (;;) {
    e = castTo(i.next_1(), 16);
    key = e.getKey();
    value_0 = e.getValue();
    sb.string += '' + (maskUndefined(key) === maskUndefined(this)?'(this Map)':key);
    sb.string += '=';
    sb.string += '' + (maskUndefined(value_0) === maskUndefined(this)?'(this Map)':value_0);
    if (!i.hasNext_0())
      return (sb.string += '}' , sb).string;
    $append_1((sb.string += ',' , sb), 32);
  }
}
;
_.modCount_0 = 0;
_.size_0 = 0;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'LightMap', 92, Ljava_lang_Object_2_classLit);
function LightMap$1(this$0){
  this.this$01 = this$0;
}

defineClass(362, 443, $intern_2, LightMap$1);
_.clear_0 = function clear_4(){
  (new LightMap$EntrySet(this.this$01)).clear_0();
}
;
_.contains = function contains_0(k){
  return $getIndex(this.this$01, k) != -1;
}
;
_.iterator = function iterator_1(){
  return new LightMap$1$1(this);
}
;
_.size_1 = function size_5(){
  return this.this$01.size_0;
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap$1_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 362, Ljava_util_AbstractSet_2_classLit);
function LightMap$1$1(this$1){
  this.this$11 = this$1;
  this.i = (new LightMap$EntrySet(this.this$11.this$01)).iterator();
}

defineClass(363, 1, {}, LightMap$1$1);
_.forEachRemaining = function forEachRemaining_1(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext_1(){
  return this.i.hasNext_0();
}
;
_.next_1 = function next_2(){
  return castTo(this.i.next_1(), 16).getKey();
}
;
_.remove_2 = function remove_6(){
  this.i.remove_2();
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap$1$1_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 363, Ljava_lang_Object_2_classLit);
function $next_0(this$static){
  var entry;
  if (this$static.idx >= this$static.this$01.size_0) {
    throw toJs(new NoSuchElementException);
  }
  if (this$static.this$01.modCount_0 != this$static.itrModCount) {
    throw toJs(new ConcurrentModificationException);
  }
  entry = new LightMap$EntryIterator$LightMapEntry(this$static, this$static.idx++);
  return entry;
}

function LightMap$EntryIterator(this$0){
  this.this$01 = this$0;
  this.itrModCount = this.this$01.modCount_0;
}

defineClass(360, 1, {}, LightMap$EntryIterator);
_.forEachRemaining = function forEachRemaining_2(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_3(){
  return $next_0(this);
}
;
_.hasNext_0 = function hasNext_2(){
  return this.idx < this.this$01.size_0;
}
;
_.remove_2 = function remove_7(){
  throw toJs(new UnsupportedOperationException_0('remove'));
}
;
_.idx = 0;
_.itrModCount = 0;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap$EntryIterator_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'LightMap/EntryIterator', 360, Ljava_lang_Object_2_classLit);
function LightMap$EntryIterator$LightMapEntry(this$1, idx){
  this.this$11 = this$1;
  this.entryIdx = idx;
}

defineClass(361, 1, $intern_3, LightMap$EntryIterator$LightMapEntry);
_.equals_0 = function equals_3(other){
  return maskUndefined(this) === maskUndefined(other);
}
;
_.hashCode_0 = function hashCode_4(){
  return getHashCode_0(this);
}
;
_.getKey = function getKey_0(){
  return this.this$11.this$01.elementData[this.entryIdx * 2];
}
;
_.getValue = function getValue_0(){
  return this.this$11.this$01.elementData[this.entryIdx * 2 + 1];
}
;
_.setValue = function setValue_0(value_0){
  this.this$11.this$01.elementData[this.entryIdx * 2 + 1] = value_0;
  return value_0;
}
;
_.entryIdx = 0;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap$EntryIterator$LightMapEntry_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'LightMap/EntryIterator/LightMapEntry', 361, Ljava_lang_Object_2_classLit);
function LightMap$EntrySet(this$0){
  this.this$01 = this$0;
}

defineClass(41, 443, $intern_2, LightMap$EntrySet);
_.clear_0 = function clear_5(){
  this.this$01.elementData = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, 0, 5, 1);
  this.this$01.size_0 = 0;
  this.this$01.modCount_0 = 0;
}
;
_.iterator = function iterator_2(){
  return new LightMap$EntryIterator(this.this$01);
}
;
_.size_1 = function size_6(){
  return this.this$01.size_0;
}
;
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap$EntrySet_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'LightMap/EntrySet', 41, Ljava_util_AbstractSet_2_classLit);
function $compareTo(this$static, other){
  return this$static.ordinal - other.ordinal;
}

function Enum(name_0, ordinal){
  this.name_0 = name_0;
  this.ordinal = ordinal;
}

defineClass(33, 1, {3:1, 32:1, 33:1});
_.compareTo = function compareTo(other){
  return $compareTo(this, castTo(other, 33));
}
;
_.equals_0 = function equals_4(other){
  return this === other;
}
;
_.hashCode_0 = function hashCode_5(){
  return getHashCode_0(this);
}
;
_.toString_0 = function toString_6(){
  return this.name_0 != null?this.name_0:'' + this.ordinal;
}
;
_.ordinal = 0;
var Ljava_lang_Enum_2_classLit = createForClass('java.lang', 'Enum', 33, Ljava_lang_Object_2_classLit);
function $clinit_Registration$Implementation(){
  $clinit_Registration$Implementation = emptyMethod;
  INSTANCE = new Registration$Implementation('INSTANCE', 0);
  FACTORY = new Registration$Implementation('FACTORY', 1);
  SINGLETON = new Registration$Implementation('SINGLETON', 2);
}

function Registration$Implementation(enum$name, enum$ordinal){
  Enum.call(this, enum$name, enum$ordinal);
}

function values_1(){
  $clinit_Registration$Implementation();
  return stampJavaTypeInfo(getClassLiteralForArray(Lcc_alcina_framework_common_client_logic_reflection_Registration$Implementation_2_classLit, 1), $intern_0, 75, 0, [INSTANCE, FACTORY, SINGLETON]);
}

defineClass(75, 33, {75:1, 3:1, 32:1, 33:1}, Registration$Implementation);
var FACTORY, INSTANCE, SINGLETON;
var Lcc_alcina_framework_common_client_logic_reflection_Registration$Implementation_2_classLit = createForEnum('cc.alcina.framework.common.client.logic.reflection', 'Registration/Implementation', 75, Ljava_lang_Enum_2_classLit, values_1);
function $clinit_Registration$Priority(){
  $clinit_Registration$Priority = emptyMethod;
  REMOVE = new Registration$Priority('REMOVE', 0);
  _DEFAULT = new Registration$Priority('_DEFAULT', 1);
  BASE_LIBRARY = new Registration$Priority('BASE_LIBRARY', 2);
  INTERMEDIATE_LIBRARY = new Registration$Priority('INTERMEDIATE_LIBRARY', 3);
  PREFERRED_LIBRARY = new Registration$Priority('PREFERRED_LIBRARY', 4);
  APP = new Registration$Priority('APP', 5);
}

function Registration$Priority(enum$name, enum$ordinal){
  Enum.call(this, enum$name, enum$ordinal);
}

function values_2(){
  $clinit_Registration$Priority();
  return stampJavaTypeInfo(getClassLiteralForArray(Lcc_alcina_framework_common_client_logic_reflection_Registration$Priority_2_classLit, 1), $intern_0, 52, 0, [REMOVE, _DEFAULT, BASE_LIBRARY, INTERMEDIATE_LIBRARY, PREFERRED_LIBRARY, APP]);
}

defineClass(52, 33, {52:1, 3:1, 32:1, 33:1}, Registration$Priority);
var APP, BASE_LIBRARY, INTERMEDIATE_LIBRARY, PREFERRED_LIBRARY, REMOVE, _DEFAULT;
var Lcc_alcina_framework_common_client_logic_reflection_Registration$Priority_2_classLit = createForEnum('cc.alcina.framework.common.client.logic.reflection', 'Registration/Priority', 52, Ljava_lang_Enum_2_classLit, values_2);
function $clinit_Registry(){
  $clinit_Registry = emptyMethod;
  provider = new Registry$BasicRegistryProvider;
  delegateCreator = new CollectionCreators$UnsortedMapCreator;
}

function Registry(){
  $clinit_Registry();
  this.singletons = new Registry$Singletons;
  this.registrations = new Registry$Registrations;
  this.implementations = new Registry$Implementations(this);
  this.registryKeys = new Registry$RegistryKeys;
  this.logger = getLogger_1(this.___clazz);
}

defineClass(389, 1, {}, Registry);
var delegateCreator, provider;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry', 389, Ljava_lang_Object_2_classLit);
function Registry$BasicRegistryProvider(){
}

defineClass(390, 1, {}, Registry$BasicRegistryProvider);
_.getRegistry = function getRegistry(){
  !this.instance && (this.instance = new Registry);
  return this.instance;
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$BasicRegistryProvider_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/BasicRegistryProvider', 390, Ljava_lang_Object_2_classLit);
function $exists(this$static, key){
  return this$static.lookup.root_0.map_0.containsKey_0(key);
}

function $implementation(this$static, query, throwIfNotNull){
  var ascent, data_0, first, itr, keys_0, located, second;
  keys_0 = castTo(query.classes.stream().map_1(new Registry$Query$3methodref$get$Type(query.this$01.registryKeys)).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  data_0 = castTo($get_2(this$static.lookup, keys_0), 95);
  if (data_0) {
    return data_0;
  }
  ascent = new Registry$Implementations$KeyAscent(this$static, keys_0);
  do {
    data_0 = castTo($get_2(this$static.lookup, keys_0), 95);
    if (!data_0) {
      located = $registrations(this$static.this$01.registrations, ascent.keys_0);
      itr = located.stream().sorted().iterator();
      if (itr.hasNext_0()) {
        first = castTo(itr.next_1(), 82);
        if (itr.hasNext_0()) {
          second = castTo(itr.next_1(), 82);
          if (first.priority == second.priority) {
            if (first.registeringClassKey == second.registeringClassKey && located.size_1() == 2) {
              this$static.this$01.logger.warn('Duplicate registration of same class (probably fragment/split issue):\n{}', located);
            }
             else {
              throw toJs(new IllegalStateException_0(format_0('Query: %s - resolved keys: %s - equal top priorities: \n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [query, ascent.keys_0, located.stream().sorted().map_1(new Registry$Implementations$8methodref$toString$Type).collect_0(of(new Collectors$lambda$15$Type('\n', '', ''), new Collectors$9methodref$add$Type, new Collectors$10methodref$merge$Type, new Collectors$11methodref$toString$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [])))]))));
            }
          }
        }
        data_0 = new Registry$Implementations$ImplementationData(this$static, first);
        $put_2(this$static.lookup, ascent.initialKeys, data_0);
      }
    }
    if (data_0) {
      return data_0;
    }
  }
   while ($ascend(ascent));
  if (throwIfNotNull) {
    throw toJs(new NoSuchElementException_0(format_0('Query: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [castTo(query.classes.stream().map_1(new Registry$Query$3methodref$get$Type(query.this$01.registryKeys)).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [IDENTITY_FINISH]))), 15)]))));
  }
   else {
    return null;
  }
}

function Registry$Implementations(this$0){
  this.this$01 = this$0;
  this.lookup = new Registry$LookupTree;
}

defineClass(391, 1, {}, Registry$Implementations);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Implementations_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Implementations', 391, Ljava_lang_Object_2_classLit);
function Registry$Implementations$8methodref$toString$Type(){
}

defineClass(396, 1, {}, Registry$Implementations$8methodref$toString$Type);
_.apply_0 = function apply_0(arg0){
  return toString_26(arg0);
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Implementations$8methodref$toString$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.reflection.registry', 396, Ljava_lang_Object_2_classLit);
function $instance(this$static){
  var registeredClass;
  registeredClass = $clazz(this$static.registrationData.registeringClassKey);
  switch (this$static.registrationData.implementation.ordinal) {
    case 0:
      return null.$_nullMethod(new Reflections$lambda$0$Type(registeredClass)) , null.$_nullMethod();
    case 2:
      return $ensure(this$static.this$11.this$01.singletons, registeredClass);
    case 1:
      return throwClassCastExceptionUnlessNull($ensure(this$static.this$11.this$01.singletons, registeredClass)).$_nullMethod();
    default:throw toJs(new UnsupportedOperationException);
  }
}

function Registry$Implementations$ImplementationData(this$1, registrationData){
  this.this$11 = this$1;
  this.registrationData = registrationData;
}

defineClass(95, 1, {95:1}, Registry$Implementations$ImplementationData);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Implementations$ImplementationData_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Implementations/ImplementationData', 95, Ljava_lang_Object_2_classLit);
function $ascend(this$static){
  var key, superclass;
  if (this$static.keys_0.size_1() == 1) {
    return false;
  }
  key = castTo(this$static.keys_0.get_2(this$static.keys_0.size_1() - 1), 37);
  superclass = (!key.clazz && (key.clazz = forName(key.name_0)) , key.clazz).superclass;
  if (!superclass) {
    if (this$static.ascendedFinalKey) {
      return false;
    }
    this$static.keys_0 = this$static.keys_0.subList(0, this$static.keys_0.size_1() - 1);
    this$static.ascendedFinalKey = true;
  }
   else {
    if (this$static.ascendedFinalKey) {
      return false;
    }
     else {
      this$static.keys_0.set_0(this$static.keys_0.size_1() - 1, $get_4(this$static.this$11.this$01.registryKeys, superclass));
    }
  }
  return true;
}

function Registry$Implementations$KeyAscent(this$1, keys_0){
  this.this$11 = this$1;
  this.keys_0 = keys_0;
  this.initialKeys = castTo(keys_0.stream().collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
}

defineClass(392, 1, {}, Registry$Implementations$KeyAscent);
_.ascendedFinalKey = false;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Implementations$KeyAscent_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Implementations/KeyAscent', 392, Ljava_lang_Object_2_classLit);
function $add(this$static, keys_0, value_0){
  var itr;
  itr = keys_0.iterator();
  $add_0(this$static.root_0, itr, value_0);
}

function $clear(this$static, key){
  this$static.root_0.map_0.remove_0(key);
}

function $get_2(this$static, keys_0){
  var itr;
  itr = keys_0.iterator();
  return $get_3(this$static.root_0, itr);
}

function $put_2(this$static, keys_0, t){
  var itr;
  itr = keys_0.iterator();
  $put_3(this$static.root_0, itr, t);
}

function Registry$LookupTree(){
  this.root_0 = new Registry$LookupTree$Node(this);
}

defineClass(150, 1, {}, Registry$LookupTree);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$LookupTree_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/LookupTree', 150, Ljava_lang_Object_2_classLit);
function $add_0(this$static, itr, value_0){
  var child, key;
  key = castTo(itr.next_1(), 37);
  child = castTo(this$static.map_0.computeIfAbsent(key, new Registry$LookupTree$Node$lambda$1$Type(this$static)), 49);
  itr.hasNext_0()?$add_0(child, itr, value_0):castTo(child.value_0, 15).add_0(value_0);
}

function $get_3(this$static, itr){
  var child, key;
  key = castTo(itr.next_1(), 37);
  child = castTo(this$static.map_0.get_0(key), 49);
  if (!child) {
    return null;
  }
  return itr.hasNext_0()?$get_3(child, itr):castTo(this$static.map_0.get_0(key), 49).value_0;
}

function $lambda$1_0(this$static){
  var n;
  n = new Registry$LookupTree$Node(this$static.this$11);
  n.value_0 = new ArrayList;
  return n;
}

function $put_3(this$static, itr, t){
  var child, key;
  key = castTo(itr.next_1(), 37);
  child = castTo(this$static.map_0.computeIfAbsent(key, new Registry$LookupTree$Node$lambda$2$Type(this$static)), 49);
  itr.hasNext_0()?$put_3(child, itr, t):(child.value_0 = t);
}

function Registry$LookupTree$Node(this$1){
  this.this$11 = this$1;
  this.map_0 = ($clinit_Registry() , delegateCreator).createDelegateMap(0, 0);
}

defineClass(49, 1, {49:1}, Registry$LookupTree$Node);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$LookupTree$Node_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/LookupTree/Node', 49, Ljava_lang_Object_2_classLit);
function Registry$LookupTree$Node$lambda$1$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(397, 1, {}, Registry$LookupTree$Node$lambda$1$Type);
_.apply_0 = function apply_1(arg0){
  return castTo(arg0, 37) , $lambda$1_0(this.$$outer_0);
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$LookupTree$Node$lambda$1$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.reflection.registry', 397, Ljava_lang_Object_2_classLit);
function Registry$LookupTree$Node$lambda$2$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(398, 1, {}, Registry$LookupTree$Node$lambda$2$Type);
_.apply_0 = function apply_2(arg0){
  return castTo(arg0, 37) , new Registry$LookupTree$Node(this.$$outer_0.this$11);
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$LookupTree$Node$lambda$2$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.reflection.registry', 398, Ljava_lang_Object_2_classLit);
function $impl(this$static){
  var implementation;
  return implementation = $implementation(this$static.this$01.implementations, this$static, true) , !implementation?null:$instance(implementation);
}

function Registry$Query(this$0, type_0){
  this.this$01 = this$0;
  this.classes = new ArrayList;
  this.classes.add_0(type_0);
}

defineClass(94, 1, {}, Registry$Query);
_.toString_0 = function toString_7(){
  return format_0('Query: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [castTo(this.classes.stream().map_1(new Registry$Query$3methodref$get$Type(this.this$01.registryKeys)).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15)]));
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Query_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Query', 94, Ljava_lang_Object_2_classLit);
function Registry$Query$3methodref$get$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(96, 1, {}, Registry$Query$3methodref$get$Type);
_.apply_0 = function apply_3(arg0){
  return $get_4(this.$$outer_0, castTo(arg0, 27));
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Query$3methodref$get$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.reflection.registry', 96, Ljava_lang_Object_2_classLit);
function $add_1(this$static, registeringClassKey, keys_0, implementation, priority){
  $register(this$static.this$01.registrations, registeringClassKey, keys_0, implementation, priority);
}

function $singleton(this$static, type_0, implementation){
  var implementationType, typeKey;
  typeKey = $get_4(this$static.this$01.registryKeys, type_0);
  if ($exists(this$static.this$01.implementations, typeKey)) {
    throw toJs(new IllegalStateException_0(format_0('Registering %s at key %s - existing registration', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [implementation, typeKey]))));
  }
  checkState(!$exists(this$static.this$01.implementations, typeKey));
  $clear_0(this$static.this$01.registrations, typeKey);
  implementationType = ($clinit_Registration$Implementation() , SINGLETON);
  $add_1(this$static, $get_4(this$static.this$01.registryKeys, getClass__Ljava_lang_Class___devirtual$(implementation)), ($clinit_Collections() , new Collections$SingletonList(typeKey)), implementationType, ($clinit_Registration$Priority() , APP));
  $put_4(this$static.this$01.singletons, implementation);
}

function Registry$Register(this$0){
  this.this$01 = this$0;
}

defineClass(124, 1, {}, Registry$Register);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Register_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Register', 124, Ljava_lang_Object_2_classLit);
function $clear_0(this$static, typeKey){
  $clear(this$static.lookup, typeKey);
}

function $register(this$static, registeringClassKey, keys_0, implementation, priority){
  checkArgument(priority != ($clinit_Registration$Priority() , REMOVE));
  $add(this$static.lookup, keys_0, new Registry$Registrations$RegistrationData(registeringClassKey, implementation, priority));
}

function $registrations(this$static, keys_0){
  var value_0;
  value_0 = castTo($get_2(this$static.lookup, keys_0), 15);
  return !value_0?new ArrayList:value_0;
}

function Registry$Registrations(){
  this.lookup = new Registry$LookupTree;
}

defineClass(393, 1, {}, Registry$Registrations);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Registrations_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Registrations', 393, Ljava_lang_Object_2_classLit);
function $compareTo_0(this$static, o){
  return -$compareTo(this$static.priority, o.priority);
}

function Registry$Registrations$RegistrationData(registeringClassKey, implementation, priority){
  this.registeringClassKey = registeringClassKey;
  this.implementation = implementation;
  this.priority = priority;
}

defineClass(82, 1, {82:1, 32:1}, Registry$Registrations$RegistrationData);
_.compareTo = function compareTo_0(o){
  return $compareTo_0(this, castTo(o, 82));
}
;
_.toString_0 = function toString_8(){
  return format_0('%s - %s - %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this.registeringClassKey, this.implementation, this.priority]));
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Registrations$RegistrationData_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Registrations/RegistrationData', 82, Ljava_lang_Object_2_classLit);
function $get_4(this$static, clazz){
  var name_0;
  name_0 = ($ensureNamesAreInitialized(clazz) , clazz.typeName);
  return castTo(this$static.keys_0.computeIfAbsent(name_0, new Registry$RegistryKeys$lambda$0$Type(clazz)), 37);
}

function Registry$RegistryKeys(){
  this.keys_0 = ($clinit_CollectionCreators$Bootstrap() , new LinkedHashMap);
}

defineClass(394, 1, {}, Registry$RegistryKeys);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$RegistryKeys_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/RegistryKeys', 394, Ljava_lang_Object_2_classLit);
function Registry$RegistryKeys$lambda$0$Type(clazz_0){
  this.clazz_0 = clazz_0;
}

defineClass(399, 1, {}, Registry$RegistryKeys$lambda$0$Type);
_.apply_0 = function apply_4(arg0){
  var lastArg;
  return new RegistryKey((lastArg = this.clazz_0 , castToString(arg0) , lastArg));
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$RegistryKeys$lambda$0$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.logic.reflection.registry', 399, Ljava_lang_Object_2_classLit);
function $ensure(this$static, singletonClass){
  var value_0;
  value_0 = this$static.byClass.get_0(singletonClass);
  if (value_0 != null) {
    return value_0;
  }
  value_0 = this$static.byClass.get_0(singletonClass);
  if (value_0 == null) {
    value_0 = (null.$_nullMethod(new Reflections$lambda$0$Type(singletonClass)) , null.$_nullMethod());
    this$static.byClass.put_0(singletonClass, value_0);
  }
  return value_0;
}

function $put_4(this$static, implementation){
  var clazz, existing;
  clazz = getClass__Ljava_lang_Class___devirtual$(implementation);
  existing = this$static.byClass.get_0(clazz);
  if (existing != null && maskUndefined(existing) !== maskUndefined(implementation)) {
    throw toJs(new IllegalStateException_0(format_0('Existing registration of singleton - %s\n\t:: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [($ensureNamesAreInitialized(clazz) , clazz.typeName), $getName(getClass__Ljava_lang_Class___devirtual$(this$static.byClass.get_0(clazz)))]))));
  }
  this$static.byClass.put_0(clazz, implementation);
}

function Registry$Singletons(){
  this.byClass = ($clinit_CollectionCreators$Bootstrap() , new LinkedHashMap);
}

defineClass(395, 1, {}, Registry$Singletons);
var Lcc_alcina_framework_common_client_logic_reflection_registry_Registry$Singletons_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'Registry/Singletons', 395, Ljava_lang_Object_2_classLit);
function $clazz(this$static){
  !this$static.clazz && (this$static.clazz = forName(this$static.name_0));
  return this$static.clazz;
}

function RegistryKey(clazz){
  this.clazz = clazz;
  this.name_0 = ($ensureNamesAreInitialized(clazz) , clazz.typeName);
}

defineClass(37, 1, {37:1}, RegistryKey);
_.equals_0 = function equals_5(anObject){
  if (instanceOf(anObject, 37)) {
    return $equals_0(this.name_0, castTo(anObject, 37).name_0);
  }
  return false;
}
;
_.hashCode_0 = function hashCode_6(){
  return getHashCode_1(this.name_0);
}
;
_.toString_0 = function toString_9(){
  return this.name_0 + ' (rk)';
}
;
var Lcc_alcina_framework_common_client_logic_reflection_registry_RegistryKey_2_classLit = createForClass('cc.alcina.framework.common.client.logic.reflection.registry', 'RegistryKey', 37, Ljava_lang_Object_2_classLit);
function AnnotationProvider$LookupProvider(){
  $clinit_CollectionCreators$Bootstrap();
  new LinkedHashMap;
}

defineClass(193, 1, {}, AnnotationProvider$LookupProvider);
var Lcc_alcina_framework_common_client_reflection_AnnotationProvider$LookupProvider_2_classLit = createForClass('cc.alcina.framework.common.client.reflection', 'AnnotationProvider/LookupProvider', 193, Ljava_lang_Object_2_classLit);
function $clinit_ClassReflector(){
  $clinit_ClassReflector = emptyMethod;
  var prim, prim$array, prim$index, prim$max, prims, std, std$array, std$index, std$max, stds;
  stdClassMap = new HashMap;
  primitiveClassMap = new HashMap;
  stdAndPrimitivesMap = new HashMap;
  stds = stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Class_2_classLit, 1), $intern_0, 27, 0, [Ljava_lang_Long_2_classLit, Ljava_lang_Double_2_classLit, Ljava_lang_Float_2_classLit, Ljava_lang_Short_2_classLit, Ljava_lang_Byte_2_classLit, Ljava_lang_Integer_2_classLit, Ljava_lang_Boolean_2_classLit, Ljava_lang_Character_2_classLit, Ljava_util_Date_2_classLit, Ljava_lang_String_2_classLit, Ljava_sql_Timestamp_2_classLit]);
  for (std$array = stds , std$index = 0 , std$max = std$array.length; std$index < std$max; ++std$index) {
    std = std$array[std$index];
    stdClassMap.put_0(($ensureNamesAreInitialized(std) , std.typeName), std);
  }
  prims = stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Class_2_classLit, 1), $intern_0, 27, 0, [J_classLit, I_classLit, S_classLit, C_classLit, B_classLit, Z_classLit, D_classLit, F_classLit]);
  for (prim$array = prims , prim$index = 0 , prim$max = prim$array.length; prim$index < prim$max; ++prim$index) {
    prim = prim$array[prim$index];
    primitiveClassMap.put_0(($ensureNamesAreInitialized(prim) , prim.typeName), prim);
  }
  stdAndPrimitivesMap.putAll(stdClassMap);
  stdAndPrimitivesMap.putAll(primitiveClassMap);
  new HashSet_0(stdAndPrimitivesMap.values_0());
  primitives = new HashSet_0(primitiveClassMap.values_0());
}

function ClassReflector(reflectedClass, properties, byName, annotationResolver, assignableTo, interfaces){
  $clinit_ClassReflector();
  this.reflectedClass = reflectedClass;
  primitives.contains(reflectedClass);
}

defineClass(105, 1, {105:1}, ClassReflector);
_.toString_0 = function toString_10(){
  return $toString_2(this.reflectedClass);
}
;
var primitiveClassMap, primitives, stdAndPrimitivesMap, stdClassMap;
var Lcc_alcina_framework_common_client_reflection_ClassReflector_2_classLit = createForClass('cc.alcina.framework.common.client.reflection', 'ClassReflector', 105, Ljava_lang_Object_2_classLit);
function ClassReflector$lambda$0$Type(){
}

defineClass(192, 1, {}, ClassReflector$lambda$0$Type);
_.negate = function negate_0(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_1(arg0){
  return castTo(arg0, 27) , $clinit_ClassReflector() , false;
}
;
var Lcc_alcina_framework_common_client_reflection_ClassReflector$lambda$0$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.reflection', 192, Ljava_lang_Object_2_classLit);
function $clinit_ClientReflections(){
  $clinit_ClientReflections = emptyMethod;
  $ensureNamesAreInitialized(Lcc_alcina_framework_common_client_reflection_ClientReflections_2_classLit);
  moduleReflectors = new ArrayList;
  $clinit_CollectionCreators$Bootstrap();
  new LinkedHashMap;
}

function getClassReflector(clazz){
  $clinit_ClientReflections();
  var optional;
  optional = moduleReflectors.stream().map_1(new ClientReflections$lambda$2$Type(clazz)).filter_0(new ClientReflections$1methodref$nonNull$Type).findFirst();
  if (optional.ref == null) {
    if ($startsWith(($ensureNamesAreInitialized(clazz) , clazz.typeName), 'java.') || (clazz.modifiers & 1) != 0) {
      return $clinit_ClassReflector() , new ClassReflector(clazz, ($clinit_Collections() , $clinit_Collections() , EMPTY_LIST), (null , EMPTY_MAP), new AnnotationProvider$LookupProvider, new ClassReflector$lambda$0$Type, (null , EMPTY_LIST));
    }
    throw toJs(new NoSuchElementException_0(($ensureNamesAreInitialized(clazz) , 'No reflector for ' + clazz.typeName)));
  }
  return checkCriticalElement(optional.ref != null) , castTo(optional.ref, 105);
}

var moduleReflectors;
var Lcc_alcina_framework_common_client_reflection_ClientReflections_2_classLit = createForClass('cc.alcina.framework.common.client.reflection', 'ClientReflections', null, Ljava_lang_Object_2_classLit);
function ClientReflections$1methodref$nonNull$Type(){
}

defineClass(412, 1, {}, ClientReflections$1methodref$nonNull$Type);
_.negate = function negate_1(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_2(arg0){
  return arg0 != null;
}
;
var Lcc_alcina_framework_common_client_reflection_ClientReflections$1methodref$nonNull$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.reflection', 412, Ljava_lang_Object_2_classLit);
function ClientReflections$lambda$2$Type(clazz_0){
}

defineClass(411, 1, {}, ClientReflections$lambda$2$Type);
_.apply_0 = function apply_5(arg0){
  return $clinit_ClientReflections() , throwClassCastExceptionUnlessNull(arg0) , null.$_nullMethod();
}
;
var Lcc_alcina_framework_common_client_reflection_ClientReflections$lambda$2$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.reflection', 411, Ljava_lang_Object_2_classLit);
function forName(fqn){
  if (fqn == null) {
    return null;
  }
  switch (fqn) {
    case 'boolean':
      return Z_classLit;
    case 'byte':
      return B_classLit;
    case 'short':
      return S_classLit;
    case 'int':
      return I_classLit;
    case 'long':
      return J_classLit;
    case 'float':
      return F_classLit;
    case 'double':
      return D_classLit;
    case 'char':
      return C_classLit;
    case 'void':
      return V_classLit;
  }
  return null.$_nullMethod();
}

var theInstance;
function Reflections$lambda$0$Type(clazz_0){
  this.clazz_0 = clazz_0;
}

defineClass(104, 1, {}, Reflections$lambda$0$Type);
_.apply_0 = function apply_6(arg0){
  var lastArg;
  return getClassReflector((lastArg = this.clazz_0 , castTo(arg0, 27) , lastArg));
}
;
var Lcc_alcina_framework_common_client_reflection_Reflections$lambda$0$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.reflection', 104, Ljava_lang_Object_2_classLit);
function blankTo(string){
  return $clinit_CommonUtils() , string == null || string.length == 0?'---':string;
}

function notBlank(string){
  return $clinit_CommonUtils() , !(string == null || string.length == 0);
}

function out_0(template, args){
  $clinit_System();
  format_0(template, args);
}

function $get_5(this$static, key){
  var e, o;
  o = this$static.map_0.get_0(key);
  if (o != null) {
    return o;
  }
  if (!this$static.map_0.containsKey_0(key)) {
    try {
      this$static.map_0.put_0(key, this$static.function_0.apply_0(key));
    }
     catch ($e0) {
      $e0 = toJava($e0);
      if (instanceOf($e0, 17)) {
        e = $e0;
        throw toJs(new WrappedRuntimeException(e));
      }
       else 
        throw toJs($e0);
    }
  }
  return this$static.map_0.get_0(key);
}

function CachingMap(function_0){
  CachingMap_0.call(this, function_0, new LinkedHashMap);
}

function CachingMap_0(function_0, map_0){
  this.map_0 = map_0;
  this.function_0 = function_0;
  !this.map_0 && (this.map_0 = new LinkedHashMap);
}

defineClass(148, 1, {}, CachingMap);
_.toString_0 = function toString_11(){
  return this.map_0.toString_0();
}
;
var Lcc_alcina_framework_common_client_util_CachingMap_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'CachingMap', 148, Ljava_lang_Object_2_classLit);
function $clinit_CollectionCreators$Bootstrap(){
  $clinit_CollectionCreators$Bootstrap = emptyMethod;
  new CollectionCreators$ConcurrentMapCreator;
  new CollectionCreators$ConcurrentMapCreator;
}

function CollectionCreators$ConcurrentMapCreator(){
}

defineClass(143, 1, {}, CollectionCreators$ConcurrentMapCreator);
var Lcc_alcina_framework_common_client_util_CollectionCreators$ConcurrentMapCreator_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'CollectionCreators/ConcurrentMapCreator', 143, Ljava_lang_Object_2_classLit);
function CollectionCreators$UnsortedMapCreator(){
}

defineClass(144, 1, {}, CollectionCreators$UnsortedMapCreator);
_.createDelegateMap = function createDelegateMap(depthFromRoot, depth){
  return new LinkedHashMap;
}
;
var Lcc_alcina_framework_common_client_util_CollectionCreators$UnsortedMapCreator_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'CollectionCreators/UnsortedMapCreator', 144, Ljava_lang_Object_2_classLit);
function $clinit_CommonUtils(){
  $clinit_CommonUtils = emptyMethod;
  new UnsortedMultikeyMap(3);
  new LinkedHashSet;
  castTo(stream_0($split('A,An,The,And,But,Or,Nor,For,Yet,So,As,In,Of,On,To,For,From,Into,LikeExcluded,Over,With,Upon', ',', 0)).collect_0(of(new Collectors$23methodref$ctor$Type, new Collectors$24methodref$add$Type, new Collectors$lambda$50$Type, new Collectors$lambda$51$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , UNORDERED), IDENTITY_FINISH]))), 30);
  $clinit_LoggerFactory();
  getLogger_2(($ensureNamesAreInitialized(Lcc_alcina_framework_common_client_util_CommonUtils_2_classLit) , Lcc_alcina_framework_common_client_util_CommonUtils_2_classLit.typeName));
  castTo((new Arrays$ArrayList(stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Class_2_classLit, 1), $intern_0, 27, 0, [Ljava_util_ArrayList_2_classLit, Ljava_util_LinkedList_2_classLit, Ljava_util_HashSet_2_classLit, Ljava_util_LinkedHashSet_2_classLit, Ljava_util_TreeSet_2_classLit, Ljava_util_HashMap_2_classLit, Ljava_util_LinkedHashMap_2_classLit, Ljava_util_TreeMap_2_classLit, Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightSet_2_classLit, Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LiSet_2_classLit, Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightMap_2_classLit, Lcc_alcina_framework_common_client_util_CountingMap_2_classLit, Lcc_alcina_framework_common_client_collections_IdentityArrayList_2_classLit]))).stream().map_1(new CommonUtils$0methodref$getCanonicalName$Type).collect_0(of(new Collectors$23methodref$ctor$Type, new Collectors$24methodref$add$Type, new Collectors$lambda$50$Type, new Collectors$lambda$51$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [UNORDERED, IDENTITY_FINISH]))), 30);
  castTo((new Arrays$ArrayList(stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Class_2_classLit, 1), $intern_0, 27, 0, [Ljava_lang_Class_2_classLit, Ljava_sql_Timestamp_2_classLit, Ljava_util_Date_2_classLit, Ljava_lang_String_2_classLit]))).stream().map_1(new CommonUtils$1methodref$getCanonicalName$Type).collect_0(of(new Collectors$23methodref$ctor$Type, new Collectors$24methodref$add$Type, new Collectors$lambda$50$Type, new Collectors$lambda$51$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [UNORDERED, IDENTITY_FINISH]))), 30);
  castTo((new Arrays$ArrayList(stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Class_2_classLit, 1), $intern_0, 27, 0, [J_classLit, I_classLit, S_classLit, C_classLit, B_classLit, Z_classLit, D_classLit, F_classLit, V_classLit]))).stream().map_1(new CommonUtils$2methodref$getCanonicalName$Type).collect_0(of(new Collectors$23methodref$ctor$Type, new Collectors$24methodref$add$Type, new Collectors$lambda$50$Type, new Collectors$lambda$51$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [UNORDERED, IDENTITY_FINISH]))), 30);
  castTo((new Arrays$ArrayList(stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Class_2_classLit, 1), $intern_0, 27, 0, [Ljava_lang_Long_2_classLit, Ljava_lang_Double_2_classLit, Ljava_lang_Float_2_classLit, Ljava_lang_Short_2_classLit, Ljava_lang_Byte_2_classLit, Ljava_lang_Integer_2_classLit, Ljava_lang_Boolean_2_classLit, Ljava_lang_Character_2_classLit, Ljava_lang_Void_2_classLit]))).stream().map_1(new CommonUtils$3methodref$getCanonicalName$Type).collect_0(of(new Collectors$23methodref$ctor$Type, new Collectors$24methodref$add$Type, new Collectors$lambda$50$Type, new Collectors$lambda$51$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [UNORDERED, IDENTITY_FINISH]))), 30);
}

function format_0(source, args){
  $clinit_CommonUtils();
  var argsIndex, from, len, sb, to;
  if (source == null) {
    return null;
  }
  if (args.length == 0) {
    return source;
  }
  sb = new StringBuilder;
  from = 0;
  len = source.length;
  argsIndex = 0;
  while (from < len) {
    to = source.indexOf('%s', from);
    to = to == -1?len:to;
    sb.string += '' + $substring_0(source == null?'null':toString_26(source), from, to == -1?len:to);
    if (to != len) {
      $append_4(sb, args[argsIndex++]);
      to += 2;
    }
    from = to;
  }
  return sb.string;
}

function indexedOrNullWithDelta(list, item_0){
  $clinit_CommonUtils();
  var idx;
  idx = list.indexOf_0(item_0);
  if (idx == -1) {
    return null;
  }
  idx += 1;
  if (idx < 0 || idx >= list.size_1()) {
    return null;
  }
  return list.get_2(idx);
}

function join_0(objects){
  $clinit_CommonUtils();
  var objs;
  objs = objects.toArray_0(initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, objects.size_1(), 5, 1));
  return join_1(objs, '\n', false);
}

function join_1(objects, separator, ignoreEmpties){
  var app, obj, obj$array, obj$index, obj$max, sb;
  sb = new StringBuilder;
  for (obj$array = objects , obj$index = 0 , obj$max = obj$array.length; obj$index < obj$max; ++obj$index) {
    obj = obj$array[obj$index];
    app = obj == null?'null':toString_26(obj);
    app = app == null?'null':app;
    sb.string.length > 0 && (app.length != 0 || !ignoreEmpties) && (sb.string += separator , sb);
    sb.string += '' + app;
  }
  return sb.string;
}

function last_0(list){
  $clinit_CommonUtils();
  if (list.isEmpty()) {
    return null;
  }
  return list.get_2(list.size_1() - 1);
}

function padStringLeft(input_0, length_0, pad){
  $clinit_CommonUtils();
  var i, sb;
  input_0 = input_0 == null?'(null)':input_0;
  sb = new StringBuffer;
  for (i = 0; i < length_0 - input_0.length; i++) {
    sb.string += '' + pad;
  }
  sb.string += '' + input_0;
  return sb.string;
}

function padStringRight(input_0, length_0){
  $clinit_CommonUtils();
  var i, sb;
  input_0 = input_0 == null?'(null)':input_0;
  sb = new StringBuffer;
  sb.string += '' + input_0;
  for (i = 0; i < length_0 - input_0.length; i++) {
    sb.string += ' ';
  }
  return sb.string;
}

function padTwo(number){
  $clinit_CommonUtils();
  return number < 10?'0' + number:'' + number;
}

function trimToWsChars(s, maxChars, ellipsis){
  $clinit_CommonUtils();
  maxChars < 0 && (maxChars = 100);
  if (s == null || s.length <= maxChars) {
    return s;
  }
  if (s.substr(maxChars / 2 | 0, maxChars - (maxChars / 2 | 0)).indexOf(' ') == -1) {
    return s.substr(0, maxChars) + ellipsis;
  }
  return $substring_0(s, 0, $lastIndexOf(s.substr(0, maxChars), fromCodePoint(32))) + ellipsis;
}

var Lcc_alcina_framework_common_client_util_CommonUtils_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'CommonUtils', null, Ljava_lang_Object_2_classLit);
function CommonUtils$0methodref$getCanonicalName$Type(){
}

defineClass(350, 1, {}, CommonUtils$0methodref$getCanonicalName$Type);
_.apply_0 = function apply_7(arg0){
  return $getCanonicalName(castTo(arg0, 27));
}
;
var Lcc_alcina_framework_common_client_util_CommonUtils$0methodref$getCanonicalName$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.util', 350, Ljava_lang_Object_2_classLit);
function CommonUtils$1methodref$getCanonicalName$Type(){
}

defineClass(351, 1, {}, CommonUtils$1methodref$getCanonicalName$Type);
_.apply_0 = function apply_8(arg0){
  return $getCanonicalName(castTo(arg0, 27));
}
;
var Lcc_alcina_framework_common_client_util_CommonUtils$1methodref$getCanonicalName$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.util', 351, Ljava_lang_Object_2_classLit);
function CommonUtils$2methodref$getCanonicalName$Type(){
}

defineClass(352, 1, {}, CommonUtils$2methodref$getCanonicalName$Type);
_.apply_0 = function apply_9(arg0){
  return $getCanonicalName(castTo(arg0, 27));
}
;
var Lcc_alcina_framework_common_client_util_CommonUtils$2methodref$getCanonicalName$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.util', 352, Ljava_lang_Object_2_classLit);
function CommonUtils$3methodref$getCanonicalName$Type(){
}

defineClass(353, 1, {}, CommonUtils$3methodref$getCanonicalName$Type);
_.apply_0 = function apply_10(arg0){
  return $getCanonicalName(castTo(arg0, 27));
}
;
var Lcc_alcina_framework_common_client_util_CommonUtils$3methodref$getCanonicalName$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.util', 353, Ljava_lang_Object_2_classLit);
function $ensureIndent(this$static){
  if (!this$static.indented && this$static.indent != 0) {
    this$static.indented = true;
    $append_5(this$static.sb, ($clinit_CommonUtils() , padStringLeft('', this$static.indent, ' ')));
  }
}

function $format(this$static, template, args){
  $ensureIndent(this$static);
  $maybeAppendSeparator(this$static);
  $append_5(this$static.sb, args.length == 0?template:format_0(template, args));
  return this$static;
}

function $maybeAppendSeparator(this$static){
  if (this$static.sb.string.length > 0 && this$static.separator.length > 0) {
    $ensureIndent(this$static);
    $append_5(this$static.sb, this$static.separator);
  }
}

function $newLine(this$static){
  $append_5(this$static.sb, '\n');
  this$static.indented = false;
  return this$static;
}

function FormatBuilder(){
  this.sb = new StringBuilder;
}

defineClass(108, 1, {}, FormatBuilder);
_.toString_0 = function toString_12(){
  return this.sb.string;
}
;
_.indent = 0;
_.indented = false;
_.separator = '';
var Lcc_alcina_framework_common_client_util_FormatBuilder_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'FormatBuilder', 108, Ljava_lang_Object_2_classLit);
function $asTuples(this$static, maxDepth){
  var depth, k2, k2$iterator, kArr, kArr2, key, key$iterator, keys_0, m, m0, mkm, next, nextK, result_0;
  result_0 = new ArrayList;
  result_0.add_0(new ArrayList);
  for (depth = 0; depth < maxDepth; depth++) {
    next = new ArrayList;
    for (key$iterator = result_0.iterator(); key$iterator.hasNext_0();) {
      key = castTo(key$iterator.next_1(), 15);
      kArr = key.toArray_0(initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, key.size_1(), 5, 1));
      keys_0 = (m0 = (mkm = (m = castTo($getWithKeys(this$static, false, 0, kArr), 78) , m) , !mkm?null:mkm.delegate) , !m0?null:m0.keySet());
      !keys_0 && (keys_0 = new Arrays$ArrayList(stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [new MultikeyMapBase$MissingObject])));
      for (k2$iterator = keys_0.iterator(); k2$iterator.hasNext_0();) {
        k2 = k2$iterator.next_1();
        nextK = new ArrayList_0(key);
        nextK.add_0(k2);
        if (depth == this$static.depth - 1) {
          kArr2 = nextK.toArray_0(initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, nextK.size_1(), 5, 1));
          nextK.add_0($getWithKeys(this$static, false, 0, kArr2));
        }
        next.add_0(nextK);
      }
    }
    result_0 = next;
  }
  return result_0;
}

function $getWithKeys(this$static, ensure, ignoreCount, objects){
  var idx, key, last, map_0, object;
  map_0 = this$static;
  last = objects.length - 1 - ignoreCount;
  for (idx = 0; idx <= last; idx++) {
    key = objects[idx];
    object = map_0.writeableDelegate().get_0(key);
    if (object != null) {
      if (idx == last) {
        return object;
      }
       else {
        map_0 = castTo(object, 78);
      }
    }
     else {
      if (ensure && idx != this$static.depth - 1) {
        object = this$static.createMap(this$static.depth - idx - 1);
        map_0.writeableDelegate().put_0(key, object);
        map_0 = castTo(object, 78);
      }
       else {
        return null;
      }
    }
  }
  return map_0;
}

function MultikeyMapBase(depth, depthFromRoot, delegateMapCreator){
  this.depth = depth;
  this.depthFromRoot = depthFromRoot;
  this.delegateMapCreator = delegateMapCreator;
  this.ensureDelegateMapCreator();
  this.delegate = castTo(this, 48).delegateMapCreator.createDelegateMap(castTo(this, 48).depthFromRoot, castTo(this, 48).depth);
}

defineClass(122, 1, {78:1, 3:1});
_.toString_0 = function toString_13(){
  return format_0('mkm - depth %s - tuples: \n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(this.depth), join_0($asTuples(this, this.depth))]));
}
;
_.writeableDelegate = function writeableDelegate(){
  return this.delegate;
}
;
_.depth = 0;
_.depthFromRoot = 0;
var Lcc_alcina_framework_common_client_util_MultikeyMapBase_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'MultikeyMapBase', 122, Ljava_lang_Object_2_classLit);
function MultikeyMapBase$MissingObject(){
}

defineClass(359, 1, {}, MultikeyMapBase$MissingObject);
_.toString_0 = function toString_14(){
  return 'Missing object';
}
;
var Lcc_alcina_framework_common_client_util_MultikeyMapBase$MissingObject_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'MultikeyMapBase/MissingObject', 359, Ljava_lang_Object_2_classLit);
function $add_2(this$static, key, item_0){
  this$static.map_0.containsKey_0(key) || $put_5(this$static, key, new ArrayList);
  castTo(this$static.map_0.get_0(key), 15).add_0(item_0);
}

function $getAndEnsure(this$static, key){
  this$static.map_0.containsKey_0(key) || $put_5(this$static, key, new ArrayList);
  return castTo(this$static.map_0.get_0(key), 15);
}

function $put_5(this$static, key, value_0){
  return castTo(this$static.map_0.put_0(key, value_0), 15);
}

function Multimap(){
  this.map_0 = new LinkedHashMap;
}

defineClass(91, 1, {91:1, 3:1, 54:1}, Multimap);
_.computeIfAbsent = function computeIfAbsent_1(key, remappingFunction){
  return $computeIfAbsent(this, key, remappingFunction);
}
;
_.forEach = function forEach_2(consumer){
  $forEach_0(this, consumer);
}
;
_.get_0 = function get_3(key){
  return castTo(this.map_0.get_0(key), 15);
}
;
_.put_0 = function put_2(key, value_0){
  return $put_5(this, key, castTo(value_0, 15));
}
;
_.remove_0 = function remove_8(key){
  return castTo(this.map_0.remove_0(key), 15);
}
;
_.containsKey_0 = function containsKey_2(key){
  return this.map_0.containsKey_0(key);
}
;
_.entrySet = function entrySet_1(){
  return this.map_0.entrySet();
}
;
_.equals_0 = function equals_6(o){
  return this.map_0.equals_0(o);
}
;
_.hashCode_0 = function hashCode_7(){
  return this.map_0.hashCode_0();
}
;
_.keySet = function keySet_1(){
  return this.map_0.keySet();
}
;
_.size_1 = function size_7(){
  return this.map_0.size_1();
}
;
_.toString_0 = function toString_15(){
  return this.map_0.isEmpty()?'{}':join_0(this.map_0.entrySet());
}
;
var Lcc_alcina_framework_common_client_util_Multimap_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'Multimap', 91, Ljava_lang_Object_2_classLit);
function $containsValue(this$static, value_0, entries){
  var entry, entry$iterator;
  for (entry$iterator = entries.iterator(); entry$iterator.hasNext_0();) {
    entry = castTo(entry$iterator.next_1(), 16);
    if (this$static.equals_1(value_0, entry.getValue())) {
      return true;
    }
  }
  return false;
}

function $reset(this$static){
  this$static.hashCodeMap = new InternalHashCodeMap(this$static);
  this$static.stringMap = new InternalStringMap(this$static);
  structureChanged(this$static);
}

defineClass(89, 442, {54:1});
_.clear_0 = function clear_6(){
  $reset(this);
}
;
_.containsKey_0 = function containsKey_3(key){
  return instanceOfString(key)?key == null?!!$getEntry(this.hashCodeMap, null):$contains_0(this.stringMap, key):!!$getEntry(this.hashCodeMap, key);
}
;
_.containsValue = function containsValue_1(value_0){
  return $containsValue(this, value_0, this.stringMap) || $containsValue(this, value_0, this.hashCodeMap);
}
;
_.entrySet = function entrySet_2(){
  return new AbstractHashMap$EntrySet(this);
}
;
_.get_0 = function get_4(key){
  return instanceOfString(key)?key == null?getEntryValueOrNull($getEntry(this.hashCodeMap, null)):$get_6(this.stringMap, key):getEntryValueOrNull($getEntry(this.hashCodeMap, key));
}
;
_.put_0 = function put_3(key, value_0){
  return instanceOfString(key)?key == null?$put_7(this.hashCodeMap, null, value_0):$put_8(this.stringMap, key, value_0):$put_7(this.hashCodeMap, key, value_0);
}
;
_.remove_0 = function remove_9(key){
  return instanceOfString(key)?key == null?$remove_6(this.hashCodeMap, null):$remove_7(this.stringMap, key):$remove_6(this.hashCodeMap, key);
}
;
_.size_1 = function size_8(){
  return this.hashCodeMap.size_0 + this.stringMap.size_0;
}
;
var Ljava_util_AbstractHashMap_2_classLit = createForClass('java.util', 'AbstractHashMap', 89, Ljava_util_AbstractMap_2_classLit);
function HashMap(){
  $reset(this);
}

function HashMap_0(ignored){
  $clinit_InternalPreconditions();
  checkCriticalArgument_0(ignored >= 0, 'Negative initial capacity');
  checkCriticalArgument_0(true, 'Non-positive load factor');
  $reset(this);
}

defineClass(34, 89, $intern_4, HashMap, HashMap_0);
_.equals_1 = function equals_7(value1, value2){
  return maskUndefined(value1) === maskUndefined(value2) || value1 != null && equals_Ljava_lang_Object__Z__devirtual$(value1, value2);
}
;
_.getHashCode = function getHashCode(key){
  var hashCode;
  hashCode = hashCode__I__devirtual$(key);
  return hashCode | 0;
}
;
var Ljava_util_HashMap_2_classLit = createForClass('java.util', 'HashMap', 34, Ljava_util_AbstractHashMap_2_classLit);
function $clear_1(this$static){
  this$static.map_0.clear_0();
  this$static.head.prev = this$static.head;
  this$static.head.next_0 = this$static.head;
}

function $put_6(this$static, key, value_0){
  var newEntry, old, oldValue;
  old = castTo(this$static.map_0.get_0(key), 60);
  if (!old) {
    newEntry = new LinkedHashMap$ChainEntry_0(this$static, key, value_0);
    this$static.map_0.put_0(key, newEntry);
    $addToEnd(newEntry);
    return null;
  }
   else {
    oldValue = $setValue(old, value_0);
    $recordAccess(this$static, old);
    return oldValue;
  }
}

function $recordAccess(this$static, entry){
  if (this$static.accessOrder) {
    $remove_9(entry);
    $addToEnd(entry);
  }
}

function $remove_3(this$static, key){
  var entry;
  entry = castTo(this$static.map_0.remove_0(key), 60);
  if (entry) {
    $remove_9(entry);
    return entry.value_0;
  }
  return null;
}

function LinkedHashMap(){
  $reset(this);
  this.head = new LinkedHashMap$ChainEntry(this);
  this.map_0 = new HashMap;
  this.head.prev = this.head;
  this.head.next_0 = this.head;
}

defineClass(18, 34, $intern_4, LinkedHashMap);
_.clear_0 = function clear_7(){
  $clear_1(this);
}
;
_.containsKey_0 = function containsKey_4(key){
  return this.map_0.containsKey_0(key);
}
;
_.containsValue = function containsValue_2(value_0){
  var node;
  node = this.head.next_0;
  while (node != this.head) {
    if (equals_18(node.value_0, value_0)) {
      return true;
    }
    node = node.next_0;
  }
  return false;
}
;
_.entrySet = function entrySet_3(){
  return new LinkedHashMap$EntrySet(this);
}
;
_.get_0 = function get_5(key){
  var entry;
  entry = castTo(this.map_0.get_0(key), 60);
  if (entry) {
    $recordAccess(this, entry);
    return entry.value_0;
  }
  return null;
}
;
_.put_0 = function put_4(key, value_0){
  return $put_6(this, key, value_0);
}
;
_.remove_0 = function remove_10(key){
  return $remove_3(this, key);
}
;
_.size_1 = function size_9(){
  return this.map_0.size_1();
}
;
_.accessOrder = false;
var Ljava_util_LinkedHashMap_2_classLit = createForClass('java.util', 'LinkedHashMap', 18, Ljava_util_HashMap_2_classLit);
function $clinit_StringMap(){
  $clinit_StringMap = emptyMethod;
  new StringMap;
}

function StringMap(){
  $clinit_StringMap();
  LinkedHashMap.call(this);
}

defineClass(85, 18, $intern_4, StringMap);
var Lcc_alcina_framework_common_client_util_StringMap_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'StringMap', 85, Ljava_util_LinkedHashMap_2_classLit);
function $clinit_TextUtils(){
  $clinit_TextUtils = emptyMethod;
  WS_PATTERN = new RegExp('(?:[\\u0009\\u000A\\u000B\\u000C\\u000D\\u0020\\u00A0\\u0085\\u2000\\u2001\\u2002\\u2003])+', 'g');
}

function normalizeWhitespace(input_0){
  $clinit_TextUtils();
  return input_0 == null?null:$replace(WS_PATTERN, input_0, ' ');
}

var WS_PATTERN;
function $publishTopic(this$static, key){
  var listener$iterator, listeners;
  listeners = null;
  listeners = castTo($getAndEnsure(this$static.lookup, key).stream().collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  key != null && $getAndEnsure(this$static.lookup, null).stream().forEach_0(new TopicPublisher$0methodref$add$Type(listeners));
  for (listener$iterator = listeners.iterator(); listener$iterator.hasNext_0();) {
    throwClassCastExceptionUnlessNull(listener$iterator.next_1());
    null.$_nullMethod();
  }
}

function TopicPublisher(){
  this.lookup = new Multimap;
}

defineClass(134, 1, {}, TopicPublisher);
var Lcc_alcina_framework_common_client_util_TopicPublisher_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'TopicPublisher', 134, Ljava_lang_Object_2_classLit);
function TopicPublisher$0methodref$add$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(338, 1, $intern_5, TopicPublisher$0methodref$add$Type);
_.accept_0 = function accept_0(arg0){
  this.$$outer_0.add_0(throwClassCastExceptionUnlessNull(arg0));
}
;
var Lcc_alcina_framework_common_client_util_TopicPublisher$0methodref$add$Type_2_classLit = createForAnonymousClass('cc.alcina.framework.common.client.util', 338, Ljava_lang_Object_2_classLit);
function TopicPublisher$GlobalTopicPublisher(){
  this.lookup = new Multimap;
}

function get_6(){
  if (!singleton) {
    singleton = new TopicPublisher$GlobalTopicPublisher;
    $singleton(($clinit_Registry() , new Registry$Register(provider.getRegistry())), Lcc_alcina_framework_common_client_util_TopicPublisher$GlobalTopicPublisher_2_classLit, singleton);
  }
  return singleton;
}

defineClass(336, 134, {}, TopicPublisher$GlobalTopicPublisher);
var singleton;
var Lcc_alcina_framework_common_client_util_TopicPublisher$GlobalTopicPublisher_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'TopicPublisher/GlobalTopicPublisher', 336, Lcc_alcina_framework_common_client_util_TopicPublisher_2_classLit);
function $publish(this$static, t){
  var e;
  try {
    $publishTopic(this$static.topicPublisher, this$static.topic);
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 10)) {
      e = $e0;
      $printStackTraceImpl(e, ($clinit_System() , err), '');
    }
     else 
      throw toJs($e0);
  }
}

function TopicPublisher$Topic(topic){
  this.topic = topic;
  this.topicPublisher = get_6();
}

function global_0(topic){
  requireNonNull(topic);
  return new TopicPublisher$Topic(topic);
}

defineClass(337, 1, {}, TopicPublisher$Topic);
var Lcc_alcina_framework_common_client_util_TopicPublisher$Topic_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'TopicPublisher/Topic', 337, Ljava_lang_Object_2_classLit);
function UnsortedMultikeyMap(depth){
  MultikeyMapBase.call(this, depth, 0, null);
}

function UnsortedMultikeyMap_0(depth, depthFromRoot, delegateMapCreator){
  MultikeyMapBase.call(this, depth, depthFromRoot, delegateMapCreator);
}

defineClass(48, 122, {78:1, 48:1, 3:1}, UnsortedMultikeyMap, UnsortedMultikeyMap_0);
_.createMap = function createMap(childDepth){
  return new UnsortedMultikeyMap_0(childDepth, this.depthFromRoot + (this.depth - childDepth), this.delegateMapCreator);
}
;
_.ensureDelegateMapCreator = function ensureDelegateMapCreator(){
  !this.delegateMapCreator && (this.delegateMapCreator = new CollectionCreators$UnsortedMapCreator);
  return this.delegateMapCreator;
}
;
var Lcc_alcina_framework_common_client_util_UnsortedMultikeyMap_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'UnsortedMultikeyMap', 48, Lcc_alcina_framework_common_client_util_MultikeyMapBase_2_classLit);
var isIe9 = null, userAgent = null;
var CHROME_IOS_USER_AGENT = 'CriOS/';
function invokeJsDebugger(){
  debugger;
}

function jsArrayToTypedArray(typedArray){
  var i, result_0;
  result_0 = new ArrayList;
  for (i = 0; i < typedArray.length; i++) {
    result_0.add_0(typedArray[i]);
  }
  return result_0;
}

function expandEmptyElements(s){
  var app, c, c2, idx, j, l, s2, tagEnd, tagStart;
  s2 = (round_int(s.length * 1.1) , new StringBuilder_0);
  l = s.length - 1;
  tagStart = -1;
  tagEnd = -1;
  idx = 0;
  for (; idx < l; idx++) {
    app = true;
    c = ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(idx, s.length) , s.charCodeAt(idx));
    c2 = (checkCriticalStringElementIndex(idx + 1, s.length) , s.charCodeAt(idx + 1));
    if (c == 60) {
      tagEnd = -1;
      c2 != 47?(tagStart = idx + 1):(tagStart = -1);
    }
     else if (tagStart != -1) {
      if (c == 47 && c2 == 62) {
        s2.string += '><\/';
        tagEnd = tagEnd == -1?idx:tagEnd;
        for (j = tagStart; j < tagEnd; j++) {
          $append_1(s2, (checkCriticalStringElementIndex(j, s.length) , s.charCodeAt(j)));
        }
        s2.string += '>';
        ++idx;
        app = false;
      }
       else if (tagEnd == -1) {
        if (c >= 97 && c <= 122 || c >= 65 && c <= 90 || c >= 48 && c <= 57 || c == 95 || c == 45)
        ;
        else {
          tagEnd = idx;
        }
      }
    }
    app && (s2.string += String.fromCharCode(c) , s2);
  }
  idx == l && $append_1(s2, ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(idx, s.length) , s.charCodeAt(idx)));
  return s2.string;
}

function checkArgument(expression){
  if (!expression) {
    throw toJs(new IllegalArgumentException);
  }
}

function checkArgument_0(expression){
  if (!expression) {
    throw toJs(new IllegalArgumentException_0('refchild not a child of this node'));
  }
}

function checkState(expression){
  if (!expression) {
    throw toJs(new IllegalStateException);
  }
}

function setUncaughtExceptionHandler(handler){
  uncaughtExceptionHandler = handler;
  $clinit_Impl();
}

var uncaughtExceptionHandler = null;
defineClass(110, 11, $intern_1);
var Ljava_lang_JsException_2_classLit = createForClass('java.lang', 'JsException', 110, Ljava_lang_RuntimeException_2_classLit);
defineClass(215, 110, $intern_1);
var Lcom_google_gwt_core_client_impl_JavaScriptExceptionBase_2_classLit = createForClass('com.google.gwt.core.client.impl', 'JavaScriptExceptionBase', 215, Ljava_lang_JsException_2_classLit);
function $clinit_JavaScriptException(){
  $clinit_JavaScriptException = emptyMethod;
  NOT_SET = new Object_0;
}

function $ensureInit(this$static){
  var exception;
  if (this$static.message_0 == null) {
    exception = maskUndefined(this$static.e) === maskUndefined(NOT_SET)?null:this$static.e;
    this$static.name_0 = exception == null?'null':instanceOfJso(exception)?getExceptionName0(castToJso(exception)):instanceOfString(exception)?'String':$getName(getClass__Ljava_lang_Class___devirtual$(exception));
    this$static.description = this$static.description + ': ' + (instanceOfJso(exception)?getExceptionDescription0(castToJso(exception)):exception + '');
    this$static.message_0 = '(' + this$static.name_0 + ') ' + this$static.description;
  }
}

function JavaScriptException(e){
  $clinit_JavaScriptException();
  $$init(this);
  $fillInStackTrace(this);
  this.backingJsObject = e;
  $linkBack(this, e);
  this.detailMessage = e == null?'null':toString_26(e);
  this.description = '';
  this.e = e;
  this.description = '';
}

function getExceptionDescription0(e){
  return e == null?null:e.message;
}

function getExceptionName0(e){
  return e == null?null:e.name;
}

defineClass(58, 215, {58:1, 3:1, 17:1, 11:1, 10:1}, JavaScriptException);
_.getMessage = function getMessage_0(){
  $ensureInit(this);
  return this.message_0;
}
;
_.getThrown = function getThrown(){
  return maskUndefined(this.e) === maskUndefined(NOT_SET)?null:this.e;
}
;
var NOT_SET;
var Lcom_google_gwt_core_client_JavaScriptException_2_classLit = createForClass('com.google.gwt.core.client', 'JavaScriptException', 58, Lcom_google_gwt_core_client_impl_JavaScriptExceptionBase_2_classLit);
function now_1(){
  if (Date.now) {
    return Date.now();
  }
  return (new Date).getTime();
}

defineClass(415, 1, {});
var INSTANCE_0;
var Lcom_google_gwt_core_client_Scheduler_2_classLit = createForClass('com.google.gwt.core.client', 'Scheduler', 415, Ljava_lang_Object_2_classLit);
function $clinit_Impl(){
  $clinit_Impl = emptyMethod;
  !!($clinit_StackTraceCreator() , collector_1);
}

function apply_11(jsFunction, thisObj, args){
  return jsFunction.apply(thisObj, args);
  var __0;
}

function enter(){
  var now_0;
  if (entryDepth != 0) {
    now_0 = now_1();
    if (now_0 - watchdogEntryDepthLastScheduled > 2000) {
      watchdogEntryDepthLastScheduled = now_0;
      watchdogEntryDepthTimerId = $wnd.setTimeout(watchdogEntryDepthRun, 10);
    }
  }
  if (entryDepth++ == 0) {
    $flushEntryCommands(($clinit_SchedulerImpl() , INSTANCE_1));
    return true;
  }
  return false;
}

function entry_0(jsFunction){
  $clinit_Impl();
  return function(){
    return entry0(jsFunction, this, arguments);
    var __0;
  }
  ;
}

function entry0(jsFunction, thisObj, args){
  var initialEntry, t;
  initialEntry = enter();
  try {
    initialEntry && $stopObserving(($clinit_LocalDom() , mutations));
    if (uncaughtExceptionHandler) {
      try {
        return apply_11(jsFunction, thisObj, args);
      }
       catch ($e0) {
        $e0 = toJava($e0);
        if (instanceOf($e0, 10)) {
          t = $e0;
          reportUncaughtException(t);
          return undefined;
        }
         else 
          throw toJs($e0);
      }
    }
     else {
      return apply_11(jsFunction, thisObj, args);
    }
  }
   finally {
    exit(initialEntry);
    initialEntry && $startObserving(($clinit_LocalDom() , mutations));
  }
}

function exit(initialEntry){
  initialEntry && $flushFinallyCommands(($clinit_SchedulerImpl() , INSTANCE_1));
  --entryDepth;
  if (initialEntry) {
    if (watchdogEntryDepthTimerId != -1) {
      watchdogEntryDepthCancel(watchdogEntryDepthTimerId);
      watchdogEntryDepthTimerId = -1;
    }
  }
}

function reportToBrowser(e){
  $wnd.setTimeout(function(){
    throw e;
  }
  , 0);
}

function reportUncaughtException(e){
  $clinit_Impl();
  var handler;
  handler = uncaughtExceptionHandler;
  if (handler) {
    if (handler == uncaughtExceptionHandlerForTest) {
      return;
    }
    handler.onUncaughtException(e);
    return;
  }
  reportToBrowser(instanceOf(e, 58)?castTo(e, 58).getThrown():e);
}

function watchdogEntryDepthCancel(timerId){
  $wnd.clearTimeout(timerId);
}

function watchdogEntryDepthRun(){
  entryDepth != 0 && (entryDepth = 0);
  watchdogEntryDepthTimerId = -1;
}

var entryDepth = 0, uncaughtExceptionHandlerForTest, watchdogEntryDepthLastScheduled = 0, watchdogEntryDepthTimerId = -1;
function $clinit_SchedulerImpl(){
  $clinit_SchedulerImpl = emptyMethod;
  INSTANCE_1 = new SchedulerImpl;
}

function $flushEntryCommands(this$static){
  var oldQueue, rescheduled;
  if (this$static.entryCommands) {
    rescheduled = null;
    do {
      oldQueue = this$static.entryCommands;
      this$static.entryCommands = null;
      rescheduled = runScheduledTasks(oldQueue, rescheduled);
    }
     while (this$static.entryCommands);
    this$static.entryCommands = rescheduled;
  }
}

function $flushFinallyCommands(this$static){
  var oldQueue, rescheduled;
  if (this$static.finallyCommands) {
    rescheduled = null;
    do {
      oldQueue = this$static.finallyCommands;
      this$static.finallyCommands = null;
      rescheduled = runScheduledTasks(oldQueue, rescheduled);
    }
     while (this$static.finallyCommands);
    this$static.finallyCommands = rescheduled;
  }
}

function SchedulerImpl(){
}

function push_0(queue, task){
  !queue && (queue = new ArrayList);
  queue.add_0(task);
  return queue;
}

function runScheduledTasks(tasks, rescheduled){
  var e, i, j, t;
  for (i = 0 , j = tasks.size_1(); i < j; i++) {
    t = castTo(tasks.get_2(i), 114);
    try {
      t.repeating?null.$_nullMethod() && (rescheduled = push_0(rescheduled, t)):t.scheduledCommand.execute();
    }
     catch ($e0) {
      $e0 = toJava($e0);
      if (instanceOf($e0, 10)) {
        e = $e0;
        reportUncaughtException(e);
      }
       else 
        throw toJs($e0);
    }
  }
  return rescheduled;
}

defineClass(315, 415, {}, SchedulerImpl);
_.scheduleFinally = function scheduleFinally(cmd){
  var task;
  this.finallyCommands = push_0(this.finallyCommands, (task = new SchedulerImpl$Task , task.scheduledCommand = cmd , task.repeating = false , task));
}
;
var INSTANCE_1;
var Lcom_google_gwt_core_client_impl_SchedulerImpl_2_classLit = createForClass('com.google.gwt.core.client.impl', 'SchedulerImpl', 315, Lcom_google_gwt_core_client_Scheduler_2_classLit);
function SchedulerImpl$Task(){
}

defineClass(114, 1, {114:1}, SchedulerImpl$Task);
_.repeating = false;
var Lcom_google_gwt_core_client_impl_SchedulerImpl$Task_2_classLit = createForClass('com.google.gwt.core.client.impl', 'SchedulerImpl/Task', 114, Ljava_lang_Object_2_classLit);
function $clinit_StackTraceCreator(){
  $clinit_StackTraceCreator = emptyMethod;
  var c, enforceLegacy;
  enforceLegacy = !supportsErrorStack();
  c = new StackTraceCreator$CollectorModernNoSourceMap;
  collector_1 = instanceOf(c, 111) && enforceLegacy?new StackTraceCreator$CollectorLegacy:c;
}

function captureStackTrace(error){
  $clinit_StackTraceCreator();
  collector_1.collect(error);
}

function dropInternalFrames(stackTrace){
  var dropFrameUntilFnName, dropFrameUntilFnName2, i, numberOfFramesToSearch;
  dropFrameUntilFnName = 'captureStackTrace';
  dropFrameUntilFnName2 = 'initializeBackingError';
  numberOfFramesToSearch = $wnd.Math.min(stackTrace.length, 5);
  for (i = numberOfFramesToSearch - 1; i >= 0; i--) {
    if ($equals_0(stackTrace[i].methodName, dropFrameUntilFnName) || $equals_0(stackTrace[i].methodName, dropFrameUntilFnName2)) {
      stackTrace.length >= i + 1 && stackTrace.splice(0, i + 1);
      break;
    }
  }
  return stackTrace;
}

function extractFunctionName(fnName){
  var fnRE = /function(?:\s+([\w$]+))?\s*\(/;
  var match_0 = fnRE.exec(fnName);
  return match_0 && match_0[1] || 'anonymous';
}

function parseInt_0(number){
  $clinit_StackTraceCreator();
  return parseInt(number) || -1;
}

function split_0(t){
  $clinit_StackTraceCreator();
  var e = t.backingJsObject;
  if (e && e.stack) {
    var stack_0 = e.stack;
    var toString_0 = e + '\n';
    stack_0.substring(0, toString_0.length) == toString_0 && (stack_0 = stack_0.substring(toString_0.length));
    return stack_0.split('\n');
  }
  return [];
}

function supportsErrorStack(){
  if (Error.stackTraceLimit > 0) {
    $wnd.Error.stackTraceLimit = Error.stackTraceLimit = 64;
    return true;
  }
  return 'stack' in new Error;
}

var collector_1;
defineClass(430, 1, {});
var Lcom_google_gwt_core_client_impl_StackTraceCreator$Collector_2_classLit = createForClass('com.google.gwt.core.client.impl', 'StackTraceCreator/Collector', 430, Ljava_lang_Object_2_classLit);
function StackTraceCreator$CollectorLegacy(){
}

defineClass(216, 430, {}, StackTraceCreator$CollectorLegacy);
_.collect = function collect(error){
  var seen = {}, name_1;
  var fnStack = [];
  error['fnStack'] = fnStack;
  var callee = arguments.callee.caller;
  while (callee) {
    var name_0 = ($clinit_StackTraceCreator() , callee.name || (callee.name = extractFunctionName(callee.toString())));
    fnStack.push(name_0);
    var keyName = ':' + name_0;
    var withThisName = seen[keyName];
    if (withThisName) {
      var i, j;
      for (i = 0 , j = withThisName.length; i < j; i++) {
        if (withThisName[i] === callee) {
          return;
        }
      }
    }
    (withThisName || (seen[keyName] = [])).push(callee);
    callee = callee.caller;
  }
}
;
_.getStackTrace = function getStackTrace(t){
  var i, length_0, stack_0, stackTrace;
  stack_0 = ($clinit_StackTraceCreator() , t && t['fnStack']?t['fnStack']:[]);
  length_0 = stack_0.length;
  stackTrace = initUnidimensionalArray(Ljava_lang_StackTraceElement_2_classLit, $intern_0, 43, length_0, 0, 1);
  for (i = 0; i < length_0; i++) {
    stackTrace[i] = new StackTraceElement(stack_0[i], null, -1);
  }
  return stackTrace;
}
;
var Lcom_google_gwt_core_client_impl_StackTraceCreator$CollectorLegacy_2_classLit = createForClass('com.google.gwt.core.client.impl', 'StackTraceCreator/CollectorLegacy', 216, Lcom_google_gwt_core_client_impl_StackTraceCreator$Collector_2_classLit);
function $parse(this$static, stString){
  var closeParen, col, endFileUrlIndex, fileName, index_0, lastColonIndex, line, location_0, toReturn;
  location_0 = '';
  if (stString.length == 0) {
    return this$static.createSte('Unknown', 'anonymous', -1, -1);
  }
  toReturn = $trim(stString);
  $equals_0(toReturn.substr(0, 3), 'at ') && (toReturn = toReturn.substr(3));
  toReturn = toReturn.replace(/\[.*?\]/g, '');
  index_0 = toReturn.indexOf('(');
  if (index_0 == -1) {
    index_0 = toReturn.indexOf('@');
    if (index_0 == -1) {
      location_0 = toReturn;
      toReturn = '';
    }
     else {
      location_0 = $trim(toReturn.substr(index_0 + 1));
      toReturn = $trim(toReturn.substr(0, index_0));
    }
  }
   else {
    closeParen = toReturn.indexOf(')', index_0);
    location_0 = toReturn.substr(index_0 + 1, closeParen - (index_0 + 1));
    toReturn = $trim(toReturn.substr(0, index_0));
  }
  index_0 = $indexOf(toReturn, fromCodePoint(46));
  index_0 != -1 && (toReturn = toReturn.substr(index_0 + 1));
  (toReturn.length == 0 || $equals_0(toReturn, 'Anonymous function')) && (toReturn = 'anonymous');
  lastColonIndex = $lastIndexOf(location_0, fromCodePoint(58));
  endFileUrlIndex = $lastIndexOf_0(location_0, fromCodePoint(58), lastColonIndex - 1);
  line = -1;
  col = -1;
  fileName = 'Unknown';
  if (lastColonIndex != -1 && endFileUrlIndex != -1) {
    fileName = location_0.substr(0, endFileUrlIndex);
    line = parseInt_0(location_0.substr(endFileUrlIndex + 1, lastColonIndex - (endFileUrlIndex + 1)));
    col = parseInt_0(location_0.substr(lastColonIndex + 1));
  }
  return this$static.createSte(fileName, toReturn, line, col);
}

defineClass(111, 430, {111:1});
_.collect = function collect_0(error){
}
;
_.createSte = function createSte(fileName, method, line, col){
  return new StackTraceElement(method, fileName + '@' + col, line < 0?-1:line);
}
;
_.getStackTrace = function getStackTrace_0(t){
  var addIndex, i, length_0, stack_0, stackTrace, ste;
  stack_0 = split_0(t);
  stackTrace = initUnidimensionalArray(Ljava_lang_StackTraceElement_2_classLit, $intern_0, 43, 0, 0, 1);
  addIndex = 0;
  length_0 = stack_0.length;
  if (length_0 == 0) {
    return stackTrace;
  }
  ste = $parse(this, stack_0[0]);
  $equals_0(ste.methodName, 'anonymous') || (stackTrace[addIndex++] = ste);
  for (i = 1; i < length_0; i++) {
    stackTrace[addIndex++] = $parse(this, stack_0[i]);
  }
  return stackTrace;
}
;
var Lcom_google_gwt_core_client_impl_StackTraceCreator$CollectorModern_2_classLit = createForClass('com.google.gwt.core.client.impl', 'StackTraceCreator/CollectorModern', 111, Lcom_google_gwt_core_client_impl_StackTraceCreator$Collector_2_classLit);
function StackTraceCreator$CollectorModernNoSourceMap(){
}

defineClass(217, 111, {111:1}, StackTraceCreator$CollectorModernNoSourceMap);
_.createSte = function createSte_0(fileName, method, line, col){
  return new StackTraceElement(method, fileName, -1);
}
;
var Lcom_google_gwt_core_client_impl_StackTraceCreator$CollectorModernNoSourceMap_2_classLit = createForClass('com.google.gwt.core.client.impl', 'StackTraceCreator/CollectorModernNoSourceMap', 217, Lcom_google_gwt_core_client_impl_StackTraceCreator$CollectorModern_2_classLit);
function $clinit_GWT(){
  $clinit_GWT = emptyMethod;
  new JsLogger;
}

function JsLogger(){
}

defineClass(218, 1, {}, JsLogger);
var Lcom_google_gwt_core_shared_impl_JsLogger_2_classLit = createForClass('com.google.gwt.core.shared.impl', 'JsLogger', 218, Ljava_lang_Object_2_classLit);
function $doPreTreeResolution(this$static, child){
  var ensureBecauseChildResolved;
  if (child) {
    ensureBecauseChildResolved = (child.resolvedEventId > 0 || child.linkedToRemote()) && (!this$static.linkedToRemote() || this$static.isPendingResolution());
    ensureBecauseChildResolved && ($clinit_LocalDom() , $ensureRemote0(instance, this$static));
    $ensureRemoteCheck(this$static);
    this$static.linkedToRemote() && (this$static.resolvedEventId > 0 || child.resolvedEventId > 0) && (child.resolvedEventId > 0?($clinit_LocalDom() , $ensureRemote0(instance, child)):($clinit_LocalDom() , $ensureRemoteNodeMaybePendingResolution0(instance, child)));
  }
}

function $ensureRemoteCheck(this$static){
  if (!this$static.linkedToRemote() && this$static.resolvedEventId > 0 && !!$provideSelfOrAncestorLinkedToRemote(this$static) && ($clinit_LocalDom() , !disableRemoteWrite) && (this$static.getNodeType() == 3 || this$static.getNodeType() == 1)) {
    $clinit_LocalDom();
    $ensureRemote0((null , instance), this$static);
    return true;
  }
   else {
    return false;
  }
}

function $insertBefore(this$static, newChild, refChild){
  var e, result_0;
  try {
    this$static.validateInsert(newChild);
    $doPreTreeResolution(this$static, newChild);
    $doPreTreeResolution(this$static, refChild);
    result_0 = this$static.local_0().insertBefore_0(newChild, refChild);
    insertBefore_Lcom_google_gwt_dom_client_Node_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(this$static.remote_0(), newChild, refChild);
    return result_0;
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 17)) {
      e = $e0;
      throw toJs(new LocalDomException(e));
    }
     else 
      throw toJs($e0);
  }
}

function $provideSelfOrAncestorLinkedToRemote(this$static){
  if (this$static.linkedToRemote()) {
    return this$static;
  }
  if (this$static.getParentElement()) {
    return $provideSelfOrAncestorLinkedToRemote(this$static.getParentElement());
  }
  return null;
}

function $resolved(this$static, wasResolvedEventId){
  checkState(this$static.resolvedEventId == 0 || this$static.resolvedEventId == wasResolvedEventId);
  this$static.resolvedEventId = wasResolvedEventId;
}

function $sameTreeNodeFor(this$static, domNode){
  if (!domNode) {
    return null;
  }
  return instanceOf(domNode, 53)?this$static.local_0():this$static.remote_0();
}

defineClass(7, 1, $intern_6);
_.appendChild_0 = function appendChild(newChild){
  var node;
  this.validateInsert(newChild);
  $doPreTreeResolution(this, newChild);
  node = this.local_0().appendChild_0(newChild);
  appendChild_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(this.remote_0(), newChild);
  return node;
}
;
_.getChild = function getChild(index_0){
  return $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(this), index_0);
}
;
_.getChildCount = function getChildCount(){
  return getLength__I__devirtual$(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(this).impl);
}
;
_.getChildNodes = function getChildNodes(){
  return this.local_0().getChildNodes();
}
;
_.getNextSibling = function getNextSibling(){
  return this.local_0().getNextSibling();
}
;
_.getNodeName = function getNodeName(){
  return this.local_0().getNodeName();
}
;
_.getNodeType = function getNodeType(){
  return this.local_0().getNodeType();
}
;
_.getNodeValue = function getNodeValue(){
  return this.local_0().getNodeValue();
}
;
_.getParentElement = function getParentElement(){
  return this.local_0().getParentElement();
}
;
_.getParentNode = function getParentNode(){
  return this.local_0().getParentNode();
}
;
_.insertBefore_0 = function insertBefore_0(newChild, refChild){
  return $insertBefore(this, newChild, refChild);
}
;
_.isPendingResolution = function isPendingResolution(){
  return false;
}
;
_.removeChild_0 = function removeChild(oldChild){
  var result_0;
  $doPreTreeResolution(this, oldChild);
  result_0 = this.local_0().removeChild_0(oldChild);
  removeChild_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(this.remote_0(), oldChild);
  return result_0;
}
;
_.removeFromParent = function removeFromParent(){
  $ensureRemoteCheck(this);
  removeFromParent__V__devirtual$(this.remote_0());
  this.local_0().removeFromParent();
}
;
_.validateInsert = function validateInsert(newChild){
}
;
_.resolvedEventId = 0;
var Lcom_google_gwt_dom_client_Node_2_classLit = createForClass('com.google.gwt.dom.client', 'Node', 7, Ljava_lang_Object_2_classLit);
function $getChildIndexLocal(this$static, child){
  if (child.getParentElement() != this$static) {
    return -1;
  }
  return $indexInParentChildren(child.local_0());
}

function $getStyle(this$static){
  !this$static.style_0 && (this$static.style_0 = new Style(this$static));
  return this$static.style_0;
}

function $putLocal(this$static, local){
  checkState(!this$static.local);
  this$static.local = local;
  local.element = this$static;
  this$static.remote = ($clinit_ElementNull() , INSTANCE_2);
  return this$static;
}

function $putRemote(this$static, remote, resolved){
  var existingBits;
  $clinit_GWT();
  checkState(this$static.resolvedEventId > 0 == resolved);
  checkState(this$static.remote == ($clinit_ElementNull() , INSTANCE_2) || remote == this$static.remote);
  checkState(!!remote);
  this$static.remote = remote;
  if (remote) {
    if (!!this$static.local && this$static.local.eventBits != 0) {
      existingBits = ($clinit_DOM() , $getEventsSunk(impl_2, this$static));
      sinkEvents_2(this$static, existingBits | this$static.local.eventBits);
    }
  }
}

function $remote(this$static){
  $clinit_LocalDom();
  if (disableRemoteWrite) {
    return $clinit_ElementNull() , INSTANCE_2;
  }
  return this$static.remote;
}

function $removeClassName(this$static, className){
  var result_0;
  $ensureRemoteCheck(this$static);
  result_0 = removeClassName_0(this$static.local, className);
  removeClassName_Ljava_lang_String__Z__devirtual$($remote(this$static), className);
  return result_0;
}

function $replaceRemote(this$static, remote){
  var parentRemote;
  parentRemote = $getParentElementRemote(castToJso($remote(this$static)));
  if (parentRemote) {
    $insertBefore0(parentRemote, remote, castToJso($remote(this$static)));
    $removeFromParent0(castToJso($remote(this$static)));
  }
  checkState(!!remote);
  this$static.remote = remote;
}

function $resolveRemoteDefined(this$static){
  var value_0;
  if ((value_0 = castToString($get_1(this$static.local.attributes_0, 'class')) , value_0 != null?value_0:'').indexOf('__localdom-remote-defined') != -1) {
    out_0('resolve remote defined: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(hashCode__I__devirtual$(this$static))]));
    $clinit_LocalDom();
    $resolve0(instance, false);
    $ensureRemote0(instance, this$static);
    castToJso($remote(this$static));
    $parseAndMarkResolved(instance, castToJso($remote(this$static)), $getOuterHtml_0(castToJso($remote(this$static))), this$static);
    setStyleName(this$static, '__localdom-remote-defined');
    return true;
  }
   else {
    return false;
  }
}

function Element_0(){
}

function is(o){
  return !!o && $is(o, Lcom_google_gwt_dom_client_Element_2_classLit);
}

defineClass(5, 7, $intern_7, Element_0);
_.local_0 = function local_0(){
  return this.local;
}
;
_.node = function node_0(){
  return this;
}
;
_.remote_0 = function remote_1(){
  return $remote(this);
}
;
_.typedRemote = function typedRemote_1(){
  return castToJso($remote(this));
}
;
_.addClassName = function addClassName(className){
  var result_0;
  return $ensureRemoteCheck(this) , result_0 = addClassName_0(this.local, className) , addClassName_Ljava_lang_String__Z__devirtual$($remote(this), className) , result_0;
}
;
_.cast = function cast(){
  return this;
}
;
_.getAttributeMap = function getAttributeMap(){
  return this.local.attributes_0;
}
;
_.getChild = function getChild_0(index_0){
  return $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(this), index_0);
}
;
_.getClassName = function getClassName(){
  var value_0;
  return value_0 = castToString($get_1(this.local.attributes_0, 'class')) , value_0 != null?value_0:'';
}
;
_.getInnerHTML = function getInnerHTML(){
  return $getInnerHTML_0(this.local);
}
;
_.getNextSibling = function getNextSibling_0(){
  return this.local.getNextSibling();
}
;
_.getNodeName = function getNodeName_0(){
  return this.local.tagName_0;
}
;
_.getNodeType = function getNodeType_0(){
  return 1;
}
;
_.getNodeValue = function getNodeValue_0(){
  return this.local.tagName_0;
}
;
_.getStyle = function getStyle(){
  return $getStyle(this);
}
;
_.isPendingResolution = function isPendingResolution_0(){
  return this.pendingResolution;
}
;
_.linkedToRemote = function linkedToRemote_0(){
  return $remote(this) != ($clinit_ElementNull() , INSTANCE_2);
}
;
_.putRemote = function putRemote(remote, resolved){
  $putRemote(this, remote, resolved);
}
;
_.removeClassName = function removeClassName(className){
  return $removeClassName(this, className);
}
;
_.setClassName = function setClassName(className){
  var current, value_0;
  current = (value_0 = castToString($get_1(this.local.attributes_0, 'class')) , value_0 != null?value_0:'');
  if (current == className || current != null && equals_Ljava_lang_Object__Z__devirtual$(current, className)) {
    return;
  }
  $ensureRemoteCheck(this);
  $setClassName(this.local, className);
  setClassName_Ljava_lang_String__V__devirtual$($remote(this), className);
}
;
_.setPropertyString = function setPropertyString(name_0, value_0){
  $ensureRemoteCheck(this);
  $setPropertyString(this.local, name_0, value_0);
  setPropertyString_Ljava_lang_String_Ljava_lang_String__V__devirtual$($remote(this), name_0, value_0);
}
;
_.sinkEvents = function sinkEvents(eventBits){
  $sinkEvents(this.local, eventBits);
  sinkEvents_I_V__devirtual$($remote(this), eventBits);
}
;
_.toString_0 = function toString_16(){
  var cursor, fb, value_0, value0;
  fb = new FormatBuilder;
  $format(fb, '%s#%s.%s - %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$toString_1(this.local), blankTo((value0 = castToString($get_1(this.local.attributes_0, 'id')) , value0 != null?value0:'')), blankTo((value_0 = castToString($get_1(this.local.attributes_0, 'class')) , value_0 != null?value_0:'')), '(no uiobject)']));
  if (this.getChildCount() != 0) {
    $format(fb, '\n\t', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
    cursor = this.local;
    while (cursor.getChildCount() > 0) {
      cursor = castTo((!cursor.children && (cursor.children = new ArrayList) , cursor.children).get_2(0), 26);
      $format(fb, '%s ', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [cursor.getNodeName()]));
    }
  }
  return fb.sb.string;
}
;
_.typedRemote_0 = function typedRemote_0(){
  return castToJso($remote(this));
}
;
_.pendingResolution = false;
var Lcom_google_gwt_dom_client_Element_2_classLit = createForClass('com.google.gwt.dom.client', 'Element', 5, Lcom_google_gwt_dom_client_Node_2_classLit);
function AnchorElement(){
}

defineClass(160, 5, $intern_7, AnchorElement);
var Lcom_google_gwt_dom_client_AnchorElement_2_classLit = createForClass('com.google.gwt.dom.client', 'AnchorElement', 160, Lcom_google_gwt_dom_client_Element_2_classLit);
function AreaElement(){
}

defineClass(189, 5, $intern_7, AreaElement);
var Lcom_google_gwt_dom_client_AreaElement_2_classLit = createForClass('com.google.gwt.dom.client', 'AreaElement', 189, Lcom_google_gwt_dom_client_Element_2_classLit);
function BRElement(){
}

defineClass(173, 5, $intern_7, BRElement);
var Lcom_google_gwt_dom_client_BRElement_2_classLit = createForClass('com.google.gwt.dom.client', 'BRElement', 173, Lcom_google_gwt_dom_client_Element_2_classLit);
function BaseElement(){
}

defineClass(188, 5, $intern_7, BaseElement);
var Lcom_google_gwt_dom_client_BaseElement_2_classLit = createForClass('com.google.gwt.dom.client', 'BaseElement', 188, Lcom_google_gwt_dom_client_Element_2_classLit);
function BodyElement(){
}

defineClass(155, 5, $intern_7, BodyElement);
var Lcom_google_gwt_dom_client_BodyElement_2_classLit = createForClass('com.google.gwt.dom.client', 'BodyElement', 155, Lcom_google_gwt_dom_client_Element_2_classLit);
function ButtonElement(){
}

defineClass(156, 5, $intern_7, ButtonElement);
var Lcom_google_gwt_dom_client_ButtonElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ButtonElement', 156, Lcom_google_gwt_dom_client_Element_2_classLit);
function DListElement(){
}

defineClass(180, 5, $intern_7, DListElement);
var Lcom_google_gwt_dom_client_DListElement_2_classLit = createForClass('com.google.gwt.dom.client', 'DListElement', 180, Lcom_google_gwt_dom_client_Element_2_classLit);
function $clinit_DOMImpl(){
  $clinit_DOMImpl = emptyMethod;
  impl_0 = new DomDispatch;
  cache = new DOMImpl$DomImplCache;
}

function $createElement(doc, tag){
  return doc.createElement(tag);
}

function $eventGetType(this$static, evt){
  if (cache.lastEventForGetType != evt) {
    cache.lastEventForGetType = evt;
    cache.lastEventType = evt.type;
  }
  return cache.lastEventType;
}

defineClass(458, 1, {});
_.eventGetCurrentTarget = function eventGetCurrentTarget(event_0){
  var jso;
  jso = event_0.currentTarget;
  return !jso?null:new EventTarget_0(jso);
}
;
var cache, impl_0;
var Lcom_google_gwt_dom_client_DOMImpl_2_classLit = createForClass('com.google.gwt.dom.client', 'DOMImpl', 458, Ljava_lang_Object_2_classLit);
function DOMImpl$DomImplCache(){
}

defineClass(368, 1, {}, DOMImpl$DomImplCache);
var Lcom_google_gwt_dom_client_DOMImpl$DomImplCache_2_classLit = createForClass('com.google.gwt.dom.client', 'DOMImpl/DomImplCache', 368, Ljava_lang_Object_2_classLit);
defineClass(459, 458, {});
_.eventGetTarget = function eventGetTarget(evt){
  if (evt != this.lastEvent) {
    this.lastEventTarget = !evt.target?null:new EventTarget_0(evt.target);
    this.lastEvent = evt;
  }
  return this.lastEventTarget;
}
;
_.eventPreventDefault = function eventPreventDefault(evt){
  evt.preventDefault();
}
;
var Lcom_google_gwt_dom_client_DOMImplStandard_2_classLit = createForClass('com.google.gwt.dom.client', 'DOMImplStandard', 459, Lcom_google_gwt_dom_client_DOMImpl_2_classLit);
defineClass(460, 459, {});
_.eventGetCurrentTarget = function eventGetCurrentTarget_0(event_0){
  return new EventTarget_0(event_0.currentTarget || $wnd);
}
;
var Lcom_google_gwt_dom_client_DOMImplStandardBase_2_classLit = createForClass('com.google.gwt.dom.client', 'DOMImplStandardBase', 460, Lcom_google_gwt_dom_client_DOMImplStandard_2_classLit);
function DOMImplWebkit(){
  $clinit_DOMImpl();
}

defineClass(402, 460, {}, DOMImplWebkit);
_.eventGetTarget = function eventGetTarget_0(evt){
  var target = evt.target;
  target && target.nodeType == 3 && (target = target.parentNode);
  var wrapped = new EventTarget_0(target);
  return wrapped;
}
;
var Lcom_google_gwt_dom_client_DOMImplWebkit_2_classLit = createForClass('com.google.gwt.dom.client', 'DOMImplWebkit', 402, Lcom_google_gwt_dom_client_DOMImplStandardBase_2_classLit);
function DivElement(){
}

defineClass(153, 5, $intern_7, DivElement);
var Lcom_google_gwt_dom_client_DivElement_2_classLit = createForClass('com.google.gwt.dom.client', 'DivElement', 153, Lcom_google_gwt_dom_client_Element_2_classLit);
function $createElement_0(this$static, tagName){
  return $createElement_1(this$static.local, tagName);
}

function $createTextNode(this$static, data_0){
  return $createTextNode_0(this$static.local, data_0);
}

function Document_0(){
}

function get_7(){
  var local, doc;
  if (!doc_0) {
    local = new DocumentLocal;
    doc_0 = (doc = new Document_0 , doc.local = local , $clinit_LocalDom() , useRemoteDom && (doc.remote = $doc) , doc);
    local.document_0 = doc_0;
    register(doc_0);
  }
  return doc_0;
}

defineClass(219, 7, $intern_6, Document_0);
_.cast = function cast_0(){
  return this;
}
;
_.local_0 = function local_1(){
  return this.local;
}
;
_.remote_0 = function remote_2(){
  return this.remote;
}
;
_.typedRemote = function typedRemote_2(){
  return this.remote;
}
;
_.appendChild_0 = function appendChild_0(newChild){
  throw toJs(new UnsupportedOperationException);
}
;
_.getChild = function getChild_1(index_0){
  return this.local.getChild(index_0);
}
;
_.getChildCount = function getChildCount_0(){
  return this.local.getChildCount();
}
;
_.getChildNodes = function getChildNodes_0(){
  return this.local.getChildNodes();
}
;
_.getNextSibling = function getNextSibling_1(){
  return this.local.getNextSibling();
}
;
_.getNodeName = function getNodeName_1(){
  return '#document';
}
;
_.getNodeType = function getNodeType_1(){
  return 9;
}
;
_.getNodeValue = function getNodeValue_1(){
  return $getNodeValue();
}
;
_.getParentElement = function getParentElement_0(){
  return null;
}
;
_.getParentNode = function getParentNode_0(){
  return null;
}
;
_.linkedToRemote = function linkedToRemote_1(){
  return true;
}
;
_.node = function node_2(){
  return this;
}
;
_.putRemote = function putRemote_0(remote, resolved){
  throw toJs(new UnsupportedOperationException);
}
;
_.removeFromParent = function removeFromParent_0(){
  throw toJs(new UnsupportedOperationException);
}
;
var doc_0;
var Lcom_google_gwt_dom_client_Document_2_classLit = createForClass('com.google.gwt.dom.client', 'Document', 219, Lcom_google_gwt_dom_client_Node_2_classLit);
function $getChildren(this$static){
  !this$static.children && (this$static.children = new ArrayList);
  return this$static.children;
}

function $indexInParentChildren(this$static){
  return $getChildren(this$static.parentNode_0).indexOf_0(this$static);
}

function $provideLocalDomTree0(this$static, buf, depth){
  var child, idx, idx0, node;
  for (idx0 = 0; idx0 < depth; idx0++) {
    buf.string += ' ';
  }
  node = this$static.node();
  $append_2(buf, node.getNodeType());
  buf.string += ': ';
  switch (node.getNodeType()) {
    case 3:
    case 8:
    case 7:
      buf.string += '[';
      $append_5(buf, $replace_0($replace_0($replace_0(node.getNodeValue(), '\n', '\\n'), '\t', '\\t'), '\r', '\\r'));
      buf.string += ']';
      break;
    case 1:
      $append_5(buf, node.getNodeName().toUpperCase());
      buf.string += ' : ';
  }
  buf.string += '\n';
  if (node.getNodeType() == 1) {
    idx = 0;
    for (; idx < (!this$static.children && (this$static.children = new ArrayList) , this$static.children).size_1(); idx++) {
      child = castTo((!this$static.children && (this$static.children = new ArrayList) , this$static.children).get_2(idx), 26);
      $provideLocalDomTree0(child, buf, depth + 1);
    }
  }
  return buf;
}

function $setParentNode(this$static, local){
  this$static.parentNode_0 != local && !!this$static.parentNode_0 && !!local && $getChildren(this$static.parentNode_0).remove_1(this$static);
  this$static.parentNode_0 = local;
}

function $walk(this$static, consumer){
  var idx;
  consumer.accept_0(this$static);
  for (idx = 0; idx < (!this$static.children && (this$static.children = new ArrayList) , this$static.children).size_1(); idx++) {
    $walk(castTo((!this$static.children && (this$static.children = new ArrayList) , this$static.children).get_2(idx), 26), consumer);
  }
}

function nodeFor(nodeLocal){
  return !nodeLocal?null:nodeLocal.node();
}

defineClass(26, 1, $intern_8);
_.appendChild_0 = function appendChild_1(newChild){
  (!this.children && (this.children = new ArrayList) , this.children).add_0(newChild.local_0());
  $setParentNode(newChild.local_0(), this);
  return newChild;
}
;
_.getChild = function getChild_2(index_0){
  return $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(this), index_0);
}
;
_.getChildCount = function getChildCount_1(){
  if (!this.children) {
    return 0;
  }
  return getLength__I__devirtual$(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(this).impl);
}
;
_.getChildNodes = function getChildNodes_1(){
  return new NodeList_0(new NodeListLocal((!this.children && (this.children = new ArrayList) , this.children)));
}
;
_.getNextSibling = function getNextSibling_2(){
  var nextLocal;
  if (!this.parentNode_0) {
    return null;
  }
  nextLocal = castTo(indexedOrNullWithDelta($getChildren(this.parentNode_0), this), 26);
  if (nextLocal == this) {
    return null;
  }
  return !nextLocal?null:nextLocal.node();
}
;
_.getNodeValue = function getNodeValue_2(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getParentElement = function getParentElement_1(){
  return !this.parentNode_0?null:castTo(this.parentNode_0.node(), 5);
}
;
_.getParentNode = function getParentNode_1(){
  return nodeFor(this.parentNode_0);
}
;
_.insertBefore_0 = function insertBefore_1(newChild, refChild){
  var idx;
  if (!refChild) {
    (!this.children && (this.children = new ArrayList) , this.children).add_0(newChild.local_0());
  }
   else {
    idx = (!this.children && (this.children = new ArrayList) , this.children).indexOf_0(refChild.local_0());
    checkArgument_0(idx != -1);
    (!this.children && (this.children = new ArrayList) , this.children).add_1(idx, newChild.local_0());
  }
  $setParentNode(newChild.local_0(), this);
  return newChild;
}
;
_.removeChild_0 = function removeChild_0(oldChild){
  $setParentNode(oldChild.local_0(), null);
  (!this.children && (this.children = new ArrayList) , this.children).remove_1(oldChild.local_0());
  return oldChild;
}
;
_.removeFromParent = function removeFromParent_1(){
  removeFromParent_2(this);
}
;
var Lcom_google_gwt_dom_client_NodeLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'NodeLocal', 26, Ljava_lang_Object_2_classLit);
function $createElement_1(this$static, tagName){
  var element, local;
  local = new ElementLocal(this$static, tagName);
  element = $putLocal(($clinit_LocalDom() , $createElement0(instance, tagName)), local);
  return element;
}

function $createTextNode_0(this$static, data_0){
  var local, text_0;
  local = new TextLocal(this$static, data_0);
  text_0 = new Text_0(local);
  local.textNode = text_0;
  return text_0;
}

function $getNodeValue(){
  throw toJs(new UnsupportedOperationException);
}

function DocumentLocal(){
}

defineClass(222, 26, $intern_8, DocumentLocal);
_.node = function node_3(){
  return this.document_0;
}
;
_.appendOuterHtml = function appendOuterHtml(builder){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeName = function getNodeName_2(){
  return '#document';
}
;
_.getNodeType = function getNodeType_2(){
  return 9;
}
;
_.getNodeValue = function getNodeValue_3(){
  return $getNodeValue();
}
;
var Lcom_google_gwt_dom_client_DocumentLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'DocumentLocal', 222, Lcom_google_gwt_dom_client_NodeLocal_2_classLit);
function $appendChild(this$static, newChild){
  var toAppend;
  $clinit_LocalDom();
  if ($isPending0(instance, this$static)) {
    return null;
  }
  toAppend = $resolvedOrPending(newChild);
  return nodeFor_0(this$static.appendChild(toAppend));
}

function $getParentElementRemote(this$static){
  var parentElement = this$static.parentElement;
  if (parentElement) {
    return parentElement;
  }
  var parentNode = this$static.parentNode;
  if (parentNode && parentNode.nodeType == 1) {
    return parentNode;
  }
   else {
    return null;
  }
}

function $indexInParentChildren_0(this$static){
  var idx = 0;
  var size_0 = this$static.parentNode.childNodes.length;
  for (; idx < size_0; idx++) {
    var node = this$static.parentNode.childNodes.item(idx);
    if (node == this$static) {
      return idx;
    }
  }
  return -1;
}

function $insertBefore_0(this$static, newChild, refChild){
  var newChildDom, refChildDom;
  $clinit_LocalDom();
  if ($isPending0((null , instance), this$static)) {
    return null;
  }
  newChildDom = $resolvedOrPending(newChild);
  refChildDom = $resolvedOrPending(refChild);
  return nodeFor_0(this$static.insertBefore(newChildDom, refChildDom));
}

function $insertBefore0(this$static, newChild, refChild){
  return this$static.insertBefore(newChild, refChild);
}

function $removeChild0(this$static, oldChild){
  if (oldChild.parentNode == null && oldChild.nodeType == 3) {
    var children = this$static.childNodes;
    for (var i = 0; i < children.length; i++) {
      var node = children[i];
      if (node.nodeType == 3 && node.data == oldChild.data) {
        this$static.removeChild(node);
        return oldChild;
      }
    }
  }
  return this$static.removeChild(oldChild);
}

function $resolvedOrPending(node){
  if (!node) {
    return null;
  }
  if (node.linkedToRemote()) {
    return castToJso(node.remote_0());
  }
   else {
    if (node.resolvedEventId > 0) {
      $clinit_LocalDom();
      $ensureRemote0((null , instance), node);
      return castToJso(node.remote_0());
    }
     else {
      return $clinit_LocalDom() , $ensureRemoteNodeMaybePendingResolution0((null , instance), node);
    }
  }
}

function appendChild_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(this$static, newChild){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.appendChild_0(newChild):$appendChild(this$static, newChild);
}

function getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(this$static){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.getChildNodes():new NodeList_0(this$static.childNodes);
}

function insertBefore_Lcom_google_gwt_dom_client_Node_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(this$static, newChild, refChild){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.insertBefore_0(newChild, refChild):$insertBefore_0(this$static, newChild, refChild);
}

function is_0(o){
  try {
    return !!o && !!o.nodeType;
  }
   catch (e) {
    return false;
  }
}

function node__Lcom_google_gwt_dom_client_Node___devirtual$(this$static){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.node():($clinit_LocalDom() , $nodeFor0((null , instance), this$static, false));
}

function removeChild_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(this$static, oldChild){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.removeChild_0(oldChild):(oldChild.linkedToRemote() && $removeChild0(this$static, castToJso(oldChild.remote_0())) , null);
}

function removeFromParent__V__devirtual$(this$static){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.removeFromParent():(removeFromParent_2(this$static) , undefined);
}

function $createTextNode0(this$static, data_0){
  return this$static.createTextNode(data_0);
}

function $generateFromOuterHtml(this$static, outer){
  var div = this$static.createElement('div');
  div.innerHTML = outer;
  return div.childNodes[0];
}

function $eventGetCurrentTarget(this$static, event_0){
  return castTo(this$static.remote, 51).domImpl.eventGetCurrentTarget(event_0);
}

function $eventGetTarget(this$static, evt){
  return castTo(this$static.remote, 51).domImpl.eventGetTarget(evt);
}

function $eventGetType_0(this$static, evt){
  return $eventGetType(castTo(this$static.remote, 51).domImpl, evt);
}

function $eventPreventDefault(this$static, evt){
  $clinit_LocalDom();
  $eventMod0((null , instance), evt, 'eventPreventDefault');
  this$static.local.eventPreventDefault(evt);
  this$static.remote.eventPreventDefault(evt);
}

function $getInnerHTML(){
  throw toJs(new DomDispatch$RemoteOnlyException);
}

function DomDispatch(){
  new DomResolver;
  this.local = new DomDispatchNull;
  this.remote = new DomDispatchNull;
  this.local = new DomDispatchLocal;
  this.remote = new DomDispatchRemote;
  castTo(this.remote, 51).domImpl = new DOMImplWebkit;
}

defineClass(366, 1, {}, DomDispatch);
_.eventPreventDefault = function eventPreventDefault_0(evt){
  $eventPreventDefault(this, evt);
}
;
var Lcom_google_gwt_dom_client_DomDispatch_2_classLit = createForClass('com.google.gwt.dom.client', 'DomDispatch', 366, Ljava_lang_Object_2_classLit);
function UnsupportedOperationException(){
  RuntimeException.call(this);
}

function UnsupportedOperationException_0(message){
  RuntimeException_0.call(this, message);
}

defineClass(13, 11, $intern_1, UnsupportedOperationException, UnsupportedOperationException_0);
var Ljava_lang_UnsupportedOperationException_2_classLit = createForClass('java.lang', 'UnsupportedOperationException', 13, Ljava_lang_RuntimeException_2_classLit);
function DomDispatch$RemoteOnlyException(){
  UnsupportedOperationException.call(this);
}

defineClass(367, 13, $intern_1, DomDispatch$RemoteOnlyException);
var Lcom_google_gwt_dom_client_DomDispatch$RemoteOnlyException_2_classLit = createForClass('com.google.gwt.dom.client', 'DomDispatch/RemoteOnlyException', 367, Ljava_lang_UnsupportedOperationException_2_classLit);
function DomDispatchLocal(){
}

defineClass(401, 1, {}, DomDispatchLocal);
_.eventPreventDefault = function eventPreventDefault_1(evt){
  $clinit_LocalDom();
  $eventMod0((null , instance), evt, 'eventPreventDefault');
}
;
var Lcom_google_gwt_dom_client_DomDispatchLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'DomDispatchLocal', 401, Ljava_lang_Object_2_classLit);
function DomDispatchNull(){
}

defineClass(151, 1, {}, DomDispatchNull);
_.eventPreventDefault = function eventPreventDefault_2(evt){
  throw toJs(new UnsupportedOperationException);
}
;
var Lcom_google_gwt_dom_client_DomDispatchNull_2_classLit = createForClass('com.google.gwt.dom.client', 'DomDispatchNull', 151, Ljava_lang_Object_2_classLit);
function $createElement_2(this$static, tagName){
  return $createElement(get_7().remote, tagName);
}

function DomDispatchRemote(){
}

defineClass(51, 1, {51:1}, DomDispatchRemote);
_.eventPreventDefault = function eventPreventDefault_3(evt){
  this.domImpl.eventPreventDefault(evt);
}
;
var Lcom_google_gwt_dom_client_DomDispatchRemote_2_classLit = createForClass('com.google.gwt.dom.client', 'DomDispatchRemote', 51, Ljava_lang_Object_2_classLit);
function addClassName_0(domElement, className){
  var idx, oldClassName;
  className = trimClassName(className);
  oldClassName = getClassName__Ljava_lang_String___devirtual$(domElement);
  idx = indexOfName(oldClassName, className);
  if (idx == -1) {
    oldClassName.length > 0?setClassName_Ljava_lang_String__V__devirtual$(domElement, oldClassName + ' ' + className):setClassName_Ljava_lang_String__V__devirtual$(domElement, className);
    return true;
  }
  return false;
}

function indexOfName(nameList, name_0){
  var idx, last, lastPos;
  idx = nameList.indexOf(name_0);
  while (idx != -1) {
    if (idx == 0 || ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(idx - 1, nameList.length) , nameList.charCodeAt(idx - 1) == 32)) {
      last = idx + name_0.length;
      lastPos = nameList.length;
      if (last == lastPos || last < lastPos && ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(last, nameList.length) , nameList.charCodeAt(last) == 32)) {
        break;
      }
    }
    idx = nameList.indexOf(name_0, idx + 1);
  }
  return idx;
}

function removeClassName_0(domElement, className){
  var begin, end, idx, newClassName, oldClassName;
  className = trimClassName(className);
  oldClassName = getClassName__Ljava_lang_String___devirtual$(domElement);
  idx = indexOfName(oldClassName, className);
  if (idx != -1) {
    begin = $trim(oldClassName.substr(0, idx));
    end = $trim($substring(oldClassName, idx + className.length));
    begin.length == 0?(newClassName = end):end.length == 0?(newClassName = begin):(newClassName = begin + ' ' + end);
    setClassName_Ljava_lang_String__V__devirtual$(domElement, newClassName);
    return true;
  }
  return false;
}

function trimClassName(className){
  className = $trim(className);
  return className;
}

function stream0(ref){
  var idx, list;
  list = new ArrayList;
  for (idx = 0; idx < ref.getLength(); idx++) {
    list.add_0(ref.getItem(idx));
  }
  return list.stream();
}

function removeFromParent_2(domNode){
  var parent_0, parentDomNode;
  parent_0 = hasJavaObjectVirtualDispatch(domNode)?domNode.getParentElement():($clinit_LocalDom() , castTo(nodeFor_0($getParentElementRemote(domNode)), 5));
  if (parent_0) {
    parentDomNode = $sameTreeNodeFor(parent_0, domNode);
    removeChild_Lcom_google_gwt_dom_client_Node__Lcom_google_gwt_dom_client_Node___devirtual$(parentDomNode, node__Lcom_google_gwt_dom_client_Node___devirtual$(domNode));
  }
}

function shortLog(node){
  var remoteHash;
  if (!node) {
    return '(null)';
  }
  remoteHash = '';
  instanceOf(node, 53) && node__Lcom_google_gwt_dom_client_Node___devirtual$(castTo(node, 53)).linkedToRemote() && (remoteHash = '' + hashCode__I__devirtual$(node__Lcom_google_gwt_dom_client_Node___devirtual$(castTo(node, 53)).remote_0()));
  return format_0('%s%s%s%s%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, ['    ', padStringRight(hasJavaObjectVirtualDispatch(node)?node.getNodeName():node.nodeName, 12), '  ', padStringRight('' + hashCode__I__devirtual$(node), 16), padStringRight(remoteHash, 16), (hasJavaObjectVirtualDispatch(node)?node.getNodeType():node.nodeType) == 3?($clinit_CommonUtils() , trimToWsChars(normalizeWhitespace(hasJavaObjectVirtualDispatch(node)?node.getNodeValue():node.nodeValue), 50, '...')):'']));
}

function shortLog_0(list){
  var domNode, domNode$iterator, idx, stringBuilder;
  idx = 0;
  stringBuilder = new StringBuilder;
  for (domNode$iterator = list.iterator(); domNode$iterator.hasNext_0();) {
    domNode = castToAllowJso(domNode$iterator.next_1(), 8);
    $append_5(stringBuilder, format_0('%s%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [padTwo(idx++), shortLog(domNode)])));
  }
  return stringBuilder.string;
}

function DomResolver(){
}

defineClass(400, 1, {}, DomResolver);
var Lcom_google_gwt_dom_client_DomResolver_2_classLit = createForClass('com.google.gwt.dom.client', 'DomResolver', 400, Ljava_lang_Object_2_classLit);
function Element$ElementImplAccess(this$0){
  this.this$01 = this$0;
}

defineClass(47, 1, {}, Element$ElementImplAccess);
var Lcom_google_gwt_dom_client_Element$ElementImplAccess_2_classLit = createForClass('com.google.gwt.dom.client', 'Element/ElementImplAccess', 47, Ljava_lang_Object_2_classLit);
function $clinit_ElementLocal(){
  $clinit_ElementLocal = emptyMethod;
  new RegExp('[A-Za-z0-9\\-_]+');
}

function $appendOuterHtml(this$static, builder){
  var applyStyleAttribute, styleAttributeValue, value_0;
  this$static.eventBits != 0 && (value_0 = castToString($get_1(this$static.attributes_0, 'id')) , value_0 != null?value_0:'').length == 0 && $setId(this$static, '__localdom__' + ++_idCounter);
  $append_5(builder.sb, '<');
  $appendHtmlConstant(builder, this$static.tagName_0);
  styleAttributeValue = castToString($get_1(this$static.attributes_0, 'style'));
  if (this$static.attributes_0.size_0 != 0) {
    applyStyleAttribute = !this$static.element.style_0 || this$static.hasUnparsedStyle && $getStyle(this$static.element).local.properties.size_0 == 0;
    (new LightMap$EntrySet(this$static.attributes_0)).forEach_0(new ElementLocal$lambda$7$Type(applyStyleAttribute, builder));
  }
  if (!!$getStyle(this$static.element) && $getStyle(this$static.element).local.properties.size_0 != 0) {
    $append_5(builder.sb, ' style="');
    $clinit_CommonUtils();
    if (!(styleAttributeValue == null || styleAttributeValue.length == 0)) {
      $append_5(builder.sb, styleAttributeValue);
      $append_5(builder.sb, '; ');
    }
    (new LightMap$EntrySet($getStyle(this$static.element).local.properties)).forEach_0(new ElementLocal$lambda$8$Type(builder));
    $append_5(builder.sb, '"');
  }
  $append_5(builder.sb, '>');
  $containsUnescapedText(this$static)?(!this$static.children && (this$static.children = new ArrayList) , this$static.children).stream().forEach_0(new ElementLocal$lambda$4$Type(builder)):(!this$static.children && (this$static.children = new ArrayList) , this$static.children).stream().forEach_0(new ElementLocal$lambda$5$Type(builder));
  if (!isSelfClosingTag(this$static.tagName_0)) {
    $append_5(builder.sb, '<\/');
    $appendHtmlConstant(builder, this$static.tagName_0);
    $append_5(builder.sb, '>');
  }
}

function $clearChildrenAndAttributes0(this$static){
  (!this$static.children && (this$static.children = new ArrayList) , this$static.children).clear_0();
  (new LightMap$EntrySet(this$static.attributes_0)).clear_0();
}

function $containsUnescapedText(this$static){
  if ($equalsIgnoreCase(this$static.tagName_0, 'style') || $equalsIgnoreCase(this$static.tagName_0, 'script')) {
    checkState((!this$static.children && (this$static.children = new ArrayList) , this$static.children).stream().allMatch(new ElementLocal$lambda$6$Type));
    return true;
  }
   else {
    return false;
  }
}

function $getInnerHTML_0(this$static){
  var builder;
  builder = new UnsafeHtmlBuilder;
  $containsUnescapedText(this$static)?(!this$static.children && (this$static.children = new ArrayList) , this$static.children).stream().forEach_0(new ElementLocal$lambda$4$Type(builder)):(!this$static.children && (this$static.children = new ArrayList) , this$static.children).stream().forEach_0(new ElementLocal$lambda$5$Type(builder));
  return (new UnsafeHtmlBuilder$SafeHtmlString(builder.sb.string)).asString();
}

function $getOuterHtml(this$static){
  var builder;
  builder = new UnsafeHtmlBuilder;
  $appendOuterHtml(this$static, builder);
  return (new UnsafeHtmlBuilder$SafeHtmlString(builder.sb.string)).asString();
}

function $orSunkEventsOfAllChildren(this$static, sunk){
  var child, child$iterator;
  for (child$iterator = (!this$static.children && (this$static.children = new ArrayList) , this$static.children).iterator(); child$iterator.hasNext_0();) {
    child = castTo(child$iterator.next_1(), 26);
    instanceOf(child, 80) && (sunk = $orSunkEventsOfAllChildren(castTo(child, 80), sunk));
  }
  sunk |= this$static.eventBits;
  return sunk;
}

function $removeAttribute(this$static, name_0){
  $remove_2(this$static.attributes_0, name_0);
}

function $setAttribute(this$static, name_0, value_0){
  $put_1(this$static.attributes_0, name_0, value_0);
}

function $setClassName(this$static, className){
  $put_1(this$static.attributes_0, 'class', className);
}

function $setId(this$static, id_0){
  $put_1(this$static.attributes_0, 'id', id_0);
}

function $setInnerText(this$static, text_0){
  $clinit_CommonUtils();
  if (text_0 == null || text_0.length == 0)
  ;
  else {
    (!this$static.children && (this$static.children = new ArrayList) , this$static.children).clear_0();
    this$static.appendChild_0($createTextNode_0(this$static.ownerDocument, text_0));
  }
}

function $setPropertyString(this$static, name_0, value_0){
  $put_1(this$static.attributes_0, name_0, value_0);
}

function $sinkEvents(this$static, eventBits){
  this$static.eventBits |= eventBits;
}

function $toString_1(this$static){
  var number;
  return $getName(getClass__Ljava_lang_Class___devirtual$(this$static)) + '@' + (number = hashCode__I__devirtual$(this$static) >>> 0 , number.toString(16)) + '\n\t' + this$static.tagName_0;
}

function ElementLocal(document_Jvm, tagName){
  $clinit_ElementLocal();
  this.attributes_0 = new LightMap;
  this.ownerDocument = document_Jvm;
  this.tagName_0 = tagName;
}

function lambda$4(builder_0, node_1){
  $clinit_ElementLocal();
  $appendUnescaped(castTo(node_1, 125), builder_0);
}

function lambda$5(builder_0, child_1){
  $clinit_ElementLocal();
  child_1.appendOuterHtml(builder_0);
}

function lambda$7(applyStyleAttribute_0, builder_1, e_2){
  $clinit_ElementLocal();
  if ($equals_0(castToString(e_2.getKey()), 'style') && !applyStyleAttribute_0) {
    return;
  }
  $append_5(builder_1.sb, ' ');
  $appendEscaped(builder_1, castToString(e_2.getKey()));
  $append_5(builder_1.sb, '="');
  $appendEscaped(builder_1, castToString(e_2.getValue()));
  $append_5(builder_1.sb, '"');
}

function lambda$8(builder_0, e_1){
  $clinit_ElementLocal();
  $appendEscaped(builder_0, declarativeCssName(castToString(e_1.getKey())));
  $append_5(builder_0.sb, ':');
  $appendEscaped(builder_0, castToString(e_1.getValue()));
  $append_5(builder_0.sb, '; ');
}

defineClass(80, 26, {211:1, 8:1, 80:1, 53:1, 26:1}, ElementLocal);
_.addClassName = function addClassName_1(className){
  return addClassName_0(this, className);
}
;
_.appendOuterHtml = function appendOuterHtml_0(builder){
  $appendOuterHtml(this, builder);
}
;
_.getAttributeMap = function getAttributeMap_0(){
  return this.attributes_0;
}
;
_.getClassName = function getClassName_0(){
  var value_0;
  return value_0 = castToString($get_1(this.attributes_0, 'class')) , value_0 != null?value_0:'';
}
;
_.getInnerHTML = function getInnerHTML_0(){
  return $getInnerHTML_0(this);
}
;
_.getNodeName = function getNodeName_3(){
  return this.tagName_0;
}
;
_.getNodeType = function getNodeType_3(){
  return 1;
}
;
_.getNodeValue = function getNodeValue_4(){
  return this.tagName_0;
}
;
_.getStyle = function getStyle_0(){
  return $getStyle(this.element);
}
;
_.node = function node_4(){
  return this.element;
}
;
_.removeClassName = function removeClassName_1(className){
  return removeClassName_0(this, className);
}
;
_.setClassName = function setClassName_0(className){
  $setClassName(this, className);
}
;
_.setPropertyString = function setPropertyString_0(name_0, value_0){
  $setPropertyString(this, name_0, value_0);
}
;
_.sinkEvents = function sinkEvents_0(eventBits){
  $sinkEvents(this, eventBits);
}
;
_.toString_0 = function toString_17(){
  return $toString_1(this);
}
;
_.eventBits = 0;
_.hasUnparsedStyle = false;
var _idCounter = 0;
var Lcom_google_gwt_dom_client_ElementLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'ElementLocal', 80, Lcom_google_gwt_dom_client_NodeLocal_2_classLit);
function ElementLocal$lambda$4$Type(builder_0){
  this.builder_0 = builder_0;
}

defineClass(118, 1, $intern_5, ElementLocal$lambda$4$Type);
_.accept_0 = function accept_1(arg0){
  lambda$4(this.builder_0, castTo(arg0, 26));
}
;
var Lcom_google_gwt_dom_client_ElementLocal$lambda$4$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 118, Ljava_lang_Object_2_classLit);
function ElementLocal$lambda$5$Type(builder_0){
  this.builder_0 = builder_0;
}

defineClass(119, 1, $intern_5, ElementLocal$lambda$5$Type);
_.accept_0 = function accept_2(arg0){
  lambda$5(this.builder_0, castTo(arg0, 26));
}
;
var Lcom_google_gwt_dom_client_ElementLocal$lambda$5$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 119, Ljava_lang_Object_2_classLit);
function ElementLocal$lambda$6$Type(){
}

defineClass(347, 1, {}, ElementLocal$lambda$6$Type);
_.negate = function negate_2(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_3(arg0){
  return $clinit_ElementLocal() , castTo(arg0, 26).getNodeType() == 3;
}
;
var Lcom_google_gwt_dom_client_ElementLocal$lambda$6$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 347, Ljava_lang_Object_2_classLit);
function ElementLocal$lambda$7$Type(applyStyleAttribute_0, builder_1){
  this.applyStyleAttribute_0 = applyStyleAttribute_0;
  this.builder_1 = builder_1;
}

defineClass(348, 1, $intern_5, ElementLocal$lambda$7$Type);
_.accept_0 = function accept_3(arg0){
  lambda$7(this.applyStyleAttribute_0, this.builder_1, castTo(arg0, 16));
}
;
_.applyStyleAttribute_0 = false;
var Lcom_google_gwt_dom_client_ElementLocal$lambda$7$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 348, Ljava_lang_Object_2_classLit);
function ElementLocal$lambda$8$Type(builder_0){
  this.builder_0 = builder_0;
}

defineClass(349, 1, $intern_5, ElementLocal$lambda$8$Type);
_.accept_0 = function accept_4(arg0){
  lambda$8(this.builder_0, castTo(arg0, 16));
}
;
var Lcom_google_gwt_dom_client_ElementLocal$lambda$8$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 349, Ljava_lang_Object_2_classLit);
defineClass(449, 26, $intern_8);
_.appendChild_0 = function appendChild_2(newChild){
  return null;
}
;
_.getChild = function getChild_3(index_0){
  throw toJs(new UnsupportedOperationException);
}
;
_.getChildCount = function getChildCount_2(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getChildNodes = function getChildNodes_2(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNextSibling = function getNextSibling_3(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getParentElement = function getParentElement_2(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getParentNode = function getParentNode_2(){
  throw toJs(new UnsupportedOperationException);
}
;
_.insertBefore_0 = function insertBefore_2(newChild, refChild){
  return null;
}
;
_.node = function node_5(){
  throw toJs(new UnsupportedOperationException);
}
;
_.removeChild_0 = function removeChild_1(oldChild){
  return null;
}
;
_.removeFromParent = function removeFromParent_3(){
}
;
_.toString_0 = function toString_18(){
  return format_0('%s: null::remote-placeholder', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$getSimpleName(this.___clazz)]));
}
;
var Lcom_google_gwt_dom_client_NodeLocalNull_2_classLit = createForClass('com.google.gwt.dom.client', 'NodeLocalNull', 449, Lcom_google_gwt_dom_client_NodeLocal_2_classLit);
function $clinit_ElementNull(){
  $clinit_ElementNull = emptyMethod;
  INSTANCE_2 = new ElementNull;
}

function $getTagName(){
  throw toJs(new UnsupportedOperationException);
}

function ElementNull(){
}

defineClass(354, 449, {211:1, 8:1, 53:1, 26:1}, ElementNull);
_.addClassName = function addClassName_2(className){
  return false;
}
;
_.appendOuterHtml = function appendOuterHtml_1(builder){
  throw toJs(new UnsupportedOperationException);
}
;
_.getAttributeMap = function getAttributeMap_1(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getClassName = function getClassName_1(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getInnerHTML = function getInnerHTML_1(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeName = function getNodeName_4(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeType = function getNodeType_4(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeValue = function getNodeValue_5(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getStyle = function getStyle_1(){
  throw toJs(new UnsupportedOperationException);
}
;
_.removeClassName = function removeClassName_2(className){
  return false;
}
;
_.setClassName = function setClassName_1(className){
}
;
_.setPropertyString = function setPropertyString_1(name_0, value_0){
}
;
_.sinkEvents = function sinkEvents_1(eventBits){
}
;
_.toString_0 = function toString_19(){
  return format_0('%s: null::remote-placeholder', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$getSimpleName(this.___clazz)])) + '\n\t' + $getTagName();
}
;
var INSTANCE_2;
var Lcom_google_gwt_dom_client_ElementNull_2_classLit = createForClass('com.google.gwt.dom.client', 'ElementNull', 354, Lcom_google_gwt_dom_client_NodeLocalNull_2_classLit);
function $clinit_ElementRemote(){
  $clinit_ElementRemote = emptyMethod;
  cache_0 = new ElementRemote$RemoteCache;
}

function $buildOuterHtml(this$static){
  $clinit_ElementRemote();
  function escapeHtml(str, buffer){
    var node = document.createTextNode(str);
    buffer.div.appendChild(node);
    var result_0 = buffer.div.innerHTML;
    buffer.div.removeChild(node);
    return result_0;
  }

  function addNodeToBuiltHtml(node, buffer, depth){
    var buf = buffer.buf;
    switch (node.nodeType) {
      case 3:
        buf += escapeHtml(node.data, buffer);
        break;
      case 7:
        buf += '<?';
        buf += node.name;
        buf += ' ';
        buf += escapeHtml(node.data, buffer);
        buf += '?>';
      case 8:
        buf += '<!--';
        buf += escapeHtml(node.data, buffer);
        buf += '-->';
        break;
      case 1:
        buf += '<';
        buf += node.tagName;
        if (node.attributes.length > 0) {
          for (var idx = 0; idx < node.attributes.length; idx++) {
            buf += ' ';
            buf += node.attributes[idx].name;
            buf += '="';
            buf += escapeHtml(node.attributes[idx].value, buffer).split('"').join('&quot;');
            buf += '"';
          }
        }

        buf += '>';
        var idx = 0;
        var size_0 = node.childNodes.length;
        buffer.buf = buf;
        for (; idx < size_0; idx++) {
          var child = node.childNodes.item(idx);
          addNodeToBuiltHtml(child, buffer, depth + 1);
        }

        buf = buffer.buf;
        var re = /^(?:area|base|br|col|command|embed|hr|img|input|keygen|link|meta|param|source|track|wbr)$/i;
        if (!node.tagName.match(re)) {
          buf += '<\/';
          buf += node.tagName;
          buf += '>';
        }

        break;
      default:throw 'node not handled:' + node;
    }
    buffer.buf = buf;
  }

  var buffer_0 = {'buf':'', 'div':$doc.createElement('div')};
  addNodeToBuiltHtml(this$static, buffer_0, 0);
  return buffer_0.buf;
}

function $getAttributeList(this$static){
  var result_0 = [];
  var attrs = this$static.attributes;
  for (var i = 0; i < attrs.length; i++) {
    result_0.push(attrs[i].name);
    result_0.push(attrs[i].value);
  }
  return result_0;
}

function $getAttributeMap(this$static){
  var arr, idx, result_0;
  result_0 = new StringMap;
  arr = $getAttributeList(this$static);
  for (idx = 0; idx < arr.length; idx += 2) {
    $put_6(result_0, arr[idx], arr[idx + 1]);
  }
  return result_0;
}

function $getOuterHtml_0(this$static){
  $clinit_ElementRemote();
  return this$static.outerHTML;
}

function $getStyle_0(){
  throw toJs(new UnsupportedOperationException);
}

function $getTagNameRemote(this$static){
  $clinit_ElementRemote();
  return this$static.tagName;
}

function $provideRemoteDomTree0(this$static){
  $clinit_ElementRemote();
  function addNode(node, buffer, depth){
    var buf = buffer.buf;
    for (var idx = 0; idx < depth; idx++) {
      buf += ' ';
    }
    buf += node.nodeType;
    buf += ': ';
    switch (node.nodeType) {
      case 3:
      case 8:
        buf += '[';
        buf += node.data.split('\n').join('\\n').split('\t').join('\\t').split('\r').join('\\r');
        buf += ']';
        break;
      case 1:
        buf += node.tagName;
        buf += ' : ';
        break;
    }
    buf += '\n';
    buffer.buf = buf;
    if (node.nodeType == 1) {
      var idx = 0;
      var size_0 = node.childNodes.length;
      for (; idx < size_0; idx++) {
        var child = node.childNodes.item(idx);
        addNode(child, buffer, depth + 1);
      }
    }
  }

  var buffer_0 = {'buf':''};
  addNode(this$static, buffer_0, 0);
  return buffer_0.buf;
}

function $provideRemoteIndex(this$static, debug){
  $clinit_ElementRemote();
  var result_0 = {'hasNode':null, 'root':null, 'indicies':[], 'ancestors':[], 'sizes':[], 'debugData':[], 'remoteDefined':[], 'debugLog':''};
  var cursor = this$static;
  while (true) {
    var hasNode = ($clinit_LocalDom() , instance.remoteLookup.containsKey_0(cursor));
    if (hasNode) {
      result_0.hasNode = cursor;
      break;
    }
    var parent_0 = cursor.parentElement;
    if (parent_0 == null) {
      result_0.root = cursor;
      break;
    }
    var idx = 0;
    var size_0 = parent_0.childNodes.length;
    for (; idx < size_0; idx++) {
      var node = parent_0.childNodes.item(idx);
      if (debug) {
        result_0.debugLog += 'Checking node - depth: ' + result_0.indicies.length;
        result_0.debugLog += ' - idx: ' + idx;
        result_0.debugLog += ' - Node type: ' + node.nodeType;
        result_0.debugLog += ' - Node name: ' + node.nodeName;
        result_0.debugLog += ' - Cursor type: ' + node.nodeType;
        result_0.debugLog += ' - Cursor name: ' + node.nodeName;
        result_0.debugLog += '\n';
      }
      if (node == cursor) {
        result_0.indicies.push(idx);
        result_0.ancestors.push(cursor);
        var className = cursor.className;
        !className.indexOf && typeof className.baseVal == 'string' && (className = className.baseVal);
        result_0.remoteDefined.push(className.indexOf('__localdom-remote-defined') != -1);
        break;
      }
    }
    result_0.sizes.push(size_0);
    if (debug) {
      var buf = '';
      var idx = 0;
      for (; idx < size_0; idx++) {
        var node = parent_0.childNodes.item(idx);
        buf += node.nodeType;
        buf += ': ';
        switch (node.nodeType) {
          case 3:
          case 8:
            buf += '[';
            buf += node.data.split('\n').join('\\n').split('\t').join('\\t');
            buf += ']';
            break;
          case 1:
            buf += node.tagName;
            buf += ' : ';
            break;
        }
        buf += '\n';
      }
      result_0.debugData.push(buf);
    }
    cursor = parent_0;
  }
  return result_0;
}

function $removeFromParent0(this$static){
  $clinit_ElementRemote();
  this$static.parentElement.removeChild(this$static);
}

function $setAttribute_0(this$static, name_0, value_0){
  $clinit_ElementRemote();
  this$static.setAttribute(name_0, value_0);
}

function $setInnerHTML(this$static, html){
  $clinit_ElementRemote();
  this$static.innerHTML = html || '';
}

function $setPropertyString_0(this$static, name_0, value_0){
  $clinit_ElementRemote();
  this$static[name_0] = value_0;
}

function $sinkEvents_0(){
  throw toJs(new UnsupportedOperationException);
}

function addClassName_Ljava_lang_String__Z__devirtual$(this$static, className){
  $clinit_ElementRemote();
  return hasJavaObjectVirtualDispatch(this$static)?this$static.addClassName(className):addClassName_0(this$static, className);
}

function commaSeparatedBoolsToList(string){
  $clinit_ElementRemote();
  if (string.length == 0) {
    return $clinit_Collections() , $clinit_Collections() , EMPTY_LIST;
  }
  return castTo((new Arrays$ArrayList($split(string, ',', 0))).stream().map_1(new ElementRemote$0methodref$parseBoolean$Type).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
}

function commaSeparatedIntsToList(string){
  $clinit_ElementRemote();
  if (string.length == 0) {
    return $clinit_Collections() , $clinit_Collections() , EMPTY_LIST;
  }
  return castTo((new Arrays$ArrayList($split(string, ',', 0))).stream().map_1(new ElementRemote$1methodref$parseInt$Type).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
}

function getClassName__Ljava_lang_String___devirtual$(this$static){
  $clinit_ElementRemote();
  return hasJavaObjectVirtualDispatch(this$static)?this$static.getClassName():this$static.className || '';
}

function is_1(o){
  $clinit_ElementRemote();
  var is0;
  if (cache_0.lastIs == o) {
    return cache_0.lastIsResult;
  }
  is0 = isRemote(o);
  cache_0.lastIs = o;
  cache_0.lastIsResult = is0;
  return is0;
}

function isRemote(o){
  if (is_0(o)) {
    return o.nodeType == 1;
  }
  return false;
}

function removeClassName_Ljava_lang_String__Z__devirtual$(this$static, className){
  $clinit_ElementRemote();
  return hasJavaObjectVirtualDispatch(this$static)?this$static.removeClassName(className):removeClassName_0(this$static, className);
}

function setClassName_Ljava_lang_String__V__devirtual$(this$static, className){
  $clinit_ElementRemote();
  return hasJavaObjectVirtualDispatch(this$static)?this$static.setClassName(className):(this$static.className = className || '' , undefined);
}

function setPropertyString_Ljava_lang_String_Ljava_lang_String__V__devirtual$(this$static, name_0, value_0){
  $clinit_ElementRemote();
  return hasJavaObjectVirtualDispatch(this$static)?this$static.setPropertyString(name_0, value_0):(this$static[name_0] = value_0 , undefined);
}

function sinkEvents_I_V__devirtual$(this$static, eventBits){
  $clinit_ElementRemote();
  return hasJavaObjectVirtualDispatch(this$static)?this$static.sinkEvents(eventBits):$sinkEvents_0();
}

var cache_0;
function ElementRemote$0methodref$parseBoolean$Type(){
}

defineClass(209, 1, {}, ElementRemote$0methodref$parseBoolean$Type);
_.apply_0 = function apply_12(arg0){
  return $clinit_Boolean() , $clinit_Boolean() , $equalsIgnoreCase('true', castToString(arg0))?true:false;
}
;
var Lcom_google_gwt_dom_client_ElementRemote$0methodref$parseBoolean$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 209, Ljava_lang_Object_2_classLit);
function ElementRemote$1methodref$parseInt$Type(){
}

defineClass(210, 1, {}, ElementRemote$1methodref$parseInt$Type);
_.apply_0 = function apply_13(arg0){
  return valueOf(__parseAndValidateInt(castToString(arg0), 10));
}
;
var Lcom_google_gwt_dom_client_ElementRemote$1methodref$parseInt$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 210, Ljava_lang_Object_2_classLit);
function $getString(this$static){
  var fb;
  fb = new FormatBuilder;
  $format(fb, ($clinit_CommonUtils() , 'Element remote:\n==========='), stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
  $append_5(fb.sb, '\n');
  fb.indented = false;
  $newLine($format(fb, 'Indicies (lowest first):\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this$static.indicies.join(',')])));
  $newLine($format(fb, 'Ancestors (lowest first):\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this$static.ancestors])));
  $newLine($format(fb, 'Root:\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$getTagNameRemote(this$static.root)])));
  $newLine($format(fb, 'Debug data:\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this$static.debugData.join('\n\n')])));
  $newLine($format(fb, '\nDebug log:\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this$static.debugLog])));
  return fb.sb.string;
}

function $hasRemoteDefined(this$static){
  var value_0, value$iterator;
  for (value$iterator = commaSeparatedBoolsToList(this$static.remoteDefined.join(',')).iterator(); value$iterator.hasNext_0();) {
    value_0 = castToBoolean(value$iterator.next_1());
    if ($clinit_InternalPreconditions() , checkCriticalNotNull(value_0) , value_0) {
      return true;
    }
  }
  return false;
}

function ElementRemote$RemoteCache(){
}

defineClass(208, 1, {}, ElementRemote$RemoteCache);
_.lastIsResult = false;
var Lcom_google_gwt_dom_client_ElementRemote$RemoteCache_2_classLit = createForClass('com.google.gwt.dom.client', 'ElementRemote/RemoteCache', 208, Ljava_lang_Object_2_classLit);
function $clinit_EntityDecoder(){
  $clinit_EntityDecoder = emptyMethod;
  var a, c, i;
  HTML_ALPHA = stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_String_2_classLit, 1), $intern_0, 2, 6, ['apos', 'nbsp', 'iexcl', 'cent', 'pound', 'curren', 'yen', 'brvbar', 'sect', 'uml', 'copy', 'ordf', 'laquo', 'not', 'shy', 'reg', 'macr', 'deg', 'plusmn', 'sup2', 'sup3', 'acute', 'micro', 'para', 'middot', 'cedil', 'sup1', 'ordm', 'raquo', 'frac14', 'frac12', 'frac34', 'iquest', 'Agrave', 'Aacute', 'Acirc', 'Atilde', 'Auml', 'Aring', 'Aelig', 'Ccedil', 'Egrave', 'Eacute', 'Ecirc', 'Euml', 'Igrave', 'Iacute', 'Icirc', 'Iuml', 'ETH', 'Ntilde', 'Ograve', 'Oacute', 'Ocirc', 'Otilde', 'Ouml', 'times', 'Oslash', 'Ugrave', 'Uacute', 'Ucirc', 'Uuml', 'Yacute', 'THORN', 'szlig', 'agrave', 'aacute', 'acirc', 'atilde', 'auml', 'aring', 'aelig', 'ccedil', 'egrave', 'eacute', 'ecirc', 'euml', 'igrave', 'iacute', 'icirc', 'iuml', 'eth', 'ntilde', 'ograve', 'oacute', 'ocirc', 'otilde', 'ouml', 'divide', 'oslash', 'ugrave', 'uacute', 'ucirc', 'uuml', 'yacute', 'thorn', 'yuml', 'quot', 'amp', 'lt', 'gt', 'OElig', 'oelig', 'Scaron', 'scaron', 'Yuml', 'circ', 'tilde', 'ensp', 'emsp', 'thinsp', 'zwnj', 'zwj', 'lrm', 'rlm', 'ndash', 'mdash', 'lsquo', 'rsquo', 'sbquo', 'ldquo', 'rdquo', 'bdquo', 'dagger', 'Dagger', 'permil', 'lsaquo', 'rsaquo', 'euro', 'fnof', 'Alpha', 'Beta', 'Gamma', 'Delta', 'Epsilon', 'Zeta', 'Eta', 'Theta', 'Iota', 'Kappa', 'Lambda', 'Mu', 'Nu', 'Xi', 'Omicron', 'Pi', 'Rho', 'Sigma', 'Tau', 'Upsilon', 'Phi', 'Chi', 'Psi', 'Omega', 'alpha', 'beta', 'gamma', 'delta', 'epsilon', 'zeta', 'eta', 'theta', 'iota', 'kappa', 'lambda', 'mu', 'nu', 'xi', 'omicron', 'pi', 'rho', 'sigmaf', 'sigma', 'tau', 'upsilon', 'phi', 'chi', 'psi', 'omega', 'thetasym', 'upsih', 'piv', 'bull', 'hellip', 'prime', 'Prime', 'oline', 'frasl', 'weierp', 'image', 'real', 'trade', 'alefsym', 'larr', 'uarr', 'rarr', 'darr', 'harr', 'crarr', 'lArr', 'uArr', 'rArr', 'dArr', 'hArr', 'forall', 'part', 'exist', 'empty', 'nabla', 'isin', 'notin', 'ni', 'prod', 'sum', 'minus', 'lowast', 'radic', 'prop', 'infin', 'ang', 'and', 'or', 'cap', 'cup', 'int', 'there4', 'sim', 'cong', 'asymp', 'ne', 'equiv', 'le', 'ge', 'sub', 'sup', 'nsub', 'sube', 'supe', 'oplus', 'otimes', 'perp', 'sdot', 'lceil', 'rceil', 'lfloor', 'rfloor', 'lang', 'rang', 'loz', 'spades', 'clubs', 'hearts', 'diams']);
  HTML_CODES = stampJavaTypeInfo(getClassLiteralForArray(I_classLit, 1), {107:1, 3:1}, 57, 15, [39, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 34, 38, 60, 62, 338, 339, 352, 353, 376, 710, 732, 8194, 8195, 8201, 8204, 8205, 8206, 8207, 8211, 8212, 8216, 8217, 8218, 8220, 8221, 8222, 8224, 8225, 8240, 8249, 8250, 8364, 402, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 931, 932, 933, 934, 935, 936, 937, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 977, 978, 982, 8226, 8230, 8242, 8243, 8254, 8260, 8472, 8465, 8476, 8482, 8501, 8592, 8593, 8594, 8595, 8596, 8629, 8656, 8657, 8658, 8659, 8660, 8704, 8706, 8707, 8709, 8711, 8712, 8713, 8715, 8719, 8721, 8722, 8727, 8730, 8733, 8734, 8736, 8743, 8744, 8745, 8746, 8747, 8756, 8764, 8773, 8776, 8800, 8801, 8804, 8805, 8834, 8835, 8836, 8838, 8839, 8853, 8855, 8869, 8901, 8968, 8969, 8970, 8971, 9001, 9002, 9674, 9824, 9827, 9829, 9830]);
  lookupMap = new LinkedHashMap;
  for (i = 0; i < HTML_ALPHA.length; i++) {
    a = HTML_ALPHA[i];
    c = HTML_CODES[i];
    lookupMap.put_0(a, String.fromCharCode(c & $intern_9));
  }
}

function decode(input_0){
  $clinit_EntityDecoder();
  var builder, chrs, entityValue, firstChar, i, j, k, len, radix, st, value_0;
  if (input_0.indexOf('&') == -1) {
    return input_0;
  }
  builder = null;
  len = input_0.length;
  i = 1;
  st = 0;
  while (true) {
    while (i < len && ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(i - 1, input_0.length) , input_0.charCodeAt(i - 1) != 38))
      ++i;
    if (i >= len)
      break;
    j = i;
    while (j < len && j < i + 6 + 1 && ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(j, input_0.length) , input_0.charCodeAt(j) != 59))
      ++j;
    if (j == len || j < i + 2 || j == i + 6 + 1) {
      ++i;
      continue;
    }
    $clinit_InternalPreconditions();
    checkCriticalStringElementIndex(i, input_0.length);
    if (input_0.charCodeAt(i) == 35) {
      k = i + 1;
      radix = 10;
      firstChar = (checkCriticalStringElementIndex(k, input_0.length) , input_0.charCodeAt(k));
      if (firstChar == 120 || firstChar == 88) {
        ++k;
        radix = 16;
      }
      try {
        entityValue = __parseAndValidateInt(input_0.substr(k, j - k), radix);
        !builder && (builder = new StringBuilder);
        $append_5(builder, input_0.substr(st, i - 1 - st));
        if (entityValue > $intern_9) {
          chrs = (checkCriticalArgument(entityValue >= 0 && entityValue <= 1114111) , entityValue >= $intern_10?stampJavaTypeInfo(getClassLiteralForArray(C_classLit, 1), $intern_11, 57, 15, [55296 + (entityValue - $intern_10 >> 10 & 1023) & $intern_9, 56320 + (entityValue - $intern_10 & 1023) & $intern_9]):stampJavaTypeInfo(getClassLiteralForArray(C_classLit, 1), $intern_11, 57, 15, [entityValue & $intern_9]));
          $append_1(builder, chrs[0]);
          $append_1(builder, chrs[1]);
        }
         else {
          $append_1(builder, entityValue & $intern_9);
        }
      }
       catch ($e0) {
        $e0 = toJava($e0);
        if (instanceOf($e0, 40)) {
          ++i;
          continue;
        }
         else 
          throw toJs($e0);
      }
    }
     else {
      value_0 = castTo(lookupMap.get_0(input_0.substr(i, j - i)), 84);
      if (value_0 == null) {
        ++i;
        continue;
      }
      !builder && (builder = new StringBuilder);
      $append_5(builder, input_0.substr(st, i - 1 - st));
      builder.string += '' + value_0;
    }
    st = j + 1;
    i = st;
  }
  if (builder) {
    $append_5(builder, input_0.substr(st, len - st));
    return builder.string;
  }
  return input_0;
}

var HTML_ALPHA, HTML_CODES, lookupMap;
function $cast(this$static){
  if (is_1(this$static.nativeTarget)) {
    return $clinit_LocalDom() , nodeFor_0(this$static.nativeTarget);
  }
  throw toJs(new FixmeUnsupportedOperationException);
}

function $is(this$static, clazz){
  if (clazz == Lcom_google_gwt_dom_client_Element_2_classLit && is_1(this$static.nativeTarget)) {
    return true;
  }
  return false;
}

function EventTarget_0(nativeTarget){
  this.nativeTarget = nativeTarget;
}

defineClass(406, 1, {12:1}, EventTarget_0);
_.toString_0 = function toString_20(){
  var number;
  return $getName(getClass__Ljava_lang_Class___devirtual$(this)) + '@' + (number = hashCode__I__devirtual$(this) >>> 0 , number.toString(16)) + ':' + this.nativeTarget;
}
;
var Lcom_google_gwt_dom_client_EventTarget_2_classLit = createForClass('com.google.gwt.dom.client', 'EventTarget', 406, Ljava_lang_Object_2_classLit);
function FieldSetElement(){
}

defineClass(182, 5, $intern_7, FieldSetElement);
var Lcom_google_gwt_dom_client_FieldSetElement_2_classLit = createForClass('com.google.gwt.dom.client', 'FieldSetElement', 182, Lcom_google_gwt_dom_client_Element_2_classLit);
function FixmeUnsupportedOperationException(){
  UnsupportedOperationException.call(this);
}

defineClass(408, 13, $intern_1, FixmeUnsupportedOperationException);
var Lcom_google_gwt_dom_client_FixmeUnsupportedOperationException_2_classLit = createForClass('com.google.gwt.dom.client', 'FixmeUnsupportedOperationException', 408, Ljava_lang_UnsupportedOperationException_2_classLit);
function FormElement(){
}

defineClass(175, 5, $intern_7, FormElement);
var Lcom_google_gwt_dom_client_FormElement_2_classLit = createForClass('com.google.gwt.dom.client', 'FormElement', 175, Lcom_google_gwt_dom_client_Element_2_classLit);
function FrameElement(){
}

defineClass(166, 5, $intern_7, FrameElement);
var Lcom_google_gwt_dom_client_FrameElement_2_classLit = createForClass('com.google.gwt.dom.client', 'FrameElement', 166, Lcom_google_gwt_dom_client_Element_2_classLit);
function FrameSetElement(){
}

defineClass(183, 5, $intern_7, FrameSetElement);
var Lcom_google_gwt_dom_client_FrameSetElement_2_classLit = createForClass('com.google.gwt.dom.client', 'FrameSetElement', 183, Lcom_google_gwt_dom_client_Element_2_classLit);
function HRElement(){
}

defineClass(174, 5, $intern_7, HRElement);
var Lcom_google_gwt_dom_client_HRElement_2_classLit = createForClass('com.google.gwt.dom.client', 'HRElement', 174, Lcom_google_gwt_dom_client_Element_2_classLit);
function HeadElement(){
}

defineClass(98, 5, $intern_7, HeadElement);
var Lcom_google_gwt_dom_client_HeadElement_2_classLit = createForClass('com.google.gwt.dom.client', 'HeadElement', 98, Lcom_google_gwt_dom_client_Element_2_classLit);
function HeadingElement(){
}

defineClass(38, 5, $intern_7, HeadingElement);
var Lcom_google_gwt_dom_client_HeadingElement_2_classLit = createForClass('com.google.gwt.dom.client', 'HeadingElement', 38, Lcom_google_gwt_dom_client_Element_2_classLit);
function $emitCData(this$static, string){
  this$static.tag = null;
  $emitText(this$static, string);
}

function $emitComment(this$static, string){
  this$static.tag = null;
  $emitText(this$static, string);
}

function $emitElement(this$static){
  var close_0, close2, closeTag, idx2, idx3, textContent;
  closeTag = false;
  if (this$static.tag == null) {
    this$static.tag = this$static.builder.string;
    if ($startsWith(this$static.tag, '/')) {
      this$static.tag = this$static.tag.substr(1);
      closeTag = true;
    }
  }
  closeTag || $emitStartElement(this$static, this$static.tag);
  this$static.selfCloseTag = this$static.selfCloseTag | isSelfClosingTag(this$static.tag);
  if (closeTag && this$static.selfCloseTag)
  ;
  else {
    (closeTag || this$static.selfCloseTag) && $emitEndElement(this$static, this$static.tag);
  }
  switch (this$static.tag) {
    case 'script':
    case 'style':
      checkState(!closeTag);
      close_0 = format_0('<\/%s>', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this$static.tag]));
      close2 = format_0('<\/%s>', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this$static.tag.toUpperCase()]));
      idx2 = $indexOf_0(this$static.html, close_0, this$static.idx);
      idx3 = $indexOf_0(this$static.html, close2, this$static.idx);
      (idx2 == -1 || idx3 != -1 && idx3 < idx2) && (idx2 = idx3);
      textContent = $substring_0(this$static.html, this$static.idx, idx2);
      $emitText(this$static, textContent);
      $emitEndElement(this$static, this$static.tag);
      this$static.idx = idx2 + close_0.length;
  }
  this$static.tokenState = ($clinit_HtmlParser$TokenState() , EXPECTING_NODE);
  this$static.tag = null;
  this$static.selfCloseTag = false;
}

function $emitEndElement(this$static, tag){
  if (!this$static.emitHtmlHeadBodyTags) {
    switch (tag) {
      case 'html':
      case 'head':
      case 'body':
        return;
    }
  }
  $setCursor(this$static, this$static.cursor.getParentElement(), -1);
}

function $emitStartElement(this$static, tag){
  var element, tbody;
  if (!this$static.emitHtmlHeadBodyTags) {
    switch (tag) {
      case 'html':
      case 'head':
      case 'body':
        return;
    }
  }
  element = null;
  if (!this$static.rootResult && !!this$static.replaceContents) {
    element = this$static.replaceContents;
  }
   else {
    element = $createElement_0(get_7(), tag);
    element.local.attributes_0 = this$static.attributes_0;
    notBlank(castToString($get_1(this$static.attributes_0, 'style'))) && (element.local.hasUnparsedStyle = true);
  }
  this$static.attributes_0 = new LightMap;
  if (!this$static.rootResult) {
    this$static.rootResult = element;
    $setCursor(this$static, element, 1);
  }
   else {
    if ($equals_0(tag, 'tr') && $equals_0(this$static.cursor.local.tagName_0, 'table')) {
      tbody = $createElement_0(get_7(), 'tbody');
      this$static.cursor.appendChild_0(tbody);
      this$static.syntheticElements.add_0(tbody);
      $setCursor(this$static, tbody, 1);
    }
    this$static.cursor.appendChild_0(element);
    $setCursor(this$static, element, 1);
  }
}

function $emitText(this$static, string){
  var text_0;
  if (string.length == 0) {
    return;
  }
  text_0 = $createTextNode(get_7(), string);
  this$static.cursor.appendChild_0(text_0);
}

function $parse_0(this$static, html, replaceContents, emitHtmlHeadBodyTags){
  var preSet;
  preSet = ($clinit_LocalDom() , $clinit_LocalDom() , disableRemoteWrite);
  try {
    disableRemoteWrite = true;
    return $parse0(this$static, html, replaceContents, emitHtmlHeadBodyTags);
  }
   finally {
    disableRemoteWrite = preSet;
  }
}

function $parse0(this$static, html, replaceContents, emitHtmlHeadBodyTags){
  var c, hasSyntheticContainer, isWhiteSpace, length_0, tagLookahead;
  html.indexOf('\uFEFF') != -1 && (html = $replace_0(html, '\uFEFF', ''));
  html.indexOf('/>') != -1 && (html = expandEmptyElements(html));
  this$static.html = html;
  this$static.replaceContents = replaceContents;
  this$static.lineNumber = 1;
  this$static.emitHtmlHeadBodyTags = emitHtmlHeadBodyTags;
  $setLength(this$static.builder, 0);
  this$static.tokenState = ($clinit_HtmlParser$TokenState() , EXPECTING_NODE);
  !!replaceContents && (replaceContents.resolvedEventId = 0);
  length_0 = html.length;
  hasSyntheticContainer = !emitHtmlHeadBodyTags && ($equals_0(html.substr(0, 6), '<html>') || $equals_0(html.substr(0, 6), '<HTML>'));
  hasSyntheticContainer && (html = format_0('<div>%s<\/div>', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [html])));
  c = $charAt(html, this$static.idx);
  while (this$static.idx < length_0) {
    c = $charAt(html, this$static.idx++);
    isWhiteSpace = false;
    switch (c) {
      case 32:
      case 9:
      case 10:
      case 13:
        isWhiteSpace = true;
    }
    switch (c) {
      case 10:
      case 13:
        ++this$static.lineNumber;
    }
    switch (this$static.tokenState.ordinal) {
      case 0:
        if (c == 60) {
          $setLength(this$static.builder, 0);
          this$static.tokenState = EXPECTING_TAG;
        }
         else {
          this$static.tokenState = TEXT;
          $append_1(this$static.builder, c);
        }

        break;
      case 2:
        if (c == 60) {
          $emitText(this$static, decode(this$static.builder.string));
          $setLength(this$static.builder, 0);
          this$static.tokenState = EXPECTING_TAG;
        }
         else {
          $append_1(this$static.builder, c);
        }

        break;
      case 1:
        this$static.selfCloseTag = false;
        tagLookahead = this$static.builder.string;
        if ($equals_0(tagLookahead, '!--')) {
          this$static.tag = tagLookahead;
          $setLength(this$static.builder, 0);
          $append_1(this$static.builder, c);
          this$static.tokenState = EXPECTING_COMMENT;
        }
         else if ($equals_0(tagLookahead, '![CDATA[')) {
          this$static.tag = tagLookahead;
          $setLength(this$static.builder, 0);
          $append_1(this$static.builder, c);
          this$static.tokenState = EXPECTING_CDATA;
        }
         else {
          if (isWhiteSpace) {
            this$static.tag = tagLookahead;
            $setLength(this$static.builder, 0);
            this$static.tokenState = EXPECTING_ATTRIBUTES;
          }
           else {
            switch (c) {
              case 47:
                this$static.builder.string.length > 0?(this$static.selfCloseTag = true):$append_1(this$static.builder, c);
                break;
              case 62:
                $emitElement(this$static);
                $setLength(this$static.builder, 0);
                break;
              default:$append_1(this$static.builder, c);
            }
          }
        }

        break;
      case 3:
        if (c == 62 && $endsWith(this$static.builder.string, '--')) {
          $setLength(this$static.builder, this$static.builder.string.length - 2);
          $emitComment(this$static, this$static.builder.string);
          $setLength(this$static.builder, 0);
          this$static.tokenState = EXPECTING_NODE;
        }
         else {
          $append_1(this$static.builder, c);
        }

        break;
      case 8:
        if (c == 62 && $endsWith(this$static.builder.string, ']]')) {
          $setLength(this$static.builder, this$static.builder.string.length - 2);
          $emitCData(this$static, this$static.builder.string);
          $setLength(this$static.builder, 0);
          this$static.tokenState = EXPECTING_NODE;
        }
         else {
          $append_1(this$static.builder, c);
        }

        break;
      case 4:
        if (isWhiteSpace) {
          continue;
        }

        switch (c) {
          case 47:
            this$static.selfCloseTag = true;
            break;
          case 62:
            $emitElement(this$static);
            $setLength(this$static.builder, 0);
            break;
          default:$append_1(this$static.builder, c);
            this$static.tokenState = EXPECTING_ATTR_SEP;
        }

        break;
      case 5:
        switch (c) {
          case 61:
            this$static.attrName = this$static.builder.string;
            this$static.attrValue = '';
            $setLength(this$static.builder, 0);
            this$static.tokenState = EXPECTING_ATTR_VALUE_DELIM;
            break;
          default:$append_1(this$static.builder, c);
        }

        break;
      case 6:
        switch (c) {
          case 34:
          case 39:
            this$static.attrDelim = c;
            this$static.tokenState = EXPECTING_ATTR_VALUE;
            break;
          case 32:
            this$static.attrValue = '';
            $put_1(this$static.attributes_0, this$static.attrName, decode(this$static.attrValue));
            $setLength(this$static.builder, 0);
            break;
          default:this$static.attrDelim = 32;
            $append_1(this$static.builder, c);
            this$static.tokenState = EXPECTING_ATTR_VALUE;
        }

        break;
      case 7:
        if (this$static.attrDelim == 32 && c == 62) {
          this$static.attrValue = this$static.builder.string;
          $put_1(this$static.attributes_0, this$static.attrName, decode(this$static.attrValue));
          $emitElement(this$static);
          $setLength(this$static.builder, 0);
          break;
        }

        if (c == this$static.attrDelim) {
          this$static.attrValue = this$static.builder.string;
          $put_1(this$static.attributes_0, this$static.attrName, decode(this$static.attrValue));
          $setLength(this$static.builder, 0);
          this$static.tokenState = EXPECTING_ATTRIBUTES;
          break;
        }

        $append_1(this$static.builder, c);
    }
  }
  return this$static.rootResult;
}

function $setCursor(this$static, element, delta){
  if (this$static.syntheticElements.contains(this$static.cursor) && delta == -1) {
    this$static.syntheticElements.remove_1(this$static.cursor);
    this$static.cursor = this$static.cursor.getParentElement();
    element = element.getParentElement();
    this$static.debugCursorDepth += delta;
  }
  this$static.cursor = element;
}

function HtmlParser(){
  this.builder = new StringBuilder;
  this.attributes_0 = new LightMap;
  this.syntheticElements = new ArrayList;
}

function isSelfClosingTag(tag){
  switch (tag.toLowerCase()) {
    case 'area':
    case 'base':
    case 'br':
    case 'col':
    case 'command':
    case 'embed':
    case 'hr':
    case 'img':
    case 'input':
    case 'keygen':
    case 'link':
    case 'meta':
    case 'param':
    case 'source':
    case 'track':
    case 'wbr':
      return true;
  }
  return false;
}

defineClass(140, 1, {}, HtmlParser);
_.attrDelim = 0;
_.debugCursorDepth = 0;
_.emitHtmlHeadBodyTags = false;
_.idx = 0;
_.lineNumber = 0;
_.selfCloseTag = false;
var Lcom_google_gwt_dom_client_HtmlParser_2_classLit = createForClass('com.google.gwt.dom.client', 'HtmlParser', 140, Ljava_lang_Object_2_classLit);
function $clinit_HtmlParser$TokenState(){
  $clinit_HtmlParser$TokenState = emptyMethod;
  EXPECTING_NODE = new HtmlParser$TokenState('EXPECTING_NODE', 0);
  EXPECTING_TAG = new HtmlParser$TokenState('EXPECTING_TAG', 1);
  TEXT = new HtmlParser$TokenState('TEXT', 2);
  EXPECTING_COMMENT = new HtmlParser$TokenState('EXPECTING_COMMENT', 3);
  EXPECTING_ATTRIBUTES = new HtmlParser$TokenState('EXPECTING_ATTRIBUTES', 4);
  EXPECTING_ATTR_SEP = new HtmlParser$TokenState('EXPECTING_ATTR_SEP', 5);
  EXPECTING_ATTR_VALUE_DELIM = new HtmlParser$TokenState('EXPECTING_ATTR_VALUE_DELIM', 6);
  EXPECTING_ATTR_VALUE = new HtmlParser$TokenState('EXPECTING_ATTR_VALUE', 7);
  EXPECTING_CDATA = new HtmlParser$TokenState('EXPECTING_CDATA', 8);
}

function HtmlParser$TokenState(enum$name, enum$ordinal){
  Enum.call(this, enum$name, enum$ordinal);
}

function values_3(){
  $clinit_HtmlParser$TokenState();
  return stampJavaTypeInfo(getClassLiteralForArray(Lcom_google_gwt_dom_client_HtmlParser$TokenState_2_classLit, 1), $intern_0, 36, 0, [EXPECTING_NODE, EXPECTING_TAG, TEXT, EXPECTING_COMMENT, EXPECTING_ATTRIBUTES, EXPECTING_ATTR_SEP, EXPECTING_ATTR_VALUE_DELIM, EXPECTING_ATTR_VALUE, EXPECTING_CDATA]);
}

defineClass(36, 33, {36:1, 3:1, 32:1, 33:1}, HtmlParser$TokenState);
var EXPECTING_ATTRIBUTES, EXPECTING_ATTR_SEP, EXPECTING_ATTR_VALUE, EXPECTING_ATTR_VALUE_DELIM, EXPECTING_CDATA, EXPECTING_COMMENT, EXPECTING_NODE, EXPECTING_TAG, TEXT;
var Lcom_google_gwt_dom_client_HtmlParser$TokenState_2_classLit = createForEnum('com.google.gwt.dom.client', 'HtmlParser/TokenState', 36, Ljava_lang_Enum_2_classLit, values_3);
function IFrameElement(){
}

defineClass(167, 166, $intern_7, IFrameElement);
var Lcom_google_gwt_dom_client_IFrameElement_2_classLit = createForClass('com.google.gwt.dom.client', 'IFrameElement', 167, Lcom_google_gwt_dom_client_FrameElement_2_classLit);
defineClass(462, 5, $intern_7);
var Lcom_google_gwt_user_client_ui_AbstractImagePrototype$ImagePrototypeElement_2_classLit = createForClass('com.google.gwt.user.client.ui', 'AbstractImagePrototype/ImagePrototypeElement', 462, Lcom_google_gwt_dom_client_Element_2_classLit);
function ImageElement(){
}

defineClass(161, 462, $intern_7, ImageElement);
var Lcom_google_gwt_dom_client_ImageElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ImageElement', 161, Lcom_google_gwt_user_client_ui_AbstractImagePrototype$ImagePrototypeElement_2_classLit);
function InputElement(){
}

defineClass(158, 5, $intern_7, InputElement);
var Lcom_google_gwt_dom_client_InputElement_2_classLit = createForClass('com.google.gwt.dom.client', 'InputElement', 158, Lcom_google_gwt_dom_client_Element_2_classLit);
function LIElement(){
}

defineClass(170, 5, $intern_7, LIElement);
var Lcom_google_gwt_dom_client_LIElement_2_classLit = createForClass('com.google.gwt.dom.client', 'LIElement', 170, Lcom_google_gwt_dom_client_Element_2_classLit);
function LabelElement(){
}

defineClass(162, 5, $intern_7, LabelElement);
_.setPropertyString = function setPropertyString_2(name_0, value_0){
  $ensureRemoteCheck(this);
  $equals_0('htmlFor', name_0)?$setAttribute(this.local, 'for', value_0):$setPropertyString(this.local, name_0, value_0);
  setPropertyString_Ljava_lang_String_Ljava_lang_String__V__devirtual$($remote(this), name_0, value_0);
}
;
var Lcom_google_gwt_dom_client_LabelElement_2_classLit = createForClass('com.google.gwt.dom.client', 'LabelElement', 162, Lcom_google_gwt_dom_client_Element_2_classLit);
function LegendElement(){
}

defineClass(190, 5, $intern_7, LegendElement);
var Lcom_google_gwt_dom_client_LegendElement_2_classLit = createForClass('com.google.gwt.dom.client', 'LegendElement', 190, Lcom_google_gwt_dom_client_Element_2_classLit);
function LinkElement(){
}

defineClass(186, 5, $intern_7, LinkElement);
var Lcom_google_gwt_dom_client_LinkElement_2_classLit = createForClass('com.google.gwt.dom.client', 'LinkElement', 186, Lcom_google_gwt_dom_client_Element_2_classLit);
function $clinit_LocalDom(){
  $clinit_LocalDom = emptyMethod;
  instance = new LocalDom;
  useRemoteDom = ($clinit_GWT() , true);
  disableRemoteWrite = false;
  TOPIC_EXCEPTION = ($ensureNamesAreInitialized(Lcom_google_gwt_dom_client_LocalDom_2_classLit) , Lcom_google_gwt_dom_client_LocalDom_2_classLit.typeName + '.' + 'TOPIC_EXCEPTION');
  TOPIC_UNABLE_TO_PARSE = ($ensureNamesAreInitialized(Lcom_google_gwt_dom_client_LocalDom_2_classLit) , Lcom_google_gwt_dom_client_LocalDom_2_classLit.typeName + '.TOPIC_UNABLE_TO_PARSE');
}

function $createElement0(this$static, tagName){
  var creator;
  creator = castTo(this$static.elementCreators.get_0(tagName.toLowerCase()), 6);
  return !creator?new Element_0:castTo(creator.get_1(), 5);
}

function $ensureFlush(this$static){
  if (!this$static.resolveCommand) {
    this$static.resolveCommand = new LocalDom$lambda$1$Type;
    (!INSTANCE_0 && (INSTANCE_0 = ($clinit_SchedulerImpl() , INSTANCE_1)) , INSTANCE_0).scheduleFinally(this$static.resolveCommand);
  }
}

function $ensurePendingResolved(this$static, node){
  var element, local, remote;
  checkState(node.linkedToRemote());
  element = castTo(node, 5);
  if (element.pendingResolution) {
    remote = castToJso(node.remote_0());
    local = castToAllowJso(node.local_0(), 211);
    $localToRemote(this$static, element, remote, local);
  }
}

function $ensureRemote0(this$static, node){
  var ancestors, cursor, idx, needsRemote, needsRemote$iterator, remote, root, withRemote;
  $resolve0(this$static, true);
  ancestors = new ArrayList;
  cursor = node;
  withRemote = null;
  while (cursor) {
    if (cursor.linkedToRemote()) {
      withRemote = cursor;
      break;
    }
     else {
      ancestors.add_0(cursor);
      cursor = cursor.getParentElement();
    }
  }
  reverse(ancestors);
  if (!withRemote) {
    root = castTo(ancestors.get_2(0), 7);
    $ensureRemoteNodeMaybePendingResolution0(instance, root);
    $ensureRemote0(this$static, node);
    return;
  }
  for (needsRemote$iterator = ancestors.iterator(); needsRemote$iterator.hasNext_0();) {
    needsRemote = castTo(needsRemote$iterator.next_1(), 7);
    idx = $indexInParentChildren(needsRemote.local_0());
    instanceOf(needsRemote, 5) && instanceOf(withRemote, 5) && $debugPutRemote(this$static.debugImpl, castTo(needsRemote, 5), idx, castTo(withRemote, 5));
    remote = withRemote.typedRemote().childNodes[idx];
    checkState(!this$static.remoteLookup.containsKey_0(remote));
    this$static.remoteLookup.put_0(remote, needsRemote);
    needsRemote.putRemote(remote, true);
    withRemote = needsRemote;
  }
}

function $ensureRemoteNodeMaybePendingResolution0(this$static, node){
  var element, nodeType, remote;
  if (node.linkedToRemote()) {
    return castToJso(node.remote_0());
  }
  $ensureFlush(this$static);
  remote = null;
  nodeType = node.getNodeType();
  switch (nodeType) {
    case 1:
      element = castTo(node, 5);
      remote = $createElement_2(castTo(($clinit_DOMImpl() , impl_0).remote, 51), element.local.tagName_0);
      element.pendingResolution = true;
      this$static.pendingResolution.add_0(node);
      log_1(($clinit_LocalDomDebug() , CREATED_PENDING_RESOLUTION), 'created pending resolution node:' + element.local.tagName_0, stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
      break;
    case 3:
      remote = $createTextNode0(get_7().remote, castTo(node, 123).local.text_0);
      break;
    default:throw toJs(new UnsupportedOperationException);
  }
  checkState(!this$static.remoteLookup.containsKey_0(remote));
  this$static.remoteLookup.put_0(remote, node);
  node.putRemote(remote, false);
  return remote;
}

function $eventMod0(this$static, evt, eventName){
  log_1(($clinit_LocalDomDebug() , EVENT_MOD), format_0('eventMod - %s %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [evt, eventName])), stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
  if (!this$static.eventMods.keySet().contains(evt)) {
    this$static.eventMods.clear_0();
    this$static.eventMods.put_0(evt, new ArrayList);
  }
  castTo(this$static.eventMods.get_0(evt), 15).add_0(eventName);
}

function $initElementCreators(this$static){
  this$static.elementCreators.put_0('div', new LocalDom$lambda$2$Type);
  this$static.elementCreators.put_0('span', new LocalDom$lambda$3$Type);
  this$static.elementCreators.put_0('body', new LocalDom$lambda$4$Type);
  this$static.elementCreators.put_0('button', new LocalDom$lambda$5$Type);
  this$static.elementCreators.put_0('style', new LocalDom$lambda$6$Type);
  this$static.elementCreators.put_0('table', new LocalDom$lambda$7$Type);
  this$static.elementCreators.put_0('head', new LocalDom$lambda$8$Type);
  this$static.elementCreators.put_0('tbody', new LocalDom$lambda$9$Type);
  this$static.elementCreators.put_0('tfoot', new LocalDom$lambda$10$Type);
  this$static.elementCreators.put_0('thead', new LocalDom$lambda$11$Type);
  this$static.elementCreators.put_0('caption', new LocalDom$lambda$12$Type);
  this$static.elementCreators.put_0('td', new LocalDom$lambda$13$Type);
  this$static.elementCreators.put_0('th', new LocalDom$lambda$14$Type);
  this$static.elementCreators.put_0('col', new LocalDom$lambda$15$Type);
  this$static.elementCreators.put_0('colgroup', new LocalDom$lambda$16$Type);
  this$static.elementCreators.put_0('tr', new LocalDom$lambda$17$Type);
  this$static.elementCreators.put_0('input', new LocalDom$lambda$18$Type);
  this$static.elementCreators.put_0('textarea', new LocalDom$lambda$19$Type);
  this$static.elementCreators.put_0('h1', new LocalDom$lambda$20$Type);
  this$static.elementCreators.put_0('h2', new LocalDom$lambda$21$Type);
  this$static.elementCreators.put_0('h3', new LocalDom$lambda$22$Type);
  this$static.elementCreators.put_0('h4', new LocalDom$lambda$23$Type);
  this$static.elementCreators.put_0('h5', new LocalDom$lambda$24$Type);
  this$static.elementCreators.put_0('h6', new LocalDom$lambda$25$Type);
  this$static.elementCreators.put_0('a', new LocalDom$lambda$26$Type);
  this$static.elementCreators.put_0('img', new LocalDom$lambda$27$Type);
  this$static.elementCreators.put_0('label', new LocalDom$lambda$28$Type);
  this$static.elementCreators.put_0('script', new LocalDom$lambda$29$Type);
  this$static.elementCreators.put_0('select', new LocalDom$lambda$30$Type);
  this$static.elementCreators.put_0('option', new LocalDom$lambda$31$Type);
  this$static.elementCreators.put_0('iframe', new LocalDom$lambda$32$Type);
  this$static.elementCreators.put_0('ul', new LocalDom$lambda$33$Type);
  this$static.elementCreators.put_0('ol', new LocalDom$lambda$34$Type);
  this$static.elementCreators.put_0('li', new LocalDom$lambda$35$Type);
  this$static.elementCreators.put_0('pre', new LocalDom$lambda$36$Type);
  this$static.elementCreators.put_0('p', new LocalDom$lambda$37$Type);
  this$static.elementCreators.put_0('br', new LocalDom$lambda$38$Type);
  this$static.elementCreators.put_0('hr', new LocalDom$lambda$39$Type);
  this$static.elementCreators.put_0('form', new LocalDom$lambda$40$Type);
  this$static.elementCreators.put_0('map', new LocalDom$lambda$41$Type);
  this$static.elementCreators.put_0('param', new LocalDom$lambda$42$Type);
  this$static.elementCreators.put_0('optgroup', new LocalDom$lambda$43$Type);
  this$static.elementCreators.put_0('blockquote', new LocalDom$lambda$44$Type);
  this$static.elementCreators.put_0('q', new LocalDom$lambda$45$Type);
  this$static.elementCreators.put_0('caption', new LocalDom$lambda$46$Type);
  this$static.elementCreators.put_0('dl', new LocalDom$lambda$47$Type);
  this$static.elementCreators.put_0('title', new LocalDom$lambda$48$Type);
  this$static.elementCreators.put_0('fieldset', new LocalDom$lambda$49$Type);
  this$static.elementCreators.put_0('frameset', new LocalDom$lambda$50$Type);
  this$static.elementCreators.put_0('meta', new LocalDom$lambda$51$Type);
  this$static.elementCreators.put_0('source', new LocalDom$lambda$52$Type);
  this$static.elementCreators.put_0('link', new LocalDom$lambda$53$Type);
  this$static.elementCreators.put_0('object', new LocalDom$lambda$54$Type);
  this$static.elementCreators.put_0('ins', new LocalDom$lambda$55$Type);
  this$static.elementCreators.put_0('del', new LocalDom$lambda$56$Type);
  this$static.elementCreators.put_0('base', new LocalDom$lambda$57$Type);
  this$static.elementCreators.put_0('frame', new LocalDom$lambda$58$Type);
  this$static.elementCreators.put_0('area', new LocalDom$lambda$59$Type);
  this$static.elementCreators.put_0('legend', new LocalDom$lambda$60$Type);
}

function $isPending0(this$static, nodeRemote){
  return this$static.pendingResolution.size_1() > 0 && this$static.pendingResolution.stream().anyMatch(new LocalDom$lambda$61$Type(nodeRemote));
}

function $isStopPropagation0(this$static, evt){
  var list;
  list = castTo(this$static.eventMods.get_0(evt), 15);
  return !!list && (list.contains('eventStopPropagation') || list.contains('eventCancelBubble'));
}

function $lambda$65(this$static, nl_0){
  $resolved(nl_0.node(), this$static.resolutionEventId);
}

function $linkRemote(this$static, remote, node){
  checkState(!this$static.remoteLookup.containsKey_0(remote));
  this$static.remoteLookup.put_0(remote, node);
}

function $localToRemote(this$static, element, remote, local){
  var bits, f_remote, innerHTML;
  innerHTML = ($clinit_ElementRemote() , hasJavaObjectVirtualDispatch(local)?local.getInnerHTML():($clinit_DOMImpl() , $clinit_LocalDom() , castTo($nodeFor0((null , instance), local, false), 5) , $getInnerHTML()));
  if (ie9) {
    switch (element.local.tagName_0) {
      case 'table':
        remote = $writeIe9Table(this$static, castTo(element, 97), remote);
        break;
      case 'tr':
        remote = $writeIe9Tr(this$static, castTo(element, 101), remote);
        break;
      default:$setInnerHTML(remote, innerHTML);
    }
  }
   else {
    $setInnerHTML(remote, innerHTML);
  }
  log_1(($clinit_LocalDomDebug() , RESOLVE), '%s - uiobj: %s - \n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [element.local.tagName_0, $orElse($map(ofNullable(element.uiObject), new LocalDom$lambda$62$Type), '(null)'), ($clinit_CommonUtils() , trimToWsChars(innerHTML, 1000, ''))]));
  f_remote = remote;
  (hasJavaObjectVirtualDispatch(local)?local.getAttributeMap():$getAttributeMap(local)).entrySet().forEach_0(new LocalDom$lambda$63$Type(f_remote));
  (hasJavaObjectVirtualDispatch(local)?local.getStyle():$getStyle_0()).local.properties.entrySet().forEach_0(new LocalDom$lambda$64$Type(f_remote));
  bits = $orSunkEventsOfAllChildren(castTo(local, 80), 0);
  bits |= ($clinit_DOM() , $getEventsSunk(impl_2, element));
  sinkEvents_2(element, bits);
  this$static.pendingResolution.remove_1(element);
  element.pendingResolution = false;
  $walk(element.local, new LocalDom$lambda$65$Type(this$static));
  this$static.resolutionEventIdDirty = true;
}

function $nodeFor0(this$static, remote, postReparse){
  var re;
  try {
    return $nodeFor1(this$static, remote, postReparse);
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 11)) {
      re = $e0;
      $publish(global_0(TOPIC_EXCEPTION), re);
      throw toJs(re);
    }
     else 
      throw toJs($e0);
  }
}

function $nodeFor1(this$static, remote, postReparse){
  var ancestors, child, childNode, childRemote, cursor, elem, hasNode, hasNode0, hasNodeRemote, hasNodeRemote0, idx, index_0, indicies, node, nodeIndex, parent_0, parentRemote, remoteIndex, remoteIndex0, root;
  if (!remote) {
    return null;
  }
  node = castTo(this$static.remoteLookup.get_0(remote), 7);
  if (node) {
    return node;
  }
  if (remote.nodeType == 3) {
    parentRemote = remote.parentNode;
    parent_0 = $nodeFor0(this$static, parentRemote, false);
    index_0 = $indexInParentChildren_0(remote);
    if (parent_0.getChildCount() == getLength__I__devirtual$(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(parentRemote).impl)) {
      childNode = parent_0.getChild(index_0);
      checkState(!this$static.remoteLookup.containsKey_0(remote));
      this$static.remoteLookup.put_0(remote, childNode);
      childNode.putRemote(remote, true);
      return childNode;
    }
     else {
      if (postReparse) {
        $publish(global_0(TOPIC_UNABLE_TO_PARSE), format_0('Text node reparse - remote:\n%s\n\nlocal:\n%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [($clinit_ElementRemote() , parentRemote.outerHTML), $getOuterHtml(castTo(parent_0, 5).local)])));
        throw toJs(new RuntimeException_0('Text node reparse'));
      }
      remoteIndex0 = $provideRemoteIndex(parentRemote, false);
      hasNodeRemote0 = remoteIndex0.hasNode;
      $reparseFromRemote(this$static, hasNodeRemote0, castTo(parent_0, 5), remoteIndex0);
      return $nodeFor0(this$static, remote, true);
    }
  }
  if (remote.nodeType != 1) {
    return null;
  }
  elem = remote;
  remoteIndex = $provideRemoteIndex(elem, false);
  hasNodeRemote = remoteIndex.hasNode;
  if (!hasNodeRemote) {
    root = remoteIndex.root;
    hasNode0 = $parseAndMarkResolved(this$static, root, ($clinit_ElementRemote() , root.outerHTML), null);
    checkState(!this$static.remoteLookup.containsKey_0(root));
    this$static.remoteLookup.put_0(root, hasNode0);
    $putRemote(hasNode0, root, true);
    hasNodeRemote = root;
  }
  hasNode = castTo(this$static.remoteLookup.get_0(hasNodeRemote), 5);
  if ($resolveRemoteDefined(hasNode)) {
    return $nodeFor0(this$static, remote, false);
  }
  if ($shouldTryReparseFromRemote(hasNode, remoteIndex) && !postReparse) {
    $reparseFromRemote(this$static, hasNodeRemote, hasNode, remoteIndex);
    return $nodeFor0(this$static, remote, true);
  }
  indicies = commaSeparatedIntsToList(remoteIndex.indicies.join(','));
  commaSeparatedBoolsToList(remoteIndex.remoteDefined.join(','));
  ancestors = remoteIndex.ancestors;
  $clinit_GWT();
  cursor = hasNode;
  for (idx = indicies.size_1() - 1; idx >= 0; idx--) {
    nodeIndex = castTo(indicies.get_2(idx), 25).value_0;
    $resolveRemoteDefined(cursor);
    child = castTo($getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(cursor), nodeIndex), 5);
    childRemote = ancestors[idx];
    checkState(!this$static.remoteLookup.containsKey_0(childRemote));
    this$static.remoteLookup.put_0(childRemote, child);
    $putRemote(child, childRemote, true);
    cursor = child;
  }
  return castTo(this$static.remoteLookup.get_0(remote), 7);
}

function $parseAndMarkResolved(this$static, root, outerHtml, replaceContents){
  var parsed, remote;
  parsed = null;
  try {
    parsed = $parse_0(new HtmlParser, outerHtml, replaceContents, root == get_7().remote.documentElement);
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 17)) {
      parsed = $parse_0(new HtmlParser, (remote = $generateFromOuterHtml(get_7().remote, outerHtml) , $buildOuterHtml(remote)), replaceContents, root == get_7().remote.documentElement);
    }
     else 
      throw toJs($e0);
  }
  parsed?($walk(parsed.local, new LocalDom$lambda$65$Type(this$static)) , this$static.resolutionEventIdDirty = true):$publish(global_0(TOPIC_UNABLE_TO_PARSE), outerHtml);
  return parsed;
}

function $reparseFromRemote(this$static, elem, hasNode, remoteIndex){
  var builtOuterHtml, cursor, e, idx, indicies, invalid, localIndex, message, node, nodeIndex, preface, remoteCursor, remoteNode, remoteOuterHtml, size_0, sizes;
  sizes = commaSeparatedIntsToList(remoteIndex.sizes.join(','));
  indicies = commaSeparatedIntsToList(remoteIndex.indicies.join(','));
  cursor = hasNode;
  remoteCursor = elem;
  for (idx = sizes.size_1() - 1; idx >= 0; idx--) {
    size_0 = castTo(sizes.get_2(idx), 25).value_0;
    invalid = cursor.getChildCount() != size_0;
    node = null;
    remoteNode = null;
    if (!invalid) {
      nodeIndex = castTo(indicies.get_2(idx), 25).value_0;
      node = $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(cursor), nodeIndex);
      remoteNode = remoteCursor.childNodes[nodeIndex];
      invalid = node.getNodeType() != remoteNode.nodeType;
    }
    if (invalid) {
      localIndex = $getChildIndexLocal(!cursor.getParentElement()?cursor:cursor.getParentElement(), cursor);
      $clearChildrenAndAttributes0(cursor.local);
      builtOuterHtml = $buildOuterHtml(remoteCursor);
      remoteOuterHtml = ($clinit_ElementRemote() , remoteCursor.outerHTML);
      $parseAndMarkResolved(this$static, remoteCursor, builtOuterHtml, cursor);
      invalid = cursor.getChildCount() != size_0;
      if (!invalid) {
        nodeIndex = castTo(indicies.get_2(idx), 25).value_0;
        node = $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(cursor), nodeIndex);
        remoteNode = remoteCursor.childNodes[nodeIndex];
        invalid = node.getNodeType() != remoteNode.nodeType;
      }
      if (invalid) {
        preface = format_0('sizes: %s\nsizeIdx:%s\nlocalIndex: %s\n(local) cursor.childCount: %s\nremoteSize:%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [sizes, valueOf(idx), valueOf(localIndex), valueOf(cursor.getChildCount()), valueOf(size_0)]));
        if (cursor.getChildCount() != size_0) {
          preface += 'size mismatch\n';
        }
         else {
          nodeIndex = castTo(indicies.get_2(idx), 25).value_0;
          node = $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(cursor), nodeIndex);
          remoteNode = remoteCursor.childNodes[nodeIndex];
          preface += '' + format_0('local node:%s\nremote node:%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [node, remoteNode]));
        }
        try {
          preface += '' + format_0('Remote index:\n%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$getString(remoteIndex)]));
        }
         catch ($e0) {
          $e0 = toJava($e0);
          if (instanceOf($e0, 17)) {
            e = $e0;
            preface += '' + format_0('Exception getting remoteIndex:\n%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$toString(e, e.getMessage())]));
          }
           else 
            throw toJs($e0);
        }
        preface += '' + format_0('Local dom tree:\n%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$provideLocalDomTree0(hasNode.local, new StringBuilder, 0).string]));
        preface += '' + format_0('(Local outer html):\n%s\n', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$getOuterHtml(hasNode.local)]));
        message = format_0('%s\n(Built outer html):\n%s\n\n(Remote outer html):\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [preface, builtOuterHtml, remoteOuterHtml]));
        $publish(global_0(TOPIC_UNABLE_TO_PARSE), message);
        $clinit_System();
        return;
      }
    }
    cursor = castTo(node, 5);
    remoteCursor = remoteNode;
  }
  $clinit_System();
}

function $resolve0(this$static, force){
  var re;
  if (this$static.resolving) {
    return;
  }
  if (!this$static.resolveCommand && !force) {
    return;
  }
  this$static.resolveCommand = null;
  log_1(($clinit_LocalDomDebug() , RESOLVE), '**resolve**', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
  try {
    this$static.resolving = true;
    if (this$static.resolutionEventIdDirty) {
      ++this$static.resolutionEventId;
      this$static.resolutionEventIdDirty = false;
    }
    (new ArrayList_0(this$static.pendingResolution)).stream().forEach_0(new LocalDom$0methodref$ensurePendingResolved$Type(this$static));
    this$static.resolutionEventIdDirty && ++this$static.resolutionEventId;
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 11)) {
      re = $e0;
      $publish(global_0(TOPIC_EXCEPTION), re);
      throw toJs(re);
    }
     else 
      throw toJs($e0);
  }
   finally {
    this$static.resolutionEventIdDirty = false;
    this$static.resolving = false;
  }
}

function $resolveExternal0(this$static, nodeRemote){
  var element, elementRemote, textNode;
  switch (nodeRemote.nodeType) {
    case 1:
      elementRemote = nodeRemote;
      element = $createElement_1(get_7().local, ($clinit_ElementRemote() , elementRemote.tagName));
      $putRemote(element, nodeRemote, false);
      $parseAndMarkResolved(instance, castToJso($remote(element)), $getOuterHtml_0(castToJso($remote(element))), element);
      checkState(!this$static.remoteLookup.containsKey_0(elementRemote));
      this$static.remoteLookup.put_0(elementRemote, element);
      return element;
    case 3:
      textNode = $createTextNode(get_7(), nodeRemote.nodeValue);
      checkState(textNode.resolvedEventId > 0);
      textNode.remote = castToAllowJso(nodeRemote, 417);
      return textNode;
    default:throw toJs(new UnsupportedOperationException);
  }
}

function $shouldTryReparseFromRemote(hasNode, remoteIndex){
  var cursor, idx, indicies, node, nodeIndex, size_0, sizes, sizesMatch;
  if ($hasRemoteDefined(remoteIndex)) {
    return false;
  }
  sizes = commaSeparatedIntsToList(remoteIndex.sizes.join(','));
  indicies = commaSeparatedIntsToList(remoteIndex.indicies.join(','));
  sizesMatch = true;
  cursor = hasNode;
  for (idx = sizes.size_1() - 1; idx >= 0; idx--) {
    size_0 = castTo(sizes.get_2(idx), 25).value_0;
    if (cursor.getChildCount() != size_0) {
      sizesMatch = false;
      break;
    }
    nodeIndex = castTo(indicies.get_2(idx), 25).value_0;
    node = $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(cursor), nodeIndex);
    if (node.getNodeType() != 1) {
      return true;
    }
    cursor = castTo(node, 5);
  }
  if (sizesMatch) {
    return false;
  }
  return true;
}

function $writeIe9Table(this$static, elem, remote){
  var outer;
  outer = $getOuterHtml(elem.local);
  this$static.remoteLookup.remove_0(remote);
  remote = $generateFromOuterHtml(get_7().remote, outer);
  $replaceRemote(elem, remote);
  return remote;
}

function $writeIe9Tr(this$static, elem, remote){
  var child, idx, node, node$iterator, remoteCell, remote_0;
  checkArgument(stream0(elem.getChildNodes()).allMatch(new LocalDom$lambda$66$Type));
  idx = 0;
  for (node$iterator = elem.getChildNodes().iterator(); node$iterator.hasNext_0();) {
    node = castTo(node$iterator.next_1(), 7);
    child = castTo(node, 5);
    remoteCell = (remote_0 = elem.typedRemote_0() , remote_0.insertCell(idx));
    $localToRemote(this$static, child, remoteCell, child.local);
    ++idx;
  }
  return remote;
}

function LocalDom(){
  this.debugImpl = new LocalDomDebugImpl;
  this.eventMods = new LinkedHashMap;
  this.pendingResolution = new ArrayList;
  $clinit_GWT();
  this.remoteLookup = new JsNativeMapWrapper(true);
  ie9 = (isIe9 == null && (isIe9 = ($clinit_Boolean() , ((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('MSIE') != -1 || (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Trident') != -1) && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1 && !((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('WebKit') != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1 && !((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Chrome/') != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1) && !($indexOf((userAgent == null && (userAgent = getUserAgent()) , userAgent), CHROME_IOS_USER_AGENT) != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1)) && !((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Chrome/') != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1) && ((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('MSIE9') != -1 || (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('MSIE 9.') != -1)?true:false)) , $booleanValue(isIe9));
  mutations = new LocalDomMutations;
  ((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('MSIE') != -1 || (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Trident') != -1) && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1 && !((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('WebKit') != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1 && !((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Chrome/') != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1) && !($indexOf((userAgent == null && (userAgent = getUserAgent()) , userAgent), CHROME_IOS_USER_AGENT) != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1)) && !((userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Chrome/') != -1 && (userAgent == null && (userAgent = getUserAgent()) , userAgent).indexOf('Opera') == -1);
  !collections && (initJs() , collections = new LocalDom$LocalDomCollections_Script , declarativeCssNames = collections.createIdentityEqualsMap(Ljava_lang_String_2_classLit) , this.elementCreators = collections.createIdentityEqualsMap(Ljava_lang_String_2_classLit) , $initElementCreators(this) , undefined);
}

function declarativeCssName(key){
  $clinit_LocalDom();
  return castToString(declarativeCssNames.computeIfAbsent(key, new LocalDom$lambda$0$Type));
}

function lambda$0_0(k_0){
  $clinit_LocalDom();
  var c, idx, lcKey, sb;
  lcKey = k_0.toLowerCase();
  if ($equals_0(lcKey, k_0)) {
    return k_0;
  }
   else {
    sb = new StringBuilder;
    for (idx = 0; idx < k_0.length; idx++) {
      c = ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(idx, k_0.length) , k_0.charCodeAt(idx));
      if (c >= 65 && c <= 90) {
        sb.string += '-';
        $append_5(sb, String.fromCharCode(c).toLowerCase());
      }
       else {
        sb.string += String.fromCharCode(c);
      }
    }
    return sb.string;
  }
}

function lambda$61(nodeRemote_0, n_1){
  $clinit_LocalDom();
  return n_1.remote_0() == nodeRemote_0;
}

function lambda$63(f_remote_0, e_1){
  $clinit_LocalDom();
  switch (castToString(e_1.getKey())) {
    case 'text':
      $setPropertyString_0(f_remote_0, castToString(e_1.getKey()), castToString(e_1.getValue()));
      break;
    default:$setAttribute_0(f_remote_0, castToString(e_1.getKey()), castToString(e_1.getValue()));
  }
}

function lambda$64(f_remote_0, e_1){
  $clinit_LocalDom();
  var remoteStyle;
  remoteStyle = ($clinit_ElementRemote() , f_remote_0.style);
  remoteStyle[castToString(e_1.getKey())] = castToString(e_1.getValue());
}

function log_1(channel, message, args){
  $clinit_LocalDom();
  $log(channel, message, args);
}

function nodeFor_0(remote){
  $clinit_LocalDom();
  return $nodeFor0(instance, remote, false);
}

function register(doc){
  $clinit_LocalDom();
  if (useRemoteDom) {
    $linkRemote(instance, doc.remote, doc);
    $nodeFor0(instance, doc.remote.documentElement, false);
    $startObservingIfNotInEventCycle(mutations);
  }
}

defineClass(223, 1, {}, LocalDom);
_.resolutionEventId = 1;
_.resolutionEventIdDirty = false;
_.resolveCommand = null;
_.resolving = false;
var TOPIC_EXCEPTION, TOPIC_UNABLE_TO_PARSE, collections, declarativeCssNames, disableRemoteWrite = false, ie9 = false, instance, mutations, useRemoteDom = false;
var Lcom_google_gwt_dom_client_LocalDom_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDom', 223, Ljava_lang_Object_2_classLit);
function LocalDom$0methodref$ensurePendingResolved$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(291, 1, $intern_5, LocalDom$0methodref$ensurePendingResolved$Type);
_.accept_0 = function accept_5(arg0){
  $ensurePendingResolved(this.$$outer_0, castTo(arg0, 7));
}
;
var Lcom_google_gwt_dom_client_LocalDom$0methodref$ensurePendingResolved$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 291, Ljava_lang_Object_2_classLit);
defineClass(435, 1, {});
_.createIdentityEqualsMap = function createIdentityEqualsMap(keyClass){
  return new LinkedHashMap;
}
;
var Lcom_google_gwt_dom_client_LocalDom$LocalDomCollections_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDom/LocalDomCollections', 435, Ljava_lang_Object_2_classLit);
function LocalDom$LocalDomCollections_Script(){
}

defineClass(224, 435, {}, LocalDom$LocalDomCollections_Script);
_.createIdentityEqualsMap = function createIdentityEqualsMap_0(keyClass){
  return new JsNativeMapWrapper(false);
}
;
var Lcom_google_gwt_dom_client_LocalDom$LocalDomCollections_1Script_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDom/LocalDomCollections_Script', 224, Lcom_google_gwt_dom_client_LocalDom$LocalDomCollections_2_classLit);
function LocalDom$lambda$0$Type(){
}

defineClass(225, 1, {}, LocalDom$lambda$0$Type);
_.apply_0 = function apply_14(arg0){
  return lambda$0_0(castToString(arg0));
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$0$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 225, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$1$Type(){
}

defineClass(226, 1, {}, LocalDom$lambda$1$Type);
_.execute = function execute(){
  $clinit_LocalDom();
  $resolve0(instance, false);
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$1$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 226, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$10$Type(){
}

defineClass(235, 1, $intern_12, LocalDom$lambda$10$Type);
_.get_1 = function get_8(){
  return $clinit_LocalDom() , new TableSectionElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$10$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 235, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$11$Type(){
}

defineClass(236, 1, $intern_12, LocalDom$lambda$11$Type);
_.get_1 = function get_9(){
  return $clinit_LocalDom() , new TableSectionElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$11$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 236, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$12$Type(){
}

defineClass(237, 1, $intern_12, LocalDom$lambda$12$Type);
_.get_1 = function get_10(){
  return $clinit_LocalDom() , new HeadElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$12$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 237, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$13$Type(){
}

defineClass(238, 1, $intern_12, LocalDom$lambda$13$Type);
_.get_1 = function get_11(){
  return $clinit_LocalDom() , new TableCellElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$13$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 238, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$14$Type(){
}

defineClass(239, 1, $intern_12, LocalDom$lambda$14$Type);
_.get_1 = function get_12(){
  return $clinit_LocalDom() , new TableCellElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$14$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 239, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$15$Type(){
}

defineClass(240, 1, $intern_12, LocalDom$lambda$15$Type);
_.get_1 = function get_13(){
  return $clinit_LocalDom() , new TableColElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$15$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 240, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$16$Type(){
}

defineClass(241, 1, $intern_12, LocalDom$lambda$16$Type);
_.get_1 = function get_14(){
  return $clinit_LocalDom() , new TableColElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$16$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 241, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$17$Type(){
}

defineClass(242, 1, $intern_12, LocalDom$lambda$17$Type);
_.get_1 = function get_15(){
  return $clinit_LocalDom() , new TableRowElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$17$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 242, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$18$Type(){
}

defineClass(243, 1, $intern_12, LocalDom$lambda$18$Type);
_.get_1 = function get_16(){
  return $clinit_LocalDom() , new InputElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$18$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 243, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$19$Type(){
}

defineClass(244, 1, $intern_12, LocalDom$lambda$19$Type);
_.get_1 = function get_17(){
  return $clinit_LocalDom() , new TextAreaElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$19$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 244, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$2$Type(){
}

defineClass(227, 1, $intern_12, LocalDom$lambda$2$Type);
_.get_1 = function get_18(){
  return $clinit_LocalDom() , new DivElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$2$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 227, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$20$Type(){
}

defineClass(245, 1, $intern_12, LocalDom$lambda$20$Type);
_.get_1 = function get_19(){
  return $clinit_LocalDom() , new HeadingElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$20$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 245, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$21$Type(){
}

defineClass(246, 1, $intern_12, LocalDom$lambda$21$Type);
_.get_1 = function get_20(){
  return $clinit_LocalDom() , new HeadingElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$21$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 246, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$22$Type(){
}

defineClass(247, 1, $intern_12, LocalDom$lambda$22$Type);
_.get_1 = function get_21(){
  return $clinit_LocalDom() , new HeadingElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$22$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 247, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$23$Type(){
}

defineClass(248, 1, $intern_12, LocalDom$lambda$23$Type);
_.get_1 = function get_22(){
  return $clinit_LocalDom() , new HeadingElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$23$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 248, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$24$Type(){
}

defineClass(249, 1, $intern_12, LocalDom$lambda$24$Type);
_.get_1 = function get_23(){
  return $clinit_LocalDom() , new HeadingElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$24$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 249, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$25$Type(){
}

defineClass(250, 1, $intern_12, LocalDom$lambda$25$Type);
_.get_1 = function get_24(){
  return $clinit_LocalDom() , new HeadingElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$25$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 250, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$26$Type(){
}

defineClass(251, 1, $intern_12, LocalDom$lambda$26$Type);
_.get_1 = function get_25(){
  return $clinit_LocalDom() , new AnchorElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$26$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 251, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$27$Type(){
}

defineClass(252, 1, $intern_12, LocalDom$lambda$27$Type);
_.get_1 = function get_26(){
  return $clinit_LocalDom() , new ImageElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$27$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 252, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$28$Type(){
}

defineClass(253, 1, $intern_12, LocalDom$lambda$28$Type);
_.get_1 = function get_27(){
  return $clinit_LocalDom() , new LabelElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$28$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 253, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$29$Type(){
}

defineClass(254, 1, $intern_12, LocalDom$lambda$29$Type);
_.get_1 = function get_28(){
  return $clinit_LocalDom() , new ScriptElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$29$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 254, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$3$Type(){
}

defineClass(228, 1, $intern_12, LocalDom$lambda$3$Type);
_.get_1 = function get_29(){
  return $clinit_LocalDom() , new SpanElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$3$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 228, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$30$Type(){
}

defineClass(255, 1, $intern_12, LocalDom$lambda$30$Type);
_.get_1 = function get_30(){
  return $clinit_LocalDom() , new SelectElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$30$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 255, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$31$Type(){
}

defineClass(256, 1, $intern_12, LocalDom$lambda$31$Type);
_.get_1 = function get_31(){
  return $clinit_LocalDom() , new OptionElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$31$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 256, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$32$Type(){
}

defineClass(257, 1, $intern_12, LocalDom$lambda$32$Type);
_.get_1 = function get_32(){
  return $clinit_LocalDom() , new IFrameElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$32$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 257, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$33$Type(){
}

defineClass(258, 1, $intern_12, LocalDom$lambda$33$Type);
_.get_1 = function get_33(){
  return $clinit_LocalDom() , new UListElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$33$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 258, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$34$Type(){
}

defineClass(259, 1, $intern_12, LocalDom$lambda$34$Type);
_.get_1 = function get_34(){
  return $clinit_LocalDom() , new OListElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$34$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 259, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$35$Type(){
}

defineClass(260, 1, $intern_12, LocalDom$lambda$35$Type);
_.get_1 = function get_35(){
  return $clinit_LocalDom() , new LIElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$35$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 260, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$36$Type(){
}

defineClass(261, 1, $intern_12, LocalDom$lambda$36$Type);
_.get_1 = function get_36(){
  return $clinit_LocalDom() , new PreElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$36$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 261, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$37$Type(){
}

defineClass(262, 1, $intern_12, LocalDom$lambda$37$Type);
_.get_1 = function get_37(){
  return $clinit_LocalDom() , new ParagraphElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$37$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 262, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$38$Type(){
}

defineClass(263, 1, $intern_12, LocalDom$lambda$38$Type);
_.get_1 = function get_38(){
  return $clinit_LocalDom() , new BRElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$38$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 263, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$39$Type(){
}

defineClass(264, 1, $intern_12, LocalDom$lambda$39$Type);
_.get_1 = function get_39(){
  return $clinit_LocalDom() , new HRElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$39$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 264, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$4$Type(){
}

defineClass(229, 1, $intern_12, LocalDom$lambda$4$Type);
_.get_1 = function get_40(){
  return $clinit_LocalDom() , new BodyElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$4$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 229, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$40$Type(){
}

defineClass(265, 1, $intern_12, LocalDom$lambda$40$Type);
_.get_1 = function get_41(){
  return $clinit_LocalDom() , new FormElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$40$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 265, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$41$Type(){
}

defineClass(266, 1, $intern_12, LocalDom$lambda$41$Type);
_.get_1 = function get_42(){
  return $clinit_LocalDom() , new MapElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$41$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 266, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$42$Type(){
}

defineClass(267, 1, $intern_12, LocalDom$lambda$42$Type);
_.get_1 = function get_43(){
  return $clinit_LocalDom() , new ParamElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$42$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 267, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$43$Type(){
}

defineClass(268, 1, $intern_12, LocalDom$lambda$43$Type);
_.get_1 = function get_44(){
  return $clinit_LocalDom() , new OptGroupElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$43$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 268, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$44$Type(){
}

defineClass(269, 1, $intern_12, LocalDom$lambda$44$Type);
_.get_1 = function get_45(){
  return $clinit_LocalDom() , new QuoteElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$44$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 269, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$45$Type(){
}

defineClass(270, 1, $intern_12, LocalDom$lambda$45$Type);
_.get_1 = function get_46(){
  return $clinit_LocalDom() , new QuoteElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$45$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 270, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$46$Type(){
}

defineClass(271, 1, $intern_12, LocalDom$lambda$46$Type);
_.get_1 = function get_47(){
  return $clinit_LocalDom() , new TableCaptionElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$46$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 271, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$47$Type(){
}

defineClass(272, 1, $intern_12, LocalDom$lambda$47$Type);
_.get_1 = function get_48(){
  return $clinit_LocalDom() , new DListElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$47$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 272, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$48$Type(){
}

defineClass(273, 1, $intern_12, LocalDom$lambda$48$Type);
_.get_1 = function get_49(){
  return $clinit_LocalDom() , new TitleElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$48$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 273, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$49$Type(){
}

defineClass(274, 1, $intern_12, LocalDom$lambda$49$Type);
_.get_1 = function get_50(){
  return $clinit_LocalDom() , new FieldSetElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$49$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 274, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$5$Type(){
}

defineClass(230, 1, $intern_12, LocalDom$lambda$5$Type);
_.get_1 = function get_51(){
  return $clinit_LocalDom() , new ButtonElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$5$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 230, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$50$Type(){
}

defineClass(275, 1, $intern_12, LocalDom$lambda$50$Type);
_.get_1 = function get_52(){
  return $clinit_LocalDom() , new FrameSetElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$50$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 275, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$51$Type(){
}

defineClass(276, 1, $intern_12, LocalDom$lambda$51$Type);
_.get_1 = function get_53(){
  return $clinit_LocalDom() , new MetaElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$51$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 276, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$52$Type(){
}

defineClass(277, 1, $intern_12, LocalDom$lambda$52$Type);
_.get_1 = function get_54(){
  return $clinit_LocalDom() , new SourceElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$52$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 277, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$53$Type(){
}

defineClass(278, 1, $intern_12, LocalDom$lambda$53$Type);
_.get_1 = function get_55(){
  return $clinit_LocalDom() , new LinkElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$53$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 278, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$54$Type(){
}

defineClass(279, 1, $intern_12, LocalDom$lambda$54$Type);
_.get_1 = function get_56(){
  return $clinit_LocalDom() , new ObjectElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$54$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 279, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$55$Type(){
}

defineClass(280, 1, $intern_12, LocalDom$lambda$55$Type);
_.get_1 = function get_57(){
  return $clinit_LocalDom() , new ModElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$55$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 280, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$56$Type(){
}

defineClass(281, 1, $intern_12, LocalDom$lambda$56$Type);
_.get_1 = function get_58(){
  return $clinit_LocalDom() , new ModElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$56$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 281, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$57$Type(){
}

defineClass(282, 1, $intern_12, LocalDom$lambda$57$Type);
_.get_1 = function get_59(){
  return $clinit_LocalDom() , new BaseElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$57$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 282, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$58$Type(){
}

defineClass(283, 1, $intern_12, LocalDom$lambda$58$Type);
_.get_1 = function get_60(){
  return $clinit_LocalDom() , new FrameElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$58$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 283, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$59$Type(){
}

defineClass(284, 1, $intern_12, LocalDom$lambda$59$Type);
_.get_1 = function get_61(){
  return $clinit_LocalDom() , new AreaElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$59$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 284, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$6$Type(){
}

defineClass(231, 1, $intern_12, LocalDom$lambda$6$Type);
_.get_1 = function get_62(){
  return $clinit_LocalDom() , new StyleElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$6$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 231, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$60$Type(){
}

defineClass(285, 1, $intern_12, LocalDom$lambda$60$Type);
_.get_1 = function get_63(){
  return $clinit_LocalDom() , new LegendElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$60$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 285, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$61$Type(nodeRemote_0){
  this.nodeRemote_0 = nodeRemote_0;
}

defineClass(286, 1, {}, LocalDom$lambda$61$Type);
_.negate = function negate_3(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_4(arg0){
  return lambda$61(this.nodeRemote_0, castTo(arg0, 7));
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$61$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 286, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$62$Type(){
}

defineClass(287, 1, {}, LocalDom$lambda$62$Type);
_.apply_0 = function apply_15(arg0){
  return $clinit_LocalDom() , throwClassCastExceptionUnlessNull(arg0) , null.$_nullMethod().$_nullMethod();
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$62$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 287, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$63$Type(f_remote_0){
  this.f_remote_0 = f_remote_0;
}

defineClass(288, 1, $intern_5, LocalDom$lambda$63$Type);
_.accept_0 = function accept_6(arg0){
  lambda$63(this.f_remote_0, castTo(arg0, 16));
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$63$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 288, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$64$Type(f_remote_0){
  this.f_remote_0 = f_remote_0;
}

defineClass(289, 1, $intern_5, LocalDom$lambda$64$Type);
_.accept_0 = function accept_7(arg0){
  lambda$64(this.f_remote_0, castTo(arg0, 16));
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$64$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 289, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$65$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(113, 1, $intern_5, LocalDom$lambda$65$Type);
_.accept_0 = function accept_8(arg0){
  $lambda$65(this.$$outer_0, castTo(arg0, 26));
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$65$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 113, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$66$Type(){
}

defineClass(290, 1, {}, LocalDom$lambda$66$Type);
_.negate = function negate_4(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_5(arg0){
  return $clinit_LocalDom() , $equals_0(castTo(arg0, 7).getNodeName(), 'td');
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$66$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 290, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$7$Type(){
}

defineClass(232, 1, $intern_12, LocalDom$lambda$7$Type);
_.get_1 = function get_64(){
  return $clinit_LocalDom() , new TableElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$7$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 232, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$8$Type(){
}

defineClass(233, 1, $intern_12, LocalDom$lambda$8$Type);
_.get_1 = function get_65(){
  return $clinit_LocalDom() , new HeadElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$8$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 233, Ljava_lang_Object_2_classLit);
function LocalDom$lambda$9$Type(){
}

defineClass(234, 1, $intern_12, LocalDom$lambda$9$Type);
_.get_1 = function get_66(){
  return $clinit_LocalDom() , new TableSectionElement;
}
;
var Lcom_google_gwt_dom_client_LocalDom$lambda$9$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 234, Ljava_lang_Object_2_classLit);
function $debugElement(this$static, element, withRemote, elementRemote, issue){
  var remoteIndex;
  $getInnerHTML_0(element.local);
  $clinit_ElementRemote();
  $provideRemoteDomTree0(elementRemote);
  $provideLocalDomTree0(withRemote.local, new StringBuilder, 0);
  $getParentElementRemote(elementRemote);
  remoteIndex = $provideRemoteIndex(elementRemote, true);
  $getString(remoteIndex);
  $log(($clinit_LocalDomDebug() , DEBUG_ISSUE), issue, stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
}

function $debugPutRemote(this$static, needsRemote, idx, withRemote){
  var childNodes, issue, localChild, remoteChild, tagName0;
  childNodes = castToJso($remote(withRemote)).childNodes;
  remoteChild = null;
  localChild = null;
  issue = null;
  childNodes.length != withRemote.getChildCount() && (issue = 'mismatched child node counts');
  if (issue == null) {
    remoteChild = childNodes[idx];
    if (!remoteChild) {
      issue = 'node removed';
    }
     else if (remoteChild.nodeType == 1) {
      localChild = $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(withRemote), idx);
      tagName0 = ($clinit_ElementRemote() , remoteChild.tagName);
      $equalsIgnoreCase(tagName0, localChild.getNodeName()) || (issue = 'mismatched tagname');
    }
  }
  issue != null && $debugElement(this$static, needsRemote, withRemote, castToJso($remote(withRemote)), issue);
}

function $log(channel, message, args){
  switch (channel.ordinal) {
    case 8:
    case 1:
    case 13:
    case 5:
    case 11:
    case 0:
    case 12:
    case 7:
      return;
  }
  args.length > 0 && (message = format_0(message, args));
  out_0('%s: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [channel, message]));
  channel == ($clinit_LocalDomDebug() , DEBUG_ISSUE) && $wnd.location.port.indexOf('8080') != -1 && ($clinit_GWT() , new RuntimeException);
}

function LocalDomDebugImpl(){
}

defineClass(330, 1, {}, LocalDomDebugImpl);
var Lcom_google_gwt_dom_client_LocalDomDebugImpl_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDomDebugImpl', 330, Ljava_lang_Object_2_classLit);
function LocalDomException(cause){
  RuntimeException_1.call(this, cause);
}

defineClass(358, 11, $intern_1, LocalDomException);
var Lcom_google_gwt_dom_client_LocalDomException_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDomException', 358, Ljava_lang_RuntimeException_2_classLit);
function $checkReceivedRecords(this$static){
  if (this$static.records.length == 0) {
    return;
  }
  var records = this$static.records;
  this$static.records = [];
  this$static.handleMutations(records);
}

function $connectObserver(this$static){
  if (this$static.disabled) {
    console.log('Mutation tracking not defined');
    return;
  }
  if (this$static.debugEntry || this$static.firstTimeConnect) {
    this$static.firstTimeConnect = false;
    console.log('Mutation observer :: connected');
  }
  this$static.observer.takeRecords();
  this$static.records = [];
  var config = {'childList':true, 'subtree':true};
  this$static.observer.observe(this$static.documentElement_0, config);
}

function $disconnectObserver(this$static){
  var mutationsList = this$static.observer.takeRecords();
  this$static.records = this$static.records.concat(mutationsList);
  if (!this$static.debugEntry) {
    this$static.observerConnected = false;
    this$static.observer.disconnect();
  }
}

function $handleMutations0(this$static, records){
  var added, childByRemote, childNodesModified, childNodesModifiedRemote, childNodesModifiedRemote$iterator, childNodesRemotePostMutation, currentLocalNode, cursor, doNotPutOwingToRace, elementRemote, history_0, idx, insertBefore, insertionPointLocalChildrenSize, itr, linkedToRemote, localNode, modifiedContainers, mutationRecords, node, node$iterator, nodeRemote, normalisedModifiedContainers, orderedNormalisedModifiedContainers, postMutationIdx, preObservedSize, previousChild, removed, retained, typedRecords;
  new LocalDomMutations$lambda$0$Type(records);
  $getOuterHtml_0(get_7().remote.documentElement);
  modifiedContainers = new Multimap;
  typedRecords = jsArrayToTypedArray(records);
  typedRecords.forEach_0(new LocalDomMutations$lambda$1$Type(modifiedContainers));
  normalisedModifiedContainers = new LinkedHashSet_0(modifiedContainers.map_0.keySet());
  itr = normalisedModifiedContainers.iterator();
  for (; itr.hasNext_0();) {
    nodeRemote = castToJso(itr.next_1());
    cursor = nodeRemote.parentNode;
    while (!!cursor && cursor != this$static.documentElement_0 && cursor != get_7().remote) {
      if (normalisedModifiedContainers.contains(cursor)) {
        itr.remove_2();
        break;
      }
      cursor = cursor.parentNode;
    }
    if (!cursor) {
      new LocalDomMutations$lambda$2$Type(nodeRemote);
      itr.remove_2();
    }
  }
  new LocalDomMutations$lambda$3$Type(normalisedModifiedContainers);
  orderedNormalisedModifiedContainers = castTo(normalisedModifiedContainers.stream().sorted_0(new LocalDomMutations$NodeDepthComparator).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  for (childNodesModifiedRemote$iterator = orderedNormalisedModifiedContainers.iterator(); childNodesModifiedRemote$iterator.hasNext_0();) {
    childNodesModifiedRemote = castToJso(childNodesModifiedRemote$iterator.next_1());
    elementRemote = childNodesModifiedRemote;
    mutationRecords = castTo(castTo(modifiedContainers.map_0.get_0(childNodesModifiedRemote), 15).stream().collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [IDENTITY_FINISH]))), 15);
    $clinit_LocalDom();
    if (!instance.remoteLookup.containsKey_0(elementRemote)) {
      continue;
    }
    $getParentElementRemote(elementRemote);
    childNodesModified = castTo($nodeFor0(instance, elementRemote, false), 5);
    insertionPointLocalChildrenSize = childNodesModified.getChildCount();
    linkedToRemote = stream0(childNodesModified.getChildNodes()).filter_0(new LocalDomMutations$1methodref$linkedToRemote$Type).count();
    childNodesRemotePostMutation = castTo($streamRemote(castToJso($remote(childNodesModified)).childNodes).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [IDENTITY_FINISH]))), 15);
    removed = castTo(mutationRecords.stream().map_1(new LocalDomMutations$lambda$5$Type).flatMap(new LocalDomMutations$lambda$6$Type).collect_0(of(new Collectors$23methodref$ctor$Type, new Collectors$24methodref$add$Type, new Collectors$lambda$50$Type, new Collectors$lambda$51$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [UNORDERED, IDENTITY_FINISH]))), 30);
    mutationRecords.removeIf(new LocalDomMutations$lambda$7$Type(this$static, removed, childNodesModified));
    history_0 = new LocalDomMutations$ChildModificationHistory(childNodesModified, mutationRecords);
    $model(history_0);
    preObservedSize = castTo(last_0(history_0.states), 56).children.size_1();
    new LocalDomMutations$lambda$11$Type(childNodesModified, insertionPointLocalChildrenSize, linkedToRemote, childNodesRemotePostMutation, preObservedSize);
    new LocalDomMutations$lambda$12$Type(childNodesModified);
    new LocalDomMutations$lambda$13$Type(childNodesRemotePostMutation);
    new LocalDomMutations$lambda$14$Type(history_0);
    new LocalDomMutations$lambda$15$Type(history_0);
    insertionPointLocalChildrenSize != preObservedSize && checkState(insertionPointLocalChildrenSize == preObservedSize);
    childByRemote = new LinkedHashMap;
    retained = new LinkedHashSet;
    added = new LinkedHashSet;
    for (idx = 0; idx < insertionPointLocalChildrenSize; idx++) {
      node = $getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(childNodesModified), idx);
      nodeRemote = castToJso(castTo(last_0(history_0.states), 56).children.get_2(idx));
      doNotPutOwingToRace = false;
      childNodesRemotePostMutation.contains(nodeRemote) || childNodesRemotePostMutation.contains(node.remote_0()) && (doNotPutOwingToRace = true);
      doNotPutOwingToRace || node.putRemote(nodeRemote, true);
      if (childNodesRemotePostMutation.contains(nodeRemote)) {
        childByRemote.put_0(nodeRemote, node);
        retained.add_0(node);
      }
    }
    for (node$iterator = castTo(stream0(childNodesModified.getChildNodes()).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [IDENTITY_FINISH]))), 15).iterator(); node$iterator.hasNext_0();) {
      node = castTo(node$iterator.next_1(), 7);
      retained.contains(node) || node.removeFromParent();
    }
    postMutationIdx = 0;
    previousChild = null;
    for (; postMutationIdx < childNodesRemotePostMutation.size_1(); postMutationIdx++) {
      nodeRemote = castToJso(childNodesRemotePostMutation.get_2(postMutationIdx));
      localNode = castTo(childByRemote.get_0(nodeRemote), 7);
      if (!localNode) {
        instance.remoteLookup.containsKey_0(nodeRemote) && (localNode = $nodeFor0(instance, nodeRemote, false));
        !localNode && (localNode = $resolveExternal0(instance, nodeRemote));
        added.add_0(localNode);
      }
      currentLocalNode = postMutationIdx >= childNodesModified.getChildCount()?null:$getItem(getChildNodes__Lcom_google_gwt_dom_client_NodeList___devirtual$(childNodesModified), postMutationIdx);
      if (localNode != currentLocalNode) {
        insertBefore = !previousChild?null:previousChild.getNextSibling();
        !!insertBefore && !added.contains(insertBefore) && !retained.contains(insertBefore) && invokeJsDebugger();
        if (insertBefore) {
          checkState(insertBefore.getParentNode() == childNodesModified);
          $insertBefore(childNodesModified, localNode, insertBefore);
        }
         else {
          childNodesModified.appendChild_0(localNode);
        }
      }
      previousChild = localNode;
    }
  }
}

function $lambda$7(removed_1, childNodesModified_2, mr_2){
  var addedRemote, alreadyInChildList;
  if (mr_2.addedNodes.length == 1) {
    addedRemote = mr_2.addedNodes[0];
    if (removed_1.contains(addedRemote)) {
      return false;
    }
     else {
      alreadyInChildList = $getChildren(childNodesModified_2.local).stream().map_1(new LocalDomMutations$lambda$8$Type).anyMatch(new LocalDomMutations$lambda$9$Type(addedRemote));
      alreadyInChildList && new LocalDomMutations$lambda$10$Type(addedRemote);
      return alreadyInChildList;
    }
  }
   else {
    return false;
  }
}

function $setupObserver(this$static){
  this$static.disabled = this$static.disabled || typeof MutationObserver == 'undefined';
  if (this$static.disabled) {
    console.log('Mutation tracking not defined');
    return;
  }
  this$static.documentElement_0 = $doc.documentElement;
  var _this = this$static;
  var callback = function(mutationsList, observer){
    _this.records = _this.records.concat(mutationsList);
  }
  ;
  this$static.observer = new MutationObserver(callback);
}

function $startObserving(this$static){
  if (this$static.disabled) {
    return;
  }
  !this$static.observer && $setupObserver(this$static);
  if (this$static.observerConnected) {
    this$static.observer.takeRecords();
    this$static.records = [];
  }
   else {
    $connectObserver(this$static);
    this$static.observerConnected = true;
  }
  this$static.inGwtEventCycle = false;
}

function $startObservingIfNotInEventCycle(this$static){
  this$static.inGwtEventCycle || $startObserving(this$static);
}

function $stopObserving(this$static){
  if (this$static.disabled) {
    return;
  }
  if (!this$static.observer) {
    return;
  }
  $disconnectObserver(this$static);
  $checkReceivedRecords(this$static);
  this$static.inGwtEventCycle = true;
}

function LocalDomMutations(){
}

function lambda$1(modifiedContainers_0, record_1){
  $add_2(modifiedContainers_0, record_1.target, record_1);
}

function lambda$11(childNodesModified_0, insertionPointLocalChildrenSize_1, linkedToRemote_2, childNodesRemotePostMutation_4, preObservedSize_5){
  return format_0('Insertionpoint - %s - local kids: %s - local linked to remote: %s - remote kids: %s - pre-observed kids: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [childNodesModified_0.local.tagName_0, valueOf(insertionPointLocalChildrenSize_1), valueOf_0(linkedToRemote_2), valueOf(childNodesRemotePostMutation_4.size_1()), valueOf(preObservedSize_5)]));
}

function lambda$9(addedRemote_0, node_1){
  return maskUndefined(node_1) === maskUndefined(addedRemote_0);
}

defineClass(293, 1, {}, LocalDomMutations);
_.handleMutations = function handleMutations(records){
  var e;
  try {
    $handleMutations0(this, records);
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 11)) {
      e = $e0;
      $printStackTraceImpl(e, ($clinit_System() , err), '');
      throw toJs(e);
    }
     else 
      throw toJs($e0);
  }
}
;
_.debugEntry = false;
_.disabled = false;
_.firstTimeConnect = true;
_.inGwtEventCycle = false;
_.observer = null;
_.observerConnected = false;
var Lcom_google_gwt_dom_client_LocalDomMutations_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDomMutations', 293, Ljava_lang_Object_2_classLit);
function LocalDomMutations$1methodref$linkedToRemote$Type(){
}

defineClass(303, 1, {}, LocalDomMutations$1methodref$linkedToRemote$Type);
_.negate = function negate_5(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_6(arg0){
  return castTo(arg0, 7).linkedToRemote();
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$1methodref$linkedToRemote$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 303, Ljava_lang_Object_2_classLit);
function $model(this$static){
  var currentState, mutationRecord, mutationRecord$iterator, reverseMutations;
  currentState = fromCurrentState(castToJso($remote(this$static.insertionPoint)));
  this$static.states.add_0(currentState);
  reverseMutations = castTo(this$static.mutationRecords.stream().collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  reverse(reverseMutations);
  for (mutationRecord$iterator = reverseMutations.iterator(); mutationRecord$iterator.hasNext_0();) {
    mutationRecord = castToJso(mutationRecord$iterator.next_1());
    currentState = $undo(currentState, mutationRecord);
    this$static.states.add_0(currentState);
  }
}

function $toLogString(this$static){
  var formatBuilder, idx, mutationRecord, mutationRecord$iterator;
  formatBuilder = new FormatBuilder;
  idx = 0;
  for (mutationRecord$iterator = this$static.mutationRecords.iterator(); mutationRecord$iterator.hasNext_0();) {
    mutationRecord = castToJso(mutationRecord$iterator.next_1());
    $newLine($format(formatBuilder, '  Mutation record %s #%s %s:\n   target %s\n   pr-sib %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(idx++), mutationRecord.type, valueOf(getHashCode_0(mutationRecord)), shortLog(mutationRecord.target), shortLog(mutationRecord.previousSibling)])));
    $streamRemote(mutationRecord.addedNodes).forEach_0(new LocalDomMutations$ChildModificationHistory$lambda$0$Type(formatBuilder));
    $streamRemote(mutationRecord.removedNodes).forEach_0(new LocalDomMutations$ChildModificationHistory$lambda$1$Type(formatBuilder));
  }
  return formatBuilder.sb.string;
}

function LocalDomMutations$ChildModificationHistory(insertionPoint, mutationRecords){
  this.states = new ArrayList;
  this.insertionPoint = insertionPoint;
  this.mutationRecords = mutationRecords;
}

defineClass(294, 1, {}, LocalDomMutations$ChildModificationHistory);
var Lcom_google_gwt_dom_client_LocalDomMutations$ChildModificationHistory_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDomMutations/ChildModificationHistory', 294, Ljava_lang_Object_2_classLit);
function LocalDomMutations$ChildModificationHistory$lambda$0$Type(formatBuilder_0){
  this.formatBuilder_0 = formatBuilder_0;
}

defineClass(296, 1, $intern_5, LocalDomMutations$ChildModificationHistory$lambda$0$Type);
_.accept_0 = function accept_9(arg0){
  $newLine($format(this.formatBuilder_0, '    + : %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [shortLog(castToJso(arg0))])));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$ChildModificationHistory$lambda$0$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 296, Ljava_lang_Object_2_classLit);
function LocalDomMutations$ChildModificationHistory$lambda$1$Type(formatBuilder_0){
  this.formatBuilder_0 = formatBuilder_0;
}

defineClass(297, 1, $intern_5, LocalDomMutations$ChildModificationHistory$lambda$1$Type);
_.accept_0 = function accept_10(arg0){
  $newLine($format(this.formatBuilder_0, '    - : %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [shortLog(castToJso(arg0))])));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$ChildModificationHistory$lambda$1$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 297, Ljava_lang_Object_2_classLit);
function $undo(this$static, record){
  var added, idx, insertionPoint, previousSibling, removed, undone;
  undone = new LocalDomMutations$ChildModificationHistoryState;
  undone.elementRemote = this$static.elementRemote;
  undone.children = castTo(this$static.children.stream().collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  added = castTo($streamRemote(record.addedNodes).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [IDENTITY_FINISH]))), 15);
  undone.children.removeAll(added);
  removed = castTo($streamRemote(record.removedNodes).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [IDENTITY_FINISH]))), 15);
  if (removed.size_1() > 0) {
    previousSibling = record.previousSibling;
    insertionPoint = !previousSibling?0:this$static.children.indexOf_0(previousSibling) + 1;
    for (idx = 0; idx < removed.size_1(); idx++) {
      undone.children.add_1(insertionPoint + idx, castToJso(removed.get_2(idx)));
    }
  }
  return undone;
}

function LocalDomMutations$ChildModificationHistoryState(){
  this.children = new ArrayList;
}

function fromCurrentState(typedRemote){
  var state;
  state = new LocalDomMutations$ChildModificationHistoryState;
  state.elementRemote = typedRemote;
  state.children = castTo($streamRemote(typedRemote.childNodes).collect_0(of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  return state;
}

defineClass(56, 1, {56:1}, LocalDomMutations$ChildModificationHistoryState);
var Lcom_google_gwt_dom_client_LocalDomMutations$ChildModificationHistoryState_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDomMutations/ChildModificationHistoryState', 56, Ljava_lang_Object_2_classLit);
function $compare(this$static, o1, o2){
  return castTo($get_5(this$static.depths, o1), 25).value_0 - castTo($get_5(this$static.depths, o2), 25).value_0;
}

function LocalDomMutations$NodeDepthComparator(){
  this.depths = new CachingMap(new LocalDomMutations$NodeDepthComparator$0methodref$depth$Type);
}

function depth_0(node){
  var cursor, depth;
  depth = 0;
  cursor = node;
  while (cursor) {
    ++depth;
    cursor = cursor.parentNode;
  }
  return depth;
}

defineClass(295, 1, {}, LocalDomMutations$NodeDepthComparator);
_.compare = function compare(o1, o2){
  return $compare(this, castToJso(o1), castToJso(o2));
}
;
_.equals_0 = function equals_8(other){
  return maskUndefined(this) === maskUndefined(other);
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$NodeDepthComparator_2_classLit = createForClass('com.google.gwt.dom.client', 'LocalDomMutations/NodeDepthComparator', 295, Ljava_lang_Object_2_classLit);
function LocalDomMutations$NodeDepthComparator$0methodref$depth$Type(){
}

defineClass(298, 1, {}, LocalDomMutations$NodeDepthComparator$0methodref$depth$Type);
_.apply_0 = function apply_16(arg0){
  return valueOf(depth_0(arg0));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$NodeDepthComparator$0methodref$depth$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 298, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$0$Type(records_0){
  this.records_0 = records_0;
}

defineClass(299, 1, $intern_12, LocalDomMutations$lambda$0$Type);
_.get_1 = function get_67(){
  return format_0('Jv records: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(this.records_0.length)]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$0$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 299, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$1$Type(modifiedContainers_0){
  this.modifiedContainers_0 = modifiedContainers_0;
}

defineClass(300, 1, $intern_5, LocalDomMutations$lambda$1$Type);
_.accept_0 = function accept_11(arg0){
  lambda$1(this.modifiedContainers_0, castToJso(arg0));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$1$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 300, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$10$Type(addedRemote_0){
  this.addedRemote_0 = addedRemote_0;
}

defineClass(308, 1, $intern_12, LocalDomMutations$lambda$10$Type);
_.get_1 = function get_68(){
  return format_0('removing add node from mutation list because already in localdom children - %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(getHashCode_0(this.addedRemote_0))]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$10$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 308, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$11$Type(childNodesModified_0, insertionPointLocalChildrenSize_1, linkedToRemote_2, childNodesRemotePostMutation_4, preObservedSize_5){
  this.childNodesModified_0 = childNodesModified_0;
  this.insertionPointLocalChildrenSize_1 = insertionPointLocalChildrenSize_1;
  this.linkedToRemote_2 = linkedToRemote_2;
  this.childNodesRemotePostMutation_4 = childNodesRemotePostMutation_4;
  this.preObservedSize_5 = preObservedSize_5;
}

defineClass(310, 1, $intern_12, LocalDomMutations$lambda$11$Type);
_.get_1 = function get_69(){
  return lambda$11(this.childNodesModified_0, this.insertionPointLocalChildrenSize_1, this.linkedToRemote_2, this.childNodesRemotePostMutation_4, this.preObservedSize_5);
}
;
_.insertionPointLocalChildrenSize_1 = 0;
_.linkedToRemote_2 = 0;
_.preObservedSize_5 = 0;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$11$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 310, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$12$Type(childNodesModified_0){
  this.childNodesModified_0 = childNodesModified_0;
}

defineClass(311, 1, $intern_12, LocalDomMutations$lambda$12$Type);
_.get_1 = function get_70(){
  return format_0('Local children (pre-mutation):\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [shortLog_0($getChildren(this.childNodesModified_0.local))]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$12$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 311, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$13$Type(childNodesRemotePostMutation_0){
  this.childNodesRemotePostMutation_0 = childNodesRemotePostMutation_0;
}

defineClass(312, 1, $intern_12, LocalDomMutations$lambda$13$Type);
_.get_1 = function get_71(){
  return format_0('Remote children  (post-mutation):\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [shortLog_0(this.childNodesRemotePostMutation_0)]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$13$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 312, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$14$Type(history_0){
  this.history_0 = history_0;
}

defineClass(313, 1, $intern_12, LocalDomMutations$lambda$14$Type);
_.get_1 = function get_72(){
  return format_0('Remote children (pre-observed):\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [shortLog_0(castTo(last_0(this.history_0.states), 56).children)]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$14$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 313, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$15$Type(history_0){
  this.history_0 = history_0;
}

defineClass(314, 1, $intern_12, LocalDomMutations$lambda$15$Type);
_.get_1 = function get_73(){
  return format_0('Mutations:\n%s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [$toLogString(this.history_0)]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$15$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 314, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$2$Type(nodeRemote_0){
  this.nodeRemote_0 = nodeRemote_0;
}

defineClass(301, 1, $intern_12, LocalDomMutations$lambda$2$Type);
_.get_1 = function get_74(){
  return format_0('LDM - ignoring root (detached): %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [shortLog(this.nodeRemote_0)]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$2$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 301, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$3$Type(normalisedModifiedContainers_0){
  this.normalisedModifiedContainers_0 = normalisedModifiedContainers_0;
}

defineClass(302, 1, $intern_12, LocalDomMutations$lambda$3$Type);
_.get_1 = function get_75(){
  return format_0('Jv - root nodes: %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(this.normalisedModifiedContainers_0.size_1())]));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$3$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 302, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$5$Type(){
}

defineClass(304, 1, {}, LocalDomMutations$lambda$5$Type);
_.apply_0 = function apply_17(arg0){
  return castToJso(arg0).removedNodes;
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$5$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 304, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$6$Type(){
}

defineClass(305, 1, {}, LocalDomMutations$lambda$6$Type);
_.apply_0 = function apply_18(arg0){
  return $streamRemote(castToJso(arg0));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$6$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 305, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$7$Type($$outer_0, removed_1, childNodesModified_2){
  this.$$outer_0 = $$outer_0;
  this.removed_1 = removed_1;
  this.childNodesModified_2 = childNodesModified_2;
}

defineClass(309, 1, {}, LocalDomMutations$lambda$7$Type);
_.negate = function negate_6(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_7(arg0){
  return $lambda$7(this.removed_1, this.childNodesModified_2, castToJso(arg0));
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$7$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 309, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$8$Type(){
}

defineClass(306, 1, {}, LocalDomMutations$lambda$8$Type);
_.apply_0 = function apply_19(arg0){
  return castTo(arg0, 26).node().remote_0();
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$8$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 306, Ljava_lang_Object_2_classLit);
function LocalDomMutations$lambda$9$Type(addedRemote_0){
  this.addedRemote_0 = addedRemote_0;
}

defineClass(307, 1, {}, LocalDomMutations$lambda$9$Type);
_.negate = function negate_7(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_8(arg0){
  return lambda$9(this.addedRemote_0, arg0);
}
;
var Lcom_google_gwt_dom_client_LocalDomMutations$lambda$9$Type_2_classLit = createForAnonymousClass('com.google.gwt.dom.client', 307, Ljava_lang_Object_2_classLit);
function MapElement(){
}

defineClass(176, 5, $intern_7, MapElement);
var Lcom_google_gwt_dom_client_MapElement_2_classLit = createForClass('com.google.gwt.dom.client', 'MapElement', 176, Lcom_google_gwt_dom_client_Element_2_classLit);
function MetaElement(){
}

defineClass(184, 5, $intern_7, MetaElement);
var Lcom_google_gwt_dom_client_MetaElement_2_classLit = createForClass('com.google.gwt.dom.client', 'MetaElement', 184, Lcom_google_gwt_dom_client_Element_2_classLit);
function ModElement(){
}

defineClass(103, 5, $intern_7, ModElement);
var Lcom_google_gwt_dom_client_ModElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ModElement', 103, Lcom_google_gwt_dom_client_Element_2_classLit);
function $getItem(this$static, index_0){
  return getItem_I_Lcom_google_gwt_dom_client_Node___devirtual$(this$static.impl, index_0);
}

function NodeList_0(impl){
  this.impl = impl;
}

defineClass(128, 1, {}, NodeList_0);
_.forEach_0 = function forEach_3(action){
  $forEach(this, action);
}
;
_.getItem = function getItem(index_0){
  return $getItem(this, index_0);
}
;
_.getLength = function getLength(){
  return getLength__I__devirtual$(this.impl);
}
;
_.iterator = function iterator_3(){
  return new NodeList$NodeListIterator(this);
}
;
var Lcom_google_gwt_dom_client_NodeList_2_classLit = createForClass('com.google.gwt.dom.client', 'NodeList', 128, Ljava_lang_Object_2_classLit);
function $next_1(this$static){
  if (this$static.cursor >= getLength__I__devirtual$(this$static.this$01.impl)) {
    throw toJs(new NoSuchElementException);
  }
  return $getItem(this$static.this$01, this$static.cursor++);
}

function NodeList$NodeListIterator(this$0){
  this.this$01 = this$0;
}

defineClass(357, 1, {}, NodeList$NodeListIterator);
_.forEachRemaining = function forEachRemaining_3(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_4(){
  return $next_1(this);
}
;
_.remove_2 = function remove_11(){
  $remove_8();
}
;
_.hasNext_0 = function hasNext_3(){
  return getLength__I__devirtual$(this.this$01.impl) > this.cursor;
}
;
_.cursor = 0;
var Lcom_google_gwt_dom_client_NodeList$NodeListIterator_2_classLit = createForClass('com.google.gwt.dom.client', 'NodeList/NodeListIterator', 357, Ljava_lang_Object_2_classLit);
function NodeListLocal(nodes){
  this.nodes = nodes;
}

defineClass(384, 1, {}, NodeListLocal);
_.getItem = function getItem_0(index_0){
  return index_0 < 0 || index_0 >= this.nodes.size_1()?null:castTo(this.nodes.get_2(index_0), 26).node();
}
;
_.getLength = function getLength_0(){
  return this.nodes.size_1();
}
;
var Lcom_google_gwt_dom_client_NodeListLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'NodeListLocal', 384, Ljava_lang_Object_2_classLit);
function $streamRemote(this$static){
  var idx, list;
  list = new ArrayList;
  for (idx = 0; idx < this$static.length; idx++) {
    list.add_0(this$static[idx]);
  }
  return list.stream();
}

function getItem_I_Lcom_google_gwt_dom_client_Node___devirtual$(this$static, index_0){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.getItem(index_0):nodeFor_0(this$static[index_0]);
}

function getLength__I__devirtual$(this$static){
  return hasJavaObjectVirtualDispatch(this$static)?this$static.getLength():this$static.length;
}

function OListElement(){
}

defineClass(169, 5, $intern_7, OListElement);
var Lcom_google_gwt_dom_client_OListElement_2_classLit = createForClass('com.google.gwt.dom.client', 'OListElement', 169, Lcom_google_gwt_dom_client_Element_2_classLit);
function ObjectElement(){
}

defineClass(187, 5, $intern_7, ObjectElement);
var Lcom_google_gwt_dom_client_ObjectElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ObjectElement', 187, Lcom_google_gwt_dom_client_Element_2_classLit);
function OptGroupElement(){
}

defineClass(178, 5, $intern_7, OptGroupElement);
var Lcom_google_gwt_dom_client_OptGroupElement_2_classLit = createForClass('com.google.gwt.dom.client', 'OptGroupElement', 178, Lcom_google_gwt_dom_client_Element_2_classLit);
function OptionElement(){
}

defineClass(165, 5, $intern_7, OptionElement);
var Lcom_google_gwt_dom_client_OptionElement_2_classLit = createForClass('com.google.gwt.dom.client', 'OptionElement', 165, Lcom_google_gwt_dom_client_Element_2_classLit);
function ParagraphElement(){
}

defineClass(172, 5, $intern_7, ParagraphElement);
var Lcom_google_gwt_dom_client_ParagraphElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ParagraphElement', 172, Lcom_google_gwt_dom_client_Element_2_classLit);
function ParamElement(){
}

defineClass(177, 5, $intern_7, ParamElement);
var Lcom_google_gwt_dom_client_ParamElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ParamElement', 177, Lcom_google_gwt_dom_client_Element_2_classLit);
function PreElement(){
}

defineClass(171, 5, $intern_7, PreElement);
var Lcom_google_gwt_dom_client_PreElement_2_classLit = createForClass('com.google.gwt.dom.client', 'PreElement', 171, Lcom_google_gwt_dom_client_Element_2_classLit);
function QuoteElement(){
}

defineClass(102, 5, $intern_7, QuoteElement);
var Lcom_google_gwt_dom_client_QuoteElement_2_classLit = createForClass('com.google.gwt.dom.client', 'QuoteElement', 102, Lcom_google_gwt_dom_client_Element_2_classLit);
function ScriptElement(){
}

defineClass(163, 5, $intern_7, ScriptElement);
var Lcom_google_gwt_dom_client_ScriptElement_2_classLit = createForClass('com.google.gwt.dom.client', 'ScriptElement', 163, Lcom_google_gwt_dom_client_Element_2_classLit);
function SelectElement(){
}

defineClass(164, 5, $intern_7, SelectElement);
_.setPropertyString = function setPropertyString_3(name_0, value_0){
  $ensureRemoteCheck(this);
  $equals_0(name_0, 'multiple') && !$booleanValue(($clinit_Boolean() , $equalsIgnoreCase('true', value_0)?true:false))?$removeAttribute(this.local, 'multiple'):$setPropertyString(this.local, name_0, value_0);
  setPropertyString_Ljava_lang_String_Ljava_lang_String__V__devirtual$($remote(this), name_0, value_0);
}
;
var Lcom_google_gwt_dom_client_SelectElement_2_classLit = createForClass('com.google.gwt.dom.client', 'SelectElement', 164, Lcom_google_gwt_dom_client_Element_2_classLit);
function SourceElement(){
}

defineClass(185, 5, $intern_7, SourceElement);
var Lcom_google_gwt_dom_client_SourceElement_2_classLit = createForClass('com.google.gwt.dom.client', 'SourceElement', 185, Lcom_google_gwt_dom_client_Element_2_classLit);
function SpanElement(){
}

defineClass(154, 5, $intern_7, SpanElement);
var Lcom_google_gwt_dom_client_SpanElement_2_classLit = createForClass('com.google.gwt.dom.client', 'SpanElement', 154, Lcom_google_gwt_dom_client_Element_2_classLit);
function Style(element){
  $clinit_StyleNull();
  this.local = new StyleLocal;
}

defineClass(387, 1, {}, Style);
var Lcom_google_gwt_dom_client_Style_2_classLit = createForClass('com.google.gwt.dom.client', 'Style', 387, Ljava_lang_Object_2_classLit);
function StyleElement(){
}

defineClass(157, 5, $intern_7, StyleElement);
var Lcom_google_gwt_dom_client_StyleElement_2_classLit = createForClass('com.google.gwt.dom.client', 'StyleElement', 157, Lcom_google_gwt_dom_client_Element_2_classLit);
function StyleLocal(){
  this.properties = new LightMap;
}

defineClass(385, 1, {}, StyleLocal);
var Lcom_google_gwt_dom_client_StyleLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'StyleLocal', 385, Ljava_lang_Object_2_classLit);
function $clinit_StyleNull(){
  $clinit_StyleNull = emptyMethod;
  INSTANCE_3 = new StyleNull;
}

function StyleNull(){
}

defineClass(405, 1, {}, StyleNull);
var INSTANCE_3;
var Lcom_google_gwt_dom_client_StyleNull_2_classLit = createForClass('com.google.gwt.dom.client', 'StyleNull', 405, Ljava_lang_Object_2_classLit);
function TableCaptionElement(){
}

defineClass(179, 5, $intern_7, TableCaptionElement);
var Lcom_google_gwt_dom_client_TableCaptionElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TableCaptionElement', 179, Lcom_google_gwt_dom_client_Element_2_classLit);
function TableCellElement(){
}

defineClass(99, 5, $intern_7, TableCellElement);
var Lcom_google_gwt_dom_client_TableCellElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TableCellElement', 99, Lcom_google_gwt_dom_client_Element_2_classLit);
function TableColElement(){
}

defineClass(100, 5, $intern_7, TableColElement);
var Lcom_google_gwt_dom_client_TableColElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TableColElement', 100, Lcom_google_gwt_dom_client_Element_2_classLit);
function TableElement(){
}

defineClass(97, 5, {12:1, 8:1, 5:1, 7:1, 97:1}, TableElement);
var Lcom_google_gwt_dom_client_TableElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TableElement', 97, Lcom_google_gwt_dom_client_Element_2_classLit);
function TableRowElement(){
}

defineClass(101, 5, {12:1, 8:1, 5:1, 7:1, 101:1}, TableRowElement);
_.validateInsert = function validateInsert_0(newChild){
  var tagName;
  if (newChild.getNodeType() == 1) {
    tagName = newChild.getNodeName().toLowerCase();
    checkState($equals_0(tagName, 'th') || $equals_0(tagName, 'td'));
  }
}
;
var Lcom_google_gwt_dom_client_TableRowElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TableRowElement', 101, Lcom_google_gwt_dom_client_Element_2_classLit);
function TableSectionElement(){
}

defineClass(76, 5, $intern_7, TableSectionElement);
var Lcom_google_gwt_dom_client_TableSectionElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TableSectionElement', 76, Lcom_google_gwt_dom_client_Element_2_classLit);
function Text_0(local){
  this.local = local;
  this.remote = ($clinit_TextNull() , INSTANCE_4);
}

defineClass(123, 7, {12:1, 8:1, 7:1, 123:1}, Text_0);
_.cast = function cast_1(){
  return this;
}
;
_.local_0 = function local_2(){
  return this.local;
}
;
_.remote_0 = function remote_3(){
  return this.remote;
}
;
_.typedRemote = function typedRemote_3(){
  return castToJso(this.remote);
}
;
_.linkedToRemote = function linkedToRemote_3(){
  return this.remote != ($clinit_TextNull() , INSTANCE_4);
}
;
_.node = function node_6(){
  return this;
}
;
_.putRemote = function putRemote_1(remote, resolved){
  checkState(this.resolvedEventId > 0 == resolved);
  this.remote = castToAllowJso(remote, 417);
}
;
_.toString_0 = function toString_21(){
  return format_0('#TEXT[%s]', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this.local.text_0]));
}
;
var Lcom_google_gwt_dom_client_Text_2_classLit = createForClass('com.google.gwt.dom.client', 'Text', 123, Lcom_google_gwt_dom_client_Node_2_classLit);
function TextAreaElement(){
}

defineClass(159, 5, $intern_7, TextAreaElement);
_.setPropertyString = function setPropertyString_4(name_0, value_0){
  $ensureRemoteCheck(this);
  $equals_0('value', name_0)?$setInnerText(this.local, value_0):$setPropertyString(this.local, name_0, value_0);
  setPropertyString_Ljava_lang_String_Ljava_lang_String__V__devirtual$($remote(this), name_0, value_0);
}
;
var Lcom_google_gwt_dom_client_TextAreaElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TextAreaElement', 159, Lcom_google_gwt_dom_client_Element_2_classLit);
function $appendUnescaped(this$static, builder){
  $appendUnsafeHtml(builder, this$static.text_0);
}

function TextLocal(documentLocal, text_0){
  this.ownerDocument = documentLocal;
  this.text_0 = text_0;
}

defineClass(125, 26, {8:1, 53:1, 26:1, 125:1}, TextLocal);
_.appendOuterHtml = function appendOuterHtml_2(builder){
  $appendEscapedNoQuotes(builder, this.text_0);
}
;
_.getNodeName = function getNodeName_5(){
  return '#text';
}
;
_.getNodeType = function getNodeType_5(){
  return 3;
}
;
_.getNodeValue = function getNodeValue_6(){
  return this.text_0;
}
;
_.node = function node_7(){
  return this.textNode;
}
;
_.toString_0 = function toString_22(){
  return format_0('#TEXT[%s]', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [this.text_0]));
}
;
var Lcom_google_gwt_dom_client_TextLocal_2_classLit = createForClass('com.google.gwt.dom.client', 'TextLocal', 125, Lcom_google_gwt_dom_client_NodeLocal_2_classLit);
function $clinit_TextNull(){
  $clinit_TextNull = emptyMethod;
  INSTANCE_4 = new TextNull;
}

function TextNull(){
}

defineClass(403, 449, $intern_8, TextNull);
_.appendOuterHtml = function appendOuterHtml_3(builder){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeName = function getNodeName_6(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeType = function getNodeType_6(){
  throw toJs(new UnsupportedOperationException);
}
;
_.getNodeValue = function getNodeValue_7(){
  throw toJs(new UnsupportedOperationException);
}
;
var INSTANCE_4;
var Lcom_google_gwt_dom_client_TextNull_2_classLit = createForClass('com.google.gwt.dom.client', 'TextNull', 403, Lcom_google_gwt_dom_client_NodeLocalNull_2_classLit);
function TitleElement(){
}

defineClass(181, 5, $intern_7, TitleElement);
var Lcom_google_gwt_dom_client_TitleElement_2_classLit = createForClass('com.google.gwt.dom.client', 'TitleElement', 181, Lcom_google_gwt_dom_client_Element_2_classLit);
function UListElement(){
}

defineClass(168, 5, $intern_7, UListElement);
var Lcom_google_gwt_dom_client_UListElement_2_classLit = createForClass('com.google.gwt.dom.client', 'UListElement', 168, Lcom_google_gwt_dom_client_Element_2_classLit);
function $clinit_UnsafeHtmlBuilder(){
  $clinit_UnsafeHtmlBuilder = emptyMethod;
  unsafeTest = new RegExp('[&<>\'"]');
  unsafeTestNoQuotes = new RegExp('[&<>]');
}

function $appendEscaped(this$static, text_0){
  text_0 != null && (!$exec(unsafeTest, text_0)?$append_5(this$static.sb, text_0):$append_5(this$static.sb, htmlEscape(text_0)));
  return this$static;
}

function $appendEscapedNoQuotes(this$static, text_0){
  text_0 != null && (!$exec(unsafeTestNoQuotes, text_0)?$append_5(this$static.sb, text_0):$append_5(this$static.sb, htmlEscapeNoQuotes(text_0)));
  return this$static;
}

function $appendHtmlConstant(this$static, html){
  $append_5(this$static.sb, html);
  return this$static;
}

function $appendUnsafeHtml(this$static, html){
  $append_5(this$static.sb, html);
  return this$static;
}

function UnsafeHtmlBuilder(){
  $clinit_UnsafeHtmlBuilder();
  this.sb = new StringBuilder;
}

defineClass(142, 1, {}, UnsafeHtmlBuilder);
var unsafeTest, unsafeTestNoQuotes;
var Lcom_google_gwt_dom_client_UnsafeHtmlBuilder_2_classLit = createForClass('com.google.gwt.dom.client', 'UnsafeHtmlBuilder', 142, Ljava_lang_Object_2_classLit);
function UnsafeHtmlBuilder$SafeHtmlString(html){
  if (html == null) {
    throw toJs(new NullPointerException_0('html is null'));
  }
  this.html = html;
}

defineClass(121, 1, $intern_13, UnsafeHtmlBuilder$SafeHtmlString);
_.asString = function asString(){
  return this.html;
}
;
_.equals_0 = function equals_9(obj){
  if (!instanceOf(obj, 106)) {
    return false;
  }
  return $equals_0(this.html, castTo(obj, 106).asString());
}
;
_.hashCode_0 = function hashCode_8(){
  return getHashCode_1(this.html);
}
;
_.toString_0 = function toString_23(){
  return 'safe: "' + this.html + '"';
}
;
var Lcom_google_gwt_dom_client_UnsafeHtmlBuilder$SafeHtmlString_2_classLit = createForClass('com.google.gwt.dom.client', 'UnsafeHtmlBuilder/SafeHtmlString', 121, Ljava_lang_Object_2_classLit);
function canSet(array, value_0){
  var elementTypeCategory;
  switch (getElementTypeCategory(array)) {
    case 6:
      return instanceOfString(value_0);
    case 7:
      return instanceOfDouble(value_0);
    case 8:
      return instanceOfBoolean(value_0);
    case 3:
      return Array.isArray(value_0) && (elementTypeCategory = getElementTypeCategory(value_0) , !(elementTypeCategory >= 14 && elementTypeCategory <= 16));
    case 11:
      return value_0 != null && typeof value_0 === 'function';
    case 12:
      return value_0 != null && typeof value_0 === 'object';
    case 0:
      return canCast(value_0, array.__elementTypeId$);
    case 2:
      return isJsObjectOrFunction(value_0) && !(value_0.typeMarker === typeMarkerFn);
    case 1:
      return isJsObjectOrFunction(value_0) && !(value_0.typeMarker === typeMarkerFn) || canCast(value_0, array.__elementTypeId$);
    default:return true;
  }
}

function getClassLiteralForArray(clazz, dimensions){
  return getClassLiteralForArray_0(clazz, dimensions);
}

function getElementTypeCategory(array){
  return array.__elementTypeCategory$ == null?10:array.__elementTypeCategory$;
}

function initUnidimensionalArray(leafClassLiteral, castableTypeMap, elementTypeId, length_0, elementTypeCategory, dimensions){
  var result_0;
  result_0 = initializeArrayElementsWithDefaults(elementTypeCategory, length_0);
  elementTypeCategory != 10 && stampJavaTypeInfo(getClassLiteralForArray(leafClassLiteral, dimensions), castableTypeMap, elementTypeId, elementTypeCategory, result_0);
  return result_0;
}

function initializeArrayElementsWithDefaults(elementTypeCategory, length_0){
  var array = new Array(length_0);
  var initValue;
  switch (elementTypeCategory) {
    case 14:
    case 15:
      initValue = 0;
      break;
    case 16:
      initValue = false;
      break;
    default:return array;
  }
  for (var i = 0; i < length_0; ++i) {
    array[i] = initValue;
  }
  return array;
}

function isJavaArray(src_0){
  return Array.isArray(src_0) && src_0.typeMarker === typeMarkerFn;
}

function setCheck(array, index_0, value_0){
  $clinit_InternalPreconditions();
  checkCriticalArrayType(value_0 == null || canSet(array, value_0));
  return array[index_0] = value_0;
}

function stampJavaTypeInfo(arrayClass, castableTypeMap, elementTypeId, elementTypeCategory, array){
  array.___clazz = arrayClass;
  array.castableTypeMap = castableTypeMap;
  array.typeMarker = typeMarkerFn;
  array.__elementTypeId$ = elementTypeId;
  array.__elementTypeCategory$ = elementTypeCategory;
  return array;
}

function stampJavaTypeInfo_0(array, referenceType){
  getElementTypeCategory(referenceType) != 10 && stampJavaTypeInfo(getClass__Ljava_lang_Class___devirtual$(referenceType), referenceType.castableTypeMap, referenceType.__elementTypeId$, getElementTypeCategory(referenceType), array);
  return array;
}

function create(value_0){
  var a0, a1, a2;
  a0 = value_0 & $intern_14;
  a1 = value_0 >> 22 & $intern_14;
  a2 = value_0 < 0?$intern_15:0;
  return create0(a0, a1, a2);
}

function create_0(a){
  return create0(a.l, a.m, a.h);
}

function create0(l, m, h){
  return {'l':l, 'm':m, 'h':h};
}

function divMod(a, b, computeRemainder){
  var aIsCopy, aIsMinValue, aIsNegative, bpower, c, negative;
  if (b.l == 0 && b.m == 0 && b.h == 0) {
    throw toJs(new ArithmeticException);
  }
  if (a.l == 0 && a.m == 0 && a.h == 0) {
    computeRemainder && (remainder = create0(0, 0, 0));
    return create0(0, 0, 0);
  }
  if (b.h == $intern_16 && b.m == 0 && b.l == 0) {
    return divModByMinValue(a, computeRemainder);
  }
  negative = false;
  if (b.h >> 19 != 0) {
    b = neg(b);
    negative = !negative;
  }
  bpower = powerOfTwo(b);
  aIsNegative = false;
  aIsMinValue = false;
  aIsCopy = false;
  if (a.h == $intern_16 && a.m == 0 && a.l == 0) {
    aIsMinValue = true;
    aIsNegative = true;
    if (bpower == -1) {
      a = create_0(($clinit_BigLongLib$Const() , MAX_VALUE));
      aIsCopy = true;
      negative = !negative;
    }
     else {
      c = shr(a, bpower);
      negative && negate_8(c);
      computeRemainder && (remainder = create0(0, 0, 0));
      return c;
    }
  }
   else if (a.h >> 19 != 0) {
    aIsNegative = true;
    a = neg(a);
    aIsCopy = true;
    negative = !negative;
  }
  if (bpower != -1) {
    return divModByShift(a, bpower, negative, aIsNegative, computeRemainder);
  }
  if (compare_0(a, b) < 0) {
    computeRemainder && (aIsNegative?(remainder = neg(a)):(remainder = create0(a.l, a.m, a.h)));
    return create0(0, 0, 0);
  }
  return divModHelper(aIsCopy?a:create0(a.l, a.m, a.h), b, negative, aIsNegative, aIsMinValue, computeRemainder);
}

function divModByMinValue(a, computeRemainder){
  if (a.h == $intern_16 && a.m == 0 && a.l == 0) {
    computeRemainder && (remainder = create0(0, 0, 0));
    return create_0(($clinit_BigLongLib$Const() , ONE));
  }
  computeRemainder && (remainder = create0(a.l, a.m, a.h));
  return create0(0, 0, 0);
}

function divModByShift(a, bpower, negative, aIsNegative, computeRemainder){
  var c;
  c = shr(a, bpower);
  negative && negate_8(c);
  if (computeRemainder) {
    a = maskRight(a, bpower);
    aIsNegative?(remainder = neg(a)):(remainder = create0(a.l, a.m, a.h));
  }
  return c;
}

function divModHelper(a, b, negative, aIsNegative, aIsMinValue, computeRemainder){
  var bshift, gte, quotient, shift_0, a1, a2, a0;
  shift_0 = numberOfLeadingZeros(b) - numberOfLeadingZeros(a);
  bshift = shl(b, shift_0);
  quotient = create0(0, 0, 0);
  while (shift_0 >= 0) {
    gte = trialSubtract(a, bshift);
    if (gte) {
      shift_0 < 22?(quotient.l |= 1 << shift_0 , undefined):shift_0 < 44?(quotient.m |= 1 << shift_0 - 22 , undefined):(quotient.h |= 1 << shift_0 - 44 , undefined);
      if (a.l == 0 && a.m == 0 && a.h == 0) {
        break;
      }
    }
    a1 = bshift.m;
    a2 = bshift.h;
    a0 = bshift.l;
    bshift.h = a2 >>> 1;
    bshift.m = a1 >>> 1 | (a2 & 1) << 21;
    bshift.l = a0 >>> 1 | (a1 & 1) << 21;
    --shift_0;
  }
  negative && negate_8(quotient);
  if (computeRemainder) {
    if (aIsNegative) {
      remainder = neg(a);
      aIsMinValue && (remainder = sub_0(remainder, ($clinit_BigLongLib$Const() , ONE)));
    }
     else {
      remainder = create0(a.l, a.m, a.h);
    }
  }
  return quotient;
}

function maskRight(a, bits){
  var b0, b1, b2;
  if (bits <= 22) {
    b0 = a.l & (1 << bits) - 1;
    b1 = b2 = 0;
  }
   else if (bits <= 44) {
    b0 = a.l;
    b1 = a.m & (1 << bits - 22) - 1;
    b2 = 0;
  }
   else {
    b0 = a.l;
    b1 = a.m;
    b2 = a.h & (1 << bits - 44) - 1;
  }
  return create0(b0, b1, b2);
}

function negate_8(a){
  var neg0, neg1, neg2;
  neg0 = ~a.l + 1 & $intern_14;
  neg1 = ~a.m + (neg0 == 0?1:0) & $intern_14;
  neg2 = ~a.h + (neg0 == 0 && neg1 == 0?1:0) & $intern_15;
  a.l = neg0;
  a.m = neg1;
  a.h = neg2;
}

function numberOfLeadingZeros(a){
  var b1, b2;
  b2 = numberOfLeadingZeros_0(a.h);
  if (b2 == 32) {
    b1 = numberOfLeadingZeros_0(a.m);
    return b1 == 32?numberOfLeadingZeros_0(a.l) + 32:b1 + 20 - 10;
  }
   else {
    return b2 - 12;
  }
}

function powerOfTwo(a){
  var h, l, m;
  l = a.l;
  if ((l & l - 1) != 0) {
    return -1;
  }
  m = a.m;
  if ((m & m - 1) != 0) {
    return -1;
  }
  h = a.h;
  if ((h & h - 1) != 0) {
    return -1;
  }
  if (h == 0 && m == 0 && l == 0) {
    return -1;
  }
  if (h == 0 && m == 0 && l != 0) {
    return numberOfTrailingZeros(l);
  }
  if (h == 0 && m != 0 && l == 0) {
    return numberOfTrailingZeros(m) + 22;
  }
  if (h != 0 && m == 0 && l == 0) {
    return numberOfTrailingZeros(h) + 44;
  }
  return -1;
}

function trialSubtract(a, b){
  var sum0, sum1, sum2;
  sum2 = a.h - b.h;
  if (sum2 < 0) {
    return false;
  }
  sum0 = a.l - b.l;
  sum1 = a.m - b.m + (sum0 >> 22);
  sum2 += sum1 >> 22;
  if (sum2 < 0) {
    return false;
  }
  a.l = sum0 & $intern_14;
  a.m = sum1 & $intern_14;
  a.h = sum2 & $intern_15;
  return true;
}

var remainder;
function add_1(a, b){
  var sum0, sum1, sum2;
  sum0 = a.l + b.l;
  sum1 = a.m + b.m + (sum0 >> 22);
  sum2 = a.h + b.h + (sum1 >> 22);
  return create0(sum0 & $intern_14, sum1 & $intern_14, sum2 & $intern_15);
}

function compare_0(a, b){
  var a0, a1, a2, b0, b1, b2, signA, signB;
  signA = a.h >> 19;
  signB = b.h >> 19;
  if (signA != signB) {
    return signB - signA;
  }
  a2 = a.h;
  b2 = b.h;
  if (a2 != b2) {
    return a2 - b2;
  }
  a1 = a.m;
  b1 = b.m;
  if (a1 != b1) {
    return a1 - b1;
  }
  a0 = a.l;
  b0 = b.l;
  return a0 - b0;
}

function fromDouble(value_0){
  var a0, a1, a2, negative, result_0;
  if (isNaN(value_0)) {
    return $clinit_BigLongLib$Const() , ZERO;
  }
  if (value_0 < -9223372036854775808) {
    return $clinit_BigLongLib$Const() , MIN_VALUE;
  }
  if (value_0 >= 9223372036854775807) {
    return $clinit_BigLongLib$Const() , MAX_VALUE;
  }
  negative = false;
  if (value_0 < 0) {
    negative = true;
    value_0 = -value_0;
  }
  a2 = 0;
  if (value_0 >= $intern_17) {
    a2 = round_int(value_0 / $intern_17);
    value_0 -= a2 * $intern_17;
  }
  a1 = 0;
  if (value_0 >= $intern_18) {
    a1 = round_int(value_0 / $intern_18);
    value_0 -= a1 * $intern_18;
  }
  a0 = round_int(value_0);
  result_0 = create0(a0, a1, a2);
  negative && negate_8(result_0);
  return result_0;
}

function neg(a){
  var neg0, neg1, neg2;
  neg0 = ~a.l + 1 & $intern_14;
  neg1 = ~a.m + (neg0 == 0?1:0) & $intern_14;
  neg2 = ~a.h + (neg0 == 0 && neg1 == 0?1:0) & $intern_15;
  return create0(neg0, neg1, neg2);
}

function shl(a, n){
  var res0, res1, res2;
  n &= 63;
  if (n < 22) {
    res0 = a.l << n;
    res1 = a.m << n | a.l >> 22 - n;
    res2 = a.h << n | a.m >> 22 - n;
  }
   else if (n < 44) {
    res0 = 0;
    res1 = a.l << n - 22;
    res2 = a.m << n - 22 | a.l >> 44 - n;
  }
   else {
    res0 = 0;
    res1 = 0;
    res2 = a.l << n - 44;
  }
  return create0(res0 & $intern_14, res1 & $intern_14, res2 & $intern_15);
}

function shr(a, n){
  var a2, negative, res0, res1, res2;
  n &= 63;
  a2 = a.h;
  negative = (a2 & $intern_16) != 0;
  negative && (a2 |= -1048576);
  if (n < 22) {
    res2 = a2 >> n;
    res1 = a.m >> n | a2 << 22 - n;
    res0 = a.l >> n | a.m << 22 - n;
  }
   else if (n < 44) {
    res2 = negative?$intern_15:0;
    res1 = a2 >> n - 22;
    res0 = a.m >> n - 22 | a2 << 44 - n;
  }
   else {
    res2 = negative?$intern_15:0;
    res1 = negative?$intern_14:0;
    res0 = a2 >> n - 44;
  }
  return create0(res0 & $intern_14, res1 & $intern_14, res2 & $intern_15);
}

function sub_0(a, b){
  var sum0, sum1, sum2;
  sum0 = a.l - b.l;
  sum1 = a.m - b.m + (sum0 >> 22);
  sum2 = a.h - b.h + (sum1 >> 22);
  return create0(sum0 & $intern_14, sum1 & $intern_14, sum2 & $intern_15);
}

function toInt(a){
  return a.l | a.m << 22;
}

function toString_24(a){
  var digits, rem, res, tenPowerLong, zeroesNeeded;
  if (a.l == 0 && a.m == 0 && a.h == 0) {
    return '0';
  }
  if (a.h == $intern_16 && a.m == 0 && a.l == 0) {
    return '-9223372036854775808';
  }
  if (a.h >> 19 != 0) {
    return '-' + toString_24(neg(a));
  }
  rem = a;
  res = '';
  while (!(rem.l == 0 && rem.m == 0 && rem.h == 0)) {
    tenPowerLong = create(1000000000);
    rem = divMod(rem, tenPowerLong, true);
    digits = '' + toInt(remainder);
    if (!(rem.l == 0 && rem.m == 0 && rem.h == 0)) {
      zeroesNeeded = 9 - digits.length;
      for (; zeroesNeeded > 0; zeroesNeeded--) {
        digits = '0' + digits;
      }
    }
    res = digits + res;
  }
  return res;
}

function $clinit_BigLongLib$Const(){
  $clinit_BigLongLib$Const = emptyMethod;
  MAX_VALUE = create0($intern_14, $intern_14, 524287);
  MIN_VALUE = create0(0, 0, $intern_16);
  ONE = create(1);
  create(2);
  ZERO = create(0);
}

var MAX_VALUE, MIN_VALUE, ONE, ZERO;
function toJava(e){
  var javaException;
  if (instanceOf(e, 10)) {
    return e;
  }
  javaException = e && e.__java$exception;
  if (!javaException) {
    javaException = new JavaScriptException(e);
    captureStackTrace(javaException);
  }
  return javaException;
}

function toJs(t){
  return t.backingJsObject;
}

function add_2(a, b){
  var result_0;
  if (isSmallLong0(a) && isSmallLong0(b)) {
    result_0 = a + b;
    if ($intern_19 < result_0 && result_0 < $intern_17) {
      return result_0;
    }
  }
  return createLongEmul(add_1(isSmallLong0(a)?toBigLong(a):a, isSmallLong0(b)?toBigLong(b):b));
}

function compare_1(a, b){
  var result_0;
  if (isSmallLong0(a) && isSmallLong0(b)) {
    result_0 = a - b;
    if (!isNaN(result_0)) {
      return result_0;
    }
  }
  return compare_0(isSmallLong0(a)?toBigLong(a):a, isSmallLong0(b)?toBigLong(b):b);
}

function createLongEmul(big_0){
  var a2;
  a2 = big_0.h;
  if (a2 == 0) {
    return big_0.l + big_0.m * $intern_18;
  }
  if (a2 == $intern_15) {
    return big_0.l + big_0.m * $intern_18 - $intern_17;
  }
  return big_0;
}

function eq(a, b){
  return compare_1(a, b) == 0;
}

function fromDouble_0(value_0){
  if ($intern_19 < value_0 && value_0 < $intern_17) {
    return value_0 < 0?$wnd.Math.ceil(value_0):$wnd.Math.floor(value_0);
  }
  return createLongEmul(fromDouble(value_0));
}

function isSmallLong0(value_0){
  return typeof value_0 === 'number';
}

function toBigLong(longValue){
  var a0, a1, a3, value_0;
  value_0 = longValue;
  a3 = 0;
  if (value_0 < 0) {
    value_0 += $intern_17;
    a3 = $intern_15;
  }
  a1 = round_int(value_0 / $intern_18);
  a0 = round_int(value_0 - a1 * $intern_18);
  return create0(a0, a1, a3);
}

function toInt_0(a){
  if (isSmallLong0(a)) {
    return a | 0;
  }
  return toInt(a);
}

function toString_25(a){
  if (isSmallLong0(a)) {
    return '' + a;
  }
  return toString_24(a);
}

function init(){
  (new UserAgentAsserter).onModuleLoad();
  (new DocumentModeAsserter).onModuleLoad();
  (new LogConfiguration).onModuleLoad();
}

function $clinit_LogConfiguration(){
  $clinit_LogConfiguration = emptyMethod;
  impl_1 = new LogConfiguration$LogConfigurationImplNull;
}

function LogConfiguration(){
  $clinit_LogConfiguration();
}

defineClass(198, 1, {}, LogConfiguration);
_.onModuleLoad = function onModuleLoad(){
  var log_0;
  impl_1.configureClientSideLogging();
  if (impl_1.loggingIsEnabled()) {
    if (!uncaughtExceptionHandler) {
      log_0 = getLogger_0(($ensureNamesAreInitialized(Lcom_google_gwt_logging_client_LogConfiguration_2_classLit) , Lcom_google_gwt_logging_client_LogConfiguration_2_classLit.typeName));
      setUncaughtExceptionHandler(new LogConfiguration$1(log_0));
    }
  }
}
;
var impl_1;
var Lcom_google_gwt_logging_client_LogConfiguration_2_classLit = createForClass('com.google.gwt.logging.client', 'LogConfiguration', 198, Ljava_lang_Object_2_classLit);
function LogConfiguration$1(val$log){
  this.val$log2 = val$log;
}

defineClass(207, 1, {}, LogConfiguration$1);
_.onUncaughtException = function onUncaughtException(e){
  $log_1(this.val$log2, ($clinit_Level() , SEVERE), e.getMessage(), e);
}
;
var Lcom_google_gwt_logging_client_LogConfiguration$1_2_classLit = createForAnonymousClass('com.google.gwt.logging.client', 207, Ljava_lang_Object_2_classLit);
function LogConfiguration$LogConfigurationImplNull(){
}

defineClass(206, 1, {}, LogConfiguration$LogConfigurationImplNull);
_.configureClientSideLogging = function configureClientSideLogging(){
}
;
_.loggingIsEnabled = function loggingIsEnabled(){
  return false;
}
;
var Lcom_google_gwt_logging_client_LogConfiguration$LogConfigurationImplNull_2_classLit = createForClass('com.google.gwt.logging.client', 'LogConfiguration/LogConfigurationImplNull', 206, Ljava_lang_Object_2_classLit);
function $exec(this$static, input_0){
  return this$static.exec(input_0);
}

function $replace(this$static, input_0, replacement){
  return input_0.replace(this$static, replacement);
}

function $test(this$static, input_0){
  return this$static.test(input_0);
}

function SafeHtmlString(){
  this.html = '';
}

defineClass(407, 1, $intern_13, SafeHtmlString);
_.asString = function asString_0(){
  return this.html;
}
;
_.equals_0 = function equals_10(obj){
  if (!instanceOf(obj, 106)) {
    return false;
  }
  return $equals_0(this.html, castTo(obj, 106).asString());
}
;
_.hashCode_0 = function hashCode_9(){
  return getHashCode_1(this.html);
}
;
_.toString_0 = function toString_27(){
  return 'safe: "' + this.html + '"';
}
;
var Lcom_google_gwt_safehtml_shared_SafeHtmlString_2_classLit = createForClass('com.google.gwt.safehtml.shared', 'SafeHtmlString', 407, Ljava_lang_Object_2_classLit);
function $clinit_SafeHtmlUtils(){
  $clinit_SafeHtmlUtils = emptyMethod;
  new SafeHtmlString;
  HTML_CHARS_RE = new RegExp('[&<>\'"]');
  AMP_RE = new RegExp('&', 'g');
  GT_RE = new RegExp('>', 'g');
  LT_RE = new RegExp('<', 'g');
  SQUOT_RE = new RegExp("'", 'g');
  QUOT_RE = new RegExp('"', 'g');
}

function htmlEscape(s){
  $clinit_SafeHtmlUtils();
  if (!$test(HTML_CHARS_RE, s)) {
    return s;
  }
  s.indexOf('&') != -1 && (s = $replace(AMP_RE, s, '&amp;'));
  s.indexOf('<') != -1 && (s = $replace(LT_RE, s, '&lt;'));
  s.indexOf('>') != -1 && (s = $replace(GT_RE, s, '&gt;'));
  s.indexOf('"') != -1 && (s = $replace(QUOT_RE, s, '&quot;'));
  s.indexOf("'") != -1 && (s = $replace(SQUOT_RE, s, '&#39;'));
  return s;
}

function htmlEscapeNoQuotes(s){
  $clinit_SafeHtmlUtils();
  if (!$test(HTML_CHARS_RE, s)) {
    return s;
  }
  s.indexOf('&') != -1 && (s = $replace(AMP_RE, s, '&amp;'));
  s.indexOf('<') != -1 && (s = $replace(LT_RE, s, '&lt;'));
  s.indexOf('>') != -1 && (s = $replace(GT_RE, s, '&gt;'));
  return s;
}

var AMP_RE, GT_RE, HTML_CHARS_RE, LT_RE, QUOT_RE, SQUOT_RE;
function $clinit_DOM(){
  $clinit_DOM = emptyMethod;
  impl_2 = new DOMImplWebkit_0;
  recentDispatches = new ArrayList;
}

function dispatchEvent_0(evt, elem, listener){
  $clinit_DOM();
  var prevCurrentEvent;
  prevCurrentEvent = currentEvent;
  currentEvent = evt;
  dispatchEventImpl(evt, elem, listener);
  currentEvent = prevCurrentEvent;
}

function dispatchEventImpl(event_0, elem, listener){
  var childElement, dispatchInfo, entry, entry$iterator, eventTarget, first, forDispatch, lcType, message;
  elem == sCaptureElem && $eventGetTypeInt($eventGetType_0(($clinit_DOMImpl() , impl_0), event_0)) == 8192 && (sCaptureElem = null);
  eventTarget = $eventGetTarget(($clinit_DOMImpl() , impl_0), event_0);
  lcType = $eventGetType_0(impl_0, event_0).toLowerCase();
  $eventGetTypeInt(lcType);
  if (recentDispatches.stream().anyMatch(new DOM$lambda$0$Type(event_0, listener))) {
    return;
  }
  dispatchInfo = null;
  first = recentDispatches.stream().filter_0(new DOM$lambda$1$Type(event_0)).findFirst();
  if (first.ref != null) {
    dispatchInfo = (checkCriticalElement(first.ref != null) , castTo(first.ref, 81));
  }
   else {
    dispatchInfo = new DOM$DispatchInfo(event_0);
    recentDispatches.add_0(dispatchInfo);
    recentDispatches.size_1() > 10 && recentDispatches.remove_3(0);
  }
  if (!!eventTarget && $is(eventTarget, Lcom_google_gwt_dom_client_Element_2_classLit)) {
    childElement = eventTarget?castTo($cast(eventTarget), 5):throwClassCastExceptionUnlessNull(eventTarget);
    forDispatch = new LinkedHashMap;
    while (childElement != elem && !!childElement) {
      childElement = childElement.getParentElement();
    }
    for (entry$iterator = forDispatch.entrySet().iterator(); entry$iterator.hasNext_0();) {
      entry = castTo(entry$iterator.next_1(), 16);
      eventCurrentTarget = castTo(entry.getKey(), 5);
      throwClassCastExceptionUnlessNull(entry.getValue());
      if (lcType.indexOf('click') != -1 || lcType.indexOf('mousedown') != -1) {
        message = format_0('dispatch mouse event - %s - %s', stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [lcType, null.$_nullMethod().$_nullMethod()]));
        log_1(($clinit_LocalDomDebug() , DOM_MOUSE_EVENT), message, stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, []));
      }
      null.$_nullMethod();
      $clinit_LocalDom();
      if ($isStopPropagation0(instance, event_0)) {
        return;
      }
    }
    $willDispatchTo(dispatchInfo, eventTarget?castTo($cast(eventTarget), 5):throwClassCastExceptionUnlessNull(eventTarget));
  }
  if (is($eventGetCurrentTarget(impl_0, event_0))) {
    eventCurrentTarget = castTo($cast($eventGetCurrentTarget(impl_0, event_0)), 5);
    $willDispatchTo(dispatchInfo, eventCurrentTarget);
  }
   else {
    eventCurrentTarget = null;
  }
  null.$_nullMethod();
}

function lambda$0_1(event_0, listener_1, di_2){
  $clinit_DOM();
  return di_2.event_0 == event_0 && di_2.dispatchedToListeners.contains(listener_1);
}

function lambda$1_0(event_0, di_1){
  $clinit_DOM();
  return di_1.event_0 == event_0;
}

function sinkEvents_2(elem, eventBits){
  $clinit_DOM();
  var attachToAncestor, attachedAncestor;
  if ($remote((new Element$ElementImplAccess(elem)).this$01) != ($clinit_ElementNull() , INSTANCE_2)) {
    impl_2.sinkEvents_0(elem, eventBits);
  }
   else {
    attachedAncestor = castTo($provideSelfOrAncestorLinkedToRemote((new Element$ElementImplAccess(elem)).this$01), 5);
    attachToAncestor = !!attachedAncestor && attachedAncestor != elem;
    attachToAncestor && impl_2.sinkEvents_0(attachedAncestor, $getEventsSunk(impl_2, attachedAncestor) | eventBits);
    attachedAncestor != elem && ($sinkEvents(elem.local, eventBits) , sinkEvents_I_V__devirtual$($remote(elem), eventBits));
  }
}

var currentEvent = null, eventCurrentTarget, impl_2, recentDispatches, sCaptureElem;
function $willDispatchTo(this$static, childElement){
  this$static.dispatchedToElements.add_0(childElement);
}

function DOM$DispatchInfo(event_0){
  this.dispatchedToListeners = new ArrayList;
  this.dispatchedToElements = new ArrayList;
  this.event_0 = event_0;
}

defineClass(81, 1, {81:1}, DOM$DispatchInfo);
var Lcom_google_gwt_user_client_DOM$DispatchInfo_2_classLit = createForClass('com.google.gwt.user.client', 'DOM/DispatchInfo', 81, Ljava_lang_Object_2_classLit);
function DOM$lambda$0$Type(event_0, listener_1){
  this.event_0 = event_0;
  this.listener_1 = listener_1;
}

defineClass(355, 1, {}, DOM$lambda$0$Type);
_.negate = function negate_9(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_9(arg0){
  return lambda$0_1(this.event_0, this.listener_1, castTo(arg0, 81));
}
;
var Lcom_google_gwt_user_client_DOM$lambda$0$Type_2_classLit = createForAnonymousClass('com.google.gwt.user.client', 355, Ljava_lang_Object_2_classLit);
function DOM$lambda$1$Type(event_0){
  this.event_0 = event_0;
}

defineClass(356, 1, {}, DOM$lambda$1$Type);
_.negate = function negate_10(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_10(arg0){
  return lambda$1_0(this.event_0, castTo(arg0, 81));
}
;
var Lcom_google_gwt_user_client_DOM$lambda$1$Type_2_classLit = createForAnonymousClass('com.google.gwt.user.client', 356, Ljava_lang_Object_2_classLit);
function DocumentModeAsserter(){
}

defineClass(197, 1, {}, DocumentModeAsserter);
_.onModuleLoad = function onModuleLoad_0(){
  var allowedModes, currentMode, i, impl, message, severity;
  impl = new DocumentModeAsserter_DocumentModeProperty;
  severity = impl.getDocumentModeSeverity();
  if (severity == 1) {
    return;
  }
  currentMode = get_7().remote.compatMode;
  allowedModes = impl.getAllowedDocumentModes();
  for (i = 0; i < allowedModes.length; i++) {
    if ($equals_0(allowedModes[i], currentMode)) {
      return;
    }
  }
  allowedModes.length == 1 && $equals_0('CSS1Compat', allowedModes[0]) && $equals_0('BackCompat', currentMode)?(message = "GWT no longer supports Quirks Mode (document.compatMode=' BackCompat').<br>Make sure your application's host HTML page has a Standards Mode (document.compatMode=' CSS1Compat') doctype,<br>e.g. by using &lt;!doctype html&gt; at the start of your application's HTML page.<br><br>To continue using this unsupported rendering mode and risk layout problems, suppress this message by adding<br>the following line to your*.gwt.xml module file:<br>&nbsp;&nbsp;&lt;extend-configuration-property name=\"document.compatMode\" value=\"" + currentMode + '"/&gt;'):(message = "Your *.gwt.xml module configuration prohibits the use of the current document rendering mode (document.compatMode=' " + currentMode + "').<br>Modify your application's host HTML page doctype, or update your custom " + "'document.compatMode' configuration property settings.");
  if (severity == 0) {
    throw toJs(new RuntimeException_0(message));
  }
  $clinit_GWT();
}
;
var Lcom_google_gwt_user_client_DocumentModeAsserter_2_classLit = createForClass('com.google.gwt.user.client', 'DocumentModeAsserter', 197, Ljava_lang_Object_2_classLit);
function DocumentModeAsserter_DocumentModeProperty(){
}

defineClass(204, 1, {}, DocumentModeAsserter_DocumentModeProperty);
_.getAllowedDocumentModes = function getAllowedDocumentModes(){
  return stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_String_2_classLit, 1), $intern_0, 2, 6, ['CSS1Compat']);
}
;
_.getDocumentModeSeverity = function getDocumentModeSeverity(){
  return 2;
}
;
var Lcom_google_gwt_user_client_DocumentModeAsserter_1DocumentModeProperty_2_classLit = createForClass('com.google.gwt.user.client', 'DocumentModeAsserter_DocumentModeProperty', 204, Ljava_lang_Object_2_classLit);
function $clinit_LocalDomDebug(){
  $clinit_LocalDomDebug = emptyMethod;
  DOM_MOUSE_EVENT = new LocalDomDebug('DOM_MOUSE_EVENT', 0);
  DOM_EVENT = new LocalDomDebug('DOM_EVENT', 1);
  PUT_DOM_IMPL = new LocalDomDebug('PUT_DOM_IMPL', 2);
  ENSURE_JSO = new LocalDomDebug('ENSURE_JSO', 3);
  DUMP_LOCAL = new LocalDomDebug('DUMP_LOCAL', 4);
  CREATED_PENDING_RESOLUTION = new LocalDomDebug('CREATED_PENDING_RESOLUTION', 5);
  DETACH_remote = new LocalDomDebug('DETACH_remote', 6);
  EVENT_MOD = new LocalDomDebug('EVENT_MOD', 7);
  RESOLVE = new LocalDomDebug('RESOLVE', 8);
  ENSURE_FLUSH = new LocalDomDebug('ENSURE_FLUSH', 9);
  DUPLICATE_ELT_ID = new LocalDomDebug('DUPLICATE_ELT_ID', 10);
  DISPATCH_DETAILS = new LocalDomDebug('DISPATCH_DETAILS', 11);
  STYLE = new LocalDomDebug('STYLE', 12);
  REQUIRES_SYNC = new LocalDomDebug('REQUIRES_SYNC', 13);
  DEBUG_ISSUE = new LocalDomDebug('DEBUG_ISSUE', 14);
}

function LocalDomDebug(enum$name, enum$ordinal){
  Enum.call(this, enum$name, enum$ordinal);
}

function values_4(){
  $clinit_LocalDomDebug();
  return stampJavaTypeInfo(getClassLiteralForArray(Lcom_google_gwt_user_client_LocalDomDebug_2_classLit, 1), $intern_0, 28, 0, [DOM_MOUSE_EVENT, DOM_EVENT, PUT_DOM_IMPL, ENSURE_JSO, DUMP_LOCAL, CREATED_PENDING_RESOLUTION, DETACH_remote, EVENT_MOD, RESOLVE, ENSURE_FLUSH, DUPLICATE_ELT_ID, DISPATCH_DETAILS, STYLE, REQUIRES_SYNC, DEBUG_ISSUE]);
}

defineClass(28, 33, {28:1, 3:1, 32:1, 33:1}, LocalDomDebug);
var CREATED_PENDING_RESOLUTION, DEBUG_ISSUE, DETACH_remote, DISPATCH_DETAILS, DOM_EVENT, DOM_MOUSE_EVENT, DUMP_LOCAL, DUPLICATE_ELT_ID, ENSURE_FLUSH, ENSURE_JSO, EVENT_MOD, PUT_DOM_IMPL, REQUIRES_SYNC, RESOLVE, STYLE;
var Lcom_google_gwt_user_client_LocalDomDebug_2_classLit = createForEnum('com.google.gwt.user.client', 'LocalDomDebug', 28, Ljava_lang_Enum_2_classLit, values_4);
function getUserAgent(){
  try {
    return $wnd.navigator.userAgent;
  }
   catch (e) {
    return 'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2;';
  }
}

function $eventGetTypeInt(eventType){
  switch (eventType) {
    case 'blur':
      return 4096;
    case 'change':
      return 1024;
    case 'click':
      return 1;
    case 'dblclick':
      return 2;
    case 'focus':
      return 2048;
    case 'keydown':
      return 128;
    case 'keypress':
      return 256;
    case 'keyup':
      return 512;
    case 'load':
      return 32768;
    case 'losecapture':
      return 8192;
    case 'mousedown':
      return 4;
    case 'mousemove':
      return 64;
    case 'mouseout':
      return 32;
    case 'mouseover':
      return 16;
    case 'mouseup':
      return 8;
    case 'scroll':
      return $intern_20;
    case 'error':
      return $intern_10;
    case 'DOMMouseScroll':
    case 'mousewheel':
      return 131072;
    case 'contextmenu':
      return 262144;
    case 'paste':
      return $intern_16;
    case 'touchstart':
      return 1048576;
    case 'touchmove':
      return 2097152;
    case 'touchend':
      return $intern_18;
    case 'touchcancel':
      return 8388608;
    case 'gesturestart':
      return $intern_21;
    case 'gesturechange':
      return $intern_22;
    case 'gestureend':
      return $intern_23;
    default:return -1;
  }
}

function $getEventsSunk(this$static, elem){
  var remote;
  if ($remote((new Element$ElementImplAccess(elem)).this$01) != ($clinit_ElementNull() , INSTANCE_2)) {
    remote = castToJso($remote((new Element$ElementImplAccess(elem)).this$01));
    return remote.__eventBits || 0;
  }
   else {
    return elem.local.eventBits;
  }
}

function $maybeInitializeEventSystem(this$static){
  if (!eventSystemIsInitialized) {
    this$static.initEventSystem();
    eventSystemIsInitialized = true;
  }
}

function getEventListener(elem){
  var maybeListener;
  $remote((new Element$ElementImplAccess(elem)).this$01) != ($clinit_ElementNull() , INSTANCE_2) && (maybeListener = castToJso($remote((new Element$ElementImplAccess(elem)).this$01)).__listener , null);
  return elem.uiObjectListener;
}

defineClass(453, 1, {});
var eventSystemIsInitialized = false;
var Lcom_google_gwt_user_client_impl_DOMImpl_2_classLit = createForClass('com.google.gwt.user.client.impl', 'DOMImpl', 453, Ljava_lang_Object_2_classLit);
function $clinit_DOMImplStandard(){
  $clinit_DOMImplStandard = emptyMethod;
  bitlessEventDispatchers = {'_default_':dispatchEvent_2, 'dragenter':dispatchDragEvent, 'dragover':dispatchDragEvent};
  captureEventDispatchers = getCaptureEventDispatchers0(($clinit_GWT() , false));
}

function $sinkEventsImpl(elem, bits){
  var chMask = (elem.__eventBits || 0) ^ bits;
  elem.__eventBits = bits;
  if (!chMask)
    return;
  chMask & 1 && (elem.onclick = bits & 1?dispatchEvent_1:null);
  chMask & 2 && (elem.ondblclick = bits & 2?dispatchEvent_1:null);
  chMask & 4 && (elem.onmousedown = bits & 4?dispatchEvent_1:null);
  chMask & 8 && (elem.onmouseup = bits & 8?dispatchEvent_1:null);
  chMask & 16 && (elem.onmouseover = bits & 16?dispatchEvent_1:null);
  chMask & 32 && (elem.onmouseout = bits & 32?dispatchEvent_1:null);
  chMask & 64 && (elem.onmousemove = bits & 64?dispatchEvent_1:null);
  chMask & 128 && (elem.onkeydown = bits & 128?dispatchEvent_1:null);
  chMask & 256 && (elem.onkeypress = bits & 256?dispatchEvent_1:null);
  chMask & 512 && (elem.onkeyup = bits & 512?dispatchEvent_1:null);
  chMask & 1024 && (elem.onchange = bits & 1024?dispatchEvent_1:null);
  chMask & 2048 && (elem.onfocus = bits & 2048?dispatchEvent_1:null);
  chMask & 4096 && (elem.onblur = bits & 4096?dispatchEvent_1:null);
  chMask & 8192 && (elem.onlosecapture = bits & 8192?dispatchEvent_1:null);
  chMask & $intern_20 && (elem.onscroll = bits & $intern_20?dispatchEvent_1:null);
  chMask & 32768 && (elem.onload = bits & 32768?dispatchUnhandledEvent:null);
  chMask & $intern_10 && (elem.onerror = bits & $intern_10?dispatchEvent_1:null);
  chMask & 131072 && (elem.onmousewheel = bits & 131072?dispatchEvent_1:null);
  chMask & 262144 && (elem.oncontextmenu = bits & 262144?dispatchEvent_1:null);
  chMask & $intern_16 && (elem.onpaste = bits & $intern_16?dispatchEvent_1:null);
  chMask & 1048576 && (elem.ontouchstart = bits & 1048576?dispatchEvent_1:null);
  chMask & 2097152 && (elem.ontouchmove = bits & 2097152?dispatchEvent_1:null);
  chMask & $intern_18 && (elem.ontouchend = bits & $intern_18?dispatchEvent_1:null);
  chMask & 8388608 && (elem.ontouchcancel = bits & 8388608?dispatchEvent_1:null);
  chMask & $intern_21 && (elem.ongesturestart = bits & $intern_21?dispatchEvent_1:null);
  chMask & $intern_22 && (elem.ongesturechange = bits & $intern_22?dispatchEvent_1:null);
  chMask & $intern_23 && (elem.ongestureend = bits & $intern_23?dispatchEvent_1:null);
}

function dispatchCapturedEvent(evt){
  $clinit_DOM();
}

function dispatchCapturedMouseEvent(evt){
  $clinit_DOM() , true , false;
  return;
}

function dispatchDragEvent(evt){
  $eventPreventDefault(($clinit_DOMImpl() , impl_0), evt);
  dispatchEvent_2(evt);
}

function dispatchEvent_2(evt){
  var element;
  element = getFirstAncestorWithListener(evt);
  if (!element) {
    return;
  }
  dispatchEvent_0(evt, element, getEventListener(element));
}

function dispatchUnhandledEvent_0(evt){
  var element;
  element = castTo($cast($eventGetCurrentTarget(($clinit_DOMImpl() , impl_0), evt)), 5);
  element.setPropertyString('__gwtLastUnhandledEvent', $eventGetType_0(impl_0, evt));
  dispatchEvent_2(evt);
}

function getCaptureEventDispatchers0(noMouseMove){
  function noop(){
  }

  return {'click':dispatchCapturedMouseEvent, 'dblclick':dispatchCapturedMouseEvent, 'mousedown':dispatchCapturedMouseEvent, 'mouseup':dispatchCapturedMouseEvent, 'mousemove':noMouseMove?noop:dispatchCapturedMouseEvent, 'mouseover':dispatchCapturedMouseEvent, 'mouseout':dispatchCapturedMouseEvent, 'mousewheel':dispatchCapturedMouseEvent, 'keydown':dispatchCapturedEvent, 'keyup':dispatchCapturedEvent, 'keypress':dispatchCapturedEvent, 'blur':dispatchCapturedEvent, 'focus':dispatchCapturedEvent, 'touchstart':dispatchCapturedMouseEvent, 'touchend':dispatchCapturedMouseEvent, 'touchmove':dispatchCapturedMouseEvent, 'touchcancel':dispatchCapturedMouseEvent, 'gesturestart':dispatchCapturedMouseEvent, 'gestureend':dispatchCapturedMouseEvent, 'gesturechange':dispatchCapturedMouseEvent};
}

function getFirstAncestorWithListener(evt){
  var curElem, parentNode;
  if (!is($eventGetCurrentTarget(($clinit_DOMImpl() , impl_0), evt))) {
    return null;
  }
  curElem = castTo($cast($eventGetCurrentTarget(impl_0, evt)), 5);
  while (!!curElem && (getEventListener(curElem) , true)) {
    parentNode = curElem.getParentNode();
    curElem = !parentNode?null:castTo(parentNode.cast(), 5);
  }
  return curElem;
}

defineClass(454, 453, {});
_.initEventSystem = function initEventSystem(){
  dispatchEvent_1 = $entry(dispatchEvent_2);
  dispatchUnhandledEvent = $entry(dispatchUnhandledEvent_0);
  var foreach = foreach_0;
  var bitlessEvents = bitlessEventDispatchers;
  foreach(bitlessEvents, function(e, fn){
    bitlessEvents[e] = $entry(fn);
  }
  );
  var captureEvents_0 = captureEventDispatchers;
  foreach(captureEvents_0, function(e, fn){
    captureEvents_0[e] = $entry(fn);
  }
  );
  foreach(captureEvents_0, function(e, fn){
    $wnd.addEventListener(e, fn, true);
  }
  );
}
;
_.sinkEvents_0 = function sinkEvents_3(elem, bits){
  $maybeInitializeEventSystem(this);
  $remote((new Element$ElementImplAccess(elem)).this$01) != ($clinit_ElementNull() , INSTANCE_2) && $sinkEventsImpl(castToJso($remote((new Element$ElementImplAccess(elem)).this$01)), bits);
}
;
var bitlessEventDispatchers, captureEventDispatchers, dispatchEvent_1, dispatchUnhandledEvent;
var Lcom_google_gwt_user_client_impl_DOMImplStandard_2_classLit = createForClass('com.google.gwt.user.client.impl', 'DOMImplStandard', 454, Lcom_google_gwt_user_client_impl_DOMImpl_2_classLit);
defineClass(455, 454, {});
var Lcom_google_gwt_user_client_impl_DOMImplStandardBase_2_classLit = createForClass('com.google.gwt.user.client.impl', 'DOMImplStandardBase', 455, Lcom_google_gwt_user_client_impl_DOMImplStandard_2_classLit);
function DOMImplWebkit_0(){
  $clinit_DOMImplStandard();
}

defineClass(369, 455, {}, DOMImplWebkit_0);
var Lcom_google_gwt_user_client_impl_DOMImplWebkit_2_classLit = createForClass('com.google.gwt.user.client.impl', 'DOMImplWebkit', 369, Lcom_google_gwt_user_client_impl_DOMImplStandardBase_2_classLit);
function foreach_0(map_0, fn){
  for (var e in map_0) {
    map_0.hasOwnProperty(e) && fn(e, map_0[e]);
  }
}

function setStyleName(elem, style){
  if (!elem) {
    throw toJs(new RuntimeException_0('Null widget handle. If you are creating a composite, ensure that initWidget() has been called.'));
  }
  style = $trim(style);
  if (style.length == 0) {
    throw toJs(new IllegalArgumentException_0('Style names cannot be empty'));
  }
  $removeClassName(elem, style);
}

function UserAgentAsserter(){
}

function assertCompileTimeUserAgent(){
  var compileTimeValue, impl, runtimeValue;
  impl = new UserAgentImplSafari;
  compileTimeValue = impl.getCompileTimeValue();
  runtimeValue = impl.getRuntimeValue();
  if (!$equals_0(compileTimeValue, runtimeValue)) {
    throw toJs(new UserAgentAsserter$UserAgentAssertionError(compileTimeValue, runtimeValue));
  }
}

defineClass(196, 1, {}, UserAgentAsserter);
_.onModuleLoad = function onModuleLoad_1(){
  $wnd.setTimeout($entry(assertCompileTimeUserAgent));
}
;
var Lcom_google_gwt_useragent_client_UserAgentAsserter_2_classLit = createForClass('com.google.gwt.useragent.client', 'UserAgentAsserter', 196, Ljava_lang_Object_2_classLit);
function Error_0(message, cause){
  $$init(this);
  this.cause = cause;
  this.detailMessage = message;
  $fillInStackTrace(this);
  this.initializeBackingError();
}

defineClass(55, 10, $intern_24);
var Ljava_lang_Error_2_classLit = createForClass('java.lang', 'Error', 55, Ljava_lang_Throwable_2_classLit);
defineClass(31, 55, $intern_24);
var Ljava_lang_AssertionError_2_classLit = createForClass('java.lang', 'AssertionError', 31, Ljava_lang_Error_2_classLit);
function UserAgentAsserter$UserAgentAssertionError(compileTimeValue, runtimeValue){
  Error_0.call(this, 'Possible problem with your *.gwt.xml module file.\nThe compile time user.agent value (' + compileTimeValue + ') ' + 'does not match the runtime user.agent value (' + runtimeValue + ').\n' + 'Expect more errors.' == null?'null':toString_26('Possible problem with your *.gwt.xml module file.\nThe compile time user.agent value (' + compileTimeValue + ') ' + 'does not match the runtime user.agent value (' + runtimeValue + ').\n' + 'Expect more errors.'), instanceOf('Possible problem with your *.gwt.xml module file.\nThe compile time user.agent value (' + compileTimeValue + ') ' + 'does not match the runtime user.agent value (' + runtimeValue + ').\n' + 'Expect more errors.', 10)?castTo('Possible problem with your *.gwt.xml module file.\nThe compile time user.agent value (' + compileTimeValue + ') ' + 'does not match the runtime user.agent value (' + runtimeValue + ').\n' + 'Expect more errors.', 10):null);
}

defineClass(201, 31, $intern_24, UserAgentAsserter$UserAgentAssertionError);
var Lcom_google_gwt_useragent_client_UserAgentAsserter$UserAgentAssertionError_2_classLit = createForClass('com.google.gwt.useragent.client', 'UserAgentAsserter/UserAgentAssertionError', 201, Ljava_lang_AssertionError_2_classLit);
function UserAgentImplSafari(){
}

defineClass(200, 1, {}, UserAgentImplSafari);
_.getCompileTimeValue = function getCompileTimeValue(){
  return 'safari';
}
;
_.getRuntimeValue = function getRuntimeValue(){
  var ua = navigator.userAgent.toLowerCase();
  var docMode = $doc.documentMode;
  if (function(){
    return ua.indexOf('webkit') != -1;
  }
  ())
    return 'safari';
  if (function(){
    return ua.indexOf('msie') != -1 && docMode >= 10 && docMode < 11;
  }
  ())
    return 'ie10';
  if (function(){
    return ua.indexOf('msie') != -1 && docMode >= 9 && docMode < 11;
  }
  ())
    return 'ie9';
  if (function(){
    return ua.indexOf('msie') != -1 && docMode >= 8 && docMode < 11;
  }
  ())
    return 'ie8';
  if (function(){
    return ua.indexOf('gecko') != -1 || docMode >= 11;
  }
  ())
    return 'gecko1_8';
  return 'unknown';
}
;
var Lcom_google_gwt_useragent_client_UserAgentImplSafari_2_classLit = createForClass('com.google.gwt.useragent.client', 'UserAgentImplSafari', 200, Ljava_lang_Object_2_classLit);
defineClass(461, 1, $intern_25);
_.toString_0 = function toString_28(){
  return $getName(this.___clazz) + '(' + this.getName() + ')';
}
;
var Lorg_slf4j_helpers_MarkerIgnoringBase_2_classLit = createForClass('org.slf4j.helpers', 'MarkerIgnoringBase', 461, Ljava_lang_Object_2_classLit);
function $log_0(this$static, level, msg, thrown){
  $log_1(this$static.logger, level, msg, thrown);
}

function JULLogger(logger){
  this.logger = logger;
}

defineClass(404, 461, $intern_25, JULLogger);
_.getName = function getName(){
  return $getName_0(this.logger);
}
;
_.warn = function warn(format, arg){
  var ft;
  if ($isLoggable(this.logger, ($clinit_Level() , WARNING))) {
    ft = arrayFormat(format, stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [arg]));
    $log_0(this, WARNING, ft.message_0, ft.throwable);
  }
}
;
var Lde_benediktmeurer_gwt_slf4j_jul_client_JULLogger_2_classLit = createForClass('de.benediktmeurer.gwt.slf4j.jul.client', 'JULLogger', 404, Lorg_slf4j_helpers_MarkerIgnoringBase_2_classLit);
function JULLoggerFactory(){
  this.loggers = new HashMap;
}

defineClass(388, 1, {}, JULLoggerFactory);
_.getLogger = function getLogger(name_0){
  var logger;
  logger = castTo(this.loggers.get_0(name_0), 414);
  if (!logger) {
    logger = new JULLogger(getLogger_0(name_0));
    this.loggers.put_0(logger.getName(), logger);
  }
  return logger;
}
;
var Lde_benediktmeurer_gwt_slf4j_jul_client_JULLoggerFactory_2_classLit = createForClass('de.benediktmeurer.gwt.slf4j.jul.client', 'JULLoggerFactory', 388, Ljava_lang_Object_2_classLit);
defineClass(431, 1, {});
var Ljava_io_OutputStream_2_classLit = createForClass('java.io', 'OutputStream', 431, Ljava_lang_Object_2_classLit);
defineClass(432, 431, {});
var Ljava_io_FilterOutputStream_2_classLit = createForClass('java.io', 'FilterOutputStream', 432, Ljava_io_OutputStream_2_classLit);
function PrintStream(){
}

defineClass(130, 432, {}, PrintStream);
var Ljava_io_PrintStream_2_classLit = createForClass('java.io', 'PrintStream', 130, Ljava_io_FilterOutputStream_2_classLit);
function $setLength(this$static, newLength){
  var oldLength;
  oldLength = this$static.string.length;
  newLength < oldLength?(this$static.string = $substring_0(this$static.string, 0, newLength)):newLength > oldLength && (this$static.string += valueOf_2(initUnidimensionalArray(C_classLit, $intern_11, 57, newLength - oldLength, 15, 1)));
}

function AbstractStringBuilder(string){
  this.string = string;
}

defineClass(79, 1, {84:1});
_.toString_0 = function toString_29(){
  return this.string;
}
;
var Ljava_lang_AbstractStringBuilder_2_classLit = createForClass('java.lang', 'AbstractStringBuilder', 79, Ljava_lang_Object_2_classLit);
function ArithmeticException(){
  RuntimeException_0.call(this, 'divide by zero');
}

defineClass(221, 11, $intern_1, ArithmeticException);
var Ljava_lang_ArithmeticException_2_classLit = createForClass('java.lang', 'ArithmeticException', 221, Ljava_lang_RuntimeException_2_classLit);
function IndexOutOfBoundsException(){
  RuntimeException.call(this);
}

function IndexOutOfBoundsException_0(message){
  RuntimeException_0.call(this, message);
}

defineClass(69, 11, $intern_1, IndexOutOfBoundsException, IndexOutOfBoundsException_0);
var Ljava_lang_IndexOutOfBoundsException_2_classLit = createForClass('java.lang', 'IndexOutOfBoundsException', 69, Ljava_lang_RuntimeException_2_classLit);
function ArrayIndexOutOfBoundsException(msg){
  IndexOutOfBoundsException_0.call(this, msg);
}

defineClass(152, 69, $intern_1, ArrayIndexOutOfBoundsException);
var Ljava_lang_ArrayIndexOutOfBoundsException_2_classLit = createForClass('java.lang', 'ArrayIndexOutOfBoundsException', 152, Ljava_lang_IndexOutOfBoundsException_2_classLit);
function ArrayStoreException(){
  RuntimeException.call(this);
}

function ArrayStoreException_0(message){
  RuntimeException_0.call(this, message);
}

defineClass(132, 11, $intern_1, ArrayStoreException, ArrayStoreException_0);
var Ljava_lang_ArrayStoreException_2_classLit = createForClass('java.lang', 'ArrayStoreException', 132, Ljava_lang_RuntimeException_2_classLit);
function $clinit_Boolean(){
  $clinit_Boolean = emptyMethod;
}

function $booleanValue(this$static){
  return $clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , this$static;
}

function $compareTo_1(this$static, b){
  return compare_2(($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , this$static), (checkCriticalNotNull(b) , b));
}

function compare_2(x_0, y_0){
  $clinit_Boolean();
  return x_0 == y_0?0:x_0?1:-1;
}

function compareTo_Ljava_lang_Object__I__devirtual$(this$static, other){
  $clinit_Boolean();
  return instanceOfString(this$static)?$compareTo_5(this$static, castToString(other)):instanceOfDouble(this$static)?$compareTo_2(this$static, ($clinit_InternalPreconditions() , $clinit_InternalPreconditions() , checkCriticalType(other == null || instanceOfDouble(other), null) , other)):instanceOfBoolean(this$static)?$compareTo_1(this$static, castToBoolean(other)):this$static.compareTo(other);
}

booleanCastMap = {3:1, 212:1, 32:1};
var Ljava_lang_Boolean_2_classLit = createForClass('java.lang', 'Boolean', 212, Ljava_lang_Object_2_classLit);
function digit(c, radix){
  if (radix < 2 || radix > 36) {
    return -1;
  }
  if (c >= 48 && c < 48 + $wnd.Math.min(radix, 10)) {
    return c - 48;
  }
  if (c >= 97 && c < radix + 97 - 10) {
    return c - 97 + 10;
  }
  if (c >= 65 && c < radix + 65 - 10) {
    return c - 65 + 10;
  }
  return -1;
}

var Ljava_lang_Character_2_classLit = createForClass('java.lang', 'Character', null, Ljava_lang_Object_2_classLit);
function ClassCastException(message){
  RuntimeException_0.call(this, message);
}

defineClass(214, 11, $intern_1, ClassCastException);
var Ljava_lang_ClassCastException_2_classLit = createForClass('java.lang', 'ClassCastException', 214, Ljava_lang_RuntimeException_2_classLit);
function __parseAndValidateInt(s, radix){
  var i, isTooLow, length_0, startIndex, toReturn;
  if (s == null) {
    throw toJs(new NumberFormatException('null'));
  }
  if (radix < 2 || radix > 36) {
    throw toJs(new NumberFormatException('radix ' + radix + ' out of range'));
  }
  length_0 = s.length;
  startIndex = length_0 > 0 && ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(0, s.length) , s.charCodeAt(0) == 45 || (checkCriticalStringElementIndex(0, s.length) , s.charCodeAt(0) == 43))?1:0;
  for (i = startIndex; i < length_0; i++) {
    if (digit(($clinit_InternalPreconditions() , checkCriticalStringElementIndex(i, s.length) , s.charCodeAt(i)), radix) == -1) {
      throw toJs(new NumberFormatException('For input string: "' + s + '"'));
    }
  }
  toReturn = parseInt(s, radix);
  isTooLow = toReturn < -2147483648;
  if (isNaN(toReturn)) {
    throw toJs(new NumberFormatException('For input string: "' + s + '"'));
  }
   else if (isTooLow || toReturn > 2147483647) {
    throw toJs(new NumberFormatException('For input string: "' + s + '"'));
  }
  return toReturn;
}

defineClass(86, 1, {3:1, 86:1});
var Ljava_lang_Number_2_classLit = createForClass('java.lang', 'Number', 86, Ljava_lang_Object_2_classLit);
function $compareTo_2(this$static, b){
  return compare_3(($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , this$static), (checkCriticalNotNull(b) , b));
}

function compare_3(x_0, y_0){
  if (x_0 < y_0) {
    return -1;
  }
  if (x_0 > y_0) {
    return 1;
  }
  if (x_0 == y_0) {
    return x_0 == 0?compare_3(1 / x_0, 1 / y_0):0;
  }
  return isNaN(x_0)?isNaN(y_0)?0:1:-1;
}

doubleCastMap = {3:1, 32:1, 213:1, 86:1};
var Ljava_lang_Double_2_classLit = createForClass('java.lang', 'Double', 213, Ljava_lang_Number_2_classLit);
function IllegalArgumentException(){
  RuntimeException.call(this);
}

function IllegalArgumentException_0(message){
  RuntimeException_0.call(this, message);
}

defineClass(44, 11, $intern_1, IllegalArgumentException, IllegalArgumentException_0);
var Ljava_lang_IllegalArgumentException_2_classLit = createForClass('java.lang', 'IllegalArgumentException', 44, Ljava_lang_RuntimeException_2_classLit);
function IllegalStateException(){
  RuntimeException.call(this);
}

function IllegalStateException_0(s){
  RuntimeException_0.call(this, s);
}

defineClass(46, 11, $intern_1, IllegalStateException, IllegalStateException_0);
var Ljava_lang_IllegalStateException_2_classLit = createForClass('java.lang', 'IllegalStateException', 46, Ljava_lang_RuntimeException_2_classLit);
function $compareTo_3(this$static, b){
  return compare_4(this$static.value_0, b.value_0);
}

function Integer(value_0){
  this.value_0 = value_0;
}

function compare_4(x_0, y_0){
  return x_0 < y_0?-1:x_0 > y_0?1:0;
}

function numberOfLeadingZeros_0(i){
  var m, n, y_0;
  if (i < 0) {
    return 0;
  }
   else if (i == 0) {
    return 32;
  }
   else {
    y_0 = -(i >> 16);
    m = y_0 >> 16 & 16;
    n = 16 - m;
    i = i >> m;
    y_0 = i - 256;
    m = y_0 >> 16 & 8;
    n += m;
    i <<= m;
    y_0 = i - 4096;
    m = y_0 >> 16 & 4;
    n += m;
    i <<= m;
    y_0 = i - $intern_20;
    m = y_0 >> 16 & 2;
    n += m;
    i <<= m;
    y_0 = i >> 14;
    m = y_0 & ~(y_0 >> 1);
    return n + 2 - m;
  }
}

function numberOfTrailingZeros(i){
  var r, rtn;
  if (i == 0) {
    return 32;
  }
   else {
    rtn = 0;
    for (r = 1; (r & i) == 0; r <<= 1) {
      ++rtn;
    }
    return rtn;
  }
}

function valueOf(i){
  var rebase, result_0;
  if (i > -129 && i < 128) {
    rebase = i + 128;
    result_0 = ($clinit_Integer$BoxedValues() , boxedValues)[rebase];
    !result_0 && (result_0 = boxedValues[rebase] = new Integer(i));
    return result_0;
  }
  return new Integer(i);
}

defineClass(25, 86, {3:1, 32:1, 25:1, 86:1}, Integer);
_.compareTo = function compareTo_1(b){
  return $compareTo_3(this, castTo(b, 25));
}
;
_.equals_0 = function equals_11(o){
  return instanceOf(o, 25) && castTo(o, 25).value_0 == this.value_0;
}
;
_.hashCode_0 = function hashCode_10(){
  return this.value_0;
}
;
_.toString_0 = function toString_31(){
  return '' + this.value_0;
}
;
_.value_0 = 0;
var Ljava_lang_Integer_2_classLit = createForClass('java.lang', 'Integer', 25, Ljava_lang_Number_2_classLit);
function $clinit_Integer$BoxedValues(){
  $clinit_Integer$BoxedValues = emptyMethod;
  boxedValues = initUnidimensionalArray(Ljava_lang_Integer_2_classLit, $intern_0, 25, 256, 0, 1);
}

var boxedValues;
function $compareTo_4(this$static, b){
  return compare_5(this$static.value_0, b.value_0);
}

function Long(value_0){
  this.value_0 = value_0;
}

function compare_5(x_0, y_0){
  return compare_1(x_0, y_0) < 0?-1:compare_1(x_0, y_0) > 0?1:0;
}

function valueOf_0(i){
  var rebase, result_0;
  if (compare_1(i, -129) > 0 && compare_1(i, 128) < 0) {
    rebase = toInt_0(i) + 128;
    result_0 = ($clinit_Long$BoxedValues() , boxedValues_0)[rebase];
    !result_0 && (result_0 = boxedValues_0[rebase] = new Long(i));
    return result_0;
  }
  return new Long(i);
}

defineClass(59, 86, {3:1, 32:1, 59:1, 86:1}, Long);
_.compareTo = function compareTo_2(b){
  return $compareTo_4(this, castTo(b, 59));
}
;
_.equals_0 = function equals_12(o){
  return instanceOf(o, 59) && eq(castTo(o, 59).value_0, this.value_0);
}
;
_.hashCode_0 = function hashCode_11(){
  return toInt_0(this.value_0);
}
;
_.toString_0 = function toString_32(){
  return '' + toString_25(this.value_0);
}
;
_.value_0 = 0;
var Ljava_lang_Long_2_classLit = createForClass('java.lang', 'Long', 59, Ljava_lang_Number_2_classLit);
function $clinit_Long$BoxedValues(){
  $clinit_Long$BoxedValues = emptyMethod;
  boxedValues_0 = initUnidimensionalArray(Ljava_lang_Long_2_classLit, $intern_0, 59, 256, 0, 1);
}

var boxedValues_0;
defineClass(505, 1, {});
function NullPointerException(){
  RuntimeException.call(this);
}

function NullPointerException_0(message){
  RuntimeException_0.call(this, message);
}

defineClass(87, 110, $intern_1, NullPointerException, NullPointerException_0);
_.createError = function createError_0(msg){
  return new TypeError(msg);
}
;
var Ljava_lang_NullPointerException_2_classLit = createForClass('java.lang', 'NullPointerException', 87, Ljava_lang_JsException_2_classLit);
function NumberFormatException(message){
  IllegalArgumentException_0.call(this, message);
}

defineClass(40, 44, {3:1, 17:1, 40:1, 11:1, 10:1}, NumberFormatException);
var Ljava_lang_NumberFormatException_2_classLit = createForClass('java.lang', 'NumberFormatException', 40, Ljava_lang_IllegalArgumentException_2_classLit);
function StackTraceElement(methodName, fileName, lineNumber){
  this.className_0 = 'Unknown';
  this.methodName = methodName;
  this.fileName = fileName;
  this.lineNumber = lineNumber;
}

defineClass(43, 1, {3:1, 43:1}, StackTraceElement);
_.equals_0 = function equals_13(other){
  var st;
  if (instanceOf(other, 43)) {
    st = castTo(other, 43);
    return this.lineNumber == st.lineNumber && equals_18(this.methodName, st.methodName) && equals_18(this.className_0, st.className_0) && equals_18(this.fileName, st.fileName);
  }
  return false;
}
;
_.hashCode_0 = function hashCode_12(){
  return hashCode_16(stampJavaTypeInfo(getClassLiteralForArray(Ljava_lang_Object_2_classLit, 1), $intern_0, 1, 5, [valueOf(this.lineNumber), this.className_0, this.methodName, this.fileName]));
}
;
_.toString_0 = function toString_33(){
  return this.className_0 + '.' + this.methodName + '(' + (this.fileName != null?this.fileName:'Unknown Source') + (this.lineNumber >= 0?':' + this.lineNumber:'') + ')';
}
;
_.lineNumber = 0;
var Ljava_lang_StackTraceElement_2_classLit = createForClass('java.lang', 'StackTraceElement', 43, Ljava_lang_Object_2_classLit);
function $charAt(this$static, index_0){
  $clinit_InternalPreconditions();
  checkCriticalStringElementIndex(index_0, this$static.length);
  return this$static.charCodeAt(index_0);
}

function $compareTo_5(this$static, other){
  var a, b;
  a = ($clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , this$static);
  b = (checkCriticalNotNull(other) , other);
  return a == b?0:a < b?-1:1;
}

function $endsWith(this$static, suffix){
  var suffixlength;
  suffixlength = suffix.length;
  return $equals_0(this$static.substr(this$static.length - suffixlength, suffixlength), suffix);
}

function $equals_0(this$static, other){
  return $clinit_InternalPreconditions() , checkCriticalNotNull(this$static) , maskUndefined(this$static) === maskUndefined(other);
}

function $equalsIgnoreCase(this$static, other){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(this$static);
  if (other == null) {
    return false;
  }
  if ($equals_0(this$static, other)) {
    return true;
  }
  return this$static.length == other.length && $equals_0(this$static.toLowerCase(), other.toLowerCase());
}

function $indexOf(this$static, str){
  return this$static.indexOf(str);
}

function $indexOf_0(this$static, str, startIndex){
  return this$static.indexOf(str, startIndex);
}

function $lastIndexOf(this$static, str){
  return this$static.lastIndexOf(str);
}

function $lastIndexOf_0(this$static, str, start_0){
  return this$static.lastIndexOf(str, start_0);
}

function $replace_0(this$static, from, to){
  var regex, replacement;
  regex = $replaceAll(toString_26(from), '([/\\\\\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}$^])', '\\\\$1');
  replacement = $replaceAll($replaceAll(toString_26(to), '\\\\', '\\\\\\\\'), '\\$', '\\\\$');
  return $replaceAll(this$static, regex, replacement);
}

function $replaceAll(this$static, regex, replace){
  replace = translateReplaceString(replace);
  return this$static.replace(new RegExp(regex, 'g'), replace);
}

function $split(this$static, regex, maxMatch){
  var compiled, count, lastNonEmpty, lastTrail, matchIndex, matchObj, out, trail;
  compiled = new RegExp(regex, 'g');
  out = initUnidimensionalArray(Ljava_lang_String_2_classLit, $intern_0, 2, 0, 6, 1);
  count = 0;
  trail = this$static;
  lastTrail = null;
  while (true) {
    matchObj = compiled.exec(trail);
    if (matchObj == null || trail == '' || count == maxMatch - 1 && maxMatch > 0) {
      out[count] = trail;
      break;
    }
     else {
      matchIndex = matchObj.index;
      out[count] = trail.substr(0, matchIndex);
      trail = $substring_0(trail, matchIndex + matchObj[0].length, trail.length);
      compiled.lastIndex = 0;
      if (lastTrail == trail) {
        out[count] = trail.substr(0, 1);
        trail = trail.substr(1);
      }
      lastTrail = trail;
      ++count;
    }
  }
  if (maxMatch == 0 && this$static.length > 0) {
    lastNonEmpty = out.length;
    while (lastNonEmpty > 0 && out[lastNonEmpty - 1] == '') {
      --lastNonEmpty;
    }
    lastNonEmpty < out.length && (out.length = lastNonEmpty);
  }
  return out;
}

function $startsWith(this$static, prefix){
  return $equals_0(this$static.substr(0, prefix.length), prefix);
}

function $substring(this$static, beginIndex){
  return this$static.substr(beginIndex);
}

function $substring_0(this$static, beginIndex, endIndex){
  return this$static.substr(beginIndex, endIndex - beginIndex);
}

function $trim(this$static){
  var end, length_0, start_0;
  length_0 = this$static.length;
  start_0 = 0;
  while (start_0 < length_0 && ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(start_0, this$static.length) , this$static.charCodeAt(start_0) <= 32)) {
    ++start_0;
  }
  end = length_0;
  while (end > start_0 && ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(end - 1, this$static.length) , this$static.charCodeAt(end - 1) <= 32)) {
    --end;
  }
  return start_0 > 0 || end < length_0?this$static.substr(start_0, end - start_0):this$static;
}

function fromCharCode(array){
  return String.fromCharCode.apply(null, array);
}

function fromCodePoint(codePoint){
  var hiSurrogate, loSurrogate;
  if (codePoint >= $intern_10) {
    hiSurrogate = 55296 + (codePoint - $intern_10 >> 10 & 1023) & $intern_9;
    loSurrogate = 56320 + (codePoint - $intern_10 & 1023) & $intern_9;
    return String.fromCharCode(hiSurrogate) + ('' + String.fromCharCode(loSurrogate));
  }
   else {
    return String.fromCharCode(codePoint & $intern_9);
  }
}

function translateReplaceString(replaceStr){
  var pos;
  pos = 0;
  while (0 <= (pos = replaceStr.indexOf('\\', pos))) {
    $clinit_InternalPreconditions();
    checkCriticalStringElementIndex(pos + 1, replaceStr.length);
    replaceStr.charCodeAt(pos + 1) == 36?(replaceStr = replaceStr.substr(0, pos) + '$' + $substring(replaceStr, ++pos)):(replaceStr = replaceStr.substr(0, pos) + ('' + $substring(replaceStr, ++pos)));
  }
  return replaceStr;
}

function valueOf_1(x_0){
  return x_0 == null?'null':toString_26(x_0);
}

function valueOf_2(x_0){
  return valueOf_3(x_0, x_0.length);
}

function valueOf_3(x_0, count){
  var batchEnd, batchStart, end, s;
  end = 0 + count;
  checkCriticalStringBounds(0, end, x_0.length);
  s = '';
  for (batchStart = 0; batchStart < end;) {
    batchEnd = $wnd.Math.min(batchStart + 10000, end);
    s += fromCharCode(x_0.slice(batchStart, batchEnd));
    batchStart = batchEnd;
  }
  return s;
}

stringCastMap = {3:1, 84:1, 32:1, 2:1};
var Ljava_lang_String_2_classLit = createForClass('java.lang', 'String', 2, Ljava_lang_Object_2_classLit);
function $append(this$static, x_0){
  this$static.string += x_0;
  return this$static;
}

function $append_0(this$static, x_0){
  this$static.string += '' + x_0;
  return this$static;
}

function StringBuffer(){
  AbstractStringBuilder.call(this, '');
}

function StringBuffer_0(){
  AbstractStringBuilder.call(this, '');
}

defineClass(109, 79, {84:1}, StringBuffer, StringBuffer_0);
var Ljava_lang_StringBuffer_2_classLit = createForClass('java.lang', 'StringBuffer', 109, Ljava_lang_AbstractStringBuilder_2_classLit);
function $append_1(this$static, x_0){
  this$static.string += String.fromCharCode(x_0);
  return this$static;
}

function $append_2(this$static, x_0){
  this$static.string += x_0;
  return this$static;
}

function $append_3(this$static, x_0){
  this$static.string += '' + x_0;
  return this$static;
}

function $append_4(this$static, x_0){
  this$static.string += '' + x_0;
  return this$static;
}

function $append_5(this$static, x_0){
  this$static.string += '' + x_0;
  return this$static;
}

function StringBuilder(){
  AbstractStringBuilder.call(this, '');
}

function StringBuilder_0(){
  AbstractStringBuilder.call(this, '');
}

function StringBuilder_1(s){
  AbstractStringBuilder.call(this, ($clinit_InternalPreconditions() , checkCriticalNotNull(s) , s));
}

defineClass(24, 79, {84:1}, StringBuilder, StringBuilder_0, StringBuilder_1);
var Ljava_lang_StringBuilder_2_classLit = createForClass('java.lang', 'StringBuilder', 24, Ljava_lang_AbstractStringBuilder_2_classLit);
function StringIndexOutOfBoundsException(message){
  IndexOutOfBoundsException_0.call(this, message);
}

defineClass(129, 69, $intern_1, StringIndexOutOfBoundsException);
var Ljava_lang_StringIndexOutOfBoundsException_2_classLit = createForClass('java.lang', 'StringIndexOutOfBoundsException', 129, Ljava_lang_IndexOutOfBoundsException_2_classLit);
function $clinit_System(){
  $clinit_System = emptyMethod;
  err = new PrintStream;
  new PrintStream;
}

function arraycopy(src_0, srcOfs, dest, destOfs, len){
  $clinit_System();
  var destArray, destComp, destEnd, destType, destlen, srcArray, srcComp, srcType, srclen;
  $clinit_InternalPreconditions();
  checkCriticalNotNull_0(src_0, 'src');
  checkCriticalNotNull_0(dest, 'dest');
  srcType = getClass__Ljava_lang_Class___devirtual$(src_0);
  destType = getClass__Ljava_lang_Class___devirtual$(dest);
  checkCriticalArrayType_0((srcType.modifiers & 4) != 0, 'srcType is not an array');
  checkCriticalArrayType_0((destType.modifiers & 4) != 0, 'destType is not an array');
  srcComp = srcType.componentType;
  destComp = destType.componentType;
  checkCriticalArrayType_0((srcComp.modifiers & 1) != 0?equals_Ljava_lang_Object__Z__devirtual$(srcComp, destComp):(destComp.modifiers & 1) == 0, "Array types don't match");
  srclen = src_0.length;
  destlen = dest.length;
  if (srcOfs < 0 || destOfs < 0 || len < 0 || srcOfs + len > srclen || destOfs + len > destlen) {
    throw toJs(new IndexOutOfBoundsException);
  }
  if ((srcComp.modifiers & 1) == 0 && !equals_Ljava_lang_Object__Z__devirtual$(srcType, destType)) {
    srcArray = src_0;
    destArray = dest;
    if (src_0 === dest && srcOfs < destOfs) {
      srcOfs += len;
      for (destEnd = destOfs + len; destEnd-- > destOfs;) {
        setCheck(destArray, destEnd, srcArray[--srcOfs]);
      }
    }
     else {
      for (destEnd = destOfs + len; destOfs < destEnd;) {
        setCheck(destArray, destOfs++, srcArray[srcOfs++]);
      }
    }
  }
   else 
    len > 0 && copy(src_0, srcOfs, dest, destOfs, len, true);
}

defineClass(509, 1, {});
var err;
function Throwable$lambda$0$Type(){
}

defineClass(202, 1, {}, Throwable$lambda$0$Type);
_.apply_0 = function apply_20(arg0){
  return castTo(arg0, 10).backingJsObject;
}
;
var Ljava_lang_Throwable$lambda$0$Type_2_classLit = createForAnonymousClass('java.lang', 202, Ljava_lang_Object_2_classLit);
function $contains(this$static, o){
  if (instanceOf(o, 16)) {
    return $containsEntry(this$static.this$01, castTo(o, 16));
  }
  return false;
}

function AbstractHashMap$EntrySet(this$0){
  this.this$01 = this$0;
}

defineClass(324, 443, $intern_2, AbstractHashMap$EntrySet);
_.clear_0 = function clear_8(){
  this.this$01.clear_0();
}
;
_.contains = function contains_1(o){
  return $contains(this, o);
}
;
_.iterator = function iterator_4(){
  return new AbstractHashMap$EntrySetIterator(this.this$01);
}
;
_.remove_1 = function remove_12(entry){
  var key;
  if ($contains(this, entry)) {
    key = castTo(entry, 16).getKey();
    this.this$01.remove_0(key);
    return true;
  }
  return false;
}
;
_.size_1 = function size_10(){
  return this.this$01.size_1();
}
;
var Ljava_util_AbstractHashMap$EntrySet_2_classLit = createForClass('java.util', 'AbstractHashMap/EntrySet', 324, Ljava_util_AbstractSet_2_classLit);
function $computeHasNext(this$static){
  if (this$static.current.hasNext_0()) {
    return true;
  }
  if (this$static.current != this$static.stringMapEntries) {
    return false;
  }
  this$static.current = new InternalHashCodeMap$1(this$static.this$01.hashCodeMap);
  return this$static.current.hasNext_0();
}

function AbstractHashMap$EntrySetIterator(this$0){
  this.this$01 = this$0;
  this.stringMapEntries = new InternalStringMap$1(this.this$01.stringMap);
  this.current = this.stringMapEntries;
  this.hasNext = $computeHasNext(this);
  recordLastKnownStructure(this$0, this);
}

defineClass(325, 1, {}, AbstractHashMap$EntrySetIterator);
_.forEachRemaining = function forEachRemaining_4(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_5(){
  var rv;
  return checkStructuralChange(this.this$01, this) , $clinit_InternalPreconditions() , checkCriticalElement(this.hasNext) , this.last = this.current , rv = castTo(this.current.next_1(), 16) , this.hasNext = $computeHasNext(this) , rv;
}
;
_.hasNext_0 = function hasNext_4(){
  return this.hasNext;
}
;
_.remove_2 = function remove_13(){
  $clinit_InternalPreconditions();
  checkCriticalState(!!this.last);
  checkStructuralChange(this.this$01, this);
  this.last.remove_2();
  this.last = null;
  this.hasNext = $computeHasNext(this);
  recordLastKnownStructure(this.this$01, this);
}
;
_.hasNext = false;
var Ljava_util_AbstractHashMap$EntrySetIterator_2_classLit = createForClass('java.util', 'AbstractHashMap/EntrySetIterator', 325, Ljava_lang_Object_2_classLit);
defineClass(434, 433, $intern_26);
_.spliterator_0 = function spliterator_2(){
  return new Spliterators$IteratorSpliterator(this, 16);
}
;
_.add_1 = function add_3(index_0, element){
  throw toJs(new UnsupportedOperationException_0('Add not supported on this list'));
}
;
_.add_0 = function add_4(obj){
  this.add_1(this.size_1(), obj);
  return true;
}
;
_.clear_0 = function clear_9(){
  this.removeRange(0, this.size_1());
}
;
_.equals_0 = function equals_14(o){
  var elem, elem$iterator, elemOther, iterOther, other;
  if (o === this) {
    return true;
  }
  if (!instanceOf(o, 15)) {
    return false;
  }
  other = castTo(o, 15);
  if (this.size_1() != other.size_1()) {
    return false;
  }
  iterOther = other.iterator();
  for (elem$iterator = this.iterator(); elem$iterator.hasNext_0();) {
    elem = elem$iterator.next_1();
    elemOther = iterOther.next_1();
    if (!(maskUndefined(elem) === maskUndefined(elemOther) || elem != null && equals_Ljava_lang_Object__Z__devirtual$(elem, elemOther))) {
      return false;
    }
  }
  return true;
}
;
_.hashCode_0 = function hashCode_13(){
  return hashCode_18(this);
}
;
_.indexOf_0 = function indexOf(toFind){
  var i, n;
  for (i = 0 , n = this.size_1(); i < n; ++i) {
    if (equals_18(toFind, this.get_2(i))) {
      return i;
    }
  }
  return -1;
}
;
_.iterator = function iterator_5(){
  return new AbstractList$IteratorImpl(this);
}
;
_.listIterator = function listIterator(){
  return new AbstractList$ListIteratorImpl(this, 0);
}
;
_.listIterator_0 = function listIterator_0(from){
  return new AbstractList$ListIteratorImpl(this, from);
}
;
_.remove_3 = function remove_14(index_0){
  throw toJs(new UnsupportedOperationException_0('Remove not supported on this list'));
}
;
_.removeRange = function removeRange(fromIndex, endIndex){
  var i, iter;
  iter = new AbstractList$ListIteratorImpl(this, fromIndex);
  for (i = fromIndex; i < endIndex; ++i) {
    iter.next_1();
    iter.remove_2();
  }
}
;
_.set_0 = function set_0(index_0, o){
  throw toJs(new UnsupportedOperationException_0('Set not supported on this list'));
}
;
_.subList = function subList(fromIndex, toIndex){
  return new AbstractList$SubList(this, fromIndex, toIndex);
}
;
var Ljava_util_AbstractList_2_classLit = createForClass('java.util', 'AbstractList', 434, Ljava_util_AbstractCollection_2_classLit);
function $remove_4(this$static){
  $clinit_InternalPreconditions();
  checkCriticalState(this$static.last != -1);
  this$static.this$01_0.remove_3(this$static.last);
  this$static.i = this$static.last;
  this$static.last = -1;
}

function AbstractList$IteratorImpl(this$0){
  this.this$01_0 = this$0;
}

defineClass(131, 1, {}, AbstractList$IteratorImpl);
_.forEachRemaining = function forEachRemaining_5(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext_5(){
  return this.i < this.this$01_0.size_1();
}
;
_.next_1 = function next_6(){
  $clinit_InternalPreconditions();
  checkCriticalElement(this.i < this.this$01_0.size_1());
  return this.this$01_0.get_2(this.last = this.i++);
}
;
_.remove_2 = function remove_15(){
  $remove_4(this);
}
;
_.i = 0;
_.last = -1;
var Ljava_util_AbstractList$IteratorImpl_2_classLit = createForClass('java.util', 'AbstractList/IteratorImpl', 131, Ljava_lang_Object_2_classLit);
function AbstractList$ListIteratorImpl(this$0, start_0){
  this.this$01 = this$0;
  AbstractList$IteratorImpl.call(this, this$0);
  $clinit_InternalPreconditions();
  checkCriticalPositionIndex(start_0, this$0.size_1());
  this.i = start_0;
}

defineClass(112, 131, {}, AbstractList$ListIteratorImpl);
_.remove_2 = function remove_16(){
  $remove_4(this);
}
;
_.nextIndex = function nextIndex_0(){
  return this.i;
}
;
_.previous = function previous_0(){
  $clinit_InternalPreconditions();
  checkCriticalElement(this.i > 0);
  return this.this$01.get_2(this.last = --this.i);
}
;
_.previousIndex = function previousIndex(){
  return this.i - 1;
}
;
_.set_1 = function set_1(o){
  $clinit_InternalPreconditions();
  checkCriticalState(this.last != -1);
  this.this$01.set_0(this.last, o);
}
;
var Ljava_util_AbstractList$ListIteratorImpl_2_classLit = createForClass('java.util', 'AbstractList/ListIteratorImpl', 112, Ljava_util_AbstractList$IteratorImpl_2_classLit);
function AbstractList$SubList(wrapped, fromIndex, toIndex){
  checkCriticalPositionIndexes(fromIndex, toIndex, wrapped.size_1());
  this.wrapped = wrapped;
  this.fromIndex = fromIndex;
  this.size_0 = toIndex - fromIndex;
}

defineClass(220, 434, $intern_26, AbstractList$SubList);
_.add_1 = function add_5(index_0, element){
  $clinit_InternalPreconditions();
  checkCriticalPositionIndex(index_0, this.size_0);
  this.wrapped.add_1(this.fromIndex + index_0, element);
  ++this.size_0;
}
;
_.get_2 = function get_76(index_0){
  $clinit_InternalPreconditions();
  checkCriticalElementIndex(index_0, this.size_0);
  return this.wrapped.get_2(this.fromIndex + index_0);
}
;
_.remove_3 = function remove_17(index_0){
  var result_0;
  $clinit_InternalPreconditions();
  checkCriticalElementIndex(index_0, this.size_0);
  result_0 = this.wrapped.remove_3(this.fromIndex + index_0);
  --this.size_0;
  return result_0;
}
;
_.set_0 = function set_2(index_0, element){
  $clinit_InternalPreconditions();
  checkCriticalElementIndex(index_0, this.size_0);
  return this.wrapped.set_0(this.fromIndex + index_0, element);
}
;
_.size_1 = function size_11(){
  return this.size_0;
}
;
_.fromIndex = 0;
_.size_0 = 0;
var Ljava_util_AbstractList$SubList_2_classLit = createForClass('java.util', 'AbstractList/SubList', 220, Ljava_util_AbstractList_2_classLit);
function AbstractMap$1(this$0){
  this.this$01 = this$0;
}

defineClass(326, 443, $intern_2, AbstractMap$1);
_.clear_0 = function clear_10(){
  this.this$01.clear_0();
}
;
_.contains = function contains_2(key){
  return this.this$01.containsKey_0(key);
}
;
_.iterator = function iterator_6(){
  var outerIter;
  outerIter = this.this$01.entrySet().iterator();
  return new AbstractMap$1$1(outerIter);
}
;
_.remove_1 = function remove_18(key){
  if (this.this$01.containsKey_0(key)) {
    this.this$01.remove_0(key);
    return true;
  }
  return false;
}
;
_.size_1 = function size_12(){
  return this.this$01.size_1();
}
;
var Ljava_util_AbstractMap$1_2_classLit = createForAnonymousClass('java.util', 326, Ljava_util_AbstractSet_2_classLit);
function AbstractMap$1$1(val$outerIter){
  this.val$outerIter2 = val$outerIter;
}

defineClass(327, 1, {}, AbstractMap$1$1);
_.forEachRemaining = function forEachRemaining_6(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext_6(){
  return this.val$outerIter2.hasNext_0();
}
;
_.next_1 = function next_7(){
  var entry;
  entry = castTo(this.val$outerIter2.next_1(), 16);
  return entry.getKey();
}
;
_.remove_2 = function remove_19(){
  this.val$outerIter2.remove_2();
}
;
var Ljava_util_AbstractMap$1$1_2_classLit = createForAnonymousClass('java.util', 327, Ljava_lang_Object_2_classLit);
function AbstractMap$2(this$0){
  this.this$01 = this$0;
}

defineClass(328, 433, {19:1}, AbstractMap$2);
_.clear_0 = function clear_11(){
  this.this$01.clear_0();
}
;
_.contains = function contains_3(value_0){
  return this.this$01.containsValue(value_0);
}
;
_.iterator = function iterator_7(){
  var outerIter;
  outerIter = this.this$01.entrySet().iterator();
  return new AbstractMap$2$1(outerIter);
}
;
_.size_1 = function size_13(){
  return this.this$01.size_1();
}
;
var Ljava_util_AbstractMap$2_2_classLit = createForAnonymousClass('java.util', 328, Ljava_util_AbstractCollection_2_classLit);
function AbstractMap$2$1(val$outerIter){
  this.val$outerIter2 = val$outerIter;
}

defineClass(329, 1, {}, AbstractMap$2$1);
_.forEachRemaining = function forEachRemaining_7(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext_7(){
  return this.val$outerIter2.hasNext_0();
}
;
_.next_1 = function next_8(){
  var entry;
  entry = castTo(this.val$outerIter2.next_1(), 16);
  return entry.getValue();
}
;
_.remove_2 = function remove_20(){
  this.val$outerIter2.remove_2();
}
;
var Ljava_util_AbstractMap$2$1_2_classLit = createForAnonymousClass('java.util', 329, Ljava_lang_Object_2_classLit);
function $setValue(this$static, value_0){
  var oldValue;
  oldValue = this$static.value_0;
  this$static.value_0 = value_0;
  return oldValue;
}

defineClass(321, 1, $intern_3);
_.equals_0 = function equals_15(other){
  var entry;
  if (!instanceOf(other, 16)) {
    return false;
  }
  entry = castTo(other, 16);
  return equals_18(this.key, entry.getKey()) && equals_18(this.value_0, entry.getValue());
}
;
_.getKey = function getKey_1(){
  return this.key;
}
;
_.getValue = function getValue_1(){
  return this.value_0;
}
;
_.hashCode_0 = function hashCode_14(){
  return hashCode_19(this.key) ^ hashCode_19(this.value_0);
}
;
_.setValue = function setValue_1(value_0){
  return $setValue(this, value_0);
}
;
_.toString_0 = function toString_34(){
  return this.key + '=' + this.value_0;
}
;
var Ljava_util_AbstractMap$AbstractEntry_2_classLit = createForClass('java.util', 'AbstractMap/AbstractEntry', 321, Ljava_lang_Object_2_classLit);
function AbstractMap$SimpleEntry(key, value_0){
  this.key = key;
  this.value_0 = value_0;
}

defineClass(115, 321, $intern_3, AbstractMap$SimpleEntry);
var Ljava_util_AbstractMap$SimpleEntry_2_classLit = createForClass('java.util', 'AbstractMap/SimpleEntry', 115, Ljava_util_AbstractMap$AbstractEntry_2_classLit);
defineClass(445, 1, $intern_3);
_.equals_0 = function equals_16(other){
  var entry;
  if (!instanceOf(other, 16)) {
    return false;
  }
  entry = castTo(other, 16);
  return equals_18(this.getKey(), entry.getKey()) && equals_18(this.getValue(), entry.getValue());
}
;
_.hashCode_0 = function hashCode_15(){
  return hashCode_19(this.getKey()) ^ hashCode_19(this.getValue());
}
;
_.toString_0 = function toString_35(){
  return this.getKey() + '=' + this.getValue();
}
;
var Ljava_util_AbstractMapEntry_2_classLit = createForClass('java.util', 'AbstractMapEntry', 445, Ljava_lang_Object_2_classLit);
function $$init_0(this$static){
  this$static.array = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, 0, 5, 1);
}

function $indexOf_1(this$static, o, index_0){
  for (; index_0 < this$static.array.length; ++index_0) {
    if (equals_18(o, this$static.array[index_0])) {
      return index_0;
    }
  }
  return -1;
}

function $remove_5(this$static, index_0){
  var previous;
  previous = ($clinit_InternalPreconditions() , checkCriticalElementIndex(index_0, this$static.array.length) , this$static.array[index_0]);
  removeFrom(this$static.array, index_0, 1);
  return previous;
}

function ArrayList(){
  $$init_0(this);
}

function ArrayList_0(c){
  $$init_0(this);
  insertTo_0(this.array, 0, c.toArray());
}

defineClass(9, 434, $intern_27, ArrayList, ArrayList_0);
_.add_1 = function add_6(index_0, o){
  $clinit_InternalPreconditions();
  checkCriticalPositionIndex(index_0, this.array.length);
  insertTo(this.array, index_0, o);
}
;
_.add_0 = function add_7(o){
  return setCheck(this.array, this.array.length, o) , true;
}
;
_.addAll = function addAll_0(c){
  var cArray, len;
  cArray = c.toArray();
  len = cArray.length;
  if (len == 0) {
    return false;
  }
  insertTo_0(this.array, this.array.length, cArray);
  return true;
}
;
_.clear_0 = function clear_12(){
  this.array = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, 0, 5, 1);
}
;
_.contains = function contains_4(o){
  return $indexOf_1(this, o, 0) != -1;
}
;
_.forEach_0 = function forEach_4(consumer){
  var e, e$array, e$index, e$max;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  for (e$array = this.array , e$index = 0 , e$max = e$array.length; e$index < e$max; ++e$index) {
    e = e$array[e$index];
    consumer.accept_0(e);
  }
}
;
_.get_2 = function get_77(index_0){
  return $clinit_InternalPreconditions() , checkCriticalElementIndex(index_0, this.array.length) , this.array[index_0];
}
;
_.indexOf_0 = function indexOf_0(o){
  return $indexOf_1(this, o, 0);
}
;
_.isEmpty = function isEmpty_1(){
  return this.array.length == 0;
}
;
_.iterator = function iterator_8(){
  return new ArrayList$1(this);
}
;
_.remove_3 = function remove_21(index_0){
  return $remove_5(this, index_0);
}
;
_.remove_1 = function remove_22(o){
  var i;
  i = $indexOf_1(this, o, 0);
  if (i == -1) {
    return false;
  }
  $clinit_InternalPreconditions() , checkCriticalElementIndex(i, this.array.length) , this.array[i];
  removeFrom(this.array, i, 1);
  return true;
}
;
_.removeIf = function removeIf_0(filter){
  var e, index_0, newArray, newIndex;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(filter);
  newArray = null;
  newIndex = 0;
  for (index_0 = 0; index_0 < this.array.length; ++index_0) {
    e = this.array[index_0];
    if (filter.test_0(e)) {
      if (newArray == null) {
        newArray = clone(this.array, index_0);
        newIndex = index_0;
      }
    }
     else 
      newArray != null && setCheck(newArray, newIndex++, e);
  }
  if (newArray == null) {
    return false;
  }
  this.array = newArray;
  return true;
}
;
_.removeRange = function removeRange_0(fromIndex, endIndex){
  var count;
  $clinit_InternalPreconditions();
  checkCriticalPositionIndexes(fromIndex, endIndex, this.array.length);
  count = endIndex - fromIndex;
  removeFrom(this.array, fromIndex, count);
}
;
_.set_0 = function set_3(index_0, o){
  var previous;
  previous = ($clinit_InternalPreconditions() , checkCriticalElementIndex(index_0, this.array.length) , this.array[index_0]);
  setCheck(this.array, index_0, o);
  return previous;
}
;
_.size_1 = function size_14(){
  return this.array.length;
}
;
_.sort_0 = function sort_0(c){
  sort_1(this.array, this.array.length, c);
}
;
_.toArray = function toArray_1(){
  return clone(this.array, this.array.length);
}
;
_.toArray_0 = function toArray_2(out){
  var i, size_0;
  size_0 = this.array.length;
  out.length < size_0 && (out = stampJavaTypeInfo_1(new Array(size_0), out));
  for (i = 0; i < size_0; ++i) {
    setCheck(out, i, this.array[i]);
  }
  out.length > size_0 && setCheck(out, size_0, null);
  return out;
}
;
var Ljava_util_ArrayList_2_classLit = createForClass('java.util', 'ArrayList', 9, Ljava_util_AbstractList_2_classLit);
function ArrayList$1(this$0){
  this.this$01 = this$0;
}

defineClass(316, 1, {}, ArrayList$1);
_.forEachRemaining = function forEachRemaining_8(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext_8(){
  return this.i < this.this$01.array.length;
}
;
_.next_1 = function next_9(){
  $clinit_InternalPreconditions();
  checkCriticalElement(this.i < this.this$01.array.length);
  this.last = this.i++;
  return this.this$01.array[this.last];
}
;
_.remove_2 = function remove_23(){
  $clinit_InternalPreconditions();
  checkCriticalState(this.last != -1);
  $remove_5(this.this$01, this.i = this.last);
  this.last = -1;
}
;
_.i = 0;
_.last = -1;
var Ljava_util_ArrayList$1_2_classLit = createForAnonymousClass('java.util', 316, Ljava_lang_Object_2_classLit);
function hashCode_16(a){
  var e, e$array, e$index, e$max, hashCode;
  hashCode = 1;
  for (e$array = a , e$index = 0 , e$max = e$array.length; e$index < e$max; ++e$index) {
    e = e$array[e$index];
    hashCode = 31 * hashCode + (e != null?hashCode__I__devirtual$(e):0);
    hashCode = hashCode | 0;
  }
  return hashCode;
}

function insertionSort(array, low, high, comp){
  var i, j, t;
  for (i = low + 1; i < high; ++i) {
    for (j = i; j > low && comp.compare(array[j - 1], array[j]) > 0; --j) {
      t = array[j];
      setCheck(array, j, array[j - 1]);
      setCheck(array, j - 1, t);
    }
  }
}

function merge(src_0, srcLow, srcMid, srcHigh, dest, destLow, destHigh, comp){
  var topIdx;
  topIdx = srcMid;
  while (destLow < destHigh) {
    topIdx >= srcHigh || srcLow < srcMid && comp.compare(src_0[srcLow], src_0[topIdx]) <= 0?setCheck(dest, destLow++, src_0[srcLow++]):setCheck(dest, destLow++, src_0[topIdx++]);
  }
}

function mergeSort(x_0, fromIndex, toIndex, comp){
  var temp;
  comp = ($clinit_Comparators() , !comp?INTERNAL_NATURAL_ORDER:comp);
  temp = x_0.slice(fromIndex, toIndex);
  mergeSort_0(temp, x_0, fromIndex, toIndex, -fromIndex, comp);
}

function mergeSort_0(temp, array, low, high, ofs, comp){
  var length_0, tempHigh, tempLow, tempMid;
  length_0 = high - low;
  if (length_0 < 7) {
    insertionSort(array, low, high, comp);
    return;
  }
  tempLow = low + ofs;
  tempHigh = high + ofs;
  tempMid = tempLow + (tempHigh - tempLow >> 1);
  mergeSort_0(array, temp, tempLow, tempMid, -ofs, comp);
  mergeSort_0(array, temp, tempMid, tempHigh, -ofs, comp);
  if (comp.compare(temp[tempMid - 1], temp[tempMid]) <= 0) {
    while (low < high) {
      setCheck(array, low++, temp[tempLow++]);
    }
    return;
  }
  merge(temp, tempLow, tempMid, tempHigh, array, low, high, comp);
}

function sort_1(x_0, toIndex, c){
  checkCriticalArrayBounds_0(0, toIndex, x_0.length);
  mergeSort(x_0, 0, toIndex, c);
}

function spliterator_3(array, endExclusive){
  return checkCriticalArrayBounds(0, endExclusive, array.length) , new Spliterators$ArraySpliterator(array, 0, endExclusive, 1040);
}

function stream_0(array){
  return new StreamImpl(null, spliterator_3(array, array.length));
}

function $toArray(this$static, out){
  var i, size_0;
  size_0 = this$static.array.length;
  out.length < size_0 && (out = stampJavaTypeInfo_1(new Array(size_0), out));
  for (i = 0; i < size_0; ++i) {
    setCheck(out, i, this$static.array[i]);
  }
  out.length > size_0 && setCheck(out, size_0, null);
  return out;
}

function Arrays$ArrayList(array){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(array);
  this.array = array;
}

defineClass(39, 434, $intern_27, Arrays$ArrayList);
_.contains = function contains_5(o){
  return this.indexOf_0(o) != -1;
}
;
_.forEach_0 = function forEach_5(consumer){
  var e, e$array, e$index, e$max;
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  for (e$array = this.array , e$index = 0 , e$max = e$array.length; e$index < e$max; ++e$index) {
    e = e$array[e$index];
    consumer.accept_0(e);
  }
}
;
_.get_2 = function get_78(index_0){
  return $clinit_InternalPreconditions() , checkCriticalElementIndex(index_0, this.array.length) , this.array[index_0];
}
;
_.set_0 = function set_4(index_0, value_0){
  var was;
  was = ($clinit_InternalPreconditions() , checkCriticalElementIndex(index_0, this.array.length) , this.array[index_0]);
  setCheck(this.array, index_0, value_0);
  return was;
}
;
_.size_1 = function size_15(){
  return this.array.length;
}
;
_.toArray = function toArray_3(){
  return $toArray(this, initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, this.array.length, 5, 1));
}
;
_.toArray_0 = function toArray_4(out){
  return $toArray(this, out);
}
;
var Ljava_util_Arrays$ArrayList_2_classLit = createForClass('java.util', 'Arrays/ArrayList', 39, Ljava_util_AbstractList_2_classLit);
function $clinit_Collections(){
  $clinit_Collections = emptyMethod;
  EMPTY_LIST = new Collections$EmptyList;
  EMPTY_MAP = new Collections$EmptyMap;
  EMPTY_SET = new Collections$EmptySet;
}

function hashCode_17(collection){
  $clinit_Collections();
  var e, e$iterator, hashCode;
  hashCode = 0;
  for (e$iterator = collection.iterator(); e$iterator.hasNext_0();) {
    e = e$iterator.next_1();
    hashCode = hashCode + (e != null?hashCode__I__devirtual$(e):0);
    hashCode = hashCode | 0;
  }
  return hashCode;
}

function hashCode_18(list){
  $clinit_Collections();
  var e, e$iterator, hashCode;
  hashCode = 1;
  for (e$iterator = list.iterator(); e$iterator.hasNext_0();) {
    e = e$iterator.next_1();
    hashCode = 31 * hashCode + (e != null?hashCode__I__devirtual$(e):0);
    hashCode = hashCode | 0;
  }
  return hashCode;
}

function reverse(l){
  var t;
  $clinit_Collections();
  var head, headElem, iBack, iFront, tail, tailElem;
  if (instanceOf(l, 195)) {
    for (iFront = 0 , iBack = l.size_1() - 1; iFront < iBack; ++iFront , --iBack) {
      t = l.get_2(iFront);
      l.set_0(iFront, l.get_2(iBack));
      l.set_0(iBack, t);
    }
  }
   else {
    head = l.listIterator();
    tail = l.listIterator_0(l.size_1());
    while (head.nextIndex() < tail.previousIndex()) {
      headElem = head.next_1();
      tailElem = tail.previous();
      head.set_1(tailElem);
      tail.set_1(headElem);
    }
  }
}

var EMPTY_LIST, EMPTY_MAP, EMPTY_SET;
function Collections$EmptyList(){
}

defineClass(339, 434, $intern_27, Collections$EmptyList);
_.contains = function contains_6(object){
  return false;
}
;
_.get_2 = function get_79(location_0){
  $clinit_InternalPreconditions();
  checkCriticalElementIndex(location_0, 0);
  return null;
}
;
_.iterator = function iterator_9(){
  return $clinit_Collections() , $clinit_Collections$EmptyListIterator() , INSTANCE_5;
}
;
_.listIterator = function listIterator_1(){
  return $clinit_Collections() , $clinit_Collections$EmptyListIterator() , INSTANCE_5;
}
;
_.size_1 = function size_16(){
  return 0;
}
;
var Ljava_util_Collections$EmptyList_2_classLit = createForClass('java.util', 'Collections/EmptyList', 339, Ljava_util_AbstractList_2_classLit);
function $clinit_Collections$EmptyListIterator(){
  $clinit_Collections$EmptyListIterator = emptyMethod;
  INSTANCE_5 = new Collections$EmptyListIterator;
}

function Collections$EmptyListIterator(){
}

defineClass(340, 1, {}, Collections$EmptyListIterator);
_.forEachRemaining = function forEachRemaining_9(consumer){
  $forEachRemaining(this, consumer);
}
;
_.hasNext_0 = function hasNext_9(){
  return false;
}
;
_.next_1 = function next_10(){
  throw toJs(new NoSuchElementException);
}
;
_.nextIndex = function nextIndex_1(){
  return 0;
}
;
_.remove_2 = function remove_24(){
  throw toJs(new IllegalStateException);
}
;
_.set_1 = function set_5(o){
  throw toJs(new IllegalStateException);
}
;
var INSTANCE_5;
var Ljava_util_Collections$EmptyListIterator_2_classLit = createForClass('java.util', 'Collections/EmptyListIterator', 340, Ljava_lang_Object_2_classLit);
function Collections$EmptyMap(){
}

defineClass(342, 442, $intern_4, Collections$EmptyMap);
_.containsKey_0 = function containsKey_5(key){
  return false;
}
;
_.containsValue = function containsValue_3(value_0){
  return false;
}
;
_.entrySet = function entrySet_4(){
  return $clinit_Collections() , EMPTY_SET;
}
;
_.get_0 = function get_80(key){
  return null;
}
;
_.keySet = function keySet_2(){
  return $clinit_Collections() , EMPTY_SET;
}
;
_.size_1 = function size_17(){
  return 0;
}
;
_.values_0 = function values_5(){
  return $clinit_Collections() , EMPTY_LIST;
}
;
var Ljava_util_Collections$EmptyMap_2_classLit = createForClass('java.util', 'Collections/EmptyMap', 342, Ljava_util_AbstractMap_2_classLit);
function Collections$EmptySet(){
}

defineClass(341, 443, {3:1, 19:1, 30:1}, Collections$EmptySet);
_.contains = function contains_7(object){
  return false;
}
;
_.iterator = function iterator_10(){
  return $clinit_Collections() , $clinit_Collections$EmptyListIterator() , INSTANCE_5;
}
;
_.size_1 = function size_18(){
  return 0;
}
;
var Ljava_util_Collections$EmptySet_2_classLit = createForClass('java.util', 'Collections/EmptySet', 341, Ljava_util_AbstractSet_2_classLit);
function Collections$SingletonList(element){
  this.element = element;
}

defineClass(135, 434, {3:1, 19:1, 15:1}, Collections$SingletonList);
_.contains = function contains_8(item_0){
  return equals_18(this.element, item_0);
}
;
_.get_2 = function get_81(index_0){
  $clinit_InternalPreconditions();
  checkCriticalElementIndex(index_0, 1);
  return this.element;
}
;
_.size_1 = function size_19(){
  return 1;
}
;
var Ljava_util_Collections$SingletonList_2_classLit = createForClass('java.util', 'Collections/SingletonList', 135, Ljava_util_AbstractList_2_classLit);
function $clinit_Comparators(){
  $clinit_Comparators = emptyMethod;
  INTERNAL_NATURAL_ORDER = new Comparators$NaturalOrderComparator;
  NATURAL_ORDER = new Comparators$NaturalOrderComparator;
}

var INTERNAL_NATURAL_ORDER, NATURAL_ORDER;
function $compare_0(a, b){
  return $clinit_InternalPreconditions() , checkCriticalNotNull(a) , compareTo_Ljava_lang_Object__I__devirtual$(a, (checkCriticalNotNull(b) , b));
}

function Comparators$NaturalOrderComparator(){
}

defineClass(191, 1, $intern_28, Comparators$NaturalOrderComparator);
_.compare = function compare_6(a, b){
  return $compare_0(castTo(a, 32), castTo(b, 32));
}
;
_.equals_0 = function equals_17(other){
  return maskUndefined(this) === maskUndefined(other);
}
;
var Ljava_util_Comparators$NaturalOrderComparator_2_classLit = createForClass('java.util', 'Comparators/NaturalOrderComparator', 191, Ljava_lang_Object_2_classLit);
function $clinit_ConcurrentModificationDetector(){
  $clinit_ConcurrentModificationDetector = emptyMethod;
  API_CHECK = ($clinit_InternalPreconditions() , true);
}

function checkStructuralChange(host, iterator){
  $clinit_ConcurrentModificationDetector();
  if (!API_CHECK) {
    return;
  }
  if (iterator.$modCount != host.$modCount) {
    throw toJs(new ConcurrentModificationException);
  }
}

function recordLastKnownStructure(host, iterator){
  $clinit_ConcurrentModificationDetector();
  if (!API_CHECK) {
    return;
  }
  iterator.$modCount = host.$modCount;
}

function structureChanged(host){
  $clinit_ConcurrentModificationDetector();
  var modCount, modCountable;
  if (!API_CHECK) {
    return;
  }
  modCountable = host;
  modCount = modCountable.$modCount | 0;
  modCountable.$modCount = modCount + 1;
}

var API_CHECK = false;
function ConcurrentModificationException(){
  RuntimeException.call(this);
}

defineClass(126, 11, $intern_1, ConcurrentModificationException);
var Ljava_util_ConcurrentModificationException_2_classLit = createForClass('java.util', 'ConcurrentModificationException', 126, Ljava_lang_RuntimeException_2_classLit);
function $add_3(this$static, o){
  var old;
  old = this$static.map_0.put_0(o, this$static);
  return old == null;
}

function HashSet(){
  this.map_0 = new HashMap;
}

function HashSet_0(c){
  this.map_0 = new HashMap_0(c.size_1());
  this.addAll(c);
}

function HashSet_1(map_0){
  this.map_0 = map_0;
}

defineClass(35, 443, $intern_29, HashSet, HashSet_0);
_.add_0 = function add_8(o){
  return $add_3(this, o);
}
;
_.clear_0 = function clear_13(){
  this.map_0.clear_0();
}
;
_.contains = function contains_9(o){
  return this.map_0.containsKey_0(o);
}
;
_.iterator = function iterator_11(){
  return this.map_0.keySet().iterator();
}
;
_.remove_1 = function remove_25(o){
  return this.map_0.remove_0(o) != null;
}
;
_.size_1 = function size_20(){
  return this.map_0.size_1();
}
;
var Ljava_util_HashSet_2_classLit = createForClass('java.util', 'HashSet', 35, Ljava_util_AbstractSet_2_classLit);
function $findEntryInChain(this$static, key, chain){
  var entry, entry$array, entry$index, entry$max;
  for (entry$array = chain , entry$index = 0 , entry$max = entry$array.length; entry$index < entry$max; ++entry$index) {
    entry = entry$array[entry$index];
    if (this$static.host.equals_1(key, entry.getKey())) {
      return entry;
    }
  }
  return null;
}

function $getChainOrEmpty(this$static, hashCode){
  var chain;
  chain = this$static.backingMap.get(hashCode);
  return chain == null?new Array:chain;
}

function $getEntry(this$static, key){
  return $findEntryInChain(this$static, key, $getChainOrEmpty(this$static, key == null?0:this$static.host.getHashCode(key)));
}

function $put_7(this$static, key, value_0){
  var chain, chain0, entry, hashCode;
  hashCode = key == null?0:this$static.host.getHashCode(key);
  chain0 = (chain = this$static.backingMap.get(hashCode) , chain == null?new Array:chain);
  if (chain0.length == 0) {
    this$static.backingMap.set(hashCode, chain0);
  }
   else {
    entry = $findEntryInChain(this$static, key, chain0);
    if (entry) {
      return entry.setValue(value_0);
    }
  }
  setCheck(chain0, chain0.length, new AbstractMap$SimpleEntry(key, value_0));
  ++this$static.size_0;
  structureChanged(this$static.host);
  return null;
}

function $remove_6(this$static, key){
  var chain, chain0, entry, hashCode, i;
  hashCode = key == null?0:this$static.host.getHashCode(key);
  chain0 = (chain = this$static.backingMap.get(hashCode) , chain == null?new Array:chain);
  for (i = 0; i < chain0.length; i++) {
    entry = chain0[i];
    if (this$static.host.equals_1(key, entry.getKey())) {
      if (chain0.length == 1) {
        chain0.length = 0;
        $delete(this$static.backingMap, hashCode);
      }
       else {
        chain0.splice(i, 1);
      }
      --this$static.size_0;
      structureChanged(this$static.host);
      return entry.getValue();
    }
  }
  return null;
}

function InternalHashCodeMap(host){
  this.backingMap = newJsMap();
  this.host = host;
}

defineClass(346, 1, {}, InternalHashCodeMap);
_.forEach_0 = function forEach_6(action){
  $forEach(this, action);
}
;
_.iterator = function iterator_12(){
  return new InternalHashCodeMap$1(this);
}
;
_.size_0 = 0;
var Ljava_util_InternalHashCodeMap_2_classLit = createForClass('java.util', 'InternalHashCodeMap', 346, Ljava_lang_Object_2_classLit);
function InternalHashCodeMap$1(this$0){
  this.this$01 = this$0;
  this.chains = this.this$01.backingMap.entries();
  this.chain = new Array;
}

defineClass(139, 1, {}, InternalHashCodeMap$1);
_.forEachRemaining = function forEachRemaining_10(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_11(){
  return this.lastEntry = this.chain[this.itemIndex++] , this.lastEntry;
}
;
_.hasNext_0 = function hasNext_10(){
  var current;
  if (this.itemIndex < this.chain.length) {
    return true;
  }
  current = this.chains.next();
  if (!current.done) {
    this.chain = current.value[1];
    this.itemIndex = 0;
    return true;
  }
  return false;
}
;
_.remove_2 = function remove_26(){
  $remove_6(this.this$01, this.lastEntry.getKey());
  this.itemIndex != 0 && --this.itemIndex;
}
;
_.itemIndex = 0;
_.lastEntry = null;
var Ljava_util_InternalHashCodeMap$1_2_classLit = createForAnonymousClass('java.util', 139, Ljava_lang_Object_2_classLit);
function $delete(this$static, key){
  var fn;
  fn = this$static['delete'];
  fn.call(this$static, key);
}

function $delete_0(this$static, key){
  var fn;
  fn = this$static['delete'];
  fn.call(this$static, key);
}

function $clinit_InternalJsMapFactory(){
  $clinit_InternalJsMapFactory = emptyMethod;
  jsMapCtor = getJsMapConstructor();
}

function canHandleObjectCreateAndProto(){
  if (!Object.create || !Object.getOwnPropertyNames) {
    return false;
  }
  var protoField = '__proto__';
  var map_0 = Object.create(null);
  if (map_0[protoField] !== undefined) {
    return false;
  }
  var keys_0 = Object.getOwnPropertyNames(map_0);
  if (keys_0.length != 0) {
    return false;
  }
  map_0[protoField] = 42;
  if (map_0[protoField] !== 42) {
    return false;
  }
  if (Object.getOwnPropertyNames(map_0).length == 0) {
    return false;
  }
  return true;
}

function getJsMapConstructor(){
  function isCorrectIterationProtocol(){
    try {
      return (new Map).entries().next().done;
    }
     catch (e) {
      return false;
    }
  }

  if (typeof Map === 'function' && Map.prototype.entries && isCorrectIterationProtocol()) {
    return Map;
  }
   else {
    return getJsMapPolyFill();
  }
}

function getJsMapPolyFill(){
  function Stringmap(){
    this.obj = this.createObject();
  }

  ;
  Stringmap.prototype.createObject = function(key){
    return Object.create(null);
  }
  ;
  Stringmap.prototype.get = function(key){
    return this.obj[key];
  }
  ;
  Stringmap.prototype.set = function(key, value_0){
    this.obj[key] = value_0;
  }
  ;
  Stringmap.prototype['delete'] = function(key){
    delete this.obj[key];
  }
  ;
  Stringmap.prototype.keys = function(){
    return Object.getOwnPropertyNames(this.obj);
  }
  ;
  Stringmap.prototype.entries = function(){
    var keys_0 = this.keys();
    var map_0 = this;
    var nextIndex = 0;
    return {'next':function(){
      if (nextIndex >= keys_0.length)
        return {'done':true};
      var key = keys_0[nextIndex++];
      return {'value':[key, map_0.get(key)], 'done':false};
    }
    };
  }
  ;
  if (!canHandleObjectCreateAndProto()) {
    Stringmap.prototype.createObject = function(){
      return {};
    }
    ;
    Stringmap.prototype.get = function(key){
      return this.obj[':' + key];
    }
    ;
    Stringmap.prototype.set = function(key, value_0){
      this.obj[':' + key] = value_0;
    }
    ;
    Stringmap.prototype['delete'] = function(key){
      delete this.obj[':' + key];
    }
    ;
    Stringmap.prototype.keys = function(){
      var result_0 = [];
      for (var key in this.obj) {
        key.charCodeAt(0) == 58 && result_0.push(key.substring(1));
      }
      return result_0;
    }
    ;
  }
  return Stringmap;
}

function newJsMap(){
  $clinit_InternalJsMapFactory();
  return new jsMapCtor;
}

var jsMapCtor;
function $contains_0(this$static, key){
  return !(this$static.backingMap.get(key) === undefined);
}

function $get_6(this$static, key){
  return this$static.backingMap.get(key);
}

function $put_8(this$static, key, value_0){
  var oldValue;
  oldValue = this$static.backingMap.get(key);
  this$static.backingMap.set(key, value_0 === undefined?null:value_0);
  if (oldValue === undefined) {
    ++this$static.size_0;
    structureChanged(this$static.host);
  }
   else {
    ++this$static.valueMod;
  }
  return oldValue;
}

function $remove_7(this$static, key){
  var value_0;
  value_0 = this$static.backingMap.get(key);
  if (value_0 === undefined) {
    ++this$static.valueMod;
  }
   else {
    $delete_0(this$static.backingMap, key);
    --this$static.size_0;
    structureChanged(this$static.host);
  }
  return value_0;
}

function InternalStringMap(host){
  this.backingMap = newJsMap();
  this.host = host;
}

defineClass(344, 1, {}, InternalStringMap);
_.forEach_0 = function forEach_7(action){
  $forEach(this, action);
}
;
_.iterator = function iterator_13(){
  return new InternalStringMap$1(this);
}
;
_.size_0 = 0;
_.valueMod = 0;
var Ljava_util_InternalStringMap_2_classLit = createForClass('java.util', 'InternalStringMap', 344, Ljava_lang_Object_2_classLit);
function InternalStringMap$1(this$0){
  this.this$01 = this$0;
  this.entries_0 = this.this$01.backingMap.entries();
  this.current = this.entries_0.next();
}

defineClass(137, 1, {}, InternalStringMap$1);
_.forEachRemaining = function forEachRemaining_11(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_12(){
  return this.last = this.current , this.current = this.entries_0.next() , new InternalStringMap$2(this.this$01, this.last, this.this$01.valueMod);
}
;
_.hasNext_0 = function hasNext_11(){
  return !this.current.done;
}
;
_.remove_2 = function remove_27(){
  $remove_7(this.this$01, this.last.value[0]);
}
;
var Ljava_util_InternalStringMap$1_2_classLit = createForAnonymousClass('java.util', 137, Ljava_lang_Object_2_classLit);
function InternalStringMap$2(this$0, val$entry, val$lastValueMod){
  this.this$01 = this$0;
  this.val$entry2 = val$entry;
  this.val$lastValueMod3 = val$lastValueMod;
}

defineClass(138, 445, $intern_3, InternalStringMap$2);
_.getKey = function getKey_2(){
  return this.val$entry2.value[0];
}
;
_.getValue = function getValue_2(){
  if (this.this$01.valueMod != this.val$lastValueMod3) {
    return $get_6(this.this$01, this.val$entry2.value[0]);
  }
  return this.val$entry2.value[1];
}
;
_.setValue = function setValue_2(object){
  return $put_8(this.this$01, this.val$entry2.value[0], object);
}
;
_.val$lastValueMod3 = 0;
var Ljava_util_InternalStringMap$2_2_classLit = createForAnonymousClass('java.util', 138, Ljava_util_AbstractMapEntry_2_classLit);
function $addToEnd(this$static){
  var tail;
  tail = this$static.this$01.head.prev;
  this$static.prev = tail;
  this$static.next_0 = this$static.this$01.head;
  tail.next_0 = this$static.this$01.head.prev = this$static;
}

function $remove_9(this$static){
  this$static.next_0.prev = this$static.prev;
  this$static.prev.next_0 = this$static.next_0;
  this$static.next_0 = this$static.prev = null;
}

function LinkedHashMap$ChainEntry(this$0){
  LinkedHashMap$ChainEntry_0.call(this, this$0, null, null);
}

function LinkedHashMap$ChainEntry_0(this$0, key, value_0){
  this.this$01 = this$0;
  AbstractMap$SimpleEntry.call(this, key, value_0);
}

defineClass(60, 115, {60:1, 16:1}, LinkedHashMap$ChainEntry, LinkedHashMap$ChainEntry_0);
var Ljava_util_LinkedHashMap$ChainEntry_2_classLit = createForClass('java.util', 'LinkedHashMap/ChainEntry', 60, Ljava_util_AbstractMap$SimpleEntry_2_classLit);
function $contains_1(this$static, o){
  if (instanceOf(o, 16)) {
    return $containsEntry(this$static.this$01, castTo(o, 16));
  }
  return false;
}

function LinkedHashMap$EntrySet(this$0){
  this.this$01 = this$0;
}

defineClass(322, 443, $intern_2, LinkedHashMap$EntrySet);
_.clear_0 = function clear_14(){
  $clear_1(this.this$01);
}
;
_.contains = function contains_10(o){
  return $contains_1(this, o);
}
;
_.iterator = function iterator_14(){
  return new LinkedHashMap$EntrySet$EntryIterator(this);
}
;
_.remove_1 = function remove_28(entry){
  var key;
  if ($contains_1(this, entry)) {
    key = castTo(entry, 16).getKey();
    $remove_3(this.this$01, key);
    return true;
  }
  return false;
}
;
_.size_1 = function size_21(){
  return this.this$01.map_0.size_1();
}
;
var Ljava_util_LinkedHashMap$EntrySet_2_classLit = createForClass('java.util', 'LinkedHashMap/EntrySet', 322, Ljava_util_AbstractSet_2_classLit);
function LinkedHashMap$EntrySet$EntryIterator(this$1){
  this.this$11 = this$1;
  this.next_0 = this$1.this$01.head.next_0;
  recordLastKnownStructure(this$1.this$01.map_0, this);
}

defineClass(323, 1, {}, LinkedHashMap$EntrySet$EntryIterator);
_.forEachRemaining = function forEachRemaining_12(consumer){
  $forEachRemaining(this, consumer);
}
;
_.next_1 = function next_13(){
  return checkStructuralChange(this.this$11.this$01.map_0, this) , checkCriticalElement(this.next_0 != this.this$11.this$01.head) , this.last = this.next_0 , this.next_0 = this.next_0.next_0 , this.last;
}
;
_.hasNext_0 = function hasNext_12(){
  return this.next_0 != this.this$11.this$01.head;
}
;
_.remove_2 = function remove_29(){
  $clinit_InternalPreconditions();
  checkCriticalState(!!this.last);
  checkStructuralChange(this.this$11.this$01.map_0, this);
  $remove_9(this.last);
  this.this$11.this$01.map_0.remove_0(this.last.key);
  recordLastKnownStructure(this.this$11.this$01.map_0, this);
  this.last = null;
}
;
var Ljava_util_LinkedHashMap$EntrySet$EntryIterator_2_classLit = createForClass('java.util', 'LinkedHashMap/EntrySet/EntryIterator', 323, Ljava_lang_Object_2_classLit);
function LinkedHashSet(){
  HashSet_1.call(this, new LinkedHashMap);
}

function LinkedHashSet_0(c){
  HashSet_1.call(this, new LinkedHashMap);
  this.addAll(c);
}

defineClass(62, 35, $intern_29, LinkedHashSet, LinkedHashSet_0);
var Ljava_util_LinkedHashSet_2_classLit = createForClass('java.util', 'LinkedHashSet', 62, Ljava_util_HashSet_2_classLit);
function NoSuchElementException(){
  RuntimeException.call(this);
}

function NoSuchElementException_0(s){
  RuntimeException_0.call(this, s);
}

defineClass(50, 11, $intern_1, NoSuchElementException, NoSuchElementException_0);
var Ljava_util_NoSuchElementException_2_classLit = createForClass('java.util', 'NoSuchElementException', 50, Ljava_lang_RuntimeException_2_classLit);
function equals_18(a, b){
  return maskUndefined(a) === maskUndefined(b) || a != null && equals_Ljava_lang_Object__Z__devirtual$(a, b);
}

function hashCode_19(o){
  return o != null?hashCode__I__devirtual$(o):0;
}

function requireNonNull(obj){
  if (obj == null) {
    throw toJs(new NullPointerException);
  }
  return obj;
}

function $clinit_Optional(){
  $clinit_Optional = emptyMethod;
  EMPTY = new Optional(null);
}

function $map(this$static, mapper){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(mapper);
  if (this$static.ref != null) {
    return ofNullable(mapper.apply_0(this$static.ref));
  }
  return EMPTY;
}

function $orElse(this$static, other){
  return this$static.ref != null?this$static.ref:other;
}

function Optional(ref){
  $clinit_Optional();
  this.ref = ref;
}

function ofNullable(value_0){
  $clinit_Optional();
  return value_0 == null?EMPTY:new Optional(checkCriticalNotNull(value_0));
}

defineClass(67, 1, {67:1}, Optional);
_.equals_0 = function equals_19(obj){
  var other;
  if (obj === this) {
    return true;
  }
  if (!instanceOf(obj, 67)) {
    return false;
  }
  other = castTo(obj, 67);
  return equals_18(this.ref, other.ref);
}
;
_.hashCode_0 = function hashCode_20(){
  return hashCode_19(this.ref);
}
;
_.toString_0 = function toString_36(){
  return this.ref != null?'Optional.of(' + valueOf_1(this.ref) + ')':'Optional.empty()';
}
;
var EMPTY;
var Ljava_util_Optional_2_classLit = createForClass('java.util', 'Optional', 67, Ljava_lang_Object_2_classLit);
function $forEachRemaining_0(this$static, consumer){
  while (this$static.tryAdvance(consumer))
  ;
}

function checkCriticalArrayBounds(start_0, end, length_0){
  if (start_0 > end || start_0 < 0 || end > length_0) {
    throw toJs(new ArrayIndexOutOfBoundsException('fromIndex: ' + start_0 + ', toIndex: ' + end + ', length: ' + length_0));
  }
}

defineClass(371, 1, {});
_.forEachRemaining = function forEachRemaining_13(consumer){
  $forEachRemaining_0(this, consumer);
}
;
_.characteristics_0 = function characteristics_0(){
  return this.characteristics;
}
;
_.estimateSize_0 = function estimateSize(){
  return this.sizeEstimate;
}
;
_.characteristics = 0;
_.sizeEstimate = 0;
var Ljava_util_Spliterators$BaseSpliterator_2_classLit = createForClass('java.util', 'Spliterators/BaseSpliterator', 371, Ljava_lang_Object_2_classLit);
function Spliterators$AbstractSpliterator(size_0, characteristics){
  this.sizeEstimate = size_0;
  this.characteristics = (characteristics & 64) != 0?characteristics | $intern_20:characteristics;
}

defineClass(93, 371, {});
var Ljava_util_Spliterators$AbstractSpliterator_2_classLit = createForClass('java.util', 'Spliterators/AbstractSpliterator', 93, Ljava_util_Spliterators$BaseSpliterator_2_classLit);
function $forEachRemaining_1(this$static, consumer){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  while (this$static.index_0 < this$static.limit) {
    this$static.consume(consumer, this$static.index_0++);
  }
}

function $tryAdvance(this$static, consumer){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  if (this$static.index_0 < this$static.limit) {
    this$static.consume(consumer, this$static.index_0++);
    return true;
  }
  return false;
}

defineClass(343, 1, {});
_.forEachRemaining = function forEachRemaining_14(consumer){
  $forEachRemaining_0(this, consumer);
}
;
_.characteristics_0 = function characteristics_1(){
  return this.characteristics;
}
;
_.estimateSize_0 = function estimateSize_0(){
  return this.limit - this.index_0;
}
;
_.characteristics = 0;
_.index_0 = 0;
_.limit = 0;
var Ljava_util_Spliterators$BaseArraySpliterator_2_classLit = createForClass('java.util', 'Spliterators/BaseArraySpliterator', 343, Ljava_lang_Object_2_classLit);
function $consume(this$static, consumer, index_0){
  consumer.accept_0(this$static.array[index_0]);
}

function Spliterators$ArraySpliterator(array, from, limit, characteristics){
  this.index_0 = from;
  this.limit = limit;
  this.characteristics = characteristics | 64 | $intern_20;
  this.array = array;
}

defineClass(116, 343, {}, Spliterators$ArraySpliterator);
_.consume = function consume(consumer, index_0){
  $consume(this, castTo(consumer, 20), index_0);
}
;
_.forEachRemaining = function forEachRemaining_15(consumer){
  $forEachRemaining_1(this, consumer);
}
;
_.tryAdvance = function tryAdvance(consumer){
  return $tryAdvance(this, consumer);
}
;
var Ljava_util_Spliterators$ArraySpliterator_2_classLit = createForClass('java.util', 'Spliterators/ArraySpliterator', 116, Ljava_util_Spliterators$BaseArraySpliterator_2_classLit);
function Spliterators$ConsumerIterator(spliterator){
  this.spliterator = ($clinit_InternalPreconditions() , checkCriticalNotNull(spliterator) , spliterator);
}

defineClass(136, 1, $intern_5, Spliterators$ConsumerIterator);
_.forEachRemaining = function forEachRemaining_16(consumer){
  $forEachRemaining(this, consumer);
}
;
_.remove_2 = function remove_30(){
  $remove_8();
}
;
_.accept_0 = function accept_12(element){
  this.nextElement = element;
}
;
_.hasNext_0 = function hasNext_13(){
  return this.hasElement || (this.hasElement = this.spliterator.tryAdvance(this)) , this.hasElement;
}
;
_.next_1 = function next_14(){
  var element;
  checkCriticalElement((this.hasElement || (this.hasElement = this.spliterator.tryAdvance(this)) , this.hasElement));
  this.hasElement = false;
  element = this.nextElement;
  this.nextElement = null;
  return element;
}
;
_.hasElement = false;
var Ljava_util_Spliterators$ConsumerIterator_2_classLit = createForClass('java.util', 'Spliterators/ConsumerIterator', 136, Ljava_lang_Object_2_classLit);
function $initIterator(this$static){
  if (!this$static.it) {
    this$static.it = this$static.collection.iterator();
    this$static.estimateSize = this$static.collection.size_1();
  }
}

function Spliterators$IteratorSpliterator(collection, characteristics){
  this.collection = ($clinit_InternalPreconditions() , checkCriticalNotNull(collection) , collection);
  this.characteristics = (characteristics & 4096) == 0?characteristics | 64 | $intern_20:characteristics;
}

defineClass(61, 1, {}, Spliterators$IteratorSpliterator);
_.characteristics_0 = function characteristics_2(){
  return this.characteristics;
}
;
_.estimateSize_0 = function estimateSize_1(){
  $initIterator(this);
  return this.estimateSize;
}
;
_.forEachRemaining = function forEachRemaining_17(consumer){
  $initIterator(this);
  this.it.forEachRemaining(consumer);
}
;
_.tryAdvance = function tryAdvance_0(consumer){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(consumer);
  $initIterator(this);
  if (this.it.hasNext_0()) {
    consumer.accept_0(this.it.next_1());
    return true;
  }
  return false;
}
;
_.characteristics = 0;
_.estimateSize = 0;
var Ljava_util_Spliterators$IteratorSpliterator_2_classLit = createForClass('java.util', 'Spliterators/IteratorSpliterator', 61, Ljava_lang_Object_2_classLit);
function $add_4(this$static, newElement){
  !this$static.builder?(this$static.builder = new StringBuilder_1(this$static.prefix)):$append_5(this$static.builder, this$static.delimiter);
  $append_3(this$static.builder, newElement);
  return this$static;
}

function $toString_3(this$static){
  return !this$static.builder?this$static.emptyValue:this$static.suffix.length == 0?this$static.builder.string:this$static.builder.string + ('' + this$static.suffix);
}

function StringJoiner(delimiter, prefix, suffix){
  this.delimiter = toString_26(delimiter);
  this.prefix = toString_26(prefix);
  this.suffix = toString_26(suffix);
  this.emptyValue = this.prefix + ('' + this.suffix);
}

defineClass(45, 1, {45:1}, StringJoiner);
_.toString_0 = function toString_37(){
  return $toString_3(this);
}
;
var Ljava_util_StringJoiner_2_classLit = createForClass('java.util', 'StringJoiner', 45, Ljava_lang_Object_2_classLit);
function Function$lambda$0$Type(){
}

defineClass(127, 1, {}, Function$lambda$0$Type);
_.apply_0 = function apply_21(t){
  return t;
}
;
var Ljava_util_function_Function$lambda$0$Type_2_classLit = createForAnonymousClass('java.util.function', 127, Ljava_lang_Object_2_classLit);
function Predicate$lambda$2$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(29, 1, {}, Predicate$lambda$2$Type);
_.negate = function negate_11(){
  return new Predicate$lambda$2$Type(this);
}
;
_.test_0 = function test_11(t){
  return !this.$$outer_0.test_0(t);
}
;
var Ljava_util_function_Predicate$lambda$2$Type_2_classLit = createForAnonymousClass('java.util.function', 29, Ljava_lang_Object_2_classLit);
function $clinit_Level(){
  $clinit_Level = emptyMethod;
  new Level$LevelAll;
  INFO = new Level$LevelInfo;
  SEVERE = new Level$LevelSevere;
  WARNING = new Level$LevelWarning;
}

defineClass(436, 1, $intern_28);
_.getName = function getName_1(){
  return 'DUMMY';
}
;
_.intValue = function intValue(){
  return -1;
}
;
_.toString_0 = function toString_38(){
  return this.getName();
}
;
var INFO, SEVERE, WARNING;
var Ljava_util_logging_Level_2_classLit = createForClass('java.util.logging', 'Level', 436, Ljava_lang_Object_2_classLit);
function Level$LevelAll(){
}

defineClass(317, 436, $intern_28, Level$LevelAll);
_.getName = function getName_2(){
  return 'ALL';
}
;
_.intValue = function intValue_0(){
  return -2147483648;
}
;
var Ljava_util_logging_Level$LevelAll_2_classLit = createForClass('java.util.logging', 'Level/LevelAll', 317, Ljava_util_logging_Level_2_classLit);
function Level$LevelInfo(){
}

defineClass(318, 436, $intern_28, Level$LevelInfo);
_.getName = function getName_3(){
  return 'INFO';
}
;
_.intValue = function intValue_1(){
  return 800;
}
;
var Ljava_util_logging_Level$LevelInfo_2_classLit = createForClass('java.util.logging', 'Level/LevelInfo', 318, Ljava_util_logging_Level_2_classLit);
function Level$LevelSevere(){
}

defineClass(319, 436, $intern_28, Level$LevelSevere);
_.getName = function getName_4(){
  return 'SEVERE';
}
;
_.intValue = function intValue_2(){
  return 1000;
}
;
var Ljava_util_logging_Level$LevelSevere_2_classLit = createForClass('java.util.logging', 'Level/LevelSevere', 319, Ljava_util_logging_Level_2_classLit);
function Level$LevelWarning(){
}

defineClass(320, 436, $intern_28, Level$LevelWarning);
_.getName = function getName_5(){
  return 'WARNING';
}
;
_.intValue = function intValue_3(){
  return 900;
}
;
var Ljava_util_logging_Level$LevelWarning_2_classLit = createForClass('java.util.logging', 'Level/LevelWarning', 320, Ljava_util_logging_Level_2_classLit);
function $addLoggerImpl(this$static, logger){
  this$static.loggerMap.put_0(($clinit_Logger() , LOGGING_OFF)?null:logger.name_0, logger);
}

function $ensureLogger(this$static, name_0){
  var logger, newLogger, name_1, parentName;
  logger = castTo(this$static.loggerMap.get_0(name_0), 70);
  if (!logger) {
    newLogger = new Logger(name_0);
    name_1 = ($clinit_Logger() , LOGGING_OFF)?null:newLogger.name_0;
    parentName = $substring_0(name_1, 0, $wnd.Math.max(0, $lastIndexOf(name_1, fromCodePoint(46))));
    $setParent(newLogger, $ensureLogger(this$static, parentName));
    this$static.loggerMap.put_0(LOGGING_OFF?null:newLogger.name_0, newLogger);
    return newLogger;
  }
  return logger;
}

function LogManager(){
  this.loggerMap = new HashMap;
}

function getLogManager(){
  var rootLogger;
  if (!singleton_0) {
    singleton_0 = new LogManager;
    rootLogger = new Logger('');
    $setLevel(rootLogger, ($clinit_Level() , INFO));
    $addLoggerImpl(singleton_0, rootLogger);
  }
  return singleton_0;
}

defineClass(292, 1, {}, LogManager);
var singleton_0;
var Ljava_util_logging_LogManager_2_classLit = createForClass('java.util.logging', 'LogManager', 292, Ljava_lang_Object_2_classLit);
function LogRecord(level, msg){
  $clinit_System();
  fromDouble_0(Date.now());
}

defineClass(345, 1, $intern_28, LogRecord);
var Ljava_util_logging_LogRecord_2_classLit = createForClass('java.util.logging', 'LogRecord', 345, Ljava_lang_Object_2_classLit);
function $clinit_Logger(){
  $clinit_Logger = emptyMethod;
  LOGGING_OFF = true;
  ALL_ENABLED = false;
  INFO_ENABLED = false;
  WARNING_ENABLED = false;
  SEVERE_ENABLED = false;
}

function $actuallyLog(this$static, record){
  var handler, handler$array, handler$array0, handler$index, handler$index0, handler$max, handler$max0, logger;
  for (handler$array0 = $getHandlers(this$static) , handler$index0 = 0 , handler$max0 = handler$array0.length; handler$index0 < handler$max0; ++handler$index0) {
    handler = handler$array0[handler$index0];
    null.$_nullMethod();
  }
  logger = !LOGGING_OFF && this$static.useParentHandlers?LOGGING_OFF?null:this$static.parent_0:null;
  while (logger) {
    for (handler$array = $getHandlers(logger) , handler$index = 0 , handler$max = handler$array.length; handler$index < handler$max; ++handler$index) {
      handler = handler$array[handler$index];
      null.$_nullMethod();
    }
    logger = !LOGGING_OFF && logger.useParentHandlers?LOGGING_OFF?null:logger.parent_0:null;
  }
}

function $getEffectiveLevel(this$static){
  var effectiveLevel, logger;
  if (this$static.level) {
    return this$static.level;
  }
  logger = LOGGING_OFF?null:this$static.parent_0;
  while (logger) {
    effectiveLevel = LOGGING_OFF?null:logger.level;
    if (effectiveLevel) {
      return effectiveLevel;
    }
    logger = LOGGING_OFF?null:logger.parent_0;
  }
  return $clinit_Level() , INFO;
}

function $getHandlers(this$static){
  if (LOGGING_OFF) {
    return initUnidimensionalArray(Ljava_util_logging_Handler_2_classLit, $intern_30, 88, 0, 0, 1);
  }
  return castTo(this$static.handlers.toArray_0(initUnidimensionalArray(Ljava_util_logging_Handler_2_classLit, $intern_30, 88, this$static.handlers.size_1(), 0, 1)), 413);
}

function $getName_0(this$static){
  return LOGGING_OFF?null:this$static.name_0;
}

function $isLoggable(this$static, messageLevel){
  return ALL_ENABLED?messageLevel.intValue() >= $getEffectiveLevel(this$static).intValue():INFO_ENABLED?messageLevel.intValue() >= ($clinit_Level() , INFO).intValue():WARNING_ENABLED?messageLevel.intValue() >= ($clinit_Level() , WARNING).intValue():SEVERE_ENABLED && messageLevel.intValue() >= ($clinit_Level() , SEVERE).intValue();
}

function $log_1(this$static, level, msg, thrown){
  var record;
  (ALL_ENABLED?level.intValue() >= $getEffectiveLevel(this$static).intValue():INFO_ENABLED?level.intValue() >= ($clinit_Level() , INFO).intValue():WARNING_ENABLED?level.intValue() >= ($clinit_Level() , WARNING).intValue():SEVERE_ENABLED && level.intValue() >= ($clinit_Level() , SEVERE).intValue()) && (record = new LogRecord(level, msg) , $actuallyLog(this$static, record) , undefined);
}

function $setLevel(this$static, newLevel){
  if (LOGGING_OFF) {
    return;
  }
  this$static.level = newLevel;
}

function $setParent(this$static, newParent){
  if (LOGGING_OFF) {
    return;
  }
  !!newParent && (this$static.parent_0 = newParent);
}

function Logger(name_0){
  $clinit_Logger();
  if (LOGGING_OFF) {
    return;
  }
  this.name_0 = name_0;
  this.useParentHandlers = true;
  this.handlers = new ArrayList;
}

function getLogger_0(name_0){
  $clinit_Logger();
  if (LOGGING_OFF) {
    return new Logger(null);
  }
  return $ensureLogger(getLogManager(), name_0);
}

defineClass(70, 1, {70:1}, Logger);
_.useParentHandlers = false;
var ALL_ENABLED = false, INFO_ENABLED = false, LOGGING_OFF = false, SEVERE_ENABLED = false, WARNING_ENABLED = false;
var Ljava_util_logging_Logger_2_classLit = createForClass('java.util.logging', 'Logger', 70, Ljava_lang_Object_2_classLit);
function of(supplier, accumulator, combiner, finisher, characteristics){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(supplier);
  checkCriticalNotNull(accumulator);
  checkCriticalNotNull(combiner);
  checkCriticalNotNull(finisher);
  checkCriticalNotNull(characteristics);
  return new CollectorImpl(supplier, accumulator, combiner, finisher);
}

function of_0(supplier, accumulator, combiner, characteristics){
  $clinit_InternalPreconditions();
  checkCriticalNotNull(supplier);
  checkCriticalNotNull(accumulator);
  checkCriticalNotNull(combiner);
  checkCriticalNotNull(characteristics);
  return new CollectorImpl(supplier, accumulator, combiner, new Function$lambda$0$Type);
}

function $clinit_Collector$Characteristics(){
  $clinit_Collector$Characteristics = emptyMethod;
  CONCURRENT = new Collector$Characteristics('CONCURRENT', 0);
  IDENTITY_FINISH = new Collector$Characteristics('IDENTITY_FINISH', 1);
  UNORDERED = new Collector$Characteristics('UNORDERED', 2);
}

function Collector$Characteristics(enum$name, enum$ordinal){
  Enum.call(this, enum$name, enum$ordinal);
}

function values_6(){
  $clinit_Collector$Characteristics();
  return stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [CONCURRENT, IDENTITY_FINISH, UNORDERED]);
}

defineClass(14, 33, {3:1, 32:1, 33:1, 14:1}, Collector$Characteristics);
var CONCURRENT, IDENTITY_FINISH, UNORDERED;
var Ljava_util_stream_Collector$Characteristics_2_classLit = createForEnum('java.util.stream', 'Collector/Characteristics', 14, Ljava_lang_Enum_2_classLit, values_6);
function CollectorImpl(supplier, accumulator, combiner, finisher){
  this.supplier = supplier;
  this.accumulator = accumulator;
  $clinit_Collections();
  this.combiner = combiner;
  this.finisher = finisher;
}

defineClass(149, 1, {}, CollectorImpl);
_.accumulator_0 = function accumulator_0(){
  return this.accumulator;
}
;
_.combiner_0 = function combiner_0(){
  return this.combiner;
}
;
_.finisher_0 = function finisher_0(){
  return this.finisher;
}
;
_.supplier_0 = function supplier_0(){
  return this.supplier;
}
;
var Ljava_util_stream_CollectorImpl_2_classLit = createForClass('java.util.stream', 'CollectorImpl', 149, Ljava_lang_Object_2_classLit);
function Collectors$10methodref$merge$Type(){
}

defineClass(73, 1, {}, Collectors$10methodref$merge$Type);
var Ljava_util_stream_Collectors$10methodref$merge$Type_2_classLit = createForAnonymousClass('java.util.stream', 73, Ljava_lang_Object_2_classLit);
function Collectors$11methodref$toString$Type(){
}

defineClass(74, 1, {}, Collectors$11methodref$toString$Type);
_.apply_0 = function apply_22(arg0){
  return $toString_3(castTo(arg0, 45));
}
;
var Ljava_util_stream_Collectors$11methodref$toString$Type_2_classLit = createForAnonymousClass('java.util.stream', 74, Ljava_lang_Object_2_classLit);
function Collectors$20methodref$add$Type(){
}

defineClass(21, 1, {}, Collectors$20methodref$add$Type);
_.accept = function accept_13(arg0, arg1){
  castTo(arg0, 19).add_0(arg1);
}
;
var Ljava_util_stream_Collectors$20methodref$add$Type_2_classLit = createForAnonymousClass('java.util.stream', 21, Ljava_lang_Object_2_classLit);
function Collectors$21methodref$ctor$Type(){
}

defineClass(23, 1, $intern_12, Collectors$21methodref$ctor$Type);
_.get_1 = function get_82(){
  return new ArrayList;
}
;
var Ljava_util_stream_Collectors$21methodref$ctor$Type_2_classLit = createForAnonymousClass('java.util.stream', 23, Ljava_lang_Object_2_classLit);
function Collectors$23methodref$ctor$Type(){
}

defineClass(63, 1, $intern_12, Collectors$23methodref$ctor$Type);
_.get_1 = function get_83(){
  return new HashSet;
}
;
var Ljava_util_stream_Collectors$23methodref$ctor$Type_2_classLit = createForAnonymousClass('java.util.stream', 63, Ljava_lang_Object_2_classLit);
function Collectors$24methodref$add$Type(){
}

defineClass(64, 1, {}, Collectors$24methodref$add$Type);
_.accept = function accept_14(arg0, arg1){
  $add_3(castTo(arg0, 35), arg1);
}
;
var Ljava_util_stream_Collectors$24methodref$add$Type_2_classLit = createForAnonymousClass('java.util.stream', 64, Ljava_lang_Object_2_classLit);
function Collectors$9methodref$add$Type(){
}

defineClass(72, 1, {}, Collectors$9methodref$add$Type);
_.accept = function accept_15(arg0, arg1){
  $add_4(castTo(arg0, 45), castTo(arg1, 84));
}
;
var Ljava_util_stream_Collectors$9methodref$add$Type_2_classLit = createForAnonymousClass('java.util.stream', 72, Ljava_lang_Object_2_classLit);
function Collectors$lambda$15$Type(delimiter_0, prefix_1, suffix_2){
  this.delimiter_0 = delimiter_0;
  this.prefix_1 = prefix_1;
  this.suffix_2 = suffix_2;
}

defineClass(71, 1, $intern_12, Collectors$lambda$15$Type);
_.get_1 = function get_84(){
  return new StringJoiner(this.delimiter_0, this.prefix_1, this.suffix_2);
}
;
var Ljava_util_stream_Collectors$lambda$15$Type_2_classLit = createForAnonymousClass('java.util.stream', 71, Ljava_lang_Object_2_classLit);
function Collectors$lambda$42$Type(){
}

defineClass(22, 1, {}, Collectors$lambda$42$Type);
var Ljava_util_stream_Collectors$lambda$42$Type_2_classLit = createForAnonymousClass('java.util.stream', 22, Ljava_lang_Object_2_classLit);
function Collectors$lambda$50$Type(){
}

defineClass(65, 1, {}, Collectors$lambda$50$Type);
var Ljava_util_stream_Collectors$lambda$50$Type_2_classLit = createForAnonymousClass('java.util.stream', 65, Ljava_lang_Object_2_classLit);
function Collectors$lambda$51$Type(){
}

defineClass(66, 1, {}, Collectors$lambda$51$Type);
_.apply_0 = function apply_23(arg0){
  return castTo(arg0, 35);
}
;
var Ljava_util_stream_Collectors$lambda$51$Type_2_classLit = createForAnonymousClass('java.util.stream', 66, Ljava_lang_Object_2_classLit);
function $close(this$static){
  if (!this$static.root_0) {
    this$static.terminated = true;
    $runClosers(this$static);
  }
   else {
    this$static.root_0.close_0();
  }
}

function $runClosers(this$static){
  var e, i, size_0, suppressed, throwables;
  throwables = new ArrayList;
  this$static.onClose.forEach_0(new TerminatableStream$lambda$0$Type(throwables));
  this$static.onClose.clear_0();
  if (throwables.array.length != 0) {
    e = ($clinit_InternalPreconditions() , checkCriticalElementIndex(0, throwables.array.length) , castTo(throwables.array[0], 10));
    for (i = 1 , size_0 = throwables.array.length; i < size_0; ++i) {
      suppressed = (checkCriticalElementIndex(i, throwables.array.length) , castTo(throwables.array[i], 10));
      suppressed != e && $addSuppressed(e, suppressed);
    }
    if (instanceOf(e, 11)) {
      throw toJs(castTo(e, 11));
    }
    if (instanceOf(e, 55)) {
      throw toJs(castTo(e, 55));
    }
  }
}

function $terminate(this$static){
  if (!this$static.root_0) {
    $throwIfTerminated(this$static);
    this$static.terminated = true;
  }
   else {
    $terminate(this$static.root_0);
  }
}

function $throwIfTerminated(this$static){
  if (this$static.root_0) {
    $throwIfTerminated(this$static.root_0);
  }
   else if (this$static.terminated) {
    throw toJs(new IllegalStateException_0("Stream already terminated, can't be modified or used"));
  }
}

function TerminatableStream(previous){
  if (!previous) {
    this.root_0 = null;
    this.onClose = new ArrayList;
  }
   else {
    this.root_0 = previous;
    this.onClose = null;
  }
}

function lambda$0_2(throwables_0){
  var e;
  try {
    null.$_nullMethod();
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 10)) {
      e = $e0;
      setCheck(throwables_0.array, throwables_0.array.length, e);
    }
     else 
      throw toJs($e0);
  }
}

defineClass(370, 1, {});
_.terminated = false;
var Ljava_util_stream_TerminatableStream_2_classLit = createForClass('java.util.stream', 'TerminatableStream', 370, Ljava_lang_Object_2_classLit);
function $clinit_StreamImpl(){
  $clinit_StreamImpl = emptyMethod;
  NULL_CONSUMER = new StreamImpl$lambda$0$Type;
}

function $collect(this$static, collector){
  var lastArg;
  return collector.finisher_0().apply_0($reduce(this$static, collector.supplier_0().get_1(), (lastArg = new StreamImpl$lambda$4$Type(collector) , collector.combiner_0() , lastArg)));
}

function $filter(this$static, predicate){
  $throwIfTerminated(this$static);
  return new StreamImpl(this$static, new StreamImpl$FilterSpliterator(predicate, this$static.spliterator));
}

function $reduce(this$static, identity, accumulator){
  var consumer;
  $terminate(this$static);
  consumer = new StreamImpl$ValueConsumer;
  consumer.value_0 = identity;
  this$static.spliterator.forEachRemaining(new StreamImpl$lambda$5$Type(consumer, accumulator));
  return consumer.value_0;
}

function $sorted(this$static, comparator){
  var sortedSpliterator;
  $throwIfTerminated(this$static);
  sortedSpliterator = new StreamImpl$5(this$static, this$static.spliterator.estimateSize_0(), this$static.spliterator.characteristics_0() | 4, comparator);
  return new StreamImpl(this$static, sortedSpliterator);
}

function $toArray_0(this$static, generator){
  var collected;
  collected = castTo($collect(this$static, of_0(new Collectors$21methodref$ctor$Type, new Collectors$20methodref$add$Type, new Collectors$lambda$42$Type, stampJavaTypeInfo(getClassLiteralForArray(Ljava_util_stream_Collector$Characteristics_2_classLit, 1), $intern_0, 14, 0, [($clinit_Collector$Characteristics() , IDENTITY_FINISH)]))), 15);
  return collected.toArray_0(generator.apply_1(collected.size_1()));
}

function StreamImpl(prev, spliterator){
  $clinit_StreamImpl();
  TerminatableStream.call(this, prev);
  this.spliterator = spliterator;
}

function lambda$4_0(collector_0, a_1, t_2){
  $clinit_StreamImpl();
  collector_0.accumulator_0().accept(a_1, t_2);
  return a_1;
}

function lambda$5_0(consumer_0, accumulator_1, item_2){
  $clinit_StreamImpl();
  $accept(consumer_0, accumulator_1.apply_2(consumer_0.value_0, item_2));
}

defineClass(42, 370, {465:1}, StreamImpl);
_.close_0 = function close_1(){
  $close(this);
}
;
_.allMatch = function allMatch(predicate){
  return !$filter(this, predicate.negate()).spliterator_0().tryAdvance(NULL_CONSUMER);
}
;
_.anyMatch = function anyMatch(predicate){
  return ($throwIfTerminated(this) , new StreamImpl(this, new StreamImpl$FilterSpliterator(predicate, this.spliterator))).spliterator_0().tryAdvance(NULL_CONSUMER);
}
;
_.collect_0 = function collect_1(collector){
  return $collect(this, collector);
}
;
_.count = function count_0(){
  var count;
  $terminate(this);
  count = 0;
  while (this.spliterator.tryAdvance(new StreamImpl$lambda$1$Type)) {
    count = add_2(count, 1);
  }
  return count;
}
;
_.filter_0 = function filter_0(predicate){
  return $filter(this, predicate);
}
;
_.findFirst = function findFirst(){
  var holder;
  $terminate(this);
  holder = new StreamImpl$ValueConsumer;
  if (this.spliterator.tryAdvance(holder)) {
    return $clinit_Optional() , new Optional(checkCriticalNotNull(holder.value_0));
  }
  return $clinit_Optional() , $clinit_Optional() , EMPTY;
}
;
_.flatMap = function flatMap(mapper){
  var flatMapSpliterator, spliteratorOfStreams;
  $throwIfTerminated(this);
  spliteratorOfStreams = new StreamImpl$MapToObjSpliterator(mapper, this.spliterator);
  flatMapSpliterator = new StreamImpl$1(spliteratorOfStreams);
  return new StreamImpl(this, flatMapSpliterator);
}
;
_.forEach_0 = function forEach_8(action){
  $terminate(this);
  this.spliterator.forEachRemaining(action);
}
;
_.iterator = function iterator_15(){
  return new Spliterators$ConsumerIterator(($terminate(this) , this.spliterator));
}
;
_.map_1 = function map_1(mapper){
  $throwIfTerminated(this);
  return new StreamImpl(this, new StreamImpl$MapToObjSpliterator(mapper, this.spliterator));
}
;
_.sorted = function sorted(){
  var c;
  $throwIfTerminated(this);
  c = ($clinit_Comparators() , $clinit_Comparators() , NATURAL_ORDER);
  return $sorted(this, c);
}
;
_.sorted_0 = function sorted_0(comparator){
  return $sorted(this, comparator);
}
;
_.spliterator_0 = function spliterator_4(){
  return $terminate(this) , this.spliterator;
}
;
_.toArray = function toArray_5(){
  return $toArray_0(this, new StreamImpl$0methodref$lambda$2$Type);
}
;
var NULL_CONSUMER;
var Ljava_util_stream_StreamImpl_2_classLit = createForClass('java.util.stream', 'StreamImpl', 42, Ljava_util_stream_TerminatableStream_2_classLit);
function StreamImpl$0methodref$lambda$2$Type(){
}

defineClass(376, 1, {}, StreamImpl$0methodref$lambda$2$Type);
_.apply_1 = function apply_24(arg0){
  return $clinit_StreamImpl() , initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, arg0, 5, 1);
}
;
var Ljava_util_stream_StreamImpl$0methodref$lambda$2$Type_2_classLit = createForAnonymousClass('java.util.stream', 376, Ljava_lang_Object_2_classLit);
function $advanceToNextSpliterator(this$static){
  while (!this$static.next_0) {
    if (!this$static.val$spliteratorOfStreams5.tryAdvance(new StreamImpl$1$lambda$0$Type(this$static))) {
      return false;
    }
  }
  return true;
}

function $lambda$0(this$static, n_0){
  if (n_0) {
    this$static.nextStream = n_0;
    this$static.next_0 = n_0.spliterator_0();
  }
}

function StreamImpl$1(val$spliteratorOfStreams){
  this.val$spliteratorOfStreams5 = val$spliteratorOfStreams;
  Spliterators$AbstractSpliterator.call(this, {l:$intern_14, m:$intern_14, h:524287}, 0);
}

defineClass(379, 93, {}, StreamImpl$1);
_.tryAdvance = function tryAdvance_1(action){
  while ($advanceToNextSpliterator(this)) {
    if (this.next_0.tryAdvance(action)) {
      return true;
    }
     else {
      this.nextStream.close_0();
      this.nextStream = null;
      this.next_0 = null;
    }
  }
  return false;
}
;
var Ljava_util_stream_StreamImpl$1_2_classLit = createForAnonymousClass('java.util.stream', 379, Ljava_util_Spliterators$AbstractSpliterator_2_classLit);
function StreamImpl$1$lambda$0$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(380, 1, $intern_5, StreamImpl$1$lambda$0$Type);
_.accept_0 = function accept_16(arg0){
  $lambda$0(this.$$outer_0, castTo(arg0, 465));
}
;
var Ljava_util_stream_StreamImpl$1$lambda$0$Type_2_classLit = createForAnonymousClass('java.util.stream', 380, Ljava_lang_Object_2_classLit);
function StreamImpl$5(this$0, $anonymous0, $anonymous1, val$comparator){
  this.this$01 = this$0;
  this.val$comparator5 = val$comparator;
  Spliterators$AbstractSpliterator.call(this, $anonymous0, $anonymous1);
}

defineClass(381, 93, {}, StreamImpl$5);
_.tryAdvance = function tryAdvance_2(action){
  var list;
  if (!this.ordered) {
    list = new ArrayList;
    this.this$01.spliterator.forEachRemaining(new StreamImpl$5$2methodref$add$Type(list));
    $clinit_Collections();
    list.sort_0(this.val$comparator5);
    this.ordered = list.spliterator_0();
  }
  return this.ordered.tryAdvance(action);
}
;
_.ordered = null;
var Ljava_util_stream_StreamImpl$5_2_classLit = createForAnonymousClass('java.util.stream', 381, Ljava_util_Spliterators$AbstractSpliterator_2_classLit);
function StreamImpl$5$2methodref$add$Type($$outer_0){
  this.$$outer_0 = $$outer_0;
}

defineClass(382, 1, $intern_5, StreamImpl$5$2methodref$add$Type);
_.accept_0 = function accept_17(arg0){
  this.$$outer_0.add_0(arg0);
}
;
var Ljava_util_stream_StreamImpl$5$2methodref$add$Type_2_classLit = createForAnonymousClass('java.util.stream', 382, Ljava_lang_Object_2_classLit);
function $lambda$0_0(this$static, action_1, item_1){
  if (this$static.filter.test_0(item_1)) {
    this$static.found = true;
    action_1.accept_0(item_1);
  }
}

function StreamImpl$FilterSpliterator(filter, original){
  Spliterators$AbstractSpliterator.call(this, original.estimateSize_0(), original.characteristics_0() & -16449);
  $clinit_InternalPreconditions();
  checkCriticalNotNull(filter);
  this.filter = filter;
  this.original = original;
}

defineClass(146, 93, {}, StreamImpl$FilterSpliterator);
_.tryAdvance = function tryAdvance_3(action){
  this.found = false;
  while (!this.found && this.original.tryAdvance(new StreamImpl$FilterSpliterator$lambda$0$Type(this, action)))
  ;
  return this.found;
}
;
_.found = false;
var Ljava_util_stream_StreamImpl$FilterSpliterator_2_classLit = createForClass('java.util.stream', 'StreamImpl/FilterSpliterator', 146, Ljava_util_Spliterators$AbstractSpliterator_2_classLit);
function StreamImpl$FilterSpliterator$lambda$0$Type($$outer_0, action_1){
  this.$$outer_0 = $$outer_0;
  this.action_1 = action_1;
}

defineClass(373, 1, $intern_5, StreamImpl$FilterSpliterator$lambda$0$Type);
_.accept_0 = function accept_18(arg0){
  $lambda$0_0(this.$$outer_0, this.action_1, arg0);
}
;
var Ljava_util_stream_StreamImpl$FilterSpliterator$lambda$0$Type_2_classLit = createForAnonymousClass('java.util.stream', 373, Ljava_lang_Object_2_classLit);
function $lambda$0_1(this$static, action_1, u_1){
  action_1.accept_0(this$static.map_0.apply_0(u_1));
}

function StreamImpl$MapToObjSpliterator(map_0, original){
  Spliterators$AbstractSpliterator.call(this, original.estimateSize_0(), original.characteristics_0() & -6);
  $clinit_InternalPreconditions();
  checkCriticalNotNull(map_0);
  this.map_0 = map_0;
  this.original = original;
}

defineClass(145, 93, {}, StreamImpl$MapToObjSpliterator);
_.tryAdvance = function tryAdvance_4(action){
  return this.original.tryAdvance(new StreamImpl$MapToObjSpliterator$lambda$0$Type(this, action));
}
;
var Ljava_util_stream_StreamImpl$MapToObjSpliterator_2_classLit = createForClass('java.util.stream', 'StreamImpl/MapToObjSpliterator', 145, Ljava_util_Spliterators$AbstractSpliterator_2_classLit);
function StreamImpl$MapToObjSpliterator$lambda$0$Type($$outer_0, action_1){
  this.$$outer_0 = $$outer_0;
  this.action_1 = action_1;
}

defineClass(372, 1, $intern_5, StreamImpl$MapToObjSpliterator$lambda$0$Type);
_.accept_0 = function accept_19(arg0){
  $lambda$0_1(this.$$outer_0, this.action_1, arg0);
}
;
var Ljava_util_stream_StreamImpl$MapToObjSpliterator$lambda$0$Type_2_classLit = createForAnonymousClass('java.util.stream', 372, Ljava_lang_Object_2_classLit);
function $accept(this$static, value_0){
  this$static.value_0 = value_0;
}

function StreamImpl$ValueConsumer(){
}

defineClass(147, 1, $intern_5, StreamImpl$ValueConsumer);
_.accept_0 = function accept_20(value_0){
  $accept(this, value_0);
}
;
var Ljava_util_stream_StreamImpl$ValueConsumer_2_classLit = createForClass('java.util.stream', 'StreamImpl/ValueConsumer', 147, Ljava_lang_Object_2_classLit);
function StreamImpl$lambda$0$Type(){
}

defineClass(374, 1, $intern_5, StreamImpl$lambda$0$Type);
_.accept_0 = function accept_21(arg0){
  $clinit_StreamImpl();
}
;
var Ljava_util_stream_StreamImpl$lambda$0$Type_2_classLit = createForAnonymousClass('java.util.stream', 374, Ljava_lang_Object_2_classLit);
function StreamImpl$lambda$1$Type(){
}

defineClass(375, 1, $intern_5, StreamImpl$lambda$1$Type);
_.accept_0 = function accept_22(arg0){
  $clinit_StreamImpl();
}
;
var Ljava_util_stream_StreamImpl$lambda$1$Type_2_classLit = createForAnonymousClass('java.util.stream', 375, Ljava_lang_Object_2_classLit);
function StreamImpl$lambda$4$Type(collector_0){
  this.collector_0 = collector_0;
}

defineClass(377, 1, {}, StreamImpl$lambda$4$Type);
_.apply_2 = function apply_25(arg0, arg1){
  return lambda$4_0(this.collector_0, arg0, arg1);
}
;
var Ljava_util_stream_StreamImpl$lambda$4$Type_2_classLit = createForAnonymousClass('java.util.stream', 377, Ljava_lang_Object_2_classLit);
function StreamImpl$lambda$5$Type(consumer_0, accumulator_1){
  this.consumer_0 = consumer_0;
  this.accumulator_1 = accumulator_1;
}

defineClass(378, 1, $intern_5, StreamImpl$lambda$5$Type);
_.accept_0 = function accept_23(arg0){
  lambda$5_0(this.consumer_0, this.accumulator_1, arg0);
}
;
var Ljava_util_stream_StreamImpl$lambda$5$Type_2_classLit = createForAnonymousClass('java.util.stream', 378, Ljava_lang_Object_2_classLit);
function TerminatableStream$lambda$0$Type(throwables_0){
  this.throwables_0 = throwables_0;
}

defineClass(383, 1, $intern_5, TerminatableStream$lambda$0$Type);
_.accept_0 = function accept_24(arg0){
  var lastArg;
  lambda$0_2((lastArg = this.throwables_0 , throwClassCastExceptionUnlessNull(arg0) , lastArg));
}
;
var Ljava_util_stream_TerminatableStream$lambda$0$Type_2_classLit = createForAnonymousClass('java.util.stream', 383, Ljava_lang_Object_2_classLit);
function clone(array, toIndex){
  var result_0;
  result_0 = array.slice(0, toIndex);
  return stampJavaTypeInfo_0(result_0, array);
}

function copy(src_0, srcOfs, dest, destOfs, len, overwrite){
  var batchEnd, batchStart, destArray, end, spliceArgs;
  if (maskUndefined(src_0) === maskUndefined(dest)) {
    src_0 = src_0.slice(srcOfs, srcOfs + len);
    srcOfs = 0;
  }
  destArray = dest;
  for (batchStart = srcOfs , end = srcOfs + len; batchStart < end;) {
    batchEnd = $wnd.Math.min(batchStart + 10000, end);
    len = batchEnd - batchStart;
    spliceArgs = src_0.slice(batchStart, batchEnd);
    spliceArgs.splice(0, 0, destOfs, overwrite?len:0);
    Array.prototype.splice.apply(destArray, spliceArgs);
    batchStart = batchEnd;
    destOfs += len;
  }
}

function insertTo(array, index_0, value_0){
  array.splice(index_0, 0, value_0);
}

function insertTo_0(array, index_0, values){
  copy(values, 0, array, index_0, values.length, false);
}

function removeFrom(array, index_0, deleteCount){
  array.splice(index_0, deleteCount);
}

defineClass(507, 1, {});
function stampJavaTypeInfo_1(array, referenceType){
  return stampJavaTypeInfo_0(array, referenceType);
}

defineClass(141, 1, {});
var Ljavaemul_internal_ConsoleLogger_2_classLit = createForClass('javaemul.internal', 'ConsoleLogger', 141, Ljava_lang_Object_2_classLit);
function $clinit_InternalPreconditions(){
  $clinit_InternalPreconditions = emptyMethod;
}

function checkCriticalArgument(expression){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new IllegalArgumentException);
  }
}

function checkCriticalArgument_0(expression, errorMessage){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new IllegalArgumentException_0(errorMessage));
  }
}

function checkCriticalArrayBounds_0(start_0, end, length_0){
  $clinit_InternalPreconditions();
  if (start_0 > end) {
    throw toJs(new IllegalArgumentException_0('fromIndex: ' + start_0 + ' > toIndex: ' + end));
  }
  if (start_0 < 0 || end > length_0) {
    throw toJs(new ArrayIndexOutOfBoundsException('fromIndex: ' + start_0 + ', toIndex: ' + end + ', length: ' + length_0));
  }
}

function checkCriticalArrayType(expression){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new ArrayStoreException);
  }
}

function checkCriticalArrayType_0(expression, errorMessage){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new ArrayStoreException_0(errorMessage));
  }
}

function checkCriticalElement(expression){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new NoSuchElementException);
  }
}

function checkCriticalElementIndex(index_0, size_0){
  $clinit_InternalPreconditions();
  if (index_0 < 0 || index_0 >= size_0) {
    throw toJs(new IndexOutOfBoundsException_0('Index: ' + index_0 + ', Size: ' + size_0));
  }
}

function checkCriticalNotNull(reference){
  $clinit_InternalPreconditions();
  if (reference == null) {
    throw toJs(new NullPointerException);
  }
  return reference;
}

function checkCriticalNotNull_0(reference, errorMessage){
  $clinit_InternalPreconditions();
  if (reference == null) {
    throw toJs(new NullPointerException_0(errorMessage));
  }
}

function checkCriticalPositionIndex(index_0, size_0){
  $clinit_InternalPreconditions();
  if (index_0 < 0 || index_0 > size_0) {
    throw toJs(new IndexOutOfBoundsException_0('Index: ' + index_0 + ', Size: ' + size_0));
  }
}

function checkCriticalPositionIndexes(start_0, end, size_0){
  $clinit_InternalPreconditions();
  if (start_0 < 0 || end > size_0) {
    throw toJs(new IndexOutOfBoundsException_0('fromIndex: ' + start_0 + ', toIndex: ' + end + ', size: ' + size_0));
  }
  if (start_0 > end) {
    throw toJs(new IllegalArgumentException_0('fromIndex: ' + start_0 + ' > toIndex: ' + end));
  }
}

function checkCriticalState(expression){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new IllegalStateException);
  }
}

function checkCriticalStringBounds(start_0, end, length_0){
  $clinit_InternalPreconditions();
  if (start_0 < 0 || end > length_0 || end < start_0) {
    throw toJs(new StringIndexOutOfBoundsException('fromIndex: ' + start_0 + ', toIndex: ' + end + ', length: ' + length_0));
  }
}

function checkCriticalStringElementIndex(index_0, size_0){
  $clinit_InternalPreconditions();
  if (index_0 < 0 || index_0 >= size_0) {
    throw toJs(new StringIndexOutOfBoundsException('Index: ' + index_0 + ', Size: ' + size_0));
  }
}

function checkCriticalType(expression, message){
  $clinit_InternalPreconditions();
  if (!expression) {
    throw toJs(new ClassCastException(message));
  }
}

defineClass(504, 1, {});
function getHashCode_0(o){
  return o.$H || (o.$H = ++nextHashId);
}

var nextHashId = 0;
function $clinit_StringHashCache(){
  $clinit_StringHashCache = emptyMethod;
  back_0 = new Object_0;
  front = new Object_0;
}

function compute(str){
  var hashCode, i, n, nBatch;
  hashCode = 0;
  n = str.length;
  nBatch = n - 4;
  i = 0;
  while (i < nBatch) {
    hashCode = ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(i + 3, str.length) , str.charCodeAt(i + 3) + (checkCriticalStringElementIndex(i + 2, str.length) , 31 * (str.charCodeAt(i + 2) + (checkCriticalStringElementIndex(i + 1, str.length) , 31 * (str.charCodeAt(i + 1) + (checkCriticalStringElementIndex(i, str.length) , 31 * (str.charCodeAt(i) + 31 * hashCode)))))));
    hashCode = hashCode | 0;
    i += 4;
  }
  while (i < n) {
    hashCode = hashCode * 31 + $charAt(str, i++);
  }
  hashCode = hashCode | 0;
  return hashCode;
}

function getHashCode_1(str){
  $clinit_StringHashCache();
  var hashCode, key, result_0;
  key = ':' + str;
  result_0 = front[key];
  if (result_0 != null) {
    return round_int(($clinit_InternalPreconditions() , checkCriticalNotNull(result_0) , result_0));
  }
  result_0 = back_0[key];
  hashCode = result_0 == null?compute(str):round_int(($clinit_InternalPreconditions() , checkCriticalNotNull(result_0) , result_0));
  increment();
  front[key] = hashCode;
  return hashCode;
}

function increment(){
  if (count_1 == 256) {
    back_0 = front;
    front = new Object_0;
    count_1 = 0;
  }
  ++count_1;
}

var back_0, count_1 = 0, front;
function $clinit_LoggerFactory(){
  $clinit_LoggerFactory = emptyMethod;
  iLoggerFactory_0 = new JULLoggerFactory;
}

function getLogger_1(clazz){
  $clinit_LoggerFactory();
  return getLogger_2(($ensureNamesAreInitialized(clazz) , clazz.typeName));
}

function getLogger_2(name_0){
  $clinit_LoggerFactory();
  var iLoggerFactory;
  iLoggerFactory = iLoggerFactory_0;
  return iLoggerFactory.getLogger(name_0);
}

var iLoggerFactory_0;
function $clinit_FormattingTuple(){
  $clinit_FormattingTuple = emptyMethod;
  new FormattingTuple;
}

function FormattingTuple(){
  FormattingTuple_0.call(this, null, null, null);
}

function FormattingTuple_0(message, argArray, throwable){
  $clinit_FormattingTuple();
  this.message_0 = message;
  this.throwable = throwable;
  if (!throwable)
  ;
  else {
    trimmedCopy(argArray);
  }
}

function trimmedCopy(argArray){
  var trimemdLen, trimmed;
  if (argArray == null || argArray.length == 0) {
    throw toJs(new IllegalStateException_0('non-sensical empty or null argument array'));
  }
  trimemdLen = argArray.length - 1;
  trimmed = initUnidimensionalArray(Ljava_lang_Object_2_classLit, $intern_0, 1, trimemdLen, 5, 1);
  arraycopy(argArray, 0, trimmed, 0, trimemdLen);
  return trimmed;
}

defineClass(77, 1, {}, FormattingTuple, FormattingTuple_0);
var Lorg_slf4j_helpers_FormattingTuple_2_classLit = createForClass('org.slf4j.helpers', 'FormattingTuple', 77, Ljava_lang_Object_2_classLit);
function arrayFormat(messagePattern, argArray){
  var L, i, j, sbuf, throwableCandidate;
  throwableCandidate = getThrowableCandidate(argArray);
  i = 0;
  sbuf = new StringBuffer_0;
  for (L = 0; L < argArray.length; L++) {
    j = messagePattern.indexOf('{}', i);
    if (j == -1) {
      if (i == 0) {
        return new FormattingTuple_0(messagePattern, argArray, throwableCandidate);
      }
       else {
        $append_0(sbuf, $substring_0(messagePattern, i, messagePattern.length));
        return new FormattingTuple_0(sbuf.string, argArray, throwableCandidate);
      }
    }
     else {
      if (isEscapedDelimeter(messagePattern, j)) {
        if (j >= 2 && $charAt(messagePattern, j - 2) == 92) {
          $append_0(sbuf, messagePattern.substr(i, j - 1 - i));
          deeplyAppendParameter(sbuf, argArray[L], new HashMap);
          i = j + 2;
        }
         else {
          --L;
          $append_0(sbuf, messagePattern.substr(i, j - 1 - i));
          sbuf.string += '{';
          i = j + 1;
        }
      }
       else {
        $append_0(sbuf, messagePattern.substr(i, j - i));
        deeplyAppendParameter(sbuf, argArray[L], new HashMap);
        i = j + 2;
      }
    }
  }
  $append_0(sbuf, $substring_0(messagePattern, i, messagePattern.length));
  return L < argArray.length - 1?new FormattingTuple_0(sbuf.string, argArray, throwableCandidate):new FormattingTuple_0(sbuf.string, argArray, null);
}

function booleanArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    sbuf.string += a[i];
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function byteArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    $append(sbuf, a[i]);
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function charArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    sbuf.string += String.fromCharCode(a[i]);
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function deeplyAppendParameter(sbuf, o, seenMap){
  var elementTypeCategory;
  if (o == null) {
    sbuf.string += 'null';
    return;
  }
  (getClass__Ljava_lang_Class___devirtual$(o).modifiers & 4) != 0?instanceOf(o, 466)?booleanArrayAppend(sbuf, castTo(o, 466)):instanceOf(o, 467)?byteArrayAppend(sbuf, castTo(o, 467)):instanceOf(o, 83)?charArrayAppend(sbuf, castTo(o, 83)):instanceOf(o, 468)?shortArrayAppend(sbuf, castTo(o, 468)):instanceOf(o, 107)?intArrayAppend(sbuf, castTo(o, 107)):instanceOf(o, 194)?longArrayAppend(sbuf, castTo(o, 194)):instanceOf(o, 469)?floatArrayAppend(sbuf, castTo(o, 469)):instanceOf(o, 470)?doubleArrayAppend(sbuf, castTo(o, 470)):objectArrayAppend(sbuf, ($clinit_InternalPreconditions() , $clinit_InternalPreconditions() , checkCriticalType(o == null || Array.isArray(o) && (elementTypeCategory = getElementTypeCategory(o) , !(elementTypeCategory >= 14 && elementTypeCategory <= 16)), null) , o), seenMap):safeObjectAppend(sbuf, o);
}

function doubleArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    sbuf.string += a[i];
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function floatArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    sbuf.string += a[i];
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function getThrowableCandidate(argArray){
  var lastEntry;
  if (argArray.length == 0) {
    return null;
  }
  lastEntry = argArray[argArray.length - 1];
  if (instanceOf(lastEntry, 10)) {
    return castTo(lastEntry, 10);
  }
  return null;
}

function intArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    $append(sbuf, a[i]);
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function isEscapedDelimeter(messagePattern, delimeterStartIndex){
  var potentialEscape;
  if (delimeterStartIndex == 0) {
    return false;
  }
  potentialEscape = ($clinit_InternalPreconditions() , checkCriticalStringElementIndex(delimeterStartIndex - 1, messagePattern.length) , messagePattern.charCodeAt(delimeterStartIndex - 1));
  return potentialEscape == 92;
}

function longArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    sbuf.string += toString_25(a[i]);
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

function objectArrayAppend(sbuf, a, seenMap){
  var i, len;
  sbuf.string += '[';
  if (seenMap.containsKey_0(a)) {
    sbuf.string += '...';
  }
   else {
    seenMap.put_0(a, null);
    len = a.length;
    for (i = 0; i < len; i++) {
      deeplyAppendParameter(sbuf, a[i], seenMap);
      i != len - 1 && (sbuf.string += ', ' , sbuf);
    }
    seenMap.remove_0(a);
  }
  sbuf.string += ']';
}

function safeObjectAppend(sbuf, o){
  var oAsString, t;
  try {
    oAsString = toString_26(o);
    sbuf.string += '' + oAsString;
  }
   catch ($e0) {
    $e0 = toJava($e0);
    if (instanceOf($e0, 10)) {
      t = $e0;
      $clinit_System();
      'SLF4J: Failed toString() invocation on an object of type [' + $getName(getClass__Ljava_lang_Class___devirtual$(o)) + ']';
      $printStackTraceImpl(t, err, '');
      sbuf.string += '[FAILED toString()]';
    }
     else 
      throw toJs($e0);
  }
}

function shortArrayAppend(sbuf, a){
  var i, len;
  sbuf.string += '[';
  len = a.length;
  for (i = 0; i < len; i++) {
    $append(sbuf, a[i]);
    i != len - 1 && (sbuf.string += ', ' , sbuf);
  }
  sbuf.string += ']';
}

var C_classLit = createForPrimitive('char', 'C');
var I_classLit = createForPrimitive('int', 'I');
var Z_classLit = createForPrimitive('boolean', 'Z');
var J_classLit = createForPrimitive('long', 'J');
var B_classLit = createForPrimitive('byte', 'B');
var Ljava_lang_Byte_2_classLit = createForClass('java.lang', 'Byte', null, Ljava_lang_Number_2_classLit);
var D_classLit = createForPrimitive('double', 'D');
var F_classLit = createForPrimitive('float', 'F');
var Ljava_lang_Float_2_classLit = createForClass('java.lang', 'Float', null, Ljava_lang_Number_2_classLit);
var S_classLit = createForPrimitive('short', 'S');
var Ljava_lang_Short_2_classLit = createForClass('java.lang', 'Short', null, Ljava_lang_Number_2_classLit);
var Ljava_util_logging_Handler_2_classLit = createForClass('java.util.logging', 'Handler', 88, Ljava_lang_Object_2_classLit);
var Ljava_util_AbstractSequentialList_2_classLit = createForClass('java.util', 'AbstractSequentialList', null, Ljava_util_AbstractList_2_classLit);
var Ljava_util_LinkedList_2_classLit = createForClass('java.util', 'LinkedList', null, Ljava_util_AbstractSequentialList_2_classLit);
var Ljava_util_TreeSet_2_classLit = createForClass('java.util', 'TreeSet', null, Ljava_util_AbstractSet_2_classLit);
var Ljava_util_AbstractNavigableMap_2_classLit = createForClass('java.util', 'AbstractNavigableMap', null, Ljava_util_AbstractMap_2_classLit);
var Ljava_util_TreeMap_2_classLit = createForClass('java.util', 'TreeMap', null, Ljava_util_AbstractNavigableMap_2_classLit);
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LightSet_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'LightSet', null, Ljava_util_AbstractSet_2_classLit);
var Lcc_alcina_framework_common_client_logic_domaintransform_lookup_LiSet_2_classLit = createForClass('cc.alcina.framework.common.client.logic.domaintransform.lookup', 'LiSet', null, Ljava_util_AbstractSet_2_classLit);
var Lcc_alcina_framework_common_client_util_CountingMap_2_classLit = createForClass('cc.alcina.framework.common.client.util', 'CountingMap', null, Ljava_util_LinkedHashMap_2_classLit);
var Lcc_alcina_framework_common_client_collections_IdentityArrayList_2_classLit = createForClass('cc.alcina.framework.common.client.collections', 'IdentityArrayList', null, Ljava_util_ArrayList_2_classLit);
var Ljava_util_Date_2_classLit = createForClass('java.util', 'Date', null, Ljava_lang_Object_2_classLit);
var Ljava_sql_Timestamp_2_classLit = createForClass('java.sql', 'Timestamp', null, Ljava_util_Date_2_classLit);
var V_classLit = createForPrimitive('void', 'V');
var Ljava_lang_Void_2_classLit = createForClass('java.lang', 'Void', null, Ljava_lang_Object_2_classLit);
var Lcc_alcina_framework_common_client_domain_DomainCollections_2_classLit = createForClass('cc.alcina.framework.common.client.domain', 'DomainCollections', null, Ljava_lang_Object_2_classLit);
var $entry = ($clinit_Impl() , entry_0);
var gwtOnLoad = gwtOnLoad = gwtOnLoad_0;
addInitFunctions(init);
setGwtProperty('permProps', [[['locale', 'default'], ['user.agent', 'safari']]]);
$sendStats('moduleStartup', 'moduleEvalEnd');
gwtOnLoad(__gwtModuleFunction.__errFn, __gwtModuleFunction.__moduleName, __gwtModuleFunction.__moduleBase, __gwtModuleFunction.__softPermutationId,__gwtModuleFunction.__computePropValue);
$sendStats('moduleStartup', 'end');
$gwt && $gwt.permProps && __gwtModuleFunction.__moduleStartupDone($gwt.permProps);
//# sourceURL=cc.alcina.extras.dev.console.remote.RemoteConsoleClient-0.js

