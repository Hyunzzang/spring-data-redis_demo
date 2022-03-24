# spring-data-redis_demo
* RedisRepository와 RedisTemplate 사용 샘플(사용설명).
* redis client는 lettuce를 사용.

## 1. 환경 설정
* [레디스 커스텀 환경 설정](./src/main/java/com/example/redis/config/RedisConfig.java)

### Connection Factory 설정
* spring-boot-starter-data-redis 의존성 추가하면 restTemplate과 redisConnectionFactory가 자동으로 생성 되나 다양한 설정과 여러 restTemplate를 사용하기 위해서 컨스텀 설정 하도록 하자.
* LettuceConnectionFactory 사용.
* 레디스 서버 구성 환경에 맞게 (Standalone, Sentinel, Cluster) 설정 하여 LettuceConnectionFactory 빈 생성시 생성자에 주입.
* 커넥션 폴은 apache commons-pool2 사용.

### RedisTemplate 설정 
* redis 데이터 접근을 할수 있도록 해주는 클래스.
* redis 저장소의 기본 바이너리 데이터간에 자동 직렬화 / 역직렬화를 수행(기본적으로 객체에 대해 Java 직렬화).
* 문자열의 적용 StringRedisTemplate.

#### Serializer 종류
| 직렬화 |클래스|
|-----|---|
|기본(자바직렬화)|JdkSerializationRedisSerializer|
| 문자열 |StringRedisSerializer|
| JSON |Jackson2JsonRedisSerializer|
| T타입 |GenericToStringSerializer<T>|

#### RedisTemplate에서 제공하는 연산자 인터페이스(데이터 타입별)
| 타입     |연산자 인터페이스|
|--------|---|
| string |ValueOperation|
| list   |ListOperation|
|set|SetOperation|
|sorted|ZSetOperation|
|hash|HashOperation|


## 2. spring data redis repository 사용
* [repository 샘플](./src/test/java/com/example/redis/repository/AvailablePointRedisRepositoryTest.java)

## 3. RedisTemplate 사용
* [RedisTemplate 샘플](./src/test/java/com/example/redis/repository/RedisTemplateTest.java)
* [Incr Decr Template 샘플](./src/test/java/com/example/redis/repository/IncrRedisTemplateTest.java)

## 4. 테스트
* [상품재고 관련 테스트] (./src/test/java/com/example/redis/repository/ProductQuantityServiceTest.java)