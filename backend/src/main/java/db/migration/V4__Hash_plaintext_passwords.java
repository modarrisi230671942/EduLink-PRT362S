package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * V4: Security fix S3 — passwords used to be stored in plaintext.
 * Hashes every password that is not already a BCrypt hash, so existing accounts keep working
 * with the same password but the database no longer contains readable passwords.
 */
public class V4__Hash_plaintext_passwords extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Map<Integer, String> plaintext = new LinkedHashMap<>();

        try (Statement select = context.getConnection().createStatement();
             ResultSet rows = select.executeQuery("SELECT user_id, password_hash FROM users")) {
            while (rows.next()) {
                String stored = rows.getString("password_hash");
                if (!isBcrypt(stored)) {
                    plaintext.put(rows.getInt("user_id"), stored);
                }
            }
        }

        try (PreparedStatement update = context.getConnection()
                .prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
            for (Map.Entry<Integer, String> entry : plaintext.entrySet()) {
                update.setString(1, encoder.encode(entry.getValue()));
                update.setInt(2, entry.getKey());
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private static boolean isBcrypt(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
