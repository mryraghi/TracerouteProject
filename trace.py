import socket
import struct
import sys
import requests # external module

class Traceroute:

    FREEGEOPIP_URL = 'http://freegeoip.net/json/'

    def __init__(self, sysArgs, port=33434, max_hops=30, ttl=1):
        self.dest_name = str(sysArgs[1])
        self.port = port
        self.max_hops = max_hops
        self.ttl = ttl
        self.curr_addr = None
        self.curr_name = None
        self.last_printed = [0, "", ""]
        self.hop_number = 0

    def get_ip(self):
        return socket.gethostbyname(self.dest_name)

    def getting_protocols(self, proto1, proto2):
        return socket.getprotobyname(proto1), socket.getprotobyname(proto2)

    def create_sockets(self):
        return socket.socket(socket.AF_INET, socket.SOCK_RAW, self.icmp), socket.socket(socket.AF_INET,
                                                                                        socket.SOCK_DGRAM, self.udp)

    def set_sockets(self):

        self.send_socket.setsockopt(socket.SOL_IP, socket.IP_TTL, self.ttl)
        self.recv_socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVTIMEO, self.timeout)

        self.recv_socket.bind(("", self.port))
        self.send_socket.sendto("", (self.dest_name, self.port))

    def get_hostname(self):
        # trying to get the hostname
        try:
            self.curr_name = socket.gethostbyaddr(self.curr_addr)[0]
        except socket.error:
            self.curr_name = self.curr_addr

    def close_sockets(self):
        self.send_socket.close()
        self.recv_socket.close()

    def print_curr_hop(self):
        # manipulating prints
        if self.curr_addr is not None and self.curr_addr != self.last_printed[2]:
            self.hop_number += 1
            to_print = [self.hop_number, self.curr_name, self.curr_addr, self.get_geolocation_for_ip(self.curr_addr)]
            print to_print
            self.last_printed = to_print

    def trace(self):
        self.dest_addr = self.get_ip()

        self.icmp, self.udp = self.getting_protocols('icmp', 'udp')

        self.timeout = struct.pack("ll", 5, 0)

        while self.ttl < 20:

            self.recv_socket, self.send_socket = self.create_sockets()

            self.set_sockets()

            try:
                # getting data from receiving socket
                _, self.curr_addr = self.recv_socket.recvfrom(512)
                # _ is the data and curr_addr is a tuple with ip address and port, we care only for the first one
                self.curr_addr = self.curr_addr[0]

                self.get_hostname()

            except socket.error:
                pass

            finally:
                self.close_sockets()

            self.print_curr_hop()

            self.ttl += 1

            # when to stop
            if self.curr_addr == self.dest_addr or self.ttl > self.max_hops:
                break

    def get_geolocation_for_ip(self, ip):
 
        url = '{}/{}'.format(self.FREEGEOPIP_URL, ip)
 
        try:
            response = requests.get(url)
            if response.status_code == 200:
                json_return = response.json()
                return json_return['latitude'], json_return['longitude']
            elif response.status_code == 403:
                print '403 - forbidden error'
                sys.exit()
            else:
                print 'something went wrong'
                sys.exit()
 
        except requests.exceptions.ConnectionError:
            print 'check network connection'
            sys.exit()


x = Traceroute(sys.argv)

x.trace()
