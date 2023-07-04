package eu.lts.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ApplicationSettings {
    InputStream inputStream;

    public Properties getPropValues() throws IOException {
        Properties prop = new Properties();
        String propFileName = "config.properties";
        try {
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            Objects.requireNonNull(inputStream).close();
        }
        return prop;
    }
}
