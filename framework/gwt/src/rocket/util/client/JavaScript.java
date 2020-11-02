/*
 * Copyright Miroslav Pokorny
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rocket.util.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;

/**
 * The JavaScript class contains a number of methods that make it easy to bridge
 * the gap between java and javascript object properties without escaping to
 * Jsni.
 * 
 * @author Miroslav Pokorny (mP)
 * 
 *         TODO remove xxx0 make all methods straight jsni moving checks to
 *         jsni.
 */
@SuppressWarnings("deprecation")
public class JavaScript {
	/**
	 * Convenience method which takes a Element and returns a JavaScriptObject
	 * 
	 * @param element
	 * @return
	 */
	public native static JavaScriptObject
			castFromElement(final Element element)/*-{
    return element;
	}-*/;

	/**
	 * Convenience method which takes a JavaScriptObject and casts it to an
	 * Element.
	 * 
	 * This is provided purely as a mechanism to make a JSO reference into an
	 * Element.
	 * 
	 * @param object
	 * @return
	 */
	public native static Element
			castToElement(final JavaScriptObject object)/*-{
    return object;
	}-*/;

	public static boolean getBoolean(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return getBoolean0(object, index);
	}

	/**
	 * Reads an object's property as a boolean value.
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static boolean getBoolean(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return getBoolean0(object, propertyName);
	}

	public static double getDouble(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return getDouble0(object, index);
	}

	/**
	 * Reads an object's property as a double.
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static double getDouble(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return getDouble0(object, propertyName);
	}

	public static Element getElement(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return getElement0(object, index);
	}

	/**
	 * Reads an object given a property and returns it as a Element
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static Element getElement(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return getElement0(object, propertyName);
	}

	public static int getInteger(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return getInteger0(object, index);
	}

	/**
	 * Reads an object's property as an integer value.
	 * 
	 * @param object
	 *            The object
	 * @param propertyName
	 *            The name of the property being read
	 * @return The value
	 */
	public static int getInteger(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return getInteger0(object, propertyName);
	}

	public static JavaScriptObject getObject(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return getObject0(object, index);
	}

	/**
	 * Reads an object given a property and returns it as a JavaScriptObject
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static JavaScriptObject getObject(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return getObject0(object, propertyName);
	}

	/**
	 * Retrieves the property count for the given javascript object.
	 * 
	 * @param object
	 * @return
	 */
	public static int getPropertyCount(final JavaScriptObject object) {
		Checker.notNull("parameter:object", object);
		return getPropertyCount0(object);
	}

	/**
	 * Builds an array containing the names of all the properties from the given
	 * java script object.
	 * 
	 * @param object
	 * @return
	 */
	public static String[] getPropertyNames(final JavaScriptObject object) {
		final JavaScriptObject namesArray = getPropertyNames0(object);
		final int count = JavaScript.getPropertyCount(namesArray);
		final String[] strings = new String[count];
		for (int i = 0; i < count; i++) {
			strings[i] = JavaScript.getString(namesArray, i);
		}
		return strings;
	}

	public static String getString(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return getString0(object, index);
	}

	/**
	 * Retrieves an object property as a String.
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static String getString(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return getString0(object, propertyName);
	}

	/**
	 * Retrieves the actual javascript type for the property value at the given
	 * slot.
	 * 
	 * @param object
	 * @param index
	 * @return
	 */
	public static String getType(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return JavaScript.getType0(object, index);
	}

	/**
	 * Retrieves the actual javascript type for the property value.
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static String getType(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return JavaScript.getType0(object, propertyName);
	}

	/**
	 * Tests if the given javascript object has a property at the given slot
	 * 
	 * @param object
	 * @param index
	 * @return
	 */
	public static boolean hasProperty(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return hasProperty0(object, index);
	}

	/**
	 * Tests if a particular property is present on the given object.
	 * 
	 * @param object
	 * @param propertyName
	 * @return
	 */
	public static boolean hasProperty(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return hasProperty0(object, propertyName);
	}

	/**
	 * Searches the given array for an element returning the index of the first
	 * match
	 * 
	 * @param array
	 * @param element
	 * @return
	 */
	public static int indexOf(final JavaScriptObject array,
			final JavaScriptObject element) {
		return JavaScript.indexOf0(array, element);
	}

	/**
	 * Searches the given array for an element starting with the last element
	 * until a match is found and then returns that index.
	 * 
	 * @param array
	 * @param element
	 * @return
	 */
	public static int lastIndexOf(final JavaScriptObject array,
			final JavaScriptObject element) {
		return JavaScript.lastIndexOf0(array, element);
	}

