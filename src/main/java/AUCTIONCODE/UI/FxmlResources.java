package AUCTIONCODE.UI;

import javafx.fxml.FXMLLoader;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FxmlResources {
    private FxmlResources() {
    }

    public static FXMLLoader loader(String resourcePath) {
        return new FXMLLoader(location(resourcePath));
    }

    public static URL location(String resourcePath) {
        URL classpathResource = FxmlResources.class.getResource(resourcePath);
        if (classpathResource != null) {
            return classpathResource;
        }

        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        Path sourceResource = Path.of("src", "main", "resources").resolve(normalized).toAbsolutePath();
        if (Files.exists(sourceResource)) {
            try {
                return sourceResource.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Invalid FXML path: " + sourceResource, e);
            }
        }

        throw new IllegalStateException("FXML resource not found: " + resourcePath
                + " (also checked " + sourceResource + ")");
    }
}
