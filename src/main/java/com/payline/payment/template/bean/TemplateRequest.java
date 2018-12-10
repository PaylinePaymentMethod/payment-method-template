package com.payline.payment.template.bean;

import com.payline.payment.template.utils.InvalidRequestException;
import com.payline.payment.template.utils.TemplateCardConstants;
import com.payline.pmapi.bean.payment.ContractConfiguration;

import java.util.Base64;

public abstract class TemplateRequest {
    private transient String authenticationHeader;

    TemplateRequest(ContractConfiguration configuration) throws InvalidRequestException {
        if (configuration == null || configuration.getProperty(TemplateCardConstants.AUTHORISATIONKEY_KEY).getValue() == null) {
            throw new InvalidRequestException("TemplateRequest must have an authorisation key when created");
        } else {
            this.authenticationHeader = "Basic " + encodeToBase64(configuration.getProperty(TemplateCardConstants.AUTHORISATIONKEY_KEY).getValue());
        }
    }

    public String getAuthenticationHeader() {
        return authenticationHeader;
    }

    public static String encodeToBase64(String toEncode) {
        if (toEncode == null) toEncode = "";
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }
}
