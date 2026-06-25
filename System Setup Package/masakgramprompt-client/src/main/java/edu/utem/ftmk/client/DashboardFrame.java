package edu.utem.ftmk.client;

import edu.utem.ftmk.network.Request;
import edu.utem.ftmk.network.Response;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * DashboardFrame — MasakGramPrompt main UI.
 *
 * Tabs:
 *   1. Reel Analysis    — table of reels, transcript preview, analysis status
 *   2. Run Experiment   — multi-select techniques, real-time status grid
 *   3. Results Viewer   — ground truth vs LLM fact sheet, CSV download
 *   4. Data Export      — 10 evaluation CSV exports
 */
public class DashboardFrame extends JFrame {

    // ── Palette (Light Mode) ───────────────────────────────────────────────────────────────
    private static final Color BG_DARK   = new Color(243, 244, 246); // Light gray background
    private static final Color BG_PANEL  = new Color(255, 255, 255); // White panels
    private static final Color BG_CARD   = new Color(249, 250, 251); // Off-white cards
    private static final Color ACCENT    = new Color(37, 99, 235);   // Blue
    private static final Color ACCENT2   = new Color(124, 58, 237);  // Purple
    private static final Color TEXT_MAIN = new Color(31, 41, 55);    // Dark gray text
    private static final Color TEXT_DIM  = new Color(107, 114, 128); // Medium gray text
    private static final Color SUCCESS   = new Color(16, 185, 129);  // Green
    private static final Color WARNING   = new Color(245, 158, 11);  // Orange
    private static final Color DANGER    = new Color(239, 68, 68);   // Red
    private static final Color MY_COLOR  = new Color(253, 230, 138); // Light yellow/orange highlight

    // ── Reels tab ─────────────────────────────────────────────────────────────
    private DefaultTableModel reelsTableModel;
    private JTable reelsTable;
    private JTextPane transcriptPreview;
    private JPanel statusGridPanel;

    // ── Experiment tab ────────────────────────────────────────────────────────
    private JComboBox<String> modelCombo;
    private JCheckBox cbZeroShot, cbFewShot, cbChainOfThought, cbStructuredOutput;
    private JTextArea experimentLog;
    private JButton runBtn;
    private JPanel executionStatusGrid;
    private JLabel transcriptCountLabel;

    // ── Hallucination Detection panel ─────────────────────────────────────────
    private JButton detectHallucinationsBtn;
    private JTextArea hallucinationLog;

    // ── Results tab ───────────────────────────────────────────────────────────
    private JComboBox<String> experimentCombo;
    private JPanel factSheetPanel;
    private JTextArea rawJsonArea;

    // ── Data Export tab ───────────────────────────────────────────────────────
    private JLabel exportStatusLabel;
    private JTable previewTable;
    private DefaultTableModel previewTableModel;
    private String currentPreviewLabel = null;

    // Backing data
    private List<Map<String, String>> reelsList       = new ArrayList<>();
    private List<Map<String, String>> modelsList      = new ArrayList<>();
    private List<Map<String, String>> techniquesList  = new ArrayList<>();
    private List<Map<String, String>> experimentList  = new ArrayList<>();
    private List<Map<String, String>> transcriptsList = new ArrayList<>();

    // Technique ID lookup by name
    private Map<String, String> techniqueIdByName = new LinkedHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    public DashboardFrame() {
        setTitle("MasakGramPrompt — Nutritional LLM Analysis Dashboard");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        initUI();
        loadAllData();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================
    private void initUI() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tabs.addTab("  📋  Reel Analysis  ",   buildReelsTab());
        tabs.addTab("  🧪  Run Experiment  ",   buildExperimentTab());
        tabs.addTab("  📊  Results Viewer  ",   buildResultsTab());
        tabs.addTab("  📤  Data Export  ",      buildExportTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_PANEL);
        h.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("MasakGramPrompt");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setForeground(ACCENT);

        JLabel sub = new JLabel("Danish Harraz  ·  5 members  ·  10 reels  ·  BITP 3123 Sem 2 2025/2026");
        sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        sub.setForeground(TEXT_DIM);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        JButton refresh = styledButton("⟳  Refresh", ACCENT);
        refresh.addActionListener(e -> loadAllData());

        h.add(left, BorderLayout.WEST);
        h.add(refresh, BorderLayout.EAST);
        return h;
    }

