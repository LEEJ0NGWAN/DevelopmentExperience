# RequestBodyCache
Webflux는 비동기 플로우로써 request body를 한번 emit (bodyToMono or bodyToFlux)하고 난 후에는 재사용 할 수 없습니다

레거시와 연계 혹은 프록시 응용, 히스토리 로깅 등 다양한 방면에서 리퀘스트 바디를 여러번 재사용 하기 위해 캐싱을 구현합니다

ServerRequest에 대해,
구현 필터에서 먼저 리퀘스트 바디를 바이트 캐시로 보관하고 여러번 리퀘스트 바디 호출이 가능하도록 설정합니다

## usecase

ServerRequest를 인자로 다루는 웹플럭스 내부 곳에서든 자유롭게 사용 가능합니다

```java
// ping pong api
public class PingPongHandler {

    public static Mono<ServerResponse> handle(ServerRequest request) {

        return request
            .bodyToMono(String.class)
            .flatMap(ServerResponse.ok()::bodyValue);
    }
}
```

### RequestBroker

요청 리퀘스트를 외부로 전달하는 경우

```java
@Component
@RequiredArgsConstructor
public class RequestBroker {

    private final WebClient webClient;

    public Mono<ServerResponse> handle(final ServerRequest request) {

        return request
        .bodyToMono(byte[].class)
        .flatMap(
            bytes -> webClient
            .method(request.method())
            .uri( ** EXTERNAL API URI ** )
            .contentType(request.headers().contentType().orElse(MediaType.TEXT_PLAIN))
            .exchangeToMono(response -> response.toEntity(byte[].class)))
        .flatMap(
            entity -> ServerResponse
            .status(entity.getStatusCode())
            .contentType(entity.getHeaders().getContentType())
            .bodyValue(entity.getBody()));
    }
```

### RequestLogger

요청 리퀘스트의 정보를 히스토리 테이블에 레거시 동기 방식으로 save 하는 경우

```java
...
request
    .bodyToMono(String.class)
    .doOnNext(body -> Mono.fromRunnable(
        () -> {

            // Block IO for Sync DB logging
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe())
...
```
