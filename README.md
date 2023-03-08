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
