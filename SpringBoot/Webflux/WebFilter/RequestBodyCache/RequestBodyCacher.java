import java.util.function.Function;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;

@Order @Component @RequiredArgsConstructor public class RequestBodyCacher implements WebFilter {

    private static final byte[] EMPTY_BYTES = {};
    public static final String CACHED_REQUEST_BODY = "cachedRequestBody";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return cacheRequestBody(exchange, chain::filter);
    }

    private static Mono<Void> cacheRequestBody(
        ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> function) {

        return DataBufferUtils
        .join(exchange.getRequest().getBody())
        .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(EMPTY_BYTES))
        .map(dataBuffer -> decorate(exchange, dataBuffer))
        .flatMap(function);
    }

    private static ServerWebExchange decorate(
        ServerWebExchange exchange, DataBuffer dataBuffer) {

        if (dataBuffer.readableByteCount() > 0) {

            final byte[] bytes = new byte[dataBuffer.readableByteCount()];

            DataBufferUtils.release(dataBuffer.read(bytes));

            exchange
            .getAttributes()
            .put(CACHED_REQUEST_BODY, new String(bytes, UTF_8));
        }

        return exchange;
    }
}
