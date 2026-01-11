import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLayeredPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.io.File;
import java.util.Arrays;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

public class Main {
  // Attributes/skills are loaded dynamically from the save file.

  private static void usageAndExit(int code, String msg) {
    if (msg != null && !msg.isBlank()) {
      System.err.println(msg);
      System.err.println();
    }
    System.err.println("Uso:");
    System.err.println("  java -jar oni-save-editor.jar --in <arquivo.sav> --out <novo.sav> [--dupe \"Nome\"] --set Athletics=20 --set Digging=10 ...");
    System.err.println();
    System.err.println("Exemplo (editar todos os duplicantes):");
    System.err.println("  java -jar oni-save-editor.jar --in \"Rosy Brigade.sav\" --out \"Rosy Brigade.edited.sav\" \\");
    System.err.println("    --set Athletics=20 --set Strength=20 --set Digging=20");
    System.exit(code);
  }

  private static Path detectAppHome() {
    // Priority:
    // 1) ONI_EDITOR_HOME env var
    // 2) directory of the running jar (or classpath root when running from sources)
    String env = System.getenv("ONI_EDITOR_HOME");
    if (env != null && !env.isBlank()) {
      return Paths.get(env).toAbsolutePath().normalize();
    }
    try {
      Path self =
          Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
              .toAbsolutePath()
              .normalize();
      return Files.isDirectory(self) ? self : self.getParent();
    } catch (URISyntaxException e) {
      return Paths.get(".").toAbsolutePath().normalize();
    }
  }

  private static void installDuplicityLikeTheme() {
    try {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
    } catch (Exception ignored) {}

    Font base = new Font("Segoe UI", Font.PLAIN, 13);
    var defaults = UIManager.getLookAndFeelDefaults();
    for (Object k : defaults.keySet()) {
      Object v = defaults.get(k);
      if (v instanceof FontUIResource) {
        defaults.put(k, new FontUIResource(base));
      }
    }

    // Slightly more "web app" spacing/colors
    UIManager.put("control", new Color(245, 245, 245));
    UIManager.put("info", new Color(245, 245, 245));
    UIManager.put("nimbusBase", new Color(63, 81, 181)); // indigo-ish
    UIManager.put("nimbusFocus", new Color(63, 81, 181));
    UIManager.put("nimbusSelectionBackground", new Color(63, 81, 181));
  }

  private static List<String> baseSaveToolCommand(Path appHome) throws IOException {
    // Preferred (Windows): packaged tool that doesn't require Node installed.
    Path exe = appHome.resolve("node").resolve("edit-save.exe");
    if (Files.exists(exe)) {
      return new ArrayList<>(List.of(exe.toString()));
    }

    // Next: bundled node.exe (if user drops it in).
    Path nodeExe = appHome.resolve("node").resolve("node.exe");
    Path script = appHome.resolve("node").resolve("edit-save.cjs");
    if (Files.exists(nodeExe) && Files.exists(script)) {
      return new ArrayList<>(List.of(nodeExe.toString(), script.toString()));
    }

    // Fallback: system 'node' in PATH
    if (Files.exists(script)) {
      return new ArrayList<>(List.of("node", script.toString()));
    }

    throw new IOException(
        "Não encontrei o editor do save em:\n"
            + " - "
            + exe
            + "\n - "
            + script
            + "\n\nReinstale o pacote completo (com a pasta node/) ou gere o edit-save.exe.");
  }

