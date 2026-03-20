package com.mipt.team4.cloud_storage_backend.controller.share;

import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.share.dto.*;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.share.ShareService;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ShareController {
  private final ShareService shareService;

  public void createShare(ChannelHandlerContext ctx, CreateShareRequest request) {
    ResponseUtils.send(ctx, shareService.createShare(request.userId(), request));
  }

  public void getShareInfo(ChannelHandlerContext ctx, GetShareInfoRequest request) {
    ResponseUtils.send(ctx, shareService.getShareInfo(request.token()));
  }

  public void downloadSharedFile(ChannelHandlerContext ctx, PublicShareRequest request) {
    ShareDownloadInfo downloadInfo =
        shareService.prepareDownload(request.token(), request.sharePassword());
    ResponseUtils.sendFile(
        ctx, downloadInfo.data(), downloadInfo.fileName(), downloadInfo.mimeType());
  }

  public void validatePassword(ChannelHandlerContext ctx, ValidatePasswordRequest request) {
    ResponseUtils.send(ctx, shareService.validatePassword(request));
  }

  public void deactivateShare(ChannelHandlerContext ctx, DeactivateShareRequest request) {
    shareService.deactivateShare(request.shareId(), request.userId());
    ResponseUtils.send(ctx, new SuccessResponse("Share deactivated successfully"));
  }

  public void getUserShares(ChannelHandlerContext ctx, GetUserSharesRequest request) {
    ResponseUtils.send(ctx, shareService.getUserSharesInfo(request.userId()));
  }

  public void getFileShares(ChannelHandlerContext ctx, GetFileSharesRequest request) {
    ResponseUtils.send(ctx, shareService.getFileSharesInfo(request.fileId(), request.userId()));
  }
}
