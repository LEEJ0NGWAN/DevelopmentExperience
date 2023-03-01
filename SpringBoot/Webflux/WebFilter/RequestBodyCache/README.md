# RequestBodyCache
Webflux의 경우, request body를 한번 emit (bodyToMono or bodyToFlux)하고 난 후에는 재사용 할 수 없습니다

레거시와 연계 혹은 프록시 응용, 히스토리 로깅 등 다양한 방면에서 리퀘스트 바디를 여러번 재사용 하기 위해 캐싱을 구현합니다

ServerRequest 객체의 (내부 exchange의) attributes에 리퀘스트 바디를 저장해놓는 웹 필터를 구현합니다

## usecase

ServerRequest를 인자로 다루는 웹플럭스 내부 곳에서든 자유롭게 사용 가능합니다

### RequestBroker

요청 리퀘스트를 외부로 전달하는 경우

```
@Component
@RequiredArgsConstructor
public class RequestBroker {

    private final WebClient webClient;

    public Mono<ServerResponse> handle(final ServerRequest serverRequest) {

        return Mono
        .justOrEmpty(serverRequest)
        .flatMap(
            r -> webClient
            .method(r.method())
            .uri( external URI )
            .headers($->$.putAll(r.headers().asHttpHeaders())
            .body((String) serverRequest.attribute("cachedRequestBody").orElse(""))
            .exchangeToMono(response -> response.toEntity(String.class)))
        .flatMap(response -> ServerResponse.status(response.getStatusCode()).bodyValue(response.getBody()));
    }
```

### RequestLogger

요청 리퀘스트의 정보를 로깅하는 핸들러

```
@Component
@RequiredArgsConstructor
public class RequestLogger {

    private final Repository repository;
    ...

    @Async // R2DBC가 아닌 동기식 레거시 JDBC의 비동기 처리를 위함
    public void handle(final ServerRequest serverRequest) {

        final String body = (String) serverRequest.attribute("cachedRequestBody").orElse("");

        ...
        repository.save( ... , body , ... );
    }
}
```
