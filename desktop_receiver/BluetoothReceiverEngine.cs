using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Ports;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth.Rfcomm;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;

namespace AirGlideDesktop
{
    /// <summary>
    /// Listens for incoming Bluetooth connections from the AirGlide mobile app.
    /// Supports both RFCOMM StreamSockets (AirGlide custom UUID + standard SPP UUID)
    /// and Windows Bluetooth Virtual Serial COM ports.
    /// Provides 100% zero-Wi-Fi wireless control offline, during travel, or with mobile hotspot off.
    /// </summary>
    public class BluetoothReceiverEngine
    {
        // Custom AirGlide dedicated RFCOMM UUID (never blocked or claimed by Windows OS drivers)
        public static readonly Guid AirGlideUuid = new Guid("4a9b0001-1234-4abc-8def-0123456789ab");

        // Standard Serial Port Profile (SPP) UUID
        public static readonly Guid SppUuid = new Guid("00001101-0000-1000-8000-00805F9B34FB");

        private readonly List<RfcommServiceProvider> _providers = new();
        private readonly List<StreamSocketListener> _listeners = new();
        private readonly List<SerialPort> _serialPorts = new();

        private StreamSocket? _activeSocket;
        private DataWriter? _writer;
        private CancellationTokenSource? _cts;

        public bool IsRunning { get; private set; } = false;
        public bool IsConnected { get; private set; } = false;
        public string ConnectedDeviceName { get; private set; } = "";

        public event Action<string>? OnLog;
        public event Action<string, string>? OnCommandReceived;
        public event Action<bool>? OnConnectionStateChanged;

        public async Task StartAsync()
        {
            if (IsRunning) return;

            _cts = new CancellationTokenSource();
            bool anyListenerStarted = false;

            // 1. Start Dedicated AirGlide RFCOMM Listener
            try
            {
                var airglideProvider = await RfcommServiceProvider.CreateAsync(RfcommServiceId.FromUuid(AirGlideUuid));
                SetSdpServiceName(airglideProvider, "AirGlide Remote (Direct)");

                var airglideListener = new StreamSocketListener();
                airglideListener.ConnectionReceived += OnSocketConnectionReceived;

                await airglideListener.BindServiceNameAsync(
                    airglideProvider.ServiceId.AsString(),
                    SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);

                airglideProvider.StartAdvertising(airglideListener, true);

                _providers.Add(airglideProvider);
                _listeners.Add(airglideListener);
                anyListenerStarted = true;
                OnLog?.Invoke("AirGlide direct Bluetooth RFCOMM listener active.");
            }
            catch (Exception ex)
            {
                OnLog?.Invoke($"Notice: Direct BT RFCOMM: {ex.Message}");
            }

            // 2. Start Standard SPP RFCOMM Listener
            try
            {
                var sppProvider = await RfcommServiceProvider.CreateAsync(RfcommServiceId.FromUuid(SppUuid));
                SetSdpServiceName(sppProvider, "AirGlide SPP");

                var sppListener = new StreamSocketListener();
                sppListener.ConnectionReceived += OnSocketConnectionReceived;

                await sppListener.BindServiceNameAsync(
                    sppProvider.ServiceId.AsString(),
                    SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);

                sppProvider.StartAdvertising(sppListener, true);

                _providers.Add(sppProvider);
                _listeners.Add(sppListener);
                anyListenerStarted = true;
                OnLog?.Invoke("Standard Bluetooth SPP RFCOMM listener active.");
            }
            catch (Exception ex)
            {
                OnLog?.Invoke($"Notice: Standard SPP RFCOMM: {ex.Message}");
            }

            // 3. Scan and start listening on Bluetooth Virtual Serial COM ports (if available)
            StartSerialPortsMonitor();

            IsRunning = anyListenerStarted || _serialPorts.Count > 0;
            if (IsRunning)
            {
                OnLog?.Invoke("Bluetooth receiver ready for phone connection.");
            }
            else
            {
                OnLog?.Invoke("Bluetooth notice: No Bluetooth radio detected or Bluetooth is turned off.");
            }
        }

        private static void SetSdpServiceName(RfcommServiceProvider provider, string serviceName)
        {
            try
            {
                var writer = new DataWriter();
                // SDP string attribute header: 0x25 = text string with uint8 length
                byte[] nameBytes = Encoding.UTF8.GetBytes(serviceName);
                writer.WriteByte(0x25);
                writer.WriteByte((byte)nameBytes.Length);
                writer.WriteBytes(nameBytes);
                provider.SdpRawAttributes.Add(0x0100, writer.DetachBuffer());
            }
            catch { }
        }

