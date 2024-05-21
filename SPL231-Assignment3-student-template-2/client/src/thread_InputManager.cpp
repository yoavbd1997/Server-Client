#include "../include/thread_InputManager.h"
#include "../include/StompProtocol.h"
#include <thread>
#include <chrono>
#include <thread>

thread_InputManager::thread_InputManager(ConnectionHandler *hanlder_):handler(hanlder_){}

void thread_InputManager::InputManager(){
  bool check =true;
  while(check){
		const short bufsize = 1024;
    char buf[bufsize];
    std::cin.getline(buf, bufsize);
		std::string line(buf);
    StompProtocol response = StompProtocol();
    if(response.splitByDelimiter(line,' ').at(0)!="login" && !handler->connected){
      std::cout<<"please login before"<<std::endl;
    }
    else if(response.splitByDelimiter(line,' ').at(0)=="report"){
      response.CheckSendFrame(line , handler->myUniqueId ,handler);
    }
    else{
    string send = response.CheckSendFrame(line , handler->myUniqueId ,handler);
        if(response.splitByDelimiter(send,'\n').at(0)=="CONNECT"){
          handler->close();
          handler->connect();
          handler->connected=true;
        }
        if (!handler->sendFrameAscii(send,'\0')) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
            check =false;
        }
  } 
  }
}

void thread_InputManager::ReadFromSerever(){
    bool check = true;
    while(check){
      std::string answer;
       // std::cout << "Before get a message"<< std::endl;
       while(!handler->connected){}
        if (handler->getFrameAscii(answer,'\0')){
        //std::cout << "Getting message"<< std::endl;
                StompProtocol response = StompProtocol();
        //std::cout << answer<< std::endl;
              std::cout << "\nThe response is:\n" +response.CheckGetFrame(answer ,handler) << std::endl;      
              try{
            string check = response.splitByDelimiter(answer,'\n').at(0);
            }
          catch(std::exception& e){}
        }
        else{
        check = false;
        }
        
    
  }
}