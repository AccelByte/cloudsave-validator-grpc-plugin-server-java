package net.accelbyte.cloudsave.validator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.validation.Validation;
import net.accelbyte.cloudsave.validator.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockitoAnnotations;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CloudsaveValidatorServiceTest {

    private static CloudsaveValidatorService service;
    private static ManagedChannel channel;

    @BeforeAll
    public static void setup() throws Exception {
        MockitoAnnotations.openMocks(CloudsaveValidatorServiceTest.class);
        var objectMapper = new ObjectMapper();
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new CloudsaveValidatorService(objectMapper, validator);

        String serverName = InProcessServerBuilder.generateName();
        InProcessServerBuilder.forName(serverName).directExecutor().addService(service).build().start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (channel != null) {
            try {
                channel.shutdownNow();
                assert channel.awaitTermination(5, TimeUnit.SECONDS) : "channel cannot be gracefully shutdown";
            } finally {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void testBeforeWriteGameRecord_Success() {
        // Given
        var request = GameRecord.newBuilder()
                .setKey("map")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "name": "test name",
                        "locationId": "test location",
                        "totalResource": 1,
                        "totalEnemy": 0
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        StreamObserver<GameRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.beforeWriteGameRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(GameRecordValidationResult::getIsSuccess));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAfterWriteGameRecord_Success() {
        // Given
        var request = GameRecord.newBuilder()
                .setKey("map")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "name": "test name",
                        "locationId": "test location",
                        "totalResource": 1,
                        "totalEnemy": 0
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        StreamObserver<GameRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.afterReadGameRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(GameRecordValidationResult::getIsSuccess));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAfterBulkReadGameRecord_Success() {
        // Given
        var gameRecord1 = GameRecord.newBuilder().setPayload(ByteString.copyFrom(
                """ 
                {
                "name": "test name",
                "locationId": "test location",
                "totalResource": 1,
                "totalEnemy": 0
                }
                """.trim(), Charset.defaultCharset()));
        var request = BulkGameRecord.newBuilder()
                .addGameRecords(gameRecord1)
                .build();
        StreamObserver<BulkGameRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.afterBulkReadGameRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(results ->
                results.getValidationResultsList().stream().allMatch(GameRecordValidationResult::getIsSuccess)));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testBeforeWritePlayerRecord_Success() {
        // Given
        var request = PlayerRecord.newBuilder()
                .setKey("favourite_weapon")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "userId": "test-user-id",
                        "favouriteWeaponType": "test-type",
                        "favouriteWeapon": "test-weapon"
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        StreamObserver<PlayerRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.beforeWritePlayerRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(PlayerRecordValidationResult::getIsSuccess));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAfterWritePlayerRecord_Success() {
        // Given
        var request = PlayerRecord.newBuilder()
                .setKey("favourite_weapon")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "userId": "test-user-id",
                        "favouriteWeaponType": "test-type",
                        "favouriteWeapon": "test-weapon"
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        StreamObserver<PlayerRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.afterReadPlayerRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(PlayerRecordValidationResult::getIsSuccess));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAfterBulkReadPlayerRecord_Success() {
        // Given
        var record1 = PlayerRecord.newBuilder()
                .setKey("favourite_weapon")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "userId": "test-user-id",
                        "favouriteWeaponType": "test-type",
                        "favouriteWeapon": "test-weapon"
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        var request = BulkPlayerRecord.newBuilder()
                .addPlayerRecords(record1)
                .build();
        StreamObserver<BulkPlayerRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.afterBulkReadPlayerRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(results ->
                results.getValidationResultsList().stream().allMatch(PlayerRecordValidationResult::getIsSuccess)));

        verify(responseObserver).onCompleted();
    }

    @Test
    public void testBeforeWriteAdminGameRecord_Success() {
        // Given
        var request = AdminGameRecord.newBuilder()
                .setKey("map")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "name": "test name",
                        "locationId": "test location",
                        "totalResource": 1,
                        "totalEnemy": 0
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        StreamObserver<GameRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.beforeWriteAdminGameRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(GameRecordValidationResult::getIsSuccess));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAfterWriteAdminGameRecord_Success() {
        // Given
        var request = AdminPlayerRecord.newBuilder()
                .setKey("favourite_weapon")
                .setPayload(ByteString.copyFrom(
                        """ 
                        {
                        "userId": "test-user-id",
                        "favouriteWeaponType": "test-type",
                        "favouriteWeapon": "test-weapon"
                        }
                        """.trim(), Charset.defaultCharset()))
                .build();
        StreamObserver<PlayerRecordValidationResult> responseObserver = mock(StreamObserver.class);

        // When
        service.beforeWriteAdminPlayerRecord(request, responseObserver);

        // Then
        verify(responseObserver).onNext(argThat(PlayerRecordValidationResult::getIsSuccess));
        verify(responseObserver).onCompleted();
    }
}
