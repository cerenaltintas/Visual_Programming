package com.freelancer.manager.ui;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class PieChartPanel extends JPanel {
    private Map<String, Integer> data;
    private Map<String, Color> colors;

    public PieChartPanel() {
        data = new LinkedHashMap<>();
        colors = new LinkedHashMap<>();
        setPreferredSize(new Dimension(150, 150));
        setOpaque(false);
    }

    public void updateData(int todo, int inProgress, int done) {
        data.clear();
        colors.clear();
        
        if (todo == 0 && inProgress == 0 && done == 0) {
            data.put("Boş", 1);
            colors.put("Boş", new Color(236, 240, 241));
        } else {
            if (todo > 0) {
                data.put("Yapılacak", todo);
                colors.put("Yapılacak", new Color(231, 76, 60));
            }
            if (inProgress > 0) {
                data.put("Devam Ediyor", inProgress);
                colors.put("Devam Ediyor", new Color(243, 156, 18));
            }
            if (done > 0) {
                data.put("Bitti", done);
                colors.put("Bitti", new Color(39, 174, 96));
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data.isEmpty()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - 10;
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        double currentAngle = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double angle = (entry.getValue() * 360.0) / total;
            g2d.setColor(colors.get(entry.getKey()));
            g2d.fillArc(x, y, size, size, (int) currentAngle, (int) angle);
            currentAngle += angle;
        }
        
        // Ortasını delik yap (Donut Chart stili için daha modern görünür)
        int innerSize = size / 2;
        int innerX = x + (size - innerSize) / 2;
        int innerY = y + (size - innerSize) / 2;
        
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillOval(innerX, innerY, innerSize, innerSize);
        g2d.setComposite(AlphaComposite.SrcOver);
    }
}
