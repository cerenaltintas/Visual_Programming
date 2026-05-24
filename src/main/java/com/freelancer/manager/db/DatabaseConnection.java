package com.freelancer.manager.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:freelancer_manager.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Veritabanı bağlantı hatası: " + e.getMessage());
        }
        return conn;
    }

    public static void createNewTable() {
        String sqlGorevler = "CREATE TABLE IF NOT EXISTS gorevler (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " proje_adi TEXT NOT NULL,\n"
                + " musteri TEXT NOT NULL,\n"
                + " iletisim TEXT DEFAULT '',\n"
                + " kategori TEXT DEFAULT 'Genel',\n"
                + " teslim_tarihi TEXT NOT NULL,\n"
                + " durum TEXT NOT NULL,\n"
                + " ucret REAL DEFAULT 0.0,\n"
                + " oncelik TEXT DEFAULT 'Orta',\n"
                + " notlar TEXT DEFAULT '',\n"
                + " etiketler TEXT DEFAULT '',\n"
                + " ilerleme INTEGER DEFAULT 0\n"
                + ");";

        String sqlActivityLog = "CREATE TABLE IF NOT EXISTS activity_log (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " action TEXT NOT NULL,\n"
                + " detail TEXT DEFAULT '',\n"
                + " timestamp TEXT NOT NULL\n"
                + ");";

        String sqlSettings = "CREATE TABLE IF NOT EXISTS settings (\n"
                + " key   TEXT PRIMARY KEY,\n"
                + " value TEXT DEFAULT ''\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sqlGorevler);
                stmt.execute(sqlActivityLog);
                stmt.execute(sqlSettings);

                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN ucret REAL DEFAULT 0.0;"); } catch (SQLException ignored) { }
                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN oncelik TEXT DEFAULT 'Orta';"); } catch (SQLException ignored) { }
                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN iletisim TEXT DEFAULT '';"); } catch (SQLException ignored) { }
                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN kategori TEXT DEFAULT 'Genel';"); } catch (SQLException ignored) { }
                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN notlar TEXT DEFAULT '';"); } catch (SQLException ignored) { }
                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN etiketler TEXT DEFAULT '';"); } catch (SQLException ignored) { }
                try { stmt.execute("ALTER TABLE gorevler ADD COLUMN ilerleme INTEGER DEFAULT 0;"); } catch (SQLException ignored) { }

                // Mevcut "Bitti" görevlerin ilerlemesini 100'e düzelt
                stmt.execute("UPDATE gorevler SET ilerleme = 100 WHERE durum = 'Bitti' AND ilerleme < 100;");

                System.out.println("Veritabanı tabloları kontrol edildi/güncellendi.");
            }
        } catch (SQLException e) {
            System.out.println("Tablo oluşturma hatası: " + e.getMessage());
        }
    }
}
