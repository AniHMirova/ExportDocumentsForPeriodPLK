import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.operations.IDfExportNode;
import com.documentum.operations.IDfExportOperation;
import com.documentum.operations.outbound.DfExportOperation;

import java.io.File;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {

//        IDfSession mySession = null;
//        SessionManager mySessMgr = null;
        String outputFile;
        String fromDate;
        String toDate;
        String documentType;
        IDfSessionManager sMgr = null;
        IDfSession session = null;



        try {
            DfClientX cx = new DfClientX();
            IDfClient dfCl = cx.getLocalClient();
            IDfLoginInfo li = cx.getLoginInfo();
            li.setUser("dmadmin");
            li.setPassword("c2EQ95979K");
//            IDfSessionManager sMgr = dfCl.newSessionManager();
            sMgr = dfCl.newSessionManager();
            sMgr.setIdentity("D2PRD", li);

//	    	IDfSession session = sMgr.getSession("LKRTEST");
            session = sMgr.getSession("D2PRD");

            outputFile = "C:\\Temp\\export";
            fromDate = "02/02/2000";
            toDate = "02/02/2023";
            documentType = "ddt_internal";

            PrintStream fileOutput = new PrintStream(outputFile + "\\outputResult.txt");

            System.out.println("outputFile = [" + outputFile + "]");
            System.out.println("fromDate = [" + fromDate + "]");
            System.out.println("toDate = [" + toDate + "]");
            System.out.println("documentType = [" + documentType + "]");

            String allDocumentsQuery = MessageFormat.format("select r_object_id from ddt_general where r_object_id = ''090006ac81e60da6''", documentType);
            List<HashMap<String, String>> resultAllDocumentsQuery = runQuery(session, allDocumentsQuery);
            String rObjectId = resultAllDocumentsQuery.get(0).get("r_object_id");

            IDfSysObject docObj  = (IDfSysObject) session.getObject(new DfId(rObjectId));
            String regNumber = docObj.getString("dss_reg_number");

            System.out.println("regNumber = [" + regNumber + "]");

            if(regNumber.contains("\\")){
                 regNumber = regNumber.replace("\\", "_");
            }
            System.out.println("regNumber = [" + regNumber + "]");

            String folder_name = (new StringBuilder()).append(regNumber).toString();
            File folder = new File((new StringBuilder(String.valueOf(outputFile))).append("\\").append(folder_name).toString());
            folder.mkdir();

            System.out.println("documentType = [" + documentType + "]");

            String getPath = (new StringBuilder(String.valueOf(outputFile))).append("\\").append(folder_name).append("\\").toString();

            System.out.println("folder = [" + folder.getName() + "]");
            System.out.println("getPath = [" + getPath + "]");


//            String allAttachmentsQuery = MessageFormat.format("select r_object_id, r_object_type, a_content_type from ddt_document_content where r_object_id in (select parent_id from dm_relation where child_id=''090006ac81f0753c'' and relation_name = ''DOC_APPENDIX'' and child_label IN (SELECT r_version_label from ddt_registered (all) where r_object_id = ''090006ac81f0753c'')) and r_content_size>0 order by dsi_appendix_order", documentType);
//            List<HashMap<String, String>> resultAttachmentsQuery = runQuery(session, allAttachmentsQuery);
//            String attachObjectId;
//            IDfSysObject attachObj;
//            if (resultAttachmentsQuery.size() > 0) {
//                for (int i = 0; i < resultAttachmentsQuery.size(); i++) {
//                    attachObjectId = resultAttachmentsQuery.get(i).get("r_object_id");
//                    attachObj  = (IDfSysObject) session.getObject(new DfId(attachObjectId));
//                    System.out.println("attachObjectId = [" + attachObjectId + "]");
//                    IDfClientX clientx = new DfClientX();
//                    IDfExportOperation operation = clientx.getExportOperation();
//                    operation.setDestinationDirectory( getPath );
//                    IDfExportNode node = (IDfExportNode)operation.add( attachObj );
//                    node.setFormat( attachObj.getFormat().getName() );
//                    operation.execute();
//
//                }
//            }


            IDfClientX clientx = new DfClientX();
            IDfExportOperation operation = clientx.getExportOperation();
            operation.setDestinationDirectory( getPath );
            IDfExportNode node = (IDfExportNode)operation.add( docObj );
//            node.setFormat( "pdf");
            operation.execute();
            System.out.println( "exported file path: " + node.getFilePath() );

            System.out.println("exportOperation = [" + operation.getDescription() + "]");
            if(!operation.execute())
                fileOutput.println("[Export operation failed]");
            else
                fileOutput.println("[Export operation successful]");


        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static List<HashMap<String, String>> runQuery(IDfSession session, String query) throws DfException {

        List<HashMap<String, String>> results = new ArrayList<>();
        IDfQuery q = new DfQuery();
        q.setDQL(query);
        IDfCollection col = q.execute(session, DfQuery.DF_READ_QUERY);
        while (col.next()) {
            HashMap<String, String> row = new HashMap<>();
            for (int i = 0; i < col.getAttrCount(); i++) {
                String attributeName = col.getAttr(i).getName();
                row.put(attributeName, col.getString(attributeName));
            }
            results.add(row);
        }
        col.close();

        return results;
    }

    }
