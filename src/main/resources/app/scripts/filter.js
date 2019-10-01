/*
 * conf(id,title,columns,isActive(v),setFilter(v),keys(),valueOf(k))
 */
function buildFilterGroup(conf) {
   var s="";
   var cl="filter";
   var a=false;
   var si="";

   // build self/no self selector
   si="<div class='filter_sel' id='min_filter_"+conf.id+"'>";
   s+="<fieldset class='filter'><legend class='filter' onclick='toggleVisibility(\"filter_"+conf.id+"\")'>"+conf.title+"</legend>";
   s+="<table class='filter' id='filter_"+conf.id+"'>";
       
       if(conf.valueOf(null)) {
         a=conf.isActive(null);
         s+="<tr class='filter'><td class='"+cl+(a ? "_active": "")+"'";
         s+=" onclick='"+conf.setFilter(null)+" loadEvents(); renderFilter();'";
         if(conf.columns) s+=" colspan='"+conf.columns+"'";
         s+=">";
         s+=conf.valueOf(null);
         s+="</td></tr>";
         if(a) si+=conf.valueOf(null)+" ";
       }

       if(conf.columns)
           s+="<tr class='filter'>";
       
       var ac=0;
       for(var k in conf.keys()) {
         a=conf.isActive(k);
         
         if(conf.columns) {
           var n=new Number(k);
           if(n>0 && (n % conf.columns) == 0){
             s+="</tr><tr class='filter'>";
           }
         } else {
           s+="<tr class='filter'>";
         }
         
         s+="<td class='"+cl+((a) ? "_active": "")+"'";
         s+=" onclick='"+conf.setFilter(k)+"; loadEvents(); renderFilter();'>";
         s+=conf.valueOf(k);
         if(conf.columns)
           s+="</td>";
         else
           s+="</td></tr>";
         if(a) {
           if(ac>0) si+=' ';
           si+=conf.valueOf(k);
           ac++;
         }
       }
       if(conf.columns) s+="</tr>";
       
       s+="</table>";
       s+=si+"</div>";
       s+="</fieldset>\n";
       return s;
}


/* Toggle filter-like component visibility: if visible - hide "min_", otherwise hide element and show "min_" one.
*/
function toggleVisibility(elementName) {
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
}

// build self/no self selector
function buildMyFilter(title,all,my,notMy) {
     var s="";
     if(isValidUser()) {
        var udata=new Object();
        udata[userId]=my;
        udata["!"+userId]=notMy;
        s+=buildFilterGroup({
           title:title,
           id:"my",
           columns: 2,
           data: udata,
           isActive: function(k) {
              return k==eventsFilter.participant;
           },
           setFilter: function(k) {
              if(k)
              return "eventsFilter.participant=\""+k+"\";";
              else
              return "eventsFilter.participant=null;";
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              if(!k) return all;
              return this.data[k];
           }}
        );
     }
  return s;
}
     
     
// build month selector     
function buildMonthsFilter(title,locale) {
        return buildFilterGroup({
           title:title,
           id:"month",
           columns: 3,
           data: eventsMeta.ranges.months,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                var b = v[1]==eventsFilter.from && v[2]==eventsFilter.to;
                if(b) {
                   try{
                     eventsFilter.prev=this.data[new Number(k)-1];
                   }catch(e){ eventsFilter.prev=null;}
                   try{
                     eventsFilter.next=this.data[new Number(k)+1];
                   }catch(e){ eventsFilter.next=null;}
                }
                return b;
              } else {
                return false;
              }
           },
           setFilter: function(k) {
              if(k) {
                var v=this.data[k];
                return "if(eventsFilter.mode!=\"list\") eventsFilter.mode=\"month\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              } else {
                return "if(eventsFilter.mode==\"month\") eventsFilter.from=null; eventsFilter.to=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              if(!k) return null;
              var v=this.data[k];
              return new Date(v[1]).toLocaleDateString(locale, { month: 'short' });
           }}
        );
}

     
// build week selector     
function buildWeeksFilter(title,_now_) {
        return buildFilterGroup({
           title:title,
           id:"week",
           columns: 4,
           data: eventsMeta.ranges.weeks,
           dataNow: eventsMeta.ranges.default,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                var b = v[1]==eventsFilter.from && v[2]==eventsFilter.to;
                if(b) {
                   try{
                     eventsFilter.prev=this.data[new Number(k)-1];
                   }catch(e){ eventsFilter.prev=null;}
                   try{
                     eventsFilter.next=this.data[new Number(k)+1];
                   }catch(e){ eventsFilter.next=null;}
                }
                return b;
              } else {
                var v=this.dataNow;
                return v[1]==eventsFilter.from && v[2]==eventsFilter.to;
              }
           },
           setFilter: function(k) {
              if(k) {
                var v=this.data[k];
                return "eventsFilter.mode=\"week\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              } else {
                var v=this.dataNow;
                return "eventsFilter.mode=\"week\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              if(!k) return _now_;
              var v=this.data[k];
              return v[0];
           }}
        );
}