  private static void startGui() {
    JFrame frame = new JFrame("ONI Save Editor");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel root = new JPanel();
    root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

    JTextField inField = new JTextField();
    JTextField outField = new JTextField();
    // Dupe selection is now via a list.

    JButton pickIn = new JButton("Escolher .sav (entrada)");
    JButton pickOut = new JButton("Escolher destino (saída)");
    JButton loadDupes = new JButton("Carregar duplicantes");
    JButton loadGeysers = new JButton("Carregar gêiseres");
    JButton loadMaterials = new JButton("Carregar materiais");

    JFileChooser chooser = new JFileChooser();

    pickIn.addActionListener(
        _e -> {
          chooser.setDialogTitle("Escolha o arquivo .sav");
          int res = chooser.showOpenDialog(frame);
          if (res == JFileChooser.APPROVE_OPTION) {
            String p = chooser.getSelectedFile().getAbsolutePath();
            inField.setText(p);
            if (outField.getText().isBlank()) {
              if (p.toLowerCase().endsWith(".sav")) {
                outField.setText(p.substring(0, p.length() - 4) + ".edited.sav");
              } else {
                outField.setText(p + ".edited.sav");
              }
            }
          }
        });

    // Model/state for GUI
    List<DupeInfo> dupes = new ArrayList<>();
    Map<String, DupeEdits> pendingEditsByName = new LinkedHashMap<>();
    Path appHomeAtGuiStart = detectAppHome();
    final Path[] assetsDir = new Path[] {null};
    Path bundledAssets = appHomeAtGuiStart.resolve("assets");
    if (Files.isDirectory(bundledAssets)) {
      assetsDir[0] = bundledAssets;
    }

    JList<String> dupeList = new JList<>();
    dupeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane dupeScroll = new JScrollPane(dupeList);
    dupeScroll.setBorder(BorderFactory.createTitledBorder("Duplicantes (selecione um)"));

    // Geyser tab state
    List<GeyserInfo> geysers = new ArrayList<>();
    Map<String, Map<String, Double>> pendingGeyserEditsById = new LinkedHashMap<>();
    JList<String> geyserList = new JList<>();
    geyserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane geyserScroll = new JScrollPane(geyserList);
    geyserScroll.setBorder(BorderFactory.createTitledBorder("Gêiseres (selecione um)"));
    JTextField geyserTypeId = new JTextField();
    JTextField geyserRateRoll = new JTextField();
    JTextField geyserIterLenRoll = new JTextField();
    JTextField geyserIterPctRoll = new JTextField();
    JTextField geyserYearLenRoll = new JTextField();
    JTextField geyserYearPctRoll = new JTextField();

    // Materials tab state (list only for now)
    List<MaterialInfo> materials = new ArrayList<>();
    JList<String> materialList = new JList<>();
    materialList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane materialScroll = new JScrollPane(materialList);
    materialScroll.setBorder(BorderFactory.createTitledBorder("Materiais (PrimaryElement)"));
    JTextField materialFilter = new JTextField();
    JTextField materialLimit = new JTextField("500");

    pickOut.addActionListener(
        _e -> {
          chooser.setDialogTitle("Escolha onde salvar o .sav editado");
          int res = chooser.showSaveDialog(frame);
          if (res == JFileChooser.APPROVE_OPTION) {
            outField.setText(chooser.getSelectedFile().getAbsolutePath());
          }
        });

    loadDupes.addActionListener(
        _e -> {
          String in = inField.getText().trim();
          if (in.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Escolha o .sav de entrada primeiro.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          loadDupes.setEnabled(false);
          SwingWorker<List<DupeInfo>, String> worker =
              new SwingWorker<>() {
                @Override
                protected List<DupeInfo> doInBackground() throws Exception {
                  Path appHome = detectAppHome();
                  List<String> cmd = baseSaveToolCommand(appHome);
                  cmd.add("--list");
                  cmd.add("--mode");
                  cmd.add("dupes");
                  cmd.add("--in");
                  cmd.add(in);
                  ProcessBuilder pb = new ProcessBuilder(cmd);
                  Process p = pb.start();
                  String json = new String(p.getInputStream().readAllBytes());
                  String err = new String(p.getErrorStream().readAllBytes());
                  int code = p.waitFor();
                  if (code != 0) {
                    throw new IOException("Falha ao ler duplicantes.\n" + err);
                  }
                  return parseDupeListJson(json);
                }

                @Override
                protected void done() {
                  loadDupes.setEnabled(true);
                  try {
                    dupes.clear();
                    dupes.addAll(get());
                    pendingEditsByName.clear();
                    String[] names = dupes.stream().map(d -> d.name).toArray(String[]::new);
                    dupeList.setListData(names);
                    if (names.length > 0) dupeList.setSelectedIndex(0);
                  } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                  }
                }
              };
          worker.execute();
        });

    loadGeysers.addActionListener(
        _e -> {
          String in = inField.getText().trim();
          if (in.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Escolha o .sav de entrada primeiro.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          loadGeysers.setEnabled(false);
          SwingWorker<List<GeyserInfo>, String> worker =
              new SwingWorker<>() {
                @Override
                protected List<GeyserInfo> doInBackground() throws Exception {
                  Path appHome = detectAppHome();
                  List<String> cmd = baseSaveToolCommand(appHome);
                  cmd.add("--list");
                  cmd.add("--mode");
                  cmd.add("geysers");
                  cmd.add("--in");
                  cmd.add(in);
                  ProcessBuilder pb = new ProcessBuilder(cmd);
                  Process p = pb.start();
                  String json = new String(p.getInputStream().readAllBytes());
                  String err = new String(p.getErrorStream().readAllBytes());
                  int code = p.waitFor();
                  if (code != 0) throw new IOException("Falha ao ler gêiseres.\n" + err);
                  return parseGeyserListJson(json);
                }

                @Override
                protected void done() {
                  loadGeysers.setEnabled(true);
                  try {
                    geysers.clear();
                    geysers.addAll(get());
                    pendingGeyserEditsById.clear();
                    String[] labels = geysers.stream().map(g -> g.label).toArray(String[]::new);
                    geyserList.setListData(labels);
                    if (labels.length > 0) geyserList.setSelectedIndex(0);
                  } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                  }
                }
              };
          worker.execute();
        });

    loadMaterials.addActionListener(
        _e -> {
          String in = inField.getText().trim();
          if (in.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Escolha o .sav de entrada primeiro.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          loadMaterials.setEnabled(false);
          SwingWorker<List<MaterialInfo>, String> worker =
              new SwingWorker<>() {
                @Override
                protected List<MaterialInfo> doInBackground() throws Exception {
                  Path appHome = detectAppHome();
                  List<String> cmd = baseSaveToolCommand(appHome);
                  cmd.add("--list");
                  cmd.add("--mode");
                  cmd.add("materials");
                  cmd.add("--in");
                  cmd.add(in);
                  String f = materialFilter.getText().trim();
                  if (!f.isEmpty()) {
                    cmd.add("--filter");
                    cmd.add(f);
                  }
                  int lim = 500;
                  try { lim = Integer.parseInt(materialLimit.getText().trim()); } catch (Exception ignored) {}
                  cmd.add("--limit");
                  cmd.add(Integer.toString(lim));
                  ProcessBuilder pb = new ProcessBuilder(cmd);
                  Process p = pb.start();
                  String json = new String(p.getInputStream().readAllBytes());
                  String err = new String(p.getErrorStream().readAllBytes());
                  int code = p.waitFor();
                  if (code != 0) throw new IOException("Falha ao ler materiais.\n" + err);
                  return parseMaterialListJson(json);
                }

                @Override
                protected void done() {
                  loadMaterials.setEnabled(true);
                  try {
                    materials.clear();
                    materials.addAll(get());
                    String[] labels = materials.stream().map(m -> m.label).toArray(String[]::new);
                    materialList.setListData(labels);
                    if (labels.length > 0) materialList.setSelectedIndex(0);
                  } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                  }
                }
              };
          worker.execute();
        });

    JPanel pIn = new JPanel();
    pIn.setLayout(new BoxLayout(pIn, BoxLayout.Y_AXIS));
    pIn.add(new JLabel("Entrada (.sav):"));
    pIn.add(inField);
    pIn.add(Box.createVerticalStrut(5));
    pIn.add(pickIn);
    pIn.add(Box.createVerticalStrut(5));
    pIn.add(loadDupes);
    pIn.add(Box.createVerticalStrut(5));
    pIn.add(loadGeysers);
    pIn.add(Box.createVerticalStrut(5));
    pIn.add(loadMaterials);

    JPanel pOut = new JPanel();
    pOut.setLayout(new BoxLayout(pOut, BoxLayout.Y_AXIS));
    pOut.add(new JLabel("Saída (.sav):"));
    pOut.add(outField);
    pOut.add(Box.createVerticalStrut(5));
    pOut.add(pickOut);

    // Attributes table (dynamic)
    DefaultTableModel attrModel =
        new DefaultTableModel(new Object[] {"Atributo", "Valor"}, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return column == 1;
          }
        };
    JTable attrTable = new JTable(attrModel);
    TableRowSorter<DefaultTableModel> attrSorter = new TableRowSorter<>(attrModel);
    attrTable.setRowSorter(attrSorter);
    JScrollPane attrScroll = new JScrollPane(attrTable);
    attrScroll.setBorder(BorderFactory.createTitledBorder("Atributos (todos do save)"));

