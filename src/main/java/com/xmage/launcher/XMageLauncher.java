
package com.xmage.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author BetaSteward
 */
public class XMageLauncher implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(XMageLauncher.class);
    
    private final ResourceBundle messages;
    private final Locale locale;
    
    private final JFrame frame;
    private final JLabel mainPanel;
    private final JLabel labelProgress;
    private final JProgressBar progressBar;
    private final JTextArea textArea;
    private final JButton btnLaunchClient;
    private final JLabel xmageLogo;
    private final JButton btnLaunchServer;
    private final JButton btnLaunchClientServer;
    private final JScrollPane scrollPane;
    private final JButton btnDownloadJava;
    private final JButton btnDownloadXMage;
    private final JButton btnTorrentXMage;
    private final JButton btnSeedXMage;
    
    private JSONObject config;
    private File path;
    
    private Point grabPoint;

    private Process serverProcess;
    private XMageConsole serverConsole;
    private XMageConsole clientConsole;
        
    private JToolBar toolBar;
    
    private XMageLauncher() {
        locale = Locale.getDefault();
        //locale = new Locale("it", "IT");
        messages = ResourceBundle.getBundle("MessagesBundle", locale);
        localize();
        
        serverConsole = new XMageConsole("XMage Server console");
        clientConsole = new XMageConsole("XMage Client console");
        
        frame = new JFrame(messages.getString("frameTitle") + " " + Config.getVersion());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 500));
        frame.setResizable(false);
        
        createToolbar();
        
        ImageIcon icon = new ImageIcon(XMageLauncher.class.getResource("/icon-mage-flashed.png"));
        frame.setIconImage(icon.getImage());
        
        Random r = new Random();
        int imageNum = 1 + r.nextInt(17);
        ImageIcon background = new ImageIcon(new ImageIcon(XMageLauncher.class.getResource("/backgrounds/" + Integer.toString(imageNum) + ".jpg")).getImage().getScaledInstance(800, 500, Image.SCALE_SMOOTH));
        mainPanel = new JLabel(background) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                Dimension lmPrefSize = getLayout().preferredLayoutSize(this);
                size.width = Math.max(size.width, lmPrefSize.width);
                size.height = Math.max(size.height, lmPrefSize.height);
                return size;
            }
        };
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                grabPoint = e.getPoint();
                mainPanel.getComponentAt(grabPoint);
            }
        });
        mainPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {

                // get location of Window
                int thisX = frame.getLocation().x;
                int thisY = frame.getLocation().y;

                // Determine how much the mouse moved since the initial click
                int xMoved = (thisX + e.getX()) - (thisX + grabPoint.x);
                int yMoved = (thisY + e.getY()) - (thisY + grabPoint.y);

                // Move window to this position
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                frame.setLocation(X, Y);
            }
        });
        mainPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 10, 10, 10);
    
        Font font14 = new Font("Arial", Font.BOLD, 14);
        Font font12 = new Font("Arial", Font.PLAIN, 12);

        mainPanel.add(Box.createHorizontalStrut(250));
        mainPanel.add(Box.createVerticalStrut(50));

        ImageIcon logo = new ImageIcon(new ImageIcon(XMageLauncher.class.getResource("/label-xmage.png")).getImage().getScaledInstance(150, 75, Image.SCALE_SMOOTH));
        xmageLogo = new JLabel(logo);
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        mainPanel.add(xmageLogo, constraints);
        
        btnLaunchClient = new JButton(messages.getString("launchClient"));
        btnLaunchClient.setToolTipText(messages.getString("launchClient.tooltip"));
        btnLaunchClient.setFont(font14);
        btnLaunchClient.setForeground(Color.GRAY);
        btnLaunchClient.setEnabled(false);

        btnLaunchClient.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleClient();
            }
        });      

        constraints.gridx = 4;
        constraints.gridy = 3;
        constraints.weightx = 0.25;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(btnLaunchClient, constraints);
        
        btnLaunchClientServer = new JButton(messages.getString("launchClientServer"));
        btnLaunchClientServer.setToolTipText(messages.getString("launchClientServer.tooltip"));
        btnLaunchClientServer.setFont(font14);
        btnLaunchClientServer.setEnabled(false);
        btnLaunchClientServer.setForeground(Color.GRAY);
        btnLaunchClientServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleServer();
                handleClient();
            }
        });      

        constraints.gridx = 2;
        mainPanel.add(btnLaunchClientServer, constraints);

        btnLaunchServer = new JButton(messages.getString("launchServer"));
        btnLaunchServer.setToolTipText(messages.getString("launchServer.tooltip"));
        btnLaunchServer.setFont(font14);
        btnLaunchServer.setEnabled(false);
        btnLaunchServer.setForeground(Color.GRAY);
        btnLaunchServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleServer();
            }
        });      

        constraints.gridx = 3;
        mainPanel.add(btnLaunchServer, constraints);
                
        btnDownloadJava = new JButton(messages.getString("download.java"));
        btnDownloadJava.setToolTipText(messages.getString("download.java.tooltip"));
        btnDownloadJava.setFont(font14);
        btnDownloadJava.setForeground(Color.GRAY);
        btnDownloadJava.setEnabled(false);

        btnDownloadJava.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleDownloadJava();
            }
        });      

        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.25;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(btnDownloadJava, constraints);

        btnDownloadXMage = new JButton(messages.getString("download.xmage"));
        btnDownloadXMage.setToolTipText(messages.getString("download.xmage.tooltip"));
        btnDownloadXMage.setFont(font14);
        btnDownloadXMage.setForeground(Color.GRAY);
        btnDownloadXMage.setEnabled(false);

        btnDownloadXMage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleDownloadXMage();
            }
        });      

        constraints.gridx = 2;
        mainPanel.add(btnDownloadXMage, constraints);

        btnTorrentXMage = new JButton(messages.getString("torrent.xmage"));
        btnTorrentXMage.setToolTipText(messages.getString("torrent.xmage.tooltip"));
        btnTorrentXMage.setFont(font14);
        btnTorrentXMage.setForeground(Color.GRAY);
        btnTorrentXMage.setEnabled(false);

        btnTorrentXMage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleTorrentXMage();
            }
        });      

        constraints.gridx = 3;
        mainPanel.add(btnTorrentXMage, constraints);

        btnSeedXMage = new JButton(messages.getString("seed.xmage"));
        btnSeedXMage.setToolTipText(messages.getString("seed.xmage.tooltip"));
        btnSeedXMage.setFont(font14);
        btnSeedXMage.setForeground(Color.GRAY);
        btnSeedXMage.setEnabled(false);

        btnSeedXMage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                handleSeedXMage();
            }
        });      

        constraints.gridx = 4;
        mainPanel.add(btnSeedXMage, constraints);
        
        textArea = new JTextArea(5, 40);
        textArea.setEditable(false);
        textArea.setForeground(Color.WHITE);
        textArea.setBackground(Color.BLACK);
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        scrollPane = new JScrollPane (textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane, constraints);
        
        labelProgress = new JLabel(messages.getString("progress"));
        labelProgress.setFont(font12);
        labelProgress.setForeground(Color.WHITE);
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        mainPanel.add(labelProgress, constraints);

        progressBar = new JProgressBar(0, 100);
        constraints.gridx = 2;
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(progressBar, constraints);

        frame.add(mainPanel);
        frame.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width/2 - frame.getSize().width/2, dim.height/2 - frame.getSize().height/2);
    }
    
    private void createToolbar() {
        
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        Border emptyBorder = BorderFactory.createEmptyBorder();

        JButton toolbarButton = new JButton("Settings");
        toolbarButton.setBorder(emptyBorder);
        toolbarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                SettingsDialog settings = new SettingsDialog();
                settings.setVisible(true);
            }
        });
        toolBar.add(toolbarButton);
        toolBar.addSeparator();

        toolbarButton = new JButton("About");
        toolbarButton.setBorder(emptyBorder);
        toolbarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                AboutDialog about = new AboutDialog();
                about.setVisible(true);
            }
        });
        toolBar.add(toolbarButton);
        toolBar.addSeparator();

        toolbarButton = new JButton("Forum");
        toolbarButton.setBorder(emptyBorder);
        toolbarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                openWebpage("http://www.slightlymagic.net/forum/viewforum.php?f=70");
            }
        });
        toolBar.add(toolbarButton);
        toolBar.addSeparator();

        toolbarButton = new JButton("Website");
        toolbarButton.setBorder(emptyBorder);
        toolbarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                openWebpage("http://xmage.info");
            }
        });
        toolBar.add(toolbarButton);
                
        frame.add(toolBar, BorderLayout.PAGE_START);
       
    }
    
    private static void openWebpage(String uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(uri));
            } catch (URISyntaxException ex) {
                logger.error("Error: ", ex);
            } catch (IOException ex) {
                logger.error("Error: ", ex);
            }
        }
    }
    
    private void handleClient() {
        Process p = Utilities.launchClientProcess(textArea);
        clientConsole.setVisible(true);
        clientConsole.start(p);
    }
    
    private void handleServer() {
        if (serverProcess == null) {
            serverProcess = Utilities.launchServerProcess(textArea);
            serverConsole.setVisible(true);
            serverConsole.start(serverProcess);
            btnLaunchServer.setText(messages.getString("stopServer"));
            btnLaunchClientServer.setEnabled(false);
        }
        else {
            Utilities.stopProcess(serverProcess);
            serverProcess = null;
            btnLaunchServer.setText(messages.getString("launchServer"));
            btnLaunchClientServer.setEnabled(true);
        }
    }

    private void handleDownloadJava() {
        disableButtons();
        DownloadJavaTask java = new DownloadJavaTask(progressBar);
        java.execute();
//        try {
//            TorrentClient.download(new File(path.getAbsolutePath() + File.separator + "1AFA61A32DB8B1F8DCCF9346F3B0016C4F2689AE.torrent"), new File(path.getAbsolutePath()), progressBar);
//        } catch (IOException ex) {
//            logger.error("Error: ", ex);
//        }
    }
    
    private void handleDownloadXMage() {
        disableButtons();
        DownloadXMageTask xmage = new DownloadXMageTask(progressBar, false);
        xmage.execute();
    }

    private void handleTorrentXMage() {
        disableButtons();
        DownloadXMageTask xmage = new DownloadXMageTask(progressBar, true);
        xmage.execute();
    }

    private void handleSeedXMage() {
        
    }
    
    private void localize() {
        UIManager.put("OptionPane.yesButtonText", messages.getString("yes"));
        UIManager.put("OptionPane.noButtonText", messages.getString("no"));
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            XMageLauncher gui = new XMageLauncher();
            SwingUtilities.invokeLater(gui);
        } catch (ClassNotFoundException ex) {
            logger.error("Error: ", ex);
        } catch (InstantiationException ex) {
            logger.error("Error: ", ex);
        } catch (IllegalAccessException ex) {
            logger.error("Error: ", ex);
        } catch (UnsupportedLookAndFeelException ex) {
            logger.error("Error: ", ex);
        }
    }
    
    @Override
    public void run() {
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (serverProcess != null) {
                    int response = JOptionPane.showConfirmDialog(frame, messages.getString("serverRunning.message"), messages.getString("serverRunning.title"), JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        Utilities.stopProcess(serverProcess);
                    }
                }
                Config.saveProperties();
            }
        });
        
        final String xmageConfig = Config.getXMageHome() + "/config.json";
        try {
            final URL xmageUrl = new URL(xmageConfig);
            textArea.append(messages.getString("readingConfig") + xmageUrl.toString() + "\n");
        
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        config = Utilities.readJsonFromUrl(xmageUrl);
                    } catch (IOException ex) {
                        logger.error("Error reading config from " + xmageConfig, ex);
                        textArea.append(messages.getString("readingConfig.error") + xmageConfig + "\n" + messages.getString("readingConfig.error.causes") + "\n");
                        enableButtons();
                        return;
                    } catch (JSONException ex) {
                        logger.error("Invalid config from " + xmageConfig, ex);
                        textArea.append(messages.getString("invalidConfig") + xmageConfig + "\n");
                        enableButtons();
                        return;
                    }
                    path = Utilities.getInstallPath();
                    textArea.append(messages.getString("folder") + path.getAbsolutePath() + "\n");
                    checkJava();
                    checkXMage();
                    enableButtons();
