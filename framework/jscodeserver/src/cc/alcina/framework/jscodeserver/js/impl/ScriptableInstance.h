class ScriptableInstance: public NPObjectWrapper < ScriptableInstance > , SessionHandler {
    friend class JavaObject;
    public:
        ScriptableInstance(NPP npp);
    ~ScriptableInstance();

    void pluginDeath() {
        // our local objects will get freed anyway when the plugin dies
        localObjects.freeAll();
        localObjects.setDontFree(true);
        // TODO(jat): leaving this in causes SEGV upon plugin destruction in the
        // NPN_ReleaseObject call.
        //    if (window) {
        //      NPN_ReleaseObject(window);
        //      window = 0;
        //    }
    }

    // NPObjectWrapper methods
    bool enumeration(NPIdentifier ** values, uint32_t * count);
    bool getProperty(NPIdentifier name, NPVariant * result);
    bool setProperty(NPIdentifier name,
        const NPVariant * value);
    bool invoke(NPIdentifier name,
        const NPVariant * args, unsigned argCount, NPVariant * result);
    bool invokeDefault(const NPVariant * args, unsigned argCount, NPVariant * result);
    bool hasMethod(NPIdentifier name);
    bool hasProperty(NPIdentifier name);

    void dumpJSresult(const char * js);

    int getLocalObjectRef(NPObject * obj);

    NPObject * getLocalObject(int refid) {
        return localObjects.getById(refid);
    }

    bool tryGetStringPrimitive(NPObject * obj, NPVariant & result);

    JavaObject * createJavaWrapper(int objectId);
    void destroyJavaWrapper(JavaObject * );

    static
    const uint32_t VERSION = 1;

    protected:
        virtual void disconnectDetectedImpl();

    private:
        // Map of object ID to JavaObject
        hash_map < int, JavaObject * > javaObjects;
    std::set < int > javaObjectsToFree;

    vector < JavaClass * > classes;

    Plugin & plugin;
    HostChannel * _channel;
    LocalObjectTable localObjects;

    static bool jsIdentitySafe;
    int savedValueIdx;

    // Identifiers
    const NPIdentifier _connectId;
    const NPIdentifier initID;
    const NPIdentifier toStringID;

    const NPIdentifier loadHostEntriesID;
    const NPIdentifier locationID;
    const NPIdentifier hrefID;
    const NPIdentifier urlID;
    const NPIdentifier includeID;
    const NPIdentifier getHostPermissionID;
    const NPIdentifier testJsIdentityID;

    const NPIdentifier connectedID;
    const NPIdentifier statsID;

    const NPIdentifier jsDisconnectedID;
    const NPIdentifier jsInvokeID;
    const NPIdentifier jsResultID;
    const NPIdentifier jsTearOffID;
    const NPIdentifier jsValueOfID;
    const NPIdentifier idx0;
    const NPIdentifier idx1;

    NPObject * window;
    void dupString(const char * str, NPString & npString);
    void dumpObjectBytes(NPObject * obj);
    bool makeResult(bool isException,
        const gwt::Value & value, NPVariant * result);

    // SessionHandler methods
    virtual bool invoke(HostChannel & channel,
        const gwt::Value & thisObj,
            const std::string & methodName, int numArgs,
                const gwt::Value *
                    const args,
                        gwt::Value * returnValue);
    virtual bool invokeSpecial(HostChannel & channel, SpecialMethodId dispatchId,
        int numArgs,
        const gwt::Value *
            const args, gwt::Value * returnValue);
    virtual void freeValue(HostChannel & channel, int idCount,
        const int * ids);
    virtual void sendFreeValues(HostChannel & channel);
    virtual void loadJsni(HostChannel & channel,
        const std::string & js);
    virtual void fatalError(HostChannel & channel,
        const std::string & message);

    void connect(const NPVariant * args, unsigned argCount, NPVariant * result);
    void init(const NPVariant * args, unsigned argCount, NPVariant * result);
    void loadHostEntries(const NPVariant * args, unsigned argCount, NPVariant * result);
    void getHostPermission(const NPVariant * args, unsigned argCount, NPVariant * result);
    void testJsIdentity(const NPVariant * args, unsigned argCount, NPVariant * result);
    gwt::Value clientMethod_getProperty(HostChannel & channel, int numArgs,
        const gwt::Value *
            const args);
    gwt::Value clientMethod_setProperty(HostChannel & channel, int numArgs,
        const gwt::Value *
            const args);

    void JavaObject_invalidate(int objectId);
    bool JavaObject_invoke(int objectId, int dispId,
        const NPVariant * args,
            uint32_t numArgs, NPVariant * result);
    bool JavaObject_getProperty(int objectId, int dispId, NPVariant * result);
    bool JavaObject_setProperty(int objectId, int dispId,
        const NPVariant * value);
    bool JavaObject_getToStringTearOff(NPVariant * result);

    private:
        std::string computeTabIdentity();
    std::string getLocationHref();
};

#
endif