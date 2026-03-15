package com.mipt.team4.cloud_storage_backend.netty.handlers.auth;

import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class JwtAuthHandler {
  private final JwtService jwtService;

  private
}
