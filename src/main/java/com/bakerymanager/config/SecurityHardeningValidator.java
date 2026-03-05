package com.bakerymanager.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class SecurityHardeningValidator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHardeningValidator.class);

    @Value("${app.security.require-env-secrets:false}")
    private boolean requireEnvSecrets;

    @Value("${app.security.master-key:}")
    private String masterKey;

    @Value("${app.security.master-key-id:unset}")
    private String masterKeyId;

    @Value("${app.security.master-key-rotated-at:}")
    private String masterKeyRotatedAt;

    @Value("${app.security.max-key-age-days:90}")
    private long maxKeyAgeDays;

    @Value("${smartbill.api.token:}")
    private String smartBillToken;

    @PostConstruct
    public void validateSecurityPosture() {
        validateMasterKey();
        validateApiTokenPolicy();
        validateKeyRotationPolicy();
    }

    private void validateMasterKey() {
        if (masterKey == null || masterKey.isBlank()) {
            String msg = "APP master key is missing. Set APP_SECURITY_MASTER_KEY for sensitive-data crypto.";
            if (requireEnvSecrets) {
                throw new IllegalStateException(msg);
            }
            logger.warn(msg);
            return;
        }

        if (masterKey.length() < 32) {
            String msg = "APP_SECURITY_MASTER_KEY length is weak (<32 chars).";
            if (requireEnvSecrets) {
                throw new IllegalStateException(msg);
            }
            logger.warn(msg);
        }

        logger.info("Security key configured: keyId={}", masterKeyId);
    }

    private void validateApiTokenPolicy() {
        if (smartBillToken == null || smartBillToken.isBlank()) {
            String msg = "SmartBill token is missing from environment.";
            if (requireEnvSecrets) {
                throw new IllegalStateException(msg);
            }
            logger.warn(msg);
            return;
        }

        if (smartBillToken.toLowerCase().contains("your-smartbill-token") || smartBillToken.length() < 20) {
            String msg = "SmartBill token appears placeholder/weak. Rotate and set a valid production token.";
            if (requireEnvSecrets) {
                throw new IllegalStateException(msg);
            }
            logger.warn(msg);
        }
    }

    private void validateKeyRotationPolicy() {
        if (masterKeyRotatedAt == null || masterKeyRotatedAt.isBlank()) {
            logger.warn("APP_SECURITY_MASTER_KEY_ROTATED_AT not set. Key rotation tracking is disabled.");
            return;
        }

        try {
            LocalDate rotatedAt = LocalDate.parse(masterKeyRotatedAt);
            LocalDate expiryDate = rotatedAt.plusDays(maxKeyAgeDays);
            if (LocalDate.now().isAfter(expiryDate)) {
                String msg = "Master key rotation overdue. Rotate APP_SECURITY_MASTER_KEY (keyId=" + masterKeyId + ").";
                if (requireEnvSecrets) {
                    throw new IllegalStateException(msg);
                }
                logger.warn(msg);
            }
        } catch (DateTimeParseException ex) {
            String msg = "Invalid APP_SECURITY_MASTER_KEY_ROTATED_AT format. Use YYYY-MM-DD.";
            if (requireEnvSecrets) {
                throw new IllegalStateException(msg);
            }
            logger.warn(msg);
        }
    }
}
