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
