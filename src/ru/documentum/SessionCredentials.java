package ru.documentum;

import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;

class SessionCredentials {
    private String m_docbaseName;
    private String m_login;
    private String m_password;

    SessionCredentials() {
        this(null, null, null);
    }

    private SessionCredentials(String p_docbaseName, String p_login, String p_password) {
        m_docbaseName = p_docbaseName;
        m_login = p_login;
        m_password = p_password;
    }

    IDfLoginInfo getLoginInfo() {
        IDfLoginInfo loginInfo = new DfLoginInfo();
        if (getLogin() != null) {
            loginInfo.setUser(getLogin());
        }
        if (getPassword() != null) {
            loginInfo.setPassword(getPassword());
        }
        return loginInfo;
    }

    String getDocbaseName() {
        return m_docbaseName;
    }

    void setDocbaseName(String p_docbaseName) {
        m_docbaseName = p_docbaseName;
    }

    private String getLogin() {
        return m_login;
    }

    void setLogin(String p_login) {
        m_login = p_login;
    }

    private String getPassword() {
        return m_password;
    }

    void setPassword(String p_password) {
        m_password = p_password;
    }
}
