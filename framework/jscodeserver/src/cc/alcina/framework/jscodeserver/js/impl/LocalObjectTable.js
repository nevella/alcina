class gwt_hm_LocalObjectTable {
    objects = new Map();
    ids = new Map();
    nextId = 0;
    dontFree = false;
    jsIdentitySafe = true;
    constructor() {}
    setFree(id) {
        var obj = this.getById(id);
        this.objects.remove(id);
        this.ids.remove(obj);
    }
    add(obj) {
        var id = this.nextId++;
        this.set(id, obj);
        if (!this.jsIdentitySafe) {
            throw "not implemented";
        }
    }
    set(id, obj) {
        if (!this.jsIdentitySafe) {
            throw "not implemented";
        }
        this.objects.set(id, obj);
        this.ids.set(obj, id);
    }
    freeAll() {
        this.objects.clear();
        this.ids.clear();
    }
    getById(id) {
        if (!this.objects.has(id)) {
            throw `Object not found: ${id}`;
        }
        return this.objects.get(id);
    }
    getObjectId(object) {
        if (!this.ids.has(object)) {
            throw `Id not found: ${object}`;
        }
        return this.ids.get(object);
    }
    ensureObjectRef(obj) {
      if (!this.ids.has(obj)) {
          this.add(obj);
      }
      return this.ids.get(obj);
  }
   
}