        private async void OnSocketConnectionReceived(StreamSocketListener sender, StreamSocketListenerConnectionReceivedEventArgs args)
        {
            try
            {
                _activeSocket = args.Socket;
                _writer = new DataWriter(_activeSocket.OutputStream);

                string remoteHost = args.Socket.Information.RemoteHostName?.DisplayName ?? "Bluetooth Device";
                ConnectedDeviceName = remoteHost;
                IsConnected = true;
                OnConnectionStateChanged?.Invoke(true);
                OnLog?.Invoke($"Bluetooth phone connected: {remoteHost}");

                using var reader = new DataReader(_activeSocket.InputStream)
                {
                    InputStreamOptions = InputStreamOptions.Partial
                };

                var sb = new StringBuilder();

                while (_cts?.IsCancellationRequested != true)
                {
                    uint bytesLoaded = await reader.LoadAsync(1024);
                    if (bytesLoaded == 0) break; // Client disconnected

                    string chunk = reader.ReadString(bytesLoaded);
                    sb.Append(chunk);

                    // Extract and dispatch all lines
                    while (true)
                    {
                        string buffer = sb.ToString();
                        int newlineIndex = buffer.IndexOf('\n');
                        if (newlineIndex < 0) break;

                        string line = buffer.Substring(0, newlineIndex).Trim();
                        sb.Remove(0, newlineIndex + 1);

                        if (!string.IsNullOrEmpty(line))
                        {
                            OnCommandReceived?.Invoke(line, ConnectedDeviceName);

                            // Reply to presence handshake
                            if (line.StartsWith("P:", StringComparison.Ordinal) && _writer != null)
                            {
                                try
                                {
                                    var telem = NativeInput.GetLaptopTelemetry();
                                    _writer.WriteString($"AIRGLIDE_ACK:Bluetooth:5055:{telem.batPct}:{(telem.isCharging ? 1 : 0)}:{telem.cpuPct}:{telem.ramPct}\n");
                                    await _writer.StoreAsync();
                                }
                                catch { }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                OnLog?.Invoke($"Bluetooth connection ended: {ex.Message}");
            }
            finally
            {
                DisconnectClient();
            }
        }

        private void StartSerialPortsMonitor()
        {
            Task.Run(async () =>
            {
                while (_cts?.IsCancellationRequested != true)
                {
                    try
                    {
                        string[] portNames = SerialPort.GetPortNames();
                        foreach (string port in portNames)
                        {
                            // Avoid re-opening ports already managed
                            if (_serialPorts.Exists(p => p.PortName.Equals(port, StringComparison.OrdinalIgnoreCase)))
                                continue;

                            TryOpenSerialPort(port);
                        }
                    }
                    catch { }

                    await Task.Delay(5000);
                }
            });
        }

        private void TryOpenSerialPort(string portName)
        {
            try
            {
                var sp = new SerialPort(portName, 115200, Parity.None, 8, StopBits.One)
                {
                    ReadTimeout = 500,
                    WriteTimeout = 500,
                    NewLine = "\n"
                };

                sp.DataReceived += (s, e) =>
                {
                    try
                    {
                        while (sp.IsOpen && sp.BytesToRead > 0)
                        {
                            string line = sp.ReadLine().Trim();
                            if (!string.IsNullOrEmpty(line))
                            {
                                if (!IsConnected)
                                {
                                    IsConnected = true;
                                    ConnectedDeviceName = portName;
                                    OnConnectionStateChanged?.Invoke(true);
                                    OnLog?.Invoke($"Serial Bluetooth port connected: {portName}");
                                }

                                OnCommandReceived?.Invoke(line, portName);

                                if (line.StartsWith("P:", StringComparison.Ordinal))
                                {
                                    sp.WriteLine("AIRGLIDE_ACK:Bluetooth");
                                }
                            }
                        }
                    }
                    catch { }
                };

                sp.Open();
                _serialPorts.Add(sp);
                OnLog?.Invoke($"Listening on serial port {portName} for Bluetooth link.");
            }
            catch
            {
                // Port might be in use by another device or not accessible; ignore
            }
        }

        public void DisconnectClient()
        {
            try { _writer?.DetachStream(); } catch { }
            try { _activeSocket?.Dispose(); } catch { }
            _writer = null;
            _activeSocket = null;
            IsConnected = false;
            ConnectedDeviceName = "";
            OnConnectionStateChanged?.Invoke(false);
        }

        public void Stop()
        {
            _cts?.Cancel();
            DisconnectClient();

            foreach (var sp in _serialPorts)
            {
                try { if (sp.IsOpen) sp.Close(); sp.Dispose(); } catch { }
            }
            _serialPorts.Clear();

            foreach (var provider in _providers)
            {
                try { provider.StopAdvertising(); } catch { }
            }
            _providers.Clear();

            foreach (var listener in _listeners)
            {
                try { listener.Dispose(); } catch { }
            }
            _listeners.Clear();

            IsRunning = false;
            OnLog?.Invoke("Bluetooth receiver stopped.");
        }
    }
}
