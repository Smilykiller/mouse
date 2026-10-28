using System;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    public class MainForm : Form
    {
        private readonly UdpReceiverEngine _engine;
        private readonly bool _startMinimized;
        private NotifyIcon _trayIcon = null!;
        private ContextMenuStrip _trayMenu = null!;

        // Dynamic State UI Elements
        private Label _lblStatusBadge = null!;
        private Panel _pnlHeroStatus = null!;
        private Label _lblHeroTitle = null!;
        private Label _lblHeroDesc = null!;
        private Button _btnDisconnectPhone = null!;
        private Button _btnToggleServer = null!;

        private Label _lblIpDisplay = null!;
        private Button _btnCopyIp = null!;

        private CheckBox _chkAutoStart = null!;
        private Button _btnMakeShortcuts = null!;

        private Panel _pnlLogsDrawer = null!;
        private Button _btnToggleLogs = null!;
        private ListBox _lstLogs = null!;

        private Button _btnMinimize = null!;
        private System.Windows.Forms.Timer _refreshTimer = null!;

        // Modern Fluent Palette
        private readonly Color BgColor = Color.FromArgb(15, 17, 23);
        private readonly Color CardBg = Color.FromArgb(24, 27, 36);
        private readonly Color CardBorder = Color.FromArgb(42, 47, 62);
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

            LoadAppIcon();
            InitializeComponent();
            SetupTrayMenu();

            _engine.OnLog += msg => AppendLogSafe($"[SYS] {msg}");
            _engine.OnPacketProcessed += (cmd, detail) =>
            {
                if (cmd != "MOVE") AppendLogSafe($"[{cmd}] {detail}");
            };

            _engine.Start();
            UpdateUiFromState();

            if (_startMinimized)
            {
                this.WindowState = FormWindowState.Minimized;
                this.ShowInTaskbar = false;
            }
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
            this.Size = new Size(650, 750);
            this.MinimumSize = new Size(580, 680);
            this.BackColor = BgColor;
            this.ForeColor = TextPrimary;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.Font = new Font("Segoe UI", 10f, FontStyle.Regular);
            this.AutoScaleMode = AutoScaleMode.Dpi;

            // Scrollable view for all DPIs
            Panel scrollWrapper = new Panel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                BackColor = BgColor,
                Padding = new Padding(24, 16, 24, 20)
            };

            // Main vertical layout
            TableLayoutPanel layout = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 1,
                RowCount = 6,
                BackColor = BgColor
            };
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));

            // 1. Header with App Logo & Title
            layout.Controls.Add(BuildHeaderRow());

            // 2. Hero Status Card (Big, Friendly, Clean)
            layout.Controls.Add(BuildHeroStatusCard());

            // 3. Clear 3-Step Guide for Beginners
            layout.Controls.Add(BuildStepGuideCard());

            // 4. Laptop Wi-Fi Connection Address Card
            layout.Controls.Add(BuildNetworkCard());

            // 5. Windows Taskbar, Start Menu & Startup Card
            layout.Controls.Add(BuildShortcutsCard());

            // 6. Diagnostics & Footer Bar
            layout.Controls.Add(BuildFooterSection());

            scrollWrapper.Controls.Add(layout);
            this.Controls.Add(scrollWrapper);

            // Timer to refresh status smoothly
            _refreshTimer = new System.Windows.Forms.Timer { Interval = 800 };
            _refreshTimer.Tick += (s, e) => UpdateUiFromState();
            _refreshTimer.Start();
        }

        private Control BuildHeaderRow()
        {
            TableLayoutPanel pnl = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 1,
                Margin = new Padding(0, 0, 0, 14),
                BackColor = BgColor
            };
            pnl.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 70f));
            pnl.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 30f));

            // Left branding: Logo + Title + Subtitle
            FlowLayoutPanel branding = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill,
                AutoSize = true,
                FlowDirection = FlowDirection.LeftToRight,
                WrapContents = false,
                BackColor = BgColor
            };

            PictureBox picLogo = new PictureBox
            {
                Size = new Size(42, 42),
                SizeMode = PictureBoxSizeMode.Zoom,
                Margin = new Padding(0, 2, 12, 0)
            };
            try
            {
                string pngPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", "app_icon.png");
                if (File.Exists(pngPath))
                {
                    picLogo.Image = Image.FromFile(pngPath);
                }
                else if (this.Icon != null)
                {
                    picLogo.Image = this.Icon.ToBitmap();
                }
            }
            catch { }

            FlowLayoutPanel titleText = new FlowLayoutPanel
            {
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = BgColor
            };
            Label lblApp = new Label
            {
                Text = "AirGlide Remote",
                Font = new Font("Segoe UI", 16f, FontStyle.Bold),
                ForeColor = TextPrimary,
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 2)
            };
            Label lblTag = new Label
            {
                Text = "Wireless Mouse & Keyboard for Windows",
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                ForeColor = TextSecondary,
                AutoSize = true,
                Margin = new Padding(0)
            };
            titleText.Controls.Add(lblApp);
            titleText.Controls.Add(lblTag);

            branding.Controls.Add(picLogo);
            branding.Controls.Add(titleText);
            pnl.Controls.Add(branding, 0, 0);

            // Right: Status Badge
            _lblStatusBadge = new Label
            {
                Text = "● READY",
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                ForeColor = AccentGreen,
                BackColor = CardBg,
                TextAlign = ContentAlignment.MiddleCenter,
                Size = new Size(130, 36),
                Anchor = AnchorStyles.Right,
                Margin = new Padding(0, 4, 0, 0)
            };
            pnl.Controls.Add(_lblStatusBadge, 1, 0);

            return pnl;
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
                    AppendLogSafe("Receiver stopped by user (Turned off).");
                }
                else
                {
                    _engine.Start();
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

            // Step 1
            flow.Controls.Add(CreateGuideItem("1", "Same Wi-Fi Network", "Make sure your phone and laptop are connected to the same Wi-Fi or mobile hotspot."));
            // Step 2
            flow.Controls.Add(CreateGuideItem("2", "Open AirGlide on Phone", "Launch the app on your phone. It will automatically detect this laptop."));
            // Step 3
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

            // Number Badge
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
                MessageBox.Show(this, $"Copied IP ({_engine.LocalIp}) to clipboard!\n\nYour phone will find this laptop automatically, but you can also type this IP in your phone if needed.", "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Information);
            };

            Label lblTip = new Label
            {
                Text = "Auto-Discovery is active. You only need to type this IP if your Wi-Fi router blocks auto-discovery.",
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
                Text = "EASY ACCESS & TASKBAR PINNING",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = TextMuted,
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

            _chkAutoStart = new CheckBox
            {
                Text = "Start AirGlide automatically when your laptop turns on",
                AutoSize = true,
                ForeColor = TextSecondary,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                Checked = WindowsShortcutHelper.IsRunAtStartupEnabled(),
                Margin = new Padding(0, 4, 0, 0)
            };
            _chkAutoStart.CheckedChanged += (s, e) =>
            {
                WindowsShortcutHelper.SetRunAtStartup(_chkAutoStart.Checked);
            };
            flow.Controls.Add(_chkAutoStart);

            card.Controls.Add(flow);
            return card;
        }

        private Control BuildFooterSection()
        {
            FlowLayoutPanel flow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                Margin = new Padding(0, 0, 0, 10),
                BackColor = BgColor
            };

            // Main Buttons Row: Minimize to Tray & Technical Details Toggle
            TableLayoutPanel actionRow = new TableLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                ColumnCount = 2,
                RowCount = 1,
                Margin = new Padding(0, 0, 0, 8),
                BackColor = BgColor
            };
            actionRow.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50f));
            actionRow.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50f));

            _btnMinimize = new Button
            {
                Text = "⬇ Minimize to System Tray",
                Size = new Size(240, 36),
                BackColor = CardBg,
                ForeColor = TextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular)
            };
            _btnMinimize.FlatAppearance.BorderColor = CardBorder;
            _btnMinimize.Click += (s, e) =>
            {
                this.Hide();
                _trayIcon.ShowBalloonTip(1500, "AirGlide Remote", "Running silently in system tray. Double-click icon to open anytime.", ToolTipIcon.Info);
            };

            _btnToggleLogs = new Button
            {
                Text = "⚙ Advanced / Activity Log ▼",
                Size = new Size(240, 36),
                BackColor = CardBg,
                ForeColor = TextSecondary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                Anchor = AnchorStyles.Right
            };
            _btnToggleLogs.FlatAppearance.BorderColor = CardBorder;

            actionRow.Controls.Add(_btnMinimize, 0, 0);
            actionRow.Controls.Add(_btnToggleLogs, 1, 0);
            flow.Controls.Add(actionRow);

            // Collapsible Log Drawer
            _pnlLogsDrawer = new Panel
            {
                Size = new Size(580, 160),
                BackColor = CardBg,
                Visible = false,
                Margin = new Padding(0, 4, 0, 10),
                Padding = new Padding(12)
            };
            _pnlLogsDrawer.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, _pnlLogsDrawer.ClientRectangle, CardBorder, ButtonBorderStyle.Solid);
            };

            _lstLogs = new ListBox
            {
                Dock = DockStyle.Fill,
                BackColor = BgColor,
                ForeColor = TextSecondary,
                BorderStyle = BorderStyle.None,
                Font = new Font("Consolas", 8.5f, FontStyle.Regular)
            };
            _pnlLogsDrawer.Controls.Add(_lstLogs);

            bool logExpanded = false;
            _btnToggleLogs.Click += (s, e) =>
            {
                logExpanded = !logExpanded;
                _pnlLogsDrawer.Visible = logExpanded;
                _btnToggleLogs.Text = logExpanded ? "⚙ Hide Advanced Log ▲" : "⚙ Advanced / Activity Log ▼";
            };

            flow.Controls.Add(_pnlLogsDrawer);
            return flow;
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
                AppendLogSafe("Receiver stopped from tray menu.");
                UpdateUiFromState();
            });
            _trayMenu.Items.Add("Start Server", null, (s, e) =>
            {
                _engine.Start();
                AppendLogSafe("Receiver started from tray menu.");
                UpdateUiFromState();
            });
            _trayMenu.Items.Add("-");
            _trayMenu.Items.Add("Exit AirGlide (Quit)", null, (s, e) =>
            {
                _refreshTimer?.Stop();
                _engine?.Stop();
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

                if (_engine.ActiveDeviceId != null && _engine.Devices.TryGetValue(_engine.ActiveDeviceId, out var dev))
                {
                    _lblHeroTitle.Text = $"🟢 Phone Connected: {dev.Name}";
                    _lblHeroTitle.ForeColor = AccentGreen;
                    _lblHeroDesc.Text = $"{dev.PacketCount} packets received • Your phone is controlling this laptop.";
                    _btnDisconnectPhone.Visible = true;
                }
                else if (_engine.Devices.Count > 0)
                {
                    var first = _engine.Devices.Values.First();
                    _lblHeroTitle.Text = $"🟢 Phone Connected: {first.Name}";
                    _lblHeroTitle.ForeColor = AccentGreen;
                    _lblHeroDesc.Text = $"{_engine.TotalPackets} packets received • Your phone is controlling this laptop.";
                    _btnDisconnectPhone.Visible = true;
                }
                else
                {
                    _lblHeroTitle.Text = "🟢 Ready to Connect";
                    _lblHeroTitle.ForeColor = AccentGreen;
                    _lblHeroDesc.Text = "Open the AirGlide app on your phone. It connects automatically over Wi-Fi.";
                    _btnDisconnectPhone.Visible = false;
                }
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
            }
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
