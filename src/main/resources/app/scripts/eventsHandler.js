// <!--

function eventsHandler(id,conf) {
var eh = DataHandler({
    id: id,
    loadMeta: function() {
        var caller=this;
        REST_api.eventsMeta(null,null,function (url, data) {
            if (data.result) {
                caller.meta=data.result; 
                caller.filter.from=caller.meta.ranges.default[1];
                caller.filter.to=caller.meta.ranges.default[2];
		caller.onLoadedMeta(data.result);
            }
        });
    },
    loadData: function() {
        var caller=this;
        REST_api.events(
            this.filter.room,
            this.filter.name,
            this.filter.from,
            this.filter.to,
            this.filter.status,
            this.filter.trainer,
            this.filter.participant,
            this.filter.category,
            this.filter.course,
            this.filter.group,
            this.filter.dayOfWeek, // [-1],
            function(url,data) {
                if(data.result)  {
                    caller.data=data.result;
                    caller.onLoadedData(data.result);
                }
            });
    },
    renderData: function() {
        var caller=this;
        var calendar = new Calendar({
            locale:userLocale,
            toPrev: (this.filter.prev) ? "toPrevPeriod(); loadEvents(); " : null,
            toNext: (this.filter.next) ? "toNextPeriod(); loadEvents();" : null,
            showDates: true,
            renderCellEntryActions: function(id,entry){
                var s="";
                return s;
            },
            renderCellEntry: function(id, entry, dMin, dMax) {
                var s="";
                var cst="";
                s+="<fieldset  id='"+id+"' class='calendar_cell "+cst+"' tMin='"+dMin.getTime()+"' tMax='"+dMax.getTime()+"'>";
                s+="<legend class='calendar_cell'>";
                s+="<font size=-2>";
                s+=""+this.toTimeHM(entry.start)+" - "+this.toTimeHM(entry.end);
                s+="</font>";
                s+="</legend>";
                s+=""+((entry.shortName) ? entry.shortName: entry.name);
                s+="</fieldset>";
                return s;
            },
            adjustableElements: function() {
                return document.getElementsByTagName("fieldset");
            }
        });
        calendar.render('data_view',this.data);
    }
})
.configure(null, function(dh){
    dh.filterRenderer.configure({
        viewElementId: 'h_filter',    
        viewSwithWidth: 700,
        columnSize: 10,
        onStart: function(dfs,params) {
            
            return "";
        },
        isVisible: function(item) {
            var visible=true;
            var el=(item && item.id) ? document.getElementById('filter_'+item.id) : null;
            if(el && el.style.display!='none') {
            } else if(el){
                visible=false;
            }else {
                visible=false;
            }
            return visible;
        },
        toggleVisibility: function(item) {
            var elementName="filter_"+item.id;
            var el=document.getElementById(elementName);
            var elMin=document.getElementById("min_"+elementName);
            if(el) {
                if(el.style.display!='none') {
                    el.style.display='none';
                    if(elMin) elMin.style.display='block';
                } else {
                    el.style.display=null;
                    if(elMin) elMin.style.display='none';
                }
            }
        },
        doStarter: function(items) {
            {   // switch filter view area from "h_" to "v_" or vice versa...
                if(document.documentElement.clientWidth>this.viewSwithWidth) {
                    this.viewElementId = this.viewElementId.replace("h_","v_");
                } else {
                    this.viewElementId = this.viewElementId.replace("v_","h_");
                    if(this.columnSize!=-1) {
                        this.columnSize=Math.ceil(document.documentElement.clientWidth/150);
                    }
                }
            }
            return (document.documentElement.clientWidth>this.viewSwithWidth) ? "" : "<table border='0' width='100%'><tr><td valign='top'>";
        },
        doColumn: function(index,item,params) {
            console.info("doColumn: "+index+", "+item.id);
            return (document.documentElement.clientWidth>this.viewSwithWidth) 
            ? "" 
            : "</td></tr><tr><td valign='top'>";
        },
        doDelimiter: function(index,item,params) {
            console.info("doDelim: "+index+", "+item.id);
            return (document.documentElement.clientWidth>this.viewSwithWidth) 
            ? "" 
            : "</td><td valign='top'>";
        },
        doEnder: function(items) {
            return (document.documentElement.clientWidth>this.viewSwithWidth) ? "" : "</td></tr></table>";
        },
        doItem: function(idx,df,params) {
            var s="";
            if(df) {
                var cl="filter";
                var a=false;
                var si="";

                // build self/no self selector
                si="<div class='filter_sel' id='min_filter_"+df.id+"'>";
                s+="<fieldset class='filter'><legend class='filter' onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]);'>"+df.title+"</legend>";
                s+="<table class='filter' id='filter_"+df.id+"'>";

                if(df.valueOf(null)) {
                    a=df.isActive(null);
                    s+="<tr class='filter'><td class='"+cl+(a ? "_active": "")+"'";
                    s+=" onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.dataFilters[\""+df.id+"\"].onActivate(null)"+"; dh.renderFilter(); dh.loadData(); '";
                    if(df.columns) s+=" colspan='"+df.columns+"'";
                    s+=">";
                    s+=df.valueOf(null);
                    s+="</td></tr>";
                    if(a) si+=df.valueOf(null)+" ";
                }

                if(df.columns)
                    s+="<tr class='filter'>";

                var ac=0;
                for(var k in df.keys()) {
                    if(!df.isValueKey(k)) continue;
                    a=df.isActive(k);

                    if(df.columns) {
                        var n=new Number(k);
                        if(n>0 && (n % df.columns) == 0){
                            s+="</tr><tr class='filter'>";
                        }
                    } else {
                        s+="<tr class='filter'>";
                    }

                    s+="<td class='"+cl+((a) ? "_active": "")+"'";
                    s+=" onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.dataFilters[\""+df.id+"\"].onActivate(\""+k+"\")"+"; dh.renderFilter(); dh.loadData(); '>";
                    s+=df.valueOf(k);
                    if(df.columns)
                        s+="</td>";
                    else
                        s+="</td></tr>";
                    if(a) {
                        if(ac>0) si+=' ';
                        si+=df.valueOf(k);
                        ac++;
                    }
                }
                if(df.columns) s+="</tr>";

                s+="</table>";
                s+=si+"</div>";
                s+="</fieldset>\n";
            }
            return s;
        }
    });
})
.addDataFilter(SimpleDataFilter('my','#{My}', { add:function(id,val) {this[id]=val; return this;}}.add(""+userId,"#{my}").add("!"+userId,"#{not_my}"), 'participant', {nullValue:'#{ALL}', keyIndex: -1, valueIndex: 0, visible: isValidUser()}))
.addDataFilter(
    TimeRangeDataFilter(
        'month',
        '#{Month}', 
        'this.meta.ranges.months', 
        {
            columns:4, 
            nullValue:'#{NOW}',
            locale: userLocale,
            valueOf: function(key) {
                var v=(key) ? this.data[key] : null; 
                return (v) 
                    ? new Date(v[1]).toLocaleDateString(this.locale, { month: 'short' }) 
                    : null;
            }
        }
    )
)
.addDataFilter(TimeRangeDataFilter('week','#{Week}', 'this.meta.ranges.weeks', {columns:4, nullValue:'#{Now}', nullData: 'this.meta.ranges.default'}))
.addDataFilter(SimpleDataFilter('room','#{Rooms}', 'this.meta.rooms', 'room', {nullValue:'#{ALL}', valueIndex: -1}))
.addDataFilter(SimpleDataFilter('group','#{Groups}', 'this.meta.groups', 'group', {nullValue:'#{ALL}', valueIndex: -1}))
.addDataFilter(SimpleDataFilter('trainer','#{Trainers}', 'this.meta.trainers', 'trainer', {nullValue:'#{ALL}', keyIndex: -1, valueIndex: 0}))
.addDataFilter(SimpleDataFilter('course','#{Courses}', 'this.meta.courses', 'course', {nullValue:'#{ALL}', valueIndex: -1}))
.addDataFilter(SimpleDataFilter('category','#{Categories}', 'this.meta.categories', 'category', {nullValue:'#{ALL}', valueIndex: -1}))
;

eh.configure(conf);
return eh;

}


// -->