package main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class Controller implements Initializable {
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
    LineChart.Series<Double, Double> series1;
    private ObservableList<String> traceroute = FXCollections.observableArrayList(); // Dynamic list of nodes
    private Thread t;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set content to ListView from the beginning
        list_results.setItems(traceroute);

        ObservableList<XYChart.Series<Double, Double>> lineChartData = FXCollections.observableArrayList();

        series1 = new LineChart.Series<>();
        series1.setName("Hello Munsell");

        lineChartData.add(series1);

        chart.setData(lineChartData);
        chart.createSymbolsProperty();

    }

    public void beginTraceroute() throws IOException {
        traceroute.removeAll();
        list_results.getItems().clear();
        series1.getData().clear();
        error();

        status("Loading, this can take some time...");
        // Loader
        progress.setProgress(-1.0f);
        button_start.setDisable(true);
        button_stop.setDisable(false);
        try {
            String prg = getScript();
            BufferedWriter out = new BufferedWriter(new FileWriter("trace.py"));
            out.write(prg);
            out.close();
            ProcessBuilder pd = new ProcessBuilder().command("sudo", "python", "trace.py", destination_ip.getText());
            pd.redirectErrorStream(true);
            Process p = pd.start();

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line, message = null;

                        while ((line = in.readLine()) != null) {
                            if (!Objects.equals(line.substring(0, 1), "[")) {
                                if (Objects.equals(line.substring(0, 9), "Traceback")) {
                                    message = "Please insert a valid domain name";
                                } else if (Objects.equals(line.substring(0, 4), "sudo")) {
                                    message = "Please run this app with sudo privileges!";
                                } else {
                                    message = "Unknown error occurred!";
                                    error(line);
                                }
                                break;
                            } else {
                                traceroute.add(line);
                            }
                        }

                        final String finalMessage = message;
                        Platform.runLater(() -> {
                            stopTraceroute();
                            if (finalMessage != null) {
                                error(finalMessage);
                                series1.getData().clear();
                            } else {
                                double i = 5.0;
                                for (String s : traceroute) {
                                    double height = (Math.random() * 50) + 25;
                                    final XYChart.Data<Double, Double> data = new XYChart.Data<>(i, height);
                                    String[] output = s.split("'", 5);
                                    data.setNode(new HoveredThresholdNode(output[3], i));
                                    series1.getData().add(data);
                                    i += 5;
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.out.print(p.getErrorStream());
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

    public void stopTraceroute() {
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

    private String getScript() {
        return "import socket\n" +
                "import struct\n" +
                "import sys\n" +
                "import requests # external module\n" +
                "\n" +
                "class Traceroute:\n" +
                "\n" +
                "    FREEGEOPIP_URL = 'http://freegeoip.net/json/'\n" +
                "\n" +
                "    def __init__(self, sysArgs, port=33434, max_hops=30, ttl=1):\n" +
                "        self.dest_name = str(sysArgs[1])\n" +
                "        self.port = port\n" +
                "        self.max_hops = max_hops\n" +
                "        self.ttl = ttl\n" +
                "        self.curr_addr = None\n" +
                "        self.curr_name = None\n" +
                "        self.last_printed = [0, \"\", \"\"]\n" +
                "        self.hop_number = 0\n" +
                "\n" +
                "    def get_ip(self):\n" +
                "        return socket.gethostbyname(self.dest_name)\n" +
                "\n" +
                "    def getting_protocols(self, proto1, proto2):\n" +
                "        return socket.getprotobyname(proto1), socket.getprotobyname(proto2)\n" +
                "\n" +
                "    def create_sockets(self):\n" +
                "        return socket.socket(socket.AF_INET, socket.SOCK_RAW, self.icmp), socket.socket(socket.AF_INET,\n" +
                "                                                                                        socket.SOCK_DGRAM, self.udp)\n" +
                "\n" +
                "    def set_sockets(self):\n" +
                "\n" +
                "        self.send_socket.setsockopt(socket.SOL_IP, socket.IP_TTL, self.ttl)\n" +
                "        self.recv_socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVTIMEO, self.timeout)\n" +
                "\n" +
                "        self.recv_socket.bind((\"\", self.port))\n" +
                "        self.send_socket.sendto(\"\", (self.dest_name, self.port))\n" +
                "\n" +
                "    def get_hostname(self):\n" +
                "        # trying to get the hostname\n" +
                "        try:\n" +
                "            self.curr_name = socket.gethostbyaddr(self.curr_addr)[0]\n" +
                "        except socket.error:\n" +
                "            self.curr_name = self.curr_addr\n" +
                "\n" +
                "    def close_sockets(self):\n" +
                "        self.send_socket.close()\n" +
                "        self.recv_socket.close()\n" +
                "\n" +
                "    def print_curr_hop(self):\n" +
                "        # manipulating prints\n" +
                "        if self.curr_addr is not None and self.curr_addr != self.last_printed[2]:\n" +
                "            self.hop_number += 1\n" +
                "            to_print = [self.hop_number, self.curr_name, self.curr_addr, self.get_geolocation_for_ip(self.curr_addr)]\n" +
                "            print to_print\n" +
                "            self.last_printed = to_print\n" +
                "\n" +
                "    def trace(self):\n" +
                "        self.dest_addr = self.get_ip()\n" +
                "\n" +
                "        self.icmp, self.udp = self.getting_protocols('icmp', 'udp')\n" +
                "\n" +
                "        self.timeout = struct.pack(\"ll\", 5, 0)\n" +
                "\n" +
                "        while self.ttl < 20:\n" +
                "\n" +
                "            self.recv_socket, self.send_socket = self.create_sockets()\n" +
                "\n" +
                "            self.set_sockets()\n" +
                "\n" +
                "            try:\n" +
                "                # getting data from receiving socket\n" +
                "                _, self.curr_addr = self.recv_socket.recvfrom(512)\n" +
                "                # _ is the data and curr_addr is a tuple with ip address and port, we care only for the first one\n" +
                "                self.curr_addr = self.curr_addr[0]\n" +
                "\n" +
                "                self.get_hostname()\n" +
                "\n" +
                "            except socket.error:\n" +
                "                pass\n" +
                "\n" +
                "            finally:\n" +
                "                self.close_sockets()\n" +
                "\n" +
                "            self.print_curr_hop()\n" +
                "\n" +
                "            self.ttl += 1\n" +
                "\n" +
                "            # when to stop\n" +
                "            if self.curr_addr == self.dest_addr or self.ttl > self.max_hops:\n" +
                "                break\n" +
                "\n" +
                "    def get_geolocation_for_ip(self, ip):\n" +
                " \n" +
                "        url = '{}/{}'.format(self.FREEGEOPIP_URL, ip)\n" +
                " \n" +
                "        try:\n" +
                "            response = requests.get(url)\n" +
                "            if response.status_code == 200:\n" +
                "                json_return = response.json()\n" +
                "                return json_return['latitude'], json_return['longitude']\n" +
                "            elif response.status_code == 403:\n" +
                "                print '403 - forbidden error'\n" +
                "                sys.exit()\n" +
                "            else:\n" +
                "                print 'something went wrong'\n" +
                "                sys.exit()\n" +
                " \n" +
                "        except requests.exceptions.ConnectionError:\n" +
                "            print 'check network connection'\n" +
                "            sys.exit()\n" +
                "\n" +
                "\n" +
                "x = Traceroute(sys.argv)\n" +
                "\n" +
                "x.trace()\n";
    }

}

class HoveredThresholdNode extends StackPane {
    HoveredThresholdNode(String s, double width) {
        setPrefSize(15, 15);

        final Label label = createDataThresholdLabel(s, width);

        setOnMouseEntered(mouseEvent -> {
            getChildren().setAll(label);
            setCursor(Cursor.NONE);
            toFront();
        });
        setOnMouseExited(mouseEvent -> {
            getChildren().clear();
            setCursor(Cursor.CROSSHAIR);
        });
    }

    private Label createDataThresholdLabel(String s, double width) {
        final Label label = new Label(s);
        String styles = "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #fff; -fx-padding: 5px; -fx-border-radius: 3px;";
        if (width > 50) {
            label.setStyle(styles + " -fx-translate-x: -50px");
        } else {
            label.setStyle(styles + " -fx-translate-x: 30px");
        }

        label.setTextFill(Color.BLACK);

        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        return label;
    }
}
