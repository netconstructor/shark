package shark;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.hadoop.hive.cli.TestCliDriver;
import org.apache.hadoop.hive.ql.QTestUtil;

/**
 * The test driver. It overloads Hive's TestCliDriver to use SharkQTestUtil.
 * There is also a feature to selectively run tests, i.e. only tests whose
 * names match the regular expression pattern defined in environmental variable
 * TEST are invoked.
 */
public class TestSharkCliDriver extends TestCliDriver {

  static {
    // Replace qt in Hive's TestCliDriver with SharkQTestUtil.
    try {

      Field qtField = TestCliDriver.class.getDeclaredField("qt");
      qtField.setAccessible(true);

      Field outDirField = QTestUtil.class.getDeclaredField("outDir");
      outDirField.setAccessible(true);
      Field logDirField = QTestUtil.class.getDeclaredField("logDir");
      logDirField.setAccessible(true);

      QTestUtil qt = (QTestUtil) qtField.get(null);
      String outDir = (String) outDirField.get(qt);
      String logDir = (String) logDirField.get(qt);

      qt = new SharkQTestUtil(outDir, logDir);
      qt.cleanUp();
      qt.createSources();

      qtField.set(null, qt);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public TestSharkCliDriver(String name) {
    super(name);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    TestSuite hiveSuite = (TestSuite) TestCliDriver.suite();
    
    @SuppressWarnings("unchecked")
    Enumeration<TestCliDriver> tests = (Enumeration<TestCliDriver>) hiveSuite.tests();
    
    String fileName = System.getenv("TEST_FILE");
    Set<String> regTestsFromFile = new HashSet<String>();
    if (fileName != null && fileName.length() > 0) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = null;
        while ((line = reader.readLine()) != null) {
          regTestsFromFile.add(line);
        }
      } catch (FileNotFoundException e) {
        System.err.println("FileNotFoundException: " + e.getMessage());
        System.exit(1);
      } catch (IOException e) {
        System.err.println("IOException: " + e.getMessage());
        System.exit(1);
      }
    }
    
    Pattern regexPattern = null;
    String pattern = System.getenv("TEST");
    if (pattern != null && pattern.length() > 0) {
      regexPattern = Pattern.compile(System.getenv("TEST"));
    }
    
    System.out.println("---------------------------------------------------");
    System.out.println("---------------------------------------------------");
    System.out.println("---------------------------------------------------");
    System.out.println("---------------------------------------------------");
    System.out.println(TestSharkCliDriver.class.getName());

    while (tests.hasMoreElements()) {
      TestCliDriver test = tests.nextElement();
      
      boolean passRegex = (regexPattern == null);
      boolean passFile = (regTestsFromFile.size() == 0);
      
      if (regexPattern != null) {
        Matcher m = regexPattern.matcher(test.getName());
        if (m.find() || test.getName() == "testCliDriver_shutdown") {
          passRegex = true;
        }
      }
      
      if (regTestsFromFile.size() > 0) {
        passFile = regTestsFromFile.contains(test.getName());
      }
      
      if (passRegex && passFile) {
        suite.addTest(test);
        System.out.println("TestSharkCliDriver: " + test.getName());
      }
    }
    
    System.out.println("TestSharkCliDriver total test to run: " + suite.countTestCases());
    
    System.out.println("---------------------------------------------------");
    System.out.println("---------------------------------------------------");
    System.out.println("---------------------------------------------------");
    System.out.println("---------------------------------------------------");

    
    return suite;
  }
}
