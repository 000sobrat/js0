// <!--
/*
    Defines DataHandler, DataRenderer, DataFilter (with 2 generic implementations).

    Each component allows inline configuration with "configure(conf,init)" 
    where "conf" is overridable properties set, and init - optional function that is passed single param - this)
    to execute in scope of object.
*/

// global access point to data handlers
var DHS = {}
function getDataHandler(id) {
    return DHS[id];
}

function DataHandler(conf) {
    var dh = {
        meta: null,
        filter: {},
        dataFilters: {},
        data: null,
        filterView: 'filter_view',
        dataView: 'data_view',
        filterRenderer: DataRenderer(),
        configure: function(conf,init) {
            if(conf) for(var c in conf) {
                this[c]=conf[c];
            }
            if("function"==typeof(init)) init(this);
            return this;
        },
        init: function() {
            this.loadMeta();
        },
        loadMeta: function() {
            caller.onLoadedMeta(null);
        },
        onLoadedMeta: function(v) { 
            this.meta=v;
            this.renderFilter(); 
            this.loadData(); 
        },
        loadData: function() {
            caller.onLoadedData(null);
        },
        onLoadedData: function(v) { 
            this.data=v;
            this.renderData(); 
        },
        renderFilter: function() {
            // ensure data filters data are initialized...
            var visibility={};
            var params={visibility: visibility};
            try {
                for(var dfi in this.dataFilters) {
                    var df=this.dataFilters[dfi];
                    if(df && df.visible) {
                        if("string"==typeof(df.data)) {
                            df.data=eval(df.data);
                            if(df.nullData && "string"==typeof(df.nullData)) {
                                df.nullData=eval(df.nullData);
                            }
                        }
                        visibility[df.id]= this.filterRenderer.isVisible(df);
                    }
                }
            }catch(e) {
                var a=0;
                console.error("renderFilter:evaluate visibility:ERROR: "+e);
            }
            // do rendering
            try{
                //s=this.renderFilterBody();
                var el=document.getElementById(this.filterRenderer.viewElementId);
                if(el) el.innerHTML="";
                var s=this.filterRenderer.render(this.dataFilters,params);
                el=document.getElementById(this.filterRenderer.viewElementId);
                if(el) el.innerHTML=s;

                // adjust visiblity
                for(var dfi in this.dataFilters) {
                    var df=this.dataFilters[dfi];
                    if(visibility[df.id]) {
                    } else {
                        this.filterRenderer.toggleVisibility(df);
                    }
                }
            } catch(e){
                var a=0;
                console.error("renderFilter:render/adjust visibility:ERROR: "+e);
            }
        },
        renderData: function() {
        },
        addDataFilter: function(df, order) {
            if(df) {
                df.dhId=this.id;
                df.filter=this.filter;
                if(order || order==0) {
                    var sz=Object.keys(this.dataFilters).length;
                    if(order>=sz) {
                        this.dataFilters[df.id]=df;
                    } else {
                        // order items in a[]
                        var a=new Array();
                        var c=0;
                        for(var i in this.dataFilters) {
                            if(order==c) {
                                a.push(df);
                            }
                            a.push(this.dataFilters[i]);
                            c++;
                        }
                        // delete all current fiters...
                        for(var i in this.dataFilters) {
                            delete this.dataFilters[i];
                        }
                        // add items in new order
                        for(var i in a) {
                            var aa=a[i];
                            this.dataFilters[aa.id]=aa;
                        }
                    }
                } else {
                    this.dataFilters[df.id]=df;
                }
            }
            return this;
        },
        removeDataFilter: function(id) {
            if(this.dataFilters) for(var i in this.dataFilters) {
                var df=this.dataFilters[i];
                if(df && df.id==id) {
                    delete this.dataFilters[i];
                    break;
                }
            }
            return this;
        }        
    };

    dh.configure(conf);
    DHS[dh.id]=dh;
    return dh;
}


/*
    data renderer provide multi-step rendering engine with simple default output:
        render -> onStart -> doStart -> [doItem,doDelimiter,doColumn] -> doEnder -> onEnd
    functionality
        onStart prepares for rendering and produces initial output (default - empty string)
        doStarter prefix
        doItem  item to text
        doDelimiter between items text
        doColumn    alternative "hard" delimiter
        doEnder suffix
        onEnd   finalize rendering context and optionally modify produced text

        isVisible   if filter element is visible/expanded, default - true
        toggleVisibility    change filter element visibility mode
*/
function DataRenderer(conf) {
   var dr={
        viewElementId: 'filterView',
        columnSize: -1,
        starter: "",
        delimiter: "",
        ender: "",
        configure: function(conf,init) {
            if(conf) for(var c in conf) {
                this[c]=conf[c];
            }
            if("function"==typeof(init)) init(this);
            return this;
        },
        render: function(items,params) {
            if(!params) params={};
            var s=this.onStart(items,params);
            s+=this.doStarter(items);
            var si="";
            var column=0;
            if(items) for(var i in items) {
                if(!items[i] || !items[i].visible) continue;
                if(""!=si) {
                    console.info("render: "+column+"/"+this.columnSize);
                    if(this.columnSize==column) {
                        si+=this.doColumn(i,items[i],params);
                        column=0;
                    } else {
                        si+=this.doDelimiter(i,items[i],params);
                    }
                }
                si+=this.doItem(i,items[i],params);
                column++;
            }
            s+=si;
            s+=this.doEnder(items);
            s=this.onEnd(s,items,params);
            return s;
        },
        onStart: function(items,params){return "";},
        onEnd: function(s,items,params){return s;},
        doStarter: function(items){return (this.starter) ? this.starter : "";},
        doColumn: function(index,item,params) { return "";},
        doDelimiter: function(index,item,params){return (this.delimiter) ? this.delimiter : "";},
        doItem: function(index,item,params){return ""+item;},
        doEnder: function(items){return (this.ender) ? this.ender : "";},
        isVisible: function(item){return true;},
        toggleVisibility: function(item) {}
   };

    if(conf) for(var c in conf) {
        dr[c]=conf[c];
    }
    return dr;
}

