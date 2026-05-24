package com.freelancer.manager;

import com.freelancer.manager.db.DatabaseConnection;
import com.freelancer.manager.ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        System.out.println("Uygulama Başlatılıyor...");
        
        // 1. Veritabanı ve tablo hazırlığı
        DatabaseConnection.createNewTable();
        
        // 2. Look and Feel (Modern Tema) Ayarlanıyor
        try {
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("FlatLaf başlatılamadı, varsayılan tema kullanılacak.");
        }

        // 3. UI (Kullanıcı Arayüzü) Tasarımı Başlatılıyor
        javax.swing.SwingUtilities.invokeLater(() -> {
            com.freelancer.manager.ui.MainFrame frame = new com.freelancer.manager.ui.MainFrame();
            frame.setVisible(true);
            frame.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
        });
        
        System.out.println("Arayüz başarıyla başlatıldı.");
    }
}
