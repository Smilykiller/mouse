using System;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    public class MainForm : Form
    {
        private readonly UdpReceiverEngine _engine;
        private readonly bool _startMinimized;
        private NotifyIcon _trayIcon = null!;
        private ContextMenuStrip _trayMenu = null!;

        // Controls
        private Label _lblStatusDot = null!;
        private Label _lblStatusText = null!;
        private Label _lblActiveDevice = null!;
        private Label _lblActiveSub = null!;
        private Button _btnDisconnectPhone = null!;

        private Label _lblIpAddress = null!;
        private Button _btnCopyIp = null!;

        private CheckBox _chkRunAtStartup = null!;
        private Button _btnCreateShortcuts = null!;

        private Panel _pnlLogSection = null!;
        private Button _btnToggleLog = null!;
        private ListBox _lstLogs = null!;

        private Button _btnToggleServer = null!;
        private Button _btnMinimizeTray = null!;
        private System.Windows.Forms.Timer _uiRefreshTimer = null!;

        // Theme Colors
        private readonly Color ColorBg = Color.FromArgb(12, 12, 16);
        private readonly Color ColorCard = Color.FromArgb(22, 22, 28);
        private readonly Color ColorCardHighlight = Color.FromArgb(28, 28, 36);
        private readonly Color ColorBorder = Color.FromArgb(45, 45, 55);
        private readonly Color ColorTextPrimary = Color.FromArgb(250, 250, 250);
        private readonly Color ColorTextSecondary = Color.FromArgb(160, 160, 175);
        private readonly Color ColorTextMuted = Color.FromArgb(110, 110, 125);
        private readonly Color ColorGreen = Color.FromArgb(34, 197, 94);
        private readonly Color ColorCyan = Color.FromArgb(6, 182, 212);
        private readonly Color ColorRed = Color.FromArgb(239, 68, 68);

        public MainForm(bool startMinimized = false)
        {
            _startMinimized = startMinimized;
            _engine = new UdpReceiverEngine(5055);

            LoadAppIcon();
            InitializeComponent();
            SetupTrayIcon();

            _engine.OnLog += msg =>
            {
                AppendLogSafe($"[SYS] {msg}");
            };
            _engine.OnPacketProcessed += (cmd, detail) =>
            {
                if (cmd != "MOVE") // Don't flood the UI list with high-rate mouse moves
                {
                    AppendLogSafe($"[{cmd}] {detail}");
                }
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
            this.Size = new Size(680, 720);
            this.MinimumSize = new Size(620, 600);
            this.BackColor = ColorBg;
            this.ForeColor = ColorTextPrimary;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);

            // Main scrollable canvas
            Panel scrollCanvas = new Panel
            {
                Dock = DockStyle.Fill,
                AutoScroll = true,
                BackColor = ColorBg,
                Padding = new Padding(18)
            };

            // Vertical container
            FlowLayoutPanel contentFlow = new FlowLayoutPanel
            {
                Dock = DockStyle.Top,
                AutoSize = true,
                FlowDirection = FlowDirection.TopDown,
                WrapContents = false,
                BackColor = ColorBg,
                Width = 620
            };

            // 1. Header with Logo & Status
            Panel pnlHeader = CreateHeaderPanel();
            contentFlow.Controls.Add(pnlHeader);

            // 2. Active Controller Status Card
            Panel pnlDeviceCard = CreateDeviceCard();
            contentFlow.Controls.Add(pnlDeviceCard);

            // 3. Simple 3-Step Guide for New Users
            Panel pnlGuide = CreateQuickGuideCard();
            contentFlow.Controls.Add(pnlGuide);

            // 4. Laptop Network & Connection Info
            Panel pnlNetwork = CreateNetworkCard();
            contentFlow.Controls.Add(pnlNetwork);

            // 5. Windows Taskbar & Start Menu Shortcuts Card
            Panel pnlShortcuts = CreateShortcutsCard();
            contentFlow.Controls.Add(pnlShortcuts);

            // 6. Diagnostics & Log Panel (Collapsible)
            _pnlLogSection = CreateLogSection();
            contentFlow.Controls.Add(_pnlLogSection);

            // 7. Bottom Action Controls
            Panel pnlActions = CreateBottomActions();
            contentFlow.Controls.Add(pnlActions);

            scrollCanvas.Controls.Add(contentFlow);
            this.Controls.Add(scrollCanvas);

            // Refresh timer for live UI stats
            _uiRefreshTimer = new System.Windows.Forms.Timer { Interval = 800 };
            _uiRefreshTimer.Tick += (s, e) => UpdateUiFromState();
            _uiRefreshTimer.Start();
        }

        private Panel CreateHeaderPanel()
        {
            Panel pnl = new Panel
            {
                Size = new Size(610, 52),
                Margin = new Padding(0, 0, 0, 12),
                BackColor = ColorBg
            };

            // Logo Picture
            PictureBox picLogo = new PictureBox
            {
                Size = new Size(38, 38),
                Location = new Point(0, 6),
                SizeMode = PictureBoxSizeMode.Zoom
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

            Label lblTitle = new Label
            {
                Text = "AirGlide Remote",
                Font = new Font("Segoe UI", 15f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                AutoSize = true,
                Location = new Point(46, 4)
            };

            Label lblSub = new Label
            {
                Text = "Ultra-Low Latency Wireless Trackpad & Keyboard",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = ColorTextSecondary,
                AutoSize = true,
                Location = new Point(48, 28)
            };

            // Status Badge (Top Right)
            Panel pnlBadge = new Panel
            {
                Size = new Size(160, 32),
                Location = new Point(445, 10),
                BackColor = ColorCard
            };
            pnlBadge.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, pnlBadge.ClientRectangle, ColorBorder, ButtonBorderStyle.Solid);
            };

            _lblStatusDot = new Label
            {
                Text = "●",
                Font = new Font("Segoe UI", 12f, FontStyle.Bold),
                ForeColor = ColorGreen,
                Size = new Size(18, 24),
                Location = new Point(8, 4)
            };
            _lblStatusText = new Label
            {
                Text = "LISTENING (5055)",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                Size = new Size(125, 20),
                Location = new Point(28, 7)
            };
            pnlBadge.Controls.Add(_lblStatusDot);
            pnlBadge.Controls.Add(_lblStatusText);

            pnl.Controls.Add(picLogo);
            pnl.Controls.Add(lblTitle);
            pnl.Controls.Add(lblSub);
            pnl.Controls.Add(pnlBadge);
            return pnl;
        }

        private Panel CreateDeviceCard()
        {
            Panel card = CreateRoundedCard(610, 95);
            card.Margin = new Padding(0, 0, 0, 12);

            Label lblHeading = new Label
            {
                Text = "MOBILE CONNECTION STATUS",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = ColorTextMuted,
                Location = new Point(16, 12),
                AutoSize = true
            };

            _lblActiveDevice = new Label
            {
                Text = "Waiting for phone to connect...",
                Font = new Font("Segoe UI", 13f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                Location = new Point(15, 32),
                AutoSize = true
            };

            _lblActiveSub = new Label
            {
                Text = "Open AirGlide on your phone. It connects automatically over Wi-Fi.",
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                ForeColor = ColorTextSecondary,
                Location = new Point(16, 62),
                AutoSize = true
            };

            _btnDisconnectPhone = new Button
            {
                Text = "Disconnect",
                Size = new Size(100, 32),
                Location = new Point(490, 36),
                BackColor = Color.FromArgb(45, 20, 20),
                ForeColor = Color.FromArgb(248, 113, 113),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold),
                Visible = false
            };
            _btnDisconnectPhone.FlatAppearance.BorderSize = 1;
            _btnDisconnectPhone.FlatAppearance.BorderColor = Color.FromArgb(127, 29, 29);
            _btnDisconnectPhone.Click += (s, e) =>
            {
                _engine.ReleaseController();
                UpdateUiFromState();
            };

            card.Controls.Add(lblHeading);
            card.Controls.Add(_lblActiveDevice);
            card.Controls.Add(_lblActiveSub);
            card.Controls.Add(_btnDisconnectPhone);
            return card;
        }

        private Panel CreateQuickGuideCard()
        {
            Panel card = CreateRoundedCard(610, 120);
            card.Margin = new Padding(0, 0, 0, 12);

            Label lblTitle = new Label
            {
                Text = "HOW TO USE (QUICK START GUIDE)",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = ColorCyan,
                Location = new Point(16, 10),
                AutoSize = true
            };
            card.Controls.Add(lblTitle);

            int colWidth = 190;
            // Step 1
            Panel s1 = CreateStepColumn("1. Same Wi-Fi", "Connect phone and laptop to the same Wi-Fi or mobile hotspot.", 16);
            // Step 2
            Panel s2 = CreateStepColumn("2. Open App", "Launch AirGlide on your phone. It auto-discovers this laptop.", 16 + colWidth);
            // Step 3
            Panel s3 = CreateStepColumn("3. Control PC", "Swipe to move cursor, tap buttons, or use the live keyboard.", 16 + colWidth * 2);

            card.Controls.Add(s1);
            card.Controls.Add(s2);
            card.Controls.Add(s3);
            return card;
        }

        private Panel CreateStepColumn(string title, string desc, int left)
        {
            Panel pnl = new Panel
            {
                Size = new Size(180, 75),
                Location = new Point(left, 32),
                BackColor = ColorCardHighlight
            };
            pnl.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, pnl.ClientRectangle, ColorBorder, ButtonBorderStyle.Solid);
            };

            Label lblT = new Label
            {
                Text = title,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                Location = new Point(8, 8),
                AutoSize = true
            };
            Label lblD = new Label
            {
                Text = desc,
                Font = new Font("Segoe UI", 8f, FontStyle.Regular),
                ForeColor = ColorTextSecondary,
                Location = new Point(8, 28),
                Size = new Size(164, 42)
            };
            pnl.Controls.Add(lblT);
            pnl.Controls.Add(lblD);
            return pnl;
        }

        private Panel CreateNetworkCard()
        {
            Panel card = CreateRoundedCard(610, 95);
            card.Margin = new Padding(0, 0, 0, 12);

            Label lblHeader = new Label
            {
                Text = "LAPTOP NETWORK ADDRESS",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = ColorTextMuted,
                Location = new Point(16, 10),
                AutoSize = true
            };

            _lblIpAddress = new Label
            {
                Text = $"{_engine.LocalIp}:{_engine.Port}",
                Font = new Font("Consolas", 18f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                Location = new Point(14, 28),
                AutoSize = true
            };

            _btnCopyIp = new Button
            {
                Text = "Copy IP",
                Size = new Size(80, 30),
                Location = new Point(490, 28),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Bold)
            };
            _btnCopyIp.FlatAppearance.BorderSize = 0;
            _btnCopyIp.Click += (s, e) =>
            {
                Clipboard.SetText(_engine.LocalIp);
                MessageBox.Show(this, $"Copied IP ({_engine.LocalIp}) to clipboard!", "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Information);
            };

            Label lblNotice = new Label
            {
                Text = "✓ Auto-Discovery is active. You only need to type this IP if your Wi-Fi blocks auto-discovery.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = ColorGreen,
                Location = new Point(16, 64),
                AutoSize = true
            };

            card.Controls.Add(lblHeader);
            card.Controls.Add(_lblIpAddress);
            card.Controls.Add(_btnCopyIp);
            card.Controls.Add(lblNotice);
            return card;
        }

        private Panel CreateShortcutsCard()
        {
            Panel card = CreateRoundedCard(610, 85);
            card.Margin = new Padding(0, 0, 0, 12);

            Label lblTitle = new Label
            {
                Text = "WINDOWS INTEGRATION (SHORTCUTS & TASKBAR)",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = ColorTextMuted,
                Location = new Point(16, 10),
                AutoSize = true
            };

            _btnCreateShortcuts = new Button
            {
                Text = "📌 Add to Start Menu & Desktop",
                Size = new Size(240, 34),
                Location = new Point(16, 34),
                BackColor = ColorCyan,
                ForeColor = Color.Black,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Bold)
            };
            _btnCreateShortcuts.FlatAppearance.BorderSize = 0;
            _btnCreateShortcuts.Click += (s, e) =>
            {
                var result = WindowsShortcutHelper.InstallAndCreateShortcuts();
                if (result.Success)
                {
                    MessageBox.Show(this, 
                        $"{result.Message}\n\nTo pin to Taskbar in Windows 11:\nSearch 'AirGlide' in Start, right-click and click 'Pin to taskbar'!", 
                        "AirGlide Remote", 
                        MessageBoxButtons.OK, 
                        MessageBoxIcon.Information);
                }
                else
                {
                    MessageBox.Show(this, result.Message, "AirGlide Remote", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            };

            _chkRunAtStartup = new CheckBox
            {
                Text = "Start automatically when Windows turns on",
                AutoSize = true,
                Location = new Point(275, 41),
                ForeColor = ColorTextSecondary,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                Checked = WindowsShortcutHelper.IsRunAtStartupEnabled()
            };
            _chkRunAtStartup.CheckedChanged += (s, e) =>
            {
                WindowsShortcutHelper.SetRunAtStartup(_chkRunAtStartup.Checked);
            };

            card.Controls.Add(lblTitle);
            card.Controls.Add(_btnCreateShortcuts);
            card.Controls.Add(_chkRunAtStartup);
            return card;
        }

        private Panel CreateLogSection()
        {
            Panel pnl = new Panel
            {
                Size = new Size(610, 42),
                Margin = new Padding(0, 0, 0, 8),
                BackColor = ColorCard
            };
            pnl.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, pnl.ClientRectangle, ColorBorder, ButtonBorderStyle.Solid);
            };

            _btnToggleLog = new Button
            {
                Text = "Show Activity Log ▼",
                Size = new Size(150, 28),
                Location = new Point(12, 7),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular)
            };
            _btnToggleLog.FlatAppearance.BorderSize = 0;

            Button btnClear = new Button
            {
                Text = "Clear Log",
                Size = new Size(80, 28),
                Location = new Point(170, 7),
                BackColor = ColorBorder,
                ForeColor = ColorTextSecondary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular)
            };
            btnClear.FlatAppearance.BorderSize = 0;
            btnClear.Click += (s, e) => _lstLogs.Items.Clear();

            _lstLogs = new ListBox
            {
                Location = new Point(12, 44),
                Size = new Size(586, 120),
                BackColor = ColorBg,
                ForeColor = ColorTextSecondary,
                BorderStyle = BorderStyle.FixedSingle,
                Font = new Font("Consolas", 8.5f, FontStyle.Regular),
                Visible = false
            };

            bool expanded = false;
            _btnToggleLog.Click += (s, e) =>
            {
                expanded = !expanded;
                if (expanded)
                {
                    pnl.Height = 175;
                    _lstLogs.Visible = true;
                    _btnToggleLog.Text = "Hide Activity Log ▲";
                }
                else
                {
                    pnl.Height = 42;
                    _lstLogs.Visible = false;
                    _btnToggleLog.Text = "Show Activity Log ▼";
                }
            };

            pnl.Controls.Add(_btnToggleLog);
            pnl.Controls.Add(btnClear);
            pnl.Controls.Add(_lstLogs);
            return pnl;
        }

        private Panel CreateBottomActions()
        {
            Panel pnl = new Panel
            {
                Size = new Size(610, 48),
                Margin = new Padding(0, 4, 0, 16),
                BackColor = ColorBg
            };

            _btnMinimizeTray = new Button
            {
                Text = "Minimize to System Tray",
                Size = new Size(180, 36),
                Location = new Point(0, 4),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular)
            };
            _btnMinimizeTray.FlatAppearance.BorderSize = 0;
            _btnMinimizeTray.Click += (s, e) =>
            {
                this.Hide();
                _trayIcon.ShowBalloonTip(1500, "AirGlide Remote", "Running in system tray. Double-click to reopen.", ToolTipIcon.Info);
            };

            _btnToggleServer = new Button
            {
                Text = "Pause Receiver",
                Size = new Size(130, 36),
                Location = new Point(190, 4),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular)
            };
            _btnToggleServer.FlatAppearance.BorderSize = 0;
            _btnToggleServer.Click += (s, e) =>
            {
                if (_engine.IsRunning) _engine.Stop();
                else _engine.Start();
                UpdateUiFromState();
            };

            pnl.Controls.Add(_btnMinimizeTray);
            pnl.Controls.Add(_btnToggleServer);
            return pnl;
        }

        private Panel CreateRoundedCard(int width, int height)
        {
            Panel card = new Panel
            {
                Size = new Size(width, height),
                BackColor = ColorCard
            };
            card.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, card.ClientRectangle, ColorBorder, ButtonBorderStyle.Solid);
            };
            return card;
        }

        private void SetupTrayIcon()
        {
            _trayMenu = new ContextMenuStrip();
            _trayMenu.Items.Add("Open AirGlide", null, (s, e) => RestoreWindow());
            _trayMenu.Items.Add("-");
            _trayMenu.Items.Add("Exit", null, (s, e) =>
            {
                _engine.Stop();
                Application.Exit();
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

        private void RestoreWindow()
        {
            this.Show();
            this.WindowState = FormWindowState.Normal;
            this.ShowInTaskbar = true;
            this.BringToFront();
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
                _lblStatusDot.ForeColor = ColorGreen;
                _lblStatusText.Text = $"READY ({_engine.Port})";
                _btnToggleServer.Text = "Pause Receiver";
            }
            else
            {
                _lblStatusDot.ForeColor = ColorRed;
                _lblStatusText.Text = "PAUSED";
                _btnToggleServer.Text = "Resume Receiver";
            }

            _lblIpAddress.Text = $"{_engine.LocalIp}:{_engine.Port}";

            if (_engine.ActiveDeviceId != null && _engine.Devices.TryGetValue(_engine.ActiveDeviceId, out var dev))
            {
                _lblActiveDevice.Text = $"🟢 Connected: {dev.Name}";
                _lblActiveDevice.ForeColor = ColorGreen;
                _lblActiveSub.Text = $"{dev.PacketCount} packets received • Phone IP: {dev.IpAddress}";
                _btnDisconnectPhone.Visible = true;
            }
            else if (_engine.Devices.Count > 0)
            {
                var first = _engine.Devices.Values.First();
                _lblActiveDevice.Text = $"🟢 Connected: {first.Name}";
                _lblActiveDevice.ForeColor = ColorGreen;
                _lblActiveSub.Text = $"{_engine.TotalPackets} packets received • Phone IP: {first.IpAddress}";
                _btnDisconnectPhone.Visible = true;
            }
            else
            {
                _lblActiveDevice.Text = "Waiting for phone to connect...";
                _lblActiveDevice.ForeColor = ColorTextPrimary;
                _lblActiveSub.Text = "Open AirGlide on your phone. It connects automatically over Wi-Fi.";
                _btnDisconnectPhone.Visible = false;
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
            if (_lstLogs.Items.Count > 200)
            {
                _lstLogs.Items.RemoveAt(_lstLogs.Items.Count - 1);
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            if (e.CloseReason == CloseReason.UserClosing)
            {
                e.Cancel = true;
                this.Hide();
                _trayIcon.ShowBalloonTip(1500, "AirGlide Remote", "Running in background. Double-click tray icon to reopen.", ToolTipIcon.Info);
                return;
            }
            _uiRefreshTimer?.Stop();
            _engine?.Stop();
            _trayIcon?.Dispose();
            base.OnFormClosing(e);
        }
    }
}
