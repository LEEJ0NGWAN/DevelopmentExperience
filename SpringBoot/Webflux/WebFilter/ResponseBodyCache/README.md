[돌아가기](https://github.com/LEEJ0NGWAN/DevelopmentExperience)

# RequestBodyCacher

Webflux 에서는 한번 Response body를 emit 한 후 데이터를 재사용할 수 없습니다.

리스폰스 바디를 응답 후에도 재사용하기 위해 캐싱을 구현합니다.

ServerResponse에 대해,
리스폰스 바디를 바이트 캐시로 보관하고 여러번 리스폰스 바디 호출이 가능하도록 하는 웹플럭스 필터입니다
(단, `Mono<ServerResponse>` Mono 퍼블리셔 기준으로 구현했으며, Flux에 대해서는 추가 구현이 필요합니다.

```java
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ResponseBodyCacher implements WebFilter {

    private static final String RESPONSE = "response";
    private static final byte[] EMPTY_BYTES = {};

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return chain
        .filter(
            exchange
            .mutate()
            .response(new ResponseBodyDecorator(exchange)) // 리스폰스 데이터 캐싱 데코레이터
            .build())
        .doFinally(
            $-> Mono.fromRunnable(
                ()-> {

                    // 캐싱된 response 데이터 꺼내오기
                    final byte[] bytes = (byte[]) exchange.getAttributes().remove(RESPONSE);

                    // ... 캐시 데이터 사용 ..
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe());
    }

    public static class ResponseBodyDecorator extends ServerHttpResponseDecorator {

        private final ServerWebExchange exchange;

        public ResponseBodyDecorator(ServerWebExchange exchange) {

            super(exchange.getResponse());
            this.exchange = exchange;
        }

        // writeWith 메소드로 리스폰스 응답을 하기 전 캐싱을 위한 커스터마이즈
        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

            // Mono에 대해서만 구현; Flux의 경우 추가 구현 필요
            if (body instanceof Mono) {

                final Mono<? extends DataBuffer> mono = (Mono<? extends DataBuffer>) body;

                return super
                .writeWith(mono.map(dataBuffer -> {

                    // 리스폰스 바이트 추출 후 어트리뷰트로 보관
                    final byte[] bytes = new byte[dataBuffer.readableByteCount()];

                    DataBufferUtils.release(dataBuffer.read(bytes));

                    exchange.getAttributes().put(RESPONSE, bytes);

                    // 캐싱 후 클라이언트로 응답 데이터 반환 실시
                    return exchange.getResponse().bufferFactory().wrap(bytes);
                }))
                .onErrorResume(e -> {

                    e.printStackTrace();
                    return Mono.empty();
                });
            }

            return super.writeWith(body);
        }
    }
}
```

### CustomWebExceptionHandler
위의 필터는 웹플럭스가 익셉션에 대해 디폴트로 제공하는 에러 json 리스폰스에 대해서는 캐싱을 수행할 수 없습니다.
디폴트 에러 json도 캐싱하기 위해 **AbstractErrorWebExceptionHandler**를 커스터마이징한 핸들러를 추가로 구현합니다.
```java
import java.util.Map;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component @Order(-2)
public class CustomWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final String RESPONSE = "response";

    // ResourcePropeties가 스프링부트 2.6.x부터 제거되었기에
    // 생성자에서 ResourceProperties 추가 설정해줌 (deprecated since 2.4)
    public CustomWebExceptionHandler(
        ErrorAttributes errorAttributes, WebProperties webProperties,
        ApplicationContext applicationContext, ServerCodecConfigurer configurer) {

        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    // 디폴트 에러 리스폰스 핸들링 라우팅 함수 오버라이드
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {

        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    // 커스터마이징 관련 구현 메소드
    private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {

        // 리스폰스하게 될 에러 어트리뷰트 데이터를 로드
        final Map<String, Object> errorAttributes =
        getErrorAttributes(request, ErrorAttributeOptions.of(Include.MESSAGE));

        // ... 에러 어트리뷰트 가공 작업

        // 에러 어트리뷰트(에러 리스폰스)의 바이트 데이터를 exchange 어트리뷰트에 보관
        final byte[] bytes = errorAttributes.toString().getBytes();

        request.attributes().put(RESPONSE, bytes);

        return ServerResponse.ok().bodyValue(errorAttributes);
    }
}
```
