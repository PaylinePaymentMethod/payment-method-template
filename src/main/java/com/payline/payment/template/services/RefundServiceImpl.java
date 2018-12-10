package com.payline.payment.template.services;

import com.payline.payment.template.bean.TemplatePaymentRequest;
import com.payline.payment.template.bean.TemplatePaymentResponse;
import com.payline.payment.template.utils.InvalidRequestException;
import com.payline.payment.template.utils.TemplateCardConstants;
import com.payline.payment.template.utils.TemplateErrorHandler;
import com.payline.payment.template.utils.TemplateHttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.refund.response.RefundResponse;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseSuccess;
import com.payline.pmapi.service.RefundService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;

public class RefundServiceImpl implements RefundService {
    private static final Logger LOGGER = LogManager.getLogger(RefundServiceImpl.class);

    private TemplateHttpClient client;

    public RefundServiceImpl() {
        this.client = new TemplateHttpClient();
    }

    @Override
    public RefundResponse refundRequest(RefundRequest refundRequest) {
        String transactionId = refundRequest.getTransactionId();
        try {
            boolean isSandbox = refundRequest.getEnvironment().isSandbox();
            TemplatePaymentRequest request = createRequest(refundRequest);

            TemplatePaymentResponse response = client.refund(request, isSandbox);

            if (response.getCode() != null) {
                return TemplateErrorHandler.findRefundError(response, transactionId);
            } else if (!TemplateCardConstants.STATUS_REFUND_SUCCESS.equals(response.getStatus())) {
                return TemplateErrorHandler.getRefundResponseFailure(FailureCause.PARTNER_UNKNOWN_ERROR, transactionId);
            }

            updateRequest(request);
            response = client.refund(request, isSandbox);

            if (response.getCode() != null) {
                return TemplateErrorHandler.findRefundError(response, transactionId);
            } else if (!TemplateCardConstants.STATUS_SUCCESS.equals(response.getStatus())) {
                return TemplateErrorHandler.getRefundResponseFailure(FailureCause.PARTNER_UNKNOWN_ERROR, transactionId);
            }

            // refund Success
            return RefundResponseSuccess.RefundResponseSuccessBuilder.aRefundResponseSuccess()
                    .withStatusCode("0")
                    .withPartnerTransactionId(transactionId)
                    .build();


        } catch (InvalidRequestException | URISyntaxException | IOException e) {
            LOGGER.error("unable to refund the payment: {}" , e.getMessage(), e);
            return TemplateErrorHandler.getRefundResponseFailure(FailureCause.CANCEL, transactionId);
        }
    }

    public TemplatePaymentRequest createRequest(RefundRequest refundRequest) throws InvalidRequestException {
        return new TemplatePaymentRequest(refundRequest);
    }

    public void updateRequest(TemplatePaymentRequest request) {
        request.setCapture(true);
    }


    @Override
    public boolean canMultiple() {
        return false;
    }

    @Override
    public boolean canPartial() {
        return false;
    }
}
