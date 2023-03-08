package hello.jdbc.repository;

import com.zaxxer.hikari.HikariDataSource;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class MemberRepositoryV1Test {

    MemberRepositoryV1 repository;

    @BeforeEach
    void beforeEach() {
        //기본 DriverManager - 항상 새로운 커넥션을 획득
//        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setPoolName("MyPool");
        repository = new MemberRepositoryV1(dataSource);

    }
    @Test
    void crud() throws SQLException {
        //save
        Member member = new Member("memberV0", 10000);
        repository.save(member);

        //findById
        Member findMember = repository.findById(member.getMemberId());
        //== 동일성과, 동등성 ==//
        log.info("findMember == member {} ", findMember == member);
        log.info("findMember={}", findMember);
        assertThat(findMember).isEqualTo(member);

        //update : money: 10000 -> 20000
        repository.update(member.getMemberId(), 20000);
        Member updatedMember = repository.findById(member.getMemberId());
        assertThat(updatedMember.getMoney()).isEqualTo(20000);

        //delete
        repository.delete(member.getMemberId());
        // 삭제 시 검증 방법, 삭제 해서 데이터가 없으면 예외가 발생할 것이기 때문에 예외를 검증
        // 그러나, 실행 도중 예외가 발생해서 delete 메소드가 실행되지 않으면 테스트 또한 통과되지 않는다. 트랜잭션으로 인한 문제이다.
        assertThatThrownBy(() -> repository.findById(member.getMemberId()))
                .isInstanceOf(NoSuchElementException.class);
    }

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
}