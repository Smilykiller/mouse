using System;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    public class MainForm : Form
    {
        private readonly UdpReceiverEngine _engine;
        private NotifyIcon _trayIcon = null!;
        private ContextMenuStrip _trayMenu = null!;

        // Controls
        private Label _lblStatusDot = null!;
        private Label _lblStatusText = null!;
        private Label _lblIpAddress = null!;
        private Label _lblPort = null!;
        private Label _lblActiveDevice = null!;
        private Label _lblPacketCount = null!;
        private ListBox _lstLogs = null!;
        private Button _btnToggleServer = null!;
        private Button _btnCopyIp = null!;
        private Button _btnMinimizeTray = null!;
        private System.Windows.Forms.Timer _uiRefreshTimer = null!;

        // Colors
        private readonly Color ColorBg = Color.FromArgb(9, 9, 11);
        private readonly Color ColorCard = Color.FromArgb(24, 24, 27);
        private readonly Color ColorBorder = Color.FromArgb(46, 46, 51);
        private readonly Color ColorTextPrimary = Color.FromArgb(250, 250, 250);
        private readonly Color ColorTextSecondary = Color.FromArgb(161, 161, 170);
        private readonly Color ColorTextMuted = Color.FromArgb(113, 113, 122);
        private readonly Color ColorGreen = Color.FromArgb(34, 197, 94);
        private readonly Color ColorRed = Color.FromArgb(239, 68, 68);

        public MainForm()
        {
            _engine = new UdpReceiverEngine(5055);

            InitializeComponent();
            SetupTrayIcon();

            _engine.OnLog += msg =>
            {
                Console.WriteLine($"[SYS] {msg}");
                AppendLogSafe($"[SYS] {msg}");
            };
            _engine.OnPacketProcessed += (cmd, detail) =>
            {
                if (cmd != "MOVE") // Don't spam the console / UI list with 120Hz moves
                {
                    Console.WriteLine($"[{cmd}] {detail}");
                    AppendLogSafe($"[{cmd}] {detail}");
                }
            };

            _engine.Start();
            UpdateUiFromState();
        }

        private void InitializeComponent()
        {
            this.Text = "AirGlide Desktop Receiver";
            this.Size = new Size(640, 560);
            this.MinimumSize = new Size(540, 480);
            this.BackColor = ColorBg;
            this.ForeColor = ColorTextPrimary;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);

            // Main Layout Container
            TableLayoutPanel mainPanel = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                RowCount = 5,
                ColumnCount = 1,
                Padding = new Padding(16),
                BackColor = ColorBg
            };
            mainPanel.RowStyles.Add(new RowStyle(SizeType.Absolute, 55f)); // Header
            mainPanel.RowStyles.Add(new RowStyle(SizeType.Absolute, 125f)); // IP Info Card
            mainPanel.RowStyles.Add(new RowStyle(SizeType.Absolute, 90f)); // Controller Card
            mainPanel.RowStyles.Add(new RowStyle(SizeType.Percent, 100f)); // Logs
            mainPanel.RowStyles.Add(new RowStyle(SizeType.Absolute, 50f)); // Action Buttons

            // 1. Header Row
            Panel pnlHeader = new Panel { Dock = DockStyle.Fill };
            Label lblTitle = new Label
            {
                Text = "AirGlide Desktop",
                Font = new Font("Segoe UI", 16f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                AutoSize = true,
                Location = new Point(0, 8)
            };

            Panel pnlStatusBadge = new Panel
            {
                Size = new Size(180, 32),
                Location = new Point(410, 10),
                Anchor = AnchorStyles.Top | AnchorStyles.Right,
                BackColor = ColorCard
            };
            pnlStatusBadge.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, pnlStatusBadge.ClientRectangle, ColorBorder, ButtonBorderStyle.Solid);
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
                Size = new Size(145, 20),
                Location = new Point(28, 7)
            };
            pnlStatusBadge.Controls.Add(_lblStatusDot);
            pnlStatusBadge.Controls.Add(_lblStatusText);

            pnlHeader.Controls.Add(lblTitle);
            pnlHeader.Controls.Add(pnlStatusBadge);
            mainPanel.Controls.Add(pnlHeader, 0, 0);

            // 2. IP Card Panel
            Panel pnlIpCard = CreateCardPanel();
            pnlIpCard.Height = 115;

            Label lblIpHeader = new Label
            {
                Text = "CONNECT MOBILE APP TO THIS LAPTOP",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = ColorTextMuted,
                Location = new Point(16, 12),
                AutoSize = true
            };

            _lblIpAddress = new Label
            {
                Text = _engine.LocalIp,
                Font = new Font("Consolas", 22f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                Location = new Point(14, 30),
                AutoSize = true
            };

            _lblPort = new Label
            {
                Text = $":{_engine.Port}",
                Font = new Font("Consolas", 16f, FontStyle.Regular),
                ForeColor = ColorTextSecondary,
                Location = new Point(260, 36),
                AutoSize = true
            };

            _btnCopyIp = new Button
            {
                Text = "Copy IP",
                Size = new Size(80, 32),
                Location = new Point(360, 33),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat
            };
            _btnCopyIp.FlatAppearance.BorderSize = 0;
            _btnCopyIp.Click += (s, e) =>
            {
                Clipboard.SetText(_engine.LocalIp);
                MessageBox.Show(this, $"Copied IP ({_engine.LocalIp}) to clipboard!", "AirGlide", MessageBoxButtons.OK, MessageBoxIcon.Information);
            };

            Label lblBeaconNotice = new Label
            {
                Text = "✓ Auto-Discovery active: mobile app on the same Wi-Fi will find this laptop automatically.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = ColorGreen,
                Location = new Point(16, 80),
                AutoSize = true
            };

            pnlIpCard.Controls.Add(lblIpHeader);
            pnlIpCard.Controls.Add(_lblIpAddress);
            pnlIpCard.Controls.Add(_lblPort);
            pnlIpCard.Controls.Add(_btnCopyIp);
            pnlIpCard.Controls.Add(lblBeaconNotice);
            mainPanel.Controls.Add(pnlIpCard, 0, 1);

            // 3. Controller Device Card
            Panel pnlDeviceCard = CreateCardPanel();
            pnlDeviceCard.Height = 80;

            Label lblDevTitle = new Label
            {
                Text = "ACTIVE CONTROLLER",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = ColorTextMuted,
                Location = new Point(16, 10),
                AutoSize = true
            };

            _lblActiveDevice = new Label
            {
                Text = "Waiting for mobile connection...",
                Font = new Font("Segoe UI", 11f, FontStyle.Bold),
                ForeColor = ColorTextPrimary,
                Location = new Point(15, 28),
                AutoSize = true
            };

            _lblPacketCount = new Label
            {
                Text = "0 packets received",
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                ForeColor = ColorTextSecondary,
                Location = new Point(16, 52),
                AutoSize = true
            };

            pnlDeviceCard.Controls.Add(lblDevTitle);
            pnlDeviceCard.Controls.Add(_lblActiveDevice);
            pnlDeviceCard.Controls.Add(_lblPacketCount);
            mainPanel.Controls.Add(pnlDeviceCard, 0, 2);

            // 4. Log Panel
            _lstLogs = new ListBox
            {
                Dock = DockStyle.Fill,
                BackColor = ColorCard,
                ForeColor = ColorTextSecondary,
                BorderStyle = BorderStyle.None,
                Font = new Font("Consolas", 9f, FontStyle.Regular),
                IntegralHeight = false
            };
            mainPanel.Controls.Add(_lstLogs, 0, 3);

            // 5. Bottom Action Buttons
            Panel pnlActions = new Panel { Dock = DockStyle.Fill };

            _btnToggleServer = new Button
            {
                Text = "Stop Server",
                Size = new Size(120, 38),
                Location = new Point(0, 6),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat
            };
            _btnToggleServer.FlatAppearance.BorderSize = 0;
            _btnToggleServer.Click += (s, e) =>
            {
                if (_engine.IsRunning) _engine.Stop();
                else _engine.Start();
                UpdateUiFromState();
            };

            _btnMinimizeTray = new Button
            {
                Text = "Minimize to System Tray",
                Size = new Size(180, 38),
                Location = new Point(130, 6),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat
            };
            _btnMinimizeTray.FlatAppearance.BorderSize = 0;
            _btnMinimizeTray.Click += (s, e) =>
            {
                this.Hide();
                _trayIcon.ShowBalloonTip(2000, "AirGlide Running", "AirGlide is running in the background. Double-click icon to reopen.", ToolTipIcon.Info);
            };

            Button btnClearLog = new Button
            {
                Text = "Clear Log",
                Size = new Size(90, 38),
                Location = new Point(320, 6),
                BackColor = ColorBorder,
                ForeColor = ColorTextPrimary,
                FlatStyle = FlatStyle.Flat
            };
            btnClearLog.FlatAppearance.BorderSize = 0;
            btnClearLog.Click += (s, e) => _lstLogs.Items.Clear();

            Button btnRelease = new Button
            {
                Text = "Disconnect Phone",
                Size = new Size(130, 38),
                Location = new Point(420, 6),
                BackColor = Color.FromArgb(45, 20, 20),
                ForeColor = Color.FromArgb(248, 113, 113),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Bold)
            };
            btnRelease.FlatAppearance.BorderSize = 1;
            btnRelease.FlatAppearance.BorderColor = Color.FromArgb(127, 29, 29);
            btnRelease.Click += (s, e) =>
            {
                _engine.ReleaseController();
                UpdateUiFromState();
            };

            pnlActions.Controls.Add(_btnToggleServer);
            pnlActions.Controls.Add(_btnMinimizeTray);
            pnlActions.Controls.Add(btnClearLog);
            pnlActions.Controls.Add(btnRelease);
            mainPanel.Controls.Add(pnlActions, 0, 4);

            this.Controls.Add(mainPanel);

            // Timer to refresh device stats smoothly
            _uiRefreshTimer = new System.Windows.Forms.Timer { Interval = 800 };
            _uiRefreshTimer.Tick += (s, e) => UpdateUiFromState();
            _uiRefreshTimer.Start();
        }

        private Panel CreateCardPanel()
        {
            Panel pnl = new Panel
            {
                Dock = DockStyle.Fill,
                BackColor = ColorCard
            };
            pnl.Paint += (s, e) =>
            {
                ControlPaint.DrawBorder(e.Graphics, pnl.ClientRectangle, ColorBorder, ButtonBorderStyle.Solid);
            };
            return pnl;
        }

        private void SetupTrayIcon()
        {
            _trayMenu = new ContextMenuStrip();
            _trayMenu.Items.Add("Open AirGlide", null, (s, e) =>
            {
                this.Show();
                this.WindowState = FormWindowState.Normal;
                this.BringToFront();
            });
            _trayMenu.Items.Add("-");
            _trayMenu.Items.Add("Exit", null, (s, e) =>
            {
                _engine.Stop();
                Application.Exit();
            });

            _trayIcon = new NotifyIcon
            {
                Text = "AirGlide Desktop Receiver",
                Icon = SystemIcons.Application,
                ContextMenuStrip = _trayMenu,
                Visible = true
            };
            _trayIcon.DoubleClick += (s, e) =>
            {
                this.Show();
                this.WindowState = FormWindowState.Normal;
                this.BringToFront();
            };
        }

        private void UpdateUiFromState()
        {
            if (this.InvokeRequired)
            {
                if (!this.IsHandleCreated || this.IsDisposed) return;
                try { this.BeginInvoke(new Action(UpdateUiFromState)); } catch (InvalidOperationException) { }
                return;
            }

            if (_engine.IsRunning)
            {
                _lblStatusDot.ForeColor = ColorGreen;
                _lblStatusText.Text = $"LISTENING ({_engine.Port})";
                _btnToggleServer.Text = "Stop Server";
            }
            else
            {
                _lblStatusDot.ForeColor = ColorRed;
                _lblStatusText.Text = "STOPPED";
                _btnToggleServer.Text = "Start Server";
            }

            _lblIpAddress.Text = _engine.LocalIp;
            _lblPort.Text = $":{_engine.Port}";

            if (_engine.ActiveDeviceId != null && _engine.Devices.TryGetValue(_engine.ActiveDeviceId, out var dev))
            {
                _lblActiveDevice.Text = $"Connected: {dev.Name}";
                _lblActiveDevice.ForeColor = ColorGreen;
                _lblPacketCount.Text = $"{dev.PacketCount} packets received • IP: {dev.IpAddress}";
            }
            else if (_engine.Devices.Count > 0)
            {
                var firstDev = _engine.Devices.Values.First();
                _lblActiveDevice.Text = $"Connected: {firstDev.Name}";
                _lblActiveDevice.ForeColor = ColorGreen;
                _lblPacketCount.Text = $"{_engine.TotalPackets} packets received • IP: {firstDev.IpAddress}";
            }
            else
            {
                _lblActiveDevice.Text = "Waiting for mobile connection...";
                _lblActiveDevice.ForeColor = ColorTextSecondary;
                _lblPacketCount.Text = $"{_engine.TotalPackets} packets received";
            }
        }

        private void AppendLogSafe(string text)
        {
            if (this.IsDisposed) return;

            if (_lstLogs.InvokeRequired)
            {
                if (!_lstLogs.IsHandleCreated) return;
                try { _lstLogs.BeginInvoke(new Action(() => AppendLogSafe(text))); } catch (InvalidOperationException) { }
                return;
            }

            string line = $"{DateTime.Now:HH:mm:ss} {text}";
            _lstLogs.Items.Insert(0, line);
            if (_lstLogs.Items.Count > 60)
            {
                _lstLogs.Items.RemoveAt(_lstLogs.Items.Count - 1);
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            if (e.CloseReason == CloseReason.UserClosing)
            {
                // Minimize to tray instead of quitting
                e.Cancel = true;
                this.Hide();
                _trayIcon.ShowBalloonTip(1500, "AirGlide", "Still running in tray. Right-click tray icon to Exit.", ToolTipIcon.Info);
            }
            else
            {
                _engine.Stop();
                _trayIcon.Visible = false;
                _trayIcon.Dispose();
                base.OnFormClosing(e);
            }
        }
    }
}
