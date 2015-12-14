
import socket
import struct
import requests # this module has to be installed from outside python's built-in modules
import sys

class Traceroute:

    FREEGEOPIP_URL = 'http://freegeoip.net/json/'

    def __init__(self, dest_name, port=33434, max_hops=30, ttl=1):
        self.dest_name = dest_name
        self.port = port
        self.max_hops = max_hops
        self.ttl = ttl
        self.curr_addr = None
        self.curr_name = None
        self.route_list = []

    def get_ip(self):
        return socket.gethostbyname(self.dest_name)

    def getting_protocols(self, proto1, proto2):
        return socket.getprotobyname(proto1), socket.getprotobyname(proto2)

    def create_sockets(self):
        return socket.socket(socket.AF_INET, socket.SOCK_RAW, self.icmp), socket.socket(socket.AF_INET, socket.SOCK_DGRAM, self.udp)

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

    def set_curr_host(self):
        # manipulating prints
        if self.curr_addr is not None:
            self.route_list.append([self.ttl, self.curr_name, self.curr_addr])
            #self.curr_host = "%s (%s)" % (self.curr_name, self.curr_addr)
        else:
            self.route_list.append([0, "Unreachable", "Unreachable"])
            #self.curr_host = "*"

    def get_geolocation_for_ip(self, ip):

        url = '{}/{}'.format(self.FREEGEOPIP_URL, ip)

        try:
            response = requests.get(url)

            if response.status_code == 200:
                return response.json()
            elif response.status_code == 403:
                print '403 - forbidden error'
                sys.exit()
            else:
                print 'something went wrong'
                sys.exit()

        except requests.exceptions.ConnectionError:
            print 'check network connection'
            sys.exit()

    def trace(self):
        
        self.dest_addr = self.get_ip()

        self.icmp, self.udp = self.getting_protocols('icmp', 'udp')

        self.timeout = struct.pack("ll", 5, 0)

        while True:

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

            self.set_curr_host()

            #print "%d\t%s" % (self.ttl, self.curr_host)
            #print 'json file'
            #print self.get_geolocation_for_ip(self.curr_addr)
            #print '\n'

            self.ttl += 1
            
            # when to stop
            if self.curr_addr == self.dest_addr or self.ttl > self.max_hops:
                break

        return self.manipulate_list()

    def manipulate_list(self):

        final_list = []

        # making equal IP turn to be equal elements inside the array
        for x in range(0, len(self.route_list)):
            for y in range(x, len(self.route_list)):
                if self.route_list[x][2] == self.route_list[y][2]:
                    self.route_list[y][0] = self.route_list[x][0]

        # removing duplicates
        for x in self.route_list:
            if x not in final_list:
                final_list.append(x)

        for i in range(1, len(final_list) + 1):
            final_list[i - 1][0] = i

        return final_list

x = Traceroute('allspice.lcs.mit.edu')

route = x.trace()

print route