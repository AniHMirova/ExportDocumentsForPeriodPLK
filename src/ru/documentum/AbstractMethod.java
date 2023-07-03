package ru.documentum;

import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLogger;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.methodserver.IDfMethod;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Веретенников А. Б. 2008.12.
 * <p>
 * На основании класса AbstractMethod из артифакта ds-common-methods. О
 * котором сообщил Левашко.
 */

public abstract class AbstractMethod implements IDfMethod, IDfModule {

    private final static String TRANSACTION_COMMIT = "COMMIT";
    private final static String TRANSACTION_ABORT = "ABORT";

    private IDfClient m_client;
    private PrintWriter m_traceOut;
    private Map m_parameters;
    private IDfSessionManager m_sessionManager;
    private List<IDfSession> m_sessions = new LinkedList<>();
    private IDfSession m_session = null;

    private Boolean m_transactionOpened = false;

    public AbstractMethod() {
        m_client = null;
        m_traceOut = null;
        m_parameters = null;
        m_sessionManager = null;
    }

    private void initialize() {
        m_traceOut
                .println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        m_traceOut.println("<data>");
    }

    private void done() {
        m_traceOut.println("</data>");
    }

    public int execute(Map p_args, PrintWriter p_output) throws Exception {
        m_parameters = p_args;
        m_traceOut = p_output;
        m_client = DfClient.getLocalClient();

        initialize();

        int result;

        try {
            traceDebugStr("Method {0} started", new String[]{getClass()
                    .getName()});

            traceDebugArguments();

            result = doMethodBody();

            commitTransaction();

            traceDebugStr("Method {0} completed, result {1}", new Object[]{
                    getClass().getName(), result});

            return result;
        } catch (Throwable ex) {
            traceErrorStr("Error occured during method execution.", ex);
            abortTransaction();
            result = 2;
        } finally {
            releaseSessions();
        }

        done();

        return result;
    }

    protected abstract int doMethodBody() throws Exception;

    private IDfSessionManager getSessionManager() {
        if (m_sessionManager == null) {
            m_sessionManager = getLocalClient().newSessionManager();
        }
        return m_sessionManager;
    }

    private String getArgumentDocbaseName() throws DfException {
        String docbase = getArgument("docbase",
                false);

        if ((docbase == null) || (docbase.length() == 0))
            docbase = getArgument("docbase_name",
                    false);

        if ((docbase == null) || (docbase.length() == 0))
            throw new DfException(
                    "Argument 'docbase' or 'docbase_name' required");

        return docbase;
    }

    protected IDfSession getDfSession() throws DfException {
        getSessionManager();

        if (m_session == null) {
            String docbaseName = getArgumentDocbaseName();

            traceDebugStr("Getting session {0}", new Object[]{docbaseName});

            m_session = getDfSession(docbaseName);

            if (m_session == null)
                throw new DfException("Unable to obtain session");
        }

        return m_session;
    }

    private IDfSession getDfSession(String p_docbaseName) throws DfException {
        SessionCredentials credentials = new SessionCredentials();
        credentials.setLogin(System.getProperty("user.name"));
        credentials.setPassword(null);
        credentials.setDocbaseName(p_docbaseName);
        return createDfSession(credentials);
    }

    private IDfSession createDfSession(SessionCredentials p_credentials)
            throws DfException {
        registerCredentials(p_credentials);
        if (isTransactionRequired()
                && !getSessionManager().isTransactionActive()) {
            traceDebugStr("begin transaction");
            getSessionManager().beginTransaction();
            m_transactionOpened = true;
        }
        IDfSession session = getSessionManager().getSession(
                p_credentials.getDocbaseName());
        m_sessions.add(session);
        return session;
    }

    private void registerCredentials(SessionCredentials p_credentials)
            throws DfException {
        if (!getSessionManager().hasIdentity(p_credentials.getDocbaseName())) {
            IDfLoginInfo a_Login = p_credentials.getLoginInfo();
            getSessionManager().setIdentity(p_credentials.getDocbaseName(),
                    a_Login);
        }
    }

    public void releaseSessions() {
        for (IDfSession session : m_sessions) {
            getSessionManager().release(session);
        }
        m_sessions.clear();
    }

    private void commitTransaction() throws DfException {
        if (getSessionManager().isTransactionActive() && m_transactionOpened) {
            if (isCommitTransaction()) {
                traceDebugStr("commit transaction");
                getSessionManager().commitTransaction();
            } else {
                traceDebugStr("forced abort transaction");
                getSessionManager().abortTransaction();
            }
            m_transactionOpened = false;
        }
    }

