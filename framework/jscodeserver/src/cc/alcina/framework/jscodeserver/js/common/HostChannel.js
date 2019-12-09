class gwt_hm_HostChannel {
    // codepoints (binary string)
    buf_out = "";
    // codepoints (binary string)
    buf_in;
    buf_in_idx;
    handler;
    channelId;
    closeSocket = false;
    tcpHost = "";
    /*
     * Performance timing - is most of our overhead in http?
     */
    xhrTimingData = [];
    xhrTimingCumulativeMilliseconds = 0;
    
    connectToHost(host, port) {
        // ignore host (for the mo) - assume local, otherwise will need CORS
        //
        // correction, always use local (rather than routing through remote) -
        // network round trips more than compensates
        // this.host = "";
        this.host = "http://127.0.0.1:"+(parseInt(port)+1);
        this.port = port;
    }
    init(handler, minVersion, maxVersion,
        hostedHtmlVersion) {
        this.handler = handler;
        gwt_hm_CheckVersionsMessage.send(this, minVersion, maxVersion, hostedHtmlVersion);
        var type = this.readByte();
        switch (type) {
            case gwt_hm_BrowserChannel.MESSAGE_TYPE_PROTOCOL_VERSION:
                {
                    var message = gwt_hm_ProtocolVersionMessage.receive(this);
                    break;
                }
            case MESSAGE_TYPE_FATAL_ERROR:
                {
                    var message = gwt_hm_FatalErrorMessage.receive(this);
                    handler.fatalError(this, message.getError());
                    return false;
                }
            default:
                return false;
        }
        var self = this;
        window.addEventListener("unload", function(event) {
            self.disconnectFromHost();
        });
        return true;
    }
    isConnected() {
        return true
    }
    readBytes(dataLen) {
        var count = dataLen;
        var buf = "";
        while (count > 0) {
            buf += this.readByte();
            --count;
        }
    }
    sendBytes(data) {
        for (var idx = 0; idx < data.length(); idx++) {
            this.sendByte(data.charCodeAt(idx));
        }
    }
    readInt() {
        var b0 = this.readByte();
        var b1 = this.readByte();
        var b2 = this.readByte();
        var b3 = this.readByte();
        return (b0 << 24) + (b1 << 16) + (b2 << 8) + b3;
    }
    sendInt(v) {
        this.sendByte((v >>> 24) & 0xFF);
        this.sendByte((v >>> 16) & 0xFF);
        this.sendByte((v >>> 8) & 0xFF);
        this.sendByte((v >>> 0) & 0xFF);
    }
    readShort() {
        var b0 = this.readByte();
        var b1 = this.readByte();
        return (b0 << 8) + b1;
    }
    sendShort(v) {
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 0) & 0xFF);
    }
    readLong() {
        var i0 = this.readInt(data);
        var i1 = this.readInt(data);
        var ret = {
            l: i1 & gwt_hm_HostChannel.long_MASK,
            m: i1 >>> gwt_hm_HostChannel.long_BITS | ((i0 << 10) & gwt_hm_HostChannel.long_MASK),
            h: (i0 >>> 10) & gwt_hm_HostChannel.long_MASK
        };
        return ret;
    }
    sendLong(v) {
        var i0 = ((v.m >>> 10) & gwt_hm_HostChannel.long_MASK) | (v.h << 12);
        var i1 = v.l | (v.m << gwt_hm_HostChannel.long_BITS);
        this.sendInt(io);
        this.sendInt(i1);
    }
    readFloat() {
        var v = [this.readByte(), this.readByte(), this.readByte(), this.readByte()];
        return ieee754.read(v, 0, false, 23, 4);
    }
    sendFloat(v) {
        var buf = [];
        ieee754.write(buf, v, 0, false, 23, 4);
        for (var idx = 0; idx <= 3; idx++) {
            this.sendByte(buf[idx]);
        }
    }
    readDouble() {
        var v = [this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte()];
        return ieee754.read(v, 0, false, 52, 8);
    }
    sendDouble(v) {
        var buf = [];
        ieee754.write(buf, v, 0, false, 52, 8);
        for (var idx = 0; idx <= 7; idx++) {
            this.sendByte(buf[idx]);
        }
    }
    readByte() {
        if (this.buf_in_idx >= this.buf_in.length) {
            throw "stream exhausted";
        }
        return this.buf_in.charCodeAt(this.buf_in_idx++);
    }
    sendByte(c) {
        if (this.buf_out.length == 0) {
// console.log(`send >> ${c}`);
        }
        this.buf_out += String.fromCharCode(c);
    }
    readStringLength() {
        return this.readInt();
    }
    readString() {
        var len = this.readInt();
        var utf8 = this.buf_in.substring(this.buf_in_idx, this.buf_in_idx + len);
        var ret = this.utf8BinaryStringToStr(utf8);
        this.buf_in_idx += len;
        return ret;
    }
    sendString(str) {
        var utf8 = this.utf16ToUtf8(str);
        this.sendInt(utf8.length);
        this.buf_out += utf8;
    }
    utf8BinaryStringToStr(str) {
      var buf = new ArrayBuffer(str.length); // 1 bytes for each utf-8
                                              // codepoint
      var bufView = new Uint8Array(buf);
      for (var i=0, strLen=str.length; i < strLen; i++) {
        bufView[i] = str.charCodeAt(i);
      }
      var str = new TextDecoder("UTF-8").decode(buf);
      return str;
    }
    utf16ToUtf8(str) {
        var u8a = new TextEncoder().encode(str);
        var CHUNK_SZ = 0x8000;
        var c = [];
        for (var i=0; i < u8a.length; i+=CHUNK_SZ) {
          c.push(String.fromCharCode.apply(null, u8a.subarray(i, i+CHUNK_SZ)));
        }
        return c.join("");
    }
    readValue() {
        var type = this.readByte();
        var value = new gwt_hm_Value();
        switch (type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
                value.setNull();
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
                value.setUndefined();
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
                {
                    var val = this.readByte();
                    value.setBoolean(val != 0);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
                {
                    var val = this.readByte();
                    value.setByte(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
                {
                    var val = this.readShort();
                    value.setChar(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                {
                    var val = this.readShort();
                    value.setShort(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
                {
                    var val = this.readString();
                    value.setString(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
                {
                    var val = this.readInt();
                    value.setInt(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
                {
                    var val = this.readLong();
                    value.setLong(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
                {
                    var val = this.readDouble();
                    value.setDouble(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
                {
                    var val = this.readInt();
                    value.setJavaObjectId(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
                {
                    var val = this.readInt();
                    value.setJsObjectId(val);
                }
                return value;
            default:
                throw "Unhandled value type sent from server: " + type;
        }
        return false;
    }
    sendValue(value) {
        var type = value.type;
        this.sendByte(type);
        switch (type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
            case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
                return;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
                return this.sendByte(value.getBoolean() ? 1 : 0);
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
                return this.sendByte(value.getByte());
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
                return this.sendShort(short(value.getChar()));
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                return this.sendShort(value.getShort());
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
                return this.sendInt(value.getInt());
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
                return this.sendLong(value.getLong());
            case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
                return this.sendString(value.getString());
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
                return this.sendDouble(value.getDouble());
            case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
                return this.sendFloat(value.getFloat());
            case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
                return this.sendInt(value.getJsObjectId());
            case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
                return this.sendInt(value.getJavaObjectId());
            default:
                throw "Unhandled value type sent to server: " + type;
        }
    }
    reactToMessages(handler, expectReturn) {
        try {
            return this.reactToMessagesOrThrow(handler, expectReturn);
        } catch (e) {
            console.warn(e);
            this.disconnectFromHost();
        }
    }
    reactToMessagesOrThrow(handler, expectReturn) {
        while (true) {
            this.flush();
            var type = this.readByte(type);
// console.log(`message: ${this.messageId} :: ${type} `);
            switch (type) {
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKE:
                    {
                        var message = gwt_hm_InvokeMessage.receive(this);
                        if (parseInt(this.messageId) > 0) {
// console.log(`invoke: ${this.messageId} :: ${message.methodName}
// [${message.thisRef.intValue}]`);
                        }
                        var result = handler.invoke(this, message.thisRef, message.methodName,
                            message.numArgs, message.args);
                        handler.sendFreeValues(this);
                        gwt_hm_ReturnMessage.send(this, result.exception, result.value);
                    }
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKESPECIAL:
                    {
                        // scottb: I think this is never used; I think server
                        // never sends invokeSpecial
                        var message = gwt_hm_InvokeSpecialMessage.receive(this);
                        var result = handler.invokeSpecial(this, message._dispatchId, message.methodName,
                            message.numArgs, message.args);
                        handler.sendFreeValues(this);
                        gwt_hm_ReturnMessage.send(this, result.exception, result.value);
                    }
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_FREEVALUE:
                    {
                        var message = gwt_hm_FreeValueMessage.receive(this);
                        handler.freeValue(this, message.idCount, message.ids);
                    }
                    // do not send a response
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_LOADJSNI:
                    {
                        var message = gwt_hm_LoadJsniMessage.receive(this);
                        handler.loadJsni(this, message.js);
                    }
                    // do not send a response
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_RETURN:
                    if (!expectReturn) {
                        throw "Received unexpected RETURN";
                    }
                    return gwt_hm_ReturnMessage.receive(this);
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_QUIT:
                    if (expectReturn) {
                        throw "Received QUIT while waiting for return";
                    }
                    this.disconnectFromHost();
                    return 0;
                default:
                    // TODO(jat): error handling
                    throw "Unexpected message type " + type;
            }
        }
    }
    reactToMessagesWhileNotWaitingForReturn(handler) {
        return !this.reactToMessages(handler, false);
    }
    flush() {
        let body = null;
        try {
            body = btoa(this.buf_out);
        } catch (e) {
            debugger;
        }
        this.buf_out = "";
        this.flushWithBody(body);
    }
    flushWithBody(body) {
        var t0 = performance.now();
        var xhr = new XMLHttpRequest();
        var url = `${this.host}/jsCodeServer.tcp`;
        xhr.open("POST", url, false);
        xhr.setRequestHeader("XhrTcpBridge.codeserver_port", this.port);
        xhr.setRequestHeader("mixed-content", "noupgrade");
        if (this.channelId) {
            xhr.setRequestHeader("XhrTcpBridge.handle_id", this.channelId);
        }
        if (this.closeSocket) {
            xhr.setRequestHeader("XhrTcpBridge.meta", "close_socket");
        }
        try {
            xhr.send(body);
        } catch (e) {
            if (this.channelId || this.host) {
                throw e;
            } else {
                // retry with alt code server;
                this.host = "http://127.0.0.1:10005";
                this.flushWithBody(body);
                return;
            }
        }
        if (this.closeSocket) {
            return;
        }
        var xhrChannelId = xhr.getResponseHeader("XhrTcpBridge.handle_id");
        this.messageId = xhr.getResponseHeader("XhrTcpBridge.message_id");
        if (this.channelId && this.channelId != xhrChannelId) {
            throw "Different channel id";
        }
        this.channelId = xhrChannelId;
        this.buf_in = atob(xhr.responseText);
        this.buf_in_idx = 0;
        var t1 = performance.now();
        var xhrTime = t1-t0;
        this.xhrTimingData.push(xhrTime);
        this.xhrTimingCumulativeMicroseconds+=xhrTime;
        if(this.xhrTimingData.length%1000==0){
          console.log(`timing data: ${this.xhrTimingData.length} : ${this.xhrTimingCumulativeMicroseconds} `);
        }
    }
    ensureClear() {
        if (this.buf_out.length > 0) {
            throw "pending message";
        }
    }
    reactToMessagesWhileWaitingForReturn(handler) {
        return this.reactToMessages(handler, true);
    }
    disconnectFromHost() {
        new gwt_hm_QuitMessage.send(this);
    }
}
gwt_hm_HostChannel.long_BITS = 22;
gwt_hm_HostChannel.long_BITS01 = 2 * gwt_hm_HostChannel.long_BITS;
gwt_hm_HostChannel.long_BITS2 = 64 - gwt_hm_HostChannel.long_BITS01;
gwt_hm_HostChannel.long_MASK = (1 << gwt_hm_HostChannel.long_BITS) - 1;
gwt_hm_HostChannel.long_MASK_2 = (1 << gwt_hm_HostChannel.long_BITS2) - 1;