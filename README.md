# Export Documents and their attachments from D2 PLK

## This method works for the following types:
 ### -ddt_ro_contract
 ### -ddt_invoice_outgoing
 ### -ddt_invoice_ingoing
## Every document and all its attachments you can find in folder, created by different attributes.
###                    IF ddt_ro_contract         -->  dss_reg_number                 : Nr.JURIDIC 024A\13.01.2022 --> Nr.JURIDIC 024A_13.01.2022
###                    IF ddt_invoice_outgoing    --> dss_log_number                  : 3550096003
###                    IF ddt_invoice_ingoing     --> dss_iap_number                  : 0950002672
###                          dss_invoice_type                : proform

###                    IF ddt_invoice_ingoing     --> dss_log_number                  : 5800000480
###                          dss_invoice_type                : cashdesk
###                          dss_invoice_type                : Ingoing


## Arguments:
 ### config_file
 ### from_date
 ### to_date
 ### document_type
