import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CalculatorClient - Obsidian Edition
 * Ported from web/index.html
 */
public class CalculatorClient extends JFrame {

    // --- Theme Constants (from CSS) ---
    private static final Color BG_DARK = new Color(15, 15, 19);
    private static final Color BG_PANEL = new Color(255, 255, 255, 13); // 0.05 opacity
    private static Color ACCENT = new Color(124, 58, 237); // Violet Default
    private static final Color TEXT_MAIN = new Color(243, 244, 246);
    private static final Color TEXT_MUTED = new Color(156, 163, 175);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 25);
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);

    // --- Components ---
    private CardLayout contentLayout;
    private JPanel contentPanel;

    // Dashboard Components
    private CustomTextField num1Field, num2Field;
    private String currentOp = "ADD";
    private List<OpButton> opButtons = new ArrayList<>();
    private JLabel resultValueLabel;
    private ActionButton actionBtn;

    // History Components
    private JPanel historyListPanel;

    // Sidebar Components
    private StatusDot statusDot;
    private JLabel statusText;

    // Networking
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public CalculatorClient() {
        super("Obsidian | Network Core");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Main Container (Glass Effect)
        MainContainer mainContainer = new MainContainer();
        mainContainer.setLayout(null);
        setContentPane(mainContainer);

        // Close Button (Custom)
        setupWindowControls(mainContainer);

        // Split Layout: Sidebar (Left) + Content (Right)
        int sidebarWidth = 260;

        // --- Sidebar ---
        JPanel sidebar = new JPanel(null);
        sidebar.setBounds(0, 0, sidebarWidth, 650);
        sidebar.setOpaque(false);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        // Logo
        JLabel logo = new JLabel("OBSIDIAN");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logo.setForeground(Color.WHITE);
        logo.setIcon(new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT, 24, 24, new Color(59, 130, 246));
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, 24, 24, 6, 6);
            }

            public int getIconWidth() {
                return 24;
            }

            public int getIconHeight() {
                return 24;
            }
        });
        logo.setIconTextGap(12);
        logo.setBounds(30, 30, 200, 30);
        sidebar.add(logo);

        // Navigation
        addNavItem(sidebar, "Dashboard", "◆", 100, true);
        addNavItem(sidebar, "History Logs", "hz", 150, false);
        addNavItem(sidebar, "Settings", "⚙", 200, false);

        // System Status
        JPanel statusPanel = new JPanel(null);
        statusPanel.setBounds(20, 580, 220, 50);
        statusPanel.setBackground(new Color(0, 0, 0, 50));
        statusPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JLabel statusTitle = new JLabel("SYSTEM STATUS");
        statusTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusTitle.setForeground(new Color(102, 102, 102));
        statusTitle.setBounds(15, 8, 100, 15);
        statusPanel.add(statusTitle);

        statusDot = new StatusDot();
        statusDot.setBounds(15, 28, 8, 8);
        statusPanel.add(statusDot);

        statusText = new JLabel("Offline");
        statusText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusText.setForeground(TEXT_MUTED);
        statusText.setBounds(30, 24, 100, 15);
        statusPanel.add(statusText);

        sidebar.add(statusPanel);
        mainContainer.add(sidebar);

        // --- Content Area ---
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setOpaque(false);
        contentPanel.setBounds(sidebarWidth, 0, 1000 - sidebarWidth, 650);

        // Views
        contentPanel.add(createDashboardPanel(), "Dashboard");
        contentPanel.add(createHistoryPanel(), "History Logs");
        contentPanel.add(createSettingsPanel(), "Settings");

        mainContainer.add(contentPanel);

        // Drag functionality
        makeDraggable(mainContainer);

        // Start connection
        new Thread(this::connectToServer).start();
    }

    private void setupWindowControls(JPanel panel) {
        JButton closeBtn = new JButton("×");
        closeBtn.setFont(new Font("Arial", Font.PLAIN, 24));
        closeBtn.setForeground(TEXT_MUTED);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setBounds(950, 10, 40, 40);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));
        panel.add(closeBtn);
    }

    private void addNavItem(JPanel sidebar, String name, String icon, int y, boolean isActive) {
        NavButton btn = new NavButton(name, icon);
        btn.setBounds(20, y, 220, 45);
        btn.addActionListener(e -> {
            // Reset all nav buttons
            for (Component c : sidebar.getComponents()) {
                if (c instanceof NavButton)
                    ((NavButton) c).setActive(false);
            }
            btn.setActive(true);
            contentLayout.show(contentPanel, name);
        });
        if (isActive)
            btn.setActive(true);
        sidebar.add(btn);
    }

    // --- View Generators ---

    private JPanel createDashboardPanel() {
        JPanel p = new JPanel(null);
        p.setOpaque(false);

        // Header
        addHeader(p, "Execution Core", "Advanced Mathematical Computation Operations");

        // Calculator Container
        int startY = 150;

        // Var Alpha
        JLabel l1 = new JLabel("VARIABLE ALPHA");
        l1.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l1.setForeground(TEXT_MUTED);
        l1.setBounds(60, startY, 150, 20);
        p.add(l1);

        num1Field = new CustomTextField();
        num1Field.setBounds(60, startY + 25, 200, 80);
        p.add(num1Field);

        // Operators
        String[] syms = { "+", "-", "×", "÷" };
        String[] codes = { "ADD", "SUB", "MUL", "DIV" };
        int opX = 290;
        for (int i = 0; i < 4; i++) {
            OpButton op = new OpButton(syms[i], codes[i]);
            op.setBounds(opX + (i % 2) * 60, startY + 25 + (i / 2) * 60, 50, 50);
            if (i == 0)
                op.setSelected(true);
            opButtons.add(op);
            p.add(op);
        }

        // Var Beta
        JLabel l2 = new JLabel("VARIABLE BETA");
        l2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l2.setForeground(TEXT_MUTED);
        l2.setBounds(440, startY, 150, 20);
        p.add(l2);

        num2Field = new CustomTextField();
        num2Field.setBounds(440, startY + 25, 200, 80);
        p.add(num2Field);

        // Action Button
        actionBtn = new ActionButton("RUN SEQUENCE");
        actionBtn.setBounds(60, 300, 580, 70);
        actionBtn.addActionListener(e -> performCalculation());
        p.add(actionBtn);

        // Result Box
        JPanel resBox = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        resBox.setBounds(60, 400, 580, 150);
        resBox.setOpaque(false);

        JLabel resTitle = new JLabel("OUTPUT");
        resTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resTitle.setForeground(TEXT_MUTED);
        resTitle.setBounds(260, 20, 100, 20);
        resBox.add(resTitle);

        resultValueLabel = new JLabel("--");
        resultValueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 64));
        resultValueLabel.setForeground(Color.WHITE);
        resultValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultValueLabel.setBounds(0, 50, 580, 80);
        resBox.add(resultValueLabel);

        p.add(resBox);

        return p;
    }

    private JPanel createHistoryPanel() {
        JPanel p = new JPanel(null);
        p.setOpaque(false);
        addHeader(p, "Audit Logs", "Recent computational sequences.");

        historyListPanel = new JPanel();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        historyListPanel.setOpaque(false);
        historyListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(historyListPanel);
        scroll.setBounds(60, 150, 620, 450);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());

        p.add(scroll);
        return p;
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(null);
        p.setOpaque(false);
        addHeader(p, "Interface Config", "Customize your visual workspace.");

        JPanel configBox = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
        };
        configBox.setBounds(60, 150, 600, 150);
        configBox.setOpaque(false);

        JLabel l = new JLabel("ACCENT COLOR");
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(TEXT_MAIN);
        l.setBounds(30, 20, 200, 30);
        configBox.add(l);

        Color[] colors = { new Color(124, 58, 237), new Color(59, 130, 246), new Color(16, 185, 129),
                new Color(236, 72, 153), new Color(245, 158, 11) };
        int cx = 30;
        for (Color c : colors) {
            ColorButton cb = new ColorButton(c);
            cb.setBounds(cx, 60, 40, 40);
            cb.addActionListener(e -> {
                ACCENT = c;
                repaint(); // Repaint entire frame to update accent references
            });
            configBox.add(cb);
            cx += 55;
        }

        p.add(configBox);
        return p;
    }

    private void addHeader(JPanel p, String title, String subtitle) {
        JLabel h1 = new JLabel(title);
        h1.setFont(new Font("Segoe UI", Font.BOLD, 36));
        h1.setForeground(Color.WHITE);
        h1.setBounds(60, 40, 400, 50);
        p.add(h1);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(TEXT_MUTED);
        sub.setBounds(60, 90, 400, 20);
        p.add(sub);
    }

    // --- Logic ---

    private void connectToServer() {
        try {
            statusText.setText("Connecting...");
            statusDot.setColor(new Color(245, 158, 11)); // Orange

            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            SwingUtilities.invokeLater(() -> {
                statusText.setText("Online");
                statusText.setForeground(SUCCESS_COLOR);
                statusDot.setColor(SUCCESS_COLOR);
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                statusText.setText("Offline");
                statusText.setForeground(DANGER_COLOR);
                statusDot.setColor(DANGER_COLOR);
            });
        }
    }

    private void performCalculation() {
        if (socket == null || socket.isClosed()) {
            resultValueLabel.setText("Offline");
            return;
        }

        try {
            double n1 = Double.parseDouble(num1Field.getText());
            double n2 = Double.parseDouble(num2Field.getText());

            out.println(currentOp + "|" + n1 + "|" + n2);
            actionBtn.setLoading(true);

            new Thread(() -> {
                try {
                    String res = in.readLine();
                    if (res != null) {
                        String[] parts = res.split("\\|");
                        SwingUtilities.invokeLater(() -> {
                            actionBtn.setLoading(false);
                            if ("OK".equals(parts[0])) {
                                String val = parts[1];
                                resultValueLabel.setText(val);
                                addToHistory(n1, n2, getOpSymbol(currentOp), val);
                            } else {
                                resultValueLabel.setText("Err");
                            }
                        });
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        actionBtn.setLoading(false);
                        resultValueLabel.setText("Net Err");
                    });
                }
            }).start();
        } catch (NumberFormatException e) {
            resultValueLabel.setText("NaN");
        }
    }

    private String getOpSymbol(String code) {
        switch (code) {
            case "ADD":
                return "+";
            case "SUB":
                return "-";
            case "MUL":
                return "×";
            case "DIV":
                return "÷";
            default:
                return "?";
        }
    }

    private void addToHistory(double n1, double n2, String sym, String res) {
        HistoryItem item = new HistoryItem(n1, n2, sym, res);
        historyListPanel.add(item, 0); // Add to top
        historyListPanel.revalidate();
        historyListPanel.repaint();
    }

    // --- Custom UI Classes ---

    class MainContainer extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(BG_DARK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Background Gradients
            RadialGradientPaint p1 = new RadialGradientPaint(new Point2D.Float(0, 0), 400, new float[] { 0f, 1f },
                    new Color[] { new Color(124, 58, 237, 30), new Color(0, 0, 0, 0) });
            g2.setPaint(p1);
            g2.fillOval(-100, -100, 600, 600);

            RadialGradientPaint p2 = new RadialGradientPaint(new Point2D.Float(getWidth(), getHeight()), 400,
                    new float[] { 0f, 1f },
                    new Color[] { new Color(59, 130, 246, 30), new Color(0, 0, 0, 0) });
            g2.setPaint(p2);
            g2.fillOval(getWidth() - 500, getHeight() - 500, 600, 600);

            // Glass Overlay
            g2.setColor(new Color(20, 20, 25, 150));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

            // Border
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        }
    }

    class NavButton extends JButton {
        private boolean isActive = false;
        private String icon;

        public NavButton(String text, String icon) {
            super(text);
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(TEXT_MUTED);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void setActive(boolean b) {
            this.isActive = b;
            setForeground(b ? Color.WHITE : TEXT_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isActive) {
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 80));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }

            // Icon
            g2.setColor(isActive ? Color.WHITE : TEXT_MUTED);
            // Simple placeholder for icon drawing or just text
            // In a real app we'd load images or use a font icon properly
            // super.paintComponent(g) handles the text drawing
            super.paintComponent(g);
        }
    }

    class CustomTextField extends JTextField {
        public CustomTextField() {
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 32));
            setHorizontalAlignment(JTextField.CENTER);
            setBorder(BorderFactory.createEmptyBorder());
            setCaretColor(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(BG_PANEL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            if (isFocusOwner()) {
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            } else {
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
            super.paintComponent(g);
        }
    }

    class OpButton extends JButton {
        private String code;
        private boolean isSelected = false;

        public OpButton(String text, String code) {
            super(text);
            this.code = code;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 20));
            setForeground(TEXT_MUTED);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addActionListener(e -> {
                for (OpButton b : opButtons)
                    b.setSelected(false);
                setSelected(true);
                currentOp = code;
            });
        }

        public void setSelected(boolean b) {
            this.isSelected = b;
            setForeground(b ? Color.WHITE : TEXT_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected) {
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else {
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            super.paintComponent(g);
        }
    }

    class ActionButton extends JButton {
        private boolean loading = false;

        public ActionButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(TEXT_MAIN);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void setLoading(boolean b) {
            this.loading = b;
            setText(b ? "PROCESSING..." : "RUN SEQUENCE");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Gradient Background
            GradientPaint gp = new GradientPaint(0, 0, BG_PANEL, getWidth(), getHeight(), new Color(255, 255, 255, 5));
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            if (getModel().isRollover() || loading) {
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            } else {
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
            super.paintComponent(g);
        }
    }

    class HistoryItem extends JPanel {
        public HistoryItem(double n1, double n2, String sym, String res) {
            setOpaque(false);
            setPreferredSize(new Dimension(600, 80));
            setMaximumSize(new Dimension(600, 80));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 0, 15, 0)); // Margin bottom

            JPanel bg = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BG_PANEL);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(BORDER_COLOR); // Left accent border simulating border-left
                    g2.fillRoundRect(0, 0, 4, getHeight(), 2, 2);
                }
            };
            bg.setOpaque(false);
            bg.setBorder(new EmptyBorder(10, 20, 10, 20));

            JPanel left = new JPanel(new GridLayout(2, 1));
            left.setOpaque(false);
            JLabel lblOp = new JLabel("OPERATION");
            lblOp.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblOp.setForeground(TEXT_MUTED);
            JLabel lblEq = new JLabel(n1 + " " + sym + " " + n2);
            lblEq.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblEq.setForeground(TEXT_MAIN);
            left.add(lblOp);
            left.add(lblEq);

            JLabel right = new JLabel(res);
            right.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            right.setForeground(TEXT_MAIN);

            bg.add(left, BorderLayout.WEST);
            bg.add(right, BorderLayout.EAST);

            add(bg, BorderLayout.CENTER);
        }
    }

    class ColorButton extends JButton {
        Color c;

        public ColorButton(Color c) {
            this.c = c;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c);
            g2.fillOval(4, 4, 32, 32);
        }
    }

    class StatusDot extends JPanel {
        Color c = Color.GRAY;

        public StatusDot() {
            setOpaque(false);
        }

        public void setColor(Color c) {
            this.c = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c);
            g2.fillOval(0, 0, getWidth(), getHeight());
        }
    }

    class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(255, 255, 255, 30);
            this.trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
    }

    // --- Window Drag ---
    private void makeDraggable(Component c) {
        MouseAdapter ma = new MouseAdapter() {
            int lastX, lastY;

            public void mousePressed(MouseEvent e) {
                lastX = e.getXOnScreen();
                lastY = e.getYOnScreen();
            }

            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen(), y = e.getYOnScreen();
                setLocation(getLocation().x + x - lastX, getLocation().y + y - lastY);
                lastX = x;
                lastY = y;
            }
        };
        c.addMouseListener(ma);
        c.addMouseMotionListener(ma);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorClient().setVisible(true));
    }
}
