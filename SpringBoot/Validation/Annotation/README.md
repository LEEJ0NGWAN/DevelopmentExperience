
[돌아가기](https://github.com/LEEJ0NGWAN/DevelopmentExperience)

# Custom Validation Annotation

Spring 에서 validation을 수행할 때, 필드에 따라 요구되는 검증사항이 달라지기에 특성에 맞게 검증 애노테이션을 커스터마이즈 합니다

### @Enum
열거된 값들 중 하나의 값이어야 함을 검증하는 애노테이션 입니다

- 사용 예시

``` java
@Enum(values = { "Male", "Female" })
private String sex; // Male 또는 Female 이어야 합니다
```

- 에러메세지
  - must not be blank
  - must be one of values [ value1, value2, ... ]

### @Date
yyyyMMdd 포맷의 날짜인지 검증하는 애노테이션입니다

- 사용 예시

```
@Date
private String birthDate;

@Date(min = "19000101")
private String createdAt;

@Date(max = "20300101")
private String reservedAt;

@Date(min = "20220325", max ="20230324")
private String updatedAt;
```

- 에러메세지
  - must not be blank
  - must be a date - yyyyMMdd
  - must be greater than or equal to {min}
  - must be less than or equal to {max}

### @Number
정수의 숫자가 minSize,maxSize 사이의 길이인지 검증하거나, minValue,maxValue 값이 있을때 해당 허용 범위의 값인지 검증하는 애노테이션입니다

- 사용 예시

```java
@Number // default min: 1, max: 50
private String serial;

@Number(maxSize = 300)
private String height;

@Number(maxValue = "500000") // minSize: 1, maxSize: 6, maxValue: 500000
private String weight;
```

- 에러메세지
  - must not be blank
  - must be a number
  - size must be {size} (minSize == maxSize 경우)
  - size must be between {minSize} and {maxSize}
  - must be greater than or equal to {minValue}
  - must be less than or equal to {maxValue}
