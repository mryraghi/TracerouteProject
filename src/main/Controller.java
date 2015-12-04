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
import org.jnetpcap.packet.JMemoryPacket;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.protocol.JProtocol;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Udp;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
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

    private List<PcapIf> alldevs = new ArrayList<>(); // Filled with devices
    private StringBuilder errbuf = new StringBuilder(); // Error messages
    private ObservableList<String> devices = FXCollections.observableArrayList(); // Dynamic list of devices
    private Pcap pcap;
    private NetworkInterface network;
    private PcapIf pcap_selected_device;
    private int UDP_SOURCE_PORT = 7006;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Set content to ListView from the beginning
        list_devices.setItems(devices);

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
        button_start.setDisable(true);

        int snaplen = 64 * 2014; // Truncate packet at this size
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
        if (pcap == null) {
            error(errbuf.toString());
            return;
        }
        else {
            status("Loading...");
            list_devices.setDisable(true);
            button_stop.setDisable(false);
        }

        // Example data
        byte[] packet_data = "Hello".getBytes();

        // Check given IP and send packet
        if (!destination_ip.getText().isEmpty()) sendPacket(packet_data);
    }

    private void sendPacket(byte[] data) throws SocketException, UnknownHostException {
        status("Sending packet...");

        int dataLength = data.length;

        // Ethernet header (14) + IP v4 header (20) + UDP header (8)
        int packetSize = dataLength + 32;
        JPacket packet = new JMemoryPacket(packetSize);

        // ByteOrder.BIG_ENDIAN means that
        // bites are read in an increasing order
        packet.order(ByteOrder.BIG_ENDIAN);

        // 0x0800 is the EtherType
        // It refers to: Internet Protocol version 4 (IPv4)
        packet.setUShort(12, 0x0800);
        packet.scan(JProtocol.ETHERNET_ID);

        /**
         * Ethernet packet
         */

        Ethernet ethernet = packet.getHeader(new Ethernet());
        ethernet.source(network.getHardwareAddress());

        // Destination MAC address still needs to be defined
        // Left blank intentionally
        ethernet.destination();

        // From Wireshark documentation:
        // Checksums are used to ensure the integrity of
        // data portions for data transmission or storage.
        // A checksum is basically a calculated summary of such a data portion.
        ethernet.checksum(ethernet.calculateChecksum());

        /**
         * IPv4 packet
         */

        // setUByte(offset, value)
        packet.setUByte(14, 0x40 | 0x05);
        packet.scan(JProtocol.ETHERNET_ID);

        // getHeader: peers the supplied header with the native
        // header state structure and packet data buffer
        Ip4 ip4 = packet.getHeader(new Ip4());

        // Ip4.Ip4Type: table of IpTypes and their names
        ip4.type(Ip4.Ip4Type.UDP);
        ip4.length(packetSize - ethernet.size());

        // pcap_selected_device is the Pcap Object of the selected device
        ip4.source(pcap_selected_device.getAddresses().get(3).getAddr().getData());

        // destination_ip is the textfield in which an IP is typed
        // Gets the text from it and converts it into an array of bytes
        ip4.destination(getByteHexArray(destination_ip.getText()));

        // TODO: TTL is going to be lower in a loop
        ip4.ttl(32);

        // A setter method that changes the flag bits directly
        // in the peered Ip4 header structure within the packet data buffer.
        ip4.flags(0);
        ip4.offset(0);

        // Calculates a checksum using protocol specification for a header.
        ip4.checksum(ip4.calculateChecksum());

        /**
         * UDP packet
         */

        packet.scan(JProtocol.ETHERNET_ID);
        Udp udp = packet.getHeader(new Udp());
        udp.source(UDP_SOURCE_PORT);

        // Free ports between 33434 and 33534
        udp.destination(33434);
        udp.length(packetSize - ethernet.size() - ip4.size());
        udp.checksum(udp.calculateChecksum());

        // Ethernet header (14) + IP v4 header (20) + UDP header (8)
        packet.setByteArray(32, data);
        packet.scan(Ethernet.ID);

        if (pcap.sendPacket(packet) != Pcap.OK) error(pcap.getErr());
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
