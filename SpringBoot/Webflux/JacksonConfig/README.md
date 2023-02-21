# Webflux Jackson Config

스프링 MVC와 다르게, 기본적으로 웹플럭스는 별도 추가 Jackson2JsonEncoder/Decoder 등록 및 오버라이드 과정이 필요합니다

## 웹플럭스 설정을 변경하기 위한 Configuration 클래스가 WebFluxConfigurer 를 상속할 경우

Objectmapper빈을 스프링 컨테이너로부터 주입받도록 설정해줍니다

```
@Configuration
@RequiredArgsConstructor
public class WebFluxConfig implements WebFluxConfigurer {

    private final ObjectMapper objectMapper;

    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(objectMapper)
        );

        configurer.defaultCodecs().jackson2JsonDecoder(
            new Jackson2JsonDecoder(objectMapper)
        );
    }
}
```

## WebFluxConfigurer 를 상속하지 않는 경우

직접 Encode/Decoder, WebFluxConfigurer 상속 익명 객체를 생성하고, 해당 인코더 디코더빈을 익명객체에 등록할 수 있도록 수기로 구성해줍니다

```
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
```
