import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Demo {

    static final String WEBDAV_BASE_URL = "http://192.168.95.229/webdav/"; // webdav url
    static final String WEBDAV_USERNAME = "admin"; // webdav username
    static final String WEBDAV_PASSWORD = "admin"; // webdav password

    public static void main(String[] args) throws IOException {

        WebdavService webdavService = new WebdavServiceImpl(WEBDAV_BASE_URL, WEBDAV_USERNAME, WEBDAV_PASSWORD);

        cleanUpWebdavResourcesUnderDir(webdavService,""); // clean up all resources under WEBDAV_ROOT_DIR

        // check http://192.168.95.229/webdav/ exist or not
        check_exists(webdavService, ""); // http://192.168.95.229/webdav/ should be exist

        // create a directory AAA under http://192.168.95.229/webdav/
        webdavService.createDirectory("AAA");
        check_exists(webdavService, "AAA"); // http://192.168.95.229/webdav/ is exists

        // Delete AAA directory under http://192.168.95.229/webdav/
        webdavService.deleteDirectory("AAA");
        check_exists(webdavService, "AAA"); // http://192.168.95.229/webdav/AAA does not exists

        // Upload file to Webdav server
        // upload file /model/test.xml to http://ip/webdav/a/b/c/test.xml;
        String xml_file = "a/b/c/test.xml";
        InputStream xmlInputStream = getFileFromResourceAsStream("/model/test.xml");
        webdavService.uploadFile(xml_file, xmlInputStream);
        check_exists(webdavService, xml_file); // http://192.168.95.229/webdav/a/b/c/test.xml is exists

        // Copy file "http://192.168.95.229/webdav/a/b/c/test.xml" to "http://192.168.95.229/webdav/aa/bb/cc/test.xml"
        String des_file = "aa/bb/cc/test.xml";
        webdavService.copy(xml_file, des_file, true);
        check_exists(webdavService, des_file); // http://192.168.95.229/webdav/aa/bb/cc/test.xml is exists

        // Download file "http://192.168.95.229/webdav/aa/bb/cc/test.xml" from webdav
        String downloaded_file_content = webdavService.getFileAsString(des_file);
        System.out.println("Downloaded file from webdav: " + downloaded_file_content);

        // Copy file "http://192.168.95.229/webdav/a/b/c/test.xml" to "http://192.168.95.229/webdav/aaa/bbb/ccc/test.xml"
        String moved_file = "aaa/bbb/ccc/test.xml";
        webdavService.move(xml_file, moved_file, true);
        check_exists(webdavService, xml_file); // http://192.168.95.229/webdav/a/b/c/test.xml does not exists
        check_exists(webdavService, moved_file); // http://192.168.95.229/webdav/aaa/bbb/ccc/test.xml is exists

        // Lock & Unlock file on http://192.168.95.229/webdav/aaa/bbb/ccc/test.xml
        // Lock
        String token = webdavService.lock(moved_file);
        System.out.println("Look token for file http://192.168.95.229/webdav/aaa/bbb/ccc/test.xml is: " +  token);

        // Unlock
        webdavService.unlock(moved_file, token);
    }

    private static void check_exists(WebdavService webdavService, String path) {
        boolean exist = webdavService.exists(path); // check http://ip/webdav/<path> -> should be true
        System.out.println(exist ? WEBDAV_BASE_URL + path + " is exists" : WEBDAV_BASE_URL + path + " does not exists" );
    }

    private static InputStream getFileFromResourceAsStream(String filePath) {
        return Demo.class.getResourceAsStream(filePath);
    }

    private static void cleanUpWebdavResourcesUnderDir(WebdavService webdavService, String dir) throws IOException {
        List<String> list = webdavService.listFolder(dir);
        for (String s : list) {
            if (webdavService.exists(s)) {
                if (s.contains(".")) {
                    webdavService.deleteFile(s);
                } else {
                    webdavService.deleteDirectory(s);
                }
            }
        }
    }
}
