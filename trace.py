
import socket
import struct

class Traceroute:

    def __init__(self, dest_name, port=33434, max_hops=30, ttl=1):
        self.dest_name = dest_name
        self.port = port
        self.max_hops = max_hops
        self.ttl = ttl
        self.curr_addr = None
        self.curr_name = None

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
            self.curr_host = "%s (%s)" % (self.curr_name, self.curr_addr)
        else:
            self.curr_host = "*"

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

            print "%d\t%s" % (self.ttl, self.curr_host)

            self.ttl += 1
            
            # when to stop
            if self.curr_addr == self.dest_addr or self.ttl > self.max_hops:
                break



x = Traceroute('allspice.lcs.mit.edu')

x.trace()