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

import java.io.IOException;
import java.net.URL;
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
    TextField destination_ip;
    @FXML
    Label label_errbuf;
    @FXML
    Label label_status;
    @FXML
    Button button_stop;

    private List<PcapIf> alldevs = new ArrayList<>(); // Filled with devices
    private StringBuilder errbuf = new StringBuilder(); // Error messages
    private ObservableList<String> devices = FXCollections.observableArrayList(); // Dynamic list of devices
    private Pcap pcap;
    private PcapIf pcap_selected_device;

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
            } catch (Exception e) {
                current_ip.setText("No valid IP address");
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
        int snaplen = 2 * 2014; // Truncate packet at this size
        int promiscuous = Pcap.MODE_PROMISCUOUS; // = 1
        int timeout = 60 * 1000; // In milliseconds
        pcap = Pcap.openLive(pcap_selected_device.getName(),
                snaplen,
                promiscuous,
                timeout,
                errbuf);

        // Show error message or loading message
        if (pcap == null) error(errbuf.toString());
        else {
            status("Loading...");
            list_devices.setDisable(true);
            button_stop.setDisable(false);
        }
    }

    public void stopTraceroute() {
        pcap.close();
        status("Connection closed");
        button_stop.setDisable(true);
        list_devices.setDisable(false);
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
