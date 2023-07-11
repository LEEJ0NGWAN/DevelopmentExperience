
[돌아가기](https://github.com/LEEJ0NGWAN/DevelopmentExperience)

# NullToEmptyValueJacksonConfig

스프링부트 앱에서 JSON 응답을 내려줄 때, Null 대신 각 타입별로 Empty Value를 내려줄 수 있도록 ObjectMapper를 커스터마이징 한 설정 파일입니다

해당 설정 파일(JacksonConfig.java)에서는 List 컬렉션이 Null일 경우는 [], Map 컬렉션이 Null일 경우는 {}, 스트링이 Null일 경우는 ""로 치환하여 JSON 시리얼라이징을 수행합니다

JSON으로 시리얼라이징할 대상 DTO 클래스의 맴버로써 다중 깊이의 컬렉션이 들어왔을 경우도 커버가 가능하며, 리플렉션은 DTO 클래스의 맴버당 딱 1번씩만 수행하도록 캐싱이 구현되어 있습니다

### 예시

예를 들어 DTO 클래스와 해당 DTO 클래스의 객체가 다음과 같을때

```
// 클래스
public class DTO {

  private String a;
  private List<String> b;
  private List<String> c;
  private Map<String, String> d;
  private Map<String, String> e;
  private Map<String, List<String>> f;
  private List<Map<String, List<String>>> g;
}

// DTO 클래스의 어떤 객체 dto
dto {
  a = null;
  b = null;
  c = [ "1", null, "2", "3", null, "5"];
  d = {
    "1": "cheese",
    "2": null,
    "3": "grape",
    "4": null
  };
  e = null;
  f = {
    "1": [ null, null, "3", "4", "5" ];
    "2": null,
    "3": [ "0", "1"];
  }
  g = [
    { "g.a": null, "g.b": [ null, "2", null, "3" ] },
    null,
    { "g.1": [ "1", null, "3"], "g.2": null }
  ]
```

JSON 으로 시리얼라이징을 수행하면 각 null의 타입을 유추하여 알맞은 빈 값으로 치환합니다 ( String: "", Map: {}, List: [] )
```
// JSON
{
  a: "",
  b: [],
  c: [ "1", "", "2", "3", "", "5" ];
  d: {
    "1": "cheese",
    "2": "",
    "3": "grape",
    "4": ""
  }
  e: {}
  f: {
    "1": [ "", "", "3", "4", "5" ],
    "2": [],
    "3": [ "0", "1"]
  }
  g: [
    { "g.a": [], "g.b": [ "", "2", "", "3" ] },
    {},
    { "g.1": [ "1", "", "3" ], "g.2": [] }
  ]
}
```