// build season selector     
function buildSeasonsFilter(title, seasonNames) {
        return buildFilterGroup({
           title:title,
           id:"season",
           columns: 1,
           data: eventsMeta.yearSeasons,
           dataNow: eventsMeta.yearSeason,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                var b = v[1]==eventsFilter.from && v[2]==eventsFilter.to;
                if(b) {
                   try{
                     eventsFilter.prev=this.data[new Number(k)-1];
                   }catch(e){ eventsFilter.prev=null;}
                   try{
                     eventsFilter.next=this.data[new Number(k)+1];
                   }catch(e){ eventsFilter.next=null;}
                }
                return b;
              } else {
                var v=this.dataNow;
                return v && v[1]==eventsFilter.from && v[2]==eventsFilter.to;
              }
           },
           setFilter: function(k) {
              if(k) {
                var v=this.data[k];
                return "eventsFilter.mode=\"week\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              } else {
                var v=this.dataNow;
                return "eventsFilter.mode=\"week\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              if(!k) return null;
              var v=this.data[k];
              return (v[0] >> 16)+"/"+((seasonNames) ? seasonNames[(v[0] & 0xFF)] : (v[0] & 0xFF));
           }}
        );
}

// build quarter selector     
function buildQuartersFilter(title, quarterNames) {
        return buildFilterGroup({
           title:title,
           id:"quarter",
           columns: 1,
           data: eventsMeta.yearQuarters,
           dataNow: eventsMeta.yearQuarter,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                var b = v[1]==eventsFilter.from && v[2]==eventsFilter.to;
                if(b) {
                   try{
                     eventsFilter.prev=this.data[new Number(k)-1];
                   }catch(e){ eventsFilter.prev=null;}
                   try{
                     eventsFilter.next=this.data[new Number(k)+1];
                   }catch(e){ eventsFilter.next=null;}
                }
                return b;
              } else {
                var v=this.dataNow;
                return v[1]==eventsFilter.from && v[2]==eventsFilter.to;
              }
           },
           setFilter: function(k) {
              if(k) {
                var v=this.data[k];
                return "eventsFilter.mode=\"week\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              } else {
                var v=this.dataNow;
                return "eventsFilter.mode=\"week\"; eventsFilter.from="+v[1]+"; eventsFilter.to="+v[2]+";";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              if(!k) return null;
              var v=this.data[k];
              return (v[0] >> 16)+"/"+((quarterNames) ? quarterNames[(v[0] & 0xFF)] : (v[0] & 0xFF));
           }}
        );
}

