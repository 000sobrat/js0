
function Calendar(conf) {
var calendar = {
  locale: "fi", // date/time locale setting
  compact: false, // if compact=true, empty days are skipped from table(s)
  toPrev: null,
  toNext: null,

  find: function (events, from, to) {
     var a=new Array();
     if(events) {
        for(i in events) {
          var e=events[i];
          var es=new Date(e.start);
          if(from && es>=from) {
            if(to && es<to) {
              a[a.length] = e;
            }
          }
        }
     }
     return a;
  },

  render: function (element, events) {
      var el=document.getElementById(element);
      el.ess=new Object();
      el.conf=this;
      var minD=null;
      var minT=null;
      var maxD=null;
      var maxT=null;

           for(var i in events) {
              var event=events[i];

              var sd=this.toDateYMD(event.start);//  event.start.substring(0,event.start.indexOf('T'));
              var ed=(event.end) ? this.toDateYMD(event.end) : null; // (event.end) ? event.end.substring(0,event.end.indexOf('T')) : null;

              var st=this.toTimeHM(event.start); // event.start.substring(event.start.indexOf('T')+1);
              var et=(event.end) ? this.toTimeHM(event.end) : null; //(event.end) ? event.end.substring(event.end.indexOf('T')+1) : null;

              if(!minD || sd<minD) minD=sd;
              if(event.end)
              if(!maxD || ed>maxD) maxD=ed;

              if(!minT || st<minT) minT=st;
              if(event.end)
              if(!maxT || et>maxT) maxT=et;
           }

      var minDT=new Date(minD+'T'+minT);
      var maxDT=new Date(maxD+'T'+maxT);
      var tMin=minDT;
      var tMax=new Date(minD+'T'+maxT);
      minD=new Date(minD);
      maxD=new Date(maxD);

      var days=new Object();      
      var s="<table border=1 class='calendar'>";
      // date headers
      {
         s+="<tr class='calendar_date_row'>";
         s+="<th align='center' valign='middle'>";
         if(this.toPrev) s+="<img src='images/icons8-шеврон-влево-90.png' width='20px' onclick='"+this.toPrev+"'>";
         s+="-";
         if(this.toNext) s+="<img src='images/icons8-шеврон-вправо-90.png' width='20px' onclick='"+this.toNext+"'>";
         s+="</th>";
         var d=new Date(minD);
         while(d<=maxD) {
            var dMin=new Date(d);
            var dMax=new Date(d);
            dMax=new Date(dMax.setDate(dMax.getDate()+1)-1);
            var es=this.find(events,dMin,dMax);
            if(es && es.length>0) {
               days[this.toDateKey(dMin)]=true;
            }
            
            if(es && es.length>0 || !this.compact) {
              s+="<th class='calendar_date'";
              var dS=this.toDateYMD(d);
              s+=" id='th_"+dS+"'";
              s+=">";
              s+=dS;
              s+="<br/>"+this.toWeekDay(d);
              s+="</th>";
            }
            d.setDate(d.getDate()+1);
         }
         s+="</tr>";
      }


      var t=new Date(tMin);
      if(t && t.getMinutes()!=0) {
         // add dummy 0-base hour row!
        var h=t.getHours();
        var m=0;

        var rcl="calendar_time "+((m==0) ? "calendar_time_00" : "calendar_time_30" );

        s+="<tr class='"+rcl+"'>";

        s+="<th class='"+rcl+"'>";
        if(m==0) {
          s+=(h<10) ? "0"+h: h;
        }
        s+=":";
        s+=(m<10) ? "0"+m: m;
        s+="</th>";

        var d=new Date(minD);
        d.setHours(t.getHours());
        d.setMinutes(t.getMinutes());
        while(d<=maxDT) {
           if(this.compact) {
              if(!days[this.toDateKey(d)]) {
                 d.setDate(d.getDate()+1);
                 continue;
              }
           }
           s+="<td class='calendar_cell'></td>";
           d.setDate(d.getDate()+1);
        }

        s+="</tr>";
      }
      while(t<tMax) {
        var h=t.getHours();
        var m=t.getMinutes();

        var rcl="calendar_time "+((m==0) ? "calendar_time_00" : "calendar_time_30" );

        s+="<tr class='"+rcl+"'>";

        s+="<th class='"+rcl+"'>";
        if(m==0) {
          s+=(h<10) ? "0"+h: h;
        }
        s+=":";
        s+=(m<10) ? "0"+m: m;
        s+="</th>";

        var d=new Date(minD);
        d.setHours(t.getHours());
        d.setMinutes(t.getMinutes());
        while(d<=maxDT) {
           if(this.compact) {
              if(!days[this.toDateKey(d)]) {
                 d.setDate(d.getDate()+1);
                 continue;
              }
           }
           s+="<td class='calendar_cell'>";
           var dMin=new Date(d);
           var dMax=new Date(d);
           dMax.setMinutes(dMax.getMinutes()+30);
           d.setDate(d.getDate()+1);
           var es=this.find(events,dMin,dMax);
           if(es && es.length>0) {
              for(var i in es) {
                var es_id="ts_"+i+"_"+es.length+"_"+dMin.getTime()+"_"+es[i].start;
                el.ess[es_id]=es[i];
                s+=this.renderCellEntry(es_id, es[i], dMin, dMax);
              }
           }
           s+="</td>";
        }
        s+="</tr>";
        t.setMinutes(t.getMinutes()+30);
      }


      el.innerHTML=s;
      this.adjustLocations(element);
  },

  renderCellEntry: function(id, entry, dMin, dMax) {
    var s="";
    var trMax=entry.group.training.maxSize;
    var trAct=Object.keys(entry.trainees).length;
    var cst=(trAct<trMax)? (trAct>0)? (trAct>3) ?"calendar_cell_full" :"calendar_cell_participant":"":"calendar_cell_full";
    s+="<div  id='"+id+"' class='calendar_cell "+cst+"' tMin='"+dMin.getTime()+"' tMax='"+dMax.getTime()+"'>";
    s+="<font size=-2>";
    s+=""+this.toTimeHM(entry.start)+" - "+this.toTimeHM(entry.end);
    s+="<br/>";
    s+="</font>";
    s+=entry.title+"  <font size=-1><br/>"+entry.trainer+"</font>";
    s+=this.renderCellEntryActions(id,entry);
    s+="</div>";
    return s;
  },

  renderCellEntryActions(id,entry) {
     return "<font size='-2' style='position: absolute; text-align: right;'>&nbsp;&nbsp;apply</font>";
  },

  renderMonth: function(element,events) {
      var el=document.getElementById(element);
      el.ess=new Object();

      var minD=null;
      var maxD=null;

           for(var i in events) {
              var event=events[i];

              var sd=this.toDateYMD(event.start);//  event.start.substring(0,event.start.indexOf('T'));
              var ed=(event.end) ? this.toDateYMD(event.end) : null; // (event.end) ? event.end.substring(0,event.end.indexOf('T')) : null;

              if(!minD || sd<minD) minD=sd;
              if(event.end)
              if(!maxD || ed>maxD) maxD=ed;

           }

      minD=new Date(minD);
      maxD=new Date(maxD);

      var dns=new Array();

      var s="<table border=1 class='calendar'>";
      // date headers
      {
         s+="<tr class='calendar_weekday_row'>";
         s+="<th class='calendar_weekday'>";
         s+="<th align='center' valign='middle'>";
         if(this.toPrev) s+="<img src='images/icons8-shevrone-left-90.png' width='20px' onclick='"+this.toPrev+"'>";
         s+=this.toDateYM(minD);
         if(this.toNext) s+="<img src='images/icons8-shevrone-right-90.png' width='20px' onclick='"+this.toNext+"'>";
         s+="</th>";


         s+="</th>";
         var d=new Date('2019-09-02');
         var dEnd=new Date('2019-09-08');
         while(d<=dEnd) {
            s+="<th class='calendar_weekday'";
            var dS=this.toWeekDay(d);
            s+=" id='th_"+dS+"'";
            s+=">";
            s+=dS;
            dns[dns.length]=dS;
            s+="</th>";
            d.setDate(d.getDate()+1);
         }
         s+="</tr>";
      }

      s+="<tr class='calendar_weekday'><td/>";
      {
        var d=minD;
        var dnI=0;
        while(d<=maxD) {
           var wd=this.toWeekDay(d);
           while(dns[dnI]!=wd) {
              s+="<td class='calendar_weekday_out_of_range'></td>";
              dnI++;
           }
           var dMin=new Date(d);
           var dMax=new Date(dMin);
           dMax = new Date(dMax.setDate(dMin.getDate()+1)-1);

           var es=this.find(events,dMin,dMax);

           s+="<td class='calendar_weekday'>";


           if(es) {
            s+="<fieldset class='calendar_weekday_cell "+"'>";
            s+="<legend class='calendar_cell'>";
            s+=this.toDateD(d);
            s+="</legend>";
            for(var i in es) {
              var entry=es[i];
              if(1==0) {
              s+="<fieldset class='calendar_cell "+"' style='position: normal;'>";
              s+="<legend class='calendar_cell'>";
              s+="<font size=-2>";
              s+=""+this.toTimeHM(entry.start)+" - "+this.toTimeHM(entry.end);
              s+="</font>";
              s+="</legend>";
              //s+="<a onclick='if(addToBasket(\""+entry.id+"\")) loadBasket();'><img src='images/ball_blue_gradient.png' alt='add' width='12px'></a>";
              s+="&nbsp;"+entry.name;
              s+="</fieldset>";
              }
              s+="<div style='white-space: nowrap'>";
              s+="<a onclick='if(addToBasket(\""+entry.id+"\")) loadBasket();'><img src='images/ball_blue_gradient.png' alt='add' width='12px'></a>";
              s+="&nbsp;"+entry.name;
              s+="&nbsp;<font size=-2>";
              s+=""+this.toTimeHM(entry.start);//+" - "+this.toTimeHM(entry.end);
              s+="</font>";
              s+="</div>";
           }
           s+="</fieldset>";
          }
           s+="</td>";
           dnI++;
           if(dnI==dns.length) {
             dnI=0;
             s+="</tr><tr class='calendar_weekday'><td/>";
           }
           d.setDate(d.getDate()+1);
        }
      }
      s+="</tr></table>";

      el.innerHTML=s;
  },

  renderList: function(element,events) {
      var el=document.getElementById(element);
      el.ess=new Object();

      var minD=null;
      var maxD=null;

           for(var i in events) {
              var event=events[i];

              var sd=this.toDateYMD(event.start);//  event.start.substring(0,event.start.indexOf('T'));
              var ed=(event.end) ? this.toDateYMD(event.end) : null; // (event.end) ? event.end.substring(0,event.end.indexOf('T')) : null;

              if(!minD || sd<minD) minD=sd;
              if(event.end)
              if(!maxD || ed>maxD) maxD=ed;

           }

      minD=new Date(minD);
      maxD=new Date(maxD);


        var s="<pre>";
        var d=minD;
        var dnI=0;
        while(d<=maxD) {
           var dS=this.toDateYMD(d);

           var dMin=new Date(d);
           var dMax=new Date(dMin);
           dMax = new Date(dMax.setDate(dMin.getDate()+1)-1);

           var es=this.find(events,dMin,dMax);

           if(es && es.length>0) {
             s+="\n<div>"+dS+"</div>";
             for(var i in es) {
               var entry=es[i];
               s+="  ";
               s+=""+this.toTimeHM(entry.start)+" - "+this.toTimeHM(entry.end);
               s+=" "+entry.confSize+"/"+entry.size+"/"+entry.maxSize;
               s+="  "+entry.name;
               if(entry.participants) {
                  for(var pk in entry.participants) {
                     var p=entry.participants[pk];
                     s+="\n      "+pk+" "+p;
                  }
               }
               s+="\n";
             }
           }

           d.setDate(d.getDate()+1);
        }

    s+="</pre>";
    el.innerHTML=s;
  },

  toWeekDay: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return val.toLocaleDateString(this.locale, { weekday: 'short' }); 
  },

  toWeek: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return val.toLocaleDateString(this.locale, { week: 'short' }); 
  },

  toMonth: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return val.toLocaleDateString(this.locale, { month: 'short' }); 
  },

  toDateYMD: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return ""
           +val.getFullYear()
           +"-"
           +((val.getMonth()<9) ? "0" : "")+(val.getMonth()+1)
           +"-"
           +((val.getDate()<10) ? "0"+val.getDate() : val.getDate());
  },

  toDateKey: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return "key"
           +val.getFullYear()
           +((val.getMonth()<9) ? "0" : "")+(val.getMonth()+1)
           +((val.getDate()<10) ? "0"+val.getDate() : val.getDate());
  },

  toDateYM: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return ""
           +val.getFullYear()
           +"-"
           +((val.getMonth()<9) ? "0" : "")+(val.getMonth()+1);
  },

  toDateD: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return ""
           +((val.getDate()<10) ? "0"+val.getDate() : val.getDate());
  },

  toTimeHM: function (val) {
     if("number"==typeof(val)) val=new Date(val);
     else if("string"==typeof(val)) val=new Date(val);
     return ""
           +((val.getHours()<10) ? "0"+val.getHours() : val.getHours())
           +":"
           +((val.getMinutes()<10) ? "0"+val.getMinutes() : val.getMinutes());
  },

  toDurHM: function(val) {
     if(val) {
        var n=this.toNumeric(val);
        var t=n % 1000*60*60*24;
        var m = t % 1000*60;
        var h = t / 1000 / 60 / 60;
        return ((h>9)? h : "0"+h)+":"+((m>9) ? m : "0"+m);
     }
  },
  toNumeric: function (val) {
     if("number"==typeof(val)) return val;
     val=""+val;
     if(val.endsWith("px")) val=val.substring(0,val.length-2);
     return new Number(val);
  },

  adjustableElements: function() {
     return document.getElementsByTagName("div");
  },

  adjustLocations: function (element) {
     var el=document.getElementById(element);
     var ess=el.ess;

     var divs=this.adjustableElements();

     // relocate same-cell items in left-to-right order by adding margin-left...
     for(var di in divs) {
        var div=divs[di];
        var dId=div.id;
        if(dId && dId.startsWith("ts_0_")) {
            var ddd=dId.split("_");
            if(ddd.length==5) {
               var c=new Number(ddd[2]);
               if(c && c>1) {
                  var dds=new Array(c);
                  for(var dii in divs) {
                     var ddiv=divs[dii];
                     var ddId=ddiv.id;
                     if(ddId && ddId.startsWith(ddd[0]+"_")){
                       var dddd=ddId.split("_");
                       if(ddd[0]==dddd[0] && ddd[3]==dddd[3]) {
                         var ci=new Number(dddd[1]);
                         dds[ci]=ddiv;
                       }
                     }
                  }
                  for(var i=1;i<c;i++) {
                     var pw=window.getComputedStyle(dds[i-1], null).getPropertyValue('width');
                     var pm=window.getComputedStyle(dds[i-1], null).getPropertyValue('margin-left');
                     var m=window.getComputedStyle(dds[i], null).getPropertyValue('margin-left');
                     dds[i].style.marginLeft=(this.toNumeric(m)+this.toNumeric(pw)+this.toNumeric(pm))+"px";
                     console.log(dds[i].id+': '+dds[i].innerText+": -> "+dds[i].style.marginLeft);
                  }
               }
            }
        }
     }
     // adjust item cell heights and related column widths...
     for(var di in divs) {
        var div=divs[di];
        var dId=div.id;
        if(dId && dId.startsWith("ts_")) {
            // adjust cell height
            var esId=dId;
            var es=ess[esId];
            var h=div.parentElement.clientHeight;
            var ht=new Number(div.attributes.tMax.value)-new Number(div.attributes.tMin.value);
            var dur=es.duration;
            var ddh=div.clientTop;
            ddh+=this.toNumeric(window.getComputedStyle(div, null).getPropertyValue('padding-top'));
            ddh+=this.toNumeric(window.getComputedStyle(div, null).getPropertyValue('padding-bottom'));
            var dh= (dur/ht*h)-ddh;
            div.style.height=dh+'px';
            // adjust cell top
            var top=new Number(div.attributes.tMin.value);
            if(es.start>top) {
               div.style.marginTop=((es.start-top)/ht*h)+'px';
            }

            // adjust column width
            var th_id="th_"+this.toDateYMD(es.start);
            var dTH=document.getElementById(th_id);
            if(dTH) {
               var dH = this.toNumeric(window.getComputedStyle(dTH, null).getPropertyValue('width'));
               var dW = this.toNumeric(window.getComputedStyle(div, null).getPropertyValue('width'));
               var dWML = this.toNumeric(window.getComputedStyle(div, null).getPropertyValue('margin-left'));
               if(dH<(dW+dWML)) {
                  var a=0;
                  dTH.style.width=(dW+dWML)+"px";
               }
            }
        }
     }
  }
};
  if(conf) for(var c in conf) {
    calendar[c]=conf[c];
  }
  return calendar;
}
