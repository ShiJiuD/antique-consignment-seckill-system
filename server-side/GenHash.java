import org.mindrot.jbcrypt.BCrypt;

public class GenHash {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            System.out.println(BCrypt.hashpw("123456", BCrypt.gensalt()));
        }
    }
}
