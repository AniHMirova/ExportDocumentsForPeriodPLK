import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfLoginInfo;

public class SessionManager {
    // Member variables used to encapsulate session and user information.
    private IDfSessionManager m_sessionMgr;
    private String m_repository;
    private String m_userName;
    private String m_password;

    public SessionManager(String rep, String user, String pword) {
        try {
// Populate member variables.
            m_repository = rep;
            m_userName = user;
            m_password = pword;
// Call the local createSessionManager method.
            m_sessionMgr = createSessionManager();
        } catch (Exception e) {
            System.out.println("An exception has been thrown: " + e);
        }
    }

    private IDfSessionManager createSessionManager() throws Exception {
        DfClientX clientx = new DfClientX();
// Most objects are created using factory methods in interfaces.
// Create a client based on the DfClientX object.
        IDfClient client = clientx.getLocalClient();
// Create a session manager based on the local client.
        IDfSessionManager sMgr = client.newSessionManager();
// Set the user information in the login information variable.
        IDfLoginInfo loginInfo = clientx.getLoginInfo();
        loginInfo.setUser(m_userName);
        loginInfo.setPassword(m_password);
// Set the identity of the session manager object based on the repository
// name and login information.
        sMgr.setIdentity(m_repository, loginInfo);
// Return the populated session manager to the calling class. The session
// manager object now has the required information to connect to the
// repository, but is not actively connected.
        return sMgr;
    }

    // Request an active connection to the repository.
    public IDfSession getSession() throws DfException {
        return m_sessionMgr.getSession(m_repository);
    }

    // Release an active connection to the repository for reuse.
    public void releaseSession(IDfSession session) {
        m_sessionMgr.release(session);
    }
}
