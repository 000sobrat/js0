/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.mail.Message;
import ssg.lib.common.JSON;
import ssg.lib.http.HttpAuthenticator;
import ssg.lib.http.HttpAuthenticator.Domain;
import ssg.lib.http.HttpSession;
import ssg.lib.http.HttpUser;
import ssg.lib.http.RAT;
import ssg.lib.http.base.HttpRequest;
import ssg.lib.http.rest.annotations.XMethod;
import ssg.lib.http.rest.annotations.XParameter;
import ssg.lib.http.rest.annotations.XType;
import ssg.lib.jana.tools.EmailAgent;
import ssg.lib.oauth.OAuthClient.OAuthContext;
import ssg.lib.oauth.OAuthClient.OAuthUserInfo;

/**
 *
 * @author sesidoro
 */
@XType
public class UM_API implements AppItem, Exportable {

    public static enum UM_STATE {
        pending, // user is not verified, verification request is sent
        verified, // user is verified (e.g. via OAuth, Certificte, or e-mail-based verification
        disabled, // user is diabled - proved to be untrusted
        other
    }

    private static final long serialVersionUID = 1L;

    private String id = "js";
    Map<String, String> users = new LinkedHashMap<>();
    Map<String, UM_STATE> userStates = new LinkedHashMap<>();
    Map<String, String> pwds = new LinkedHashMap<>();
    Map<String, List<String>> roles = new LinkedHashMap<>();

    Map<String, String[]> authVariants = new LinkedHashMap<>();

    HttpAuthenticator.HttpSimpleAuth.Domain domain = new Domain(id) {
        @Override
        public HttpUser authenticate(Object provider, Object... parameters) throws IOException {
            HttpUser r = super.authenticate(provider, parameters);
            if (r != null && r.getProperties().containsKey("email")) {
                String email = (String) r.getProperties().get("email");
                // ensure email is lowcase only
                if (email != null && email.contains("@")) {
                    email = email.toLowerCase();
                }
                if (!r.getId().equals(email) && r.getProperties().containsKey("oauth")) {
                    OAuthContext c = (OAuthContext) r.getProperties().get("oauth");
                    OAuthUserInfo u = c.getOAuthUserInfo();
                    if (users.containsKey(email)) {
                        if (!userStates.containsKey(email) || UM_STATE.disabled != userStates.get(email)) {
                            userStates.put(email, UM_STATE.verified);
                        }
                    } else {
                        UM_API.this.addUser(email, null, UM_STATE.verified);
                    }
                    r = domain.toUser(r, email);
                }
            } else if (r != null) {
                // no e-mail
                if (r.getProperties().containsKey("oauth")) {
                    OAuthContext c = (OAuthContext) r.getProperties().get("oauth");
                    OAuthUserInfo u = c.getOAuthUserInfo();
                    String prefix = c.domain();
                    if (prefix == null) {
                        prefix = "undefined";
                    }
                    String id = prefix + "_" + u.id();
                    if (users.containsKey(id)) {
                        if (!userStates.containsKey(id) || UM_STATE.disabled != userStates.get(id)) {
                            userStates.put(id, UM_STATE.verified);
                        }
                    } else {
                        UM_API.this.addUser(id, null, UM_STATE.verified);
                    }
                    r = domain.toUser(r, id);
                }
            }
            System.out.println("OAuth authenticated user:\n   | " + (("" + r).replace("\n", "\n   | ")));
            return r;
        }

        @Override
        public void addUser(String name, String pwd, String dn, RAT rat) {
            super.addUser(name, pwd, dn, rat);
            if (!users.containsKey(name)) {
                UM_API.this.addUser(name, pwd, null);
            }
        }
    };
    private EmailAgent agent;
    private EmailAgent.EMailListener emailListener = null;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    public Domain getDomain() {
        if (1 == 1) {
            for (String user : pwds.keySet()) {
                if (!domain.hasUser(user)) {
                    RAT rat = new RAT(new ArrayList<String>(), null, null);
                    for (String role : roles.keySet()) {
                        List<String> lst = roles.get(role);
                        if (lst != null && lst.contains(user)) {
                            rat.getRoles().add(role);
                        }
                    }
                    domain.addUser(user, pwds.get(user), domain.getName(), rat);
                }
            }
        }
        return domain;
    }

    boolean addUser(String user, String pwd, UM_STATE verified) {
        if (user != null && !users.containsKey(user)) {
            users.put(user, pwd);
            userStates.put(user, (verified != null) ? verified : UM_STATE.other);
        }
        return false;
    }

    void addUserRoles(String user, String... roles) {
        if (user == null || user.isEmpty() || roles == null || roles.length == 0) {
            return;
        }
        for (String role : roles) {
            List<String> urs = this.roles.get(role);
            if (urs == null) {
                urs = new ArrayList<>();
                this.roles.put(role, urs);
            }
            if (!urs.contains(user)) {
                urs.add(user);
            }
        }
    }

    void removeUserRoles(String user, String... roles) {
        if (user == null || user.isEmpty() || roles == null || roles.length == 0) {
            return;
        }
        for (String role : roles) {
            List<String> urs = this.roles.get(role);
            if (urs != null) {
                if (urs.contains(user)) {
                    urs.remove(user);
                }
            }
        }
    }

