package com.payhub.core.infra;

/** Port for retrieving secrets per gateway — API keys, signing material, webhook secrets. */
public interface SecretsClient {

  String getApiKey(String gatewayName);

  String getSigningSecret(String gatewayName);

  String getWebhookSecret(String gatewayName);
}
