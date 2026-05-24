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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainFrame extends JFrame {

    // --- Tablo & Veri ---
    private JTable table;
    private DefaultTableModel tableModel;
    private GorevDAO dao;
    private TableRowSorter<DefaultTableModel> rowSorter;

    // --- Dashboard etiketleri ---
    private JLabel lblTotal, lblTodo, lblInProgress, lblDone;
    private JLabel lblBudgetTotal, lblBudgetDone, lblBudgetInProg, lblBudgetTodo;
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
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        setupShortcuts();
        loadData();
        setupReminderTimer();
        String savedNote = dao.getSetting("scratchpad");
        if (savedNote != null && !savedNote.isEmpty()) txtScratch.setText(savedNote);
        // Pazartesi sabahı haftalık özet — 2 sn gecikmeyle göster
        SwingUtilities.invokeLater(() -> new Timer(2000, e -> {
            checkWeeklySummary(false);
            ((Timer) e.getSource()).stop();
        }) {{ setRepeats(false); start(); }});
    }

    // =========================================================
    //  UI KURULUMU
    // =========================================================
    private void initUI() {
        setLayout(new BorderLayout());

        // Pomodoro bileşenleri sidebar'dan önce oluşturulmalı
        lblPomodoro = new JLabel("25:00");
        btnPomodoro = createStyledButton("▶ Odaklan", new Color(39, 174, 96));

        // ---- SOL MENÜ (Sidebar) ----
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(210, 0));
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

        // Pomodoro sidebar bölümü
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel pomSideLabel = new JLabel("  ⏱ POMODORO");
        pomSideLabel.setForeground(new Color(149, 165, 166));
        pomSideLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        pomSideLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(pomSideLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        lblPomodoro.setFont(new Font("Monospaced", Font.BOLD, 22));
        lblPomodoro.setForeground(new Color(231, 76, 60));
        lblPomodoro.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblPomodoro);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        btnPomodoro.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPomodoro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        sidebar.add(btnPomodoro);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        JButton btnPomStat = createSidebarButton("📈 Pomodoro İstatistik");
        btnPomStat.addActionListener(e -> showPomodoroStats());
        sidebar.add(btnPomStat);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        JButton btnWeekly = createSidebarButton("📋 Haftalık Özet");
        btnWeekly.addActionListener(e -> checkWeeklySummary(true));
        sidebar.add(btnWeekly);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnThemeToggle = createSidebarButton("🌙 Karanlık Mod");
        btnThemeToggle.addActionListener(e -> toggleTheme(btnThemeToggle));
        sidebar.add(btnThemeToggle);
        add(sidebar, BorderLayout.WEST);

        // ---- ORTA İÇERİK ----
        JPanel centerContainer = new JPanel(new BorderLayout(8, 8));
        centerContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Üst: Pomodoro + Dashboard
        JPanel topContainer = new JPanel(new BorderLayout(5, 0));

        JPanel pomodoroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pomodoroPanel.setOpaque(false);

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
                // Pomodoro tamamlanmasını DB'ye kaydet
                String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                String key = "pom_" + today;
                String val = dao.getSetting(key);
                int count = (val != null && !val.isEmpty()) ? Integer.parseInt(val) : 0;
                dao.setSetting(key, String.valueOf(count + 1));
                logActivity("Pomodoro tamamlandı! (" + (count + 1) + ". oturum)");
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
        // Dashboard istatistik kartları — tüm genişliği kullanır
        JPanel dashboardStats = new JPanel(new GridLayout(1, 5, 8, 0));
        lblTotal      = createDashboardLabel("Toplam",     "0", new Color(41,  128, 185), 28);
        lblTodo       = createDashboardLabel("Bekleyen",   "0", new Color(231,  76,  60), 28);
        lblInProgress = createDashboardLabel("Devam Eden", "0", new Color(243, 156,  18), 28);
        lblDone       = createDashboardLabel("Tamamlanan", "0", new Color(39,  174,  96), 28);

        // Toplam Bütçe kartı — tek kutu, 4 satır
        JPanel budgetCard = new JPanel(new BorderLayout(0, 2));
        budgetCard.setOpaque(false);
        budgetCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel budgetTitle = new JLabel("Toplam Bütçe");
        budgetTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        budgetTitle.setForeground(Color.GRAY);
        budgetTitle.setHorizontalAlignment(SwingConstants.CENTER);

        lblBudgetTotal = new JLabel("0 ₺");
        lblBudgetTotal.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblBudgetTotal.setForeground(new Color(142, 68, 173));
        lblBudgetTotal.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel budgetRows = new JPanel(new GridLayout(3, 1, 0, 1));
        budgetRows.setOpaque(false);

        lblBudgetDone   = new JLabel("✓ Bitti: 0 ₺");
        lblBudgetInProg = new JLabel("▶ Devam: 0 ₺");
        lblBudgetTodo   = new JLabel("○ Bekleyen: 0 ₺");

        for (JLabel lbl : new JLabel[]{lblBudgetDone, lblBudgetInProg, lblBudgetTodo}) {
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
        }
        lblBudgetDone.setForeground(new Color(39, 174, 96));
        lblBudgetInProg.setForeground(new Color(211, 84, 0));
        lblBudgetTodo.setForeground(new Color(192, 57, 43));

        budgetRows.add(lblBudgetDone);
        budgetRows.add(lblBudgetInProg);
        budgetRows.add(lblBudgetTodo);

        budgetCard.add(budgetTitle,   BorderLayout.NORTH);
        budgetCard.add(lblBudgetTotal, BorderLayout.CENTER);
        budgetCard.add(budgetRows,    BorderLayout.SOUTH);

        dashboardStats.add(lblTotal.getParent());
        dashboardStats.add(lblTodo.getParent());
        dashboardStats.add(lblInProgress.getParent());
        dashboardStats.add(lblDone.getParent());
        dashboardStats.add(budgetCard);
        topContainer.add(dashboardStats, BorderLayout.CENTER);

        pieChartPanel = new PieChartPanel();
        // Pie chart sağ panelle birlikte kaldırıldı
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

        // Buton paneli
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnGelir   = createStyledButton("📊 Gelir Grafiği", new Color(142, 68, 173));
        JButton btnGantt   = createStyledButton("📅 Gantt", new Color(22, 160, 133));
        JButton btnPDF     = createStyledButton("📑 PDF Raporu", new Color(192, 57, 43));
        JButton btnYenile  = createStyledButton("🔄 Yenile (F5)", new Color(52, 152, 219));
        JButton btnSil     = createStyledButton("🗑️ Sil (Del)", new Color(231, 76, 60));
        JButton btnDuzenle = createStyledButton("✏️ Düzenle", new Color(243, 156, 18));
        JButton btnEkle    = createStyledButton("➕ Yeni Kayıt (Ctrl+N)", new Color(41, 128, 185));

        buttonPanel.add(btnGelir);
        buttonPanel.add(btnGantt);
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
        btnGelir.addActionListener(e -> showGelirGrafigi());
        btnGantt.addActionListener(e -> showGanttDialog());

        // Scratchpad (gizli — sadece otomatik kayıt için tutulur, UI'da gösterilmez)
        scratchpadBorder = BorderFactory.createTitledBorder("");
        txtScratch = new JTextArea();
        scratchpadSaveTimer = new Timer(2000, e -> {
            dao.setSetting("scratchpad", txtScratch.getText());
            scratchpadSaveTimer.stop();
        });
        scratchpadSaveTimer.setRepeats(false);
        txtScratch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() { scratchpadSaveTimer.restart(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // Activity log alanı (gizli)
        txtActivityLog = new JTextArea();
        txtActivityLog.setEditable(false);

        // Yaklaşan teslimler paneli (gizli)
        rightPanelDeadlines = new JPanel();
        rightPanelDeadlines.setLayout(new BoxLayout(rightPanelDeadlines, BoxLayout.Y_AXIS));

        add(centerContainer, BorderLayout.CENTER);

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
        double budgetTodo = 0.0, budgetInProg = 0.0, budgetDone = 0.0;
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

            if ("Yapılacak".equals(g.getDurum())) { todo++; if (isIs) budgetTodo += g.getUcret(); }
            else if ("Devam Ediyor".equals(g.getDurum())) { inProg++; if (isIs) budgetInProg += g.getUcret(); }
            else if ("Bitti".equals(g.getDurum())) { done++; if (isIs) budgetDone += g.getUcret(); }

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
            lblBudgetTotal.setText(String.format("%,.0f ₺", budgetTodo + budgetInProg + budgetDone));
            lblBudgetDone.setText(  "✓ Bitti: "    + String.format("%,.0f ₺", budgetDone));
            lblBudgetInProg.setText("▶ Devam: "    + String.format("%,.0f ₺", budgetInProg));
            lblBudgetTodo.setText(  "○ Bekleyen: " + String.format("%,.0f ₺", budgetTodo));
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

        JTextField txtProjeAdi  = new JTextField(gorev != null ? gorev.getProjeAdi()  : "");
        JTextField txtMusteri   = new JTextField(gorev != null ? gorev.getMusteri()   : "");
        JTextField txtUcret     = new JTextField(gorev != null ? String.valueOf(gorev.getUcret()) : "0.0");
        JTextField txtEtiketler = new JTextField(gorev != null ? gorev.getEtiketler() : "");

        // --- Karakter limitleri ---
        limitText(txtProjeAdi,  100);   // zorunlu, max 100
        limitText(txtMusteri,    50);   // max 50
        limitText(txtEtiketler, 100);   // max 100
        // Bütçe: sadece sayısal, 0 – 9 999 999; geçersiz girişte uyarı göster
        ((javax.swing.text.AbstractDocument) txtUcret.getDocument())
                .setDocumentFilter(new javax.swing.text.DocumentFilter() {
            private boolean valid(String s) {
                if (s.isEmpty()) return true;
                try { double v = Double.parseDouble(s); return v >= 0 && v <= 9_999_999; }
                catch (NumberFormatException ex) { return s.matches("[0-9]*\\.?"); }
            }
            private void warn(String reason) {
                txtUcret.setBorder(BorderFactory.createLineBorder(new Color(231, 76, 60), 2));
                txtUcret.setToolTipText(reason);
                javax.swing.ToolTipManager.sharedInstance().mouseMoved(
                        new java.awt.event.MouseEvent(txtUcret, 0, 0, 0, 5, 5, 0, false));
                new Timer(1500, ev -> {
                    txtUcret.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
                    txtUcret.setToolTipText(null);
                    ((Timer) ev.getSource()).stop();
                }).start();
            }
            public void insertString(FilterBypass fb, int off, String text, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, off) + text + cur.substring(off);
                if (valid(next)) super.insertString(fb, off, text, a);
                else warn(next.matches(".*[^0-9.].*") ? "Sadece rakam ve nokta girilebilir." : "Bütçe en fazla 9.999.999 ₺ olabilir.");
            }
            public void replace(FilterBypass fb, int off, int len, String text, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, off) + (text != null ? text : "") + cur.substring(off + len);
                if (valid(next)) super.replace(fb, off, len, text, a);
                else warn(next.matches(".*[^0-9.].*") ? "Sadece rakam ve nokta girilebilir." : "Bütçe en fazla 9.999.999 ₺ olabilir.");
            }
        });

        // İlerleme spinner — 0..100 arası, yazarak girildiğinde da sınırlanır
        JSpinner spnIlerleme = new JSpinner(new SpinnerNumberModel(gorev != null ? gorev.getIlerleme() : 0, 0, 100, 5));
        JFormattedTextField spnEditor = ((JSpinner.DefaultEditor) spnIlerleme.getEditor()).getTextField();
        JLabel lblIlerlemWarn = new JLabel(" ");
        lblIlerlemWarn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblIlerlemWarn.setForeground(new Color(231, 76, 60));
        // focusLost'ta formatter değeri otomatik düzeltmeden ÖNCE ham metni sakla
        String[] lastRawSpn = {String.valueOf(gorev != null ? gorev.getIlerleme() : 0)};
        spnEditor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                lastRawSpn[0] = spnEditor.getText().trim();
            }
        });
        spnEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void check() {
                lastRawSpn[0] = spnEditor.getText().trim();
                String t = lastRawSpn[0];
                boolean bad;
                try { int v = Integer.parseInt(t); bad = (v < 0 || v > 100); }
                catch (NumberFormatException ex) { bad = !t.isEmpty(); }
                if (bad) {
                    spnEditor.setBorder(BorderFactory.createLineBorder(new Color(231, 76, 60), 2));
                    lblIlerlemWarn.setText("Geçerli değer girin: 0 ile 100 arasında olmalıdır.");
                } else {
                    spnEditor.setBorder(null);
                    lblIlerlemWarn.setText(" ");
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { check(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { check(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { check(); }
        });

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
        limitText(txtNotlar, 500);
        JLabel lblNotCount = new JLabel((gorev != null && gorev.getNotlar() != null ? gorev.getNotlar().length() : 0) + "/500");
        lblNotCount.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblNotCount.setForeground(Color.GRAY);
        lblNotCount.setHorizontalAlignment(SwingConstants.RIGHT);
        txtNotlar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                int len = txtNotlar.getText().length();
                lblNotCount.setText(len + "/500");
                lblNotCount.setForeground(len >= 480 ? new Color(231, 76, 60) : Color.GRAY);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // Durum ↔ İlerleme tam çift yönlü senkronizasyon
        boolean[] syncing = {false}; // döngüsel tetiklenmeyi önle
        cmbDurum.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED || syncing[0]) return;
            syncing[0] = true;
            switch (e.getItem().toString()) {
                case "Yapılacak":    spnIlerleme.setValue(0);   break;
                case "Devam Ediyor":
                    int cur = (Integer) spnIlerleme.getValue();
                    if (cur == 0 || cur == 100) spnIlerleme.setValue(50);
                    break;
                case "Bitti":        spnIlerleme.setValue(100); break;
            }
            syncing[0] = false;
        });
        spnIlerleme.addChangeListener(e -> {
            if (syncing[0]) return;
            syncing[0] = true;
            int val = (Integer) spnIlerleme.getValue();
            if (val == 0)        cmbDurum.setSelectedItem("Yapılacak");
            else if (val == 100) cmbDurum.setSelectedItem("Bitti");
            else                 cmbDurum.setSelectedItem("Devam Ediyor");
            syncing[0] = false;
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
        gbc.gridy = row++; mainPanel.add(lblIlerlemWarn, gbc);

        gbc.gridy = row++; mainPanel.add(new JLabel("Öncelik & Durum:"), gbc);
        JPanel pnlDurum = new JPanel(new GridLayout(1, 2, 10, 0)); pnlDurum.add(cmbOncelik); pnlDurum.add(cmbDurum);
        gbc.gridy = row++; mainPanel.add(pnlDurum, gbc);

        gbc.gridy = row++; mainPanel.add(new JLabel("Notlar / Açıklama:"), gbc);
        gbc.gridy = row++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JScrollPane(txtNotlar), gbc);
        gbc.gridy = row++; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(lblNotCount, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnKaydet = createStyledButton("Kaydet", new Color(41, 128, 185));
        JButton btnIptal  = createStyledButton("İptal", new Color(149, 165, 166));
        btnPanel.add(btnIptal); btnPanel.add(btnKaydet);

        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnKaydet.addActionListener(e -> {
            // --- Validasyon ---
            boolean ilerlemeBad;
            try { int iv = Integer.parseInt(lastRawSpn[0]); ilerlemeBad = (iv < 0 || iv > 100); }
            catch (NumberFormatException ex) { ilerlemeBad = true; }
            if (ilerlemeBad) {
                lblIlerlemWarn.setText("Geçerli değer girin: 0 ile 100 arasında olmalıdır.");
                spnEditor.setBorder(BorderFactory.createLineBorder(new Color(231, 76, 60), 2));
                JOptionPane.showMessageDialog(dialog, "Lütfen geçerli değer girin.\nİlerleme 0 ile 100 arasında olmalıdır.", "Geçersiz Değer", JOptionPane.WARNING_MESSAGE);
                spnEditor.requestFocus(); return;
            }
            String projeAdiVal = txtProjeAdi.getText().trim();
            if (projeAdiVal.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Proje adı boş olamaz.", "Eksik Bilgi", JOptionPane.WARNING_MESSAGE);
                txtProjeAdi.requestFocus(); return;
            }
            String ucretStr = txtUcret.getText().trim();
            double ucretVal = 0.0;
            if (!ucretStr.isEmpty()) {
                try {
                    ucretVal = Double.parseDouble(ucretStr);
                    if (ucretVal < 0 || ucretVal > 9_999_999) {
                        JOptionPane.showMessageDialog(dialog, "Bütçe 0 ile 9.999.999 ₺ arasında olmalıdır.", "Geçersiz Değer", JOptionPane.WARNING_MESSAGE);
                        txtUcret.requestFocus(); return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Bütçe geçerli bir sayı olmalıdır.", "Geçersiz Değer", JOptionPane.WARNING_MESSAGE);
                    txtUcret.requestFocus(); return;
                }
            }
            try {
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

    // =========================================================
    //  AYLIK GELİR GRAFİĞİ
    // =========================================================
    private void showGelirGrafigi() {
        List<Gorev> gorevler = dao.getAllGorevler();
        // Son 12 ayı hesapla
        LinkedHashMap<String, Double> aylikGelir = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat ayFmt = new SimpleDateFormat("MMM yy", new Locale("tr"));
        SimpleDateFormat parseFmt = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        // Son 12 ayı sıralı ekle (boş bile olsa göster)
        for (int i = 11; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, -i);
            String key = new SimpleDateFormat("yyyy-MM", new Locale("tr")).format(c.getTime());
            aylikGelir.put(key, 0.0);
        }
        for (Gorev g : gorevler) {
            if (!"Bitti".equals(g.getDurum()) || !"İş".equals(g.getKategori())) continue;
            if (g.getTeslimTarihi() == null || g.getTeslimTarihi().isEmpty()) continue;
            try {
                Date d = parseFmt.parse(g.getTeslimTarihi());
                String key = new SimpleDateFormat("yyyy-MM").format(d);
                if (aylikGelir.containsKey(key))
                    aylikGelir.put(key, aylikGelir.get(key) + g.getUcret());
            } catch (Exception ignored) {}
        }
        // Etiket map'i (görüntülenecek ay adları)
        LinkedHashMap<String, Double> display = new LinkedHashMap<>();
        for (Map.Entry<String, Double> en : aylikGelir.entrySet()) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM").parse(en.getKey());
                display.put(ayFmt.format(d).toUpperCase(new Locale("tr")), en.getValue());
            } catch (Exception ignored) {}
        }

        JDialog dlg = new JDialog(this, "📊 Aylık Gelir Grafiği (Son 12 Ay)", true);
        dlg.setSize(900, 520);
        dlg.setLocationRelativeTo(this);
        dlg.add(new BarChartPanel(display));
        dlg.setVisible(true);
    }

    // =========================================================
    //  GANTT GÖRÜNÜMÜ
    // =========================================================
    private void showGanttDialog() {
        List<Gorev> gorevler = dao.getAllGorevler();
        JDialog dlg = new JDialog(this, "📅 Gantt — Görev Zaman Çizelgesi", true);
        dlg.setSize(1000, 550);
        dlg.setLocationRelativeTo(this);
        GanttPanel gantt = new GanttPanel(gorevler);
        dlg.add(new JScrollPane(gantt));
        dlg.setVisible(true);
    }

    // =========================================================
    //  POMODORO İSTATİSTİKLERİ
    // =========================================================
    private void showPomodoroStats() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        int totalAll = 0, thisWeek = 0, today = 0;
        String bestDay = "-"; int bestCount = 0;
        // Son 30 günü tara
        for (int i = 0; i < 30; i++) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String key = "pom_" + fmt.format(cal.getTime());
            String val = dao.getSetting(key);
            int cnt = (val != null && !val.isEmpty()) ? Integer.parseInt(val) : 0;
            totalAll += cnt;
            if (i == 0) today = cnt;
            if (i < 7) thisWeek += cnt;
            if (cnt > bestCount) {
                bestCount = cnt;
                bestDay = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr")).format(cal.getTime());
            }
        }

        JDialog dlg = new JDialog(this, "📈 Pomodoro İstatistikleri", true);
        dlg.setSize(420, 340);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        String[] icons  = {"🍅", "📅", "🗓️", "🏆"};
        String[] labels = {"Bugün", "Bu Hafta", "Son 30 Gün", "En Verimli Gün"};
        String[] values = {
            today + " oturum",
            thisWeek + " oturum",
            totalAll + " oturum",
            bestCount > 0 ? bestDay + "  (" + bestCount + " oturum)" : "Henüz yok"
        };
        Color[] colors = {
            new Color(231, 76, 60),
            new Color(41, 128, 185),
            new Color(39, 174, 96),
            new Color(243, 156, 18)
        };

        for (int i = 0; i < labels.length; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel lbl = new JLabel(icons[i] + "  " + labels[i]);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbl.setForeground(colors[i]);
            JLabel val = new JLabel(values[i]);
            val.setFont(new Font("SansSerif", Font.PLAIN, 14));
            val.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(lbl, BorderLayout.WEST);
            row.add(val, BorderLayout.EAST);
            p.add(row);
        }
        JButton close = createStyledButton("Kapat", new Color(100, 100, 100));
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dlg.add(p, BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // =========================================================
    //  HAFTALIK ÖZET
    // =========================================================
    /** force=true → her zaman göster. force=false → sadece Pazartesi ve o hafta daha gösterilmediyse. */
    private void checkWeeklySummary(boolean force) {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 2 = Pazartesi
        // Haftanın kaçıncı haftası (settings key için)
        String weekKey = "weekly_shown_" + cal.get(Calendar.YEAR) + "_W" +
                cal.get(Calendar.WEEK_OF_YEAR);
        boolean alreadyShown = "1".equals(dao.getSetting(weekKey));

        if (force || (dayOfWeek == Calendar.MONDAY && !alreadyShown)) {
            dao.setSetting(weekKey, "1");
            showWeeklySummary();
        }
    }

    private void showWeeklySummary() {
        List<Gorev> gorevler = dao.getAllGorevler();
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        // Bu haftanın başı ve sonu
        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0); weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 7);

        int tamamlanan = 0, bekleyen = 0, devamEden = 0, geciken = 0;
        double haftaKazanc = 0;
        List<String[]> haftaTeslimleri = new ArrayList<>(); // [ad, tarih, kalanGun]
        Gorev enYakin = null; long enYakinMs = Long.MAX_VALUE;

        for (Gorev g : gorevler) {
            switch (g.getDurum()) {
                case "Bitti":        tamamlanan++; break;
                case "Yapılacak":    bekleyen++;   break;
                case "Devam Ediyor": devamEden++;  break;
            }
            Date deadline = null;
            try { deadline = sdf.parse(g.getTeslimTarihi()); } catch (Exception ignored) {}
            if (deadline == null) continue;

            long diff = deadline.getTime() - now.getTime();
            // Bu haftaki teslimler (bitmemiş)
            if (!"Bitti".equals(g.getDurum()) &&
                    deadline.getTime() >= weekStart.getTimeInMillis() &&
                    deadline.getTime() < weekEnd.getTimeInMillis()) {
                long days = diff / 86400000;
                String kalanStr = days < 0 ? "Gecikti!" : days == 0 ? "Bugün!" : days + " gün";
                haftaTeslimleri.add(new String[]{g.getProjeAdi(), g.getTeslimTarihi(), kalanStr});
                if ("İş".equals(g.getKategori())) haftaKazanc += g.getUcret();
            }
            // Geciken
            if (!"Bitti".equals(g.getDurum()) && diff < 0) geciken++;
            // En yakın deadline (gelecekte)
            if (!"Bitti".equals(g.getDurum()) && diff > 0 && diff < enYakinMs) {
                enYakinMs = diff; enYakin = g;
            }
        }

        // --- Dialog ---
        JDialog dlg = new JDialog(this, "📋 Haftalık Özet", true);
        dlg.setSize(520, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(0, 0));

        // Başlık bandı
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(41, 128, 185));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        Calendar c = Calendar.getInstance();
        String haftaLabel = c.get(Calendar.YEAR) + " — " + c.get(Calendar.WEEK_OF_YEAR) + ". Hafta";
        JLabel title = new JLabel("📋 Haftalık Özet  •  " + haftaLabel);
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        dlg.add(header, BorderLayout.NORTH);

        // Ana içerik
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        // İstatistik kartları
        JPanel stats = new JPanel(new GridLayout(1, 4, 8, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        addSummaryCard(stats, "✅ Tamamlanan", String.valueOf(tamamlanan), new Color(39, 174, 96));
        addSummaryCard(stats, "⏳ Bekleyen",   String.valueOf(bekleyen),   new Color(41, 128, 185));
        addSummaryCard(stats, "▶ Devam",       String.valueOf(devamEden),  new Color(243, 156, 18));
        addSummaryCard(stats, "🚨 Geciken",    String.valueOf(geciken),    new Color(192, 57, 43));
        content.add(stats);
        content.add(Box.createRigidArea(new Dimension(0, 14)));

        // En yakın teslim
        JPanel enYakinPanel = new JPanel(new BorderLayout(8, 0));
        enYakinPanel.setOpaque(false);
        enYakinPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel enYakinLbl = new JLabel("🎯 En Yakın Teslim:");
        enYakinLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        String enYakinStr = enYakin != null
                ? enYakin.getProjeAdi() + "  →  " + enYakin.getTeslimTarihi() +
                  "  (" + (enYakinMs / 86400000) + " gün kaldı)"
                : "Yaklaşan teslim yok.";
        JLabel enYakinVal = new JLabel(enYakinStr);
        enYakinVal.setFont(new Font("SansSerif", Font.PLAIN, 13));
        enYakinVal.setForeground(enYakin != null ? new Color(192, 57, 43) : Color.GRAY);
        enYakinPanel.add(enYakinLbl, BorderLayout.WEST);
        enYakinPanel.add(enYakinVal, BorderLayout.CENTER);
        content.add(enYakinPanel);
        content.add(Box.createRigidArea(new Dimension(0, 14)));

        // Bu haftaki teslimler
        JLabel secHeader = new JLabel("📅 Bu Haftaki Teslimler (" + haftaTeslimleri.size() + ")");
        secHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
        secHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(secHeader);
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        if (haftaTeslimleri.isEmpty()) {
            JLabel empty = new JLabel("  Bu hafta teslim tarihi olan görev yok.");
            empty.setFont(new Font("SansSerif", Font.ITALIC, 12));
            empty.setForeground(Color.GRAY);
            content.add(empty);
        } else {
            JPanel listPanel = new JPanel(new GridLayout(haftaTeslimleri.size(), 1, 0, 4));
            listPanel.setOpaque(false);
            listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (String[] item : haftaTeslimleri) {
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setOpaque(false);
                JLabel nameLbl = new JLabel("• " + item[0]);
                nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                JLabel dateLbl = new JLabel(item[1]);
                dateLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
                dateLbl.setForeground(Color.GRAY);
                JLabel kalanLbl = new JLabel(item[2]);
                kalanLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                boolean urgent = item[2].equals("Bugün!") || item[2].equals("Gecikti!");
                kalanLbl.setForeground(urgent ? new Color(192, 57, 43) : new Color(39, 174, 96));
                row.add(nameLbl, BorderLayout.WEST);
                row.add(dateLbl, BorderLayout.CENTER);
                row.add(kalanLbl, BorderLayout.EAST);
                listPanel.add(row);
            }
            content.add(listPanel);
        }

        // Bu haftaki potansiyel kazanç
        if (haftaKazanc > 0) {
            content.add(Box.createRigidArea(new Dimension(0, 10)));
            JLabel kazancLbl = new JLabel("💰 Bu Haftaki İş Bütçesi: " + String.format("%,.0f ₺", haftaKazanc));
            kazancLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            kazancLbl.setForeground(new Color(142, 68, 173));
            kazancLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(kazancLbl);
        }

        dlg.add(content, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton kapat = createStyledButton("Kapat", new Color(100, 100, 100));
        kapat.addActionListener(e -> dlg.dispose());
        south.add(kapat);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void addSummaryCard(JPanel parent, String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        card.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(color);
        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(new Font("SansSerif", Font.BOLD, 24));
        val.setForeground(color);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        parent.add(card);
    }

    // =========================================================
    //  BAR CHART PANELİ (Gelir Grafiği için)
    // =========================================================
    static class BarChartPanel extends JPanel {
        private final LinkedHashMap<String, Double> data;
        BarChartPanel(LinkedHashMap<String, Double> data) {
            this.data = data;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(860, 480));
        }
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 70, top = 30, botPad = 60, w = getWidth(), h = getHeight();
            int chartW = w - pad - 20, chartH = h - top - botPad;

            double max = data.values().stream().mapToDouble(v -> v).max().orElse(1);
            if (max == 0) max = 1;

            // Grid çizgileri
            g.setColor(new Color(230, 230, 230));
            for (int i = 1; i <= 5; i++) {
                int y = top + chartH - (int)(chartH * i / 5.0);
                g.drawLine(pad, y, pad + chartW, y);
                g.setColor(Color.GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                String lbl = String.format("%,.0f ₺", max * i / 5);
                g.drawString(lbl, 4, y + 4);
                g.setColor(new Color(230, 230, 230));
            }

            // Eksen
            g.setColor(new Color(180, 180, 180));
            g.drawLine(pad, top, pad, top + chartH);
            g.drawLine(pad, top + chartH, pad + chartW, top + chartH);

            // Barlar
            String[] keys = data.keySet().toArray(new String[0]);
            int n = keys.length;
            int barW = Math.max(8, (chartW - 20) / n - 8);
            int gap  = (chartW - 20 - n * barW) / (n + 1);

            for (int i = 0; i < n; i++) {
                double val = data.get(keys[i]);
                int barH = (int)(chartH * val / max);
                int x = pad + gap + i * (barW + gap);
                int y = top + chartH - barH;

                // Gradient bar
                GradientPaint gp = new GradientPaint(x, y, new Color(142, 68, 173), x, top + chartH, new Color(187, 143, 206));
                g.setPaint(gp);
                g.fillRoundRect(x, y, barW, barH, 6, 6);

                // Değer etiketi (bar üstü)
                if (val > 0) {
                    g.setColor(new Color(80, 20, 100));
                    g.setFont(new Font("SansSerif", Font.BOLD, 9));
                    String vStr = val >= 1000 ? String.format("%,.0f", val) : String.format("%.0f", val);
                    int sw = g.getFontMetrics().stringWidth(vStr);
                    g.drawString(vStr, x + (barW - sw) / 2, Math.max(top + 12, y - 3));
                }

                // X etiketi
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                int lw = g.getFontMetrics().stringWidth(keys[i]);
                g.drawString(keys[i], x + (barW - lw) / 2, top + chartH + 16);
            }

            // Başlık
            g.setColor(new Color(80, 20, 100));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            String title = "Tamamlanan İş Görevleri — Aylık Gelir (₺)";
            g.drawString(title, pad + (chartW - g.getFontMetrics().stringWidth(title)) / 2, top - 8);
        }
    }

    // =========================================================
    //  GANTT PANELİ
    // =========================================================
    static class GanttPanel extends JPanel {
        private final List<Gorev> gorevler;
        private static final int ROW_H = 38, LEFT = 220, TOP = 40, BOT = 30;
        private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        GanttPanel(List<Gorev> gorevler) {
            this.gorevler = gorevler;
            setBackground(Color.WHITE);
        }
        @Override public Dimension getPreferredSize() {
            return new Dimension(960, TOP + gorevler.size() * ROW_H + BOT + 20);
        }
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = Math.max(960, getWidth());
            int chartW = w - LEFT - 20;

            // Tarih aralığını hesapla (bugün - 7 gün → bugün + 60 gün)
            long now = System.currentTimeMillis();
            long rangeStart = now - 7L * 86400000;
            long rangeEnd   = now + 60L * 86400000;
            for (Gorev g2 : gorevler) {
                try { Date d = SDF.parse(g2.getTeslimTarihi()); if (d != null) rangeEnd = Math.max(rangeEnd, d.getTime() + 3L * 86400000); }
                catch (Exception ignored) {}
            }
            long totalMs = rangeEnd - rangeStart;

            // Başlık
            g.setColor(new Color(30, 80, 120));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("Görev Zaman Çizelgesi", LEFT + chartW / 2 - 80, 20);

            // "Bugün" çizgisi
            int todayX = LEFT + (int)((now - rangeStart) * chartW / totalMs);
            g.setColor(new Color(231, 76, 60, 180));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6, 4}, 0));
            g.drawLine(todayX, TOP, todayX, TOP + gorevler.size() * ROW_H);
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(231, 76, 60));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString("Bugün", todayX - 18, TOP - 6);

            // Haftalık grid
            g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(rangeStart);
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            SimpleDateFormat wfmt = new SimpleDateFormat("dd MMM", new Locale("tr"));
            while (c.getTimeInMillis() < rangeEnd) {
                int x = LEFT + (int)((c.getTimeInMillis() - rangeStart) * chartW / totalMs);
                if (x >= LEFT && x <= LEFT + chartW) {
                    g.setColor(new Color(220, 220, 220));
                    g.setStroke(new BasicStroke(1));
                    g.drawLine(x, TOP, x, TOP + gorevler.size() * ROW_H);
                    g.setColor(Color.GRAY);
                    g.drawString(wfmt.format(c.getTime()), x + 2, TOP - 4);
                }
                c.add(Calendar.DAY_OF_YEAR, 7);
            }

            // Görevler
            for (int i = 0; i < gorevler.size(); i++) {
                Gorev gv = gorevler.get(i);
                int y = TOP + i * ROW_H;

                // Zemin (alternatif satır)
                g.setColor(i % 2 == 0 ? new Color(248, 248, 248) : Color.WHITE);
                g.fillRect(0, y, w, ROW_H);

                // Görev adı (sol panel)
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String name = gv.getProjeAdi();
                if (name.length() > 24) name = name.substring(0, 22) + "…";
                g.drawString(name, 8, y + ROW_H / 2 + 4);

                // Deadline barı
                Date deadline = null;
                try { deadline = SDF.parse(gv.getTeslimTarihi()); } catch (Exception ignored) {}
                if (deadline == null) continue;

                long dlMs = deadline.getTime();
                int barStart = LEFT + (int)((Math.min(now, dlMs) - rangeStart) * chartW / totalMs);
                int barEnd   = LEFT + (int)((Math.max(now, dlMs) - rangeStart) * chartW / totalMs);
                barStart = Math.max(LEFT, Math.min(barStart, LEFT + chartW));
                barEnd   = Math.max(LEFT, Math.min(barEnd,   LEFT + chartW));
                int bw = Math.max(4, barEnd - barStart);

                Color barColor;
                if ("Bitti".equals(gv.getDurum()))         barColor = new Color(39, 174, 96);
                else if (dlMs < now)                        barColor = new Color(192, 57, 43);
                else if (dlMs - now < 3L * 86400000)       barColor = new Color(231, 76, 60);
                else if (dlMs - now < 7L * 86400000)       barColor = new Color(243, 156, 18);
                else                                        barColor = new Color(41, 128, 185);

                int bh = ROW_H - 12; int by = y + 6;
                GradientPaint gp = new GradientPaint(barStart, by, barColor.brighter(), barStart, by + bh, barColor);
                g.setPaint(gp);
                g.fillRoundRect(barStart, by, bw, bh, 6, 6);

                // Tarih etiketi bar içinde/yanında
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 9));
                String dlStr = new SimpleDateFormat("dd.MM").format(deadline);
                if (bw > 30) g.drawString(dlStr, barStart + 4, by + bh - 4);
                else { g.setColor(barColor.darker()); g.drawString(dlStr, barEnd + 3, by + bh - 4); }

                // Durum rozeti (sağ)
                g.setColor(new Color(100, 100, 100));
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString(gv.getDurum(), LEFT + chartW + 3, y + ROW_H / 2 + 4);
            }

            // Sol panel kenarlığı
            g.setColor(new Color(200, 200, 200));
            g.setStroke(new BasicStroke(1));
            g.drawLine(LEFT, TOP - 10, LEFT, TOP + gorevler.size() * ROW_H);
        }
    }

    /** Sınıra ulaşıldığında kırmızı border + tooltip ile uyarı gösterir, 1.5 sn sonra sıfırlar. */
    private static void showLimitWarning(javax.swing.text.JTextComponent tc, int max) {
        tc.setBorder(BorderFactory.createLineBorder(new Color(231, 76, 60), 2));
        tc.setToolTipText("Maksimum " + max + " karakter girilebilir.");
        javax.swing.ToolTipManager.sharedInstance().mouseMoved(
                new java.awt.event.MouseEvent(tc, 0, 0, 0, 5, 5, 0, false));
        new Timer(1500, ev -> {
            tc.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
            tc.setToolTipText(null);
            ((Timer) ev.getSource()).stop();
        }).start();
    }

    /** Bir text bileşenine maksimum karakter sınırı uygular; aşımda uyarı gösterir. */
    private static void limitText(javax.swing.text.JTextComponent tc, int max) {
        ((javax.swing.text.AbstractDocument) tc.getDocument())
                .setDocumentFilter(new javax.swing.text.DocumentFilter() {
            public void insertString(FilterBypass fb, int off, String text, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (fb.getDocument().getLength() + text.length() <= max)
                    super.insertString(fb, off, text, a);
                else showLimitWarning(tc, max);
            }
            public void replace(FilterBypass fb, int off, int len, String text, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                int newLen = fb.getDocument().getLength() - len + (text != null ? text.length() : 0);
                if (newLen <= max) super.replace(fb, off, len, text, a);
                else showLimitWarning(tc, max);
            }
        });
    }

    private JLabel createDashboardLabel(String title, String value, Color color, int valueFontSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setToolTipText(title);
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, valueFontSize));
        lblVal.setForeground(color);
        lblVal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(lblVal, BorderLayout.CENTER);
        return lblVal;
    }
}
