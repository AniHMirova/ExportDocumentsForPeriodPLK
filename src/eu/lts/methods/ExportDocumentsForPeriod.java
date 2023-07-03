package eu.lts.methods;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.operations.IDfExportNode;
import com.documentum.operations.IDfExportOperation;
import ru.documentum.AbstractMethod;

import java.io.File;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExportDocumentsForPeriod extends AbstractMethod {
    private IDfSession session = null;
    static String outputFile;
    static String fromDate;
    static String toDate;
    static String documentType;

    @Override
    public int doMethodBody() throws Exception {
        final String TRACE_PREFIX1 = "[ExportDocumentsForPeriod]";
        final String TRACE_PREFIX = TRACE_PREFIX1 + "[execute]: ";
        IDfSession session;
        try {
            System.out.println(TRACE_PREFIX + "---------------------------------------------------job started");
            session = getDfSession();

            outputFile = getArgument("config_file", true);
            fromDate = getArgument("from_date", true);
            toDate = getArgument("to_date", true);
            documentType = getArgument("document_type", true);

            PrintStream fileOutput = new PrintStream(outputFile + "\\outputResult.txt");

            traceDebugStr(TRACE_PREFIX + "outputFile = [" + outputFile + "]");
            traceDebugStr(TRACE_PREFIX + "fromDate = [" + fromDate + "]");
            traceDebugStr(TRACE_PREFIX + "toDate = [" + toDate + "]");
            traceDebugStr(TRACE_PREFIX + "documentType = [" + documentType + "]");

            System.out.println("outputFile = [" + outputFile + "]");
            System.out.println("fromDate = [" + fromDate + "]");
            System.out.println("toDate = [" + toDate + "]");
            System.out.println("documentType = [" + documentType + "]");

            String allDocumentsQuery = MessageFormat.format("select r_object_id from {0} where dsdt_reg_date >= DATE(''{1}'') and  dsdt_reg_date <= DATE(''{2}'')", documentType, fromDate, toDate);
//            write  info in file
            fileOutput.println(TRACE_PREFIX1 + "[Export operation start for period: ]" + fromDate + " to " + toDate);
            fileOutput.println(TRACE_PREFIX1 + "[Query: ]" + allDocumentsQuery);

            List<HashMap<String, String>> resultAllDocumentsQuery = runQuery(session, allDocumentsQuery);
            fileOutput.println(TRACE_PREFIX1 + "[All documents for this period are ]" + resultAllDocumentsQuery.size());
            String rObjectId;
            IDfSysObject docObj;
            String invoiceType;
            String regNumber;
            String logNumber;
            String folder_name = null;

            int countSuccess = 0;
            int countFail = 0;

            if (resultAllDocumentsQuery.size() > 0) {
                for (int i = 0; i < resultAllDocumentsQuery.size(); i++) {
                    rObjectId = resultAllDocumentsQuery.get(i).get("r_object_id");
                    traceDebugStr(TRACE_PREFIX + "rObjectId = [" + rObjectId + "]");
                    docObj = (IDfSysObject) session.getObject(new DfId(rObjectId));

//                    Проверки за типа документ
//
//                    Ако е ddt_ro_contract         -->  dss_reg_number                 : Nr.JURIDIC 024A\13.01.2022 --> Nr.JURIDIC 024A_13.01.2022
//                    Ако е ddt_invoice_outgoing    --> dss_log_number                  : 3550096003
//                    Ако е ddt_invoice_ingoing     --> dss_iap_number                  : 0950002672
//                          dss_invoice_type                : proform
//
//                    Ако е ddt_invoice_ingoing     --> dss_log_number                  : 5800000480
//                          dss_invoice_type                : cashdesk
//                          dss_invoice_type                : Ingoing

                    System.out.println("documentType = [" + documentType + "]");

                    if(documentType.equals("ddt_ro_contract")){
                        regNumber = docObj.getString("dss_reg_number");
                        fileOutput.println(TRACE_PREFIX1 + "-----------OLD--------------- " + regNumber + " -------------------------");

                        if (regNumber.contains("\\")) {
                            regNumber = regNumber.replace("\\", "_");
                        }
                        fileOutput.println(TRACE_PREFIX1 + "-------- NEW------------------- " + regNumber + " -------------------------");
                        folder_name = (new StringBuilder()).append(regNumber).toString();
                    }

                    else if(documentType.equals("ddt_invoice_outgoing")){
                        logNumber = docObj.getString("dss_log_number");
                        fileOutput.println(TRACE_PREFIX1 + "------------------------- " + logNumber + " -------------------------");
                        folder_name = (new StringBuilder()).append(logNumber).toString();
                    }

                    else if(documentType.equals("ddt_invoice_ingoing") ){
                        invoiceType = docObj.getString("dss_invoice_type");
                        if(invoiceType.equals("Ingoing") || invoiceType.equals("cashdesk")){
                            logNumber = docObj.getString("dss_log_number");
                            fileOutput.println(TRACE_PREFIX1 + "------------------------- " + logNumber + " -------------------------");
                        }
                        else{
                            logNumber = docObj.getString("dss_iap_number");
                            fileOutput.println(TRACE_PREFIX1 + "------------------------- " + logNumber + " -------------------------");
                        }
                        folder_name = (new StringBuilder()).append(logNumber).toString();
                    }

                    else {
                        fileOutput.println(TRACE_PREFIX1 + "Method is working only for types: ddt_ro_contract, ddt_invoice_outgoing, ddt_invoice_ingoing");
                        return 0;
                    }


                    File folder = new File((new StringBuilder(String.valueOf(outputFile))).append("\\").append(folder_name).toString());
                    folder.mkdir();

                    String getPath = (new StringBuilder(String.valueOf(outputFile))).append("\\").append(folder_name).append("\\").toString();

                    System.out.println("folder = [" + folder.getName() + "]");
                    System.out.println("getPath = [" + getPath + "]");

//            get all attached files
                    String allAttachmentsQuery = MessageFormat.format("select r_object_id, r_object_type, a_content_type from ddt_document_content where r_object_id in " +
                            "(select parent_id from dm_relation where child_id=''{0}'' and relation_name = ''DOC_APPENDIX'' and child_label IN " +
                            "(SELECT r_version_label from ddt_registered (all) where r_object_id = ''{0}'')) and r_content_size>0 order by dsi_appendix_order", rObjectId);
                    List<HashMap<String, String>> resultAttachmentsQuery = runQuery(session, allAttachmentsQuery);
                    String attachObjectId;
                    IDfSysObject attachObj;
                    if (resultAttachmentsQuery.size() > 0) {
                        for (int j = 0; j < resultAttachmentsQuery.size(); j++) {
                            attachObjectId = resultAttachmentsQuery.get(j).get("r_object_id");
                            attachObj = (IDfSysObject) session.getObject(new DfId(attachObjectId));
                            System.out.println("attachObjectId = [" + attachObjectId + "]");
                            IDfClientX clientx = new DfClientX();
                            IDfExportOperation operation = clientx.getExportOperation();
                            operation.setDestinationDirectory(getPath);
                            IDfExportNode node = (IDfExportNode) operation.add(attachObj);
                            node.setFormat(attachObj.getFormat().getName());
                            if (!operation.execute()) {
                                countFail ++;
                                fileOutput.println(TRACE_PREFIX1 + "[Failed] :" + attachObj.getObjectId());
                            } else {
                                countSuccess ++;
                                fileOutput.println(TRACE_PREFIX1 + "[Successful] " + attachObj.getObjectId());
                            }
                        }
                    }

//            get main document
                    IDfClientX clientx = new DfClientX();
                    IDfExportOperation operation = clientx.getExportOperation();
                    operation.setDestinationDirectory(getPath);
                    IDfExportNode node = (IDfExportNode) operation.add(docObj);
//            node.setFormat( docObj.getFormat().getName() );
                    if (!operation.execute()) {
                        countFail ++;
                        fileOutput.println(TRACE_PREFIX1 + "[Failed] :" + docObj.getObjectId());
                    } else {
                        countSuccess ++;
                        fileOutput.println(TRACE_PREFIX1 + "[Successful] " + docObj.getObjectId());
                    }

                }

            }
            fileOutput.println(TRACE_PREFIX1 + "[Export operation finished successfully ]");
            fileOutput.println(TRACE_PREFIX1 + "[Export successfully " + countSuccess + " files ]");
            fileOutput.println(TRACE_PREFIX1 + "[Failed export for " + countFail + " files ]");

        } catch (Exception ex) {
            traceErrorStr(TRACE_PREFIX + "Exception:", ex);
        } finally {
            traceDebugStr(TRACE_PREFIX + "try to releaseSession");
            releaseSessions();
            traceDebugStr(TRACE_PREFIX + "job finished");
        }
        return 0;
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
