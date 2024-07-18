class gwt_hm_Value {
	type;
	boolValue;
	byteValue;
	charValue;
	doubleValue;
	floatValue;
	intValue;
	longValue;
	shortValue;
	stringValue;
	arrayValue;
	constructor() {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED;
	}
	getBoolean() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN);
		return this.boolValue;
	}
	getByte() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_BYTE);
		return this.byteValue;
	}
	getChar() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_CHAR);
		return this.charValue;
	}
	getDouble() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE);
		return this.doubleValue;
	}
	getFloat() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT);
		return this.floatValue;
	}
	getInt() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_INT);
		return this.intValue;
	}
	getJavaObjectId() {
		if (this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT || this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT_LIST || this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JS_INT_LIST) {
			//allowed
		} else {
			this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT);
		}
		return this.intValue;
	}
	getJsObjectId() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT);
		return this.intValue;
	}
	getLong() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_LONG);
		return this.longValue;
	}
	getShort() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_SHORT);
		return this.shortValue;
	}
	getString() {
		this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_STRING);
		return this.stringValue;
	}
	getArray() {
		if (this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT_LIST || this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JS_INT_LIST) {
			//allowed
		} else {
			this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT_LIST);
		}
		return this.arrayValue;
	}
	getType() {
		return type;
	}
	isBoolean() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN;
	}
	isByte() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_BYTE;
	}
	isChar() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_CHAR;
	}
	isDouble() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE;
	}
	isFloat() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT;
	}
	isInt() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_INT;
	}
	isJavaObject() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT;
	}
	isJsObject() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT;
	}
	isLong() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_LONG;
	}
	isNull() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_NULL;
	}
	isNumber() {
		switch (this.type) {
			case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
			case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
			case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
			case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
			case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
			case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
			case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
				return true;
			default:
				return false;
		}
	}
	isPrimitive() {
		switch (this.type) {
			case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
			case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
			case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
			case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
			case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
			case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
			case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
			case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
				return true;
			default:
				return false;
		}
	}
	isShort() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_SHORT;
	}
	isString() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_STRING;
	}
	isUndefined() {
		return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED;
	}
	setBoolean(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN;
		this.boolValue = val;
	}
	setByte(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_BYTE;
		this.byteValue = val;
	}
	setChar(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_CHAR;
		this.charValue = val;
	}
	setDouble(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE;
		this.doubleValue = val;
	}
	setDouble(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE;
		this.doubleValue = val;
	}
	setFloat(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT;
		this.floatValue = val;
	}
	setInt(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_INT;
		this.intValue = val;
	}
	setJavaObjectId(objectId) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT;
		this.intValue = objectId;
	}
	setJsObjectId(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT;
		this.intValue = val;
	}
	setJavaObjectLength(jsObjectLength) {
		//the length parameter is unused (setting the js array length would require different handling)
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT_LIST;
		this.arrayValue = [];
	}
	setJavaIntLength(intObjectLength) {
		//the length parameter is unused (setting the js array length would require different handling)
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_JS_INT_LIST;
		this.arrayValue = [];
	}
	addJsObjectId(val) {
		this.arrayValue.push(val);
	}
	addJsInt(val) {
		this.arrayValue.push(val);
	}
	setLong(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_LONG;
		this.longValue = val;
	}
	setNull() {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_NULL;
	}
	setShort(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_SHORT;
		this.shortValue = val;
	}
	setString(val) {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_STRING;
		this.stringValue = val;
	}
	setUndefined() {
		this.type = gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED;
	}
	assertType(reqType) {
		if (this.type != reqType) {
			throw "Value::assertType - expecting type " + reqType;
		}
	}
	toString() {
		switch (this.type) {
			case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
				return `boolean: ${this.getBoolean()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
				return `byte: ${this.getByte()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
				return `char: ${this.getChar()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
				return `double: ${this.getDouble()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
				return `float: ${this.getFloat()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
				return `int: ${this.getInt()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
				return `long: ${this.getLong()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
				return `short: ${this.getShort()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
				return "null";
			case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
				return `string: ${this.getString()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
				return `javaobj: ${this.getJavaObjectId()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
				return `jsobj: ${this.getJsObjectId()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
				return "undefined";
			case gwt_hm_BrowserChannel.VALUE_TYPE_UNUSED:
				return "unused";
			case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT_LIST:
				return `jsobj[]: ${this.getJsObjectId()}`;
			case gwt_hm_BrowserChannel.VALUE_TYPE_JS_INT_LIST:
				return `int[]: ${this.getJsObjectId()}`;
			default:
				return "Unknown type";
		}
	}
}