    private void abortTransaction() throws DfException {
        if (getSessionManager().isTransactionActive() && m_transactionOpened) {
            traceDebugStr("abort transaction");
            getSessionManager().abortTransaction();
            m_transactionOpened = false;
        }
    }

    private Boolean isCommitTransaction() throws DfException {
        String transaction = getArgument("transaction", false);
        return TRANSACTION_COMMIT.equals(transaction);
    }

    private boolean isTransactionRequired() throws DfException {
        String transaction = getArgument("transaction", false);
        return TRANSACTION_COMMIT.equals(transaction)
                || TRANSACTION_ABORT.equals(transaction);
    }

    private IDfClient getLocalClient() {
        return m_client;
    }

    private String[] getArgumentArray(String p_parameterName,
                                      boolean p_mandatory) throws DfException {
        String a_Values[] = (String[]) m_parameters
                .get(p_parameterName);
        if (p_mandatory && (a_Values == null || a_Values.length < 1)) {
            throw new DfException((new StringBuilder()).append(
                            "Non-null value(s) for parameter -")
                    .append(p_parameterName).append(" which is required for ")
                    .append(getClass().getName()).append(
                            " method, was not defined in ARGUMENTS.")
                    .toString());
        }
        return a_Values;
    }

    protected String getArgument(String p_parameterName, boolean p_mandatory)
            throws DfException {
        String a_Values[] = getArgumentArray(p_parameterName, p_mandatory);
        if (a_Values != null && a_Values.length > 0) {
            return a_Values[0];
        } else {
            return null;
        }
    }

    protected void traceDebugStr(String p_Str) {
        DfLogger.debug(this, "{0}", new String[]{p_Str}, null);

        if (m_traceOut != null) {
            String builder = "<debug>" +
                    formatString(p_Str) +
                    "</debug>";
            m_traceOut.println(builder);
        }
    }

    private String formatString(String s) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&')
                r.append("&amp;");
            else if (c == '<')
                r.append("&lt;");
            else if (c == '>')
                r.append("&gt;");
            else if (c == '\'')
                r.append("&apos;");
            else if (c == '"')
                r.append("&quot;");
            else
                r.append(c);
        }
        return r.toString();
    }

    protected void traceErrorStr(String p_Str, Throwable p_ex) {
        DfLogger.fatal(this, "{0}", new String[]{p_Str}, p_ex);

        if (m_traceOut != null) {
            p_Str = (new StringBuilder()).append("ERROR! ").append(p_Str)
                    .append("\r\n").toString();

            StringBuilder lineBuilder = new StringBuilder();
            lineBuilder.append("<error>");

            lineBuilder.append(formatString(p_Str));

            if (p_ex != null) {

                StringBuilder errorMessageBuilder = new StringBuilder();

                String errorMessage = p_ex.getMessage();
                if (errorMessage == null)
                    errorMessage = "Message of the exception object is null";

                errorMessageBuilder.append("\r\n[");
                errorMessageBuilder.append(p_ex.getClass().getName());
                errorMessageBuilder.append("] ");
                errorMessageBuilder.append(errorMessage);
                errorMessageBuilder.append("\r\n");

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(stream);

                p_ex.printStackTrace(writer);

                writer.close();
                try {
                    errorMessageBuilder.append(new String(stream.toByteArray(),
                            "UTF-8"));
                } catch (Throwable tr) {
                    errorMessageBuilder
                            .append(new String(stream.toByteArray()));
                }

                lineBuilder
                        .append(formatString(errorMessageBuilder.toString()));
            } else {
                lineBuilder.append("Exception argument is null");
            }
            lineBuilder.append("</error>");

            m_traceOut.println(lineBuilder.toString());
        }
    }

    private void traceDebugStr(String p_Str, Object[] arguments) {
        traceDebugStr(MessageFormat.format(p_Str, arguments));
    }

    /**
     * Печатает переданные методу параметры.
     */
    private void traceDebugArguments() {
        try {
            Iterator it = m_parameters.keySet().iterator();

            traceDebugStr("Arguments:");

            if (!it.hasNext())
                traceDebugStr("List empty");

            while (it.hasNext()) {
                String key = (String) it.next();
                String[] values = (String[]) m_parameters.get(key);
                StringBuilder value = new StringBuilder();
                for (String value1 : values) value.append(value.length() > 0 ? ", " : "").append(value1);

                traceDebugStr("Argument name = ''{0}'' values = ''{1}''",
                        new String[]{key, value.toString()});
            }
        } catch (Throwable tr) {
            traceDebugStr("Internal error while print parameters");
        }
    }

}