function DataFilter(id, title, data, conf) {
    var df={
        dhId:null,
        id: id, // key in filter
        title: title,
        visible: true,
        data: data,
        nullValue: null,
        nullData: null,
        configure: function(conf,init) {
            if(conf) for(var c in conf) {
                this[c]=conf[c];
            }
            if("function"==typeof(init)) init(this);
            return this;
        },
        keys: function() {},
        isValueKey: function(key) {return true;},
        isActive: function(key){ return false;},
        valueOf: function(key) { return null;},
        onActivate: function(key) {}
    };

    if(conf) for(var c in conf) {
        df[c]=conf[c];
    }
    return df;
}

function SimpleDataFilter(id,title,data,prop,conf) {
    var df=new DataFilter(id,title,data, {
        filter: null,
        prop: prop,
        keyIndex: -1, // if -1 key and filter key match, otherwise filter key is indexed value in data...
        valueIndex: 0, // if -1 -> key is the value
        keys: function() {
            return this.data;
        },
        isValueKey: function(key) {
            return !("function"==typeof(this.data[key]));
        },
        isActive: function(key) {
            var v=(key) ? (this.keyIndex==-1) ? key : this.data[key] : (this.nullData) ? this.nullData : null;
            if(v && this.keyIndex!=-1) {
                v=v[this.keyIndex];
            }
            var active = (!key && this.nullValue && this.filter[this.prop]==null) || key == this.filter[this.prop];
           return active;
        },
        valueOf: function(key) { 
            if(!key) return this.nullValue;
            if(this.valueIndex==-1) return key;
            var v=this.data[key];
            if(this.valueIndex) {
                try{
                    v=v[this.valueIndex];
                } catch(e) {
                    console.error("SDF["+this.id+"].valueOf["+key+"]:ERROR: "+e);
                }
            }else if(this.valueIndex==0) {
                if("string"==typeof(v)) {
                } else {
                    v=v[this.valueIndex];
                }
            }
            return v;
        },
        onActivate: function(key) {
            var v = (this.keyIndex==-1) ? key : (key || key==0) ? this.data[key] : null;
            if((key || key==0) && (v || v == 0)) {
                if(this.keyIndex!=-1) {
                    try{
                        v=v[this.keyIndex];
                    }catch(e) {
                        console.error("SDF["+this.id+"].onActivate["+key+"]:ERROR: "+e);
                    }
                }
                this.filter[this.prop]= v;
            } else {
                if(!key && this.nullData) {
                    this.filter[this.prop]=this.nullData;
                } else {
                    this.filter[this.prop]=null;
                }
            }

        }
    });

    df.configure(conf);
    return df;
}

function TimeRangeDataFilter(id, title, data, conf) {
    var df=new DataFilter(id,title,data, {
        filter: null,
        isActive: function(key) {
            var v=(key) ? this.data[key] : (this.nullData) ? this.nullData : null;
            var active = v && v.length>1 && this.filter.from==v[1] && this.filter.to==v[2];
            if(active && key) {
                // prev/next
                try{
                    this.filter.prev=this.data[new Number(key)-1];
                }catch(e){ this.filter.prev=null;}
                try{
                    this.filter.next=this.data[new Number(key)+1];
                }catch(e){ this.filter.next=null;}
           }
           return active;
        },
        keys: function() { return this.data;},
        valueOf: function(key) { 
            if(!key) return this.nullValue;
            return (key && this.data[key]) 
                ? this.data[key][0] 
                : null;
        },
        onActivate: function(key) {
            if(key && this.data[key] && this.data[key].length>2) {
                var v=this.data[key];
                this.filter.fromToId=this.id;
                this.filter.from=v[1];
                this.filter.to=v[2];
            } else {
                if(!key && this.nullData) {
                    this.filter.fromToId=this.id;
                    this.filter.from=this.nullData[1];
                    this.filter.to=this.nullData[2];
                } else {
                    this.filter.fromToId=null;
                    this.filter.from=null;
                    this.filter.to=null;
                }
            }

        }
    });

    df.configure(conf);
    return df;
}


// -->