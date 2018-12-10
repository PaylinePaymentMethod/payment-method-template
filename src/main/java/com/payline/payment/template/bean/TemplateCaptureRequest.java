package com.payline.payment.template.bean;

import com.payline.payment.template.utils.InvalidRequestException;
import com.payline.payment.template.utils.TemplateCardConstants;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.request.RedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.request.TransactionStatusRequest;

public class TemplateCaptureRequest extends TemplateRequest {
    private String paymentId;

    public TemplateCaptureRequest(RedirectionPaymentRequest request) throws InvalidRequestException {
        super(request.getContractConfiguration());
        this.paymentId= request.getRequestContext().getRequestData().get(TemplateCardConstants.PSC_ID);
    }

    public TemplateCaptureRequest(String paymentId, ContractConfiguration configuration) throws InvalidRequestException {
        super(configuration);
        this.paymentId = paymentId;
    }

    public TemplateCaptureRequest(TransactionStatusRequest request) throws InvalidRequestException {
        super(request.getContractConfiguration());
        this.paymentId = request.getTransactionId();
    }

    public String getPaymentId() {
        return paymentId;
    }
}
