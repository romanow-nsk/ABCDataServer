package romanow.abc.dataserver;

public class HTTPError {
    final boolean error;
    final int code;
    final String message;
    public HTTPError(int code, String message) {
        this.error = true;
        this.code = code;
        this.message = message;
        }
    public HTTPError() {
        this.error = false;
        this.code = 0;
        this.message = "";
        }
}
