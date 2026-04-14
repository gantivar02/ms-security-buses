package com.AJJ.ms_security.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret.key}")
    private String recaptchaSecretKey;

    @Value("${recaptcha.verify.url}")
    private String recaptchaVerifyUrl;

    @Value("${recaptcha.min.score:0.5}")
    private double recaptchaMinScore;

    @Value("${recaptcha.expected.hostname:}")
    private String expectedHostname;

    public boolean verifyToken(String token, String expectedAction) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("secret", this.recaptchaSecretKey);
            requestBody.add("response", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
            Map response = restTemplate.postForObject(this.recaptchaVerifyUrl, request, Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return false;
            }

            String responseAction = response.get("action") != null
                    ? String.valueOf(response.get("action")).trim()
                    : "";
            String responseHostname = response.get("hostname") != null
                    ? String.valueOf(response.get("hostname")).trim()
                    : "";

            if (expectedAction == null || expectedAction.isBlank() || !expectedAction.equals(responseAction)) {
                return false;
            }

            if (this.expectedHostname != null
                    && !this.expectedHostname.isBlank()
                    && !this.expectedHostname.equalsIgnoreCase(responseHostname)) {
                return false;
            }

            return this.getScore(response) >= this.recaptchaMinScore;
        } catch (Exception e) {
            System.out.println("Error verificando reCAPTCHA: " + e.getMessage());
            return false;
        }
    }

    private double getScore(Map response) {
        Object score = response.get("score");
        if (score instanceof Number number) {
            return number.doubleValue();
        }

        if (score == null) {
            return 0;
        }

        try {
            return Double.parseDouble(String.valueOf(score));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
