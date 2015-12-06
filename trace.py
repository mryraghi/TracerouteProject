import socket
import struct

def main(dest_name):
    
    dest_addr = socket.gethostbyname(dest_name)                                         # in case the given address is a domain

    port = 33434
    
    max_hops = 30
    
    icmp = socket.getprotobyname('icmp')
    udp = socket.getprotobyname('udp')
    
    ttl = 1
    timeout = struct.pack("ll", 5, 0)

    while True:
        
        # creating sockets
        recv_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, icmp)
        send_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, udp)
        
        # setting ttl and timeout for receiving socket (to behave more like a traceroute prompt command)
        send_socket.setsockopt(socket.SOL_IP, socket.IP_TTL, ttl)
        recv_socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVTIMEO, timeout)
        
        # setting receiving soket to listen to a specific port
        recv_socket.bind(("", port))
        # sending to destination host using the same port
        send_socket.sendto("", (dest_name, port))
        
        curr_addr = None
        curr_name = None
        
        try:
            # getting data from receiving socket
            _, curr_addr = recv_socket.recvfrom(512)
            # _ is the data and curr_addr is a tuple with ip address and port, we care only for the first one
            curr_addr = curr_addr[0]
            
            # trying to get the hostname
            try:
                curr_name = socket.gethostbyaddr(curr_addr)[0]
            
            except socket.error:
                curr_name = curr_addr
        
        except socket.error:
            pass
        
        finally:
            send_socket.close()
            recv_socket.close()

        # manipulating prints
        if curr_addr is not None:
            curr_host = "%s (%s)" % (curr_name, curr_addr)
        else:
            curr_host = "*"
        
        print "%d\t%s" % (ttl, curr_host)

        ttl += 1
        
        # when to stop
        if curr_addr == dest_addr or ttl > max_hops:
            break

if __name__ == "__main__":
    #main('google.com')
    main('allspice.lcs.mit.edu')