package com.payline.payment.template.services;

import com.payline.payment.template.bean.TemplateCaptureRequest;
import com.payline.payment.template.bean.TemplatePaymentResponse;
import com.payline.payment.template.utils.InvalidRequestException;
import com.payline.payment.template.utils.TemplateCardConstants;
import com.payline.payment.template.utils.TemplateErrorHandler;
import com.payline.payment.template.utils.TemplateHttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.RedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.request.TransactionStatusRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.Card;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.CardPayment;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.service.PaymentWithRedirectionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.YearMonth;

public class PaymentWithRedirectionServiceImpl implements PaymentWithRedirectionService {
    private static final Logger logger = LogManager.getLogger(PaymentWithRedirectionServiceImpl.class);

    private TemplateHttpClient httpClient;

    public PaymentWithRedirectionServiceImpl() {
        httpClient = new TemplateHttpClient();
    }

    @Override
    public PaymentResponse finalizeRedirectionPayment(RedirectionPaymentRequest redirectionPaymentRequest) {
        try {
            TemplateCaptureRequest request = createRequest(redirectionPaymentRequest);
            boolean isSandbox = redirectionPaymentRequest.getEnvironment().isSandbox();

            // first try
            PaymentResponse response = validatePayment(request, isSandbox);
            if (PaymentResponseSuccess.class.equals(response.getClass())) {
                return response;
            } else {
                // second try
                return validatePayment(request, isSandbox);
            }

        } catch (InvalidRequestException e) {
            logger.error("unable to finalize the payment: {}", e.getMessage(), e);
            return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.INTERNAL_ERROR);
        }
    }

    @Override
    public PaymentResponse handleSessionExpired(TransactionStatusRequest transactionStatusRequest) {
        try {
            TemplateCaptureRequest request = createRequest(transactionStatusRequest);
            boolean isSandbox = transactionStatusRequest.getEnvironment().isSandbox();

            return validatePayment(request, isSandbox);
        } catch (InvalidRequestException e) {
            logger.error("unable to handle the session expiration: {}", e.getMessage(), e);
            return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.INVALID_DATA);
        }
    }

    /**
     * Used for test (mocking)
     *
     * @param transactionStatusRequest
     * @return
     * @throws InvalidRequestException
     */
    public TemplateCaptureRequest createRequest(TransactionStatusRequest transactionStatusRequest) throws InvalidRequestException {
        return new TemplateCaptureRequest(transactionStatusRequest);
    }

    /**
     * Used for test (mocking)
     *
     * @param redirectionPaymentRequest
     * @return
     * @throws InvalidRequestException
     */
    public TemplateCaptureRequest createRequest(RedirectionPaymentRequest redirectionPaymentRequest) throws InvalidRequestException {
        return new TemplateCaptureRequest(redirectionPaymentRequest);
    }

    private PaymentResponse getErrorFromStatus(String status) {
        switch (status) {
            case TemplateCardConstants.STATUS_CANCELED_CUSTOMER:
                return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.CANCEL);
            case TemplateCardConstants.STATUS_CANCELED_MERCHANT:
                return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.CANCEL);
            case TemplateCardConstants.STATUS_EXPIRED:
                return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.SESSION_EXPIRED);
            default:
                return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.PARTNER_UNKNOWN_ERROR);
        }
    }

    private PaymentResponseSuccess createResponseSuccess(TemplatePaymentResponse response) {
        Card card = Card.CardBuilder.aCard()
                .withPan(response.getFirstCardDetails().getSerial())
                .withExpirationDate(YearMonth.now())
                .build();

        CardPayment cardPayment = CardPayment.CardPaymentBuilder.aCardPayment()
                .withCard(card)
                .build();

        return PaymentResponseSuccess.PaymentResponseSuccessBuilder.aPaymentResponseSuccess()
                .withStatusCode("0")
                .withPartnerTransactionId(response.getId())
                .withTransactionDetails(cardPayment)
                .build();
    }

    private PaymentResponse validatePayment(TemplateCaptureRequest request, boolean isSandbox) {
        try {
            // retrieve payment data
            TemplatePaymentResponse response = httpClient.retrievePaymentData(request, isSandbox);
            if (response.getCode() != null) {
                return TemplateErrorHandler.findError(response);
            } else {
                // check if the payment has to be captured
                if (TemplateCardConstants.STATUS_AUTHORIZED.equals(response.getStatus())) {
                    response = httpClient.capture(request, isSandbox);
                }

                if (response.getCode() != null) {
                    return TemplateErrorHandler.findError(response);
                }
                // check if the payment is well captured
                if (TemplateCardConstants.STATUS_SUCCESS.equals(response.getStatus())) {
                    return createResponseSuccess(response);
                } else {
                    return getErrorFromStatus(response.getStatus());
                }
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("unable to validate the payment: {}", e.getMessage(), e);
            return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.COMMUNICATION_ERROR);
        }
    }
}
