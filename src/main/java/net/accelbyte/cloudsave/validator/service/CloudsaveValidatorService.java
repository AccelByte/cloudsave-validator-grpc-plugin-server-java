package net.accelbyte.cloudsave.validator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import net.accelbyte.cloudsave.validator.Error;
import net.accelbyte.cloudsave.validator.*;
import net.accelbyte.cloudsave.validator.model.CustomGameRecord;
import net.accelbyte.cloudsave.validator.model.CustomPlayerRecord;
import net.accelbyte.cloudsave.validator.model.DailyMessage;
import net.accelbyte.cloudsave.validator.model.PlayerActivity;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@GRpcService
public class CloudsaveValidatorService extends CloudsaveValidatorServiceGrpc.CloudsaveValidatorServiceImplBase {

    private final ObjectMapper objectMapper;

    private final Validator validator;

    private final OkHttpClient binaryCheckClient;

    private final long maxSizeForEventBannerInKB = 100;

    public CloudsaveValidatorService(
        @Autowired ObjectMapper objectMapper,
        @Autowired Validator validator
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        binaryCheckClient = new OkHttpClient();
    }

    @Override
    public void beforeWriteGameRecord(GameRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (request.getKey().endsWith("map")) {
            CustomGameRecord record;
            try {
                record = objectMapper.readValue(request.getPayload().toStringUtf8(), CustomGameRecord.class);
            } catch (Exception e) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }
            var res = record.validate(validator);
            if (!res.isEmpty()) {
                String invalidFields = res.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining(";"));
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(invalidFields))
                        .build();

                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }
        }

        responseGameRecordSuccess(responseObserver, request.getKey());
    }

    @Override
    public void afterReadGameRecord(GameRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (request.getKey().endsWith("daily_msg")) {
            DailyMessage message;
            try {
                message = objectMapper.readValue(request.getPayload().toStringUtf8(), DailyMessage.class);
            } catch (Exception e) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }
            if (message.getAvailableOn().isBefore(Instant.now())) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(2)
                        .setErrorMessage("not accessible yet")
                        .build();

                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }

        }

        responseGameRecordSuccess(responseObserver, request.getKey());
    }

    @Override
    public void afterBulkReadGameRecord(BulkGameRecord request, StreamObserver<BulkGameRecordValidationResult> responseObserver) {
        var result = request.getGameRecordsList().stream().map(it ->  {
            var success = GameRecordValidationResult.newBuilder()
                    .setIsSuccess(true)
                    .setKey(it.getKey())
                    .build();

            if (!it.getKey().endsWith("daily_msg")) {
                return success;
            }

            DailyMessage message;
            try {
                message = objectMapper.readValue(it.getPayload().toStringUtf8(), DailyMessage.class);
            } catch (Exception e) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                        .build();
                return GameRecordValidationResult.newBuilder()
                        .setIsSuccess(false)
                        .setKey(it.getKey())
                        .setError(errorDetail)
                        .build();
            }
            if (!message.getAvailableOn().isBefore(Instant.now())) {
                return success;
            }

            // failed
            var errorDetail = Error.newBuilder()
                    .setErrorCode(2)
                    .setErrorMessage("not accessible yet")
                    .build();

            return GameRecordValidationResult.newBuilder()
                    .setIsSuccess(false)
                    .setKey(it.getKey())
                    .setError(errorDetail)
                    .build();
        }).toList();

        var response = BulkGameRecordValidationResult.newBuilder()
                .addAllValidationResults(result)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void beforeWritePlayerRecord(PlayerRecord request,
            StreamObserver<PlayerRecordValidationResult> responseObserver) {
        if (request.getKey().endsWith("favourite_weapon")) {
            try {
                CustomPlayerRecord record;
                try {
                    record = objectMapper.readValue(request.getPayload().toStringUtf8(), CustomPlayerRecord.class);
                } catch (Exception e) {
                    var errorDetail = Error.newBuilder()
                            .setErrorCode(1)
                            .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                            .build();
                    responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), errorDetail);
                    return;
                }
                var result = record.validate(validator);
                if (!result.isEmpty()) {
                    var failedValidations = result.stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining(";"));
                    var error = Error.newBuilder()
                            .setErrorCode(1)
                            .setErrorMessage("Validation failed: [%s]".formatted(failedValidations))
                            .build();
                    responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), error);
                    return;
                }
            } catch (Exception ex) {
                var error = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed")
                        .build();
                responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), error);
                return;
            }
        }
        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    @Override
    public void afterReadPlayerRecord(PlayerRecord request, StreamObserver<PlayerRecordValidationResult> responseObserver) {
        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    @Override
    public void afterBulkReadPlayerRecord(BulkPlayerRecord request, StreamObserver<BulkPlayerRecordValidationResult> responseObserver) {
        var result = request.getPlayerRecordsList().stream()
                .map(it -> PlayerRecordValidationResult.newBuilder()
                        .setIsSuccess(true)
                        .setKey(it.getKey())
                        .setUserId(it.getUserId())
                        .build()
                ).toList();

        var response = BulkPlayerRecordValidationResult.newBuilder()
                .addAllValidationResults(result)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void beforeWriteAdminGameRecord(AdminGameRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (!request.getKey().endsWith("map")) {
            responseGameRecordSuccess(responseObserver, request.getKey());
            return;
        }

        CustomGameRecord record;
        try {
            record = objectMapper.readValue(request.getPayload().toStringUtf8(), CustomGameRecord.class);
        } catch (Exception e) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                    .build();
            responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
            return;
        }
        var res = record.validate(validator);
        if (!res.isEmpty()) {
            String invalidFields = res.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(";"));
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(invalidFields))
                    .build();

            responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
            return;
        }

        responseGameRecordSuccess(responseObserver, request.getKey());

    }

    @Override
    public void beforeWriteAdminPlayerRecord(AdminPlayerRecord request, StreamObserver<PlayerRecordValidationResult> responseObserver) {
        if (!request.getKey().endsWith("player_activity")) {
            responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
            return;
        }

        PlayerActivity activity;
        try {
            activity = objectMapper.readValue(request.getPayload().toStringUtf8(), PlayerActivity.class);
        } catch (Exception e) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                    .build();
            responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), errorDetail);
            return;
        }
        var res = activity.validate(validator);

        if (!res.isEmpty()) {
            String invalidFields = res.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(";"));
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(invalidFields))
                    .build();
            responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), errorDetail);
            return;
        }

        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    private void responseGameRecordSuccess(StreamObserver<GameRecordValidationResult> responseObserver, String key) {
        var response =  GameRecordValidationResult.newBuilder()
                .setIsSuccess(true)
                .setKey(key)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    private void responseGameRecordFailed(StreamObserver<GameRecordValidationResult> responseObserver, String key, Error errorDetail) {
        var response = GameRecordValidationResult.newBuilder()
                .setIsSuccess(false)
                .setKey(key)
                .setError(errorDetail)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void responsePlayerRecordFailed(
            StreamObserver<PlayerRecordValidationResult> responseObserver, String key, String userId, Error errorDetail
    ) {
        var response = PlayerRecordValidationResult.newBuilder()
                .setIsSuccess(false)
                .setKey(key)
                .setUserId(userId)
                .setError(errorDetail)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void responsePlayerRecordSuccess(
            StreamObserver<PlayerRecordValidationResult> responseObserver, String key, String userId
    ) {
        var response = PlayerRecordValidationResult.newBuilder()
                .setIsSuccess(true)
                .setKey(key)
                .setUserId(userId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void beforeWriteGameBinaryRecord(GameBinaryRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (!request.getKey().endsWith("event_banner")) {
            responseGameRecordSuccess(responseObserver, request.getKey());
            return;
        }

        final Request checkRequest = new Request.Builder()
                .url(request.getBinaryInfo().getUrl())
                .get()
                .build();
        try (Response checkResponse = binaryCheckClient.newCall(checkRequest).execute()) {
            if (!checkResponse.isSuccessful()) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Failed to check binary url")
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }

            if (checkResponse.body() == null) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("No body in response")
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }

            final long contentLengthInKB = checkResponse.body().contentLength() / 1000;
            if (contentLengthInKB > maxSizeForEventBannerInKB) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("maximum size for event banner is [%d] kB".formatted(maxSizeForEventBannerInKB))
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }

            responseGameRecordSuccess(responseObserver, request.getKey());

        } catch (IOException e) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
        }
    }

    @Override
    public void afterReadGameBinaryRecord(GameBinaryRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (!request.getKey().endsWith("daily_event_stage")) {
            responseGameRecordSuccess(responseObserver, request.getKey());
            return;
        }

        Timestamp tsUpdatedAt = request.getBinaryInfo().getUpdatedAt();
        Instant updatedAt = Instant.ofEpochSecond(tsUpdatedAt.getSeconds(),tsUpdatedAt.getNanos());

        LocalDate ld1 = LocalDateTime.ofInstant(updatedAt, ZoneId.systemDefault()).toLocalDate();
        LocalDate ld2 = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate();
        if (!ld1.isEqual(ld2)) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Today's %s is not ready yet".formatted(request.getKey()))
                    .build();
            responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
        }

        responseGameRecordSuccess(responseObserver, request.getKey());
    }

    @Override
    public void afterBulkReadGameBinaryRecord(BulkGameBinaryRecord request, StreamObserver<BulkGameRecordValidationResult> responseObserver) {
        var result = request.getGameBinaryRecordsList().stream().map(it -> {
            if (!it.getKey().endsWith("daily_event_stage")) {
                return GameRecordValidationResult.newBuilder().setIsSuccess(true).setKey(it.getKey()).build();
            }

            Timestamp tsUpdatedAt = it.getBinaryInfo().getUpdatedAt();
            Instant updatedAt = Instant.ofEpochSecond(tsUpdatedAt.getSeconds(),tsUpdatedAt.getNanos());

            LocalDate ld1 = LocalDateTime.ofInstant(updatedAt, ZoneId.systemDefault()).toLocalDate();
            LocalDate ld2 = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate();
            if (!ld1.isEqual(ld2)) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Today's %s is not ready yet".formatted(it.getKey()))
                        .build();
                return GameRecordValidationResult.newBuilder().setIsSuccess(false).setKey(it.getKey()).setError(errorDetail).build();
            }

            return GameRecordValidationResult.newBuilder().setIsSuccess(true).setKey(it.getKey()).build();
        }).toList();

        var response = BulkGameRecordValidationResult.newBuilder().addAllValidationResults(result).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void beforeWritePlayerBinaryRecord(PlayerBinaryRecord request, StreamObserver<PlayerRecordValidationResult> responseObserver) {
        if (!request.getKey().endsWith("id_card")) {
            responsePlayerRecordSuccess(responseObserver, request.getKey(),request.getUserId());
            return;
        }

        if (request.getBinaryInfo().getVersion() > 1) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("id card can only be created once")
                    .build();
            responsePlayerRecordFailed(responseObserver,request.getKey(),request.getUserId(),errorDetail);
        }

        responsePlayerRecordSuccess(responseObserver, request.getKey(),request.getUserId());
    }

    @Override
    public void afterReadPlayerBinaryRecord(PlayerBinaryRecord request, StreamObserver<PlayerRecordValidationResult> responseObserver) {
        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    @Override
    public void afterBulkReadPlayerBinaryRecord(BulkPlayerBinaryRecord request, StreamObserver<BulkPlayerRecordValidationResult> responseObserver) {
        var result = request.getPlayerBinaryRecordsList().stream()
                .map(it -> PlayerRecordValidationResult.newBuilder()
                        .setIsSuccess(true)
                        .setKey(it.getKey())
                        .setUserId(it.getUserId())
                        .build()
                ).toList();

        var response = BulkPlayerRecordValidationResult.newBuilder()
                .addAllValidationResults(result)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
