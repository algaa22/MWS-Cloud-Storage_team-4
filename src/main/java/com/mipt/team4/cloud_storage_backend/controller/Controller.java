package com.mipt.team4.cloud_storage_backend.controller;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;

public abstract class Controller {
  protected abstract FullHttpResponse handleRequest(FullHttpRequest request);
  protected abstract FullHttpResponse handleGet(FullHttpRequest request, String url);
  protected abstract FullHttpResponse handlePost(FullHttpRequest request, String url);
  protected abstract FullHttpResponse handleUpdate(FullHttpRequest request, String url);
}
