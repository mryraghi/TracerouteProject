package main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    Label selected_device;
    @FXML
    ListView<String> list_devices;
    @FXML
    Label current_ip;
    @FXML
    Label current_mac_address;
    @FXML
    TextField destination_ip;
    @FXML
    Label label_errbuf;
    @FXML
    Label label_status;
    @FXML
    Button button_stop;
    @FXML
    Button button_start;
    @FXML
    ListView<String> list_results;

    private List<PcapIf> alldevs = new ArrayList<>(); // Filled with devices
    private StringBuilder errbuf = new StringBuilder(); // Error messages
    private ObservableList<String> devices = FXCollections.observableArrayList(); // Dynamic list of devices
    private ObservableList<String> traceroute = FXCollections.observableArrayList(); // Dynamic list of nodes
    private Pcap pcap;
    private NetworkInterface network;
    private PcapIf pcap_selected_device;
    private int UDP_SOURCE_PORT = 7006;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Set content to ListView from the beginning
        list_devices.setItems(devices);
        list_results.setItems(traceroute);

        // Show devices from the beginning
        this.getDevices();

        // Selection listener
        list_devices.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selected_device.setText(newValue);

            // Get information about device from selection
            pcap_selected_device = alldevs.get(list_devices.getSelectionModel().getSelectedIndex());

            // Set current IP
            try {
                // Get IP from Object
                current_ip.setText(pcap_selected_device.getAddresses().get(3).getAddr().toString().substring(7, 19));
                button_start.setDisable(false);
            } catch (Exception e) {
                current_ip.setText("No valid IP address");
                button_start.setDisable(true);
            }

            //Set current MAC address
            try {
                // Get the network interface as an Object
                // from a string, which is the name of
                // the selected device
                network = NetworkInterface.getByName(newValue);

                if (network != null && network.getHardwareAddress() != null) {
                    // Format MAC address
                    byte[] mac_format = network.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac_format.length; i++) {
                        sb.append(String.format("%02X%s", mac_format[i], (i < mac_format.length - 1) ? ":" : ""));
                    }

                    // Set text
                    current_mac_address.setText(sb.toString());
                } else {
                    current_mac_address.setText("No valid MAC address");
                }

            } catch (SocketException e) {
                error(e.getMessage());
            }

            // Reset error message
            error();
        });

        // Automatically select eth0
        if (devices.contains("eth0")) list_devices.getSelectionModel().select("eth0");
    }

    public void getDevices() {
        devices.removeAll();
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            label_errbuf.setText(errbuf.toString());
        } else {
            for (PcapIf alldev : alldevs) {
                devices.add(alldev.getName());
            }
        }
    }

    public void beginTraceroute() throws IOException {

        // Disable start button
        // button_start.setDisable(true);

        /*int snaplen = 64 * 2014; // Truncate packet at this size
        // MODE_NON_PROMISCUOUS: sniffs only traffic that is directly related to it.
        // Only traffic to, from, or routed through the host will be picked up by the sniffer.
        int promiscuous = Pcap.MODE_PROMISCUOUS; // = 1
        int timeout = 60 * 1000; // In milliseconds
        pcap = Pcap.openLive(pcap_selected_device.getName(),
                snaplen,
                promiscuous,
                timeout,
                errbuf);

        // Show error message or loading message
        if (pcap == null)

        {
            error(errbuf.toString());
            return;
        } else

        {
            status("Loading...");
            list_devices.setDisable(true);
            button_stop.setDisable(false);
        }

        // Example data
        byte[] packet_data = "Hello".getBytes();

        // Check given IP and send packet
        // if (!destination_ip.getText().isEmpty()) sendPacket(packet_data);*/


        try {
            ProcessBuilder pd = new ProcessBuilder("sudo", "python", "trace.py", destination_ip.getText());
            Process p = pd.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

            for (String line = in.readLine(); line != null; line = in.readLine()) {
                traceroute.add(line);
            }

            in.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void stopTraceroute() throws UnknownHostException, SocketException {
        pcap.close();
        status("Connection closed");
        button_stop.setDisable(true);
        list_devices.setDisable(false);
        button_start.setDisable(false);
    }

    private int[] parseHex(byte[] b) {
        int[] data = new int[b.length];
        int i = 0;
        for (byte x : b) {
            System.out.println(x);
            data[i++] = x & 0xff;
        }
        return data;
    }

    /**
     * Receives an IP in a String format (i.e. 127.0.0.1),
     * splits it in parts using dots as dividers, parses
     * every part into integers, parses again every part
     * into hexadecimal values and then into bytes. Finally,
     * a byte array is created with those values.
     * Check IP before using this method, exception
     * can occur!
     *
     * @param ip IP address to be converted
     * @return array of bytes
     */
    private byte[] getByteHexArray(String ip) {
        // TODO: check IP before
        String[] _ip = ip.split("\\.");
        byte[] result = new byte[4];
        int i = 0;
        for (String x : _ip) {
            int temp_int = Integer.parseInt(x);
            String temp_hex = Integer.toHexString(temp_int);
            result[i++] = (byte) (Integer.parseInt(temp_hex, 16) & 0xff);
        }
        return result;
    }

    private void print(String s) {
        System.out.println(s);
    }

    private void status(String s) {
        label_status.setText(s);
    }

    private void error(String s) {
        label_errbuf.setText(s);
    }

    private void error() {
        label_errbuf.setText("");
    }
}
