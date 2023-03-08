package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV1;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@RequiredArgsConstructor
@Slf4j
public class MemberServiceV2 {

    private final MemberRepositoryV2 memberRepository;
    private final DataSource dataSource;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection conn = dataSource.getConnection();

        try {
            conn.setAutoCommit(false);
            // 비즈니스 로직
            bizLogic(fromId, toId, money, conn);
            conn.commit(); // 성공시 커밋
        } catch (Exception e){
            conn.rollback(); // 실패시 롤백
            throw new IllegalStateException(e);
        } finally {
            release(conn);
        }
    }

    private void bizLogic(String fromId, String toId, int money, Connection conn) throws SQLException {
        Member fromMember = memberRepository.findById(conn, fromId);
        Member toMember = memberRepository.findById(conn, toId);

        memberRepository.update(conn, fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(conn, toId, toMember.getMoney() + money);
    }

    private static void validation(Member toMember) {
        if(toMember.getMemberId().equals("ex")){
            throw new IllegalStateException("이체중 예외 발생");
        }
    }

    private static void release(Connection conn) {
        if(conn != null){
            try {
                conn.setAutoCommit(true); // 커넥션 풀을 고려해서 종료
                conn.close();
            } catch (Exception e){
                log.info("error", e);
            }
        }
    }

}
