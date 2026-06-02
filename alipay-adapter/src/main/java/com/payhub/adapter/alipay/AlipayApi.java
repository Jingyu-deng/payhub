package com.payhub.adapter.alipay;

import com.payhub.core.http.Body;
import com.payhub.core.http.Get;
import com.payhub.core.http.Header;
import com.payhub.core.http.Post;
import com.payhub.core.http.Query;
import com.payhub.core.infra.HttpClient;

/** Declarative HTTP API for Alipay payment gateway. */
@Header(name = "Content-Type", value = "application/json")
interface AlipayApi {

  @Post("/gateway.do")
  HttpClient.Response processPayment(@Body Object request);

  @Post("/gateway.do")
  HttpClient.Response refund(@Body Object request);

  @Get("/gateway.do")
  HttpClient.Response checkPaymentStatus(@Query("transactionId") String transactionId);
}
