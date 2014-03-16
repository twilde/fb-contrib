import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class LO_Sample {
    private static Logger l1 = Logger.getLogger(String.class);
    private static Logger l2 = Logger.getLogger("com.foo.LO_Sample");
    private static final org.slf4j.Logger l3 = LoggerFactory.getLogger(LO_Sample.class);

    public LO_Sample(Logger l3) {

    }

    public void testStutter() throws IOException {
        InputStream is = null;
        try {
            File f = new File("Foo");
            is = new FileInputStream(f);
        } catch (Exception e) {
            l1.error(e.getMessage(), e);
        } finally {
            is.close();
        }
    }

    public void testParmInExMessage() {
        try {
            InputStream is = new FileInputStream("foo/bar");
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse {}", e);
        }
    }

    public void testInvalidSLF4jParm() {
        l3.error("This is a problem {0}", "hello");
    }
    
    public void testLogAppending(String s) {
        try {
            l3.info("Got an error with: " + s);
        } catch (Exception e) {
            l3.warn("Go a bad error with: " + s, e);
        }
    }

    public void testWrongNumberOfParms() {
        l3.error("This is a problem {}", "hello", "hello");
        l3.error("This is a problem {} and this {}", "hello");
        l3.error("This is a problem {} and this {} and this {}", "hello", "world");
        l3.error("This is a problem {} and this {} and this {} and this {}", "hello", "hello", "hello");
    }

    public void testFPWrongNumberOfParms() {
        l3.error("This is a problem {}", "hello", new IOException("Yikes"));
        l3.error("This is a problem {} and this {} and this {} and this {}", "hello", "hello", "hello", "hello", new RuntimeException("yikes"));
        l3.error("This is a problem {} and this {}", "hello", new RuntimeException("yikes"));
    }

    public class Inner {
        public void fpUseAnon() {
            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Logger.getLogger(Inner.class).error("fp");
                    Logger.getLogger(LO_Sample.class).error("not fp");
                }
            };
        }
    }
}
