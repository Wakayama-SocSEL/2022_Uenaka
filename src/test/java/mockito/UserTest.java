package mockito;

import mockito.PasswordEncoder;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.or;

public class UserTest {
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    @Test
    public void testSnippet() {
        PasswordEncoder mock = mock(PasswordEncoder.class);

        // 2
        doReturn("1").when(mock).encode("a");
        System.out.println(mock.encode("a"));

        verify(mock).encode(or(eq("a"), endsWith("b")));
    }
}
