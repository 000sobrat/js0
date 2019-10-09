// <!--
/*
  Set of methods to login/logout/change user prperties/password.
  Requires REST_api and elements: "loginForm" and "userIdno".
*/

  function showLoginLinks(loginLinks) {
       var s="";
       s+="<table border='0' style='border-collapse: collapse;'><tr>";
       s+="<td>";
       s+="<a onclick='showLoginForm();' class='login' ttar='#{Login}...'>";
       s+="<img src='images/icons8-login-90.png' alt='#{Login}...' width='20'>"
       s+="<tt>#{Login}</tt>";
       s+="</a>";
       s+="</td>";
       if(loginLinks) {
          for(var l in loginLinks) {
             var ll=loginLinks[l];
             if(l && ll && ll[0]) {
                s+="<td nowrap>";
                s+="<a href='"+l+"' class='login' ttar='"+ll[1]+"'>";
                s+="<img src='images/"+ll[0]+"' width='20px' alt='"+ll[1]+"'>";
                s+="<tt>"+ll[1]+"</tt>";
                s+="</a>";
                s+="</td>";
             }
          }
       }
       s+="</tr></table>";

       var el=document.getElementById('userInfo');
       el.innerHTML=s;
  }
            
  function showLoginForm() {
     var s="<table border='0' width='100%' class='loginForm'>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{User_name_or_email}</td><td  class='loginForm'align='left'><input type='text' id='login_name'></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{Password}</td><td  class='loginForm'align='left'><input type='password' id='login_key'></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='center' colspan='2'>";
     s+="<input type='button' value='#{Login}' onclick='doLogin(); '></input>";
     s+="&nbsp;<input type='button' value='#{Cancel}' onclick='var el=document.getElementById(\"loginForm\"); if(el) {el.innerHTML=null; el.style.display=\"none\";}'></input>";
     s+="</td></tr>";
     s+="</table>";
    var el=document.getElementById("loginForm");
    el.innerHTML=s;
    el.style.zIndex=2;
    el.style.display='block';

    var elW=window.getComputedStyle(el, null).getPropertyValue('width');
    var ww=window.innerWidth;
    if("string"==typeof(elW))
    elW=new Number(elW.substring(0,elW.length-2));
    if("string"==typeof(ww))
    ww=new Number(ww.substring(0,ww.length-2));
    el.style.left=(ww/2-elW/2)+'px';
  }

  function showUserForm() {
     var s="<table border='0' width='100%' class='loginForm'>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{User_email}</td><td  class='loginForm'align='left'><input type='text' id='login_email' value='"+userId+"'></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{User_name}</td><td  class='loginForm'align='left'><input type='text' id='login_name' value='"+((userName && "null"!=userName)?userName:"")+"'></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='center' colspan='2'>";
     s+="<input type='button' value='#{SaveUser}' onclick='doSaveUser(); '></input>";
     s+="&nbsp;<input type='button' value='#{Cancel}' onclick='var el=document.getElementById(\"loginForm\"); if(el) {el.innerHTML=null; el.style.display=\"none\";}'></input>";
     s+="</td></tr>";
     s+="</table>";
    var el=document.getElementById("loginForm");
    el.innerHTML=s;
    el.style.zIndex=2;
    el.style.display='block';

    var elW=window.getComputedStyle(el, null).getPropertyValue('width');
    var ww=window.innerWidth;
    if("string"==typeof(elW))
    elW=new Number(elW.substring(0,elW.length-2));
    if("string"==typeof(ww))
    ww=new Number(ww.substring(0,ww.length-2));
    el.style.left=(ww/2-elW/2)+'px';
  }
  
  function showUserChangePwdForm() {
     var s="<table border='0' width='100%' class='loginForm'>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{User_email}</td><td  class='loginForm'align='left'>"+userId+"</td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{User_name}</td><td  class='loginForm'align='left'>"+((userName && "null"!=userName)?userName:"")+"</td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{OldPassword}</td><td  class='loginForm'align='left'><input type='password' id='login_key_0' value=''></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{NewPassword}</td><td  class='loginForm'align='left'><input type='password' id='login_key' value=''></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='right'>#{ConfirmNewPassword}</td><td  class='loginForm'align='left'><input type='password' id='login_key2' value=''></input></td></tr>";
     s+="<tr class='loginForm'><td  class='loginForm'align='center' colspan='2'>";
     s+="<input type='button' value='#{SaveUser}' onclick='doChangeUserPwd(); '></input>";
     s+="&nbsp;<input type='button' value='#{Cancel}' onclick='var el=document.getElementById(\"loginForm\"); if(el) {el.innerHTML=null; el.style.display=\"none\";}'></input>";
     s+="</td></tr>";
     s+="</table>";
    var el=document.getElementById("loginForm");
    el.innerHTML=s;
    el.style.zIndex=2;
    el.style.display='block';

    var elW=window.getComputedStyle(el, null).getPropertyValue('width');
    var ww=window.innerWidth;
    if("string"==typeof(elW))
    elW=new Number(elW.substring(0,elW.length-2));
    if("string"==typeof(ww))
    ww=new Number(ww.substring(0,ww.length-2));
    el.style.left=(ww/2-elW/2)+'px';
  }
  
  function doLogin() { 
    REST_api.login(
      'form',
      document.getElementById('login_name').value, 
      document.getElementById('login_key').value, 
      false, 
      function(url,data) { 
        if("OK"==data.result)
          location.reload(); 
        else 
          alert(data);
      }
    );
  }

  function doSaveUser() {
  }
  
  function doChangeUserPwd() {
  }
  
  function doLogout() { 
    REST_api.logout(
      function(url,data) { 
        location.reload(); 
      }
    );
  }


// -->