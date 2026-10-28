using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Globalization;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace AirGlideDesktop
{
    public class ConnectedDevice
    {
        public string Id { get; set; } = "";
        public string Name { get; set; } = "";
        public string IpAddress { get; set; } = "";
        public DateTime LastSeen { get; set; } = DateTime.Now;
        public long PacketCount { get; set; } = 0;
    }

    /// <summary>
    /// Receives UDP packets from the AirGlide phone app and turns them into mouse / keyboard input.
    ///
    /// Fixes compared to the old engine:
    ///  * The beacon is sent to the DISCOVERY port (5056) only. It used to be sent to our own port
    ///    5055, so we received our own beacon and treated it as a phone.
    ///  * Anything starting with "AIRGLIDE_" is discovery traffic, never input.
    ///  * Beacons go out of every real network adapter (not just one via 255.255.255.255).
    ///  * The advertised IP is the adapter with the default route, not the first (often virtual) adapter.
    ///  * Presence packets are answered with AIRGLIDE_ACK so the phone can show a truthful "Live" state.
    ///  * One phone controls the PC at a time; the lock is released after 10 s of silence.
    /// </summary>
    public class UdpReceiverEngine
    {
        public const int DefaultPort = 5055;
        public const int DiscoveryPort = 5056;
        private const double ControllerTimeoutSeconds = 10.0;

        private UdpClient? _listener;
        private CancellationTokenSource? _cts;
        private Thread? _listenThread;
        private readonly object _lock = new object();
        private DateTime _lastSeenUtc = DateTime.MinValue;
        private DateTime _lastBlockedLogUtc = DateTime.MinValue;

        private string _localIp = "127.0.0.1";
        private DateTime _localIpAtUtc = DateTime.MinValue;

        public int Port { get; private set; } = DefaultPort;
        public bool IsRunning { get; private set; } = false;
        public string HostName { get; private set; }

        public string LocalIp
        {
            get
            {
                if ((DateTime.UtcNow - _localIpAtUtc).TotalSeconds > 5)
                {
                    _localIp = DetectBestLocalIp(null);
                    _localIpAtUtc = DateTime.UtcNow;
                }
                return _localIp;
            }
        }

        public long TotalPackets { get; private set; } = 0;
        public long BlockedPackets { get; private set; } = 0;
        public string? ActiveDeviceId { get; private set; } = null;
        public ConcurrentDictionary<string, ConnectedDevice> Devices { get; } = new ConcurrentDictionary<string, ConnectedDevice>();

        public event Action<string>? OnLog;
        public event Action<string, string>? OnPacketProcessed;
        public event Action<ConnectedDevice>? OnDeviceUpdated;
        public event Action<bool>? OnRunningStateChanged;

        public UdpReceiverEngine(int port = DefaultPort)
        {
            Port = port;
            HostName = Environment.MachineName.Replace(":", "-");
        }

        public void Start()
        {
            if (IsRunning) return;

            try
            {
                _cts = new CancellationTokenSource();

                _listener = new UdpClient(AddressFamily.InterNetwork);
                _listener.Client.Bind(new IPEndPoint(IPAddress.Any, Port));
                try
                {
                    // Windows: stop "connection reset" errors when a phone closes its socket.
                    const int SIO_UDP_CONNRESET = -1744830452;
                    _listener.Client.IOControl((IOControlCode)SIO_UDP_CONNRESET, new byte[] { 0, 0, 0, 0 }, null);
                }
                catch { }

                IsRunning = true;
                OnRunningStateChanged?.Invoke(true);
                OnLog?.Invoke($"Listening on UDP port {Port} (laptop IP {LocalIp})");

                _listenThread = new Thread(ListenLoop)
                {
                    IsBackground = true,
                    Name = "AirGlideListenThread"
                };
                _listenThread.Start();

                CancellationToken token = _cts.Token;
                Task.Run(() => BeaconLoop(token));
            }
            catch (Exception ex)
            {
                IsRunning = false;
                try { _listener?.Close(); } catch { }
                _listener = null;
                OnRunningStateChanged?.Invoke(false);
                OnLog?.Invoke($"Cannot listen on port {Port}: {ex.Message} (is another AirGlide receiver already running?)");
            }
        }

        public void Stop()
        {
            if (!IsRunning) return;

            IsRunning = false;
            try { _cts?.Cancel(); } catch { }
            try { _listener?.Close(); } catch { }
            _listener = null;

            OnRunningStateChanged?.Invoke(false);
            OnLog?.Invoke("Receiver stopped.");
        }

        /// <summary>Lets a different phone take over immediately.</summary>
        public void ReleaseController()
        {
            lock (_lock)
            {
                ActiveDeviceId = null;
                Devices.Clear();
            }
            OnLog?.Invoke("Phone released - the next phone that connects takes control.");
        }

        // ------------------------------------------------------------------ receive

        private void ListenLoop()
        {
            NativeInput.EnsureInputDesktop();
            while (IsRunning)
            {
                UdpClient? listener = _listener;
                if (listener == null) break;

                try
                {
                    IPEndPoint remote = new IPEndPoint(IPAddress.Any, 0);
                    byte[] bytes = listener.Receive(ref remote);
                    HandleDatagram(Encoding.UTF8.GetString(bytes), remote);
                }
                catch (SocketException)
                {
                    if (!IsRunning) break;
                }
                catch (ObjectDisposedException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    if (IsRunning) OnLog?.Invoke($"Error handling packet: {ex.Message}");
                }
            }
        }

        private void HandleDatagram(string rawMsg, IPEndPoint sender)
        {
            string msg = rawMsg.Trim();
            if (msg.Length == 0) return;

            if (msg.StartsWith("AIRGLIDE_DISCOVERY_PING", StringComparison.Ordinal))
            {
                string ip = DetectBestLocalIp(sender.Address.ToString());
                Reply($"AIRGLIDE_SERVER_OFFER:{HostName}:{ip}:{Port}", sender);
                OnLog?.Invoke($"Discovery ping from {sender.Address} answered");
                return;
            }

            if (!TryParse(msg, out string cmd, out string payload, out string devId, out string name)) return;
            TotalPackets++;

            if (!Accept(devId, sender.Address.ToString(), name)) return;

            if (cmd == "P")
            {
                Reply($"AIRGLIDE_ACK:{HostName}:{Port}", sender);
                return;
            }

            if (cmd == "X")
            {
                ReleaseController();
                Reply($"AIRGLIDE_DISCONNECTED:{HostName}", sender);
                OnLog?.Invoke($"Phone disconnected ({sender.Address})");
                return;
            }

            Dispatch(cmd, payload);
        }

        private static bool TryParse(string msg, out string cmd, out string payload, out string devId, out string name)
        {
            cmd = "";
            payload = "";
            devId = "";
            name = "";

            // Discovery traffic is never input (this was the bug: our own beacon looked like a phone).
            if (msg.StartsWith("AIRGLIDE_", StringComparison.Ordinal)) return false;

            int first = msg.IndexOf(':');
            if (first != 1) return false;                 // commands are exactly one letter
            cmd = msg.Substring(0, 1);
            if ("MCDUSKTPX".IndexOf(cmd[0]) < 0) return false;

            string rest = msg.Substring(first + 1);

            if (cmd == "P")
            {
                int c = rest.IndexOf(':');
                devId = c < 0 ? rest : rest.Substring(0, c);
                name = c < 0 ? "Phone" : rest.Substring(c + 1);
                if (name.Length == 0) name = "Phone";
                return devId.Length > 0;
            }

            int last = rest.LastIndexOf(':');
            if (last < 0) return false;
            payload = rest.Substring(0, last);
            devId = rest.Substring(last + 1);
            return devId.Length > 0;
        }

        private bool Accept(string devId, string ip, string name)
        {
            ConnectedDevice? updated = null;
            string? logLine = null;
            bool accepted = false;
            DateTime now = DateTime.UtcNow;

            lock (_lock)
            {
                bool free = ActiveDeviceId == null || (now - _lastSeenUtc).TotalSeconds > ControllerTimeoutSeconds;
                if (free || devId == ActiveDeviceId)
                {
                    if (devId != ActiveDeviceId)
                    {
                        ActiveDeviceId = devId;
                        Devices.Clear();
                    }
                    _lastSeenUtc = now;

                    ConnectedDevice dev = Devices.GetOrAdd(devId, id => new ConnectedDevice
                    {
                        Id = id,
                        Name = $"Phone ({ip})",
                        IpAddress = ip
                    });
                    dev.IpAddress = ip;
                    dev.LastSeen = DateTime.Now;
                    dev.PacketCount++;
                    if (name.Length > 0 && dev.Name != name)
                    {
                        dev.Name = name;
                        logLine = $"Phone connected: {name} ({ip})";
                    }
                    updated = dev;
                    accepted = true;
                }
                else
                {
                    BlockedPackets++;
                    if ((now - _lastBlockedLogUtc).TotalSeconds > 5)
                    {
                        _lastBlockedLogUtc = now;
                        logLine = $"Ignored phone {devId} ({ip}): locked to another phone. Press 'Release Phone' to switch.";
                    }
                }
            }

            if (logLine != null) OnLog?.Invoke(logLine);
            if (updated != null) OnDeviceUpdated?.Invoke(updated);
            return accepted;
        }

        private void Reply(string text, IPEndPoint target)
        {
            try
            {
                byte[] bytes = Encoding.UTF8.GetBytes(text);
                _listener?.Send(bytes, bytes.Length, target);
            }
            catch { }
        }

        private static bool TryNumber(string s, out double value)
        {
            return double.TryParse(s, NumberStyles.Float, CultureInfo.InvariantCulture, out value);
        }

        private static string UnescapeText(string s)
        {
            StringBuilder sb = new StringBuilder(s.Length);
            for (int i = 0; i < s.Length; i++)
            {
                char c = s[i];
                if (c == '\\' && i + 1 < s.Length)
                {
                    i++;
                    sb.Append(s[i] == 'n' ? '\n' : s[i]);
                }
                else
                {
                    sb.Append(c);
                }
            }
            return sb.ToString();
        }

        private void Dispatch(string cmd, string payload)
        {
            switch (cmd)
            {
                case "M":
                {
                    string[] xy = payload.Split(',');
                    if (xy.Length == 2 && TryNumber(xy[0], out double dx) && TryNumber(xy[1], out double dy))
                    {
                        NativeInput.Move((int)Math.Round(dx), (int)Math.Round(dy));
                        OnPacketProcessed?.Invoke("MOVE", $"{dx},{dy}");
                    }
                    break;
                }
                case "C":
                    if (payload == "R") NativeInput.RightClick();
                    else if (payload == "M") NativeInput.MiddleClick();
                    else NativeInput.LeftClick();
                    OnPacketProcessed?.Invoke("CLICK", payload);
                    break;
                case "D":
                    NativeInput.MouseDown(payload);
                    OnPacketProcessed?.Invoke("DOWN", payload);
                    break;
                case "U":
                    NativeInput.MouseUp(payload);
                    OnPacketProcessed?.Invoke("UP", payload);
                    break;
                case "S":
                {
                    if (TryNumber(payload, out double scroll))
                    {
                        int notches = (int)Math.Round(scroll);
                        if (notches != 0)
                        {
                            NativeInput.Scroll(notches);
                            OnPacketProcessed?.Invoke("SCROLL", notches.ToString());
                        }
                    }
                    break;
                }
                case "K":
                    NativeInput.KeyPress(payload);
                    OnPacketProcessed?.Invoke("KEY", payload);
                    break;
                case "T":
                {
                    string text = UnescapeText(payload);
                    NativeInput.TypeText(text);
                    OnPacketProcessed?.Invoke("TEXT", text);
                    break;
                }
            }
        }

        // ------------------------------------------------------------------ beacon

        private async Task BeaconLoop(CancellationToken token)
        {
            while (!token.IsCancellationRequested)
            {
                try { SendBeacons(); } catch { }

                try { await Task.Delay(1500, token); }
                catch (OperationCanceledException) { break; }
            }
        }

        private void SendBeacons()
        {
            foreach ((IPAddress ip, IPAddress mask) in GetLanAddresses())
            {
                try
                {
                    byte[] bytes = Encoding.UTF8.GetBytes($"AIRGLIDE_BEACON:{HostName}:{ip}:{Port}");

                    // Bind to this adapter's address so the packet really leaves through THIS network.
                    using (UdpClient udp = new UdpClient(new IPEndPoint(ip, 0)))
                    {
                        udp.EnableBroadcast = true;
                        // Discovery port only. Never our own listening port.
                        udp.Send(bytes, bytes.Length, new IPEndPoint(IPAddress.Broadcast, DiscoveryPort));
                        udp.Send(bytes, bytes.Length, new IPEndPoint(DirectedBroadcast(ip, mask), DiscoveryPort));
                    }
                }
                catch { }
            }
        }

        private static IPAddress DirectedBroadcast(IPAddress ip, IPAddress mask)
        {
            byte[] a = ip.GetAddressBytes();
            byte[] m = mask.GetAddressBytes();
            byte[] b = new byte[4];
            for (int i = 0; i < 4; i++)
            {
                b[i] = (byte)((a[i] | (~m[i] & 0xFF)) & 0xFF);
            }
            return new IPAddress(b);
        }

        private static List<(IPAddress Ip, IPAddress Mask)> GetLanAddresses()
        {
            List<(IPAddress Ip, IPAddress Mask)> list = new List<(IPAddress Ip, IPAddress Mask)>();
            try
            {
                foreach (NetworkInterface ni in NetworkInterface.GetAllNetworkInterfaces())
                {
                    if (ni.OperationalStatus != OperationalStatus.Up) continue;
                    if (ni.NetworkInterfaceType == NetworkInterfaceType.Loopback ||
                        ni.NetworkInterfaceType == NetworkInterfaceType.Tunnel) continue;

                    foreach (UnicastIPAddressInformation ua in ni.GetIPProperties().UnicastAddresses)
                    {
                        if (ua.Address.AddressFamily != AddressFamily.InterNetwork) continue;
                        if (IPAddress.IsLoopback(ua.Address)) continue;
                        if (ua.Address.ToString().StartsWith("169.254.", StringComparison.Ordinal)) continue;
                        if (ua.IPv4Mask == null) continue;
                        list.Add((ua.Address, ua.IPv4Mask));
                    }
                }
            }
            catch { }
            return list;
        }

        /// <summary>
        /// The laptop address that really reaches the network: the adapter with the default route
        /// (or the one that routes to <paramref name="peer"/>), NOT simply the first adapter.
        /// </summary>
        public static string DetectBestLocalIp(string? peer)
        {
            List<string> probes = new List<string>();
            if (!string.IsNullOrEmpty(peer)) probes.Add(peer!);
            probes.AddRange(new[] { "8.8.8.8", "192.168.0.1", "192.168.43.1", "10.0.0.1", "172.20.10.1" });

            foreach (string probe in probes)
            {
                try
                {
                    using (Socket s = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp))
                    {
                        s.Connect(IPAddress.Parse(probe), 9);   // UDP connect sends nothing; it only picks the route
                        if (s.LocalEndPoint is IPEndPoint ep)
                        {
                            string ip = ep.Address.ToString();
                            if (!ip.StartsWith("127.", StringComparison.Ordinal) &&
                                !ip.StartsWith("169.254.", StringComparison.Ordinal))
                            {
                                return ip;
                            }
                        }
                    }
                }
                catch { }
            }

            List<(IPAddress Ip, IPAddress Mask)> lan = GetLanAddresses();
            if (lan.Count > 0) return lan[0].Ip.ToString();
            return "127.0.0.1";
        }
    }
}