    public void setEmailAgent(EmailAgent agent) {
        if (this.agent != null) {
            if (emailListener != null) {
                this.agent.removeEMailListener(emailListener);
            }
        }

        this.agent = agent;
        if (agent != null) {
            if (emailListener == null) {
                emailListener = new EmailAgent.EMailListener() {
                    @Override
                    public void onReceivedEMail(EmailAgent agent, String from, String subject, Message msg) {
                    }

                    @Override
                    public void onSentEMail(EmailAgent agent, String to, String subject, Message msg) {
                    }

                    @Override
                    public void onFailedToSendEMail(EmailAgent agent, String to, String subject, Message msg) {
                    }
                };
            }
            agent.addEMailListener(emailListener);
        }
    }

    public EmailAgent emailAgent() {
        return agent;
    }

    @Override
    public void exportTo(Writer wr) throws IOException {
        JSON.Encoder jsonEncoder = new JSON.Encoder("UTF-8");
        Map m = new LinkedHashMap();
        m.put("users", users);
        m.put("userStates", userStates);
        m.put("pws", pwds);
        m.put("roles", roles);
        //System.out.println("M : "+m.toString());
        jsonEncoder.writeObject(m, wr);
    }

    @Override
    public void importFrom(Reader rdr) throws IOException {
        JSON.Decoder jsonDecoder = new JSON.Decoder("UTF-8");
        Map m = jsonDecoder.readObject(rdr, Map.class);
        if (m.containsKey("users")) {
            users.putAll((Map) m.get("users"));
        }
        if (m.containsKey("userStates")) {
            userStates.putAll((Map) m.get("userStates"));
        } else {
            // if no userStates info -> all users are trusted -> verified state
            for (String un : users.keySet()) {
                userStates.put(un, UM_STATE.verified);
            }
        }
        if (m.containsKey("pws")) {
            pwds.putAll((Map) m.get("pws"));
        }
        if (m.containsKey("roles")) {
            roles.putAll((Map) m.get("roles"));
        }
    }

    public String findUser(String name) {
        if (users.containsKey(name)) {
            return name;
        }
        if (users.containsValue(name)) {
            for (Entry<String, String> e : users.entrySet()) {
                if (name.equals(e.getValue())) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    @XMethod(name = "login")
    public String login(HttpRequest req,
            @XParameter(name = "type", optional = true) String type,
            @XParameter(name = "user", optional = true) String user,
            @XParameter(name = "pwd", optional = true) String pwd,
            @XParameter(name = "force", optional = true) boolean force
    ) throws IOException {
        if (req.getHttpSession() != null && req.getHttpSession().isValid()) {
            HttpSession sess = req.getHttpSession();
            if (sess.getUser() == null || sess.getRevalidateUser() != null || force) {
                if (type != null && "Basic".equalsIgnoreCase(type) && (sess.getApplication() == null || sess.getApplication().isBasicAuthEnabled())) {
                    String vp = (sess.getRevalidateUser() != null) ? "?" : "";
                    sess.setRevalidateUser(null);
                    sess.getApplication().doAuthentication(req);
//                    HttpResponse resp = req.getResponse();
//                    resp.setResponseCode(401, "Access denied");
//                    String dn = sess.getBaseURL(); // "localhost/app/"; // domain.getName()
//                    if (dn.contains("//")) {
//                        dn = dn.substring(dn.indexOf("//") + 2);
//                    }
//                    resp.setHeader("WWW-Authenticate", "Basic realm=\"" + vp + dn + "\", charset=\"UTF-8\"");
//                    //resp.setHeader(HttpData.HH_CONTENT_LENGTH, "" + buf.length);
//                    resp.onHeaderLoaded();
//                    resp.onLoaded();
                    return "OK";
                } else if (user != null) {
                    // ensure email is lowcase only
                    if (user.contains("@")) {
                        user = user.toLowerCase();
                    }
                    HttpUser httpUser = domain.authenticate(null, null, user, pwd);
                    if (httpUser != null) {
                        httpUser.getProperties().put("email", httpUser.getName());
                        httpUser.getProperties().put("name", (users.containsKey(httpUser.getName())) ? users.get(httpUser.getName()) : "");
                        sess.setUser(httpUser);
                        return "OK";
                    }
                }
            }
        }
        throw new IOException("no authentication available for type=" + type + ", user=" + user);
    }

    @XMethod(name = "logout")
    public String logout(HttpRequest req
    ) {
        HttpUser user = null;
        if (req.getHttpSession() != null && req.getHttpSession().isValid()) {
            user = req.getHttpSession().getUser();
            req.getHttpSession().setUser(null);
            //req.getHttpSession().setRevalidateUser("revalidate");
            return "logged out " + ((user != null) ? user.getName() : "");
        } else {
            return "not logged in";
        }
    }

    @XMethod(name = "loginVariants")
    public Map<String, String[]> loginVariants(HttpRequest req) {
        Map<String, String[]> r = new LinkedHashMap<>();

        String host = req.getHostURL();
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        String path = req.getQuery();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = path.substring(0, path.lastIndexOf("/") + 1);
        r.put(host + "/" + path, null);

        for (Entry<String, String[]> entry : authVariants.entrySet()) {
            String s = entry.getKey();
            if (s.startsWith("/")) {
                s = s.substring(1);
            }
            r.put(host + "/" + s, entry.getValue());
        }
        return r;
    }

    public Map<String, String[]> getAuthVariants() {
        return authVariants;
    }
}
