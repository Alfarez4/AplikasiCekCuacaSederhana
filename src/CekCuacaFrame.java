import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.json.*;

public class CekCuacaFrame extends javax.swing.JFrame {
    
    DefaultTableModel model;

    public CekCuacaFrame() {
        initComponents();
        setLocationRelativeTo(null);
        setTitle("Aplikasi Cek Cuaca Sederhana");
        muatFavorit();

        // Buat model tabel (tidak bisa diedit)
        model = new DefaultTableModel(new String[]{"KOTA", "SUHU", "CUACA", "WAKTU"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCuaca.setModel(model);

        // Combo box awal kosong (kota favorit)
        cbKota.removeAllItems();

        // Tambahkan event tombol
        btnCek.addActionListener(e -> cekCuaca());
        btnFavorit.addActionListener(e -> tambahKotaFavorit());
        btnSimpan.addActionListener(e -> simpanCSV());
        btnMuat.addActionListener(e -> muatCSV());
        btnReset.addActionListener(e -> resetForm());
        btnKeluar.addActionListener(e -> System.exit(0));
    }

    private void cekCuaca() {
    String kota = txtKota.getText().trim();

    if (kota.isEmpty()) {
        if (cbKota.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Masukkan nama kota atau pilih dari favorit!");
            return;
        }
        kota = cbKota.getSelectedItem().toString();
    }

    try {
        // 🔒 Baca API key dari file config.properties
        String apiKey = "";
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("config.properties"));
            apiKey = prop.getProperty("API_KEY");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal membaca API key dari config.properties");
            return;
        }

        String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" + kota
                + "&appid=" + apiKey + "&units=metric&lang=id";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Kode respons: " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();

        JSONObject json = new JSONObject(response.toString());
        String kondisi = json.getJSONArray("weather").getJSONObject(0).getString("main");
        double suhu = json.getJSONObject("main").getDouble("temp");
        int kelembapan = json.getJSONObject("main").getInt("humidity");

        lblTampilCuaca.setText(kondisi);
        lblTampilSuhu.setText(String.format("%.1f °C", suhu));
        lblTampilLembap.setText(kelembapan + "%");

        tampilkanGambar(kondisi);
        model.addRow(new Object[]{kota, suhu + " °C", kondisi, new Date().toString()});

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mengambil data cuaca: " + e.getMessage());
    }
}

    private void tampilkanGambar(String kondisi) {
        String path;
        if (kondisi.toLowerCase().contains("rain")) path = "/rain.png";
        else if (kondisi.toLowerCase().contains("cloud")) path = "/cloud.png";
        else if (kondisi.toLowerCase().contains("clear")) path = "/sun.png";
        else path = "/mist.png";

        try {
            lblTampilGambar.setIcon(new ImageIcon(getClass().getResource(path)));
            lblTampilGambar.setText("");
        } catch (Exception e) {
            lblTampilGambar.setText("Gambar tidak ditemukan");
        }
    }

    private void tambahKotaFavorit() {
    String kotaBaru = JOptionPane.showInputDialog(this, "Masukkan nama kota favorit:");
    if (kotaBaru != null && !kotaBaru.trim().isEmpty()) {
        cbKota.addItem(kotaBaru.trim());
        JOptionPane.showMessageDialog(this, "Kota " + kotaBaru + " berhasil ditambahkan ke favorit!");
        simpanFavorit(); // simpan setiap kali ada kota baru
        }
    }

    private void simpanCSV() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Simpan data tabel ke file CSV");
    chooser.setSelectedFile(new File("data_cuaca.csv")); // default filename

    int userSelection = chooser.showSaveDialog(this);
    if (userSelection != JFileChooser.APPROVE_OPTION) {
        return; // user batal
    }