//                    DownloadXMageTask xmage = new DownloadXMageTask(progressBar, false);            
//                    DownloadJavaTask java = new DownloadJavaTask(xmage, progressBar);
                    DownloadLauncherTask launcher = new DownloadLauncherTask(progressBar);
                    launcher.execute();
                }
            });
        } catch (MalformedURLException ex) {
            logger.error("Error reading config from " + xmageConfig, ex);
            textArea.append(messages.getString("readingConfig.error") + xmageConfig + "\n" + messages.getString("readingConfig.error.causes") + "\n");
            enableButtons();
        }

    }

    private void checkJava() {
        try {
            String javaAvailableVersion = (String)config.getJSONObject("java").get(("version"));
            String javaInstalledVersion = Config.getInstalledJavaVersion();
            textArea.append(messages.getString("java.installed") + javaInstalledVersion + "\n");
            textArea.append(messages.getString("java.available") + javaAvailableVersion + "\n");
            if (!javaAvailableVersion.equals(javaInstalledVersion)) {
                String javaMessage = "";
                String javaTitle = "";
                if (javaInstalledVersion.isEmpty()) {
                    textArea.append(messages.getString("java.none") + "\n");
                    javaMessage = messages.getString("java.none.message");
                    javaTitle = messages.getString("java.none");
                }
                else {
                    textArea.append(messages.getString("java.new") + "\n");
                    javaMessage = messages.getString("java.new.message");
                    javaTitle = messages.getString("java.new");
                }
                JOptionPane.showMessageDialog(frame, javaMessage, javaTitle, JOptionPane.INFORMATION_MESSAGE);
                this.btnDownloadJava.setEnabled(true);
                this.btnDownloadJava.setForeground(Color.BLACK);
            }
        }
        catch (JSONException ex) {
            logger.error("Error: ", ex);
        }
    }
    
    private void checkXMage() {
        try {
            String xmageAvailableVersion = (String)config.getJSONObject("XMage").get(("version"));
            String xmageInstalledVersion = Config.getInstalledXMageVersion();
            textArea.append(messages.getString("xmage.installed") + xmageInstalledVersion + "\n");
            textArea.append(messages.getString("xmage.available") + xmageAvailableVersion + "\n");                
            if (!xmageAvailableVersion.equals(xmageInstalledVersion)) {
                String xmageMessage = "";
                String xmageTitle = "";
                if (xmageInstalledVersion.isEmpty()) {
                    textArea.append(messages.getString("xmage.none") + "\n");
                    xmageMessage = messages.getString("xmage.none.message");
                    xmageTitle = messages.getString("xmage.none");
                }
                else {
                    textArea.append(messages.getString("xmage.new") + "\n");
                    xmageMessage = messages.getString("xmage.new.message");
                    xmageTitle = messages.getString("xmage.new");
                }
                JOptionPane.showMessageDialog(frame, xmageMessage, xmageTitle, JOptionPane.INFORMATION_MESSAGE);
                this.btnDownloadXMage.setEnabled(true);
                this.btnDownloadXMage.setForeground(Color.BLACK);
            }
        }
        catch (JSONException ex) {
            logger.error("Error: ", ex);
        }
        
    }
    
    private void enableButtons() {
        String javaInstalledVersion = Config.getInstalledJavaVersion();
        if (!javaInstalledVersion.isEmpty()) {
            String xmageInstalledVersion = Config.getInstalledXMageVersion();
            if (!xmageInstalledVersion.isEmpty()) {
                btnLaunchClient.setEnabled(true);
                btnLaunchClient.setForeground(Color.BLACK);
                btnLaunchClientServer.setEnabled(true);
                btnLaunchClientServer.setForeground(Color.BLACK);
                btnLaunchServer.setEnabled(true);
                btnLaunchServer.setForeground(Color.BLACK);
            }
            else {
                textArea.append(messages.getString("noXMage"));
            }
        }
        else {
            textArea.append(messages.getString("noJava"));
        }
    }

    private void disableButtons() {
        btnLaunchClient.setEnabled(false);
        btnLaunchClient.setForeground(Color.GRAY);
        btnLaunchClientServer.setEnabled(false);
        btnLaunchClientServer.setForeground(Color.GRAY);
        btnLaunchServer.setEnabled(false);
        btnLaunchServer.setForeground(Color.GRAY);
        btnDownloadJava.setEnabled(false);
        btnDownloadJava.setForeground(Color.GRAY);
        btnDownloadXMage.setEnabled(false);
        btnDownloadXMage.setForeground(Color.GRAY);
        btnTorrentXMage.setEnabled(false);
        btnTorrentXMage.setForeground(Color.GRAY);
        btnSeedXMage.setEnabled(false);
        btnSeedXMage.setForeground(Color.GRAY);
    }
    
    private class DownloadLauncherTask extends DownloadTask {
                
        public DownloadLauncherTask(JProgressBar progressBar) {
            super(progressBar);
        }

        @Override
        protected Void doInBackground() {
            try {
                File launcherFolder = new File(path.getAbsolutePath());
                String launcherAvailableVersion = (String)config.getJSONObject("XMage").getJSONObject("Launcher").get(("version"));
                String launcherInstalledVersion = Config.getVersion();
                textArea.append(messages.getString("xmage.launcher.installed") + launcherInstalledVersion + "\n");
                textArea.append(messages.getString("xmage.launcher.available") + launcherAvailableVersion + "\n");
                removeOldLauncherFiles(launcherFolder, launcherInstalledVersion);
                if (!launcherAvailableVersion.equals(launcherInstalledVersion)) {
                    String launcherMessage = "";
                    String launcherTitle = "";
                    textArea.append(messages.getString("xmage.launcher.new") + "\n");
                    launcherMessage = messages.getString("xmage.launcher.new.message");
                    launcherTitle = messages.getString("xmage.launcher.new");
                    int response = JOptionPane.showConfirmDialog(frame, "<html>" + launcherMessage + messages.getString("installNow") + "</html>", launcherTitle, JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        String launcherRemoteLocation = (String)config.getJSONObject("XMage").getJSONObject("Launcher").get(("location"));
                        URL launcher = new URL(launcherRemoteLocation);
                        textArea.append(messages.getString("xmage.launcher.downloading") + launcher.toString() + "\n");

                        download(launcher, path.getAbsolutePath(), "");

                        File from = new File(path.getAbsolutePath() + File.separator + "xmage.dl");
                        textArea.append(messages.getString("xmage.launcher.installing"));
                        File to = new File(launcherFolder, "XMageLauncher-" + launcherAvailableVersion + ".jar");
                        from.renameTo(to);
                        textArea.append(messages.getString("done") + "\n");
                        progressBar.setValue(0);
                        JOptionPane.showMessageDialog(frame, "<html>" + messages.getString("restartMessage") + "</html>", messages.getString("restartTitle"), JOptionPane.WARNING_MESSAGE);
                        Utilities.restart(to);
                    }
                }
            }
            catch (IOException ex) {
                progressBar.setValue(0);
                this.cancel(true);
                logger.error("Error: ", ex);
            }
            catch (JSONException ex) {
                progressBar.setValue(0);
                this.cancel(true);
                logger.error("Error: ", ex);
            }
            return null;
        }
        
        private void removeOldLauncherFiles(File xmageFolder, final String launcherVersion) {
            File[] files = xmageFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    if (name.matches("XMageLauncher.*\\.jar")) {
                        if (name.equals("XMageLauncher-" + launcherVersion + ".jar")) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }
            } );
            if (files.length > 0) {
                textArea.append(messages.getString("removing") + "\n");
                for (final File file : files) {
                    if (!file.isDirectory() && !file.delete()) {
                        logger.error("Can't remove " + file.getAbsolutePath());
                   }
                }
            }
        }

        @Override
        public void done() {
            enableButtons();
        }

    }

    private class DownloadJavaTask extends DownloadTask {
                
        public DownloadJavaTask(JProgressBar progressBar) {
            super(progressBar);
        }

        @Override
        protected Void doInBackground() {
            try {
                File javaFolder = new File(path.getAbsolutePath() + File.separator + "java");
                String javaAvailableVersion = (String)config.getJSONObject("java").get(("version"));
                if (javaFolder.isDirectory()) {  //remove existing install
                    textArea.append(messages.getString("removing") + "\n");
                    removeJavaFiles(javaFolder);
                }
                javaFolder.mkdirs();
                String javaRemoteLocation = (String)config.getJSONObject("java").get(("location"));
                URL java = new URL(javaRemoteLocation + Utilities.getOSandArch() + ".tar.gz");
                textArea.append(messages.getString("java.downloading") + java.toString() + "\n");

                download(java, path.getAbsolutePath(), "oraclelicense=accept-securebackup-cookie");

                File from = new File(path.getAbsolutePath() + File.separator + "xmage.dl");
                textArea.append(messages.getString("java.installing") + "\n");

                extract(from, javaFolder);
                textArea.append(messages.getString("done") + "\n");
                progressBar.setValue(0);
                if (!from.delete()) {
                    textArea.append(messages.getString("error.cleanup") + "\n");
                    logger.error("Error: could not cleanup temporary files");
                }
                Config.setInstalledJavaVersion(javaAvailableVersion);
                Config.saveProperties();
            }
            catch (IOException ex) {
                progressBar.setValue(0);
                this.cancel(true);
                logger.error("Error: ", ex);
            }
            catch (JSONException ex) {
                progressBar.setValue(0);
                this.cancel(true);
                logger.error("Error: ", ex);
            } 
            return null;
        }

        private void removeJavaFiles(File javaFolder) {
            File[] files = javaFolder.listFiles();
            for (final File file : files) {
                if (file.isDirectory()) {
                    removeJavaFiles(file);
                }
                if (!file.delete()) {
                    logger.error("Can't remove " + file.getAbsolutePath());
                }
            }
        }
        
        @Override
        public void done() {
            enableButtons();
        }
    }
    
    private class DownloadXMageTask extends DownloadTask {
        
        private final boolean useTorrent;
        
        public DownloadXMageTask(JProgressBar progressBar, boolean useTorrent) {
            super(progressBar);
            this.useTorrent = useTorrent;
        }

        @Override
        protected Void doInBackground() {
            try {
                File xmageFolder = new File(path.getAbsolutePath() + File.separator + "xmage");
                String xmageAvailableVersion = (String)config.getJSONObject("XMage").get(("version"));
                if (xmageFolder.isDirectory()) {  //remove existing install
                    textArea.append(messages.getString("removing") + "\n");
                    removeXMageFiles(xmageFolder);
                }
                xmageFolder.mkdirs();
                String xmageRemoteLocation;
                if (useTorrent) {
                    xmageRemoteLocation = (String)config.getJSONObject("XMage").get(("torrent"));
                }
                else {
                    xmageRemoteLocation = (String)config.getJSONObject("XMage").get(("location"));
                    String[] otherLocations = (String[])config.getJSONObject("XMage").get(("other_locations"));
                }
                URL xmage = new URL(xmageRemoteLocation);
                textArea.append(messages.getString("xmage.downloading") + xmage.toString() + "\n");

                download(xmage, path.getAbsolutePath(), "");
                File from = new File(path.getAbsolutePath() + File.separator + "xmage.dl");
                if (useTorrent) {
                    torrent(from, xmageFolder);
                }

                textArea.append(messages.getString("xmage.installing"));

                unzip(from, xmageFolder);
                textArea.append(messages.getString("done") + "\n");
                progressBar.setValue(0);
                if (!from.delete()) {
                    textArea.append(messages.getString("error.cleanup") + "\n");
                    logger.error("Error: could not cleanup temporary files");
                }
                Config.setInstalledXMageVersion(xmageAvailableVersion);
                Config.saveProperties();
            }
            catch (IOException ex) {
                progressBar.setValue(0);
                this.cancel(true);
                logger.error("Error: ", ex);
            }
            catch (JSONException ex) {
                progressBar.setValue(0);
                this.cancel(true);
                logger.error("Error: ", ex);
            } 
            return null;
        }
        
        private void removeXMageFiles(File xmageFolder) {
            // keep images folder -- no need to make users download these again
            File[] files = xmageFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return !name.matches("images|gameLogs|backgrounds|mageclient\\.log|mageserver\\.log|.*\\.dck");
                }
            } );
            for (final File file : files) {
                if (file.isDirectory()) {
                    removeXMageFiles(file);
                }
                else if (!file.delete()) {
                    logger.error("Can't remove " + file.getAbsolutePath());
               }
            }
        }
        
        @Override
        public void done() {
            enableButtons();
        }

    }

}
