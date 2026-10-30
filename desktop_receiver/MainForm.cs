using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    public enum AppTab
    {
        Connect,
        Settings,
        Logs
    }

    public class MainForm : Form
    {
        private readonly UdpReceiverEngine _engine;
        private readonly BluetoothReceiverEngine _btEngine;
        private readonly bool _startMinimized;
        private NotifyIcon _trayIcon = null!;
        private ContextMenuStrip _trayMenu = null!;

        // Tab Navigation
        private AppTab _currentTab = AppTab.Connect;
        private Button _tabBtnConnect = null!;
        private Button _tabBtnSettings = null!;
        private Button _tabBtnLogs = null!;
        private Panel _pageConnect = null!;
        private Panel _pageSettings = null!;
        private Panel _pageLogs = null!;

        // Dynamic State UI Elements - Header
        private Label _lblStatusBadge = null!;

        // Dynamic State UI Elements - Connect Tab
        private Panel _pnlHeroStatus = null!;
        private Label _lblHeroTitle = null!;
        private Label _lblHeroDesc = null!;
        private Button _btnDisconnectPhone = null!;
        private Button _btnToggleServer = null!;
        private Label _lblIpDisplay = null!;
        private Button _btnCopyIp = null!;

        // Dynamic State UI Elements - Settings Tab
        private CheckBox _chkAutoStart = null!;
        private Button _btnMakeShortcuts = null!;
        private Button _btnFirewallFix = null!;
        private Button _btnMinimize = null!;

        // Dynamic State UI Elements - Logs Tab
        private Label _lblStatsStatus = null!;
        private Label _lblStatsDevice = null!;
        private Label _lblStatsPackets = null!;
        private ListBox _lstLogs = null!;
        private Button _btnClearLogs = null!;
        private Button _btnCopyLogs = null!;

        private System.Windows.Forms.Timer _refreshTimer = null!;

        // Modern Fluent Palette
        private readonly Color BgColor = Color.FromArgb(15, 17, 23);
        private readonly Color CardBg = Color.FromArgb(24, 27, 36);
        private readonly Color CardBorder = Color.FromArgb(42, 47, 62);
        private readonly Color NavBg = Color.FromArgb(20, 23, 31);
        private readonly Color NavActiveBg = Color.FromArgb(32, 38, 52);
        private readonly Color TextPrimary = Color.FromArgb(245, 247, 250);
        private readonly Color TextSecondary = Color.FromArgb(155, 165, 180);
        private readonly Color TextMuted = Color.FromArgb(105, 115, 135);
        private readonly Color AccentCyan = Color.FromArgb(6, 182, 212);
        private readonly Color AccentGreen = Color.FromArgb(34, 197, 94);
        private readonly Color AccentRed = Color.FromArgb(239, 68, 68);

        public MainForm(bool startMinimized = false)
        {
            _startMinimized = startMinimized;
            _engine = new UdpReceiverEngine(5055);
            _btEngine = new BluetoothReceiverEngine();

            LoadAppIcon();
            InitializeComponent();
            SetupTrayMenu();

            _engine.OnLog += msg => AppendLogSafe($"[SYS] {msg}");
            _engine.OnPacketProcessed += (cmd, detail) =>
            {
                if (cmd != "MOVE") AppendLogSafe($"[{cmd}] {detail}");
            };

            _btEngine.OnLog += msg => AppendLogSafe($"[BT] {msg}");
            _btEngine.OnCommandReceived += (cmd, client) =>
            {
                _engine.ProcessExternalCommand(cmd, $"Bluetooth: {client}");
            };
            _btEngine.OnConnectionStateChanged += connected =>
            {
                UpdateUiFromState();
            };

            this.Load += (s, e) =>
            {
                _engine.Start();
                _ = _btEngine.StartAsync();
                UpdateUiFromState();
                if (_startMinimized)
                {
                    this.Hide();
                    this.ShowInTaskbar = false;
                }
            };
        }

        private void LoadAppIcon()
        {
            try
            {
                string localIco = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", "app_icon.ico");
                if (File.Exists(localIco))
                {
                    this.Icon = new Icon(localIco);
                    return;
                }

                using var stream = typeof(MainForm).Assembly.GetManifestResourceStream("AirGlideDesktop.Resources.app_icon.ico");
                if (stream != null)
                {
                    this.Icon = new Icon(stream);
                }
            }
            catch { }
        }

        private void InitializeComponent()
        {
            this.Text = "AirGlide Remote - Laptop Receiver";
            this.Size = new Size(660, 680);
            this.MinimumSize = new Size(580, 560);
            this.BackColor = BgColor;
            this.ForeColor = TextPrimary;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.Font = new Font("Segoe UI", 10f, FontStyle.Regular);
            this.AutoScaleMode = AutoScaleMode.Dpi;

            // Root panel
            Panel root = new Panel
            {
                Dock = DockStyle.Fill,
                BackColor = BgColor,
                Padding = new Padding(22, 14, 22, 16)
            };

            // Top Header & Nav Container
            FlowLayoutPanel topContainer = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = BgColor,
                Margin = new Padding(0)
            };

            topContainer.Controls.Add(BuildHeaderRow());
            topContainer.Controls.Add(BuildNavBar());

            // Tab Pages Container
            Panel contentPanel = new Panel
            {
                Dock = DockStyle.Fill,
                BackColor = BgColor
            };

            contentPanel.Controls.Add(BuildConnectPage());
            contentPanel.Controls.Add(BuildSettingsPage());
            contentPanel.Controls.Add(BuildLogsPage());

            root.Controls.Add(contentPanel);
            root.Controls.Add(topContainer);
            contentPanel.BringToFront();

            this.Controls.Add(root);

            // Select default tab
            SelectTab(AppTab.Connect);

            // Smooth UI timer
            _refreshTimer = new System.Windows.Forms.Timer { Interval = 800 };
            _refreshTimer.Tick += (s, e) => UpdateUiFromState();
            _refreshTimer.Start();
        }

        // ======================================================================
        // Header & Navigation Bar
        // ======================================================================

        private Control BuildHeaderRow()
        {
            TableLayoutPanel pnl = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 1,
                Margin = new Padding(0, 0, 0, 10),
                BackColor = BgColor
            };
            pnl.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 65f));
            pnl.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 35f));

            // Left brand: Logo + Title + Subtitle
            TableLayoutPanel brandLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 1,
                BackColor = BgColor
            };
            brandLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 46f));
            brandLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));

            PictureBox logoBox = new PictureBox
            {
                Size = new Size(38, 38),
                SizeMode = PictureBoxSizeMode.Zoom,
                Margin = new Padding(0, 2, 8, 0)
            };

            try
            {
                string pngPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", "app_icon.png");
                if (File.Exists(pngPath))
                {
                    logoBox.Image = Image.FromFile(pngPath);
                }
                else if (this.Icon != null)
                {
                    logoBox.Image = this.Icon.ToBitmap();
                }
            }
            catch
            {
                Bitmap bmp = new Bitmap(38, 38);
                using (Graphics g = Graphics.FromImage(bmp))
                {
                    g.Clear(Color.FromArgb(14, 165, 233));
                    using var brush = new SolidBrush(Color.White);
                    using var f = new Font("Segoe UI", 16f, FontStyle.Bold);
                    g.DrawString("⚡", f, brush, 4, 2);
                }
                logoBox.Image = bmp;
            }

            FlowLayoutPanel titleCol = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = BgColor
            };

            Label lblTitle = new Label
            {
                Text = "AirGlide Remote",
                Font = new Font("Segoe UI", 13.5f, FontStyle.Bold),
                ForeColor = TextPrimary,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 1)
            };

            Label lblSub = new Label
            {
                Text = "Wireless Mouse & Keyboard for Windows",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0)
            };

            titleCol.Controls.Add(lblTitle);
            titleCol.Controls.Add(lblSub);

            brandLayout.Controls.Add(logoBox, 0, 0);
            brandLayout.Controls.Add(titleCol, 1, 0);

            // Right status pill badge
            _lblStatusBadge = new Label
            {
                Text = "● READY",
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                ForeColor = AccentGreen,
                BackColor = Color.FromArgb(16, 45, 28),
                TextAlign = ContentAlignment.MiddleCenter,
                Size = new Size(160, 30),
                Anchor = AnchorStyles.Right,
                Margin = new Padding(0, 4, 0, 0)
            };

            pnl.Controls.Add(brandLayout, 0, 0);
            pnl.Controls.Add(_lblStatusBadge, 1, 0);

            return pnl;
        }

        private Control BuildNavBar()
        {
            TableLayoutPanel nav = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                Height = 44,
                ColumnCount = 3,
                RowCount = 1,
                BackColor = NavBg,
                Margin = new Padding(0, 0, 0, 14),
                Padding = new Padding(3)
            };
            nav.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 33.33f));
            nav.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 33.33f));
            nav.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 33.34f));

            _tabBtnConnect = CreateTabButton("📱  Connect", AppTab.Connect);
            _tabBtnSettings = CreateTabButton("⚙️  Settings", AppTab.Settings);
            _tabBtnLogs = CreateTabButton("📊  Activity & Logs", AppTab.Logs);

            nav.Controls.Add(_tabBtnConnect, 0, 0);
            nav.Controls.Add(_tabBtnSettings, 1, 0);
            nav.Controls.Add(_tabBtnLogs, 2, 0);

            return nav;
        }

        private Button CreateTabButton(string text, AppTab tab)
        {
            Button btn = new Button
            {
                Text = text,
                Dock = DockStyle.Fill,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                Cursor = Cursors.Hand,
                Margin = new Padding(2)
            };
            btn.FlatAppearance.BorderSize = 0;
            btn.Click += (s, e) => SelectTab(tab);
            return btn;
        }

        private void SelectTab(AppTab tab)
        {
            _currentTab = tab;
            _pageConnect.Visible = (tab == AppTab.Connect);
            _pageSettings.Visible = (tab == AppTab.Settings);
            _pageLogs.Visible = (tab == AppTab.Logs);

            UpdateTabButtonVisual(_tabBtnConnect, tab == AppTab.Connect);
            UpdateTabButtonVisual(_tabBtnSettings, tab == AppTab.Settings);
            UpdateTabButtonVisual(_tabBtnLogs, tab == AppTab.Logs);
        }

        private void UpdateTabButtonVisual(Button btn, bool isActive)
        {
            if (isActive)
            {
                btn.BackColor = NavActiveBg;
                btn.ForeColor = AccentCyan;
            }
            else
            {
                btn.BackColor = Color.Transparent;
                btn.ForeColor = TextSecondary;
            }
        }

        // ======================================================================
        // TAB 1: 📱 CONNECT (Beginner-Friendly Main Screen)
        // ======================================================================

        private Control BuildConnectPage()
        {
            _pageConnect = new Panel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                BackColor = BgColor
            };

            TableLayoutPanel layout = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 1,
                RowCount = 3,
                BackColor = BgColor
            };
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));

            layout.Controls.Add(BuildHeroStatusCard());
            layout.Controls.Add(BuildStepGuideCard());
            layout.Controls.Add(BuildNetworkCard());

            _pageConnect.Controls.Add(layout);
            return _pageConnect;
        }

        private Control BuildHeroStatusCard()
        {
            _pnlHeroStatus = CreateCardContainer();
            _pnlHeroStatus.Margin = new Padding(0, 0, 0, 14);
            _pnlHeroStatus.Padding = new Padding(20, 16, 20, 16);

            FlowLayoutPanel inner = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            _lblHeroTitle = new Label
            {
                Text = "🟢 Ready to Connect",
                Font = new Font("Segoe UI", 14f, FontStyle.Bold),
                ForeColor = AccentGreen,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 6)
            };

            _lblHeroDesc = new Label
            {
                Text = "Open the AirGlide app on your phone. It connects automatically over Wi-Fi.",
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 12)
            };

            FlowLayoutPanel btnRow = new FlowLayoutPanel
            {
                AutoSize = true,
                FlowDirection = FlowDirection.LeftToRight,
                WrapContents = true,
                BackColor = CardBg,
                Margin = new Padding(0)
            };

            _btnDisconnectPhone = new Button
            {
                Text = "⏹ Disconnect Phone",
                AutoSize = true,
                Height = 38,
                Padding = new Padding(16, 0, 16, 0),
                BackColor = Color.FromArgb(60, 20, 20),
                ForeColor = Color.FromArgb(248, 113, 113),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                Cursor = Cursors.Hand,
                Margin = new Padding(0, 0, 10, 0),
                Visible = false
            };
            _btnDisconnectPhone.FlatAppearance.BorderSize = 1;
            _btnDisconnectPhone.FlatAppearance.BorderColor = Color.FromArgb(180, 50, 50);
            _btnDisconnectPhone.Click += (s, e) =>
            {
                _engine.ReleaseController();
                _btEngine.DisconnectClient();
                AppendLogSafe("Phone disconnected by user.");
                UpdateUiFromState();
            };

            _btnToggleServer = new Button
            {
                Text = "⏹ Stop Server / Turn Off",
                AutoSize = true,
                Height = 38,
                Padding = new Padding(16, 0, 16, 0),
                BackColor = Color.FromArgb(50, 18, 18),
                ForeColor = Color.FromArgb(248, 113, 113),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                Cursor = Cursors.Hand,
                Margin = new Padding(0, 0, 10, 0)
            };
            _btnToggleServer.FlatAppearance.BorderSize = 1;
            _btnToggleServer.FlatAppearance.BorderColor = Color.FromArgb(140, 35, 35);
            _btnToggleServer.Click += (s, e) =>
            {
                if (_engine.IsRunning)
                {
                    _engine.ReleaseController();
                    _engine.Stop();
                    _btEngine.Stop();
                    AppendLogSafe("Receiver stopped by user (Turned off).");
                }
                else
                {
                    _engine.Start();
                    _ = _btEngine.StartAsync();
                    AppendLogSafe("Receiver started by user (Listening).");
                }
                UpdateUiFromState();
            };

            btnRow.Controls.Add(_btnDisconnectPhone);
            btnRow.Controls.Add(_btnToggleServer);

            inner.Controls.Add(_lblHeroTitle);
            inner.Controls.Add(_lblHeroDesc);
            inner.Controls.Add(btnRow);

            _pnlHeroStatus.Controls.Add(inner);
            return _pnlHeroStatus;
        }

        private Control BuildStepGuideCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            Label lblHeader = new Label
            {
                Text = "HOW TO CONNECT (EASY 3 STEPS)",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 10)
            };
            flow.Controls.Add(lblHeader);

            flow.Controls.Add(CreateGuideItem("1", "Same Wi-Fi Network", "Make sure your phone and laptop are connected to the same Wi-Fi or mobile hotspot."));
            flow.Controls.Add(CreateGuideItem("2", "Open AirGlide on Phone", "Launch the app on your phone. It will automatically detect this laptop."));
            flow.Controls.Add(CreateGuideItem("3", "Control Your Laptop", "Swipe your finger on your phone screen to glide the mouse pointer, or type live!"));

            card.Controls.Add(flow);
            return card;
        }

        private Control CreateGuideItem(string stepNum, string stepTitle, string stepDetail)
        {
            TableLayoutPanel row = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 1,
                Margin = new Padding(0, 0, 0, 8),
                BackColor = CardBg
            };
            row.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 32f));
            row.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));

            Label numBadge = new Label
            {
                Text = stepNum,
                Font = new Font("Segoe UI", 10f, FontStyle.Bold),
                ForeColor = TextPrimary,
                BackColor = Color.FromArgb(36, 42, 58),
                TextAlign = ContentAlignment.MiddleCenter,
                Size = new Size(24, 24),
                Margin = new Padding(0, 2, 8, 0)
            };

            FlowLayoutPanel textCol = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };
            Label title = new Label
            {
                Text = stepTitle,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                ForeColor = TextPrimary,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 2)
            };
            Label detail = new Label
            {
                Text = stepDetail,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0)
            };
            textCol.Controls.Add(title);
            textCol.Controls.Add(detail);

            row.Controls.Add(numBadge, 0, 0);
            row.Controls.Add(textCol, 1, 0);
            return row;
        }

        private Control BuildNetworkCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            TableLayoutPanel table = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 3,
                BackColor = CardBg
            };
            table.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 75f));
            table.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 25f));

            Label lblHeading = new Label
            {
                Text = "LAPTOP WI-FI ADDRESS",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = TextMuted,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 4)
            };

            _lblIpDisplay = new Label
            {
                Text = $"{_engine.LocalIp}  (Port {_engine.Port})",
                Font = new Font("Consolas", 15f, FontStyle.Bold),
                ForeColor = TextPrimary,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 6)
            };

            _btnCopyIp = new Button
            {
                Text = "📋 Copy IP",
                Size = new Size(100, 34),
                BackColor = CardBorder,
                ForeColor = TextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                Anchor = AnchorStyles.Right
            };
            _btnCopyIp.FlatAppearance.BorderSize = 0;
            _btnCopyIp.Click += (s, e) =>
            {
                Clipboard.SetText(_engine.LocalIp);
                MessageBox.Show(this, $"Copied IP ({_engine.LocalIp}) to clipboard!\n\nYour phone discovers this laptop automatically, but you can also type this IP into phone settings if needed.", "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Information);
            };

            Label lblTip = new Label
            {
                Text = "Auto-Discovery is active. You only need to type this IP if your router blocks Wi-Fi broadcast.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = AccentGreen,
                AutoSize = true,
                Margin = new Padding(0)
            };

            table.Controls.Add(lblHeading, 0, 0);
            table.Controls.Add(_lblIpDisplay, 0, 1);
            table.Controls.Add(_btnCopyIp, 1, 1);
            table.Controls.Add(lblTip, 0, 2);
            table.SetColumnSpan(lblTip, 2);

            card.Controls.Add(table);
            return card;
        }

        // ======================================================================
        // TAB 2: ⚙️ SETTINGS (System & Windows Preferences)
        // ======================================================================

        private Control BuildSettingsPage()
        {
            _pageSettings = new Panel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                BackColor = BgColor,
                Visible = false
            };

            TableLayoutPanel layout = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 1,
                RowCount = 4,
                BackColor = BgColor
            };
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));

            layout.Controls.Add(BuildShortcutsCard());
            layout.Controls.Add(BuildStartupCard());
            layout.Controls.Add(BuildFirewallCard());
            layout.Controls.Add(BuildPowerModeCard());

            _pageSettings.Controls.Add(layout);
            return _pageSettings;
        }

        private Control BuildShortcutsCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            Label lblTitle = new Label
            {
                Text = "WINDOWS SHORTCUTS & TASKBAR PINNING",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 8)
            };
            flow.Controls.Add(lblTitle);

            _btnMakeShortcuts = new Button
            {
                Text = "📌 Add Shortcut to Start Menu & Desktop",
                Size = new Size(340, 38),
                BackColor = AccentCyan,
                ForeColor = Color.Black,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                Margin = new Padding(0, 0, 0, 8)
            };
            _btnMakeShortcuts.FlatAppearance.BorderSize = 0;
            _btnMakeShortcuts.Click += (s, e) =>
            {
                var res = WindowsShortcutHelper.InstallAndCreateShortcuts();
                if (res.Success)
                {
                    MessageBox.Show(this,
                        "AirGlide Remote shortcut created on your Desktop and in your Start Menu with the custom logo!\n\nTo pin to taskbar in Windows 11:\n1. Search 'AirGlide Remote' in your Start Menu\n2. Right-click it and click 'Pin to taskbar'!",
                        "AirGlide Remote",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information);
                }
                else
                {
                    MessageBox.Show(this, res.Message, "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            };
            flow.Controls.Add(_btnMakeShortcuts);

            Label lblPinTip = new Label
            {
                Text = "Tip: Press Windows key, search 'AirGlide Remote', right-click it, and select 'Pin to taskbar'.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 4)
            };
            flow.Controls.Add(lblPinTip);

            card.Controls.Add(flow);
            return card;
        }

        private Control BuildStartupCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            Label lblTitle = new Label
            {
                Text = "AUTOMATIC SYSTEM STARTUP",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 8)
            };
            flow.Controls.Add(lblTitle);

            _chkAutoStart = new CheckBox
            {
                Text = "Start AirGlide automatically when your laptop turns on",
                AutoSize = true,
                ForeColor = TextPrimary,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                Checked = WindowsShortcutHelper.IsRunAtStartupEnabled(),
                Margin = new Padding(0, 2, 0, 4)
            };
            _chkAutoStart.CheckedChanged += (s, e) =>
            {
                WindowsShortcutHelper.SetRunAtStartup(_chkAutoStart.Checked);
            };
            flow.Controls.Add(_chkAutoStart);

            Label lblDesc = new Label
            {
                Text = "AirGlide will start silently on Windows boot, ready for your phone to connect anytime.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0)
            };
            flow.Controls.Add(lblDesc);

            card.Controls.Add(flow);
            return card;
        }

        private Control BuildFirewallCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            Label lblTitle = new Label
            {
                Text = "WINDOWS DEFENDER FIREWALL",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 8)
            };
            flow.Controls.Add(lblTitle);

            _btnFirewallFix = new Button
            {
                Text = "🛡️ Allow AirGlide in Windows Firewall",
                Size = new Size(300, 36),
                BackColor = CardBorder,
                ForeColor = TextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                Margin = new Padding(0, 0, 0, 6)
            };
            _btnFirewallFix.FlatAppearance.BorderColor = CardBorder;
            _btnFirewallFix.Click += (s, e) =>
            {
                try
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = "netsh",
                        Arguments = "advfirewall firewall add rule name=\"AirGlide UDP\" dir=in action=allow protocol=UDP localport=5055,5056 profile=private,public,domain",
                        Verb = "runas",
                        UseShellExecute = true
                    });
                    MessageBox.Show(this, "Windows Firewall rule added for UDP ports 5055 and 5056!", "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                catch (Exception ex)
                {
                    MessageBox.Show(this, "Firewall configuration: " + ex.Message, "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            };
            flow.Controls.Add(_btnFirewallFix);

            Label lblDesc = new Label
            {
                Text = "AirGlide uses low-latency UDP ports 5055 & 5056. This ensures traffic is not blocked by Windows.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0)
            };
            flow.Controls.Add(lblDesc);

            card.Controls.Add(flow);
            return card;
        }

        private Control BuildPowerModeCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            Label lblTitle = new Label
            {
                Text = "BATTERY SAVING & BACKGROUND MODE",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 8)
            };
            flow.Controls.Add(lblTitle);

            _btnMinimize = new Button
            {
                Text = "⬇ Minimize to System Tray",
                Size = new Size(240, 36),
                BackColor = CardBorder,
                ForeColor = TextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                Margin = new Padding(0, 0, 0, 6)
            };
            _btnMinimize.FlatAppearance.BorderColor = CardBorder;
            _btnMinimize.Click += (s, e) =>
            {
                this.Hide();
                _trayIcon.ShowBalloonTip(1500, "AirGlide Remote", "Running silently in system tray. Double-click tray icon to open.", ToolTipIcon.Info);
            };
            flow.Controls.Add(_btnMinimize);

            Label lblDesc = new Label
            {
                Text = "Closing the window with [X] terminates the app completely so zero battery is used. Click above if you prefer running silently in the system tray.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0)
            };
            flow.Controls.Add(lblDesc);

            card.Controls.Add(flow);
            return card;
        }

        // ======================================================================
        // TAB 3: 📊 ACTIVITY & LOGS (Diagnostics & Technical Stream)
        // ======================================================================

        private Control BuildLogsPage()
        {
            _pageLogs = new Panel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                BackColor = BgColor,
                Visible = false
            };

            TableLayoutPanel layout = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 1,
                RowCount = 2,
                BackColor = BgColor
            };
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));

            layout.Controls.Add(BuildDiagnosticsStatsCard());
            layout.Controls.Add(BuildDiagnosticsLogCard());

            _pageLogs.Controls.Add(layout);
            return _pageLogs;
        }

        private Control BuildDiagnosticsStatsCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            TableLayoutPanel statsGrid = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 3,
                RowCount = 1,
                BackColor = CardBg
            };
            statsGrid.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 33.33f));
            statsGrid.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 33.33f));
            statsGrid.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 33.34f));

            _lblStatsStatus = new Label
            {
                Text = "Listening on UDP 5055",
                Font = new Font("Segoe UI", 10f, FontStyle.Bold),
                ForeColor = AccentGreen,
                AutoSize = true
            };
            _lblStatsDevice = new Label
            {
                Text = "None (Waiting)",
                Font = new Font("Segoe UI", 10f, FontStyle.Bold),
                ForeColor = TextPrimary,
                AutoSize = true
            };
            _lblStatsPackets = new Label
            {
                Text = "0 packets",
                Font = new Font("Segoe UI", 10f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true
            };

            statsGrid.Controls.Add(CreateStatBox("SERVER STATUS", _lblStatsStatus), 0, 0);
            statsGrid.Controls.Add(CreateStatBox("CONNECTED PHONE", _lblStatsDevice), 1, 0);
            statsGrid.Controls.Add(CreateStatBox("PACKETS RECEIVED", _lblStatsPackets), 2, 0);

            card.Controls.Add(statsGrid);
            return card;
        }

        private Control CreateStatBox(string caption, Label valueLabel)
        {
            FlowLayoutPanel box = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg,
                Margin = new Padding(0, 0, 8, 0)
            };

            Label lblCap = new Label
            {
                Text = caption,
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = TextMuted,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 4)
            };

            box.Controls.Add(lblCap);
            box.Controls.Add(valueLabel);
            return box;
        }

        private Control BuildDiagnosticsLogCard()
        {
            Panel card = CreateCardContainer();
            card.Margin = new Padding(0, 0, 0, 14);
            card.Padding = new Padding(20, 14, 20, 16);

            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = CardBg
            };

            TableLayoutPanel topRow = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 1,
                BackColor = CardBg,
                Margin = new Padding(0, 0, 0, 8)
            };
            topRow.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 60f));
            topRow.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 40f));

            Label lblTitle = new Label
            {
                Text = "LIVE ACTIVITY LOG",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = AccentCyan,
                AutoSize = true,
                Anchor = AnchorStyles.Left
            };

            FlowLayoutPanel btnGroup = new FlowLayoutPanel
            {
                AutoSize = true,
                FlowDirection = FlowDirection.RightToLeft,
                Anchor = AnchorStyles.Right,
                BackColor = CardBg,
                Margin = new Padding(0)
            };

            _btnCopyLogs = new Button
            {
                Text = "📋 Copy Log",
                Size = new Size(95, 30),
                BackColor = CardBorder,
                ForeColor = TextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                Margin = new Padding(4, 0, 0, 0)
            };
            _btnCopyLogs.FlatAppearance.BorderSize = 0;
            _btnCopyLogs.Click += (s, e) =>
            {
                var allText = string.Join(Environment.NewLine, _lstLogs.Items.Cast<object>());
                if (!string.IsNullOrEmpty(allText))
                {
                    Clipboard.SetText(allText);
                    MessageBox.Show(this, "Copied log contents to clipboard!", "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
            };

            _btnClearLogs = new Button
            {
                Text = "🧹 Clear",
                Size = new Size(80, 30),
                BackColor = CardBorder,
                ForeColor = TextSecondary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                Margin = new Padding(0)
            };
            _btnClearLogs.FlatAppearance.BorderSize = 0;
            _btnClearLogs.Click += (s, e) => _lstLogs.Items.Clear();

            btnGroup.Controls.Add(_btnCopyLogs);
            btnGroup.Controls.Add(_btnClearLogs);

            topRow.Controls.Add(lblTitle, 0, 0);
            topRow.Controls.Add(btnGroup, 1, 0);
            flow.Controls.Add(topRow);

            _lstLogs = new ListBox
            {
                Size = new Size(570, 260),
                BackColor = BgColor,
                ForeColor = TextSecondary,
                BorderStyle = BorderStyle.None,
                Font = new Font("Consolas", 9f, FontStyle.Regular),
                Margin = new Padding(0)
            };
            flow.Controls.Add(_lstLogs);

            card.Controls.Add(flow);
            return card;
        }

        private Panel CreateCardContainer()
        {
            Panel card = new Panel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                BackColor = CardBg
            };
            card.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, card.ClientRectangle, CardBorder, ButtonBorderStyle.Solid);
            };
            return card;
        }

        // ======================================================================
        // System Tray & Activation
        // ======================================================================

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool SetForegroundWindow(IntPtr hWnd);

        private void SetupTrayMenu()
        {
            _trayMenu = new ContextMenuStrip();
            _trayMenu.Items.Add("Open AirGlide Remote", null, (s, e) => RestoreWindow());
            _trayMenu.Items.Add("-");
            _trayMenu.Items.Add("Stop Server / Disconnect", null, (s, e) =>
            {
                _engine.ReleaseController();
                _engine.Stop();
                _btEngine.Stop();
                AppendLogSafe("Receiver stopped from tray menu.");
                UpdateUiFromState();
            });
            _trayMenu.Items.Add("Start Server", null, (s, e) =>
            {
                _engine.Start();
                _ = _btEngine.StartAsync();
                AppendLogSafe("Receiver started from tray menu.");
                UpdateUiFromState();
            });
            _trayMenu.Items.Add("-");
            _trayMenu.Items.Add("Exit AirGlide (Quit)", null, (s, e) =>
            {
                _refreshTimer?.Stop();
                _engine?.Stop();
                _btEngine?.Stop();
                if (_trayIcon != null)
                {
                    _trayIcon.Visible = false;
                    _trayIcon.Dispose();
                }
                Application.Exit();
                Environment.Exit(0);
            });

            _trayIcon = new NotifyIcon
            {
                Text = "AirGlide Remote",
                Icon = this.Icon ?? SystemIcons.Application,
                ContextMenuStrip = _trayMenu,
                Visible = true
            };
            _trayIcon.DoubleClick += (s, e) => RestoreWindow();
        }

        public void RestoreWindow()
        {
            if (this.InvokeRequired)
            {
                if (!this.IsHandleCreated || this.IsDisposed) return;
                try { this.BeginInvoke(new Action(RestoreWindow)); } catch { }
                return;
            }

            if (!this.Visible) this.Show();
            if (this.WindowState == FormWindowState.Minimized)
            {
                this.WindowState = FormWindowState.Normal;
            }
            this.ShowInTaskbar = true;
            this.BringToFront();
            this.Activate();
            try { SetForegroundWindow(this.Handle); } catch { }
        }

        // ======================================================================
        // State Update & Logging
        // ======================================================================

        private void UpdateUiFromState()
        {
            if (this.InvokeRequired)
            {
                if (!this.IsHandleCreated || this.IsDisposed) return;
                try { this.BeginInvoke(new Action(UpdateUiFromState)); } catch { }
                return;
            }

            if (_engine.IsRunning)
            {
                _lblStatusBadge.BackColor = Color.FromArgb(16, 45, 28);
                _lblStatusBadge.ForeColor = AccentGreen;
                _lblStatusBadge.Text = $"● RUNNING (Port {_engine.Port})";

                _btnToggleServer.Text = "⏹ Stop Server / Turn Off";
                _btnToggleServer.BackColor = Color.FromArgb(50, 18, 18);
                _btnToggleServer.ForeColor = Color.FromArgb(248, 113, 113);
                _btnToggleServer.FlatAppearance.BorderColor = Color.FromArgb(140, 35, 35);

                _lblIpDisplay.Text = $"{_engine.LocalIp}  (Port {_engine.Port})";

                if (_btEngine != null && _btEngine.IsConnected)
                {
                    _lblHeroTitle.Text = $"🔵 Bluetooth Connected: {_btEngine.ConnectedDeviceName}";
                    _lblHeroTitle.ForeColor = AccentCyan;
                    _lblHeroDesc.Text = "Connected directly via Bluetooth RFCOMM SPP. Controlling mouse & keyboard without Wi-Fi!";
                    _btnDisconnectPhone.Visible = true;
                    _lblStatsDevice.Text = $"{_btEngine.ConnectedDeviceName} (Bluetooth)";
                }
                else if (_engine.ActiveDeviceId != null && _engine.Devices.TryGetValue(_engine.ActiveDeviceId, out var dev))
                {
                    _lblHeroTitle.Text = $"🟢 Phone Connected: {dev.Name}";
                    _lblHeroTitle.ForeColor = AccentGreen;
                    _lblHeroDesc.Text = $"{dev.PacketCount} packets received • Your phone is controlling this laptop.";
                    _btnDisconnectPhone.Visible = true;
                    _lblStatsDevice.Text = $"{dev.Name} ({dev.IpAddress})";
                }
                else if (_engine.Devices.Count > 0)
                {
                    var first = _engine.Devices.Values.First();
                    _lblHeroTitle.Text = $"🟢 Phone Connected: {first.Name}";
                    _lblHeroTitle.ForeColor = AccentGreen;
                    _lblHeroDesc.Text = $"{_engine.TotalPackets} packets received • Your phone is controlling this laptop.";
                    _btnDisconnectPhone.Visible = true;
                    _lblStatsDevice.Text = $"{first.Name} ({first.IpAddress})";
                }
                else
                {
                    _lblHeroTitle.Text = "🟢 Ready to Connect";
                    _lblHeroTitle.ForeColor = AccentGreen;
                    _lblHeroDesc.Text = "Open the AirGlide app on your phone. It connects automatically over Wi-Fi.";
                    _btnDisconnectPhone.Visible = false;
                    _lblStatsDevice.Text = "None (Waiting)";
                }

                _lblStatsStatus.Text = $"Listening on UDP {_engine.Port}" + (_btEngine?.IsRunning == true ? " & Bluetooth SPP" : "");
                _lblStatsStatus.ForeColor = AccentGreen;
            }
            else
            {
                _lblStatusBadge.BackColor = Color.FromArgb(50, 18, 18);
                _lblStatusBadge.ForeColor = AccentRed;
                _lblStatusBadge.Text = "● STOPPED (Disconnected)";

                _lblHeroTitle.Text = "🔴 Receiver Stopped / Disconnected";
                _lblHeroTitle.ForeColor = AccentRed;
                _lblHeroDesc.Text = "Receiver is turned off. Zero battery is being used. Click 'Start Server' when you are ready to connect.";

                _btnDisconnectPhone.Visible = false;

                _btnToggleServer.Text = "▶ Start Server (Turn On)";
                _btnToggleServer.BackColor = Color.FromArgb(16, 45, 28);
                _btnToggleServer.ForeColor = Color.FromArgb(74, 222, 128);
                _btnToggleServer.FlatAppearance.BorderColor = Color.FromArgb(34, 197, 94);

                _lblIpDisplay.Text = $"{_engine.LocalIp}  (Offline)";
                _lblStatsDevice.Text = "Offline";
                _lblStatsStatus.Text = "Server Stopped";
                _lblStatsStatus.ForeColor = AccentRed;
            }

            _lblStatsPackets.Text = $"{_engine.TotalPackets:N0} packets";
        }

        private void AppendLogSafe(string text)
        {
            if (this.InvokeRequired)
            {
                if (!this.IsHandleCreated || this.IsDisposed) return;
                try { this.BeginInvoke(new Action<string>(AppendLogSafe), text); } catch { }
                return;
            }

            string line = $"[{DateTime.Now:HH:mm:ss}] {text}";
            _lstLogs.Items.Insert(0, line);
            if (_lstLogs.Items.Count > 150)
            {
                _lstLogs.Items.RemoveAt(_lstLogs.Items.Count - 1);
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            _refreshTimer?.Stop();
            _btEngine?.Stop();
            _engine?.Stop();
            if (_trayIcon != null)
            {
                _trayIcon.Visible = false;
                _trayIcon.Dispose();
            }
            base.OnFormClosing(e);
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            base.OnFormClosed(e);
            Environment.Exit(0);
        }
    }
}
