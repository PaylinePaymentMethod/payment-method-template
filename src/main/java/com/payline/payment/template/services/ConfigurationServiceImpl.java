package com.payline.payment.template.services;

import com.payline.payment.template.bean.TemplatePaymentRequest;
import com.payline.payment.template.bean.TemplatePaymentResponse;
import com.payline.payment.template.utils.*;
import com.payline.pmapi.bean.configuration.ReleaseInformation;
import com.payline.pmapi.bean.configuration.parameter.AbstractParameter;
import com.payline.pmapi.bean.configuration.parameter.impl.InputParameter;
import com.payline.pmapi.bean.configuration.parameter.impl.ListBoxParameter;
import com.payline.pmapi.bean.configuration.parameter.impl.PasswordParameter;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.service.ConfigurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConfigurationServiceImpl implements ConfigurationService {
    private static final Logger LOGGER = LogManager.getLogger(ConfigurationServiceImpl.class);

    private TemplateHttpClient httpClient = new TemplateHttpClient();
    private final LocalizationService localization;

    public ConfigurationServiceImpl() {
        this.localization = LocalizationImpl.getInstance();
    }

    @Override
    public List<AbstractParameter> getParameters(Locale locale) {
        List<AbstractParameter> parameters = new ArrayList<>();

        // Merchant name
        final InputParameter merchantName = new InputParameter();
        merchantName.setKey(TemplateCardConstants.MERCHANT_NAME_KEY);
        merchantName.setLabel(localization.getSafeLocalizedString("contract.merchantName.label", locale));
        merchantName.setDescription(localization.getSafeLocalizedString("contract.merchantName.description", locale));
        merchantName.setRequired(true);

        parameters.add(merchantName);

        // Mid
        final InputParameter merchantId = new InputParameter();
        merchantId.setKey(TemplateCardConstants.MERCHANT_ID_KEY);
        merchantId.setLabel(localization.getSafeLocalizedString("contract.merchantId.label", locale));
        merchantId.setDescription(localization.getSafeLocalizedString("contract.merchantId.description", locale));
        merchantId.setRequired(true);

        parameters.add(merchantId);

        // authorisation key
        final PasswordParameter authorisationKey = new PasswordParameter();
        authorisationKey.setKey(TemplateCardConstants.AUTHORISATIONKEY_KEY);
        authorisationKey.setLabel(localization.getSafeLocalizedString("contract.authorisationKey.label", locale));
        authorisationKey.setDescription(localization.getSafeLocalizedString("contract.authorisationKey.description", locale));
        authorisationKey.setRequired(true);

        parameters.add(authorisationKey);

        //settlement key
        final PasswordParameter settlementKey = new PasswordParameter();
        settlementKey.setKey(TemplateCardConstants.SETTLEMENT_KEY);
        settlementKey.setLabel(localization.getSafeLocalizedString("contract.settlementKey.label", locale));
        settlementKey.setDescription(localization.getSafeLocalizedString("contract.settlementKey.description", locale));
        settlementKey.setRequired(false);

        parameters.add(settlementKey);

        // age limit
        final InputParameter minAge = new InputParameter();
        minAge.setKey(TemplateCardConstants.MINAGE_KEY);
        minAge.setLabel(localization.getSafeLocalizedString("contract.minAge.label", locale));
        minAge.setDescription(localization.getSafeLocalizedString("contract.minAge.description", locale));
        minAge.setRequired(false);

        parameters.add(minAge);

        // kyc level
        Map<String, String> kycLevelMap = new HashMap<>();
        kycLevelMap.put(TemplateCardConstants.KYCLEVEL_SIMPLE, localization.getSafeLocalizedString("contract.kycLevel.simple", locale));
        kycLevelMap.put(TemplateCardConstants.KYCLEVEL_FULL, localization.getSafeLocalizedString("contract.kycLevel.full", locale));

        final ListBoxParameter kycLevel = new ListBoxParameter();
        kycLevel.setKey(TemplateCardConstants.KYCLEVEL_KEY);
        kycLevel.setLabel(localization.getSafeLocalizedString("contract.kycLevel.label", locale));
        kycLevel.setDescription(localization.getSafeLocalizedString("contract.kycLevel.description", locale));
        kycLevel.setList(kycLevelMap);
        kycLevel.setRequired(false);

        parameters.add(kycLevel);

        // country restriction
        final InputParameter countryRestriction = new InputParameter();
        countryRestriction.setKey(TemplateCardConstants.COUNTRYRESTRICTION_KEY);
        countryRestriction.setLabel(localization.getSafeLocalizedString("contract.countryRestriction.label", locale));
        countryRestriction.setLabel(localization.getSafeLocalizedString("contract.countryRestriction.description", locale));
        countryRestriction.setRequired(false);

        parameters.add(countryRestriction);


        return parameters;
    }

    @Override
    public Map<String, String> check(ContractParametersCheckRequest contractParametersCheckRequest) {
        Map<String, String> errors = new HashMap<>();
        Locale locale = contractParametersCheckRequest.getLocale();

        // verify configuration fields
        String minAge = contractParametersCheckRequest.getContractConfiguration().getProperty(TemplateCardConstants.MINAGE_KEY).getValue();
        String countryRestriction = contractParametersCheckRequest.getContractConfiguration().getProperty(TemplateCardConstants.COUNTRYRESTRICTION_KEY).getValue();

        // verify fields
        try {
            DataChecker.verifyMinAge(minAge);
        } catch (BadFieldException e) {
            errors.put(e.getField(), localization.getSafeLocalizedString(e.getMessage(), locale));
        }

        try {
            DataChecker.verifyCountryRestriction(countryRestriction);
        } catch (BadFieldException e) {
            errors.put(e.getField(), localization.getSafeLocalizedString(e.getMessage(), locale));
        }

        // if there is some errors, stop the process and return them
        if (errors.size() > 0) {
            return errors;
        }

        try {
            // create a CheckRequest
            TemplatePaymentRequest checkRequest = new TemplatePaymentRequest(contractParametersCheckRequest);

            // do the request
            Boolean isSandbox = contractParametersCheckRequest.getEnvironment().isSandbox();
            TemplatePaymentResponse response = httpClient.initiate(checkRequest, isSandbox);

            // check response object
            if (response.getCode() != null) {
                findErrors(response, errors);
            }

        } catch (IOException | URISyntaxException | InvalidRequestException e) {
            LOGGER.error("unable to check the connection: {}", e.getMessage(), e);
            errors.put(ContractParametersCheckRequest.GENERIC_ERROR, e.getMessage());
        }

        return errors;
    }

    @Override
    public ReleaseInformation getReleaseInformation() {
        final Properties props = new Properties();
        try {
            props.load(ConfigurationServiceImpl.class.getClassLoader().getResourceAsStream("release.properties"));
        } catch (IOException e) {
            final String message = "An error occurred reading the file: release.properties";
            LOGGER.error(message);
            throw new RuntimeException(message, e);
        }

        final LocalDate date = LocalDate.parse(props.getProperty("release.date"), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return ReleaseInformation.ReleaseBuilder.aRelease()
                .withDate(date)
                .withVersion(props.getProperty("release.version"))
                .build();
    }

    @Override
    public String getName(Locale locale) {
        return localization.getSafeLocalizedString("project.name", locale);
    }

    public void findErrors(TemplatePaymentResponse message, Map<String, String> errors) {
        if (message.getCode() != null) {
            switch (message.getCode()) {
                case "invalid_api_key":
                    // bad authorisation key in header
                    errors.put(TemplateCardConstants.AUTHORISATIONKEY_KEY, message.getMessage());
                    break;
                case "invalid_request_parameter":
                    // bad parameter, check field "param" to find it
                    if ("kyc_level".equals(message.getParam())) {
                        errors.put(TemplateCardConstants.KYCLEVEL_KEY, message.getMessage());
                    } else if ("min_age".equals(message.getParam())) {
                        errors.put(TemplateCardConstants.MINAGE_KEY, message.getMessage());
                    }
                    break;
                case "invalid_restriction":
                    // bad country restriction value
                    errors.put(TemplateCardConstants.COUNTRYRESTRICTION_KEY, message.getMessage());
                    break;
                default:
                    errors.put(ContractParametersCheckRequest.GENERIC_ERROR, message.getMessage());
            }
        }
    }
}
