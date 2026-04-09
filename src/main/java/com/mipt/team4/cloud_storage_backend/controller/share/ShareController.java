package com.mipt.team4.cloud_storage_backend.controller.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.share.dto.*;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.share.ShareService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
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
    List<ShareInfoResponse> shares = shareService.getUserSharesInfo(request.userId());

    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.valueToTree(new SharesListResponse(shares));

    ResponseUtils.sendJson(ctx, HttpResponseStatus.OK, json);
  }

  public void getFileShares(ChannelHandlerContext ctx, GetFileSharesRequest request) {
    List<ShareInfoResponse> shares =
        shareService.getFileSharesInfo(request.fileId(), request.userId());
    ResponseUtils.send(ctx, new SharesListResponse(shares));
  }

  public void deleteSharePermanently(ChannelHandlerContext ctx, DeleteShareRequest request) {
    shareService.deleteSharePermanently(request.shareId(), request.userId());
    ResponseUtils.send(ctx, new SuccessResponse("Share permanently deleted"));
  }
}
