// <!--

function eventsHandler(id,conf) {
var eh = DataHandler({
    id: id,
    cellIconSize: 20,
    cellActionIconSize: 12,
    basket: {},
    basketView: 'data_view2',
    basketActions: {
        apply: "#{Apply}",
        remove: "#{Remove}",
        confirm: "#{Confirm}",
        clear: "#{Clear_basket}"
    },
    loadMeta: function() {
        var caller=this;
        REST_api.eventsMeta(null,null,function (url, data) {
            if (data.result) {
                caller.meta=data.result; 
                caller.filter.from=caller.meta.ranges.default[1];
                caller.filter.to=caller.meta.ranges.default[2];
		caller.onLoadedMeta(data.result);
                caller.loadBasket();
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
            toPrev: (caller.filter.prev) ? "var dh=getDataHandler(\""+caller.id+"\"); if(dh) { dh.toPrevPeriod(); dh.loadData(); }" : null,
            toNext: (caller.filter.next) ? "var dh=getDataHandler(\""+caller.id+"\"); if(dh) { dh.toNextPeriod(); dh.loadData(); }" : null,
            showDates: true,
            renderCellEntryActions: function(id,entry){
                var s="";
                return s;
            },
            renderCellEntry: function(id, entry, dMin, dMax) {
                var iconSize=""+caller.cellIconSize+"px";
                var actionIconSize=""+caller.cellActionIconSize+"px";

                var s="";
                var cst="";
                s+="<fieldset  id='"+id+"' class='calendar_cell "+cst+"' tMin='"+dMin.getTime()+"' tMax='"+dMax.getTime()+"'>";
                s+="<legend class='calendar_cell'>";
                s+="<font size=-2>";
                s+=""+this.toTimeHM(entry.start);//+" - "+this.toTimeHM(entry.end);
                s+="</font>";
                s+="</legend>";

                if(1==1) {
                    // build overlayed images with optional action...
                    var action=null;
                    var actionIcon=null;
                    if(this.basket && this.basket[entry.id]) {
                        action = "<a onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh) dh.removeFromBasket(\""+entry.id+"\");' class='cell_action'>";
                        actionIcon="images/icons8-ok-90.png";
                    } else {
                        action = "<a onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh) dh.addToBasket(\""+entry.id+"\");' class='cell_action'>";
                        actionIcon="images/icons8-circle-90.png";
                    }
    
                    s+=(action)?action:"";
                    if(entry.icon) {
                        var cIcon=entry.icon;
                        s+="<img class='overlay_wd' src='images/"+cIcon+"' alt='remove' width='"+iconSize+"'>";
                        if(entry.myState) {
                            cIcon=cIcon.replace('90','144');
                            s+="<img class='overlay_wdm' src='images/"+cIcon+"' alt='remove' width='"+iconSize+"'>";
                        }
                    }
                    s+=""+((entry.shortName) ? entry.shortName: entry.name);
                    if(action && actionIcon) {
                        s+="<img class='overlay_wda' src='"+actionIcon+"' alt='add' width='"+actionIconSize+"'>";
                        s+="</a>";
                    }
                } else {
                    s+=""+((entry.shortName) ? entry.shortName: entry.name);
                }

                s+="</fieldset>";
                return s;
            },
            adjustableElements: function() {
                return document.getElementsByTagName("fieldset");
            }
        });
        calendar.render('data_view',this.data);
    },
    loadBasket: function() {
        var current=this;
        REST_api.getEventsInBasket(function (url, data) {
            if (data.result) {
                current.basket=data.result;
                current.renderBasket();
                current.renderData();
            }
        });
    },
    addToBasket: function (tsId) {
        var current=this;
        REST_api.addEventsToBasket(tsId,function (url, data) {
            if (data.result) {
                current.loadBasket();
            }
        });
    },
    removeFromBasket: function (tsId) {
        var current=this;
        REST_api.removeEventsFromBasket(tsId,function (url, data) {
            //debugRR(url, data);

            if (data.result) {
                current.loadBasket();
            }
        });
    },
    applyForBasket: function(tsId, action) {
        var current=this;
        var email=userId;
        if(!email) {
            var el=document.getElementById('temp_email');
            if(!el) {
                var s="";
                s+="<table align='center'><tr><td align='right'>#{User_name}</td><td align='left'><input id='temp_email' type='text' value=''></td><td><input type='button' value='#{Apply}' onclick='applyForBasket("+((tsId) ? "\""+tsId+"\"": "null")+",\""+action+"\");'></td></tr></table>";
                el=document.getElementById("pane_top");
                if(el) {
                    el.innerHTML+=s;
                }
                return;
            } else {
                email=el.value;
            }
        }
        REST_api.applyForGroups(email, action, function (url, data) {
            if (data.result) {
                current.loadBasket();
                for(var i in data.result) {
                    if('added'==data.result[i] || 
                    'cancelled'==data.result[i] ||
                    'confirmed'==data.result[i]) {
                        current.loadData();
                        break;
                    }
                }
            }
        });
    },
    renderBasket: function() {
        var caller=this;
        if(this.basket && Object.keys(this.basket).length>0) {
            var s="<fieldset class='calendar_basket'><legend class='calendar_basket'>#{Basket}</legend><table class='calendar_basket'>";
            if(1==1) {
                s+="<caption class='basket_action'><table width='100%'><tr class='basket_action'><td class='basket_action'>";
                s+="<a class='basket_action' onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh) dh.applyForBasket(null,\"apply\");'>"+this.basketActions.apply+"</a>";
                //s+="</td></tr><tr class='basket_action'><td class='basket_action'>";
                s+="</td><td class='basket_action'>";
                s+="<a class='basket_action' onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh) dh.applyForBasket(null,\"remove\");'>"+this.basketActions.remove+"</a>";
                if(userHasRole('admin')) {
                    //s+="</td></tr><tr class='basket_action'><td class='basket_action'>";
                    s+="</td><td class='basket_action'>";
                    s+="<a class='basket_action' onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh) dh.applyForBasket(null,\"confirm\");'>"+this.basketActions.confirm+"</a>";
                }
                //s+="</td></tr><tr><td>";
                s+="</td><td class='basket_action'>";
                s+="<a class='basket_action' onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh) dh.removeFromBasket(-1);'>"+this.basketActions.clear+"</a>";
                s+="</td></tr></table></caption>";
            }
            var c=0;
            s+="\n<tr class='calendar_basket'>";
            for(var i in this.basket) {
                var entry=this.basket[i];
                if(entry){
                    c++;
                    var cst="";
                    s+="<td>";
                    s+="<fieldset class='calendar_cell "+cst+"' style='position: relative;'>";
                    s+="<legend class='calendar_cell'>";
                    s+="<font size=-2>";
                    s+=""+this.toDateYMD(entry.start)+" "+this.toTimeHM(entry.start)+" - "+this.toTimeHM(entry.end);
                    s+="</font>";
                    s+="</legend>";
                    s+="<a onclick='var dh=getDataHandler(\""+caller.id+"\"); if(dh && dh.removeFromBasket(\""+entry.id+"\")) dh.loadBasket();'><img src='images/icons8-del-90.png' alt='remove' width='12px'></a>";
                    s+="&nbsp;"+entry.name+"  <font size=-1><br/>"+entry.trainer+"</font>";
                    s+="</fieldset>";

                    if((c % 3) ==0)
                        s+="</td></tr><tr class='calendar_basket'>";
                    else
                        s+="</td>";
                }
            }
            s+="</tr>";
            s+="</table></fieldset>";
     
            var div=document.getElementById(this.basketView);
            if(div) { div.innerHTML=s; }
        } else {
            var div=document.getElementById(this.basketView);
            if(div) { div.innerHTML=""; }
        }
    },
    toPrevPeriod: function() {
        if(this.filter.prev || this.filter.prev==0) {
            this.filter.from=this.filter.prev[1];
            this.filter.to=this.filter.prev[2];
            this.renderFilter();
        }
    },
    toNextPeriod: function() {
        if(this.filter.next) {
            this.filter.from=this.filter.next[1];
            this.filter.to=this.filter.next[2];
            this.renderFilter();
        }
    }
})
.configure(null,function(dh) {configureFilterRendering3(dh)})
/*
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
            //console.info("doColumn: "+index+", "+item.id);
            return (document.documentElement.clientWidth>this.viewSwithWidth) 
            ? "" 
            : "</td></tr><tr><td valign='top'>";
        },
        doDelimiter: function(index,item,params) {
            //console.info("doDelim: "+index+", "+item.id);
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
}) */
;

eh.configure(conf);
return eh;

}



function configureFilterRendering1(dh) {
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
            //console.info("doColumn: "+index+", "+item.id);
            return (document.documentElement.clientWidth>this.viewSwithWidth) 
            ? "" 
            : "</td></tr><tr><td valign='top'>";
        },
        doDelimiter: function(index,item,params) {
            //console.info("doDelim: "+index+", "+item.id);
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
}

function configureFilterRendering2(dh) {
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
                    if(elMin) elMin.style.display='';
                } else {
                    el.style.display=null;
                    el.style.zIndex=1000;
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
                        this.columnSize=Math.ceil(document.documentElement.clientWidth/100);
                    }
                }
            }
            //return "<table border='0' width='100%'><tr><td valign='top'>";
            return "<table class='filter_cell'><tr class='filter_cell'><td class='filter_cell'>";
        },
        doColumn: function(index,item,params) {
            return "</td></tr><tr class='filter_cell'><td class='filter_cell'>";
        },
        doDelimiter: function(index,item,params) {
            return (document.documentElement.clientWidth>this.viewSwithWidth) 
            ? "</td></tr><tr class='filter_cell'><td class='filter_cell'>"
            : "</td><td class='filter_cell'>";
        },
        doEnder: function(items) {
            return "</td></tr></table>";
        },
        doItem: function(idx,df,params) {
            var s="";
            if(df) {
                var cl="filter";
                var a=false;
                var si="";
                var sx="";
                var hasSI=false;

                // build self/no self selector
                si="<span class='filter_sel' id='min_filter_"+df.id+"'>";
                s+="<span class='filter_main'><span class='filter' onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]);'>"+df.title;
                s+="</span>";
                sx+="<div class='filter_exp' id='filter_"+df.id+"'>";
                sx+="<table class='filter'>";

                if(df.valueOf(null)) {
                    a=df.isActive(null);
                    sx+="<tr class='filter'><td class='"+cl+(a ? "_active": "")+"'";
                    sx+=" onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.dataFilters[\""+df.id+"\"].onActivate(null)"+"; dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]); dh.renderFilter(); dh.loadData(); '";
                    if(df.columns) sx+=" colspan='"+df.columns+"'";
                    sx+=">";
                    sx+=df.valueOf(null);
                    sx+="</td></tr>";
                    if(a) {
                        si+=df.valueOf(null)+" ";
                        hasSI=true;
                    }
                }

                if(df.columns)
                    sx+="<tr class='filter'>";

                var ac=0;
                for(var k in df.keys()) {
                    if(!df.isValueKey(k)) continue;
                    a=df.isActive(k);

                    if(df.columns) {
                        var n=new Number(k);
                        if(n>0 && (n % df.columns) == 0){
                            sx+="</tr><tr class='filter'>";
                        }
                    } else {
                        sx+="<tr class='filter'>";
                    }

                    sx+="<td class='"+cl+((a) ? "_active": "")+"'";
                    sx+=" onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.dataFilters[\""+df.id+"\"].onActivate(\""+k+"\")"+"; dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]); dh.renderFilter(); dh.loadData(); '>";
                    sx+=df.valueOf(k);
                    if(df.columns)
                        sx+="</td>";
                    else
                        sx+="</td></tr>";
                    if(a) {
                        if(ac>0) si+=' ';
                        si+=df.valueOf(k);
                        hasSI=true;
                        ac++;
                    }
                }
                if(df.columns) sx+="</tr>";

                sx+="</table>";
                sx+="</div>";
                si+="</span>";
                s+=sx;
                if(hasSI) {
                    if(document.documentElement.clientWidth>this.viewSwithWidth) s+='<br/>&nbsp;';
                    //else s+='<br/>';
                    s+=si;
                }
                s+="</span>\n";
            }
            return s;
        }
    });
}

