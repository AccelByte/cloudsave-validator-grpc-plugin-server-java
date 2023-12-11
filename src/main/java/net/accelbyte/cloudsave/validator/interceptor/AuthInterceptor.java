package net.accelbyte.cloudsave.validator.interceptor;

import io.grpc.*;
import io.grpc.health.v1.HealthGrpc;
import lombok.extern.slf4j.Slf4j;
import net.accelbyte.sdk.core.AccelByteSDK;
import net.accelbyte.sdk.core.AccessTokenPayload;
import org.apache.logging.log4j.util.Strings;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

import java.util.Objects;

@Slf4j
@GRpcGlobalInterceptor
@Order(20)
public class AuthInterceptor implements ServerInterceptor {

    private AccelByteSDK sdk;

    private String namespace;

    @Value("${plugin.grpc.server.interceptor.auth.enabled}")
    private boolean enabled;

    @Autowired
    public AuthInterceptor(AccelByteSDK sdk,
                           @Value("${app.namespace}") String namespace) {
        this.sdk = sdk;
        this.namespace = namespace;
        log.info("AuthInterceptor enabled: {})", enabled);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        if (enabled) {
            if (Objects.equals(call.getMethodDescriptor().getServiceName(), HealthGrpc.SERVICE_NAME)) {
                return next.startCall(call, headers); // skip validation if health check
            }
            final String authHeader = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
            if (authHeader == null) {
                log.error("Auth header is null");
                unAuthorizedCall(call, headers);
            }
            final String[] authTypeToken = authHeader.split(" ");
            if (authTypeToken.length != 2) {
                log.error("Auth header format is invalid");
                unAuthorizedCall(call, headers);
            }
            final String authToken = authTypeToken[1];
            final AccessTokenPayload tokenPayload = sdk.parseAccessToken(authToken, true);
            if (tokenPayload == null) {
                log.error("Auth token validation failed");
                unAuthorizedCall(call, headers);
            }
            if (Strings.isBlank(tokenPayload.getExtendNamespace())
                || !Objects.equals(tokenPayload.getExtendNamespace(), namespace)) {
                log.error("Invalid extend namespace");
                unAuthorizedCall(call, headers);
            }
        }
        return next.startCall(call, headers);
    }

    private <ReqT, RespT> void unAuthorizedCall(ServerCall<ReqT, RespT> call, Metadata headers) {
        call.close(Status.UNAUTHENTICATED.withDescription("Call not authorized"), headers);
    }
}
