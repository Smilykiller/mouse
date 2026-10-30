using System;
using System.IO;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth.Rfcomm;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;

namespace AirGlideDesktop
{
    /// <summary>
    /// Listens for incoming Bluetooth RFCOMM Serial Port Profile (SPP) connections from the mobile app.
    /// Provides zero-Wi-Fi wireless control for travel, hotspots, and offline use.
    /// </summary>
    public class BluetoothReceiverEngine
    {
        // Standard SPP UUID: 00001101-0000-1000-8000-00805F9B34FB
        public static readonly Guid SppUuid = new Guid("00001101-0000-1000-8000-00805F9B34FB");

        private StreamSocketListener? _listener;
        private RfcommServiceProvider? _provider;
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

            try
            {
                _cts = new CancellationTokenSource();

                // Create the RFCOMM Service Provider for standard Serial Port
                _provider = await RfcommServiceProvider.CreateAsync(RfcommServiceId.FromUuid(SppUuid));

                _listener = new StreamSocketListener();
                _listener.ConnectionReceived += OnSocketConnectionReceived;

                // Bind listener to the RFCOMM service
                await _listener.BindServiceNameAsync(
                    _provider.ServiceId.AsString(),
                    SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);

                // Set SDP attributes so Android can discover this as an active SPP service
                _provider.StartAdvertising(_listener);

                IsRunning = true;
                OnLog?.Invoke("Bluetooth RFCOMM listener started (SPP). Ready for phone pairing.");
            }
            catch (Exception ex)
            {
                IsRunning = false;
                OnLog?.Invoke($"Bluetooth notice: {ex.Message} (Bluetooth may be disabled or unequipped)");
            }
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
                OnLog?.Invoke($"Bluetooth client connected: {remoteHost}");

                using var reader = new DataReader(_activeSocket.InputStream)
                {
                    InputStreamOptions = InputStreamOptions.Partial
                };

                var sb = new StringBuilder();

                while (_cts?.IsCancellationRequested != true)
                {
                    uint bytesLoaded = await reader.LoadAsync(512);
                    if (bytesLoaded == 0) break; // Client disconnected

                    string chunk = reader.ReadString(bytesLoaded);
                    sb.Append(chunk);

                    // Process complete lines separated by \n
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

                            // Reply to ping or presence handshake
                            if (line.StartsWith("P:", StringComparison.Ordinal) && _writer != null)
                            {
                                try
                                {
                                    _writer.WriteString("AIRGLIDE_ACK:Bluetooth\n");
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
                OnLog?.Invoke($"Bluetooth connection closed: {ex.Message}");
            }
            finally
            {
                DisconnectClient();
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

            try { _provider?.StopAdvertising(); } catch { }
            try { _listener?.Dispose(); } catch { }

            _provider = null;
            _listener = null;
            IsRunning = false;
            OnLog?.Invoke("Bluetooth receiver stopped.");
        }
    }
}
