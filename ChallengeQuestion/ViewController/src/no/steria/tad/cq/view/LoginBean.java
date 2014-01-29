package no.steria.tad.cq.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import javax.naming.Context;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import oracle.iam.platform.OIMClient;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;

public class LoginBean {
    private String _username,_password;
    private String _systemUser,_systemPassword;
    private String _question,_answer;
    private String _useLDAPS;
    private String _questionField,_answerField;
    private String _activeDomain,_activeDomainController,_oracleIdentityManagerServer;
    private static String OIMInitialContextFactory = "weblogic.jndi.WLInitialContextFactory";


    public static OIMClient loginWithCustomEnv(String OIMURL,String username,String password) throws LoginException {
            System.setProperty("APPSERVER_TYPE", "wls");
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, OIMInitialContextFactory);
            env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, OIMURL);
            OIMClient client = new OIMClient(env);
            client.login(username, password.toCharArray());
            return client;
    }

    public void setUsername(String _username) {
        this._username = _username;
    }

    public String getUsername() {
        return _username;
    }

    public void setPassword(String _password) {
        this._password = _password;
    }

    public String getPassword() {
        return _password;
    }
    public String doLogin() {
        String un = this._username;
        byte pwBytes[] = this._password.getBytes();
        //char pwChars[] = this._password.toCharArray();
        String q = this._question;
        String a = this._answer;
        setUsername(null);
        setPassword(null);
        setQuestion(null);
        setAnswer(null);
        

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        if ("true".equals(this._useLDAPS)) {
            env.put(Context.PROVIDER_URL, "ldap://"+this._activeDomainController+":636/");
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        else {
            env.put(Context.PROVIDER_URL, "ldap://"+this._activeDomainController+":389/");
        }
        // Fill in secuirty/bind variables
        env.put(Context.SECURITY_AUTHENTICATION, "simple"  ); 
        env.put(Context.SECURITY_PRINCIPAL,un+"@"+this._activeDomain); //returns user@domain.local
        env.put(Context.SECURITY_CREDENTIALS,pwBytes); //returns password

        // Create the initial context
        Context initialCtx;
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            initialCtx = new InitialDirContext(env);
            initialCtx.close();
        } catch (NamingException e) {
            e.printStackTrace();
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Ugyldig brukernavn eller passord", "Ugyldig brukernavn eller passord : ");
            ctx.addMessage(null,msg);
            return null;
        } catch (Throwable  t) {
           t.printStackTrace();
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"",t.getMessage());
            ctx.addMessage(null,msg);
            return null;
        }
        OIMClient client = null;
        try {
            client = loginWithCustomEnv("t3://"+this._oracleIdentityManagerServer+":14000",this._systemUser,this._systemPassword);
        } catch (FailedLoginException e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Ugyldig systembruker eller passord", "Ugyldig systembruker eller passord : "  + e.getMessage());
            ctx.addMessage(null,msg);
            try {
                client.logout();
            }
            catch(Throwable t) {
               //Nothing to do 
            }
            return null;
        } catch (LoginException e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"System-login feilet", e.getMessage());
            ctx.addMessage(null,msg);
            try {
                client.logout();
            }
            catch(Throwable t) {
               //Nothing to do 
            }
            return null;
        }
        catch(Throwable t) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,t.getClass().getName(), t.getMessage());
            ctx.addMessage(null,msg);
            try {
                client.logout();
            }
            catch(Throwable t1) {
               //Nothing to do 
            }
            return null;
        }
        UserManager umgr = client.getService(UserManager.class);
        List<User> users = null;
        Set<String> attrNames = null;
        attrNames = new HashSet<String>();
        HashMap<String, Object> parameters = new HashMap<String,Object>();
        SearchCriteria criteria = null;
        criteria  = new SearchCriteria(UserManagerConstants.AttributeName.USER_LOGIN.getId(),un, SearchCriteria.Operator.EQUAL);
        try {
            users = umgr.search(criteria, attrNames, parameters);
        } catch (UserSearchException e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,un + " ikke funnet i Oracle Identity Manager", un + " ikke funnet i Oracle Identity Manager");
            ctx.addMessage(null,msg);
            client.logout();;
            return null;
        }
        if (users.size() == 1) {
            User u = users.get(0);
            try {
                HashMap<String, Object> attrMap = new HashMap<String, Object>();
                User modifyUser = new User(u.getId(),attrMap);
                modifyUser.setAttribute(this.getQuestionField(), q);
                modifyUser.setAttribute(this.getAnswerField(), a);
                umgr.modify(UserManagerConstants.AttributeName.USER_LOGIN.getId(), u.getLogin(), modifyUser);
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,"Suksess", "Ditt personlige spørsmål/svar er lagret");
                ctx.addMessage(null,msg);
            } catch (ValidationFailedException e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"FailedValidationException", e.getMessage());
                ctx.addMessage(null,msg);
                return null;
            } catch (UserModifyException e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Spørsmål og svar ble ikke lagt til", e.getMessage());
                ctx.addMessage(null,msg);
                return null;
            } catch (NoSuchUserException e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,un + " ikke funnet i Oracle Identity Manager", un + " ikke funnet i Oracle Identity Manager");
                ctx.addMessage(null,msg);
                return null;
            } catch (SearchKeyNotUniqueException e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"SearchKeyNotUniqueException", e.getMessage());
                ctx.addMessage(null,msg);
                return null;
            }
            catch(AccessDeniedException e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Bruker har ikke rettigheter til å endre attributtene", "Bruker har ikke rettigheter til å endre attributtene");
                ctx.addMessage(null,msg);
                return null;
            } catch (Exception e) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,e.getClass().getName(), e.getMessage());
                ctx.addMessage(null,msg);
                return null;
            }
            finally {
                try {
                    client.logout();
                }
                catch(Throwable t) {
                   //Nothing to do 
                }
            }
            return null;
        }
        else {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,un + " ikke funnet i Oracle Identity Manager", un + " ikke funnet i Oracle Identity Manager");
            ctx.addMessage(null,msg);
            client.logout();
            return null;
        }
    }

    public void setQuestion(String _question) {
        this._question = _question;
    }

    public String getQuestion() {
        return _question;
    }

    public void setAnswer(String _answer) {
        this._answer = _answer;
    }

    public String getAnswer() {
        return _answer;
    }

    public void setActiveDomainController(String _activeDomainController) {
        this._activeDomainController = _activeDomainController;
    }

    public String getActiveDomainController() {
        return _activeDomainController;
    }

    public void setSystemUser(String _systemUser) {
        this._systemUser = _systemUser;
    }

    public String getSystemUser() {
        return _systemUser;
    }

    public void setSystemPassword(String _systemPassword) {
        this._systemPassword = _systemPassword;
    }

    public String getSystemPassword() {
        return _systemPassword;
    }

    public void setActiveDomain(String _activeDomain) {
        this._activeDomain = _activeDomain;
    }

    public String getActiveDomain() {
        return _activeDomain;
    }

    public void setOracleIdentityManagerServer(String _oracleIdentityManagerServer) {
        this._oracleIdentityManagerServer = _oracleIdentityManagerServer;
    }

    public String getOracleIdentityManagerServer() {
        return _oracleIdentityManagerServer;
    }

    public void setUseLDAPS(String _useLDAPS) {
        this._useLDAPS = _useLDAPS;
    }

    public String getUseLDAPS() {
        return _useLDAPS;
    }

    public void setQuestionField(String _questionField) {
        this._questionField = _questionField;
    }

    public String getQuestionField() {
        return _questionField;
    }

    public void setAnswerField(String _answerField) {
        this._answerField = _answerField;
    }

    public String getAnswerField() {
        return _answerField;
    }
}
	