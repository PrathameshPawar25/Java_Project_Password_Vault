import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

public class Vault {
    private final Path file;
    private byte[] salt;            // fixed per vault file
    private SecretKey key;          // derived from master password and salt
    private List<Entry> entries = new ArrayList<>();

    // file format (text): three lines
    // SALT:<base64>
    // IV:<base64>
    // DATA:<base64>
    public Vault(Path file) {
        this.file = file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public void createNew(char[] masterPassword) throws Exception {
        this.salt = CryptoUtil.generateSalt();
        this.key = CryptoUtil.deriveKey(masterPassword, salt);
        this.entries = new ArrayList<>();
        save(); // writes initial empty vault
    }

    public void load(char[] masterPassword) throws Exception {
        if (!exists()) throw new IllegalStateException("Vault file does not exist.");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8)
                .stream().filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
        if (lines.size() < 3) throw new IllegalStateException("Vault file corrupted or wrong format.");

        this.salt = Base64.getDecoder().decode(stripPrefix(lines.get(0), "SALT:"));
        byte[] iv = Base64.getDecoder().decode(stripPrefix(lines.get(1), "IV:"));
        byte[] data = Base64.getDecoder().decode(stripPrefix(lines.get(2), "DATA:"));

        this.key = CryptoUtil.deriveKey(masterPassword, salt);
        byte[] plain = CryptoUtil.decrypt(data, key, iv);
        String content = new String(plain, StandardCharsets.UTF_8);
        parseContent(content);
    }

    private String stripPrefix(String line, String prefix) {
        if (!line.startsWith(prefix)) throw new IllegalStateException("Invalid vault file format.");
        return line.substring(prefix.length());
    }

    private void parseContent(String content) {
        entries = new ArrayList<>();
        if (content.trim().isEmpty()) return;
        String[] lines = content.split("\n");
        for (String l : lines) {
            Entry e = Entry.fromLine(l);
            if (e != null) entries.add(e);
        }
    }

    private String buildContent() {
        return entries.stream().map(Entry::toLine).collect(Collectors.joining("\n"));
    }

    public void save() throws Exception {
        // encrypt current content, write salt, iv, data
        byte[] plain = buildContent().getBytes(StandardCharsets.UTF_8);
        CryptoUtil.CipherResult res = CryptoUtil.encrypt(plain, key);

        String sSalt = "SALT:" + Base64.getEncoder().encodeToString(this.salt);
        String sIv = "IV:" + Base64.getEncoder().encodeToString(res.iv);
        String sData = "DATA:" + Base64.getEncoder().encodeToString(res.cipherText);

        String out = sSalt + "\n" + sIv + "\n" + sData + "\n";
        Files.write(file, out.getBytes(StandardCharsets.UTF_8));
    }

    public void addEntry(Entry e) throws Exception {
        entries.add(e);
        save();
    }

    public List<Entry> listEntries() {
        return Collections.unmodifiableList(entries);
    }

    public Entry findByTitle(String title) {
        for (Entry e : entries) if (e.getTitle().equalsIgnoreCase(title)) return e;
        return null;
    }

    public boolean deleteByTitle(String title) throws Exception {
        Iterator<Entry> it = entries.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.getTitle().equalsIgnoreCase(title)) { it.remove(); removed = true; }
        }
        if (removed) save();
        return removed;
    }

    public void changeMasterPassword(char[] newMaster) throws Exception {
        // derive new key with same salt, replace key and save
        this.key = CryptoUtil.deriveKey(newMaster, salt);
        save();
    }
}
