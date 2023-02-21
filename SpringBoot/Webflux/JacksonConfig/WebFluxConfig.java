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
