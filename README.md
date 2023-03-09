# SpringDB-01

## 2023.03.08
### Test Package Lombok
- `build.gradle` 에 다음과 같은 코드르 추가해주어야 한다.
```java
//Slf4j를 사용하기 위해 lombok를 추가해줘야함
testCompileOnly 'org.projectlombok:lombok'
testAnnotationProcessor 'org.projectlombok:lombok'
```

### Abstract Class

- 객체의 생성을 막기 위해 추상 클래스를 사용한다하였음. 살짝 헷갈린 부분은 생성을 막는다고 해서 생성자를 못 쓰는건아니다. 추상클래스는 클래스의 상속용도로 사용되기 때문에 생성자를 통해서 자식 클래스에서 
Super()를 통해 조상의 생성자를 충분히 불러올 수 있다.

### ResultSet
- query의 실행결과를 `ResultSet`에 담아놓았을 때, 이후 쿼리를 확인을 해야한다.
- 이때, `rs.next()`를 통해서 `row cursor`를 옮길 수 있는데, 처음에는 0번 째 cursor (row - 1) 위치를 가리키고 있고, rs.next()를 실행하게 되면 아래 row로 이동한다.
- `rs.next()`는 데이터의 존재 유무를 가지고 true or false 를 판단한다. 따라서, 여러 개의 데이터가 조회가 됐을 경우 다음과 같이 쿼리를 조회할 수 있다. 
```java
while(!rs.next()){
  Member member = new Member();
  member.setMemberId(rs.getString("member_id"));
  member.setMoney(rs.getInt("money"));
  return member;
}
```
- Primary Key로 회원을 조회 할 경우에는 `if(!rs.next())` 로 작성해도 된다.