    File fileToSave = chooser.getSelectedFile();
    // Tambahkan ekstensi .csv jika user lupa
    if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
        fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
    }

    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToSave), "UTF-8"))) {
        // (Opsional) tulis header
        bw.write("KOTA,SUHU,CUACA,WAKTU");
        bw.newLine();

        for (int i = 0; i < model.getRowCount(); i++) {
            // Ambil 4 kolom pertama (asumsi format yang kita pakai)
            String k = String.valueOf(model.getValueAt(i, 0));
            String s = String.valueOf(model.getValueAt(i, 1));
            String c = String.valueOf(model.getValueAt(i, 2));
            String w = String.valueOf(model.getValueAt(i, 3));

            // Jika nilai mengandung koma, kita bungkus dengan quotes sederhana
            k = k.contains(",") ? "\"" + k.replace("\"", "\"\"") + "\"" : k;
            s = s.contains(",") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
            c = c.contains(",") ? "\"" + c.replace("\"", "\"\"") + "\"" : c;
            w = w.contains(",") ? "\"" + w.replace("\"", "\"\"") + "\"" : w;

            bw.write(k + "," + s + "," + c + "," + w);
            bw.newLine();
        }
        bw.flush();
        JOptionPane.showMessageDialog(this, "Berhasil menyimpan ke: " + fileToSave.getAbsolutePath());
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Gagal menyimpan file: " + e.getMessage());
    }
}

    private void muatCSV() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Pilih file CSV untuk diimpor");
    int userSelection = chooser.showOpenDialog(this);
    if (userSelection != JFileChooser.APPROVE_OPTION) {
        return; // user batal
    }

    File fileToOpen = chooser.getSelectedFile();
    if (!fileToOpen.exists() || !fileToOpen.isFile()) {
        JOptionPane.showMessageDialog(this, "File tidak ditemukan: " + fileToOpen.getAbsolutePath());
        return;
    }

    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileToOpen), "UTF-8"))) {
        String line;
        model.setRowCount(0); // kosongkan tabel sebelum impor

        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            // Jika ada header (mis. baris pertama mengandung "KOTA" atau "SUHU"), kita lewati
            if (firstLine) {
                String lower = line.toLowerCase();
                if (lower.contains("kota") && lower.contains("suhu")) {
                    firstLine = false;
                    continue; // lewati header
                }
            }
            firstLine = false;

            // Parsing CSV sederhana: mempertimbangkan kemungkinan nilai dibungkus "..." yang mengandung koma.
            // Implementasi lightweight: jika ada quotes di line, gunakan regex split sederhana.
            List<String> tokens = new ArrayList<>();
            if (line.contains("\"")) {
                // parse yang mendukung quoted values
                StringBuilder cur = new StringBuilder();
                boolean inQuotes = false;
                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    if (ch == '\"') {
                        // cek escaped quote
                        if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                            cur.append('\"');
                            i++; // lewati escaped quote
                        } else {
                            inQuotes = !inQuotes;
                        }
                    } else if (ch == ',' && !inQuotes) {
                        tokens.add(cur.toString());
                        cur.setLength(0);
                    } else {
                        cur.append(ch);
                    }
                }
                tokens.add(cur.toString());
            } else {
                // Simple split jika tidak ada quotes
                String[] parts = line.split(",");
                for (String p : parts) tokens.add(p);
            }

            // Pastikan minimal 4 kolom (jika lebih, ambil 4 pertama)
            if (tokens.size() >= 4) {
                String k = tokens.get(0).trim();
                String s = tokens.get(1).trim();
                String c = tokens.get(2).trim();
                String w = tokens.get(3).trim();
                model.addRow(new Object[]{k, s, c, w});
            } else {
                // jika format tidak sesuai, skip atau tambahkan dengan nilai kosong
                // di sini kita abaikan baris yang tidak sesuai
            }
        }
        JOptionPane.showMessageDialog(this, "Impor selesai dari: " + fileToOpen.getAbsolutePath());
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Gagal memuat file: " + e.getMessage());
    }
}

    private void resetForm() {
    // Hapus isi input dan label
    txtKota.setText("");
    lblTampilCuaca.setText("-");
    lblTampilSuhu.setText("-");
    lblTampilLembap.setText("-");
    lblTampilGambar.setIcon(null);
    lblTampilGambar.setText("-");

    // Hapus semua data di tabel
    model.setRowCount(0);

    // Opsional: reset combo box ke awal
    cbKota.setSelectedIndex(-1);
}
    private void simpanFavorit() {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter("favorit.txt"))) {
        for (int i = 0; i < cbKota.getItemCount(); i++) {
            bw.write(cbKota.getItemAt(i));
            bw.newLine();
        }
    } catch (IOException e) {
        System.err.println("Gagal menyimpan favorit: " + e.getMessage());
    }
}

