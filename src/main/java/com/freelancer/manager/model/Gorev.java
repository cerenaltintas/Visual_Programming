package com.freelancer.manager.model;

public class Gorev {
    private int id;
    private String projeAdi;
    private String musteri;
    private String iletisim;
    private String kategori;
    private String teslimTarihi;
    private String durum; 
    private double ucret; 
    private String oncelik; 
    private String notlar; 
    private String etiketler; 
    private int ilerleme;

    public Gorev() {
    }

    public Gorev(int id, String projeAdi, String musteri, String iletisim, String kategori, String teslimTarihi, String durum, double ucret, String oncelik, String notlar, String etiketler, int ilerleme) {
        this.id = id;
        this.projeAdi = projeAdi;
        this.musteri = musteri;
        this.iletisim = iletisim;
        this.kategori = kategori;
        this.teslimTarihi = teslimTarihi;
        this.durum = durum;
        this.ucret = ucret;
        this.oncelik = oncelik;
        this.notlar = notlar;
        this.etiketler = etiketler;
        this.ilerleme = ilerleme;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProjeAdi() { return projeAdi; }
    public void setProjeAdi(String projeAdi) { this.projeAdi = projeAdi; }

    public String getMusteri() { return musteri; }
    public void setMusteri(String musteri) { this.musteri = musteri; }

    public String getIletisim() { return iletisim; }
    public void setIletisim(String iletisim) { this.iletisim = iletisim; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getTeslimTarihi() { return teslimTarihi; }
    public void setTeslimTarihi(String teslimTarihi) { this.teslimTarihi = teslimTarihi; }

    public String getDurum() { return durum; }
    public void setDurum(String durum) { this.durum = durum; }

    public double getUcret() { return ucret; }
    public void setUcret(double ucret) { this.ucret = ucret; }

    public String getOncelik() { return oncelik; }
    public void setOncelik(String oncelik) { this.oncelik = oncelik; }
    
    public String getNotlar() { return notlar; }
    public void setNotlar(String notlar) { this.notlar = notlar; }

    public String getEtiketler() { return etiketler; }
    public void setEtiketler(String etiketler) { this.etiketler = etiketler; }
    
    public int getIlerleme() { return ilerleme; }
    public void setIlerleme(int ilerleme) { this.ilerleme = ilerleme; }
    
    @Override
    public String toString() {
        return projeAdi + " (" + kategori + ")";
    }
}
