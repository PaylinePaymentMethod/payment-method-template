package com.payline.payment.template.services;

import com.payline.payment.template.bean.TemplatePaymentRequest;
import com.payline.payment.template.bean.TemplatePaymentResponse;
import com.payline.payment.template.utils.InvalidRequestException;
import com.payline.payment.template.utils.TemplateCardConstants;
import com.payline.payment.template.utils.TemplateErrorHandler;
import com.payline.payment.template.utils.TemplateHttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PaymentServiceImpl implements PaymentService {
    private static final Logger logger = LogManager.getLogger(PaymentServiceImpl.class);

    private TemplateHttpClient httpClient = new TemplateHttpClient();

    @Override
    public PaymentResponse paymentRequest(PaymentRequest paymentRequest) {
        try {
            // create the payment request
            TemplatePaymentRequest request = new TemplatePaymentRequest(paymentRequest);

            Boolean isSandbox = paymentRequest.getEnvironment().isSandbox();
            TemplatePaymentResponse response = httpClient.initiate(request, isSandbox);

            // check response object
            if (response.getCode() != null) {
                return TemplateErrorHandler.findError(response);
            } else {
                // get the url to get
                URL redirectURL = new URL(response.getRedirectURL());
                //get a  object which contains the url to get redirection Builder
                PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder responseRedirectURL = PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder.aRedirectionRequest()
                        .withUrl(redirectURL);

                PaymentResponseRedirect.RedirectionRequest redirectionRequest = new PaymentResponseRedirect.RedirectionRequest(responseRedirectURL);
                Map<String, String> templateContext = new HashMap<>();
                templateContext.put(TemplateCardConstants.PSC_ID, response.getId());
                RequestContext requestContext = RequestContext.RequestContextBuilder.aRequestContext()
                        .withRequestData(templateContext)
                        .build();

                return PaymentResponseRedirect.PaymentResponseRedirectBuilder.aPaymentResponseRedirect()
                        .withRedirectionRequest(redirectionRequest)
                        .withPartnerTransactionId(response.getId())
                        .withStatusCode(response.getStatus())
                        .withRequestContext(requestContext)
                        .build();
            }

        } catch (IOException | URISyntaxException | InvalidRequestException e) {
            logger.error("unable init the payment: {}", e.getMessage(), e);
            return TemplateErrorHandler.getPaymentResponseFailure(FailureCause.INTERNAL_ERROR);
        }
    }
}
