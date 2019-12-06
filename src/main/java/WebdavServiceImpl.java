import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WebdavServiceImpl implements WebdavService {

    private Sardine m_sardine;
    private String m_webdav_base_url;

    public WebdavServiceImpl(String webdav_base_url, String username, String password) {
        this.m_sardine = SardineFactory.begin(username, password);
        this.m_webdav_base_url = webdav_base_url;
    }

    @Override
    public boolean exists(String url) {
        try {
            return m_sardine.exists(m_webdav_base_url + url);
        } catch (IOException e) {
            System.out.println("Unable to get resource from webdav [" + m_webdav_base_url + "]," + e);
            return false;
        }
    }

    @Override
    public List<String> listFolder(String url) throws IOException {
        System.out.println("List all files under " + m_webdav_base_url + url);
        List<DavResource> resources = m_sardine.list(m_webdav_base_url + url);
        if (null == resources || resources.isEmpty()) {
            return Collections.emptyList();
        }
        return resources.stream().
                map(DavResource::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void createDirectory(String url) throws IOException {
        System.out.println("Create directory " + m_webdav_base_url + url);
        String[] folders = getDirectories(url);
        for (String f : folders) {
            if (!exists(f)) {
                m_sardine.createDirectory(m_webdav_base_url + f);
            }
        }
    }

    // url must be end with /
    @Override
    public void deleteDirectory(String url) throws IOException {
        System.out.println("Delete directory " + m_webdav_base_url + url);
        if (exists(url)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            m_sardine.delete(m_webdav_base_url + url);
        }
    }

    @Override
    public void uploadFile(String url, InputStream inputStream) throws IOException {
        System.out.println("Upload file to " + m_webdav_base_url + url);
        if (url.contains("/")) {
            // create parent folder first
            String parentDirectory = getParentDirectory(url);
            createDirectory(parentDirectory);
        }
        m_sardine.put(m_webdav_base_url + url, inputStream);
    }

    @Override
    public String getFileAsString(String url) throws IOException {
        System.out.println("Download file from " + m_webdav_base_url + url);
        if (!exists(url)) {
            throw new IOException("File " + m_webdav_base_url + url + " does not exists");
        }

        InputStream inputStream = m_sardine.get(m_webdav_base_url + url);
        return inputStream == null ? "" : convertInputStreamToString(inputStream);
    }

    @Override
    public void deleteFile(String url) throws IOException {
        System.out.println("Delete file at " + m_webdav_base_url + url);
        if (!exists(url)) {
            throw new IOException("File " + m_webdav_base_url + url + " does not exists");
        }
        m_sardine.delete(m_webdav_base_url + url);
    }

    @Override
    public String getDescription(String url) throws IOException {
        System.out.println("Get file description for " + m_webdav_base_url + url);
        List<DavResource> resources = m_sardine.list(m_webdav_base_url + url);
        if (null == resources || resources.isEmpty()) {
            return "";
        }

        if (resources.size() > 1) {
            // resource is a directory
            return resources.stream().
                    map(DavResource::getName)
                    .collect(Collectors.toList()).toString();
        } else {
            // resource is a file
            return new StringBuilder()
                    .append("file_name: ").append(url).append("\n")
                    .append("created_at: ").append(resources.get(0).getCreation()).append("\n")
                    .append("last_modified: ").append(resources.get(0).getModified()).append("\n")
                    .append("content_type: ").append(resources.get(0).getContentType()).append("\n")
                    .append("content_length: ").append(resources.get(0).getContentLength()).append("\n")
                    .toString();
        }
    }

    @Override
    public String lock(String url) throws IOException {
        System.out.println("Lock file " + m_webdav_base_url + url);
        if (!exists(url)) {
            System.out.println(m_webdav_base_url + url + " does not exists");
            return "";
        }
        String token = m_sardine.lock(m_webdav_base_url + url);
        System.out.println("Token of file " + m_webdav_base_url + url + " : " + token);
        return token;
    }

    @Override
    public void unlock(String url, String token) throws IOException {
        System.out.println("Unlock file " + m_webdav_base_url + url + " with token " + token);
        if (!exists(url)) {
            System.out.println(m_webdav_base_url + url + " does not exists");
            return;
        }
        m_sardine.unlock(m_webdav_base_url + url, token);
    }

    @Override
    public String refreshLock(String url, String token) throws IOException {
        System.out.println("Refresh lock for file " + m_webdav_base_url + url + " with token " + token);
        if (!exists(url)) {
            System.out.println(m_webdav_base_url + url + " does not exists");
            return "";
        }
        return m_sardine.refreshLock(m_webdav_base_url + url, token, m_webdav_base_url + url);
    }

    @Override
    public void copy(String sourceUrl, String desUrl, boolean overwrite) throws IOException {
        System.out.println("Copy from " + m_webdav_base_url + sourceUrl + " to " + m_webdav_base_url + desUrl);
        if (!exists(sourceUrl)) {
            System.out.println(m_webdav_base_url + sourceUrl + " does not exists");
            return;
        }

        if (!overwrite && exists(desUrl)) {
            System.out.println(m_webdav_base_url + desUrl + " already exists");
            return;
        }
        String parent_des_dir = getParentDirectory(desUrl);
        if (!exists(parent_des_dir)) {
            if (parent_des_dir.contains("/")) {
                createDirectory(parent_des_dir);
            }
        }
        m_sardine.copy(m_webdav_base_url + sourceUrl, m_webdav_base_url + desUrl, overwrite);
    }

    @Override
    public void move(String sourceUrl, String desUrl, boolean overwrite) throws IOException {
        System.out.println("Move from " +m_webdav_base_url+  sourceUrl + " to " + m_webdav_base_url + desUrl);
        if (!exists(sourceUrl)) {
            System.out.println(m_webdav_base_url + sourceUrl + " does not exists");
            return;
        }

        if (!overwrite && exists(desUrl)) {
            System.out.println(m_webdav_base_url + desUrl + " already exists");
            return;
        }
        String parent_des_dir = getParentDirectory(desUrl);
        if (!exists(parent_des_dir)) {
            if (parent_des_dir.contains("/")) {
                createDirectory(parent_des_dir);
            }
        }
        m_sardine.move(m_webdav_base_url + sourceUrl, m_webdav_base_url + desUrl, overwrite);
    }

    private String getParentDirectory(String filePath) {
        if (filePath.contains("/")) {
            return filePath.substring(0, filePath.lastIndexOf("/"));
        } else {
            return filePath;
        }
    }

    private String[] getDirectories(String url) {
        if (null == url || url.isEmpty()) {
            return null;
        }
        String[] tempFolder = url.split("/");
        String[] folders = new String[tempFolder.length];
        for (int i = 0, j = tempFolder.length; i < j; i++) {
            if (0 == i) {
                folders[i] = tempFolder[i];
            } else {
                folders[i] = folders[i-1] + "/" + tempFolder[i];
            }
        }
        return folders;
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
