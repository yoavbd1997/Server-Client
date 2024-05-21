#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include <iostream>
#include <string>
using std::string;
#include <thread>
#include "../include/thread_InputManager.h"
#include "../include/StompProtocol.h"
#include <stdexcept>
#include <exception>
#include <iostream>
#include <sstream>

int main(int argc, char *argv[]) {
	bool times = true;
	while(times){
		const short bufsize = 1024;
    	char buf[bufsize];
    	std::cin.getline(buf, bufsize);
		std::string line(buf);
		StompProtocol insta = StompProtocol();
		try{
		vector<string> sent1 = insta.splitByDelimiter(line,' ');
			string command = sent1.at(0);
			//std::cout << command;
			if(command=="login"){
				string address = sent1.at(1);
				vector<string> sent2 = insta.splitByDelimiter(address,':');
				string host = sent2.at(0);
				short port = std::stoi(sent2.at(1));

				ConnectionHandler *connectionHandler = new ConnectionHandler(host, port);
				connectionHandler->connected =true;
			if (connectionHandler->connect()) {
					StompProtocol check = StompProtocol();
					string frame = check.Connect(sent1.at(2),sent1.at(3));
					const short bufsize = 1024;
    				char buf[bufsize];
					std::string Packet(frame);
					connectionHandler->sendFrameAscii(Packet,'\0');
					connectionHandler->user=sent1.at(2);
    		times = false;
			thread_InputManager thread_active = thread_InputManager(connectionHandler);
			std::thread t1(&thread_InputManager::InputManager,&thread_active);
    		std::thread t2(&thread_InputManager::ReadFromSerever,&thread_active);
			t1.join();
    		t2.join();
			delete connectionHandler;
			}
			}
			else{
				std::cerr << "Please login before!";
			}
		}
		catch(std::exception& e){
			    std::cerr << "Could not connect to server";
		}
	}
	
}