    // ── Tab 1: Reel Analysis ──────────────────────────────────────────────────
    private JPanel buildReelsTab() {
        JPanel panel = darkPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.add(sectionLabel("Instagram Reels — Data Collection Status"), BorderLayout.NORTH);

        // Reel table
        String[] cols = {"ID", "Reel (Instagram)", "Influencer", "Language", "Duration", "Transcript", "Ground Truth"};
        reelsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reelsTable = styledTable(reelsTableModel);
        reelsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onReelSelected();
        });
        JScrollPane reelScroll = new JScrollPane(reelsTable);
        reelScroll.getViewport().setBackground(BG_CARD);
        reelScroll.setPreferredSize(new Dimension(0, 240));

        // Transcript preview
        transcriptPreview = new JTextPane();
        transcriptPreview.setEditable(false);
        transcriptPreview.setBackground(new Color(10, 14, 26)); // Dark background
        transcriptPreview.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JScrollPane transcriptScroll = new JScrollPane(transcriptPreview);
        transcriptScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL),
            " Transcript Preview (yellow = Malay / green = English) ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));

        // Status grid placeholder
        statusGridPanel = darkCard();
        statusGridPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL), " Analysis Status (Model × Technique) ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));
        statusGridPanel.add(new JLabel("  Select a reel above to see analysis status.", SwingConstants.LEFT));
        statusGridPanel.setPreferredSize(new Dimension(0, 160));

        JButton viewFactSheetBtn = styledButton("📊  View Fact Sheet for Selected Reel", ACCENT2);
        viewFactSheetBtn.addActionListener(e -> viewFactSheetForSelectedReel());

        JPanel bottomBar = darkPanel(new FlowLayout(FlowLayout.LEFT));
        bottomBar.add(viewFactSheetBtn);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, transcriptScroll, statusGridPanel);
        split.setDividerLocation(220);
        split.setBorder(null);

        panel.add(reelScroll, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);
        return panel;
    }

    // ── Tab 2: Run Experiment ─────────────────────────────────────────────────
    private JPanel buildExperimentTab() {
        JPanel panel = darkPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.add(sectionLabel("Configure & Launch LLM Experiments"), BorderLayout.NORTH);

        // ── Config card ───────────────────────────────────────────────────────
        JPanel configCard = darkCard();
        configCard.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 8, 7, 8);
        g.anchor = GridBagConstraints.WEST;

        // Transcript count (replaces single-reel dropdown per spec §5.2a)
        g.gridx = 0; g.gridy = 0; configCard.add(dimLabel("Transcripts:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        transcriptCountLabel = new JLabel("Loading...");
        transcriptCountLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        transcriptCountLabel.setForeground(ACCENT);
        configCard.add(transcriptCountLabel, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        configCard.add(dimLabel("LLM Model:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        modelCombo = styledCombo(); configCard.add(modelCombo, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        configCard.add(dimLabel("Prompt Techniques:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        JPanel techPanel = darkPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        cbZeroShot         = styledCheckbox("zero-shot");
        cbFewShot          = styledCheckbox("few-shot");
        cbChainOfThought   = styledCheckbox("chain-of-thought");
        cbStructuredOutput = styledCheckbox("structured-output");
        cbZeroShot.setSelected(true);
        techPanel.add(cbZeroShot); techPanel.add(cbFewShot);
        techPanel.add(cbChainOfThought); techPanel.add(cbStructuredOutput);
        configCard.add(techPanel, g);

        g.gridx = 1; g.gridy = 3; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.EAST;
        runBtn = styledButton("▶  Run All Transcripts", SUCCESS);
        runBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        runBtn.addActionListener(e -> runExperiments());
        configCard.add(runBtn, g);

        // ── Execution status grid ─────────────────────────────────────────────
        executionStatusGrid = darkCard();
        executionStatusGrid.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JLabel placeholder = new JLabel("  Status grid will appear here after clicking Run.");
        placeholder.setForeground(TEXT_DIM);
        executionStatusGrid.add(placeholder);
        executionStatusGrid.setPreferredSize(new Dimension(0, 120));
        executionStatusGrid.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL), " Real-Time Execution Status ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));

        // ── Console log ───────────────────────────────────────────────────────
        experimentLog = new JTextArea();
        experimentLog.setEditable(false);
        experimentLog.setBackground(new Color(8, 10, 20));
        experimentLog.setForeground(new Color(180, 210, 140));
        experimentLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        experimentLog.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane logScroll = new JScrollPane(experimentLog);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL), " Experiment Console ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));

        // ── Hallucination Detection panel ─────────────────────────────────────
        JPanel hallucinationCard = darkCard();
        hallucinationCard.setLayout(new BorderLayout(8, 8));
        hallucinationCard.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SUCCESS), " 🔍  Hallucination Detection (Post-Process) ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), SUCCESS));

        JPanel hallucinationControls = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        detectHallucinationsBtn = styledButton("🔍  Detect Hallucinations", SUCCESS);
        detectHallucinationsBtn.addActionListener(e -> detectHallucinations());
        hallucinationControls.add(detectHallucinationsBtn);
        JLabel hallucinationHint = new JLabel("  Each experiment is judged by the same model that ran it.");
        hallucinationHint.setForeground(TEXT_DIM);
        hallucinationHint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        hallucinationControls.add(hallucinationHint);

        hallucinationLog = new JTextArea(4, 0);
        hallucinationLog.setEditable(false);
        hallucinationLog.setBackground(Color.BLACK);
        hallucinationLog.setForeground(Color.WHITE);
        hallucinationLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        hallucinationLog.setBorder(new EmptyBorder(4, 8, 4, 8));
        JScrollPane hallucinationLogScroll = new JScrollPane(hallucinationLog);

        hallucinationCard.add(hallucinationControls, BorderLayout.NORTH);
        hallucinationCard.add(hallucinationLogScroll, BorderLayout.CENTER);

        // ── Layout ────────────────────────────────────────────────────────────
        JSplitPane consoleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, executionStatusGrid, logScroll);
        consoleSplit.setDividerLocation(120);
        consoleSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, configCard, consoleSplit);
        mainSplit.setDividerLocation(180);
        mainSplit.setBorder(null);

        JSplitPane outerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, hallucinationCard);
        outerSplit.setDividerLocation(500);
        outerSplit.setBorder(null);

        panel.add(outerSplit, BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 3: Results Viewer ─────────────────────────────────────────────────
    private JPanel buildResultsTab() {
        JPanel panel = darkPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel selectorBar = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        selectorBar.add(dimLabel("Experiment:"));
        experimentCombo = styledCombo();
        experimentCombo.setPreferredSize(new Dimension(400, 28));
        selectorBar.add(experimentCombo);
        JButton loadBtn = styledButton("Load Result", ACCENT);
        loadBtn.addActionListener(e -> loadResult());
        selectorBar.add(loadBtn);
        JButton csvBtn = styledButton("⬇ Download CSV", SUCCESS);
        csvBtn.addActionListener(e -> downloadFactSheetCsv());
        selectorBar.add(csvBtn);

        factSheetPanel = darkCard();
        factSheetPanel.setLayout(new BorderLayout());
        JLabel ph = new JLabel("Select a completed experiment and click 'Load Result'", SwingConstants.CENTER);
        ph.setForeground(TEXT_DIM);
        factSheetPanel.add(ph, BorderLayout.CENTER);

        rawJsonArea = new JTextArea(5, 40);
        rawJsonArea.setEditable(false);
        rawJsonArea.setBackground(new Color(8, 10, 20));
        rawJsonArea.setForeground(new Color(180, 210, 140));
        rawJsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        rawJsonArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane rawScroll = new JScrollPane(rawJsonArea);
        rawScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL), " Raw JSON Output ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));
        rawScroll.setPreferredSize(new Dimension(0, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, factSheetPanel, rawScroll);
        split.setDividerLocation(400);
        split.setBorder(null);

        JPanel top = darkPanel(new BorderLayout());
        top.add(sectionLabel("Nutritional Fact Sheet"), BorderLayout.WEST);
        top.add(selectorBar, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 4: Data Export ────────────────────────────────────────────────────
    private JPanel buildExportTab() {
        JPanel panel = darkPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.add(sectionLabel("Data Export — Evaluation Metric CSV Files"), BorderLayout.NORTH);

        JPanel grid = darkCard();
        grid.setLayout(new GridLayout(5, 2, 12, 12));

        String[][] exports = {
            {"LAYER1A", "📄 layer1a_exact_match.csv — Exact Match"},
            {"LAYER1B", "📄 layer1b_text_similarity.csv — Fuzzy/BLEU/ROUGE"},
            {"LAYER2A", "📄 layer2a_numeric_quantity.csv — Quantity MAE/MAPE"},
            {"LAYER2B", "📄 layer2b_numeric_nutrition.csv — Nutrition MAE/MAPE"},
            {"LAYER2C", "📄 layer2c_nutrition_totals.csv — Recipe Totals"},
            {"LAYER3A", "📄 layer3a_json_validity.csv — JSON Validity Rate"},
            {"LAYER3B", "📄 layer3b_hallucination.csv — Hallucination Rate"},
            {"LAYER3C", "📄 layer3c_ingredient_detection.csv — Precision/Recall/F1"},
            {"LAYER4",  "📄 layer4_human_evaluation.csv — Human Evaluation"},
            {"LAYER5",  "📄 layer5_condition_scores.csv — Friedman/Wilcoxon Stats"},
        };

        for (String[] export : exports) {
            String label = export[0];
            String name  = export[1];
            JButton btn  = styledButton(name, BG_CARD);
            btn.setForeground(ACCENT);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.addActionListener(e -> previewExportData(label));
            grid.add(btn);
        }

        previewTableModel = new DefaultTableModel(new String[]{"Preview Data"}, 0);
        previewTable = styledTable(previewTableModel);
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane previewScroll = new JScrollPane(previewTable);
        previewScroll.getViewport().setBackground(BG_CARD);
        previewScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL), " Data Preview ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));
        
        JPanel exportBar = darkPanel(new FlowLayout(FlowLayout.RIGHT));
        exportStatusLabel = new JLabel("  Select a metric to preview.");
        exportStatusLabel.setForeground(TEXT_DIM);
        exportStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        JButton btnExport = styledButton("⬇ Download Displayed CSV", SUCCESS);
        btnExport.addActionListener(e -> {
            if (currentPreviewLabel != null) triggerExport(currentPreviewLabel);
            else JOptionPane.showMessageDialog(this, "Select a metric to preview first.");
        });
        exportBar.add(exportStatusLabel);
        exportBar.add(btnExport);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, grid, previewScroll);
        split.setDividerLocation(200);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        panel.add(exportBar, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Data Loading
    // =========================================================================
    private void loadAllData() {
        new Thread(() -> {
            loadReels(); loadModels(); loadTechniques(); loadExperiments(); loadTranscripts();
        }).start();
    }

    private void loadReels() {
        Response res = ClientConnection.sendRequest(new Request("GET_REELS"));
        if (!res.isSuccess()) return;
        reelsList = (List<Map<String, String>>) res.getData("reels");
        SwingUtilities.invokeLater(() -> {
            reelsTableModel.setRowCount(0);
            for (Map<String, String> r : reelsList) {
                reelsTableModel.addRow(new Object[]{
                    r.get("reel_id"), r.get("reel_id_instagram"),
                    r.get("influencer"), r.getOrDefault("language_tag", "code-switched"),
                    r.getOrDefault("duration", "—"), r.get("status"),
                    r.getOrDefault("gt_available", "No")
                });
            }
        });
    }

    private void loadModels() {
        Response res = ClientConnection.sendRequest(new Request("GET_MODELS"));
        if (!res.isSuccess()) return;
        modelsList = (List<Map<String, String>>) res.getData("models");
        SwingUtilities.invokeLater(() -> {
            modelCombo.removeAllItems();
            for (Map<String, String> m : modelsList) {
                modelCombo.addItem(m.get("model_name"));
            }
        });
    }

    private void loadTechniques() {
        Response res = ClientConnection.sendRequest(new Request("GET_TECHNIQUES"));
        if (!res.isSuccess()) return;
        techniquesList = (List<Map<String, String>>) res.getData("techniques");
        SwingUtilities.invokeLater(() -> {
            techniqueIdByName.clear();
            for (Map<String, String> t : techniquesList)
                techniqueIdByName.put(t.get("technique_name"), t.get("technique_id"));
        });
    }

    private void loadExperiments() {
        Response res = ClientConnection.sendRequest(new Request("GET_EXPERIMENTS"));
        if (!res.isSuccess()) return;
        experimentList = (List<Map<String, String>>) res.getData("experiments");
        SwingUtilities.invokeLater(() -> {
            experimentCombo.removeAllItems();
            for (Map<String, String> e : experimentList) {
                experimentCombo.addItem(
                    "Exp #" + e.get("experiment_id") + " | " + e.get("reel") +
                    " | " + e.get("model") + " | " + e.get("technique") +
                    "  [" + e.get("status") + "]");
            }
        });
    }

    private void loadTranscripts() {
        Response res = ClientConnection.sendRequest(new Request("GET_TRANSCRIPTS"));
        if (!res.isSuccess()) return;
        transcriptsList = (List<Map<String, String>>) res.getData("transcripts");
        SwingUtilities.invokeLater(() -> {
            int count = transcriptsList.size();
            transcriptCountLabel.setText("All " + count + " transcript(s) in database will be processed");
        });
    }

    // =========================================================================
    // Reel Analysis — Interactions
    // =========================================================================
    private void onReelSelected() {
        int row = reelsTable.getSelectedRow();
        if (row < 0 || row >= reelsList.size()) return;
        Map<String, String> reel = reelsList.get(row);

        new Thread(() -> {
            // Load transcript preview
            Request req = new Request("GET_TRANSCRIPT");
            req.addData("reelId", reel.get("reel_id"));
            Response res = ClientConnection.sendRequest(req);
            if (res.isSuccess()) {
                String text = (String) res.getData("transcriptText");
                
                Request gtReq = new Request("GET_GROUND_TRUTH_BY_REEL");
                gtReq.addData("reelId", reel.get("reel_id"));
                Response gtRes = ClientConnection.sendRequest(gtReq);
                List<Map<String, String>> gtList = gtRes.isSuccess() 
                    ? (List<Map<String, String>>) gtRes.getData("groundTruth") : new ArrayList<>();
                
                SwingUtilities.invokeLater(() -> renderTranscriptPreview(text, gtList));
            }

            // Load analysis status grid
            Request statusReq = new Request("GET_REEL_STATUS");
            statusReq.addData("reelId", reel.get("reel_id"));
            Response statusRes = ClientConnection.sendRequest(statusReq);
            if (statusRes.isSuccess()) {
                List<Map<String, String>> statusList =
                    (List<Map<String, String>>) statusRes.getData("statusList");
                SwingUtilities.invokeLater(() -> renderStatusGrid(statusList));
            }
        }).start();
    }

    /**
     * Renders transcript text with Malay ingredients highlighted in yellow,
     * English ingredients highlighted in white, and the rest dimmed.
     */
    private void renderTranscriptPreview(String text, List<Map<String, String>> gtList) {
        if (text == null) { transcriptPreview.setText("No transcript available."); return; }

        javax.swing.text.StyledDocument doc = transcriptPreview.getStyledDocument();
        javax.swing.text.Style defaultStyle = transcriptPreview.addStyle("default", null);
        javax.swing.text.StyleConstants.setForeground(defaultStyle, Color.WHITE);

        javax.swing.text.Style malayStyle = transcriptPreview.addStyle("malay", null);
        javax.swing.text.StyleConstants.setForeground(malayStyle, MY_COLOR);
        javax.swing.text.StyleConstants.setBold(malayStyle, true);

        javax.swing.text.Style engStyle = transcriptPreview.addStyle("english", null);
        javax.swing.text.StyleConstants.setForeground(engStyle, SUCCESS);
        javax.swing.text.StyleConstants.setBold(engStyle, true);

        Set<String> malayWords = new HashSet<>();
        Set<String> engWords = new HashSet<>();
        for (Map<String, String> gt : gtList) {
            String nameOrig = gt.get("name_original");
            if (nameOrig != null) {
                for (String w : nameOrig.toLowerCase().split("\\s+")) {
                    malayWords.add(w.replaceAll("[^a-z]", ""));
                }
            }
            String nameEn = gt.get("name_en");
            if (nameEn != null) {
                for (String w : nameEn.toLowerCase().split("\\s+")) {
                    engWords.add(w.replaceAll("[^a-z]", ""));
                }
            }
        }

        try {
            doc.remove(0, doc.getLength());
            String[] words = text.split("(?<=\\s)|(?=\\s)");
            for (String word : words) {
                String clean = word.trim().toLowerCase().replaceAll("[^a-z]", "");
                if (clean.isEmpty()) {
                    doc.insertString(doc.getLength(), word, defaultStyle);
                } else if (malayWords.contains(clean)) {
                    doc.insertString(doc.getLength(), word, malayStyle);
                } else if (engWords.contains(clean)) {
                    doc.insertString(doc.getLength(), word, engStyle);
                } else {
                    doc.insertString(doc.getLength(), word, defaultStyle);
                }
            }
        } catch (Exception ex) {
            transcriptPreview.setText(text);
        }
    }

    private void renderStatusGrid(List<Map<String, String>> statusList) {
        statusGridPanel.removeAll();
        statusGridPanel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6);

        // Headers
        String[] techniques = {"zero-shot", "few-shot", "chain-of-thought", "structured-output"};
        String[] models = {"Llama 3.2 3B", "Phi-4-mini 3.8B", "Qwen 2.5 3B", "Gemma-SEA-LION v4 4B", "MedGemma 4B"};

        g.gridx = 0; g.gridy = 0;
        statusGridPanel.add(dimLabel("Model \\ Technique"), g);
        for (int t = 0; t < techniques.length; t++) {
            g.gridx = t + 1; statusGridPanel.add(dimLabel(techniques[t]), g);
        }

        // Build lookup: model+technique -> status
        Map<String, String> statusMap = new HashMap<>();
        if (statusList != null) {
            for (Map<String, String> s : statusList)
                statusMap.put(s.get("model") + "|" + s.get("technique"), s.get("status"));
        }

        for (int m = 0; m < models.length; m++) {
            g.gridx = 0; g.gridy = m + 1;
            JLabel ml = dimLabel(models[m].split(" ")[0] + " " + models[m].split(" ")[1]);
            ml.setForeground(TEXT_MAIN);
            statusGridPanel.add(ml, g);
            for (int t = 0; t < techniques.length; t++) {
                String status = statusMap.getOrDefault(models[m] + "|" + techniques[t], "—");
                JLabel badge = statusBadge(status);
                g.gridx = t + 1; statusGridPanel.add(badge, g);
            }
        }
        statusGridPanel.revalidate(); statusGridPanel.repaint();
    }

    private void viewFactSheetForSelectedReel() {
        int row = reelsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Please select a reel first."); return; }
        // Switch to Results tab and filter by reel
        JTabbedPane tabs = (JTabbedPane) getContentPane().getComponent(1);
        tabs.setSelectedIndex(2);
    }

    // =========================================================================
    // Experiment Execution
    // =========================================================================
    private void runExperiments() {
        List<String[]> selectedTechniques = new ArrayList<>();
        if (cbZeroShot.isSelected())         selectedTechniques.add(new String[]{"zero-shot",         techniqueIdByName.getOrDefault("zero-shot","1")});
        if (cbFewShot.isSelected())          selectedTechniques.add(new String[]{"few-shot",          techniqueIdByName.getOrDefault("few-shot","2")});
        if (cbChainOfThought.isSelected())   selectedTechniques.add(new String[]{"chain-of-thought",  techniqueIdByName.getOrDefault("chain-of-thought","3")});
        if (cbStructuredOutput.isSelected()) selectedTechniques.add(new String[]{"structured-output", techniqueIdByName.getOrDefault("structured-output","4")});

        if (selectedTechniques.isEmpty()) {
            experimentLog.append("[ERROR] Please select at least one prompt technique.\n"); return;
        }
        if (transcriptsList.isEmpty()) {
            experimentLog.append("[ERROR] No transcripts loaded. Please refresh.\n"); return;
        }

        // Read selected combo box
        int mi = modelCombo.getSelectedIndex();
        if (mi < 0) {
            experimentLog.append("[ERROR] Please select a model.\n"); return;
        }

        String modelId   = modelsList.get(mi).get("model_id");
        String modelName = modelsList.get(mi).get("model_name");
        int totalJobs    = transcriptsList.size() * selectedTechniques.size();

        runBtn.setEnabled(false);

        // Build status grid — one card per transcript × technique
        Map<String, JLabel> statusCards = new LinkedHashMap<>();
        SwingUtilities.invokeLater(() -> {
            executionStatusGrid.removeAll();
            for (Map<String, String> transcript : transcriptsList) {
                String vid = transcript.getOrDefault("video_id", transcript.get("reel_id_instagram"));
                for (String[] tech : selectedTechniques) {
                    String cardKey = transcript.get("transcript_id") + "|" + tech[0];
                    JLabel badge = statusBadge("pending");
                    JPanel card = buildStatusCard(vid + " / " + tech[0], badge);
                    statusCards.put(cardKey, badge);
                    executionStatusGrid.add(card);
                }
            }
            executionStatusGrid.revalidate(); executionStatusGrid.repaint();
        });

        experimentLog.append("\n══════════════════════════════════════════════════════\n");
        experimentLog.append("[INFO] Starting batch: " + transcriptsList.size() + " transcripts × "
            + selectedTechniques.size() + " technique(s) = " + totalJobs + " experiments\n");
        experimentLog.append("       Model : " + modelName + "\n");

        new Thread(() -> {
            int completed = 0, failed = 0;
            for (Map<String, String> transcript : transcriptsList) {
                String transcriptId = transcript.get("transcript_id");
                String videoId      = transcript.getOrDefault("video_id", transcript.get("reel_id_instagram"));

                for (String[] tech : selectedTechniques) {
                    String techName = tech[0];
                    String techId   = tech[1];
                    String cardKey  = transcriptId + "|" + techName;

                    SwingUtilities.invokeLater(() -> {
                        updateStatusCard(statusCards.get(cardKey), "running");
                        experimentLog.append("\n[RUN] " + videoId + " | " + techName + " → creating experiment...\n");
                    });

                    try {
                        // Step 1: Create experiment row
                        Request createReq = new Request("CREATE_EXPERIMENT");
                        createReq.addData("transcriptId", transcriptId);
                        createReq.addData("modelId", modelId);
                        createReq.addData("techniqueId", techId);
                        Response createRes = ClientConnection.sendRequest(createReq);

                        if (!createRes.isSuccess()) {
                            SwingUtilities.invokeLater(() -> {
                                updateStatusCard(statusCards.get(cardKey), "failed");
                                experimentLog.append("[ERROR] " + createRes.getMessage() + "\n");
                            });
                            failed++;
                            continue;
                        }

                        String expId = (String) createRes.getData("experimentId");
                        SwingUtilities.invokeLater(() ->
                            experimentLog.append("[INFO]  Exp #" + expId + " → querying LLM (may take 1-2 min)...\n"));

                        // Step 2: Run the LLM
                        Request runReq = new Request("RUN_EXPERIMENT");
                        runReq.addData("experimentId", expId);
                        Response runRes = ClientConnection.sendRequest(runReq);

                        if (runRes.isSuccess()) {
                            completed++;
                            SwingUtilities.invokeLater(() -> {
                                updateStatusCard(statusCards.get(cardKey), "completed");
                                experimentLog.append("[OK]   Exp #" + expId + " (" + videoId + " / " + techName + ") done!\n");
                            });
                        } else {
                            failed++;
                            SwingUtilities.invokeLater(() -> {
                                updateStatusCard(statusCards.get(cardKey), "failed");
                                experimentLog.append("[ERROR] Exp #" + expId + " failed: " + runRes.getMessage() + "\n");
                            });
                        }

                    } catch (Exception ex) {
                        failed++;
                        SwingUtilities.invokeLater(() -> {
                            updateStatusCard(statusCards.get(cardKey), "failed");
                            experimentLog.append("[ERROR] " + videoId + "/" + techName + ": " + ex.getMessage() + "\n");
                        });
                    }
                }
            }
            final int doneCount = completed, failCount = failed;
            SwingUtilities.invokeLater(() -> {
                runBtn.setEnabled(true);
                experimentLog.append("\n══════════════════════════════════════════════════════\n");
                experimentLog.append("[DONE] Batch complete: " + doneCount + " succeeded, " + failCount + " failed.\n");
                experimentLog.append("       Run Detect Hallucinations below to judge results.\n");
                loadExperiments();
            });
        }).start();
    }

    // =========================================================================
    // Hallucination Detection
    // =========================================================================
    private void detectHallucinations() {
        detectHallucinationsBtn.setEnabled(false);
        hallucinationLog.append("\n══════════════════════════════════════════\n");
        hallucinationLog.append("[INFO] Starting hallucination detection\n");
        hallucinationLog.append("       Each experiment will be judged by the model that ran it.\n");
        hallucinationLog.append("       Checking all completed experiments with un-judged ingredients...\n");

        new Thread(() -> {
            Request req = new Request("DETECT_HALLUCINATIONS");
            Response res = ClientConnection.sendRequest(req);
            SwingUtilities.invokeLater(() -> {
                detectHallucinationsBtn.setEnabled(true);
                if (res.isSuccess()) {
                    String count = (String) res.getData("ingredientsJudged");
                    hallucinationLog.append("[DONE] " + count + " ingredient(s) judged successfully.\n");
                    hallucinationLog.append("       Export layer3b_hallucination.csv to see results.\n");
                } else {
                    hallucinationLog.append("[ERROR] " + res.getMessage() + "\n");
                }
            });
        }).start();
    }

    private JPanel buildStatusCard(String labelText, JLabel badge) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(BG_PANEL, 1, true), new EmptyBorder(6, 10, 6, 10)));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lbl.setForeground(TEXT_MAIN);
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgePanel.setOpaque(false);
        badgePanel.add(badge);
        card.add(lbl, BorderLayout.NORTH);
        card.add(badgePanel, BorderLayout.CENTER);
        return card;
    }

    private void updateStatusCard(JLabel badge, String status) {
        if (badge == null) return;
        switch (status) {
            case "running":   badge.setText("⟳ running");   badge.setForeground(WARNING);  break;
            case "completed": badge.setText("✓ completed"); badge.setForeground(SUCCESS);  break;
            case "failed":    badge.setText("✗ failed");    badge.setForeground(DANGER);   break;
            default:          badge.setText("· pending");   badge.setForeground(TEXT_DIM); break;
        }
    }

    // =========================================================================
    // Results Loading & Fact Sheet
    // =========================================================================
    private void loadResult() {
        int idx = experimentCombo.getSelectedIndex();
        if (idx < 0 || idx >= experimentList.size()) return;
        String expId  = experimentList.get(idx).get("experiment_id");
        String status = experimentList.get(idx).get("status");
        if (!"completed".equals(status)) {
            JOptionPane.showMessageDialog(this, "This experiment is not completed yet.", "Not Ready", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new Thread(() -> {
            // Fetch LLM result
            Request req = new Request("GET_RESULT");
            req.addData("experimentId", expId);
            Response res = ClientConnection.sendRequest(req);
            if (!res.isSuccess()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, res.getMessage()));
                return;
            }
            Map<String, String> result = (Map<String, String>) res.getData("result");
            String resultId = result.get("result_id");

            // Fetch predicted ingredients
            Request ingReq = new Request("GET_INGREDIENTS");
            ingReq.addData("resultId", resultId);
            Response ingRes = ClientConnection.sendRequest(ingReq);
            List<Map<String, String>> ingredients = ingRes.isSuccess()
                ? (List<Map<String, String>>) ingRes.getData("ingredients") : new ArrayList<>();

            // Fetch ground truth ingredients for this experiment
            Request gtReq = new Request("GET_GROUND_TRUTH");
            gtReq.addData("experimentId", expId);
            Response gtRes = ClientConnection.sendRequest(gtReq);
            List<Map<String, String>> groundTruth = gtRes.isSuccess()
                ? (List<Map<String, String>>) gtRes.getData("groundTruth") : new ArrayList<>();

            SwingUtilities.invokeLater(() -> renderFactSheet(result, ingredients, groundTruth));
        }).start();
    }

    private void renderFactSheet(Map<String, String> result, List<Map<String, String>> ingredients,
                                  List<Map<String, String>> groundTruth) {
        factSheetPanel.removeAll();
        factSheetPanel.setLayout(new BorderLayout(10, 10));

        // Header info boxes
        JPanel header = darkCard();
        header.setLayout(new GridLayout(1, 4, 10, 0));
        header.add(infoBox("Recipe",             result.getOrDefault("recipe_name","—"),        ACCENT));
        header.add(infoBox("Servings",           result.getOrDefault("servings_estimated","—"), ACCENT2));
        header.add(infoBox("JSON Valid",         result.getOrDefault("json_valid","—"),         SUCCESS));
        header.add(infoBox("Ingredients Found",  String.valueOf(ingredients.size()),             WARNING));

        // Nutrition table
        String[] labels = {"Calories (kcal)","Total Fat (g)","Saturated Fat (g)","Cholesterol (mg)",
            "Sodium (mg)","Carbohydrate (g)","Dietary Fiber (g)","Total Sugars (g)",
            "Protein (g)","Vitamin D (mcg)","Calcium (mg)","Iron (mg)","Potassium (mg)"};
        String[] sKeys = {"serving_calories","serving_total_fat_g","serving_saturated_fat_g",
            "serving_cholesterol_mg","serving_sodium_mg","serving_carbohydrate_g","serving_fiber_g",
            "serving_sugars_g","serving_protein_g","serving_vitamin_d_mcg","serving_calcium_mg",
            "serving_iron_mg","serving_potassium_mg"};
        String[] tKeys = {"total_calories","total_fat_g","total_saturated_fat_g","total_cholesterol_mg",
            "total_sodium_mg","total_carbohydrate_g","total_fiber_g","total_sugars_g",
            "total_protein_g","total_vitamin_d_mcg","total_calcium_mg","total_iron_mg","total_potassium_mg"};

        DefaultTableModel nutModel = new DefaultTableModel(
            new String[]{"Nutrient","Per Serving","Total Recipe","Ground Truth Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < labels.length; i++) {
            nutModel.addRow(new Object[]{labels[i],
                result.getOrDefault(sKeys[i],"—"), result.getOrDefault(tKeys[i],"—"), "—"});
        }
        JScrollPane nutScroll = new JScrollPane(styledTable(nutModel));
        nutScroll.getViewport().setBackground(BG_CARD);
        nutScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL), " Nutritional Values (LLM Prediction vs Ground Truth) ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));

        // Ingredients table with hallucination flag
        DefaultTableModel ingModel = new DefaultTableModel(
            new String[]{"Ingredient (Raw)","English Name","Qty","Unit","Weight (g)","Calories","Hallucination?"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Map<String, String> ing : ingredients) {
            String hall = "false".equalsIgnoreCase(ing.getOrDefault("is_hallucinated","false")) ? "No" : "⚠ YES";
            ingModel.addRow(new Object[]{
                ing.getOrDefault("name_original",""), ing.getOrDefault("name_en",""),
                ing.getOrDefault("quantity_value",""), ing.getOrDefault("unit_original",""),
                ing.getOrDefault("estimated_weight_g",""), ing.getOrDefault("calories",""), hall
            });
        }
        JTable ingTable = styledTable(ingModel);
        // Colour hallucinated rows red
        ingTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String hall = (String) t.getValueAt(r, 6);
                setBackground(hall.startsWith("⚠") ? new Color(80, 20, 20) : (sel ? new Color(55,70,110) : BG_CARD));
                setForeground(hall.startsWith("⚠") ? Color.WHITE : TEXT_MAIN);
                setBorder(new EmptyBorder(0, 4, 0, 4));
                return comp;
            }
        });
        JScrollPane ingScroll = new JScrollPane(ingTable);
        ingScroll.getViewport().setBackground(BG_CARD);
        ingScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BG_PANEL),
            " LLM Predicted Ingredients (" + ingredients.size() + ")  |  Red rows = hallucinated ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), TEXT_DIM));

        // Ground Truth ingredients table
        DefaultTableModel gtModel = new DefaultTableModel(
            new String[]{"GT Name (Raw)", "English Name", "Qty", "Culinary Unit", "Weight (g)",
                         "Calories", "Protein (g)", "Fat (g)", "Carbs (g)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Map<String, String> gt : groundTruth) {
            gtModel.addRow(new Object[]{
                gt.getOrDefault("name_original", ""),
                gt.getOrDefault("name_en", ""),
                gt.getOrDefault("quantity_value_culinary", ""),
                gt.getOrDefault("quantity_unit_culinary", ""),
                gt.getOrDefault("estimated_weight_g", ""),
                gt.getOrDefault("calories", ""),
                gt.getOrDefault("protein_g", ""),
                gt.getOrDefault("total_fat_g", ""),
                gt.getOrDefault("total_carbohydrate_g", "")
            });
        }
        JTable gtTable = styledTable(gtModel);
        gtTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBackground(sel ? new Color(55, 70, 110) : new Color(10, 35, 20));
                setForeground(Color.WHITE);
                setBorder(new EmptyBorder(0, 4, 0, 4));
                return comp;
            }
        });
        JScrollPane gtScroll = new JScrollPane(gtTable);
        gtScroll.getViewport().setBackground(new Color(10, 35, 20));
        gtScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SUCCESS),
            " Ground Truth Ingredients (" + groundTruth.size() + ") ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 11), SUCCESS));

        rawJsonArea.setText(result.getOrDefault("raw_json_output", ""));

        // Layout: GT on left, LLM predicted on right, nutrition on top
        JSplitPane ingTables = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gtScroll, ingScroll);
        ingTables.setDividerLocation(420); ingTables.setBorder(null);

        JSplitPane tables = new JSplitPane(JSplitPane.VERTICAL_SPLIT, nutScroll, ingTables);
        tables.setDividerLocation(200); tables.setBorder(null);

        factSheetPanel.add(header, BorderLayout.NORTH);
        factSheetPanel.add(tables, BorderLayout.CENTER);
        factSheetPanel.revalidate(); factSheetPanel.repaint();
    }

    private void downloadFactSheetCsv() {
        int idx = experimentCombo.getSelectedIndex();
        if (idx < 0) { JOptionPane.showMessageDialog(this, "Select an experiment first."); return; }
        String expId = experimentList.get(idx).get("experiment_id");

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose folder to save CSV");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String folder = chooser.getSelectedFile().getAbsolutePath();

        new Thread(() -> {
            Request req = new Request("EXPORT_FACT_SHEET_CSV");
            req.addData("experimentId", expId);
            req.addData("outputFolder", folder);
            Response res = ClientConnection.sendRequest(req);
            SwingUtilities.invokeLater(() -> {
                if (res.isSuccess()) {
                    JOptionPane.showMessageDialog(this, "CSV saved to:\n" + res.getData("filePath"), "Saved", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, res.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    // =========================================================================
    // Data Export
    // =========================================================================
    private void previewExportData(String label) {
        currentPreviewLabel = label;
        exportStatusLabel.setForeground(WARNING);
        exportStatusLabel.setText("  Loading preview for " + label + "...");
        
        new Thread(() -> {
            Request req = new Request("PREVIEW_CSV_DATA");
            req.addData("queryLabel", label);
            Response res = ClientConnection.sendRequest(req);
            
            SwingUtilities.invokeLater(() -> {
                if (res.isSuccess()) {
                    Map<String, Object> preview = (Map<String, Object>) res.getData("preview");
                    List<String> headers = (List<String>) preview.get("headers");
                    List<List<String>> data = (List<List<String>>) preview.get("data");
                    
                    previewTableModel.setColumnIdentifiers(headers.toArray());
                    previewTableModel.setRowCount(0);
                    for (List<String> row : data) {
                        previewTableModel.addRow(row.toArray());
                    }
                    exportStatusLabel.setForeground(SUCCESS);
                    exportStatusLabel.setText("  ✓ Preview loaded: " + data.size() + " rows.");
                } else {
                    exportStatusLabel.setForeground(DANGER);
                    exportStatusLabel.setText("  ✗ Error: " + res.getMessage());
                }
            });
        }).start();
    }

    private void triggerExport(String label) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose folder to save " + label + " CSV");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String folder = chooser.getSelectedFile().getAbsolutePath();

        exportStatusLabel.setForeground(WARNING);
        exportStatusLabel.setText("  Exporting " + label + "...");

        new Thread(() -> {
            Request req = new Request("EXPORT_CSV");
            req.addData("queryLabel", label);
            req.addData("outputFolder", folder);
            Response res = ClientConnection.sendRequest(req);
            SwingUtilities.invokeLater(() -> {
                if (res.isSuccess()) {
                    exportStatusLabel.setForeground(SUCCESS);
                    exportStatusLabel.setText("  ✓ Saved: " + res.getData("filePath"));
                } else {
                    exportStatusLabel.setForeground(DANGER);
                    exportStatusLabel.setText("  ✗ Error: " + res.getMessage());
                }
            });
        }).start();
    }

    // =========================================================================
    // UI Helpers
    // =========================================================================
    private JPanel darkPanel(LayoutManager lm) { JPanel p = new JPanel(lm); p.setBackground(BG_DARK); return p; }
    private JPanel darkCard() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(new CompoundBorder(new LineBorder(BG_PANEL,1,true), new EmptyBorder(10,12,10,12)));
        return p;
    }
    private JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font(Font.SANS_SERIF,Font.BOLD,15));
        l.setForeground(TEXT_MAIN); l.setBorder(new EmptyBorder(0,0,10,0)); return l;
    }
    private JLabel dimLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12)); l.setForeground(TEXT_DIM); return l;
    }
    private JButton styledButton(String t, Color bg) {
        JButton b = new JButton(t); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12)); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(7,16,7,16)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JCheckBox styledCheckbox(String t) {
        JCheckBox cb = new JCheckBox(t); cb.setBackground(BG_DARK); cb.setForeground(TEXT_MAIN);
        cb.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12)); return cb;
    }
    private JComboBox<String> styledCombo() {
        JComboBox<String> c = new JComboBox<>(); c.setBackground(BG_PANEL); c.setForeground(TEXT_MAIN);
        c.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12)); return c;
    }
    private JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m); t.setBackground(BG_CARD); t.setForeground(TEXT_MAIN);
        t.setSelectionBackground(new Color(55,70,110)); t.setSelectionForeground(Color.WHITE);
        t.setGridColor(BG_PANEL); t.setRowHeight(26); t.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        t.getTableHeader().setBackground(BG_PANEL); t.getTableHeader().setForeground(ACCENT);
        t.getTableHeader().setFont(new Font(Font.SANS_SERIF,Font.BOLD,12)); t.setFillsViewportHeight(true);
        return t;
    }
    private JPanel infoBox(String label, String value, Color accent) {
        JPanel box = new JPanel(new BorderLayout(4,4)); box.setBackground(BG_DARK);
        box.setBorder(new CompoundBorder(new LineBorder(accent,1,true), new EmptyBorder(8,12,8,12)));
        JLabel lbl = new JLabel(label); lbl.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10)); lbl.setForeground(TEXT_DIM);
        JLabel val = new JLabel(value==null?"—":value); val.setFont(new Font(Font.SANS_SERIF,Font.BOLD,15)); val.setForeground(accent);
        box.add(lbl,BorderLayout.NORTH); box.add(val,BorderLayout.CENTER); return box;
    }
    private JLabel statusBadge(String status) {
        JLabel l = new JLabel();
        l.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));
        switch(status==null?"":status) {
            case "completed": l.setText("✓ completed"); l.setForeground(SUCCESS); break;
            case "running":   l.setText("⟳ running");  l.setForeground(WARNING); break;
            case "failed":    l.setText("✗ failed");   l.setForeground(DANGER);  break;
            default:          l.setText("· pending");  l.setForeground(TEXT_DIM); break;
        }
        return l;
    }
}
