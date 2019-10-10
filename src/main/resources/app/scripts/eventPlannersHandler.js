// <!--
var seasonNames=["#{Spring}","#{Summer}","#{Autumn}","#{Winter}"];
var quarterNames=["#{Q1}","#{Q2}","#{Q3}","#{Q4}"];
var weekDayNames={"2":"#{Mon}","3":"#{Tue}","4":"#{Wed}","5":"#{Thu}","6":"#{Fri}","7":"#{Sat}","1":"#{Sun}"};
var weekDaysOrder=[2,3,4,5,6,7,1]


function eventPlannersHandler(id,conf) {
var eh = eventsHandler(id, {
    generatedData: null,
    editor: createDataHandlerEditor(),
    loadMeta: function() {
        var caller=this;
        REST_api.eventsMeta(null,null,function (url, data) {
            if (data.result) {
                caller.meta=data.result; 
                if(caller.meta && caller.filter && !caller.meta.from && caller.meta.yearSeason) {
                    caller.filter.from=caller.meta.yearSeason[1];
                    caller.filter.to=caller.meta.yearSeason[2];
                }
		caller.onLoadedMeta(data.result);
            }
        });
    },
    loadData: function() {
        var caller=this;
        this.editor.clear();
        REST_api.eventPlanners(
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
            [-1],
            function(url,data) {
                if(data.result)  {
                    caller.data=data.result;
                    caller.onLoadedData(data.result);
                }
            });
    },
    renderData: function() {
        this.renderEPs();
        this.generateData();
        /*
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
        */
    },
    renderEPs: function() {
        var edit=this.filter.edit==true || userHasRole('admin');
        var s="";
        var actionIconSize = '25px';
        var wdIconSize = '20px';
    
        s+="<div style='overflow: scroll; height: 200px; width: 100%;'><table class='tep'>";
        s+="<caption class='tep'>";
        if(this.filter.prev || this.filter.prev==0) {
            s+="<span ttal='#{Prev}'><img class='tep' src='images/icons8-shevrone-left-90.png' height='15px' width='20px' onclick='var dh=getDataHandler(\""+this.id+"\"); if(dh) { dh.toPrevPeriod(); dh.loadData(); }'></span>";
        } 
        s+=""+toDateYMD(this.filter.from)+" - "+toDateYMD(this.filter.to)+"";
        if(this.filter.next) {
            s+="<span ttal='#{Next}'><img class='tep' src='images/icons8-shevrone-right-90.png' height='15px' width='20px' onclick='var dh=getDataHandler(\""+this.id+"\"); if(dh) { dh.toNextPeriod(); dh.loadData(); }'></span>";
        } 
        s+="</caption>";
    
        s+="<tr class='tep'>";
        s+="<th class='tep'>";
        if(edit) s+="#{TEP_Action}";
        s+="</th>";
        //s+="<th class='tep'>#{TEP_From}</th>";
        //s+="<th class='tep'>#{TEP_To}</th>";
        s+="<th class='tep'>#{TEP_Weekday}</th>";
        s+="<th class='tep'>#{TEP_Start}</th>";
        s+="<th class='tep'>#{TEP_Duration}</th>";
        //s+="<th class='tep'>#{TEP_End}</th>";
        s+="<th class='tep'>#{TEP_Room}</th>";
        s+="<th class='tep'>#{TEP_Group}</th>";
        s+="</tr>";

        s+="<tr class='tep'>";
        s+="<th class='tep' style='text-align: left'>";
        if(edit) {
            var hasAdded=this.editor.isAddedEvent(null);
            var hasDeleted=this.editor.isDeletedEvent(null);
            var hasChanged=this.editor.isChangedEvent(null,null);
            s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.addEvent(\"added_"+(new Date())*1+"\")) de.dh.renderData();' ttal='#{AddNew}'><img src='images/icons8-add-db-90.png' width='"+actionIconSize+"' alt='#{AddNew}'></a>";
            if(hasAdded || hasChanged || hasDeleted) {
                if(hasDeleted) {
                    s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.undeleteEvent(null)) de.dh.renderData();' ttal='#{UndeleteAll}'><img src='images/icons8-restore-90.png' width='"+actionIconSize+"' alt='#{UndeleteAll}'></a>";
                }
                s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.applyEventChanges(null)) de.dh.renderData();' ttal='#{SaveAll}'><img src='images/icons8-save-all-90.png' width='"+actionIconSize+"' alt='#{SaveAll}'></a>";
                if(hasChanged || hasAdded) {
                    s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.undoEventChanges(null)) de.dh.renderData();' ttal='#{UndoAll}'><img src='images/icons8-undo-90.png' width='"+actionIconSize+"' alt='#{UndoAll}'></a>";
                }
            }
        }
        s+="</th>";
        //s+="<th class='tep'></th>";
        //s+="<th class='tep'></th>";
        s+="<th class='tep'>";
        s+="<table class='tep' border='1' width='100%'><tr>";
        for(var wd in weekDaysOrder) {
            s+="<td>"+weekDayNames[weekDaysOrder[wd]]+"</td>";
        }
        s+="</tr></table>";
        s+="</th>";
        s+="<th class='tep'></th>";
        s+="<th class='tep'></th>";
        //s+="<th class='tep'></th>";
        s+="<th class='tep'></th>";
        s+="<th class='tep'></th>";
        s+="</tr>";

        // ad "new item" row if in edit mode... ???
        if(edit) {
        }
    
        if(this.data) for(var ei in this.data) {
            var tep=this.data[ei];
            if(tep && !tep.hidden) {
                var isDeleted=this.editor.isDeletedEvent(tep.id);
                var isChanged=this.editor.isChangedEvent(tep.id,null);
                //console.log('renderEvent(m='+isChanged+', d='+'+isDeleted+'+'): '+toJSON(tep));     
          
                s+="<tr class='tep"+((isDeleted) ? "_deleted" : "")+"'>";
                s+="<td class='tep'>";
                if(edit) {
                    if(isDeleted) {
                        s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.undeleteEvent(\""+tep.id+"\")) de.dh.renderData();' ttal='#{Undelete} "+asTimeHM(tep.start)+" "+tep.name+"'><img src='images/icons8-restore-90.png' width='"+actionIconSize+"' alt='#{Undelete}'></a>";
                    } else {
                        s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.deleteEvent(\""+tep.id+"\")) de.dh.renderData();' ttal='#{Delete} "+asTimeHM(tep.start)+" "+tep.name+"'><img src='images/icons8-trashcan-90.png' width='"+actionIconSize+"' alt='#{Delete}'></a>";
                        if(isChanged) {
                            s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.applyEventChanges(\""+tep.id+"\")) de.dh.renderData();' ttal='#{Save} "+asTimeHM(tep.start)+" "+tep.name+"'><img src='images/icons8-save-90.png' width='"+actionIconSize+"' alt='#{Save}'></a>";
                            s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.undoEventChanges(\""+tep.id+"\",null)) de.dh.renderData();' ttal='#{Undo} "+asTimeHM(tep.start)+" "+tep.name+"'><img src='images/icons8-undo-90.png' width='"+actionIconSize+"' alt='#{Undo}'></a>";
                        }
                    }
                }
                s+="</td>";
                //s+="<td class='tep'>"+toDateYMD(tep.from)+"</td>";
                //s+="<td class='tep'>"+toDateYMD(tep.to)+"</td>";
          
                // weekDay
                s+="<td class='tep"+(this.editor.isChangedEvent(tep.id,'weekDays') ? "_modified" : "")+"'>";
                s+="<table class='tep' border='1' width='100%'><tr>";
                var wds="";
                for(var di in tep.weekDays) {
                    if(wds) wds+=",";
                    wds+=tep.weekDays[di];
                }

                for(var wd in weekDaysOrder) {
                    s+="<td>";
                    wd=weekDaysOrder[wd];
                    var found=false;
                    for(var di in tep.weekDays) {
                        if(wd==tep.weekDays[di]) {
                            found=true;
                            break;
                        }
                    }
                    if(found) {
                        if(edit) {
                            var v="";
                            for(var di in tep.weekDays) {
                                if(wd==tep.weekDays[di]) continue;
                                if(v) v+=",";
                                v+=tep.weekDays[di];
                            }
                        s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.modifyEvent(\""+tep.id+"\",\"weekDays\",["+v+"])) de.dh.renderData();'>";
                    }
                    s+="<img src='images/icons8-ok-90.png' width='"+wdIconSize+"'>";
                    if(edit) {
                        s+="</a>";
                    }
                    //s+="<img src='images/transparent1x1.png'  width='"+wdIconSize+"'>";
                    } else {
                        if(edit) {
                            var v=wds;
                            if(v) v+=",";
                            v+=wd;
                            s+="<a class='tep' onclick='var de=getDHEditor(\""+this.id+"\"); if(de && de.modifyEvent(\""+tep.id+"\",\"weekDays\",["+v+"])) de.dh.renderData();'>";
                            s+="<img src='images/icons8-circle-90.png' width='"+wdIconSize+"'>";
                            s+="</a>";
                        } else {
                            s+="<img src='images/transparent1x1.png' width='"+wdIconSize+"'>";
                        }
                    }
                    s+="</td>";
                }
                s+="</tr></table>";
                s+="</td>";
          
                // start
                s+="<td class='tep"+(this.editor.isChangedEvent(tep.id,'start') ? "_modified" : "")+"'>";
                if(edit) {
                    s+="<input class='tep' type='text' size='5' value='"+asTimeHM(tep.start)+"' onchange='var de=getDHEditor(\""+this.id+"\"); if(de && de.modifyEvent(\""+tep.id+"\",\"start\",fromTimeHM(event.currentTarget.value))) de.dh.renderData();'>";
                } else {
                    s+=asTimeHM(tep.start);
                }
                s+="</td>";

                // duration
                s+="<td class='tep"+(this.editor.isChangedEvent(tep.id,'duration') ? "_modified" : "")+"'>";
                if(edit) {
                    s+="<input class='tep' type='text' size='5' value='"+asTimeHM(tep.duration)+"' onchange='var de=getDHEditor(\""+this.id+"\"); if(de && de.modifyEvent(\""+tep.id+"\",\"duration\",fromTimeHM(event.currentTarget.value))) de.dh.renderData();'>";
                } else {
                    s+=asTimeHM(tep.duration);
                }
                s+="</td>";
          
                // end
                //s+="<td class='tep'>"+asTimeHM(tep.end)+"</td>";
          
                // room
                //s+="<td class='tep'>"+tep.room+"</td>";
                s+="<td class='tep"+(this.editor.isChangedEvent(tep.id,'room') ? "_modified" : "")+"'>";
                if(edit) {
                    var found=false;
                    s+="<select class='tep' onchange='var de=getDHEditor(\""+this.id+"\"); if(de && de.modifyEvent(\""+tep.id+"\",\"room\",event.currentTarget.value)) de.dh.renderData();'>";
                    for(var i in this.meta.rooms) {
                        var v=this.meta.rooms[i];
                        if(tep.room && tep.room==i) found=true;
                        s+="<option"+((tep.room==i) ? " selected" : "")+" value=\""+i+"\">";
                        s+=i;
                        s+="</option>";
                    }
                    if(!found) {
                        s+="<option selected value=\""+tep.room+"\">";
                        s+=tep.room;
                        s+="</option>";
                    }
                    s+="</select>";
                } else {
                    s+=tep.room;
                }
          
                // group
                s+="<td class='tep"+(this.editor.isChangedEvent(tep.id,'name') ? "_modified" : "")+"'>";
                {
                    var found=false;
                    for(var i in this.meta.groups) {
                        var v=this.meta.groups[i];
                        if(tep.name==i && v && v.length>1 && v[1]) {
                            s+="<img src='images/"+v[1]+"' width='"+wdIconSize+"'>&nbsp;";
                            found=true;
                            break;
                        }
                    }
                    if(!found) {
                        s+="<img src='images/icons8-circle-90.png' width='"+wdIconSize+"'>&nbsp;";
                    }
                }
                if(edit) {
                    var found=false;
                    s+="<select class='tep' onchange='var de=getDHEditor(\""+this.id+"\"); if(de && de.modifyEvent(\""+tep.id+"\",\"name\",event.currentTarget.value)) de.dh.renderData();'>";
                    for(var i in this.meta.groups) {
                        var v=this.meta.groups[i];
                        if(tep.name && tep.name==i) found=true;
                        s+="<option"+((tep.name==i) ? " selected" : "")+" value=\""+i+"\">";
                        s+=i;
                        s+="</option>";
                    }
                    if(!found) {
                        s+="<option selected value=\""+tep.name+"\">";
                        s+=tep.name;
                        s+="</option>";
                    }
                    s+="</select>";
                } else {
                    s+=tep.name;
                }
                s+="</td>";
          
                s+="</tr>";
            }
        }
    
        s+="</table></div>";
  
        var el=document.getElementById('data_view');
        if(el) el.innerHTML=s;
    },
    generateData: function() {
        var generatedEvents=[];
        for(var i in this.data) {
            var e=this.data[i];
            if(!e) continue;
            if(this.editor.isDeletedEvent(e.id)) continue;
            var wd0=(this.editor.isChangedEvent(e.id,'weekDays')) ? this.editor.modifiedEvents[e.id].changes.weekDays : null;
            var weekDays=(e.weekDays) ? jsonClone(e.weekDays) : null;
            var wds=(e.weekDays) ? jsonClone(e.weekDays) : null;
            if(wds && wds.length>0) {
                for(var i in wds) {
                    var wd=e.weekDays[i];
                    var ds=this.meta.weekDays[wd-1];
                    ge={
                        id:e.id,
                        start:e.start+ds,
                        end:e.end+ds,
                        duration:e.duration,
                        name:e.name, 
                        shortName:e.shortName, 
                        confSize: e.confSize, 
                        maxSize:e.maxSize, 
                        icon:e.icon, 
                        room: e.room, 
                        hidden: e.hidden,
                        weekDays: weekDays,
                        weekDay: wd
                    };
                    if(ge) {
                        if(e.hidden){
                            var a=0;
                        }
                        generatedEvents[generatedEvents.length]=ge;
                    }
                }
            }
            // add removed week days if any
            if(wd0 && wd0.length>0) {
                if(wd0 && wd0.length>0) for(var i in wd0) {
                    var wd=wd0[i];
                    if(wds && wds.length>0) {
                        for(var j in wds) {
                            if(wd==wds[j]) {
                                wd=-1;
                                break;
                            }
                        }
                    }
                    if(wd==-1) continue;
                    var ds=this.meta.weekDays[wd-1];
                    ge={
                        id:e.id,
                        start:e.start+ds,
                        end:e.end+ds,
                        duration:e.duration,
                        name:e.name, 
                        shortName:e.shortName, 
                        confSize: e.confSize, 
                        maxSize:e.maxSize, 
                        icon:e.icon, 
                        room: e.room, 
                        weekDays: weekDays,
                        weekDay: wd,
                        hidden: e.hidden,
                        removed: true
                    };
                    if(ge){
                        if(e.hidden){
                            var a=0;
                        }
                        generatedEvents[generatedEvents.length]=ge;
                    }
                }
            }
        }
        this.generatedData=generatedEvents;
        this.renderGeneratedData();
    },
    renderGeneratedData: function() {
        var current=this;
        var calendar = new Calendar({
            locale:userLocale,
            toPrev: (current.filter.prev) ? "var dh=getDataHandler(\""+current.id+"\"); if(dh) { dh.toPrevPeriod(); dh.loadData(); }" : null,
            toNext: (current.filter.next) ? "var dh=getDataHandler(\""+current.id+"\"); if(dh) { dh.toNextPeriod(); dh.loadData(); }" : null,
            showDates: false,
            renderCellEntryActions: function(id,entry){
                var s="";
                return s;
            },
            renderCellEntry: function(id, entry, dMin, dMax) {
                var edit=current.filter.edit==true || userHasRole('admin');
                var iconSize="24px";
                var actionIconSize="15px";
                var s="";
                var cst=(entry.removed) ? "calendar_cell_deleted" : (entry.hidden)? "calendar_cell_hidden" : "";
                s+="<fieldset  id='"+id+"' class='calendar_cell "+cst+"' tMin='"+dMin.getTime()+"' tMax='"+dMax.getTime()+"'>";
                s+="<legend class='calendar_cell'>";
                s+="<font size=-2>";
                s+=""+this.toTimeHM(entry.start);//+" - "+this.toTimeHM(entry.end);
                s+="</font>";
                s+="</legend>";

                // build overlayed images with optional action...
                var action=null;
                var actionIcon=null;
                if(edit && !entry.hidden) {
                    if(entry.removed) {
                    var rr=entry.weekDays;
                    if(!rr) rr=[];
                    rr.push(entry.weekDay);
                    action = "<a onclick='if(modifyEvent(\""+entry.id+"\",\"weekDays\","+toJSON(rr)+")) renderEvents();' class='cell_action'>";
                    actionIcon="images/icons8-restore-90.png";
                } else {
                    var rr0=entry.weekDays;
                    var rr=[];
                    for(var j in rr0) {
                        if(rr0[j]!=entry.weekDay) rr.push(rr0[j]);
                    }
                    action = "<a onclick='if(modifyEvent(\""+entry.id+"\",\"weekDays\","+toJSON(rr)+")) renderEvents();' class='cell_action'>";
                    actionIcon="images/icons8-trashcan-90.png";
                }
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
    
            s+="<br/>"+((entry.shortName) ? entry.shortName: entry.name);
            if(entry.maxSize)s+="&nbsp;<font size=-1>"+entry.maxSize+"</font>";

            if(action && actionIcon) {
                s+="<img class='overlay_wda' src='"+actionIcon+"' alt='add' width='"+actionIconSize+"'>";
                s+="</a>";
            }
    
            s+=this.renderCellEntryActions(id,entry);
            s+="</fieldset>";
            return s;
        },
        adjustableElements: function() {
            return document.getElementsByTagName("fieldset");
        }
    });
        
    calendar.render('data_view2',this.generatedData);
    }
})
.configure(null, function(dh){
    dh.filterRenderer.configure({
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
})
.addDataFilter(TimeRangeDataFilter(
    'season',
    '#{Season}', 
    'this.meta.yearSeasons', 
    {
        columns:2, 
        nullValue:'#{Now}', 
        nullData: 'this.meta.yearSeason', 
        valueOf: function(key) {
            if(!key) return null;
            var v=this.data[key];
            return (v[0] >> 16)+"/"+((seasonNames) ? seasonNames[(v[0] & 0xFF)] : (v[0] & 0xFF));
        }
    }),0)
/*
.addDataFilter(TimeRangeDataFilter(
    'quarter',
    '#{Quarter}', 
    'this.meta.yearQuarters', 
    {
        columns:2, 
        nullValue:'#{Now}', 
        nullData: 'this.meta.yearQuarter',
        valueOf: function(key) {
            if(!key) return null;
            var v=this.data[key];
            return (v[0] >> 16)+"/"+((quarterNames) ? quarterNames[(v[0] & 0xFF)] : (v[0] & 0xFF));
        }
    })) */
.removeDataFilter('week')
.removeDataFilter('month')
.removeDataFilter('my')
;

eh.editor.dh=eh;
eh.configure(conf);
return eh;

}







function getDHEditor(id) {
    var dh=getDataHandler(id);
    return (dh) ? dh.editor : null;
}
function createDataHandlerEditor(dh,conf) {
    var ed={
        dh: dh,
        modifiedEvents: {},
        deletedEvents: {},
        addedEvents: {},
        configure: function(conf,init) {
            if(conf) for(var c in conf) {
                this[c]=conf[c];
            }
            if("function"==typeof(init)) init(this);
            return this;
        },
        clear: function() {
            for(var i in this.modifiedEvents) {
                delete this.modifiedEvents[i];
            }
            for(var i in this.deletedEvents) {
                delete this.deletedEvents[i];
            }
            for(var i in this.addedEvents) {
                delete this.addedEvents[i];
            }
        },
        modifyEvent: function(id,prop,val) {
            //console.log('modifyEvent(strt): '+id+":"+prop+":"+val);     
            if(!this.modifiedEvents[id]) {
                for(var i in this.dh.data) {
                    var e=this.dh.data[i];
                    if(e.id==id) {
                        this.modifiedEvents[id]= {item: e, changes:{}};
                        break;
                    }
                }
            }
    
            var em=this.modifiedEvents[id];
            if(em) {
                if(undefined==em.changes[prop]) {
                    em.changes[prop]=em.item[prop];
                }
                em.item[prop]=val;
                this.fixModifiedEvent(em.item);
                //console.log('modifyEvent(done): '+id+":"+prop+":"+val+"\n  "+em.item[prop]+" ("+em.changes[prop]+")");     
                return true;
            }
            return false;
        },
  
        fixModifiedEvent: function(item) {
            if(item && item.start && item.duration) {
                item.end=item.start+item.duration;
            }
        },
  
        isChangedEvent: function(id,prop) {
            if(!id) return Object.keys(this.modifiedEvents).length>0;
            var r=false;
            var em=this.modifiedEvents[id];
            if(em) {
                if(prop) {
                    r = undefined!=em.changes[prop];
                    if(r) {
                        r=!jsonEqual(em.item[prop],em.changes[prop]);
                    }
                } else {
                    // just if was modified
                    r = true;
                }
            }else{
                r = false;
            }
            return r;
        },

        isDeletedEvent: function(id) {
            if(!id) return Object.keys(this.deletedEvents).length>0;
            var r=false;
            var em=this.deletedEvents[id];
            if(em){
                r = true;
            }else{
                r = false;
            }
            return r;
        },

        deleteEvent: function(id) {
            if(!id) return false;
            var r=false;
            if(this.isAddedEvent(id)) {
                delete this.addedEvents[id];
                for(var i in this.dh.data) {
                    var e=this.dh.data[i];
                    if(e.id==id) {
                        delete this.dh.data[i];
                        r=true;
                        break;
                    }
                }
            } else if(!this.isDeletedEvent(id)) {
                this.deletedEvents[id]=id;
                r=true;
            }
            return r;
        },

        undeleteEvent: function(id) {
            if(!this.isDeletedEvent(id)) return false;

            if(!id) {
                var done=false;
                for(var k in this.deletedEvents) {
                    if(this.undeleteEvent(k)) done=true;
                }
                return done;
            }

            var r=false;
            if(this.isDeletedEvent(id)) {
                delete this.deletedEvents[id];
                r=true;
            }
            return r;
        },

        addEvent: function(id) {
            if(!this.addedEvents[id]) {
                var e={
                    id:id,
                    from: this.dh.filter.from,
                    to: this.dh.filter.to,
                    start: fromTimeHM("8:00"),
                    duration: fromTimeHM("1:00"),
                    end: fromTimeHM("9:00"),
                    room: (this.dh.filter.room) ? this.dh.filter.room : Object.keys(this.dh.meta.rooms)[0],
                    name: (this.dh.filter.group) ? this.dh.filter.group : Object.keys(this.dh.meta.groups)[0]
                };
                this.addedEvents[id]=e;
                this.dh.data.unshift(e);
                return true;
            }
            return false;
        },
  
        isAddedEvent: function(id) {
            if(!id) return Object.keys(this.addedEvents).length>0;
            var r=false;
            var em=this.addedEvents[id];
            if(em){
                r = true;
            } else {
                r = false;
            }
            return r;
        },
  
        undoEventChanges: function(id,prop) {
            if(!this.isChangedEvent(id,prop)) {
                return false;
            }
  
            //console.log('undoEventChanges(strt): '+id+":"+prop);     
    
            if(!id) {
                var done=false;
                for(var k in this.modifiedEvents) {
                    if(this.undoEventChanges(k,prop)) done=true;
                }
                return done;
            }
    
            var r=false;
            var em=this.modifiedEvents[id];
            if(em){
                //console.log("\n  "+em.item[prop]+" ("+em.changes[prop]+")");     
                if(prop) {
                    r = undefined!=em.changes[prop];
                    if(r) {
                        if(!jsonEqual(em.item[prop],em.changes[prop])) {
                            em.item[prop]=em.changes[prop];
                            this.fixModifiedEvent(em.item);
                            delete em.changes[prop];
                            //console.log('undoEventChanges(strt): '+id+":"+prop+":  "+em.item[prop]+" ("+em.changes[prop]+")");     
                        }
                    }
                } else {
                    // just if was modified
                    for(var i in em.changes) {
                        em.item[i]=em.changes[i];
                    }
                    this.fixModifiedEvent(em.item);
                    delete this.modifiedEvents[id];
                    r = true;
                }
            } else {
                r = false;
            }
            return r;
        },

        applyEventChanges: function(id) {
            var added;
            var deleted;
            var modified;
     
            if(id) {
                if(this.isAddedEvent(id)) {
                    added=[];
                    added.push(this.addedEvents[id]);
                } else if(this.isDeletedEvent(id)) {
                    deleted=[];
                    deleted.push(id);
                } else if(this.isChangedEvent(id,null)) {
                    modified={};
                    for(var i in this.modifiedEvents) {
                        var te=this.modifiedEvents[i].item;
                        if(id==te.id) {
                            modified[i]=te;
                            break;
                        }
                    }
                }
            } else {
                if(this.isAddedEvent(null)) {
                    added=[];
                    for(var i in this.addedEvents) {
                        added.push(this.addedEvents[i]);
                    }
                } else if(this.isDeletedEvent(id)) {
                    deleted=[];
                    for(var i in this.deletedEvents) {
                        deleted.push(this.deletedEvents[i]);
                    }
                } else if(this.isChangedEvent(id,null)) {
                    modified={};
                    for(var i in this.modifiedEvents) {
                        var te=this.modifiedEvents[i].item;
                        modified[i]=te;
                    }
                }
            }
     
            if(added || deleted || modified) {
                var current=this;
                REST_api.modifyEventPlanners(
                    added,
                    deleted,
                    modified,
                    function(url,data) {
                        // proceed data results:
                        //   clear succesfull added replacing with actuals
                        //   remove succesfull deletes
                        //   refresh modified
                        if(data.result) {
                            var rr=data.result;
                            for(var id in rr) {
                                if(current.isAddedEvent(id)) {
                                    if("object"==typeof(rr[id])){
                                        if(current.isChangedEvent(id)) {
                                            delete current.modifiedEvents[id];
                                        }
                                        for(var i in current.dh.data) {
                                            var e=current.dh.data[i];
                                            if(id==e.id) {
                                                delete current.addedEvents[id];
                                                current.dh.data[i]=rr[id];
                                                break;
                                            }
                                        }
                                    } else if("failed"==rr[id]) {
                                        // error message
                                        var a=0;
                                    } 
                                } else if(current.isDeletedEvent(id)) {
                                    delete current.deletedEvents[id];
                                    for(var i in current.dh.data) {
                                        var e=current.dh.data[i];
                                        if(id==e.id) {
                                            delete current.dh.data[i];
                                            break;
                                        }
                                    }
                                } else if(current.isChangedEvent(id)) {
                                    if("object"==typeof(rr[id])) {
                                        delete current.modifiedEvents[id];
                                        for(var i in current.dh.data) {
                                            var e=current.dh.data[i];
                                            if(id==e.id) {
                                                current.dh.data[i]=rr[id];
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        current.dh.renderData();
                    }
                );
            }
            return false;
        }
    }

    ed.configure(conf);
    return ed;
}


  function toJSON(val) {
     return JSON.stringify(val);
  }
  function jsonEqual(a1,a2) {
    return JSON.stringify(a1)==JSON.stringify(a2);
  }  

  function jsonClone(val) {
    return (val) ? JSON.parse(JSON.stringify(val)) : null;
  }  
  
  function toDateYMD(val) {
     if(!val) return "<no date>";
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return ""
           +val.getFullYear()
           +"-"
           +((val.getMonth()<9) ? "0" : "")+(val.getMonth()+1)
           +"-"
           +((val.getDate()<10) ? "0"+val.getDate() : val.getDate());
  }
  
  function asTimeHM(val) {
     if(val) {
        var h=Math.floor(val / (1000*60*60));
        var m= Math.floor((val % (1000*60*60))/(1000*60));
        return ((h<10) ? "0"+h :h)+":"+((m<10) ? "0"+m:m);
     } else return "";
  }
  function fromTimeHM(val) {
     if(val) {
        var h=val.substring(0,val.indexOf(':'));
        var m= val.substring(val.indexOf(':')+1);
        return h*1000*60*60 + m*1000*60;
     } else return 0;
  }

// -->