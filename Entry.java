import java.util.Base64;

public class Entry {
    private String title;
    private String username;
    private String password;
    private String note;

    public Entry(String title, String username, String password, String note) {
        this.title = title;
        this.username = username;
        this.password = password;
        this.note = note;
    }

    public String getTitle() { return title; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNote() { return note; }

    // serialize as base64 fields separated by '|'
    public String toLine() {
        return b64(title) + "|" + b64(username) + "|" + b64(password) + "|" + b64(note);
    }

    public static Entry fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4) return null;
        return new Entry(d64(parts[0]), d64(parts[1]), d64(parts[2]), d64(parts[3]));
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes());
    }

    private static String d64(String s) {
        try {
            return new String(Base64.getDecoder().decode(s));
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    @Override
    public String toString() {
        return String.format("Title: %s\nUsername: %s\nPassword: %s\nNote: %s\n", title, username, password, (note == null ? "" : note));
    }
}