	public static JavaScriptObject removeProperty(final JavaScriptObject object,
			final int index) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		return removeProperty0(object, index);
	}

	/**
	 * Removes or deletes a property from the given object.
	 * 
	 * @param object
	 * @param propertyName
	 * @return The properties previous value.
	 */
	public static JavaScriptObject removeProperty(final JavaScriptObject object,
			final String propertyName) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		return removeProperty0(object, propertyName);
	}

	public static void setBoolean(final JavaScriptObject object,
			final int index, final boolean booleanValue) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		setBoolean0(object, index, booleanValue);
	}

	/**
	 * Writes a boolean value to an object's property.
	 * 
	 * @param object
	 *            The object
	 * @param propertyName
	 *            The property name
	 * @param booleanValue
	 *            THe new value
	 */
	public static void setBoolean(final JavaScriptObject object,
			final String propertyName, final boolean booleanValue) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		setBoolean0(object, propertyName, booleanValue);
	}

	public static void setDouble(final JavaScriptObject object, final int index,
			final double value) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		setDouble0(object, index, value);
	}

	/**
	 * Writes a double value to an object's property
	 * 
	 * @param object
	 * @param propertyName
	 * @param value
	 */
	public static void setDouble(final JavaScriptObject object,
			final String propertyName, final double value) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		setDouble0(object, propertyName, value);
	}

	public static void setInteger(final JavaScriptObject object,
			final int index, final int intValue) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		setInteger0(object, index, intValue);
	}

	/**
	 * Writes an integer value to an object's property
	 * 
	 * @param object
	 *            The object
	 * @param propertyName
	 *            The name of the property being set
	 * @param intValue
	 *            The new value
	 */
	public static void setInteger(final JavaScriptObject object,
			final String propertyName, final int intValue) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		setInteger0(object, propertyName, intValue);
	}

	public static void setObject(final JavaScriptObject object, final int index,
			final JavaScriptObject value) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		setObject0(object, index, value);
	}

	/**
	 * Writes an object to an object's property.
	 * 
	 * @param object
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static void setObject(final JavaScriptObject object,
			final String propertyName, final JavaScriptObject value) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		setObject0(object, propertyName, value);
	}

	public static void setString(final JavaScriptObject object, final int index,
			final String value) {
		Checker.notNull("parameter:object", object);
		Checker.greaterThanOrEqual("parameter:index", 0, index);
		setString0(object, index, value);
	}

	/**
	 * Writes a String value to an Object's property
	 * 
	 * @param object
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static void setString(final JavaScriptObject object,
			final String propertyName, final String value) {
		Checker.notNull("parameter:object", object);
		Checker.notEmpty("parameter:propertyName", propertyName);
		setString0(object, propertyName, value);
	}

	private native static boolean getBoolean0(final JavaScriptObject object,
			final int index)/*-{
    return !!object[index];
	}-*/;

	private native static boolean getBoolean0(final JavaScriptObject object,
			final String propertyName)/*-{
    return !!object[propertyName];
	}-*/;

	private native static double getDouble0(final JavaScriptObject object,
			final int index)/*-{
    var value = object[index];
    if (typeof (value) == "undefined") {
      throw "The object does not contain a property called \"" + index
          + "\", object: " + object;
    }
    return value;
	}-*/;

	private native static double getDouble0(final JavaScriptObject object,
			final String propertyName)/*-{
    var value = object[propertyName];
    if (typeof (value) == "undefined") {
      throw "The object does not contain a property called \"" + propertyName
          + "\", object: " + object;
    }
    return value;
	}-*/;

	native private static Element getElement0(final JavaScriptObject object,
			final int index)/*-{
    var value = object[index];
    return value || null;
	}-*/;

	native private static Element getElement0(final JavaScriptObject object,
			final String propertyName)/*-{
    var value = object[propertyName];
    return value || null;
	}-*/;

	private native static int getInteger0(final JavaScriptObject object,
			final int index)/*-{
    var value = object[index];
    if (typeof (value) == "undefined") {
      throw "The object does not contain a property called \"" + index
          + "\", object: " + object;
    }
    return value;
	}-*/;

	private native static int getInteger0(final JavaScriptObject object,
			final String propertyName)/*-{
    //		@rocket.util.client.Checker::checkNotNull(Ljava/lang/Object;Ljava/lang/String;)("parameter:object", object );
    //		@rocket.util.client.Checker::checkNotNull(Ljava/lang/Object;Ljava/lang/String;)("parameter:propertyName", object );

    var value = object[propertyName];
    if (typeof (value) == "undefined") {
      throw "The object does not contain a property called \"" + propertyName
          + "\", object: " + object;
    }
    return value;
	}-*/;

	native private static JavaScriptObject
			getObject0(final JavaScriptObject object, final int index)/*-{
    var value = object[index];
    return value || null;
	}-*/;

	native private static JavaScriptObject getObject0(
			final JavaScriptObject object, final String propertyName)/*-{
    var value = object[propertyName];
    return value || null;
	}-*/;

	/**
	 * Retrieves the property count for the given native object. If the length
	 * property is present that value is returned otherwise a for each loop is
	 * used to count all the properties
	 * 
	 * @param nativeObject
	 * @return
	 */
	native private static int
			getPropertyCount0(final JavaScriptObject nativeObject)/*-{
    var propertyCount = nativeObject.length;
    if (typeof (propertyCount) != "number") {

      // length not found need to count properties...
      propertyCount = 0;
      for (propertyName in nativeObject) {
        propertyCount++;
      }
    }
    return propertyCount;
	}-*/;

	native static private JavaScriptObject
			getPropertyNames0(final JavaScriptObject object)/*-{
    var names = new Array();

    for (name in object) {
      names.push(name);
    }

    return names;
	}-*/;

	private static native String getString0(final JavaScriptObject object,
			final int index)/*-{
    var value = object[index];
    return value ? "" + value : null;
	}-*/;

	private static native String getString0(final JavaScriptObject object,
			final String propertyName)/*-{
    var value = object[propertyName];
    return value ? "" + value : null;
	}-*/;

	native private static String getType0(final JavaScriptObject object,
			final int index)/*-{
    return typeof (object[index]);
	}-*/;

	native private static String getType0(final JavaScriptObject object,
			final String propertyName)/*-{
    return typeof (object[propertyName]);
	}-*/;

	private static native boolean hasProperty0(final JavaScriptObject object,
			final int index)/*-{
    var value = object[index];
    return typeof (value) != "undefined";
	}-*/;

	private static native boolean hasProperty0(final JavaScriptObject object,
			final String propertyName)/*-{
    var value = object[propertyName];

    return typeof (value) != "undefined";
	}-*/;

	native private static int indexOf0(final JavaScriptObject array,
			final JavaScriptObject element)/*-{
    var index = -1;
    for (var i = 0; i < array.length; i++) {
      if (array[i] == element) {
        index = i;
        break;
      }
    }
    return index;
	}-*/;

	native private static int lastIndexOf0(final JavaScriptObject array,
			final JavaScriptObject element)/*-{
    var index = -1;
    for (var i = array.length - 1; i >= 0; i--) {
      if (array[i] == element) {
        index = i;
        break;
      }
    }
    return index;
	}-*/;

	native private static JavaScriptObject removeProperty0(
			final JavaScriptObject object, final int index)/*-{
    var previousValue = object[index];
    delete object[index];
    return previousValue || null;
	}-*/;

	native private static JavaScriptObject removeProperty0(
			final JavaScriptObject object, final String propertyName)/*-{
    var previousValue = object[propertyName];
    delete object[propertyName];
    return previousValue || null;
	}-*/;

	private static native void setBoolean0(final JavaScriptObject object,
			final int index, final boolean booleanValue)/*-{
    object[index] = booleanValue;
	}-*/;

	private static native void setBoolean0(final JavaScriptObject object,
			final String propertyName, final boolean booleanValue)/*-{
    object[propertyName] = booleanValue;
	}-*/;

	private static native void setDouble0(final JavaScriptObject object,
			final int index, final double value)/*-{
    object[index] = value;
	}-*/;

	private static native void setDouble0(final JavaScriptObject object,
			final String propertyName, final double value)/*-{
    object[propertyName] = value;
	}-*/;

	private static native void setInteger0(final JavaScriptObject object,
			final int index, final int intValue)/*-{
    object[index] = intValue;
	}-*/;

	private static native void setInteger0(final JavaScriptObject object,
			final String propertyName, final int intValue)/*-{
    object[propertyName] = intValue;
	}-*/;

	native private static void setObject0(final JavaScriptObject object,
			final int index, final JavaScriptObject value)/*-{
    object[index] = value;
	}-*/;

	native private static void setObject0(final JavaScriptObject object,
			final String propertyName, final JavaScriptObject value)/*-{
    object[propertyName] = value;
	}-*/;

	private static native void setString0(final JavaScriptObject object,
			final int index, final String value)/*-{
    object[index] = value;
	}-*/;

	private static native void setString0(final JavaScriptObject object,
			final String propertyName, final String value)/*-{
    object[propertyName] = value;
	}-*/;
}
