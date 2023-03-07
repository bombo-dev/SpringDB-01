package hello.jdbc.connection;

// 추상 클래스에서 생성자를 추가할 수 있으나, 객체로 생성할 수는 없음.
public abstract class ConnectionConst {
    public static final String URL = "jdbc:h2:tcp://localhost/~/test";
    public static final String USERNAME = "sa";
    public static final String PASSWORD = "";
}
