package com.freelancer.manager.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.freelancer.manager.dao.GorevDAO;
import com.freelancer.manager.model.Gorev;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainFrame extends JFrame {

    // --- Tablo & Veri ---
    private JTable table;
    private DefaultTableModel tableModel;
    private GorevDAO dao;
    private TableRowSorter<DefaultTableModel> rowSorter;

    // --- Dashboard etiketleri ---
    private JLabel lblTotal, lblTodo, lblInProgress, lblDone, lblTotalBudget;
    private PieChartPanel pieChartPanel;

    // --- Filtreler ---
    private JTextField txtSearch;
    private String currentCategoryFilter = "Tümü";
    private String currentPriorityFilter = "Tümü";

    // --- Durum ---
    private boolean isDarkMode = false;
    private boolean startupReminderShown = false;

    // --- UI Bileşenleri ---
    private JLabel lblActivityLog;
    private JTextArea txtActivityLog;
    private Timer pomodoroTimer;
    private Timer reminderTimer;
    private int pSeconds = 0;
    private JLabel lblPomodoro;
    private JButton btnPomodoro;
    private JPanel rightPanelDeadlines;
    private JTextArea txtScratch;
    private javax.swing.border.TitledBorder scratchpadBorder;
    private Timer scratchpadSaveTimer;

    // --- Öncelik filtre butonu referansları ---
    private JButton activePriorityBtn = null;

    public MainFrame() {
        dao = new GorevDAO();
        setTitle("ProPortal - Kurumsal Yönetim & Ajanda Sistemi");
        setSize(1500, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        setupShortcuts();
        loadData();
        setupReminderTimer();
        String savedNote = dao.getSetting("scratchpad");
        if (savedNote != null && !savedNote.isEmpty()) txtScratch.setText(savedNote);
    }

    // =========================================================
    //  UI KURULUMU
    // =========================================================
    private void initUI() {
        setLayout(new BorderLayout());

        // ---- SOL MENÜ (Sidebar) ----
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(30, 15, 30, 15));

        JLabel logo = new JLabel("⚡ ProPortal");
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Kategori başlığı
        JLabel lblCat = new JLabel("  KATEGORİ");
        lblCat.setForeground(new Color(149, 165, 166));
        lblCat.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblCat.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblCat);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        String[] menuItems = {"🏠 Ana Sayfa", "🎓 Okul", "💼 İş", "👤 Kişisel", "📝 Genel"};
        for (String item : menuItems) {
            JButton btnMenu = createSidebarButton(item);
            btnMenu.addActionListener(e -> {
                if (item.contains("Ana Sayfa")) currentCategoryFilter = "Tümü";
                else if (item.contains("Okul")) currentCategoryFilter = "Okul";
                else if (item.contains("İş")) currentCategoryFilter = "İş";
                else if (item.contains("Kişisel")) currentCategoryFilter = "Kişisel";
                else if (item.contains("Genel")) currentCategoryFilter = "Genel";
                applyFilters();
            });
            sidebar.add(btnMenu);
            sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        // Öncelik filtre başlığı
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        JLabel lblPri = new JLabel("  ÖNCELİK FİLTRESİ");
        lblPri.setForeground(new Color(149, 165, 166));
        lblPri.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblPri.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblPri);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        String[][] priorityItems = {
            {"🔘 Tümü",   "Tümü"},
            {"🔴 Yüksek", "Yüksek"},
            {"🟡 Orta",   "Orta"},
            {"🔵 Düşük",  "Düşük"}
        };
        for (String[] pri : priorityItems) {
            JButton btnP = createSidebarButton(pri[0]);
            btnP.addActionListener(e -> {
                currentPriorityFilter = pri[1];
                if (activePriorityBtn != null) activePriorityBtn.setBackground(new Color(52, 73, 94));
                btnP.setBackground(new Color(26, 188, 156));
                activePriorityBtn = btnP;
                applyFilters();
            });
            sidebar.add(btnP);
            sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
            if (pri[1].equals("Tümü")) {
                activePriorityBtn = btnP;
                btnP.setBackground(new Color(26, 188, 156));
            }
        }

        sidebar.add(Box.createVerticalGlue());

        JButton btnThemeToggle = createSidebarButton("🌙 Karanlık Mod");
        btnThemeToggle.addActionListener(e -> toggleTheme(btnThemeToggle));
        sidebar.add(btnThemeToggle);
        add(sidebar, BorderLayout.WEST);

        // ---- ORTA İÇERİK ----
        JPanel centerContainer = new JPanel(new BorderLayout(15, 15));
        centerContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // Üst: Pomodoro + Dashboard
        JPanel topContainer = new JPanel(new BorderLayout(15, 0));

        JPanel pomodoroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pomodoroPanel.setOpaque(false);
        lblPomodoro = new JLabel("25:00");
        lblPomodoro.setFont(new Font("Monospaced", Font.BOLD, 24));
        lblPomodoro.setForeground(new Color(192, 57, 43));
        btnPomodoro = createStyledButton("▶ Odaklan", new Color(39, 174, 96));

        pSeconds = 25 * 60;
        pomodoroTimer = new Timer(1000, e -> {
            if (pSeconds > 0) {
                pSeconds--;
                lblPomodoro.setText(String.format("%02d:%02d", pSeconds / 60, pSeconds % 60));
            } else {
                pomodoroTimer.stop();
                showSystemNotification("Süre Doldu!", "Mükemmel iş çıkardınız. Biraz mola verin.");
                btnPomodoro.setText("▶ Odaklan");
                pSeconds = 25 * 60;
                lblPomodoro.setText("25:00");
            }
        });

        btnPomodoro.addActionListener(e -> {
            if (pomodoroTimer.isRunning()) {
                pomodoroTimer.stop();
                btnPomodoro.setText("▶ Devam Et");
                btnPomodoro.setBackground(new Color(39, 174, 96));
                logActivity("Odaklanma sayacı durduruldu.");
            } else {
                pomodoroTimer.start();
                btnPomodoro.setText("⏸ Duraklat");
                btnPomodoro.setBackground(new Color(243, 156, 18));
                logActivity("Odaklanma sayacı başlatıldı.");
            }
        });
        pomodoroPanel.add(new JLabel("⏱️ Pomodoro: "));
        pomodoroPanel.add(lblPomodoro);
        pomodoroPanel.add(btnPomodoro);
        topContainer.add(pomodoroPanel, BorderLayout.WEST);

        // Dashboard istatistik kartları
        JPanel dashboardStats = new JPanel(new GridLayout(1, 5, 15, 0));
        lblTotal = createDashboardLabel("Toplam", "0", new Color(41, 128, 185));
        lblTodo = createDashboardLabel("Bekleyen", "0", new Color(231, 76, 60));
        lblInProgress = createDashboardLabel("Devam Eden", "0", new Color(243, 156, 18));
        lblDone = createDashboardLabel("Tamamlanan", "0", new Color(39, 174, 96));
        lblTotalBudget = createDashboardLabel("Toplam Bütçe", "0 ₺", new Color(142, 68, 173));

        dashboardStats.add(lblTotal.getParent());
        dashboardStats.add(lblTodo.getParent());
        dashboardStats.add(lblInProgress.getParent());
        dashboardStats.add(lblDone.getParent());
        dashboardStats.add(lblTotalBudget.getParent());
        topContainer.add(dashboardStats, BorderLayout.CENTER);

        pieChartPanel = new PieChartPanel();
        topContainer.add(pieChartPanel, BorderLayout.EAST);
        centerContainer.add(topContainer, BorderLayout.NORTH);

        // ---- TABLO ----
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        JLabel lblSearch = new JLabel("🔍 Ara: ");
        lblSearch.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtSearch = new JTextField(30);
        txtSearch.putClientProperty("JTextField.placeholderText", "Görev, kişi veya etiket...");
        txtSearch.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchPanel.add(lblSearch, BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);

        // Kolon isimleri: "Kalan Gün" yeni eklendi
        String[] columnNames = {"ID", "Kategori", "Proje", "Bütçe", "Tarih & Saat", "Öncelik", "Etiketler", "İlerleme", "Durum", "Kalan Gün"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        // Tablo: Önceliğe göre satır renklendirme
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    int modelRow = convertRowIndexToModel(row);
                    Object pVal = tableModel.getValueAt(modelRow, 5);
                    String priority = pVal != null ? pVal.toString() : "";
                    Color rowColor;
                    switch (priority) {
                        case "Yüksek": rowColor = new Color(255, 235, 235); break;
                        case "Orta":   rowColor = new Color(255, 252, 230); break;
                        case "Düşük":  rowColor = new Color(235, 245, 255); break;
                        default:       rowColor = getBackground();
                    }
                    c.setBackground(rowColor);
                    c.setForeground(getForeground());
                }
                return c;
            }
        };

        table.getTableHeader().setReorderingAllowed(true);
        table.setRowHeight(35);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        table.getColumnModel().getColumn(7).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(120);
        table.getColumnModel().getColumn(9).setPreferredWidth(90);

        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilters(); }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) editSelectedRow();
            }
        });

        // İlerleme çubuğu renderer (kolon 7)
        table.getColumnModel().getColumn(7).setCellRenderer(new TableCellRenderer() {
            JPanel panel = new JPanel(new BorderLayout());
            JProgressBar pb = new JProgressBar(0, 100);
            {
                pb.setStringPainted(true);
                pb.setFont(new Font("SansSerif", Font.BOLD, 12));
                panel.add(pb, BorderLayout.CENTER);
                panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Integer) {
                    int val = (Integer) value;
                    pb.setValue(val);
                    if (val == 100) pb.setForeground(new Color(39, 174, 96));
                    else if (val > 0) pb.setForeground(new Color(41, 128, 185));
                    else pb.setForeground(Color.GRAY);
                }
                panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return panel;
            }
        });

        // Öncelik renderer (kolon 5)
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    switch (value.toString()) {
                        case "Yüksek": setText("🔴 Yüksek"); break;
                        case "Orta":   setText("🟡 Orta");   break;
                        case "Düşük":  setText("🔵 Düşük");  break;
                    }
                }
                return this;
            }
        });

        // Durum badge renderer (kolon 8)
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                JLabel badge = new JLabel(value != null ? value.toString() : "");
                badge.setOpaque(true);
                badge.setFont(new Font("SansSerif", Font.BOLD, 12));
                badge.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                if (value != null) {
                    switch (value.toString()) {
                        case "Yapılacak":    badge.setBackground(new Color(250, 219, 216)); badge.setForeground(new Color(192, 57, 43)); break;
                        case "Devam Ediyor": badge.setBackground(new Color(252, 243, 207)); badge.setForeground(new Color(211, 84, 0));  break;
                        case "Bitti":        badge.setBackground(new Color(213, 245, 227)); badge.setForeground(new Color(39, 174, 96)); break;
                    }
                }
                panel.add(badge);
                return panel;
            }
        });

        // Kalan Gün renderer (kolon 9)
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected && value != null) {
                    String v = value.toString();
                    setFont(getFont().deriveFont(Font.PLAIN));
                    if (v.startsWith("✓")) {
                        setForeground(new Color(39, 174, 96));
                    } else if (v.startsWith("Gecikti")) {
                        setForeground(new Color(192, 57, 43));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (v.equals("Bugün!")) {
                        setForeground(new Color(192, 57, 43));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (v.endsWith("gün")) {
                        try {
                            int days = Integer.parseInt(v.replace(" gün", "").trim());
                            if (days <= 3) { setForeground(new Color(231, 76, 60)); setFont(getFont().deriveFont(Font.BOLD)); }
                            else if (days <= 7) setForeground(new Color(211, 84, 0));
                            else setForeground(new Color(39, 174, 96));
                        } catch (NumberFormatException ex) {
                            setForeground(getForeground());
                        }
                    }
                }
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        tableContainer.add(searchPanel, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // Buton paneli — Excel kaldırıldı, Yenile eklendi
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        JButton btnPDF     = createStyledButton("📑 PDF Raporu", new Color(192, 57, 43));
        JButton btnYenile  = createStyledButton("🔄 Yenile (F5)", new Color(52, 152, 219));
        JButton btnSil     = createStyledButton("🗑️ Sil (Del)", new Color(231, 76, 60));
        JButton btnDuzenle = createStyledButton("✏️ Düzenle", new Color(243, 156, 18));
        JButton btnEkle    = createStyledButton("➕ Yeni Kayıt (Ctrl+N)", new Color(41, 128, 185));

        buttonPanel.add(btnPDF);
        buttonPanel.add(btnYenile);
        buttonPanel.add(btnSil);
        buttonPanel.add(btnDuzenle);
        buttonPanel.add(btnEkle);
        tableContainer.add(buttonPanel, BorderLayout.SOUTH);
        centerContainer.add(tableContainer, BorderLayout.CENTER);

        btnEkle.addActionListener(e -> showGorevDialog(null));
        btnDuzenle.addActionListener(e -> editSelectedRow());
        btnSil.addActionListener(e -> deleteSelectedRow());
        btnYenile.addActionListener(e -> { loadData(); logActivity("Veriler yenilendi."); });
        btnPDF.addActionListener(e -> exportToPDF());

        // ---- SAĞ PANEL ----
        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.setPreferredSize(new Dimension(300, 0));

        // Yaklaşan teslimler
        JPanel pnlDeadlines = new JPanel(new BorderLayout());
        pnlDeadlines.setBorder(BorderFactory.createTitledBorder("⏰ Yaklaşan Teslimler (< 7 Gün)"));
        rightPanelDeadlines = new JPanel();
        rightPanelDeadlines.setLayout(new BoxLayout(rightPanelDeadlines, BoxLayout.Y_AXIS));
        pnlDeadlines.add(new JScrollPane(rightPanelDeadlines), BorderLayout.CENTER);

        // Hızlı notlar (Markdown Scratchpad)
        JPanel pnlScratchpad = new JPanel(new BorderLayout());
        scratchpadBorder = BorderFactory.createTitledBorder("📝 Hızlı Karalama");
        pnlScratchpad.setBorder(scratchpadBorder);
        txtScratch = new JTextArea();
        txtScratch.setLineWrap(true);
        txtScratch.setWrapStyleWord(true);
        txtScratch.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JPanel pnlNotesButtons = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton btnClearMD = new JButton("🗑️ Temizle");
        btnClearMD.setBackground(new Color(149, 165, 166)); btnClearMD.setForeground(Color.WHITE); btnClearMD.setFocusPainted(false);
        btnClearMD.addActionListener(e -> txtScratch.setText(""));
        JButton btnExportMD = new JButton("📥 İndir (.md)");
        btnExportMD.setBackground(new Color(52, 73, 94)); btnExportMD.setForeground(Color.WHITE); btnExportMD.setFocusPainted(false);
        btnExportMD.addActionListener(e -> exportMarkdown());
        pnlNotesButtons.add(btnClearMD); pnlNotesButtons.add(btnExportMD);
        pnlScratchpad.add(new JScrollPane(txtScratch), BorderLayout.CENTER);
        pnlScratchpad.add(pnlNotesButtons, BorderLayout.SOUTH);

        // Otomatik kayıt: 2 sn hareketsizlikte SQLite'a yazar
        scratchpadSaveTimer = new Timer(2000, e -> {
            dao.setSetting("scratchpad", txtScratch.getText());
            scratchpadBorder.setTitle("📝 Hızlı Karalama ✓");
            pnlScratchpad.repaint();
            new Timer(2000, ev -> {
                scratchpadBorder.setTitle("📝 Hızlı Karalama");
                pnlScratchpad.repaint();
                ((Timer) ev.getSource()).stop();
            }) {{ setRepeats(false); start(); }};
            scratchpadSaveTimer.stop();
        });
        scratchpadSaveTimer.setRepeats(false);
        txtScratch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() { scratchpadSaveTimer.restart(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // Son Aktiviteler (Activity Log)
        JPanel pnlActivityLog = new JPanel(new BorderLayout());
        pnlActivityLog.setBorder(BorderFactory.createTitledBorder("🕐 Son Aktiviteler"));
        txtActivityLog = new JTextArea();
        txtActivityLog.setEditable(false);
        txtActivityLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtActivityLog.setLineWrap(true);
        txtActivityLog.setWrapStyleWord(true);
        txtActivityLog.setBackground(new Color(245, 245, 245));
        pnlActivityLog.add(new JScrollPane(txtActivityLog), BorderLayout.CENTER);

        // Sağ panel: 3 bölge — nested JSplitPane (fare ile sürüklenebilir)
        JSplitPane innerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlScratchpad, pnlActivityLog);
        innerSplit.setResizeWeight(0.60);
        innerSplit.setDividerSize(8);
        innerSplit.setOneTouchExpandable(true);
        innerSplit.setBorder(null);

        JSplitPane outerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlDeadlines, innerSplit);
        outerSplit.setResizeWeight(0.35);
        outerSplit.setDividerSize(8);
        outerSplit.setOneTouchExpandable(true);
        outerSplit.setBorder(null);

        rightPanel.add(outerSplit);

        // Ana bölünmüş panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerContainer, rightPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerLocation(1100);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // ---- ALT DURUM ÇUBUĞU ----
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        lblActivityLog = new JLabel("📝 Sistem Hazır.");
        lblActivityLog.setFont(new Font("SansSerif", Font.ITALIC, 12));
        JLabel lblDb = new JLabel("🟢 Veritabanı: Bağlı");
        lblDb.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusBar.add(lblActivityLog, BorderLayout.WEST);
        statusBar.add(lblDb, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    // =========================================================
    //  KISAYOLLAR
    // =========================================================
    private void setupShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "yeni");
        am.put("yeni", new AbstractAction() { public void actionPerformed(ActionEvent e) { showGorevDialog(null); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "focus");
        am.put("focus", new AbstractAction() { public void actionPerformed(ActionEvent e) { txtSearch.requestFocusInWindow(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "yenile");
        am.put("yenile", new AbstractAction() { public void actionPerformed(ActionEvent e) { loadData(); logActivity("Veriler yenilendi (F5)."); } });

        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "del");
        table.getActionMap().put("del", new AbstractAction() { public void actionPerformed(ActionEvent e) { deleteSelectedRow(); } });
    }

    // =========================================================
    //  HATIRLATıCı SİSTEMİ
    // =========================================================
    private void setupReminderTimer() {
        // Her 30 dakikada bir bugün teslimi olan görevleri kontrol et
        reminderTimer = new Timer(30 * 60 * 1000, e -> checkDailyReminders());
        reminderTimer.start();
    }

    private void checkStartupReminders(List<Gorev> gorevler) {
        if (startupReminderShown) return;
        startupReminderShown = true;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Date now = new Date();
        StringBuilder sb = new StringBuilder();

        for (Gorev g : gorevler) {
            if ("Bitti".equals(g.getDurum())) continue;
            Date deadline = parseTeslimTarihi(g.getTeslimTarihi());
            if (deadline == null) continue;
            long diffDays = TimeUnit.DAYS.convert(deadline.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
            if (diffDays >= 0 && diffDays <= 3) {
                sb.append("⚠️  ").append(g.getProjeAdi())
                  .append("  →  ").append(g.getTeslimTarihi())
                  .append("  (").append(diffDays == 0 ? "Bugün!" : diffDays + " gün kaldı").append(")\n");
            }
        }

        if (sb.length() > 0) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this,
                    "Aşağıdaki görevlerin teslim tarihi yaklaşıyor:\n\n" + sb.toString(),
                    "⏰ Yaklaşan Teslim Uyarısı",
                    JOptionPane.WARNING_MESSAGE));
        }
    }

    private void checkDailyReminders() {
        List<Gorev> gorevler = dao.getAllGorevler();
        Date now = new Date();
        StringBuilder sb = new StringBuilder();
        for (Gorev g : gorevler) {
            if ("Bitti".equals(g.getDurum())) continue;
            Date deadline = parseTeslimTarihi(g.getTeslimTarihi());
            if (deadline == null) continue;
            long diffDays = TimeUnit.DAYS.convert(deadline.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
            if (diffDays == 0) sb.append("• ").append(g.getProjeAdi()).append("\n");
        }
        if (sb.length() > 0) {
            showSystemNotification("Bugün Teslim!", "Teslim edilmesi gereken görevler:\n" + sb.toString());
        }
    }

    // =========================================================
    //  VERİ YÜKLEME
    // =========================================================
    private void loadData() {
        tableModel.setRowCount(0);
        rightPanelDeadlines.removeAll();

        List<Gorev> gorevler = dao.getAllGorevler();
        int todo = 0, inProg = 0, done = 0;
        double totalBudget = 0.0;
        Date now = new Date();

        for (Gorev g : gorevler) {
            String kalanGunStr = computeKalanGun(g, now);
            boolean isIs = "İş".equals(g.getKategori());
            String budgetStr = isIs ? String.format("%.2f ₺", g.getUcret()) : "-";
            Object[] row = {
                g.getId(), g.getKategori(), g.getProjeAdi(),
                budgetStr,
                g.getTeslimTarihi(), g.getOncelik(), g.getEtiketler(),
                g.getIlerleme(), g.getDurum(), kalanGunStr
            };
            tableModel.addRow(row);
            if (isIs) totalBudget += g.getUcret();

            if ("Yapılacak".equals(g.getDurum())) todo++;
            else if ("Devam Ediyor".equals(g.getDurum())) inProg++;
            else if ("Bitti".equals(g.getDurum())) done++;

            // Yaklaşan teslimler paneli
            if (!"Bitti".equals(g.getDurum())) {
                Date deadline = parseTeslimTarihi(g.getTeslimTarihi());
                if (deadline != null) {
                    long diff = TimeUnit.DAYS.convert(deadline.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
                    if (diff >= 0 && diff <= 7) addDeadlineCard(g, diff);
                }
            }
        }

        if (rightPanelDeadlines.getComponentCount() == 0) {
            JLabel lblEmpty = new JLabel("Yaklaşan teslim yok.");
            lblEmpty.setForeground(Color.GRAY);
            lblEmpty.setFont(new Font("SansSerif", Font.ITALIC, 13));
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            rightPanelDeadlines.add(lblEmpty);
        }
        rightPanelDeadlines.revalidate();
        rightPanelDeadlines.repaint();

        if (lblTotal != null) {
            lblTotal.setText(String.valueOf(gorevler.size()));
            lblTodo.setText(String.valueOf(todo));
            lblInProgress.setText(String.valueOf(inProg));
            lblDone.setText(String.valueOf(done));
            lblTotalBudget.setText(String.format("%.2f ₺", totalBudget));
            if (pieChartPanel != null) pieChartPanel.updateData(todo, inProg, done);
        }

        refreshActivityLog();
        checkStartupReminders(gorevler);
    }

    private String computeKalanGun(Gorev g, Date now) {
        if ("Bitti".equals(g.getDurum())) return "✓ Tamamlandı";
        Date deadline = parseTeslimTarihi(g.getTeslimTarihi());
        if (deadline == null) return "-";
        long diffDays = TimeUnit.DAYS.convert(deadline.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
        if (diffDays < 0) return "Gecikti! " + Math.abs(diffDays) + "g";
        if (diffDays == 0) return "Bugün!";
        return diffDays + " gün";
    }

    private Date parseTeslimTarihi(String teslimTarihi) {
        if (teslimTarihi == null || !teslimTarihi.contains(".")) return null;
        try {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(teslimTarihi);
        } catch (Exception e1) {
            try {
                return new SimpleDateFormat("dd.MM.yyyy").parse(teslimTarihi);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private void addDeadlineCard(Gorev g, long diff) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(192, 57, 43), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel lblTitle = new JLabel("⚠️ " + g.getProjeAdi());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel lblDays = new JLabel(diff == 0 ? "Bugün!" : "Son " + diff + " Gün");
        lblDays.setForeground(new Color(192, 57, 43));
        lblDays.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblDays.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(lblTitle, BorderLayout.CENTER);
        card.add(lblDays, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showGorevDialog(g); }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(231, 76, 60), 2, true),
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(192, 57, 43), 1, true),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });

        rightPanelDeadlines.add(card);
        rightPanelDeadlines.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    // =========================================================
    //  AKTİVİTE LOG
    // =========================================================
    private void refreshActivityLog() {
        if (txtActivityLog == null) return;
        List<String> activities = dao.getRecentActivities(6);
        StringBuilder sb = new StringBuilder();
        for (String a : activities) sb.append(a).append("\n");
        txtActivityLog.setText(sb.toString().trim());
        txtActivityLog.setCaretPosition(0);
    }

    private void logActivity(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        lblActivityLog.setText("📝 Son İşlem [" + time + "]: " + msg);
    }

    private void logImportantActivity(String action, String detail) {
        logActivity(action + ": " + detail);
        dao.logActivity(action, detail);
        refreshActivityLog();
    }

    // =========================================================
    //  GÖREV DİALOGU (Ekle / Düzenle)
    // =========================================================
    private void showGorevDialog(Gorev gorev) {
        JDialog dialog = new JDialog(this, gorev == null ? "Yeni Kayıt" : "Düzenle", true);
        dialog.setSize(680, 620);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        // Alanlar
        String[] kategoriler = {"Okul", "İş", "Kişisel", "Genel"};
        JComboBox<String> cmbKategori = new JComboBox<>(kategoriler);
        if (gorev != null && gorev.getKategori() != null) cmbKategori.setSelectedItem(gorev.getKategori());

        JTextField txtProjeAdi = new JTextField(gorev != null ? gorev.getProjeAdi() : "");
        JTextField txtMusteri  = new JTextField(gorev != null ? gorev.getMusteri()   : "");
        JTextField txtUcret    = new JTextField(gorev != null ? String.valueOf(gorev.getUcret()) : "0.0");
        JTextField txtEtiketler = new JTextField(gorev != null ? gorev.getEtiketler() : "");

        // İlerleme spinner
        JSpinner spnIlerleme = new JSpinner(new SpinnerNumberModel(gorev != null ? gorev.getIlerleme() : 0, 0, 100, 5));

        // Tarih + Saat spinner (SpinnerDateModel)
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner spnTarih = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spnTarih, "dd.MM.yyyy HH:mm");
        spnTarih.setEditor(dateEditor);
        if (gorev != null && gorev.getTeslimTarihi() != null && !gorev.getTeslimTarihi().isEmpty()) {
            Date parsedDate = parseTeslimTarihi(gorev.getTeslimTarihi());
            spnTarih.setValue(parsedDate != null ? parsedDate : new Date());
        }

        JComboBox<String> cmbOncelik = new JComboBox<>(new String[]{"Düşük", "Orta", "Yüksek"});
        if (gorev != null && gorev.getOncelik() != null) cmbOncelik.setSelectedItem(gorev.getOncelik());

        JComboBox<String> cmbDurum = new JComboBox<>(new String[]{"Yapılacak", "Devam Ediyor", "Bitti"});
        if (gorev != null && gorev.getDurum() != null) cmbDurum.setSelectedItem(gorev.getDurum());

        JTextArea txtNotlar = new JTextArea(gorev != null ? gorev.getNotlar() : "", 4, 20);
        txtNotlar.setLineWrap(true); txtNotlar.setWrapStyleWord(true);

        // Durum ↔ İlerleme otomatik senkronizasyonu
        cmbDurum.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && "Bitti".equals(e.getItem())) {
                spnIlerleme.setValue(100);
            }
        });
        spnIlerleme.addChangeListener(e -> {
            if ((Integer) spnIlerleme.getValue() == 100) {
                cmbDurum.setSelectedItem("Bitti");
            } else if ("Bitti".equals(cmbDurum.getSelectedItem())) {
                cmbDurum.setSelectedItem("Devam Ediyor");
            }
        });

        // Bütçe alanı: sadece "İş" kategorisinde gösterilir
        JPanel pnlUcretWrapper = new JPanel(new BorderLayout());
        pnlUcretWrapper.add(txtUcret, BorderLayout.CENTER);
        pnlUcretWrapper.setPreferredSize(new Dimension(140, 28));

        JPanel pnlRow3 = new JPanel(new BorderLayout(5, 0));
        pnlRow3.add(spnTarih, BorderLayout.CENTER);

        JLabel lblRow3 = new JLabel("Tarih & Saat:");

        boolean isIsBaslangic = "İş".equals(cmbKategori.getSelectedItem());
        if (isIsBaslangic) {
            pnlRow3.add(pnlUcretWrapper, BorderLayout.WEST);
            lblRow3.setText("Bütçe (₺) / Tarih & Saat:");
        } else {
            txtUcret.setText("0.0");
        }

        cmbKategori.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isIs = "İş".equals(e.getItem());
                if (isIs) {
                    pnlRow3.add(pnlUcretWrapper, BorderLayout.WEST);
                    lblRow3.setText("Bütçe (₺) / Tarih & Saat:");
                } else {
                    pnlRow3.remove(pnlUcretWrapper);
                    lblRow3.setText("Tarih & Saat:");
                    txtUcret.setText("0.0");
                }
                pnlRow3.revalidate();
                pnlRow3.repaint();
            }
        });

        // Layout
        int row = 0;
        gbc.gridy = row++; mainPanel.add(new JLabel("Kategori / Proje Adı:"), gbc);
        JPanel pnl1 = new JPanel(new GridLayout(1, 2, 5, 0)); pnl1.add(cmbKategori); pnl1.add(txtProjeAdi);
        gbc.gridy = row++; mainPanel.add(pnl1, gbc);

        gbc.gridy = row++; mainPanel.add(new JLabel("Etiketler (#tag) / Kişi:"), gbc);
        JPanel pnl2 = new JPanel(new GridLayout(1, 2, 5, 0)); pnl2.add(txtEtiketler); pnl2.add(txtMusteri);
        gbc.gridy = row++; mainPanel.add(pnl2, gbc);

        gbc.gridy = row++; mainPanel.add(lblRow3, gbc);
        gbc.gridy = row++; mainPanel.add(pnlRow3, gbc);

        gbc.gridy = row++; mainPanel.add(new JLabel("İlerleme (%):"), gbc);
        gbc.gridy = row++; mainPanel.add(spnIlerleme, gbc);

        gbc.gridy = row++; mainPanel.add(new JLabel("Öncelik & Durum:"), gbc);
        JPanel pnlDurum = new JPanel(new GridLayout(1, 2, 10, 0)); pnlDurum.add(cmbOncelik); pnlDurum.add(cmbDurum);
        gbc.gridy = row++; mainPanel.add(pnlDurum, gbc);

        gbc.gridy = row++; mainPanel.add(new JLabel("Notlar / Açıklama:"), gbc);
        gbc.gridy = row++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JScrollPane(txtNotlar), gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnKaydet = createStyledButton("Kaydet", new Color(41, 128, 185));
        JButton btnIptal  = createStyledButton("İptal", new Color(149, 165, 166));
        btnPanel.add(btnIptal); btnPanel.add(btnKaydet);

        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnKaydet.addActionListener(e -> {
            try {
                double ucretVal = txtUcret.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtUcret.getText().trim());
                String teslimTarihi = new SimpleDateFormat("dd.MM.yyyy HH:mm").format((Date) spnTarih.getValue());
                String yeniDurum = (String) cmbDurum.getSelectedItem();
                int ilerleme = (Integer) spnIlerleme.getValue();

                if (gorev == null) {
                    Gorev yeni = new Gorev(0, txtProjeAdi.getText(), txtMusteri.getText(), "",
                            (String) cmbKategori.getSelectedItem(), teslimTarihi, yeniDurum, ucretVal,
                            (String) cmbOncelik.getSelectedItem(), txtNotlar.getText(), txtEtiketler.getText(), ilerleme);
                    dao.addGorev(yeni);
                    logImportantActivity("Görev Eklendi", txtProjeAdi.getText());
                    if ("Bitti".equals(yeniDurum)) triggerWebhook(txtProjeAdi.getText());
                } else {
                    String eskiDurum = gorev.getDurum();
                    gorev.setProjeAdi(txtProjeAdi.getText()); gorev.setMusteri(txtMusteri.getText());
                    gorev.setUcret(ucretVal); gorev.setTeslimTarihi(teslimTarihi); gorev.setDurum(yeniDurum);
                    gorev.setNotlar(txtNotlar.getText()); gorev.setEtiketler(txtEtiketler.getText());
                    gorev.setIlerleme(ilerleme); gorev.setOncelik((String) cmbOncelik.getSelectedItem());
                    dao.updateGorev(gorev);
                    logImportantActivity("Görev Güncellendi", txtProjeAdi.getText());
                    if (!"Bitti".equals(eskiDurum) && "Bitti".equals(yeniDurum)) triggerWebhook(txtProjeAdi.getText());
                }
                dialog.dispose();
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Hata: " + ex.getMessage(), "Giriş Hatası", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnIptal.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    // =========================================================
    //  TABLO İŞLEMLERİ
    // =========================================================
    private void editSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            int id = (int) tableModel.getValueAt(modelRow, 0);
            Gorev gorev = getGorevFromDB(id);
            if (gorev != null) showGorevDialog(gorev);
        }
    }

    private void deleteSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            int id = (int) tableModel.getValueAt(modelRow, 0);
            String projeAdi = tableModel.getValueAt(modelRow, 2).toString();
            if (JOptionPane.showConfirmDialog(this,
                    "\"" + projeAdi + "\" kalıcı olarak silinecek, emin misiniz?",
                    "Onay", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                dao.deleteGorev(id);
                logImportantActivity("Görev Silindi", projeAdi);
                loadData();
            }
        }
    }

    private Gorev getGorevFromDB(int id) {
        for (Gorev g : dao.getAllGorevler()) if (g.getId() == id) return g;
        return null;
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().trim();
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!searchText.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + searchText));
        if (!"Tümü".equals(currentCategoryFilter)) filters.add(RowFilter.regexFilter("^" + currentCategoryFilter + "$", 1));
        if (!"Tümü".equals(currentPriorityFilter)) filters.add(RowFilter.regexFilter("^" + currentPriorityFilter + "$", 5));

        rowSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    // =========================================================
    //  PDF EXPORT
    // =========================================================
    private void exportToPDF() {
        logActivity("PDF raporu oluşturuluyor...");
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (!f.getAbsolutePath().endsWith(".pdf")) f = new File(f.getAbsolutePath() + ".pdf");
                Document doc = new Document();
                PdfWriter.getInstance(doc, new FileOutputStream(f));
                doc.open();
                com.itextpdf.text.Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, com.itextpdf.text.BaseColor.BLACK);
                Paragraph title = new Paragraph("ProPortal Raporu", fTitle);
                title.setAlignment(Element.ALIGN_CENTER); title.setSpacingAfter(20);
                doc.add(title);
                doc.add(new Paragraph("Tarih: " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date())));
                doc.add(new Paragraph(" "));
                PdfPTable pt = new PdfPTable(new float[]{1, 3, 2, 1, 2});
                pt.setWidthPercentage(100);
                for (String h : new String[]{"ID", "Proje", "Tarih & Saat", "Öncelik", "Durum"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h));
                    c.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                    pt.addCell(c);
                }
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    pt.addCell(tableModel.getValueAt(i, 0).toString());
                    pt.addCell(tableModel.getValueAt(i, 2).toString());
                    pt.addCell(tableModel.getValueAt(i, 4).toString());
                    pt.addCell(tableModel.getValueAt(i, 5).toString());
                    pt.addCell(tableModel.getValueAt(i, 8).toString());
                }
                doc.add(pt); doc.close();
                logImportantActivity("PDF Raporu", f.getName());
                JOptionPane.showMessageDialog(this, "PDF başarıyla oluşturuldu.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "PDF oluşturma hatası: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    //  MARKDOWN EXPORT
    // =========================================================
    private void exportMarkdown() {
        try {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Markdown Olarak Kaydet");
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (!f.getAbsolutePath().endsWith(".md")) f = new File(f.getAbsolutePath() + ".md");
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(txtScratch.getText());
                }
                logActivity("Markdown dışa aktarıldı.");
                JOptionPane.showMessageDialog(this, "Notlarınız kaydedildi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            logActivity("Markdown dışa aktarma hatası: " + ex.getMessage());
        }
    }

    // =========================================================
    //  TEMA
    // =========================================================
    private void toggleTheme(JButton btnThemeToggle) {
        isDarkMode = !isDarkMode;
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                btnThemeToggle.setText("☀️ Aydınlık Mod");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                btnThemeToggle.setText("🌙 Karanlık Mod");
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================
    //  SİSTEM BİLDİRİMİ
    // =========================================================
    private void showSystemNotification(String title, String message) {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                g2d.setColor(new Color(41, 128, 185));
                g2d.fillOval(0, 0, 16, 16);
                g2d.dispose();
                TrayIcon trayIcon = new TrayIcon(img, "ProPortal");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                new Timer(4000, e -> { tray.remove(trayIcon); ((Timer) e.getSource()).stop(); }).start();
            }
        } catch (Exception ex) {
            System.out.println("SystemTray: " + ex.getMessage());
        }
    }

    // =========================================================
    //  WEBHOOK SİMÜLASYONU
    // =========================================================
    private void triggerWebhook(String taskName) {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                SwingUtilities.invokeLater(() -> logActivity("Webhook gönderildi: " + taskName));
            } catch (Exception ignored) {}
        }).start();
    }

    // =========================================================
    //  UI YARDIMCI METODlar
    // =========================================================
    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(52, 73, 94));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!btn.getBackground().equals(new Color(26, 188, 156))) btn.setBackground(new Color(26, 188, 156)); }
            public void mouseExited(MouseEvent e)  { if (!btn.getBackground().equals(new Color(26, 188, 156))) btn.setBackground(new Color(52, 73, 94)); }
        });
        return btn;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createDashboardLabel(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setToolTipText(title);
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblVal.setForeground(color);
        lblVal.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(lblVal, BorderLayout.SOUTH);
        return lblVal;
    }
}
