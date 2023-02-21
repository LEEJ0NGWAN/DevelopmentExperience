@Configuration
public class Config {
  
    // .. override object mapper setting

    @Bean
    Jackson2JsonEncoder jackson2JsonEncoder(ObjectMapper mapper){

       return new Jackson2JsonEncoder(mapper);
    }

    @Bean
    Jackson2JsonDecoder jackson2JsonDecoder(ObjectMapper mapper){

        return new Jackson2JsonDecoder(mapper);
    }

    @Bean
    WebFluxConfigurer webFluxConfigurer(Jackson2JsonEncoder encoder, Jackson2JsonDecoder decoder){

        return new WebFluxConfigurer() {

            @Override
            public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {

                configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                configurer.defaultCodecs().jackson2JsonDecoder(decoder);
            }
        };
    }
}