    JTextField attrFilter = new JTextField();
    attrFilter.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                apply();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                apply();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                apply();
              }

              private void apply() {
                String q = attrFilter.getText().trim().toLowerCase();
                if (q.isEmpty()) {
                  attrSorter.setRowFilter(null);
                } else {
                  attrSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q), 0));
                }
              }
            });
    JTextField newAttrKey = new JTextField();
    JTextField newAttrVal = new JTextField();
    JButton addAttr = new JButton("Adicionar/Atualizar atributo");
    addAttr.addActionListener(
        _e -> {
          String k = newAttrKey.getText().trim();
          String v = newAttrVal.getText().trim();
          if (k.isEmpty() || v.isEmpty()) return;
          try {
            Double.parseDouble(v);
          } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Valor inválido: " + v, "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          // update existing row if present
          for (int r = 0; r < attrModel.getRowCount(); r++) {
            if (k.equals(attrModel.getValueAt(r, 0))) {
              attrModel.setValueAt(v, r, 1);
              return;
            }
          }
          attrModel.addRow(new Object[] {k, v});
        });

    JPanel attrControls = new JPanel();
    attrControls.setLayout(new BoxLayout(attrControls, BoxLayout.Y_AXIS));
    attrControls.add(labeledRow("Filtrar:", attrFilter));
    attrControls.add(labeledRow("Novo atributo:", newAttrKey));
    attrControls.add(labeledRow("Valor:", newAttrVal));
    attrControls.add(addAttr);

    // Skills table (dynamic, MasteryBySkillID)
    DefaultTableModel skillModel =
        new DefaultTableModel(new Object[] {"Skill", "Mastered"}, 0) {
          @Override
          public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 1 ? Boolean.class : String.class;
          }

          @Override
          public boolean isCellEditable(int row, int column) {
            return column == 1;
          }
        };
    JTable skillTable = new JTable(skillModel);
    TableRowSorter<DefaultTableModel> skillSorter = new TableRowSorter<>(skillModel);
    skillTable.setRowSorter(skillSorter);
    JScrollPane skillScroll = new JScrollPane(skillTable);
    skillScroll.setBorder(BorderFactory.createTitledBorder("Skills (MasteryBySkillID)"));
    JTextField skillFilter = new JTextField();
    skillFilter.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                apply();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                apply();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                apply();
              }

              private void apply() {
                String q = skillFilter.getText().trim().toLowerCase();
                if (q.isEmpty()) {
                  skillSorter.setRowFilter(null);
                } else {
                  skillSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q), 0));
                }
              }
            });

    JPanel skillControls = new JPanel();
    skillControls.setLayout(new BoxLayout(skillControls, BoxLayout.Y_AXIS));
    skillControls.add(labeledRow("Filtrar:", skillFilter));

    JTextArea log = new JTextArea(10, 60);
    log.setEditable(false);
    JScrollPane logScroll = new JScrollPane(log);
    logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

    JButton saveDupe = new JButton("Salvar alterações deste duplicante");
    JButton run = new JButton("Gerar novo .sav (aplicar todas alterações)");

    JCheckBox editAttributesCheck = new JCheckBox("Editar atributos/skills", true);
    JCheckBox editAppearanceCheck = new JCheckBox("Editar sexo/aparência", false);
    JComboBox<String> genderBox = new JComboBox<>(new String[] {"(não alterar)", "MALE", "FEMALE", "NB"});
    JTextField voiceIdxField = new JTextField();
    JComboBox<String> hairBox = new JComboBox<>(makeOrdinalOptions("hair", 33));
    JComboBox<String> headBox = new JComboBox<>(makeOrdinalOptions("headshape", 4));
    JComboBox<String> eyesBox = new JComboBox<>(makeOrdinalOptions("eyes", 5));
    JTextField mouthField = new JTextField(); // editable, no bundled preview
    JTextField hatField = new JTextField(); // editable, no bundled preview

    // Portrait preview (like Duplicity): stack headshape + eyes + hair
    JLayeredPane portrait = new JLayeredPane();
    portrait.setPreferredSize(new Dimension(160, 160));
    portrait.setMaximumSize(new Dimension(160, 160));
    JLabel headImg = new JLabel();
    JLabel eyesImg = new JLabel();
    JLabel hairImg = new JLabel();
    headImg.setBounds(0, 0, 160, 160);
    eyesImg.setBounds(0, 0, 160, 160);
    hairImg.setBounds(0, 0, 160, 160);
    portrait.add(headImg, Integer.valueOf(0));
    portrait.add(eyesImg, Integer.valueOf(1));
    portrait.add(hairImg, Integer.valueOf(2));

    // Duplicity-like pickers (grid of thumbnails)
    List<JToggleButton> hairToggles = new ArrayList<>();
    List<JToggleButton> headToggles = new ArrayList<>();
    List<JToggleButton> eyesToggles = new ArrayList<>();

    Runnable refreshPortrait =
        () -> {
          String head = selectedAccessory(headBox);
          String eyes = selectedAccessory(eyesBox);
          String hair = selectedAccessory(hairBox);
          headImg.setIcon(loadAccessoryIcon(assetsDir[0], "headshape", head, 160));
          eyesImg.setIcon(loadAccessoryIcon(assetsDir[0], "eyes", eyes, 160));
          hairImg.setIcon(loadAccessoryIcon(assetsDir[0], "hair", hair, 160));
        };

    JScrollPane hairGrid =
        buildPickerGrid(
            "hair",
            "hair",
            33,
            hairBox,
            assetsDir,
            refreshPortrait,
            hairToggles);
    JScrollPane headGrid =
        buildPickerGrid(
            "headshape",
            "headshape",
            4,
            headBox,
            assetsDir,
            refreshPortrait,
            headToggles);
    JScrollPane eyesGrid =
        buildPickerGrid(
            "eyes",
            "eyes",
            5,
            eyesBox,
            assetsDir,
            refreshPortrait,
            eyesToggles);

    // Top controls + portrait (like Duplicity editor)
    JPanel appearanceTop = new JPanel();
    appearanceTop.setLayout(new BoxLayout(appearanceTop, BoxLayout.X_AXIS));

    JPanel portraitCard = new JPanel();
    portraitCard.setLayout(new BoxLayout(portraitCard, BoxLayout.Y_AXIS));
    portraitCard.setBorder(BorderFactory.createTitledBorder("Portrait"));
    portraitCard.add(portrait);

    JPanel identityCard = new JPanel();
    identityCard.setLayout(new BoxLayout(identityCard, BoxLayout.Y_AXIS));
    identityCard.setBorder(BorderFactory.createTitledBorder("Identity"));
    identityCard.add(editAppearanceCheck);
    identityCard.add(Box.createVerticalStrut(6));
    identityCard.add(labeledRow("Gender:", genderBox));
    identityCard.add(labeledRow("VoiceIdx:", voiceIdxField));
    identityCard.add(labeledRow("Mouth (texto):", mouthField));
    identityCard.add(labeledRow("Hat (texto):", hatField));

    appearanceTop.add(portraitCard);
    appearanceTop.add(Box.createHorizontalStrut(10));
    appearanceTop.add(identityCard);
    appearanceTop.add(Box.createHorizontalGlue());

    JTabbedPane appearanceTabs = new JTabbedPane();
    appearanceTabs.addTab("Hair", hairGrid);
    appearanceTabs.addTab("Head", headGrid);
    appearanceTabs.addTab("Eyes", eyesGrid);

    JPanel appearancePanel = new JPanel();
    appearancePanel.setLayout(new BoxLayout(appearancePanel, BoxLayout.Y_AXIS));
    appearancePanel.add(appearanceTop);
    appearancePanel.add(Box.createVerticalStrut(10));
    appearancePanel.add(appearanceTabs);

    hairBox.addActionListener(_e -> refreshPortrait.run());
    headBox.addActionListener(_e -> refreshPortrait.run());
    eyesBox.addActionListener(_e -> refreshPortrait.run());

    // When selecting a dupe, prefill fields with current values (and pending edits if any).
    dupeList.addListSelectionListener(
        _e -> {
          if (_e.getValueIsAdjusting()) return;
          String name = dupeList.getSelectedValue();
          if (name == null) return;
          DupeInfo info = dupes.stream().filter(d -> Objects.equals(d.name, name)).findFirst().orElse(null);
          DupeEdits pending = pendingEditsByName.get(name);
          // fill attributes table
          attrModel.setRowCount(0);
          Map<String, Double> attrs = pending != null && pending.attrs != null ? pending.attrs : (info != null ? info.attrs : null);
          if (attrs != null) {
            attrs.keySet().stream()
                .sorted(String::compareToIgnoreCase)
                .forEach(k -> attrModel.addRow(new Object[] {k, stripTrailingZeros(attrs.get(k))}));
          }

          // fill skills table
          skillModel.setRowCount(0);
          Map<String, Boolean> skills = pending != null && pending.skills != null ? pending.skills : (info != null ? info.skills : null);
          if (skills != null) {
            skills.keySet().stream()
                .sorted(String::compareToIgnoreCase)
                .forEach(k -> skillModel.addRow(new Object[] {k, Boolean.TRUE.equals(skills.get(k))}));
          }

          // Identity
          IdentityInfo id = info != null ? info.identity : null;
          IdentityEdits ped = pending != null ? pending.identity : null;
          String g = ped != null && ped.gender != null ? ped.gender : (id != null ? id.gender : null);
          if (g == null) genderBox.setSelectedIndex(0);
          else genderBox.setSelectedItem(g);
          Integer vIdx = ped != null && ped.voiceIdx != null ? ped.voiceIdx : (id != null ? id.voiceIdx : null);
          voiceIdxField.setText(vIdx == null ? "" : Integer.toString(vIdx));
          // Appearance (Accessorizer)
          Map<String, String> cur = info != null ? info.appearance : null;
          Map<String, String> pend = pending != null ? pending.appearance : null;
          String hair = pend != null && pend.get("hair") != null ? pend.get("hair") : (cur != null ? cur.get("hair") : null);
          String head = pend != null && pend.get("headshape") != null ? pend.get("headshape") : (cur != null ? cur.get("headshape") : null);
          String eyes = pend != null && pend.get("eyes") != null ? pend.get("eyes") : (cur != null ? cur.get("eyes") : null);
          String mouth = pend != null && pend.get("mouth") != null ? pend.get("mouth") : (cur != null ? cur.get("mouth") : null);
          String hat = pend != null && pend.get("hat") != null ? pend.get("hat") : (cur != null ? cur.get("hat") : null);
          selectComboOrPlaceholder(hairBox, hair);
          selectComboOrPlaceholder(headBox, head);
          selectComboOrPlaceholder(eyesBox, eyes);
          mouthField.setText(nullToEmpty(mouth));
          hatField.setText(nullToEmpty(hat));
          refreshPortrait.run();
        });

    saveDupe.addActionListener(
        _e -> {
          String name = dupeList.getSelectedValue();
          if (name == null) {
            JOptionPane.showMessageDialog(frame, "Selecione um duplicante.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          DupeEdits edits = new DupeEdits();
          if (editAttributesCheck.isSelected()) {
            edits.attrs = new LinkedHashMap<>();
            for (int r = 0; r < attrModel.getRowCount(); r++) {
              Object kObj = attrModel.getValueAt(r, 0);
              Object vObj = attrModel.getValueAt(r, 1);
              if (kObj == null || vObj == null) continue;
              String k = kObj.toString().trim();
              String v = vObj.toString().trim();
              if (k.isEmpty() || v.isEmpty()) continue;
              try {
                edits.attrs.put(k, Double.parseDouble(v));
              } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Valor inválido para " + k + ": " + v, "Erro", JOptionPane.ERROR_MESSAGE);
                return;
              }
            }
            if (edits.attrs.isEmpty()) edits.attrs = null;
          }

          if (editAppearanceCheck.isSelected()) {
            edits.identity = new IdentityEdits();
            String sel = (String) genderBox.getSelectedItem();
            if (sel != null && !sel.equals("(não alterar)")) edits.identity.gender = sel;
            String v = voiceIdxField.getText().trim();
            if (!v.isEmpty()) {
              try {
                edits.identity.voiceIdx = Integer.parseInt(v);
              } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "VoiceIdx inválido: " + v, "Erro", JOptionPane.ERROR_MESSAGE);
                return;
              }
            }
            if (edits.identity.voiceIdx == null && edits.identity.gender == null) {
              edits.identity = null;
            }

            edits.appearance = new LinkedHashMap<>();
            putIfNotNull(edits.appearance, "hair", selectedAccessory(hairBox));
            putIfNotNull(edits.appearance, "headshape", selectedAccessory(headBox));
            putIfNotNull(edits.appearance, "eyes", selectedAccessory(eyesBox));
            putIfNotNull(edits.appearance, "mouth", emptyToNull(mouthField.getText()));
            putIfNotNull(edits.appearance, "hat", emptyToNull(hatField.getText()));
            if (edits.appearance.isEmpty()) edits.appearance = null;

            edits.skills = new LinkedHashMap<>();
            for (int r = 0; r < skillModel.getRowCount(); r++) {
              Object kObj = skillModel.getValueAt(r, 0);
              Object vObj = skillModel.getValueAt(r, 1);
              if (kObj == null) continue;
              String k = kObj.toString().trim();
              boolean val = vObj instanceof Boolean ? (Boolean) vObj : Boolean.parseBoolean(String.valueOf(vObj));
              if (!k.isEmpty()) edits.skills.put(k, val);
            }
            if (edits.skills.isEmpty()) edits.skills = null;
          }

          if (edits.attrs == null && edits.identity == null && edits.appearance == null && edits.skills == null) {
            pendingEditsByName.remove(name);
          } else {
            pendingEditsByName.put(name, edits);
          }
          JOptionPane.showMessageDialog(frame, "Alterações salvas para: " + name, "OK", JOptionPane.INFORMATION_MESSAGE);
        });

    // Geyser selection/edits
    geyserList.addListSelectionListener(
        _e -> {
          if (_e.getValueIsAdjusting()) return;
          int idx = geyserList.getSelectedIndex();
          if (idx < 0 || idx >= geysers.size()) return;
          GeyserInfo g = geysers.get(idx);
          Map<String, Double> pending = pendingGeyserEditsById.get(g.id);
          geyserTypeId.setText(g.typeId != null ? g.typeId : "");
          geyserRateRoll.setText(stripTrailingZeros(pick(pending, "rateRoll", g.rateRoll)));
          geyserIterLenRoll.setText(stripTrailingZeros(pick(pending, "iterationLengthRoll", g.iterLenRoll)));
          geyserIterPctRoll.setText(stripTrailingZeros(pick(pending, "iterationPercentRoll", g.iterPctRoll)));
          geyserYearLenRoll.setText(stripTrailingZeros(pick(pending, "yearLengthRoll", g.yearLenRoll)));
          geyserYearPctRoll.setText(stripTrailingZeros(pick(pending, "yearPercentRoll", g.yearPctRoll)));
        });

    JButton saveGeyser = new JButton("Salvar alterações deste gêiser");
    saveGeyser.addActionListener(
        _e -> {
          int idx = geyserList.getSelectedIndex();
          if (idx < 0 || idx >= geysers.size()) {
            JOptionPane.showMessageDialog(frame, "Selecione um gêiser.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          GeyserInfo g = geysers.get(idx);
          Map<String, Double> ed = new LinkedHashMap<>();
          tryPutDouble(ed, "rateRoll", geyserRateRoll.getText());
          tryPutDouble(ed, "iterationLengthRoll", geyserIterLenRoll.getText());
          tryPutDouble(ed, "iterationPercentRoll", geyserIterPctRoll.getText());
          tryPutDouble(ed, "yearLengthRoll", geyserYearLenRoll.getText());
          tryPutDouble(ed, "yearPercentRoll", geyserYearPctRoll.getText());
          // typeId is string; keep in dupe patch only for now (optional)
          if (ed.isEmpty()) pendingGeyserEditsById.remove(g.id);
          else pendingGeyserEditsById.put(g.id, ed);
          JOptionPane.showMessageDialog(frame, "Alterações salvas para o gêiser: " + g.label, "OK", JOptionPane.INFORMATION_MESSAGE);
        });

    run.addActionListener(
        _e -> {
          String in = inField.getText().trim();
          String out = outField.getText().trim();
          if (in.isEmpty() || out.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Preencha entrada e saída.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }
          if (pendingEditsByName.isEmpty() && pendingGeyserEditsById.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Você ainda não salvou alterações (duplicantes ou gêiseres).", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
          }

          run.setEnabled(false);
          saveDupe.setEnabled(false);
          log.setText("");

          SwingWorker<Integer, String> worker =
              new SwingWorker<>() {
                @Override
                protected Integer doInBackground() throws Exception {
                  Path appHome = detectAppHome();
                  List<String> cmd = baseSaveToolCommand(appHome);

                  // Write patch file
                  Path patch =
                      Files.createTempFile("oni-save-editor-" + UUID.randomUUID(), ".json")
                          .toAbsolutePath()
                          .normalize();
                  String patchJson = buildPatchJson(pendingEditsByName, pendingGeyserEditsById);
                  Files.writeString(patch, patchJson, StandardOpenOption.TRUNCATE_EXISTING);
                  cmd.add("--apply-json");
                  cmd.add(patch.toString());
                  cmd.add("--in");
                  cmd.add(in);
                  cmd.add("--out");
                  cmd.add(out);

                  ProcessBuilder pb = new ProcessBuilder(cmd);
                  Process p;
                  try {
                    p = pb.start();
                  } catch (IOException ex) {
                    publish(
                        "ERRO ao executar o editor do save.\n"
                            + "Dica (Windows): use o pacote com node/edit-save.exe.\n\n"
                            + ex
                            + "\n");
                    return 2;
                  }

                  Thread t1 =
                      new Thread(
                          () -> {
                            try (InputStream is = p.getInputStream()) {
                              is.transferTo(new java.io.OutputStream() {
                                @Override
                                public void write(int b) {
                                  publish(String.valueOf((char) b));
                                }
                              });
                            } catch (IOException ignored) {}
                          });
                  Thread t2 =
                      new Thread(
                          () -> {
                            try (InputStream is = p.getErrorStream()) {
                              is.transferTo(new java.io.OutputStream() {
                                @Override
                                public void write(int b) {
                                  publish(String.valueOf((char) b));
                                }
                              });
                            } catch (IOException ignored) {}
                          });
                  t1.start();
                  t2.start();

                  int code = p.waitFor();
                  t1.join();
                  t2.join();
                  return code;
                }

                @Override
                protected void process(List<String> chunks) {
                  for (String c : chunks) log.append(c);
                }

                @Override
                protected void done() {
                  run.setEnabled(true);
                  saveDupe.setEnabled(true);
                  try {
                    int code = get();
                    if (code == 0) {
                      JOptionPane.showMessageDialog(frame, "Pronto! Save salvo em:\n" + out, "OK", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                      JOptionPane.showMessageDialog(frame, "Falhou (código " + code + "). Veja o log.", "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                  } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Erro: " + ex, "Erro", JOptionPane.ERROR_MESSAGE);
                  }
                }
              };
          worker.execute();
        });

    // Tabs
    JTabbedPane tabs = new JTabbedPane();

    // Duplicantes tab
    JPanel dupTab = new JPanel();
    dupTab.setLayout(new BoxLayout(dupTab, BoxLayout.Y_AXIS));
    dupTab.add(dupeScroll);
    dupTab.add(Box.createVerticalStrut(8));

    JTabbedPane dupeTabs = new JTabbedPane();
    JPanel attrTab = new JPanel();
    attrTab.setLayout(new BoxLayout(attrTab, BoxLayout.Y_AXIS));
    attrTab.add(editAttributesCheck);
    attrTab.add(Box.createVerticalStrut(6));
    attrTab.add(attrControls);
    attrTab.add(Box.createVerticalStrut(8));
    attrTab.add(attrScroll);
    attrTab.add(Box.createVerticalStrut(8));
    attrTab.add(skillControls);
    attrTab.add(Box.createVerticalStrut(8));
    attrTab.add(skillScroll);
    dupeTabs.addTab("Atributos/Skills", attrTab);

    JPanel appTab = new JPanel();
    appTab.setLayout(new BoxLayout(appTab, BoxLayout.Y_AXIS));
    appTab.add(appearancePanel);
    dupeTabs.addTab("Sexo/Aparência", appTab);

    dupTab.add(dupeTabs);
    dupTab.add(Box.createVerticalStrut(8));
    dupTab.add(saveDupe);
    tabs.addTab("Duplicantes", dupTab);

    // Gêiseres tab
    JPanel geyserTab = new JPanel();
    geyserTab.setLayout(new BoxLayout(geyserTab, BoxLayout.Y_AXIS));
    geyserTab.add(geyserScroll);
    geyserTab.add(Box.createVerticalStrut(8));
    JPanel gFields = new JPanel();
    gFields.setLayout(new BoxLayout(gFields, BoxLayout.Y_AXIS));
    gFields.setBorder(BorderFactory.createTitledBorder("Configuração (Geyser)"));
    gFields.add(labeledRow("TypeId (somente leitura por enquanto):", geyserTypeId));
    geyserTypeId.setEditable(false);
    gFields.add(labeledRow("rateRoll:", geyserRateRoll));
    gFields.add(labeledRow("iterationLengthRoll:", geyserIterLenRoll));
    gFields.add(labeledRow("iterationPercentRoll:", geyserIterPctRoll));
    gFields.add(labeledRow("yearLengthRoll:", geyserYearLenRoll));
    gFields.add(labeledRow("yearPercentRoll:", geyserYearPctRoll));
    geyserTab.add(gFields);
    geyserTab.add(Box.createVerticalStrut(8));
    geyserTab.add(saveGeyser);
    tabs.addTab("Gêiseres", geyserTab);

    // Materiais tab (listagem + filtro)
    JPanel matTab = new JPanel();
    matTab.setLayout(new BoxLayout(matTab, BoxLayout.Y_AXIS));
    JPanel matControls = new JPanel();
    matControls.setLayout(new BoxLayout(matControls, BoxLayout.Y_AXIS));
    matControls.setBorder(BorderFactory.createTitledBorder("Filtro"));
    matControls.add(labeledRow("Filtrar (elementId ou grupo):", materialFilter));
    matControls.add(labeledRow("Limite:", materialLimit));
    matTab.add(matControls);
    matTab.add(Box.createVerticalStrut(8));
    matTab.add(materialScroll);
    tabs.addTab("Materiais", matTab);

    // Settings tab (assets dir for previews)
    JPanel settingsTab = new JPanel();
    settingsTab.setLayout(new BoxLayout(settingsTab, BoxLayout.Y_AXIS));
    JTextField assetsField = new JTextField();
    assetsField.setEditable(false);
    JButton pickAssets = new JButton("Escolher pasta de assets (preview)");
    pickAssets.addActionListener(
        _e -> {
          JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          fc.setDialogTitle("Escolha a pasta de assets (contendo subpastas eyes/hair/headshape/etc)");
          int res = fc.showOpenDialog(frame);
          if (res == JFileChooser.APPROVE_OPTION) {
            assetsDir[0] = fc.getSelectedFile().toPath();
            assetsField.setText(assetsDir[0].toString());
            refreshPortrait.run();
          }
        });
    if (assetsDir[0] != null) {
      assetsField.setText(assetsDir[0].toString());
    }
    settingsTab.add(new JLabel("Preview de aparência: coloque imagens em:"));
    settingsTab.add(new JLabel("  <assets>/eyes/eyes_001.png, <assets>/hair/hair_001.png, <assets>/headshape/headshape_001.png, etc."));
    settingsTab.add(Box.createVerticalStrut(8));
    settingsTab.add(assetsField);
    settingsTab.add(Box.createVerticalStrut(6));
    settingsTab.add(pickAssets);
    tabs.addTab("Config", settingsTab);

    // Layout
    root.add(pIn);
    root.add(Box.createVerticalStrut(8));
    root.add(pOut);
    root.add(Box.createVerticalStrut(8));
    root.add(tabs);
    root.add(Box.createVerticalStrut(8));
    root.add(run);
    root.add(Box.createVerticalStrut(8));
    root.add(logScroll);

    frame.setContentPane(root);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel labeledRow(String label, java.awt.Component component) {
    JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.add(new JLabel(label));
    row.add(Box.createHorizontalStrut(6));
    row.add(component);
    row.add(Box.createHorizontalGlue());
    return row;
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String emptyToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String[] makeOrdinalOptions(String prefix, int count) {
    String[] out = new String[count + 1];
    out[0] = "(não alterar)";
    for (int i = 1; i <= count; i++) {
      out[i] = prefix + "_" + String.format("%03d", i);
    }
    return out;
  }

  private static String selectedAccessory(JComboBox<String> box) {
    Object sel = box.getSelectedItem();
    if (sel == null) return null;
    String s = sel.toString().trim();
    if (s.isEmpty() || s.equals("(não alterar)")) return null;
    if (s.startsWith("Root.Accessories.")) s = s.substring("Root.Accessories.".length());
    return s;
  }

  private static void selectComboOrPlaceholder(JComboBox<String> box, String value) {
    if (value == null || value.isBlank()) {
      box.setSelectedIndex(0);
      return;
    }
    String v = value.trim();
    if (v.startsWith("Root.Accessories.")) v = v.substring("Root.Accessories.".length());
    box.setSelectedItem(v);
  }

  private static void putIfNotNull(Map<String, String> map, String key, String value) {
    if (map == null) return;
    if (value == null || value.isBlank()) return;
    map.put(key, value.trim());
  }

  private static JScrollPane buildPickerGrid(
      String assetType,
      String prefix,
      int count,
      JComboBox<String> targetCombo,
      Path[] assetsDir,
      Runnable onChange,
      List<JToggleButton> togglesOut) {
    JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

    ButtonGroup group = new ButtonGroup();

    // "no change" option
    JToggleButton none = new JToggleButton("—");
    none.setToolTipText("(não alterar)");
    none.setPreferredSize(new Dimension(64, 64));
    none.addActionListener(
        _e -> {
          targetCombo.setSelectedIndex(0);
          onChange.run();
        });
    group.add(none);
    grid.add(none);
    togglesOut.add(none);

    for (int i = 1; i <= count; i++) {
      String name = prefix + "_" + String.format("%03d", i);
      ImageIcon icon = loadAccessoryIcon(assetsDir[0], assetType, name, 64);
      JToggleButton b = new JToggleButton();
      b.setIcon(icon);
      b.setToolTipText(name);
      b.setPreferredSize(new Dimension(64, 64));
      b.setActionCommand(name);
      b.addActionListener(
          _e -> {
            targetCombo.setSelectedItem(name);
            onChange.run();
          });
      group.add(b);
      grid.add(b);
      togglesOut.add(b);
    }

    JScrollPane scroll = new JScrollPane(grid);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

  private static ImageIcon loadAccessoryIcon(Path assetsDir, String type, String name, int size) {
    if (assetsDir == null) return null;
    String n = name == null ? "" : name.trim();
    if (n.startsWith("Root.Accessories.")) n = n.substring("Root.Accessories.".length());
    if (n.isEmpty() || n.equals("(não alterar)")) return null;
    Path img = assetsDir.resolve("duplicant").resolve(type).resolve(n).resolve(n + "_0.png");
    if (!Files.exists(img)) return null;
    try {
      Image im = ImageIO.read(img.toFile());
      if (im == null) return null;
      Image scaled = im.getScaledInstance(size, size, Image.SCALE_SMOOTH);
      return new ImageIcon(scaled);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Minimal JSON parser so we don't depend on external jars.
   * Supports objects, arrays, strings (with escapes), numbers, booleans, null.
   */
  private static final class Json {
    static Object parse(String s) {
      if (s == null) return null;
      Parser p = new Parser(s);
      Object v = p.parseValue();
      p.skipWs();
      return v;
    }

    private static final class Parser {
      private final String s;
      private int i = 0;

      Parser(String s) {
        this.s = s;
      }

      void skipWs() {
        while (i < s.length()) {
          char c = s.charAt(i);
          if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
          else break;
        }
      }

      Object parseValue() {
        skipWs();
        if (i >= s.length()) return null;
        char c = s.charAt(i);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' && s.startsWith("true", i)) {
          i += 4;
          return Boolean.TRUE;
        }
        if (c == 'f' && s.startsWith("false", i)) {
          i += 5;
          return Boolean.FALSE;
        }
        if (c == 'n' && s.startsWith("null", i)) {
          i += 4;
          return null;
        }
        return parseNumber();
      }

      Map<String, Object> parseObject() {
        Map<String, Object> out = new LinkedHashMap<>();
        expect('{');
        skipWs();
        if (peek('}')) {
          i++;
          return out;
        }
        while (i < s.length()) {
          skipWs();
          String key = parseString();
          skipWs();
          expect(':');
          Object val = parseValue();
          out.put(key, val);
          skipWs();
          if (peek('}')) {
            i++;
            break;
          }
          expect(',');
        }
        return out;
      }

      List<Object> parseArray() {
        List<Object> out = new ArrayList<>();
        expect('[');
        skipWs();
        if (peek(']')) {
          i++;
          return out;
        }
        while (i < s.length()) {
          Object v = parseValue();
          out.add(v);
          skipWs();
          if (peek(']')) {
            i++;
            break;
          }
          expect(',');
        }
        return out;
      }

      String parseString() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (i < s.length()) {
          char c = s.charAt(i++);
          if (c == '"') break;
          if (c != '\\') {
            b.append(c);
            continue;
          }
          if (i >= s.length()) break;
          char e = s.charAt(i++);
          switch (e) {
            case '"' -> b.append('"');
            case '\\' -> b.append('\\');
            case '/' -> b.append('/');
            case 'b' -> b.append('\b');
            case 'f' -> b.append('\f');
            case 'n' -> b.append('\n');
            case 'r' -> b.append('\r');
            case 't' -> b.append('\t');
            case 'u' -> {
              if (i + 4 <= s.length()) {
                String hex = s.substring(i, i + 4);
                i += 4;
                try {
                  b.append((char) Integer.parseInt(hex, 16));
                } catch (NumberFormatException ignored) {}
              }
            }
            default -> b.append(e);
          }
        }
        return b.toString();
      }

      Number parseNumber() {
        int start = i;
        while (i < s.length()) {
          char c = s.charAt(i);
          if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') i++;
          else break;
        }
        String num = s.substring(start, i);
        try {
          if (num.contains(".") || num.contains("e") || num.contains("E")) return Double.parseDouble(num);
          long l = Long.parseLong(num);
          if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
          return l;
        } catch (NumberFormatException e) {
          return 0;
        }
      }

      void expect(char c) {
        if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("JSON parse error at " + i);
        i++;
      }

      boolean peek(char c) {
        return i < s.length() && s.charAt(i) == c;
      }
    }
  }

  private static double pick(Map<String, Double> pending, String key, double fallback) {
    if (pending == null) return fallback;
    Double v = pending.get(key);
    return v == null ? fallback : v;
  }

  private static void tryPutDouble(Map<String, Double> map, String key, String text) {
    String t = text == null ? "" : text.trim();
    if (t.isEmpty()) return;
    try {
      map.put(key, Double.parseDouble(t));
    } catch (NumberFormatException ignored) {
      // leave invalid values to the caller UI validation (kept minimal)
    }
  }

  private static String stripTrailingZeros(double v) {
    if (v == (long) v) return Long.toString((long) v);
    return Double.toString(v);
  }

  private static final Pattern DUPE_ENTRY =
      Pattern.compile(
          "\\{\\s*\"name\"\\s*:\\s*\"(?<name>(?:\\\\.|[^\"])*)\"\\s*,\\s*\"attrs\"\\s*:\\s*\\{(?<attrs>[^}]*)\\}\\s*\\}");

  private static final Pattern ATTR_ENTRY =
      Pattern.compile(
          "\"(?<key>(?:\\\\.|[^\"])*)\"\\s*:\\s*(?<val>-?\\d+(?:\\.\\d+)?)");

  // Very small JSON extractor for the controlled output of node/edit-save.cjs --list (dupes)
  @SuppressWarnings("unchecked")
  private static List<DupeInfo> parseDupeListJson(String json) {
    Object rootObj = Json.parse(json);
    if (!(rootObj instanceof Map)) return List.of();
    Map<String, Object> root = (Map<String, Object>) rootObj;
    Object dupesObj = root.get("dupes");
    if (!(dupesObj instanceof List)) return List.of();
    List<Object> list = (List<Object>) dupesObj;
    List<DupeInfo> out = new ArrayList<>();
    for (Object o : list) {
      if (!(o instanceof Map)) continue;
      Map<String, Object> m = (Map<String, Object>) o;
      String name = (String) m.get("name");
      Map<String, Double> attrs = toDoubleMap(m.get("attrs"));
      Map<String, String> appearance = toStringMap(m.get("appearance"));
      Map<String, Boolean> skills = toBoolMap(m.get("skills"));
      IdentityInfo identity = null;
      Object idObj = m.get("identity");
      if (idObj instanceof Map) {
        Map<String, Object> im = (Map<String, Object>) idObj;
        String gender = (String) im.get("gender");
        Integer voiceIdx = im.get("voiceIdx") instanceof Number ? ((Number) im.get("voiceIdx")).intValue() : null;
        if (gender != null || voiceIdx != null) identity = new IdentityInfo(gender, voiceIdx);
      }
      if (name != null) out.add(new DupeInfo(name, attrs, identity, appearance, skills));
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Double> toDoubleMap(Object obj) {
    if (!(obj instanceof Map)) return null;
    Map<String, Object> m = (Map<String, Object>) obj;
    Map<String, Double> out = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : m.entrySet()) {
      if (e.getValue() instanceof Number) out.put(e.getKey(), ((Number) e.getValue()).doubleValue());
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Boolean> toBoolMap(Object obj) {
    if (!(obj instanceof Map)) return null;
    Map<String, Object> m = (Map<String, Object>) obj;
    Map<String, Boolean> out = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : m.entrySet()) {
      Object v = e.getValue();
      if (v instanceof Boolean) out.put(e.getKey(), (Boolean) v);
      else if (v instanceof Number) out.put(e.getKey(), ((Number) v).intValue() != 0);
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> toStringMap(Object obj) {
    if (!(obj instanceof Map)) return null;
    Map<String, Object> m = (Map<String, Object>) obj;
    Map<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : m.entrySet()) {
      if (e.getValue() instanceof String) out.put(e.getKey(), (String) e.getValue());
    }
    return out;
  }

  private static String extractJsonString(String json, String key) {
    Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(?<v>(?:\\\\.|[^\"])*)\"");
    Matcher m = p.matcher(json);
    if (!m.find()) return null;
    return unescapeJsonString(m.group("v"));
  }

  private static Integer extractJsonInt(String json, String key) {
    Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(?<v>-?\\d+)");
    Matcher m = p.matcher(json);
    if (!m.find()) return null;
    return Integer.parseInt(m.group("v"));
  }

  private static String extractJsonObject(String json, String key) {
    Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\{(?<v>[^}]*)\\}");
    Matcher m = p.matcher(json);
    if (!m.find()) return null;
    return "{" + m.group("v") + "}";
  }

  // Geysers/materials list parsing (controlled output)
  private static final Pattern GEYSER_ENTRY =
      Pattern.compile("\"id\"\\s*:\\s*\"(?<id>[^\"]+)\"[^}]*\"typeId\"\\s*:\\s*(?:\"(?<type>[^\"]*)\"|null)[^}]*\"configuration\"\\s*:\\s*\\{(?<cfg>[^}]*)\\}");

  private static final Pattern CFG_NUM =
      Pattern.compile("\"(?<k>rateRoll|iterationLengthRoll|iterationPercentRoll|yearLengthRoll|yearPercentRoll)\"\\s*:\\s*(?<v>-?\\d+(?:\\.\\d+)?)");

  private static List<GeyserInfo> parseGeyserListJson(String json) {
    List<GeyserInfo> out = new ArrayList<>();
    Matcher m = GEYSER_ENTRY.matcher(json);
    while (m.find()) {
      String id = m.group("id");
      String type = m.group("type");
      String cfg = m.group("cfg");
      double rate = 0, il = 0, ip = 0, yl = 0, yp = 0;
      Matcher c = CFG_NUM.matcher(cfg);
      while (c.find()) {
        String k = c.group("k");
        double v = Double.parseDouble(c.group("v"));
        switch (k) {
          case "rateRoll" -> rate = v;
          case "iterationLengthRoll" -> il = v;
          case "iterationPercentRoll" -> ip = v;
          case "yearLengthRoll" -> yl = v;
          case "yearPercentRoll" -> yp = v;
        }
      }
      String label = "ID " + id + (type != null ? (" - " + type) : "");
      out.add(new GeyserInfo(id, label, type, rate, il, ip, yl, yp));
    }
    return out;
  }

  private static final Pattern MATERIAL_ENTRY =
      Pattern.compile("\"id\"\\s*:\\s*\"(?<id>[^\"]+)\"[^}]*\"group\"\\s*:\\s*\"(?<group>[^\"]*)\"[^}]*\"elementId\"\\s*:\\s*(?:\"(?<el>[^\"]*)\"|null)");

  private static List<MaterialInfo> parseMaterialListJson(String json) {
    List<MaterialInfo> out = new ArrayList<>();
    Matcher m = MATERIAL_ENTRY.matcher(json);
    while (m.find()) {
      String id = m.group("id");
      String group = m.group("group");
      String el = m.group("el");
      String label = "ID " + id + " - " + group + " - " + (el != null ? el : "null");
      out.add(new MaterialInfo(id, label));
    }
    return out;
  }

  private static String unescapeJsonString(String s) {
    // Minimal unescape for the output we generate (\", \\ and \n/\r/\t)
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c != '\\') {
        b.append(c);
        continue;
      }
      if (i + 1 >= s.length()) break;
      char n = s.charAt(++i);
      switch (n) {
        case '\\' -> b.append('\\');
        case '"' -> b.append('"');
        case 'n' -> b.append('\n');
        case 'r' -> b.append('\r');
        case 't' -> b.append('\t');
        default -> b.append(n);
      }
    }
    return b.toString();
  }

  private static String buildPatchJson(Map<String, DupeEdits> pendingEditsByName, Map<String, Map<String, Double>> pendingGeyserEditsById) {
    StringBuilder b = new StringBuilder();
    b.append("{\"dupes\":{");
    boolean firstDupe = true;
    for (Map.Entry<String, DupeEdits> e : pendingEditsByName.entrySet()) {
      if (!firstDupe) b.append(',');
      firstDupe = false;
      b.append('"').append(escapeJson(e.getKey())).append("\":{");
      boolean wroteAny = false;
      DupeEdits de = e.getValue();
      if (de != null && de.attrs != null && !de.attrs.isEmpty()) {
        b.append("\"attrs\":{");
        boolean firstKey = true;
        for (Map.Entry<String, Double> kv : de.attrs.entrySet()) {
          if (!firstKey) b.append(',');
          firstKey = false;
          b.append('"').append(escapeJson(kv.getKey())).append("\":").append(stripTrailingZeros(kv.getValue()));
        }
        b.append("}");
        wroteAny = true;
      }
      if (de != null && de.identity != null) {
        if (wroteAny) b.append(',');
        b.append("\"identity\":{");
        boolean first = true;
        if (de.identity.gender != null) {
          b.append("\"gender\":\"").append(escapeJson(de.identity.gender)).append("\"");
          first = false;
        }
        if (de.identity.voiceIdx != null) {
          if (!first) b.append(',');
          b.append("\"voiceIdx\":").append(de.identity.voiceIdx);
          first = false;
        }
        b.append("}");
        wroteAny = true;
      }
      if (de != null && de.appearance != null && !de.appearance.isEmpty()) {
        if (wroteAny) b.append(',');
        b.append("\"appearance\":{");
        boolean fa = true;
        for (Map.Entry<String, String> kv : de.appearance.entrySet()) {
          if (kv.getValue() == null) continue;
          if (!fa) b.append(',');
          fa = false;
          b.append('"').append(escapeJson(kv.getKey())).append("\":\"").append(escapeJson(kv.getValue())).append('"');
        }
        b.append("}");
        wroteAny = true;
      }
      if (de != null && de.skills != null && !de.skills.isEmpty()) {
        if (wroteAny) b.append(',');
        b.append("\"skills\":{");
        boolean fs = true;
        for (Map.Entry<String, Boolean> kv : de.skills.entrySet()) {
          if (!fs) b.append(',');
          fs = false;
          b.append('"').append(escapeJson(kv.getKey())).append("\":").append(Boolean.TRUE.equals(kv.getValue()) ? "true" : "false");
        }
        b.append("}");
      }
      b.append('}');
    }

    b.append("},\"objects\":{");
    boolean firstObj = true;
    for (Map.Entry<String, Map<String, Double>> e : pendingGeyserEditsById.entrySet()) {
      if (!firstObj) b.append(',');
      firstObj = false;
      b.append('"').append(escapeJson(e.getKey())).append("\":{\"geyser\":{");
      boolean fk = true;
      for (Map.Entry<String, Double> kv : e.getValue().entrySet()) {
        if (!fk) b.append(',');
        fk = false;
        b.append('"').append(escapeJson(kv.getKey())).append("\":").append(stripTrailingZeros(kv.getValue()));
      }
      b.append("}}");
    }
    b.append("}}");
    return b.toString();
  }

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private record IdentityInfo(String gender, Integer voiceIdx) {}
  private record DupeInfo(String name, Map<String, Double> attrs, IdentityInfo identity, Map<String, String> appearance, Map<String, Boolean> skills) {}

  private static class IdentityEdits {
    String gender;
    Integer voiceIdx;
  }

  private static class DupeEdits {
    Map<String, Double> attrs;
    IdentityEdits identity;
    Map<String, String> appearance;
    Map<String, Boolean> skills;
  }

  private record GeyserInfo(String id, String label, String typeId, double rateRoll, double iterLenRoll, double iterPctRoll, double yearLenRoll, double yearPctRoll) {}
  private record MaterialInfo(String id, String label) {}

  private static void updateAccessoryPreview(Path assetsDir, String type, String name, JLabel label) {
    if (assetsDir == null) {
      label.setIcon(null);
      label.setText("(sem pasta de assets)");
      return;
    }
    String n = name == null ? "" : name.trim();
    if (n.startsWith("Root.Accessories.")) {
      n = n.substring("Root.Accessories.".length());
    }
    if (n.isEmpty()) {
      label.setIcon(null);
      label.setText("(vazio)");
      return;
    }
    // preferred (bundled): <assets>/duplicant/<type>/<name>/<name>_0.png
    Path img = assetsDir.resolve("duplicant").resolve(type).resolve(n).resolve(n + "_0.png");
    if (!Files.exists(img)) {
      // fallback (old layout): <assets>/<type>/<name>.png
      img = assetsDir.resolve(type).resolve(n + ".png");
    }
    if (!Files.exists(img)) {
      label.setIcon(null);
      label.setText("(não encontrado: " + n + ")");
      return;
    }
    try {
      Image im = ImageIO.read(img.toFile());
      if (im == null) throw new IOException("Imagem inválida");
      Image scaled = im.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
      label.setText("");
      label.setIcon(new ImageIcon(scaled));
    } catch (Exception e) {
      label.setIcon(null);
      label.setText("(erro ao carregar)");
    }
  }

  public static void main(String[] argv) throws Exception {
    // If launched without args, open a clickable GUI.
    if (argv.length == 0) {
      installDuplicityLikeTheme();
      SwingUtilities.invokeLater(Main::startGui);
      return;
    }

    String in = null;
    String out = null;
    List<String> dupes = new ArrayList<>();
    List<String> sets = new ArrayList<>();

    for (int i = 0; i < argv.length; i++) {
      String a = argv[i];
      switch (a) {
        case "--in" -> {
          if (i + 1 >= argv.length) usageAndExit(2, "Faltou o valor de --in");
          in = argv[++i];
        }
        case "--out" -> {
          if (i + 1 >= argv.length) usageAndExit(2, "Faltou o valor de --out");
          out = argv[++i];
        }
        case "--dupe" -> {
          if (i + 1 >= argv.length) usageAndExit(2, "Faltou o valor de --dupe");
          dupes.add(argv[++i]);
        }
        case "--set" -> {
          if (i + 1 >= argv.length) usageAndExit(2, "Faltou o valor de --set");
          sets.add(argv[++i]);
        }
        case "--help", "-h" -> usageAndExit(0, null);
        default -> usageAndExit(2, "Argumento desconhecido: " + a);
      }
    }

    if (in == null || out == null || sets.isEmpty()) {
      usageAndExit(2, "Você precisa passar --in, --out e pelo menos um --set.");
    }

    Path appHome = detectAppHome();
    List<String> cmd;
    try {
      cmd = baseSaveToolCommand(appHome);
    } catch (IOException e) {
      usageAndExit(2, e.getMessage());
      return;
    }
    cmd.add("--in");
    cmd.add(in);
    cmd.add("--out");
    cmd.add(out);
    for (String d : dupes) {
      cmd.add("--dupe");
      cmd.add(d);
    }
    for (String s : sets) {
      cmd.add("--set");
      cmd.add(s);
    }

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.inheritIO();

    Process p;
    try {
      p = pb.start();
    } catch (IOException e) {
      usageAndExit(2, "Falha ao executar o editor do save.\n" + e);
      return;
    }

    int code = p.waitFor();
    if (code != 0) {
      System.exit(code);
    }
  }
}
