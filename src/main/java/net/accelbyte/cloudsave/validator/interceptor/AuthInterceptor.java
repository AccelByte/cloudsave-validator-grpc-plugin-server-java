package net.accelbyte.cloudsave.validator.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.accelbyte.sdk.core.AccelByteSDK;

import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

@Slf4j
@GRpcGlobalInterceptor
@Order(20)
public class AuthInterceptor implements ServerInterceptor {
    private final int ACTION_READ_ONLY = 2;

    private AccelByteSDK sdk;
    private String namespace;
    private String resource;
    @Value("${plugin.grpc.server.interceptor.auth.enabled}")
    private boolean enabled;

    @Autowired
    public AuthInterceptor(AccelByteSDK sdk, @Value("${plugin.grpc.config.resource_name}") String resource,
            @Value("${plugin.grpc.config.namespace}") String namespace) {
        this.sdk = sdk;
        this.namespace = namespace;
        this.resource = resource;
        log.info("AuthInterceptor enabled: {})", enabled);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        if (enabled) {
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
            if (!sdk.validateToken(authToken, String.format("NAMESPACE:%s:%s", this.namespace, this.resource),
                    ACTION_READ_ONLY)) {
                log.error("Auth token validation failed");
                unAuthorizedCall(call, headers);
            }
        }
        return next.startCall(call, headers);
    }

    private <ReqT, RespT> void unAuthorizedCall(ServerCall<ReqT, RespT> call, Metadata headers) {
        call.close(Status.UNAUTHENTICATED.withDescription("Call not authorized"), headers);
    }
}
