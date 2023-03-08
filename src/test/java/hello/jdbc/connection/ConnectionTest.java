package hello.jdbc.connection;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;

@Slf4j
public class ConnectionTest {
    @Test
    void driverManager() throws SQLException {
        Connection conn1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection conn2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        log.info("connection={}, class={}", conn1, conn1.getClass());
        log.info("connection={}, class={}", conn2, conn2.getClass());
    }

    @Test
    void dataSourceDriverManager() throws SQLException {
        //DriverManagerDataSource - 항상 새로운 커넥션 획득, 위하고 똑같음.
        DataSource datasource = new DriverManagerDataSource(URL, USERNAME, PASSWORD); // 다형성
        useDataSource(datasource);
    }

    @Test
    void dataSourceConnectionPool() throws SQLException, InterruptedException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("MyPool");



        useDataSource(dataSource);
        // 여기서 Pool 을 생성하는 스레드는 애플리케이션의 큰 영향을 미치지 않기 위해 별도의 스레드에서 생성이 된다.
        // 그러한 이유로 스레드를 바로 반환하면, 10개의 커넥션 풀이 생성되는 로그를 확인하기도 전에 테스트가 종료된다.
        // sync, non-blocking Data Source 를 가지고 이와 같은 개념을 설명 할 수 있을 것 같다.
        Thread.sleep(1000);

        // 이 때, 커넥션 풀이 채워지기 전에 커넥션을 획득하려고 하면 내부적으로 기다린다.
        // 교착상태 즉, DeadLock 이 걸리게 되고 Hikari 에 내부적으로 HikariDataSource 에 있는 ConnectionTimeout() 이 지정이 되어있고,
        // 이 시간이 지나게 되면 예외를 반환한다. setConnectionTimeout() 을 통해 timeout 시간을 바꿀 수 있다.
    }

    // 인터페이스를 통해서 받기 때문에 다형성으로 인해 다른 방식의 DataSource 를 받아올 수 있음.
    // 이제 커넥션이 필요할 때마다 따로 코드를 추가해주기보다 dataSource 를 사용해서 커넥션을 획득할 수 있게 되었다.
    // 이것이 설정과 사용의 분리, 개방폐쇄의 원칙과 같다고 생각한다.
    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection conn1 = dataSource.getConnection();
        Connection conn2 = dataSource.getConnection();
        log.info("connection={}, class={}", conn1, conn1.getClass());
        log.info("connection={}, class={}", conn2, conn2.getClass());
    }
}
