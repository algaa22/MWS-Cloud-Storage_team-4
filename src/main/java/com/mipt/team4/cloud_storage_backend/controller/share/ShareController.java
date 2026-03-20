package com.mipt.team4.cloud_storage_backend.controller.share;

import com.mipt.team4.cloud_storage_backend.exception.share.InvalidSharePasswordException;
import com.mipt.team4.cloud_storage_backend.exception.share.SharePasswordRequiredException;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.share.dto.*;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.share.ShareService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    public void createShare(ChannelHandlerContext ctx, CreateShareRequest request) {
        FileShare share = shareService.createShare(request.userId(), request);
        ShareCreatedResponse response = ShareCreatedResponse.fromShare(share, baseUrl);
        ResponseUtils.send(ctx, response);
    }

    public void getShareInfo(ChannelHandlerContext ctx, GetShareInfoRequest request) {
        FileShare share = shareService.validateShare(request.token(), null);
        ShareInfoResponse response = ShareInfoResponse.fromShare(share, baseUrl);
        ResponseUtils.send(ctx, response);
    }

    public void downloadSharedFile(ChannelHandlerContext ctx, PublicShareRequest request,
                                   HttpHeaders headers) {
        log.info("=== DOWNLOAD SHARED FILE CONTROLLER ===");
        log.info("Token: {}", request.token());
        log.info("Headers: {}", headers);

        log.info("Controller method invoked!");

        try {
            String password = extractPassword(headers);
            log.info("Password present: {}", password != null);

            ShareDownloadInfo downloadInfo = shareService.prepareDownload(request.token(), password);
            log.info("File size: {} bytes", downloadInfo.fileSize());
            log.info("File name: {}", downloadInfo.fileName());
            log.info("MIME type: {}", downloadInfo.mimeType());
            log.info("Data size: {} bytes", downloadInfo.data() != null ? downloadInfo.data().length : 0);

            if (downloadInfo.data() == null || downloadInfo.data().length == 0) {
                log.error("File data is empty!");
                ResponseUtils.sendError(ctx, HttpResponseStatus.NO_CONTENT, "File is empty");
                return;
            }

            log.info("Sending file via ResponseUtils.sendFile...");
            ResponseUtils.sendFile(ctx,
                    downloadInfo.data(),
                    downloadInfo.fileName(),
                    downloadInfo.mimeType());

            log.info("File sent successfully");

        } catch (SharePasswordRequiredException e) {
            log.warn("Password required for share: {}", request.token());
            ResponseUtils.sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "Password required");
        } catch (InvalidSharePasswordException e) {
            log.warn("Invalid password for share: {}", request.token());
            ResponseUtils.sendError(ctx, HttpResponseStatus.FORBIDDEN, "Invalid password");
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            ResponseUtils.sendError(ctx, HttpResponseStatus.NOT_FOUND, "Share not found: " + e.getMessage());
        }
    }

    public void validatePassword(ChannelHandlerContext ctx, ValidatePasswordRequest request) {
        try {
            ShareDownloadInfo downloadInfo = shareService.prepareDownload(request.shareToken(), request.password());
            ShareDownloadInfo infoWithoutData = ShareDownloadInfo.builder()
                    .fileName(downloadInfo.fileName())
                    .mimeType(downloadInfo.mimeType())
                    .fileSize(downloadInfo.fileSize())
                    .requiresPassword(false)
                    .shareToken(downloadInfo.shareToken())
                    .build();

            ResponseUtils.send(ctx, infoWithoutData);
        } catch (InvalidSharePasswordException e) {
            ResponseUtils.sendError(ctx, HttpResponseStatus.FORBIDDEN, "Invalid password");
        } catch (SharePasswordRequiredException e) {
            ResponseUtils.sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "Password required");
        }
    }

    public void deactivateShare(ChannelHandlerContext ctx, DeactivateShareRequest request) {
        shareService.deactivateShare(request.shareId(), request.userId());
        ResponseUtils.send(ctx, new SuccessResponse("Share deactivated successfully"));
    }

    public void getUserShares(ChannelHandlerContext ctx, GetUserSharesRequest request) {
        List<ShareInfoResponse> shares = shareService.getUserShares(request.userId())
                .stream()
                .map(share -> ShareInfoResponse.fromShare(share, baseUrl))
                .toList();
        ResponseUtils.send(ctx, shares);
    }

    public void getFileShares(ChannelHandlerContext ctx, GetFileSharesRequest request) {
        List<ShareInfoResponse> shares = shareService.getFileShares(request.fileId(), request.userId())
                .stream()
                .map(share -> ShareInfoResponse.fromShare(share, baseUrl))
                .toList();
        ResponseUtils.send(ctx, shares);
    }

    private String extractPassword(HttpHeaders headers) {
        String cookieHeader = headers.get(io.netty.handler.codec.http.HttpHeaderNames.COOKIE);
        if (cookieHeader != null) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
            return cookies.stream()
                    .filter(c -> c.name().startsWith("share_"))
                    .findFirst()
                    .map(Cookie::value)
                    .orElse(null);
        }
        return headers.get("X-Share-Password");
    }
}