### 동일성과 동등성
- 항상 배우지만 종종 까먹는데, `동일성은 ==` 이고, `동등성은 equals` 이다. 해당 코드에서는 `lombok`의 @Data를 사용함으로써 equals와 hashcode가 구현이 되어있기때문에 정상적으로 통과가 되는 모습을 볼 수 있는데, 직접 구현해야 할 경우 IDE의 힘을 빌리거나 다음과 같이 코드르 작성해주어야 한다.
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Member member = (Member) o;
    return money == member.money && Objects.equals(memberId, member.memberId);
    // money는 primitive 타입이기때문에 == 으로 비교가 가능하지만, memberId는 String이라서 동등성 비교를 해주어야 한다.
}
```
### 예외 발생 테스트 코드
```java
Assertions.assertThatThrownBy(() -> repository.findById(memberId)).isInstanceOf(예외클래스.class);
```

### 데이터베이스 커넥션과 커넥션 풀
- 애플리케이션에서 데이터베이스 커넥션을 획득하기 위해서는 애플리케이션에서 DB 드라이버를 통해서 커넥션을 조회하고, DB 드라이버가 URL을 확인하여 TCP/IP 커넥션을 획득한 후 ID/PW를 전달하여 DB에 내부 세션을 만들고, DB가 애플리케이션에게 DB 내부 세션을 만들었다고 응답하는 방식인데, 중요한건 우리가 이전에 봤던 getConnection이 이루어 질때마다 이와 같은 과정이 반복된다는 것이다.
- `TCP/IP` 통신에서는 `3-way handshake`가 발생을 하고 이는 UDP와 달리 긴 시간이 걸리는데, 많은 서비스 로직이 모였을 대 이와 같은 방식이 발생한다면 서비스가 느려지는 문제가 있을 것이다
- 따라서 이와 같은 상황을 방지하기 위해서 생긴 것이 커넥션 풀이다.
- 데이터베이스의 연결이 필요할 때마다 데이터 커넥션을 획득하는 것이 아니고, 커넥션 풀에 미리 데이터 커넥션을 획득해놓고, 애플리케이션이 커넥션을 요청하면 커넥션 풀에 있는 커넥션을 이용하는 방식이다. 이를 통해 기존 방식에 비해 빠른 속도 향상이 된다. 또한, 커넥션 풀에 있는 커넥션을 사용하고 반환할 때 없애는게 아니고 다시 커넥션 풀에 반환한다.

### DataSource를 이용한 커넥션 획득
- 기존에는 요청이 필요할 때 마다, getConnection 메소드를 호출해서 URL, USERNAME, PASSWORD 등 파라미터를 계속 호출해야 하는 문제가 있었지만, DataSource를 활용하면 처음에만 설정 파일을 통해 DataSource를 생성하면 이후에는 파라미터의 사용없이 커넥션을 획득할 수 있다. 이 처럼 실행과 설정을 분리해야 하고, 이러한 개념이 `개방폐쇄 원칙`이다.
- 추가적으로 `HikariPool`에서 커넥션 풀을 획득하는 모습을 보면 커넥션 풀이 다 채워진 것을 확인하기도 전에 테스트가 종료되는 것을 볼 수 있었는데, `Connection Pool`을 획득하는 과정은 결국 여러개의 여러 개의 Connection을 요청하는 것과 같고, 이는 TCP/IP 통신이 이용되어야 때문에 애플리케이션 실행 시 무거워질 수 있다. 따라서 다른 스레드에서 실행되기 때문에 이러한 문제가 발생하는 것이다.
이러한 개념을 `sync`, `non-blocking` 에 대입 할 수 있을 것 같다.

### DriverManagerDataSource 와 HikariDataSource 수행 시간 차이
- 계속해서 커넥션을 획득하기 위해 DB에 요청하면 TCP/IP 통신으로 인한 시간 지연이 발생한다고 배웠다. 실제로 그를 예방하기 위해 커넥션 풀을 사용했는데, 그 시간차이가 어느정도인지 테스트를 해보고 싶었다.
```java
@Test
void calcConnectionTime() throws SQLException, InterruptedException {
    //save 100개
    long startTime = System.currentTimeMillis();
    for(int i = 0; i < 100; i++){
        repository.save(new Member("memberV" + i, 10000));
    }
    long endTime = System.currentTimeMillis();
    log.info("걸린 시간 : {}", endTime - startTime);
    Thread.sleep(1000);
}
```
- 다음과 같이 간단한 테스트 코드를 작성해봤고, repository는 DataSource에 의해 의존주입 받고 있기 때문에 구현 객체만 바꿔서 걸린 시간을 확인해봤다.
INFO hello.jdbc.repository.MemberRepositoryV1Test - 걸린 시간 : 152 -> HikariPool
INFO hello.jdbc.repository.MemberRepositoryV1Test - 걸린 시간 : 402 -> DriverManagerDataSource
- 100개의 작은 데이터만 넣었을 뿐인데도, 250ms 나 차이가 났다. MySQL의 경우 커넥션 획득에 걸리는 시간이 1ms 정도라고 했는데, H2는 아무래도 좀 더 걸리는 것으로 예상된다. 

## 2023.03.09
### 트랜잭션 ACID
1. 원자성(Atomicity) : 트랜잭션 내에서 실행한 작업들은 하나의 작업인 것처럼 모두 성공하거나 모두 실패해야 한다.
- 이 말이 이론으로 들었을 때에는 무슨 말인가 싶었는데, 직접 해보고 나니 무슨 말인지 바로 이해할 수 있었던 부분.
2. 일관성(Consistency) : 모든 트랜잭션은 일관성 있는 데이터베이스 상태를 유지해야 한다. `무결성 제약 조건 항상 만족`
  - 무결성 제약 조건
    1. 개체 무결성 : 각 릴레이션의 기본키를 구성하는 속성은 널 값이나 중복된 값을 가질 수 없다.
    2. 참조 무결성 : `외래키 값은 NULL` 이거나 참조하는 릴레이션의 기본키 값과 동일해야 한다.
    3. 도메인 무결성 : 속성들의 값은 정의된 도메인에 속한 값이어야 한다. ex) IN('남', '여' 같은 것을 말한다.)
    4. 고유 무결성 : 특정 속성에 대해 고유한 값을 가지도록 조건이 주어진 경우, 릴레이션의 각 튜플이 가지는 속성 값들은 서로 달라야 한다.
    5. NULL 무결성 : 릴레이션의 특정 속성 값은 NULL이 될 수 없다. ex) NOT NULL이 NULL이 되는 경우
    6. 키 무결성 : 각 릴레이션은 최소한 한 개 이상의 키가 존재햐아 한다. 테이블엔 적어도 기본키는 있어야 한다.
3. 격리성(Isolation) : 동시에 실행되는 트랜잭션들이 서로에게 경향을 미치지 않도록 격리한다. Isolation level이 나오는데 이건 향후에 JPA 책을 보고 자세히 알아봐야 겠다.
4. 지속성(Durability) : 트랜잭션을 성공적으로 끝내면 그 결과가 기록되야 한다. 마치 게임 서버에서 시간이 지난 데이터를 백업해주는 것과 같은 것이다. 

### 조회시점 락
- 일반적으로 조회를 할 때에는 락이 필요 없을 것 같다고 생각했는데, 동시성 문제가 발생할 수 도 있다는 생각이 들었다. 당근마켓이 떠올랐는데, 판매 금액을 가지고 조회해서 그 금액에 따라 예약을 걸어두었는데, 상대방이 갑자기 거래하기 직전에 판매금을 올려서 구매자가 고스란히 가격이 올라간 물건을 사게 됐을 때? 물론 그 전에 예외를 처리할 수도 있지만 이러한 상황이 조회를 했을 때 긴 시간 락이 걸려있어야 하는 상황이 아닌가 하는 생각이 들었다. `select for update` 가 그 역할을 해준다고 한다.

### 애플리케이션 구조
- 자주 까먹는 애플리케이션 구조, 많은 구조들이 있지만 대체적으로 `프레젠테이션 계층`, `서비스 계층`, `데이터 접근 계층`으로 구분
1. 프레젠테이션 계층
  - 여기서는 주로 UI, 웹 요청, 사용자 요청 사항등을 검증하는 영역이다.
2. 서비스 계층
  - 비즈니스 로직을 담당하고, 해당 영역은 가급적 특정 기술에 의존하지 않고, 순수 자바 코드로 작성해야 된다. 즉, OCP와 DI가 적절하게 이루어져야 한다는 것이다.
3. 데이터 접근 계층
  - 실제 데이터베이스에 접근하는 코드 들이 모여있다, JDBC, JPA, File, Redis, Mongo 등..
- 예전에 많은 고민을 했던 부분이 데이터 접근 계층에서 어디까지 해야하는가가 의문이었는데, 데이터 접근에서도 순수하게 데이터 접근을 위한 코드만이 있어야하고, 서비스 로직에서는 순수하게 데이터 접근게층에 구현이 되어있는 로직을 불러와서 순수하게 서비스 로직에서는 그것을 이용해서 순수 자바코드로 작성을 해야한다.
- 참 여기서 많이 헷갈리는 부분이 DDD에서 때로는 엔티티에 비즈니스 로직을 적는 경우가 많이 보이는데, 이러한 부분을 확인해 볼 필요가 있을 것 같다. 이 부분은 아직 해소가 안됐다.

### 스프링의 트랜잭션 동기화 매니저 동작 방식
- 우선 트랜잭션 동기화 매니저는 Thread Local을 사용하여 각각의 쓰레드 별로 저장소를 소유하고 있다. -> 스레드는 본래 스택과 Program counter를 제외한 힙, 코드, 데이터 영역은 공유하는데, 예상되는 동작방식은 쓰레드 별로 공유하는 힙을 새로 생성하고 스택 포인터에 힙의 주소를 담아둠으로써 분리되어 동작하는게 아닌가 생각이 드는데, 향후 강의에서 설명을 해준다고 하니 일단 알고 넘어가자.
- 동작 방식의 간단한 설명
1. 트랜잭션을 위해서는 커넥션이 필요하다. 이 커넥션을 이전처럼 데이터 소스를 통해서 얻게 되고(물론 데이터 소스를 DriverManager를 사용하느냐 Connection Pool을 사용하느냐에 따라 커넥션을 획득하는 방식은 당연히 다르다) 이후 트랜잭션을 시작한다.
2. 트랜잭션 매니저가 데이터 소스로 부터 받은 커넥션을 트랜잭션 동기화 매니저에 트랜잭션이 시작된 커넥션을 보관한다.
3. 데이터 접근 계층의 데이터 접근 로직이 트랜잭션 동기화 매니저에 보관된 커넥션을 꺼내서 사용한다. 이러한 이유로 이전에 커넥션을 전달하는 방식은 사용하지 않아도 된다.
4. 비즈니스 로직 수행 이후 트랜잭션 동기화 매니저에서 동기화된 커넥션을 획득하고 트랜잭션 동기화 매니저를 정리한다.
