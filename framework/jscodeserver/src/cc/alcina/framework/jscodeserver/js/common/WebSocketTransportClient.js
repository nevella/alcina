class WebSocketTransportClient extends WebSocketTransport {
    connected;
    constructor(codeServer, codeServerWs, moduleName) {
        super();
        this.codeServer = codeServer;
        this.codeServerWs = codeServerWs;
        this.moduleName = moduleName;
    }
    connect(onConnect) {
        /*
         * load socket worker, wait for connect event
         */
        this.setBuffers(new SharedArrayBuffer(WebSocketTransport.BUFFER_SIZE), new SharedArrayBuffer(WebSocketTransport.BUFFER_SIZE));
        this.socketWorker = new Worker(`/${this.moduleName}/WebSocketTransport.js`);
        this.socketWorker.postMessage({
            message: "start",
            codeServerWs: this.codeServerWs,
            codeServer: this.codeServer,
            inBuffer: this.outBuffer,
            outBuffer: this.inBuffer
        });
        this.socketWorker.onmessage = e => {
            switch (e.data.message) {
                case "connected":
                    this.connected = true;
                    console.log("websocket connect message received from worker by transport client");
                    onConnect();
                    break;
                case "closed":
                    break;
                case "exception":
                    console.log(e.data.exception);
                    break;
            }
        };
    }
    //override (need to send a message to the worker that there's a packet ready)
    sendPacket(message, data) {
        this.outBuffer.write(message, data);
        this.socketWorker.postMessage({
            message: "data"
        });
        return this.read(WebSocketTransport.READ_TIMEOUT);
    }

}