function configureFilterRendering3(dh) {
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
        toNumeric: function (val) {
            if("number"==typeof(val)) return val;
            val=""+val;
            if(val.endsWith("px")) val=val.substring(0,val.length-2);
            return new Number(val);
        },
        toggleVisibility: function(item) {
            var elementName="filter_"+item.id;
            var el=document.getElementById(elementName);
            var elMain=document.getElementById("main_"+elementName);
            var elMin=document.getElementById("min_"+elementName);
            if(el) {
                if(el.style.display!='none') {
                    el.style.display='none';
                    if(elMin) elMin.style.display='';
                } else {
                    el.style.zIndex=1000;
                    if(document.documentElement.clientWidth>this.viewSwithWidth) {
                        // no corrections if within table cells...
                        el.style.display=null;
                    } else {
                        var top=evalCascaded(elMain,"offsetTop");
                        var left=evalCascaded(elMain,"offsetLeft");
                        var height=elMain.offsetHeight;//this.toNumeric(window.getComputedStyle(elMain, null).getPropertyValue('height'));
                        var elW=el.offsetWidth;
                        if(left+elW>document.documentElement.clientWidth) {
                            left=document.documentElement.clientWidth-elW-2;
                        }
                        el.style.left=(left-2)+"px";
                        el.style.top=(top+height+2)+"px";
                        el.style.display=null;
                        var elW=el.offsetWidth;
                        if(left+elW>document.documentElement.clientWidth) {
                            left=document.documentElement.clientWidth-elW-2;
                            el.style.left=(left-2)+"px";
                        }
                    }
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
                        this.columnSize=Math.ceil(document.documentElement.clientWidth/100);
                    }
                }
            }
            //return "<table border='0' width='100%'><tr><td valign='top'>";
            return "<table class='filter_cell'><tr class='filter_cell'><td class='filter_cell'>";
        },
        doColumn: function(index,item,params) {
            return "";//"</td></tr><tr class='filter_cell'><td class='filter_cell'>";
        },
        doDelimiter: function(index,item,params) {
            return (document.documentElement.clientWidth>this.viewSwithWidth) 
            ? "</td></tr><tr class='filter_cell'><td class='filter_cell'>"
            : "";
        },
        doEnder: function(items) {
            return "</td></tr></table>";
        },
        doItem: function(idx,df,params) {
            var s="";
            if(df) {
                var cl="filter";
                var a=false;
                var si="";
                var sx="";
                var hasSI=false;

                // build self/no self selector
                var wrap= (document.documentElement.clientWidth>this.viewSwithWidth) ? "white-space: nowrap" : "";
                si="<span class='filter_sel'  style='"+wrap+"' id='min_filter_"+df.id+"'>";
                s+="<span class='filter_main' style='"+wrap+"' id='main_filter_"+df.id+"'><span class='filter' onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]);'>"+df.title;
                s+="</span>";
                sx+="<div class='filter_exp' id='filter_"+df.id+"'>";
                sx+="<table class='filter'>";

                if(df.valueOf(null)) {
                    a=df.isActive(null);
                    sx+="<tr class='filter'><td class='"+cl+(a ? "_active": "")+"'";
                    sx+=" onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.dataFilters[\""+df.id+"\"].onActivate(null)"+"; dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]); dh.renderFilter(); dh.loadData(); '";
                    if(df.columns) sx+=" colspan='"+df.columns+"'";
                    sx+=">";
                    sx+=df.valueOf(null);
                    sx+="</td></tr>";
                    if(a) {
                        si+=df.valueOf(null)+" ";
                        hasSI=true;
                    }
                }

                if(df.columns)
                    sx+="<tr class='filter'>";

                var ac=0;
                for(var k in df.keys()) {
                    if(!df.isValueKey(k)) continue;
                    a=df.isActive(k);

                    if(df.columns) {
                        var n=new Number(k);
                        if(n>0 && (n % df.columns) == 0){
                            sx+="</tr><tr class='filter'>";
                        }
                    } else {
                        sx+="<tr class='filter'>";
                    }

                    sx+="<td class='"+cl+((a) ? "_active": "")+"'";
                    sx+=" onclick='var dh=getDataHandler(\""+df.dhId+"\"); dh.dataFilters[\""+df.id+"\"].onActivate(\""+k+"\")"+"; dh.filterRenderer.toggleVisibility(dh.dataFilters[\""+df.id+"\"]); dh.renderFilter(); dh.loadData(); '>";
                    sx+=df.valueOf(k);
                    if(df.columns)
                        sx+="</td>";
                    else
                        sx+="</td></tr>";
                    if(a) {
                        if(ac>0) si+=' ';
                        si+=df.valueOf(k);
                        hasSI=true;
                        ac++;
                    }
                }
                if(df.columns) sx+="</tr>";

                sx+="</table>";
                sx+="</div>";
                si+="</span>";
                s+=sx;
                if(hasSI) {
                    if(document.documentElement.clientWidth>this.viewSwithWidth) s+='<br/>&nbsp;';
                    //else s+='<br/>';
                    s+=si;
                }
                s+="</span>\n";
            }
            return s;
        }
    });
}

// -->