private void muatFavorit() {
    File file = new File("favorit.txt");
    if (!file.exists()) return; // jika belum pernah ada file, lewati saja

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        cbKota.removeAllItems();
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                cbKota.addItem(line.trim());
            }
        }
    } catch (IOException e) {
        System.err.println("Gagal memuat favorit: " + e.getMessage());
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlJudul = new javax.swing.JPanel();
        lblJudul = new javax.swing.JLabel();
        pnlKonten = new javax.swing.JPanel();
        lblKota = new javax.swing.JLabel();
        cbKota = new javax.swing.JComboBox<>();
        btnCek = new javax.swing.JButton();
        lblCuaca = new javax.swing.JLabel();
        lblTampilCuaca = new javax.swing.JLabel();
        lblSuhu = new javax.swing.JLabel();
        lblTampilSuhu = new javax.swing.JLabel();
        lblLembap = new javax.swing.JLabel();
        lblTampilLembap = new javax.swing.JLabel();
        lblGambar = new javax.swing.JLabel();
        lblTampilGambar = new javax.swing.JLabel();
        btnFavorit = new javax.swing.JButton();
        btnMuat = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        scrlTable = new javax.swing.JScrollPane();
        tblCuaca = new javax.swing.JTable();
        txtKota = new javax.swing.JTextField();
        lblKotaFav = new javax.swing.JLabel();
        btnReset = new javax.swing.JButton();
        btnKeluar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Aplikasi Cek Cuaca Sederhana");

        pnlJudul.setBackground(new java.awt.Color(255, 255, 204));
        pnlJudul.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 204, 204), 2, true));
        pnlJudul.setPreferredSize(new java.awt.Dimension(466, 50));

        lblJudul.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblJudul.setIcon(new javax.swing.ImageIcon(getClass().getResource("/weathericon.png"))); // NOI18N
        lblJudul.setText("APLIKASI CEK CUACA SEDERHANA");
        pnlJudul.add(lblJudul);

        getContentPane().add(pnlJudul, java.awt.BorderLayout.PAGE_START);

        pnlKonten.setBackground(new java.awt.Color(255, 255, 204));

        lblKota.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblKota.setText("PILIH KOTA:");

        cbKota.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        btnCek.setBackground(new java.awt.Color(51, 255, 51));
        btnCek.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCek.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cek.png"))); // NOI18N
        btnCek.setText("CEK");

        lblCuaca.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblCuaca.setText("CUACA SAAT INI:");

        lblTampilCuaca.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        lblSuhu.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblSuhu.setText("SUHU:");

        lblTampilSuhu.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        lblLembap.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblLembap.setText("KELEMBAPAN:");

        lblTampilLembap.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        lblGambar.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblGambar.setText("GAMBAR:");

        lblTampilGambar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        btnFavorit.setBackground(new java.awt.Color(255, 102, 102));
        btnFavorit.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnFavorit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/favorit.png"))); // NOI18N
        btnFavorit.setText("SIMPAN FAVORIT");

        btnMuat.setBackground(new java.awt.Color(255, 153, 0));
        btnMuat.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnMuat.setIcon(new javax.swing.ImageIcon(getClass().getResource("/load.png"))); // NOI18N
        btnMuat.setText("MUAT DATA");

        btnSimpan.setBackground(new java.awt.Color(0, 204, 255));
        btnSimpan.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSimpan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/save.png"))); // NOI18N
        btnSimpan.setText("SIMPAN CSV");

        scrlTable.setBackground(new java.awt.Color(255, 255, 204));

        tblCuaca.setBackground(new java.awt.Color(255, 255, 204));
        tblCuaca.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        tblCuaca.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        tblCuaca.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "KOTA", "SUHU", "CUACA", "WAKTU"
            }
        ));
        scrlTable.setViewportView(tblCuaca);

        txtKota.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        lblKotaFav.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblKotaFav.setText("KOTA FAVORIT:");

        btnReset.setBackground(new java.awt.Color(255, 255, 51));
        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/reset.png"))); // NOI18N
        btnReset.setText("RESET");

        btnKeluar.setBackground(new java.awt.Color(255, 0, 0));
        btnKeluar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnKeluar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/exit.png"))); // NOI18N
        btnKeluar.setText("KELUAR");

        javax.swing.GroupLayout pnlKontenLayout = new javax.swing.GroupLayout(pnlKonten);
        pnlKonten.setLayout(pnlKontenLayout);
        pnlKontenLayout.setHorizontalGroup(
            pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrlTable, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(pnlKontenLayout.createSequentialGroup()
                .addGap(149, 149, 149)
                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlKontenLayout.createSequentialGroup()
                        .addComponent(btnFavorit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                        .addComponent(btnMuat, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlKontenLayout.createSequentialGroup()
                        .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblKotaFav, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(pnlKontenLayout.createSequentialGroup()
                                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblCuaca)
                                    .addComponent(lblKota)
                                    .addComponent(lblSuhu)
                                    .addComponent(lblLembap)
                                    .addComponent(lblGambar))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTampilSuhu)
                            .addComponent(cbKota, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblTampilCuaca)
                            .addComponent(lblTampilLembap)
                            .addComponent(lblTampilGambar)
                            .addComponent(txtKota, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlKontenLayout.createSequentialGroup()
                        .addComponent(btnCek, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnKeluar, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(129, 129, 129))
        );
        pnlKontenLayout.setVerticalGroup(
            pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlKontenLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtKota, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblKota))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbKota, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblKotaFav))
                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlKontenLayout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addComponent(lblTampilSuhu)
                        .addGap(12, 12, 12)
                        .addComponent(lblTampilLembap)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTampilGambar))
                    .addGroup(pnlKontenLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblCuaca)
                            .addComponent(lblTampilCuaca))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblSuhu)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblLembap)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblGambar)))
                .addGap(24, 24, 24)
                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCek, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnReset)
                    .addComponent(btnKeluar))
                .addGap(9, 9, 9)
                .addGroup(pnlKontenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnFavorit, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMuat, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(scrlTable, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        getContentPane().add(pnlKonten, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CekCuacaFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CekCuacaFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CekCuacaFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CekCuacaFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CekCuacaFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCek;
    private javax.swing.JButton btnFavorit;
    private javax.swing.JButton btnKeluar;
    private javax.swing.JButton btnMuat;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JComboBox<String> cbKota;
    private javax.swing.JLabel lblCuaca;
    private javax.swing.JLabel lblGambar;
    private javax.swing.JLabel lblJudul;
    private javax.swing.JLabel lblKota;
    private javax.swing.JLabel lblKotaFav;
    private javax.swing.JLabel lblLembap;
    private javax.swing.JLabel lblSuhu;
    private javax.swing.JLabel lblTampilCuaca;
    private javax.swing.JLabel lblTampilGambar;
    private javax.swing.JLabel lblTampilLembap;
    private javax.swing.JLabel lblTampilSuhu;
    private javax.swing.JPanel pnlJudul;
    private javax.swing.JPanel pnlKonten;
    private javax.swing.JScrollPane scrlTable;
    private javax.swing.JTable tblCuaca;
    private javax.swing.JTextField txtKota;
    // End of variables declaration//GEN-END:variables
}
