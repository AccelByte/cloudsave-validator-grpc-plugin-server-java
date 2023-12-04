package net.accelbyte.cloudsave.validator;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import net.accelbyte.sdk.core.AccelByteSDK;

@Profile("test")
@Configuration
public class AppTestConfiguration {

    @Bean
    @Primary
    public AccelByteSDK testSdkProvider() {
        return Mockito.mock(AccelByteSDK.class);
    }
}