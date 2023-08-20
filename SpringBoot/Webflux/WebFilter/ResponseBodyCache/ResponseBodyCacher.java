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
