using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
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

    public class UdpReceiverEngine
    {
        public const int DefaultPort = 5055;
        public const int DiscoveryPort = 5056;

        private UdpClient? _listener;
        private UdpClient? _beaconSender;
        private CancellationTokenSource? _cts;
        private Thread? _listenThread;

        public int Port { get; private set; } = DefaultPort;
        public bool IsRunning { get; private set; } = false;
        public string LocalIp { get; private set; } = "127.0.0.1";
        public string HostName { get; private set; } = Environment.MachineName;

        public long TotalPackets { get; private set; } = 0;
        public string? ActiveDeviceId { get; set; } = null;
        public ConcurrentDictionary<string, ConnectedDevice> Devices { get; } = new();

        public event Action<string>? OnLog;
        public event Action<string, string>? OnPacketProcessed;
        public event Action<ConnectedDevice>? OnDeviceUpdated;
        public event Action<bool>? OnRunningStateChanged;

        public UdpReceiverEngine(int port = DefaultPort)
        {
            Port = port;
            LocalIp = DetectBestLocalIp();
        }

        public void Start()
        {
            if (IsRunning) return;

            try
            {
                LocalIp = DetectBestLocalIp();
                _cts = new CancellationTokenSource();

                _listener = new UdpClient();
                _listener.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, false);
                _listener.Client.Bind(new IPEndPoint(IPAddress.Any, Port));

                _beaconSender = new UdpClient();
                _beaconSender.EnableBroadcast = true;

                IsRunning = true;
                OnRunningStateChanged?.Invoke(true);
                OnLog?.Invoke($"Server listening on UDP port {Port} (IP: {LocalIp})");

                _listenThread = new Thread(ListenLoop)
                {
                    IsBackground = true,
                    Name = "AirGlideListenThread"
                };
                _listenThread.Start();

                Task.Run(() => BeaconBroadcastLoop(_cts.Token));
            }
            catch (Exception ex)
            {
                IsRunning = false;
                OnRunningStateChanged?.Invoke(false);
                OnLog?.Invoke($"Failed to bind to port {Port}: {ex.Message}");
            }
        }

        public void Stop()
        {
            if (!IsRunning) return;

            IsRunning = false;
            _cts?.Cancel();

            try
            {
                _listener?.Close();
                _listener?.Dispose();
            }
            catch { }

            try
            {
                _beaconSender?.Close();
                _beaconSender?.Dispose();
            }
            catch { }

            _listener = null;
            _beaconSender = null;
            OnRunningStateChanged?.Invoke(false);
            OnLog?.Invoke("Server stopped.");
        }

        private void ListenLoop()
        {
            IPEndPoint remoteEp = new IPEndPoint(IPAddress.Any, 0);

            while (IsRunning && _listener != null)
            {
                try
                {
                    byte[] bytes = _listener.Receive(ref remoteEp);
                    string rawMsg = Encoding.UTF8.GetString(bytes).Trim();
                    TotalPackets++;

                    ProcessPacket(rawMsg, remoteEp);
                }
                catch (SocketException)
                {
                    if (!IsRunning) break;
                }
                catch (Exception ex)
                {
                    if (IsRunning)
                    {
                        OnLog?.Invoke($"Error processing packet: {ex.Message}");
                    }
                }
            }
        }

        private void ProcessPacket(string msg, IPEndPoint senderEp)
        {
            if (string.IsNullOrWhiteSpace(msg)) return;

            // Handle Auto-Discovery Ping from mobile client
            if (msg.StartsWith("AIRGLIDE_DISCOVERY_PING", StringComparison.OrdinalIgnoreCase))
            {
                string offer = $"AIRGLIDE_SERVER_OFFER:{HostName}:{LocalIp}:{Port}";
                byte[] offerBytes = Encoding.UTF8.GetBytes(offer);
                _beaconSender?.Send(offerBytes, offerBytes.Length, senderEp);
                OnLog?.Invoke($"Discovery ping from {senderEp.Address} -> Replied with server offer");
                return;
            }

            // Wire format: CMD:PAYLOAD:DEVICE_ID
            int firstColon = msg.IndexOf(':');
            if (firstColon <= 0) return;

            string cmd = msg.Substring(0, firstColon);
            string remainder = msg.Substring(firstColon + 1);

            string payload;
            string devId;

            if (cmd == "P")
            {
                // P:deviceId:deviceName
                string[] parts = remainder.Split(':');
                devId = parts.Length > 0 ? parts[0] : "unknown";
                string devName = parts.Length > 1 ? parts[1] : "Phone";

                var dev = Devices.GetOrAdd(devId, id => new ConnectedDevice
                {
                    Id = id,
                    Name = devName,
                    IpAddress = senderEp.Address.ToString()
                });
                dev.Name = devName;
                dev.LastSeen = DateTime.Now;
                dev.PacketCount++;
                dev.IpAddress = senderEp.Address.ToString();

                ActiveDeviceId = devId;
                OnLog?.Invoke($"Connected controller: {dev.Name} ({dev.Id})");
                OnDeviceUpdated?.Invoke(dev);
                return;
            }

            int lastColon = remainder.LastIndexOf(':');
            if (lastColon >= 0)
            {
                payload = remainder.Substring(0, lastColon);
                devId = remainder.Substring(lastColon + 1);
            }
            else
            {
                payload = remainder;
                devId = "unknown";
            }

            // Update Device presence & packet stats
            var currentDev = Devices.GetOrAdd(devId, id => new ConnectedDevice
            {
                Id = id,
                Name = $"Phone ({senderEp.Address})",
                IpAddress = senderEp.Address.ToString()
            });

            currentDev.LastSeen = DateTime.Now;
            currentDev.PacketCount++;
            currentDev.IpAddress = senderEp.Address.ToString();
            ActiveDeviceId = devId;
            OnDeviceUpdated?.Invoke(currentDev);

            // Execute input immediately without dropping packets

            // Execute input
            switch (cmd)
            {
                case "M":
                    // Move M:dx,dy
                    string[] coords = payload.Split(',');
                    if (coords.Length == 2)
                    {
                        if (float.TryParse(coords[0], System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out float dx) &&
                            float.TryParse(coords[1], System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out float dy))
                        {
                            NativeInput.Move((int)Math.Round(dx), (int)Math.Round(dy));
                            OnPacketProcessed?.Invoke("MOVE", $"{dx:+0.0;-0.0},{dy:+0.0;-0.0}");
                        }
                    }
                    break;

                case "C":
                    // Click C:L or C:R or C:M
                    if (payload == "R") NativeInput.RightClick();
                    else if (payload == "M") NativeInput.MiddleClick();
                    else NativeInput.LeftClick();
                    OnPacketProcessed?.Invoke("CLICK", payload);
                    break;

                case "D":
                    // MouseDown
                    NativeInput.MouseDown(payload);
                    OnPacketProcessed?.Invoke("DOWN", payload);
                    break;

                case "U":
                    // MouseUp
                    NativeInput.MouseUp(payload);
                    OnPacketProcessed?.Invoke("UP", payload);
                    break;

                case "S":
                    // Scroll S:dy
                    if (float.TryParse(payload, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out float scrollDelta))
                    {
                        int intScroll = (int)Math.Round(scrollDelta);
                        if (intScroll != 0)
                        {
                            NativeInput.Scroll(intScroll);
                            OnPacketProcessed?.Invoke("SCROLL", intScroll.ToString());
                        }
                    }
                    break;

                case "K":
                    // Key K:KEYNAME
                    NativeInput.KeyPress(payload);
                    OnPacketProcessed?.Invoke("KEY", payload);
                    break;

                case "T":
                    // Text T:content
                    string textContent = payload.Replace("\\:", ":");
                    NativeInput.TypeText(textContent);
                    OnPacketProcessed?.Invoke("TEXT", textContent);
                    break;
            }
        }

        private async Task BeaconBroadcastLoop(CancellationToken token)
        {
            // Broadcast beacon every 1.5 seconds on the local broadcast address
            while (!token.IsCancellationRequested && IsRunning && _beaconSender != null)
            {
                try
                {
                    string beaconMsg = $"AIRGLIDE_BEACON:{HostName}:{LocalIp}:{Port}";
                    byte[] bytes = Encoding.UTF8.GetBytes(beaconMsg);

                    // Send to standard port and discovery port
                    _beaconSender.Send(bytes, bytes.Length, new IPEndPoint(IPAddress.Broadcast, Port));
                    _beaconSender.Send(bytes, bytes.Length, new IPEndPoint(IPAddress.Broadcast, DiscoveryPort));
                }
                catch { }

                await Task.Delay(1500, token);
            }
        }

        public static string DetectBestLocalIp()
        {
            try
            {
                foreach (NetworkInterface ni in NetworkInterface.GetAllNetworkInterfaces())
                {
                    if (ni.OperationalStatus != OperationalStatus.Up ||
                        ni.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                        continue;

                    var ipProps = ni.GetIPProperties();
                    foreach (UnicastIPAddressInformation addr in ipProps.UnicastAddresses)
                    {
                        if (addr.Address.AddressFamily == AddressFamily.InterNetwork &&
                            !IPAddress.IsLoopback(addr.Address))
                        {
                            string ipStr = addr.Address.ToString();
                            if (!ipStr.StartsWith("169.254")) // Ignore link-local
                            {
                                return ipStr;
                            }
                        }
                    }
                }
            }
            catch { }
            return "127.0.0.1";
        }
    }
}
