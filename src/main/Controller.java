package main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    List<PcapIf> alldevs = new ArrayList<>(); // Filled with devices
    StringBuilder errbuf = new StringBuilder(); // Error messages
    ObservableList<String> devices = FXCollections.observableArrayList(); // Dynamic list of devices

    @FXML
    Label selected_device;
    @FXML
    ListView<String> list_devices;

    public void getDevices() {
        devices.removeAll();
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
        } else {
            for (PcapIf alldev : alldevs) {
                devices.add(alldev.getName());
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set content to ListView from the beginning
        list_devices.setItems(devices);

        // Show devices from the beginning
        this.getDevices();

        // Selection listener
        list_devices.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    selected_device.setText(newValue);
                }
        );

        // Automatically select eth0
        if (devices.contains("eth0")) list_devices.getSelectionModel().select("eth0");

    }
}
