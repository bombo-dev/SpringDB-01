package hello.jdbc.connection;

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
