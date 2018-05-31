#!/usr/bin
import socket
import time
from threading import Thread

my_ip="192.168.1.14"
neighbours = ["192.168.1.19"]
nodecount=len(neighbours)
PORT = 5002
MESSAGE = "alive"

	
print "UDP target IP:", neighbours
print "UDP target port:", PORT
print "message:", MESSAGE


def pulse():
	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	while True:
		for i in range(nodecount):
			sock.sendto(MESSAGE, (neighbours[i], PORT))
			print("Alive sent to "+str(i)) 
		time.sleep(1)

def sense():

	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	sock.bind((my_ip, PORT))
		
	while True:
		data, addr = sock.recvfrom(1024)
    #couldn't figure out finding dead node
		if data=='alive':
			print("OK "+str(address))


'''def adapt(dir):
		print("node "+str(dir)+" failed")'''

def initHB():

	t1 = Thread(target=pulse)
	t2 = Thread(target=sense)
	t1.start()
	time.sleep(1)
	t2.start()
	

'''if __name__== "__main__":
	initHB()'''
