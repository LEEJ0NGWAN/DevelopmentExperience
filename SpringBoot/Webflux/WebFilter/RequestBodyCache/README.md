# RequestBodyCache
Webflux의 경우, request body를 한번 emit (bodyToMono or bodyToFlux)하고 난 후에는 재사용 할 수 없습니다
레거시와 연계 혹은 프록시 응용, 히스토리 로깅 등 다양한 방면에서 리퀘스트 바디를 여러번 재사용 하기 위해 캐싱을 구현합니다

ServerRequest 객체의 (내부 exchange의) attributes에 리퀘스트 바디를 저장해놓는 웹 필터를 구현합니다