// build dayOfWeek selector     
function buildDayOfWeekFilter(title,all,daysOfWeek) {
        return buildFilterGroup({
           title:title,
           id:"dayOfWeek",
           data: daysOfWeek,
           columns: 4,
           isActive: function(k) {
              var fv=eventsFilter.dayOfWeek;
              if(k) {
                if(fv)
                for(var vi in fv) {
                  if(k==fv[vi]) {
                    return true;
                  }
                }
                return false;
              } else {
                return null==eventsFilter.dayOfWeek;
              }
           },
           setFilter: function(k) {
              if(k) {
                var s="";
                var fv=eventsFilter.dayOfWeek;
                if(fv) {
                  var found=false;
                  for(var vi in fv) if(k==fv[vi]) {
                     found=true;
                  }else{
                     if(s) s+=",";
                     s+=fv[vi];
                  }
                  if(!found) {
                     if(s) s+=",";
                     s+=k;
                  }
                }else{
                   s+=k;
                }
                if(s) {
                  s='['+s+']';
                } else s="null";
                return "eventsFilter.dayOfWeek="+s+";";
              } else {
                return "eventsFilter.dayOfWeek=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              var v=this.data[k];
              return (k) ? this.data[k] : all;
           }}
        );
}


     
// build room selector     
function buildRoomsFilter(title,all) {
        return buildFilterGroup({
           title:title,
           id:"room",
           data: eventsMeta.rooms,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                return k==eventsFilter.room;
              } else {
                return null==eventsFilter.room;
              }
           },
           setFilter: function(k) {
              if(k) {
                return "eventsFilter.room=\""+k+"\";";
              } else {
                return "eventsFilter.room=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              var v=this.data[k];
              return (k) ? k : all;
           }}
        );
}

// build trainer selector
function buildTrainersFilter(title,all) {
        return buildFilterGroup({
           title:title,
           id:"trainer",
           data: eventsMeta.trainers,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                return k==eventsFilter.trainer;
              } else {
                return null==eventsFilter.trainer;
              }
           },
           setFilter: function(k) {
              if(k) {
                return "eventsFilter.trainer=\""+k+"\";";
              } else {
                return "eventsFilter.trainer=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              var v=this.data[k];
              return (k) ? v : all;
           }}
        );
}
     
// build course selector
function buildCategoriesFilter(title,all) {
        return buildFilterGroup({
           title:title,
           id:"category",
           data: eventsMeta.categories,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                return k==eventsFilter.category;
              } else {
                return null==eventsFilter.category;
              }
           },
           setFilter: function(k) {
              if(k) {
                return "eventsFilter.category=\""+k+"\";";
              } else {
                return "eventsFilter.category=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              var icon="";
              var v=this.data[k];
              if(k && v[1]) {
                icon="<img width=\"16\" src=\"images/"+v[1]+"\">&nbsp;";
                v=v[0];
              } else if(k) v=v[0];
              //return (k) ? ""+icon+k+"&nbsp;<sup>"+v+"</sup>" : all;
              return (k) ? ""+icon+k : all;
           }}
        );
}

// build course selector
function buildCoursesFilter(title,all) {
        return buildFilterGroup({
           title:title,
           id:"course",
           data: eventsMeta.courses,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                return k==eventsFilter.course;
              } else {
                return null==eventsFilter.course;
              }
           },
           setFilter: function(k) {
              if(k) {
                return "eventsFilter.course=\""+k+"\";";
              } else {
                return "eventsFilter.course=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              var icon="";
              var v=this.data[k];
              if(k && v[1]) {
                icon="<img width=\"16\" src=\"images/"+v[1]+"\">&nbsp;";
                v=v[0];
              } else if(k) v=v[0];
              //return (k) ? ""+icon+k+"&nbsp;<sup>"+v+"</sup>" : all;
              return (k) ? ""+icon+k : all;
           }}
        );
}

// build group selector
function buildGroupsFilter(title,all) {
        return buildFilterGroup({
           title:title,
           id:"group",
           data: eventsMeta.groups,
           isActive: function(k) {
              if(k) {
                var v=this.data[k];
                return k==eventsFilter.group;
              } else {
                return null==eventsFilter.group;
              }
           },
           setFilter: function(k) {
              if(k) {
                return "eventsFilter.group=\""+k+"\";";
              } else {
                return "eventsFilter.group=null;";
              }
           },
           keys: function() {
              return this.data;
           },
           valueOf: function(k) {
              var icon="";
              var v=this.data[k];
              if(k && v[1]) {
                icon="<img width=\"16\" src=\"images/"+v[1]+"\">&nbsp;";
                v=v[0];
              } else if(k) v=v[0];
              return (k) ? ""+icon+k+"&nbsp;<sup>"+v+"</sup>" : all;
           }}
        );
}
