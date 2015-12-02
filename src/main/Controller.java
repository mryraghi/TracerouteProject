package main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.util.ArrayList;
import java.util.List;

public class Controller {
    List<PcapIf> alldevs = new ArrayList<>(); // Will be filled with NICs
    StringBuilder errbuf = new StringBuilder(); // For any error msgs
    ObservableList<String> devices = FXCollections.observableArrayList();

    @FXML
    TextField textfield_ip;
    @FXML
    ListView<String> list_devices = new ListView<>(devices);

    public void getDevices() {
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
        } else {
            System.out.println(r);
            devices.removeAll();
            for (PcapIf alldev : alldevs) {
                devices.add(alldev.getName());
            }
        }
    }
}
