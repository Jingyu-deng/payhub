package com.payhub.adapter.wechat;

import com.payhub.core.http.Body;
import com.payhub.core.http.Get;
import com.payhub.core.http.Header;
import com.payhub.core.http.Post;
import com.payhub.core.http.Query;
import com.payhub.core.infra.HttpClient;

/** Declarative HTTP API for WeChat Pay payment gateway. */
@Header(name = "Content-Type", value = "application/json")
interface WechatPayApi {

  @Post("/pay/unifiedorder")
  HttpClient.Response processPayment(@Body Object request);

  @Post("/secapi/pay/refund")
  HttpClient.Response refund(@Body Object request);

  @Get("/pay/orderquery")
  HttpClient.Response checkPaymentStatus(@Query("transactionId") String transactionId);
}
