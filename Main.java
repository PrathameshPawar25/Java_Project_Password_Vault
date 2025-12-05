import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String VAULT_FILENAME = "vault.dat";

    public static void main(String[] args) {
        System.out.println("=== Simple Password Vault (local) ===");
        Path vaultPath = Paths.get(VAULT_FILENAME);
        Vault vault = new Vault(vaultPath);
        Scanner sc = new Scanner(System.in);

        try {
            if (!vault.exists()) {
                System.out.println("No vault found. Create new? (y/n)");
                String a = sc.nextLine().trim();
                if (!a.equalsIgnoreCase("y")) {
                    System.out.println("Aborting.");
                    return;
                }
                char[] master = promptPassword("Set master password: ", sc);
                char[] confirm = promptPassword("Confirm master password: ", sc);
                if (!String.valueOf(master).equals(String.valueOf(confirm))) {
                    System.out.println("Passwords do not match. Aborting.");
                    return;
                }
                vault.createNew(master);
                System.out.println("Vault created at: " + vaultPath.toAbsolutePath());
            }

            // existing vault: unlock
            boolean unlocked = false;
            for (int tries = 0; tries < 3 && !unlocked; tries++) {
                char[] master = promptPassword("Enter master password: ", sc);
                try {
                    vault.load(master);
                    unlocked = true;
                } catch (Exception ex) {
                    System.out.println("Failed to open vault (bad password or corrupted). Attempts left: " + (2-tries));
                    if (tries == 2) {
                        System.out.println("Too many failed attempts. Exiting.");
                        return;
                    }
                }
            }

            // main loop
            while (true) {
                System.out.println("\nCommands: add | list | get | delete | changepw | export | exit");
                System.out.print("> ");
                String cmd = sc.nextLine().trim();
                if (cmd.equalsIgnoreCase("add")) {
                    System.out.print("Title: ");
                    String title = sc.nextLine().trim();
                    System.out.print("Username: ");
                    String user = sc.nextLine().trim();
                    String pass = new String(promptPassword("Password (input hidden): ", sc));
                    System.out.print("Note (optional): ");
                    String note = sc.nextLine();
                    vault.addEntry(new Entry(title, user, pass, note));
                    System.out.println("Entry added.");
                } else if (cmd.equalsIgnoreCase("list")) {
                    List<Entry> list = vault.listEntries();
                    if (list.isEmpty()) System.out.println("(empty)");
                    else {
                        System.out.println("Entries:");
                        for (Entry e : list) System.out.println("- " + e.getTitle());
                    }
                } else if (cmd.equalsIgnoreCase("get")) {
                    System.out.print("Title to retrieve: ");
                    String t = sc.nextLine().trim();
                    Entry e = vault.findByTitle(t);
                    if (e == null) System.out.println("Not found.");
                    else System.out.println(e);
                } else if (cmd.equalsIgnoreCase("delete")) {
                    System.out.print("Title to delete: ");
                    String t = sc.nextLine().trim();
                    boolean ok = vault.deleteByTitle(t);
                    System.out.println(ok ? "Deleted." : "Not found.");
                } else if (cmd.equalsIgnoreCase("changepw")) {
                    char[] newpw = promptPassword("New master password: ", sc);
                    char[] confirm = promptPassword("Confirm new master password: ", sc);
                    if (!String.valueOf(newpw).equals(String.valueOf(confirm))) {
                        System.out.println("Passwords do not match.");
                    } else {
                        vault.changeMasterPassword(newpw);
                        System.out.println("Master password changed.");
                    }
                } else if (cmd.equalsIgnoreCase("export")) {
                    System.out.print("Export file name (plaintext JSON): ");
                    String out = sc.nextLine().trim();
                    // build a plain text export (not encrypted) - warning shown
                    System.out.println("WARNING: This will write plaintext to disk. Continue? (y/n)");
                    String yes = sc.nextLine().trim();
                    if (yes.equalsIgnoreCase("y")) {
                        java.nio.file.Files.writeString(Paths.get(out), buildPlainExport(vault), java.nio.charset.StandardCharsets.UTF_8);
                        System.out.println("Exported to " + out);
                    } else System.out.println("Cancelled.");
                } else if (cmd.equalsIgnoreCase("exit")) {
                    System.out.println("Bye.");
                    return;
                } else {
                    System.out.println("Unknown command.");
                }
            }

        } catch (Exception e) {
            System.err.println("Fatal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    private static char[] promptPassword(String prompt, Scanner sc) {
        // Attempt to use System.console to hide input; fallback to visible input if not available
        try {
            java.io.Console console = System.console();
            if (console != null) {
                return console.readPassword(prompt);
            } else {
                System.out.print(prompt);
                String s = sc.nextLine();
                return s.toCharArray();
            }
        } catch (Exception ex) {
            System.out.print(prompt);
            String s = sc.nextLine();
            return s.toCharArray();
        }
    }

    private static String buildPlainExport(Vault vault) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"entries\": [\n");
        List<Entry> list = vault.listEntries();
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            sb.append("    {\n");
            sb.append("      \"title\": ").append(jsonEsc(e.getTitle())).append(",\n");
            sb.append("      \"username\": ").append(jsonEsc(e.getUsername())).append(",\n");
            sb.append("      \"password\": ").append(jsonEsc(e.getPassword())).append(",\n");
            sb.append("      \"note\": ").append(jsonEsc(e.getNote())).append("\n");
            sb.append("    }").append(i+1<list.size()? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static String jsonEsc(String s) {
        if (s == null) return "\"\"";
        String esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + esc + "\"";
    }
}
