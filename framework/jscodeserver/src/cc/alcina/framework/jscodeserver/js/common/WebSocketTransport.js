/*
 * cleanup - make hostedmode aware of 'am worker' (and make worker load from that file if need be)
 */
class WebSocketTransport {

    static MESSAGE_WAIT = 0;
    static MESSAGE_CONNECT = 1;
    static MESSAGE_DATA_PACKET = 2;
    static MESSAGE_WINDOW_UNLOAD = 3;
    static MESSAGE_SOCKET_CLOSED = 4;
    static BUFFER_SIZE = 5000000;
    /*
     * we may pause in the java codeserver debugger, so make timeout biiiig (5 minutes)
     */
    static READ_TIMEOUT = 300000;
    constructor() {

    };
    setBuffers(inBuffer, outBuffer) {
        this.inBuffer = new WebSocketTransportBuffer(inBuffer);
        this.outBuffer = new WebSocketTransportBuffer(outBuffer);
    }
    sendConnect() {
        return this.sendPacket(WebSocketTransport.MESSAGE_CONNECT, new TextEncoder().encode(""));
    }
    send(string) {
        return this.sendPacket(WebSocketTransport.MESSAGE_DATA_PACKET, new TextEncoder().encode(string));
    }
    read(timeout) {
        return this.inBuffer.read(timeout);
    }
    sendPacket(message, data) {
        this.outBuffer.write(message, data);
       
        return this.read(WebSocketTransport.READ_TIMEOUT);
    }
}
class WebSocketTransportBuffer {
    closed = false;
    /*
     * buffer write operation (byte array b[n] - for Atomics.wait to work we
     * need int32)
     * 
     * [0] ::op (0: wait - 1: connect - 2: data packet...etc, see MESSAGE constants in WebSocketTransport)
     * 
     * [1] : n (as int) byte
     * 
     * [2=>2+n-1] : data
     * 
     */
    constructor(sharedArrayBuffer) {
        this.sharedArrayBuffer = sharedArrayBuffer;
        this.int32 = new Int32Array(sharedArrayBuffer);
    }
    read(timeout) {
        if (this.closed) {
            throw "Socket closed";
        }
        /*
         * Wait fot the other thread
         */
        if (WebSocketTransport_is_worker) {
            /*
             * should not be needed (worker will already have a message saying
             * 'packet ready'). Can't use this on main thread cos 'javascript
             * doesn't block on main thread'...much
             */
            Atomics.wait(this.int32, 0, WebSocketTransport.MESSAGE_WAIT, timeout);
        } else {
            let t0 = performance.now();
            let counter = 0;
            /*
             * This works! On the main thread! (this.int32 underlying buffer is
             * changed by write() on the worker thread)
             */
            while (Atomics.load(this.int32, 0) == 0) {
                if (counter++ % 1000000 == 0) {
                    var t1 = performance.now();
                    if (t1 - t0 > timeout) {
                        this.closed = true;
                        throw "Read timeout";
                    }
                }
                // don't spook a spectre...?
            }
            let message = Atomics.load(this.int32, 0);
            switch (message) {
                case WebSocketTransport.MESSAGE_SOCKET_CLOSED:
                  this.closed=true;
                    throw "WebSocketTransportClient: received MESSAGE_SOCKET_CLOSED";
                case WebSocketTransport.MESSAGE_WINDOW_UNLOAD:
                  this.closed=true;
                    throw "WebSocketTransportClient: received MESSAGE_WINDOW_UNLOAD";
            }
        }
        var len = this.int32[1];
        /*
         * prep for next read (could probably make this array copy nicer, but
         * dev tools got hung up...?)
         */
        Atomics.store(this.int32, 0, WebSocketTransport.MESSAGE_WAIT);
        let result = [];
        for (let idx = 0; idx < len; idx++) {
            result[idx] = this.int32[idx + 2];
        }
        return result;
    }
    write(message, data) {
        var v = data.length;
        Atomics.store(this.int32, 1, data.length);
        this.int32.set(data, 2);
        /*
         * and now...store the messaage code and wake one sleeping thread
         * 
         */
        Atomics.store(this.int32, 0, message);
        Atomics.notify(this.int32, 0, 1);
    }
}
class WebSocketTransportSocketChannel extends WebSocketTransport {
    constructor() {
        super();
        onmessage = e => {
            switch (e.data.message) {
                case "start":
                    this.codeServerWs = e.data.codeServerWs;
                    this.codeServer = e.data.codeServer;
                    this.setBuffers(e.data.inBuffer.sharedArrayBuffer, e.data.outBuffer.sharedArrayBuffer);
                    this.start();
                    break;
                case "data":
                    this.onData();
                    break;
            }
        };
        /*
         * worker receives close event (caused by window unload) 
         */
        self.addEventListener('close', e => {
            this.outBuffer.write(WebSocketTransport.MESSAGE_WINDOW_UNLOAD, new TextEncoder().encode(""));
        });
        
    };
    start() {
        this.socket = new WebSocket(`ws://${this.codeServerWs}/jsCodeServerWs.tcp?gwt.codesvr=${this.codeServer}`);
        this.socket.addEventListener('open', e => {
            this.onOpen();
        });
        this.socket.onmessage = e => this.onMessage(e);
        this.socket.onclose = e => {
            this.outBuffer.write(WebSocketTransport.MESSAGE_SOCKET_CLOSED, new TextEncoder().encode(""));
        };
    }
    onOpen() {
        postMessage({
            message: "connected"
        });
    }
    onData() {
        /*
         * received data from gwt.hosted js - send to codeserver
         */
        let bytes = this.read(); // will be int array (but int in byte range -
        // in fact ascii ) - in our case b64 text
        let packet = "";
        for (var idx = 0; idx < bytes.length; idx++) {
            packet += String.fromCharCode(bytes[idx]);
        }
        this.socket.send(packet);
    }
    onMessage(e) {
        /*
         * received data from codeserver - send to gwt.hosted js
         */
        this.outBuffer.write(WebSocketTransport.MESSAGE_DATA_PACKET, new TextEncoder().encode(e.data));

    }
}

function WebSocketTransport_maybeStartWorker() {
    if (WebSocketTransport_is_worker) {
        new WebSocketTransportSocketChannel();
    }
}
var WebSocketTransport_is_worker = typeof WorkerGlobalScope !== 'undefined' && self instanceof WorkerGlobalScope;
WebSocketTransport_maybeStartWorker();