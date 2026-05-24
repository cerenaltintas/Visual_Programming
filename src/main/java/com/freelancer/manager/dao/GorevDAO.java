package com.freelancer.manager.dao;

import com.freelancer.manager.db.DatabaseConnection;
import com.freelancer.manager.model.Gorev;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GorevDAO {

    public void addGorev(Gorev gorev) {
        String sql = "INSERT INTO gorevler(proje_adi, musteri, iletisim, kategori, teslim_tarihi, durum, ucret, oncelik, notlar, etiketler, ilerleme) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, gorev.getProjeAdi());
            pstmt.setString(2, gorev.getMusteri());
            pstmt.setString(3, gorev.getIletisim());
            pstmt.setString(4, gorev.getKategori());
            pstmt.setString(5, gorev.getTeslimTarihi());
            pstmt.setString(6, gorev.getDurum());
            pstmt.setDouble(7, gorev.getUcret());
            pstmt.setString(8, gorev.getOncelik());
            pstmt.setString(9, gorev.getNotlar());
            pstmt.setString(10, gorev.getEtiketler());
            pstmt.setInt(11, gorev.getIlerleme());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Görev ekleme hatası: " + e.getMessage());
        }
    }

    public List<Gorev> getAllGorevler() {
        String sql = "SELECT * FROM gorevler";
        List<Gorev> gorevler = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                gorevler.add(new Gorev(
                        rs.getInt("id"),
                        rs.getString("proje_adi"),
                        rs.getString("musteri"),
                        rs.getString("iletisim"),
                        rs.getString("kategori"),
                        rs.getString("teslim_tarihi"),
                        rs.getString("durum"),
                        rs.getDouble("ucret"),
                        rs.getString("oncelik"),
                        rs.getString("notlar"),
                        rs.getString("etiketler"),
                        rs.getInt("ilerleme")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Görevleri listeleme hatası: " + e.getMessage());
        }
        return gorevler;
    }

    public void updateGorev(Gorev gorev) {
        String sql = "UPDATE gorevler SET proje_adi=?, musteri=?, iletisim=?, kategori=?, teslim_tarihi=?, durum=?, ucret=?, oncelik=?, notlar=?, etiketler=?, ilerleme=? WHERE id=?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, gorev.getProjeAdi());
            pstmt.setString(2, gorev.getMusteri());
            pstmt.setString(3, gorev.getIletisim());
            pstmt.setString(4, gorev.getKategori());
            pstmt.setString(5, gorev.getTeslimTarihi());
            pstmt.setString(6, gorev.getDurum());
            pstmt.setDouble(7, gorev.getUcret());
            pstmt.setString(8, gorev.getOncelik());
            pstmt.setString(9, gorev.getNotlar());
            pstmt.setString(10, gorev.getEtiketler());
            pstmt.setInt(11, gorev.getIlerleme());
            pstmt.setInt(12, gorev.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Görev güncelleme hatası: " + e.getMessage());
        }
    }

    public void deleteGorev(int id) {
        String sql = "DELETE FROM gorevler WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Görev silme hatası: " + e.getMessage());
        }
    }

    public void logActivity(String action, String detail) {
        String sql = "INSERT INTO activity_log(action, detail, timestamp) VALUES(?,?,?)";
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, action);
            pstmt.setString(2, detail);
            pstmt.setString(3, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Activity log hatası: " + e.getMessage());
        }
    }

    public String getSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        } catch (SQLException e) {
            System.out.println("Settings okuma hatası: " + e.getMessage());
        }
        return null;
    }

    public void setSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings(key, value) VALUES(?, ?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Settings yazma hatası: " + e.getMessage());
        }
    }

    public List<String> getRecentActivities(int limit) {
        String sql = "SELECT action, detail, timestamp FROM activity_log ORDER BY id DESC LIMIT ?";
        List<String> activities = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activities.add("[" + rs.getString("timestamp") + "] "
                            + rs.getString("action") + ": " + rs.getString("detail"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Activity log okuma hatası: " + e.getMessage());
        }
        return activities;
    }
}
