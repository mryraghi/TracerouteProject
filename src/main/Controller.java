package main;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

public class Controller implements Initializable, MapComponentInitializedListener {
    public GoogleMapView mapView;
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
    @FXML
    ProgressBar progress;
    @FXML
    LineChart chart;
    @FXML
    NumberAxis yAxis;
    @FXML
    NumberAxis xAxis;
    private ObservableList<String> traceroute = FXCollections.observableArrayList(); // Dynamic list of nodes
    private ObservableList<XYChart.Series<Double, Double>> series;
    private ObservableList<XYChart.Data<Double, Double>> data;
    private Thread t;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set content to ListView from the beginning
        list_results.setItems(traceroute);

        series = FXCollections.observableArrayList();

        data = FXCollections.observableArrayList();
        data.add(new XYChart.Data<>(0.0, 1.0));
        data.add(new XYChart.Data<>(1.2, 1.4));
        data.add(new XYChart.Data<>(2.2, 1.9));
        data.add(new XYChart.Data<>(2.7, 2.3));
        data.add(new XYChart.Data<>(2.9, 0.5));

        series.add(new XYChart.Series<>("Series1", data));


        chart = new LineChart<>(xAxis, yAxis, series);

        chart.setData(data);

        mapView = new GoogleMapView(true);
        mapView.addMapInializedListener(this);
    }

    public void beginTraceroute() throws IOException {
        traceroute.removeAll();
        status("Loading, this can take some time...");
        // Loader
        progress.setProgress(-1.0f);
        button_start.setDisable(true);
        button_stop.setDisable(false);
        try {
            ProcessBuilder pd = new ProcessBuilder().command("sudo", "python", "trace.py", destination_ip.getText());
            Process p = pd.start();

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    try {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                            String line;

                            while ((line = in.readLine()) != null) {
                                traceroute.add(line);
                            }

//                            Platform.runLater(() -> {
//                                try {
//                                    stopTraceroute();
//                                } catch (UnknownHostException | SocketException e) {
//                                    e.printStackTrace();
//                                }
//                            });

                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    return null;
                }
            };
            t = new Thread(task);
            t.setDaemon(true);
            t.start();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void stopTraceroute() throws UnknownHostException, SocketException {
        status("Connection closed");
        progress.setProgress(0f);
        button_stop.setDisable(true);
        button_start.setDisable(false);
        t.interrupt();
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

    @Override
    public void mapInitialized() {
        LatLong joeSmithLocation = new LatLong(47.6197, -122.3231);
        //Set the initial properties of the map.
        MapOptions mapOptions = new MapOptions();

        mapOptions.center(new LatLong(47.6097, -122.3331))
//                .mapType(MapType.ROADMAP)
                .overviewMapControl(false)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(false)
                .zoom(12);

        GoogleMap map = mapView.createMap(mapOptions);

        //Add markers to the map
        MarkerOptions markerOptions1 = new MarkerOptions();
        markerOptions1.position(joeSmithLocation).visible(Boolean.TRUE)
                .title("My Marker");
        Marker joeSmithMarker = new Marker(markerOptions1);
        map.addMarker( joeSmithMarker );
    }
}
