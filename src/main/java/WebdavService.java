import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface WebdavService {

    boolean exists(String url);

    // Directory
    List<String> listFolder(String url) throws IOException;

    void createDirectory(String url) throws IOException;

    void deleteDirectory(String url) throws IOException;

    void copy(String sourceUrl, String desUrl, boolean overwrite) throws IOException;

    void move(String sourceUrl, String desUrl, boolean overwrite) throws IOException;

    // File
    void uploadFile(String url, InputStream inputStream) throws IOException;

    String getFileAsString(String url) throws IOException;

    void deleteFile(String url) throws IOException;

    String getDescription(String url) throws IOException;

    // Lock/Unlock
    String lock(String url) throws IOException;

    void unlock(String url, String token) throws IOException;

    String refreshLock(String url, String token) throws IOException;
}
