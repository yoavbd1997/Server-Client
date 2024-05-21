#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include "../include/event.h"
#include <iostream>
#include <string>
#include <fstream>
//#include <json.hpp>
#include <typeinfo>
#include <unordered_map>
#include <iterator>
#include <map>
#include <fstream>
#include <filesystem>
#include <string>
#include <exception>
using std::string;
#include <cctype>
#include <algorithm>

string StompProtocol::Connect(string &user,string &passcode){
    string frame ="CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:" + user +"\npasscode:" + passcode;
    return frame;
}
string StompProtocol::CheckSendFrame(string &ans, int uniqueId ,ConnectionHandler *handler){
    string Header_title = splitByDelimiter(ans,' ').at(0); 

    if(Header_title=="join"){
        string id_message = handler->user + std::to_string(handler->counter);
        handler->counter = handler->counter+1;
        handler->idToTopic[id_message] = splitByDelimiter(ans,' ').at(1);
        handler->topicToId[splitByDelimiter(ans,' ').at(1)]=id_message;
        string frame= "SUBSCRIBE\ndestination:/" +splitByDelimiter(ans,' ').at(1)+"\nid:" + id_message;
        handler->receiptToResponse[std::to_string(handler->counter)]="Joined channel " +splitByDelimiter(ans,' ').at(1);
        frame = frame + "\nreceipt:" + std::to_string(handler->counter);
        handler->counter = handler->counter +1;
        return frame;
}

    else if(Header_title=="exit"){
        string id_message = handler->user + std::to_string(handler->counter);
        string uniuqeID= handler->topicToId[splitByDelimiter(ans,' ').at(1)];
        std::cout<<uniuqeID <<std::endl;
        string frame= "UNSUBSCRIBE\nid:"+uniuqeID;
        handler->receiptToResponse[std::to_string(handler->counter)]="Exit channel " +splitByDelimiter(ans,' ').at(1);
        frame = frame + "\nreceipt:" + std::to_string(handler->counter);
        handler->counter = handler->counter +1;
        
        return frame;
    }
    else if(Header_title=="login"){
       string userName = splitByDelimiter(ans,' ').at(2);
       string pass = splitByDelimiter(ans,' ').at(3);  
       string frame="CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:"+userName+"\npasscode:"+pass;
       return frame;
    }
     else if(Header_title=="logout"){
       string receipt= return_receipt(handler);
       string frame="DISCONNECT\nreceipt:" + receipt;
       handler->connected=false;
      // handler->close();
       return frame;
    }
    else if(Header_title=="report"){
     string str(splitByDelimiter(ans,' ').at(1));
    names_and_events a = parseEventsFile(str);
    for(int i=0;i<((int)a.events.size());i++){
    string frame = "SEND\n";
    frame = frame+ "destination:/" + a.team_a_name + "_" + a.team_b_name + "\n\n";
    frame = frame + "user: " + handler->user + "\n";
    frame = frame + "team a: " + a.team_a_name + "\nteam b: " + a.team_b_name + "\n";
    int time = a.events.at(i).get_time();
    frame = frame + "event name: " +a.events.at(i).get_name() +"\ntime: " + std::to_string(time)+"\ngeneral game updates: \n";
   // if(a.events.at(i).get_game_updates().count("active") >0){
     //       frame = frame + "   " + "active: "+ a.events.at(i).get_game_updates().at("active");
   // }
    //if(a.events.at(i).get_game_updates().count("before halftime") >0){
      //      frame = frame + "   " + "before halftime: "+ a.events.at(i).get_game_updates().at("before halftime");
    //}
      for (const auto& pair : a.events.at(i).get_game_updates()) {
        frame=frame+"   " + pair.first +": "+ pair.second +"\n";
    }
    frame = frame + "\nteam a updates: \n";
      for (const auto& pair : a.events.at(i).get_team_a_updates()) {
        frame=frame+"   " + pair.first +": "+ pair.second +"\n";
    }
      frame = frame + "\nteam b updates: \n";
      for (const auto& pair : a.events.at(i).get_team_b_updates()) {
        frame=frame+"   " + pair.first +": "+ pair.second +"\n";
    }
    frame = frame + "description:\n" + a.events.at(i).get_discription();
    handler->sendFrameAscii(frame,'\0');
    }
        return " ";
    }
     else if(Header_title=="summary"){

        std::vector<std::string> filename=splitByDelimiter(ans, ' ');
        string userName = filename.at(2);
        string topic = filename.at(1);
        string name = filename.at(3);
        std::fstream file(name, std::ios::in);
   
      std::map<int,string> sorted = sortByTime(handler->userToDetails[userName]);
        string team_a =splitByDelimiter(topic,'_').at(0);
        string lower_caseA = team_a;
        lower_caseA[0]=std::tolower(team_a[0]);
        string team_b =splitByDelimiter(topic,'_').at(1);
        string lower_caseB = team_b;
        lower_caseB[0]=std::tolower(team_b[0]);
        string part1 =makeSummary1(sorted,lower_caseA,lower_caseB);
       string frame_begin= lower_caseA + " vs " + lower_caseB +  "\nGame stats:\nGeneral stats:\nactive: false\nbefore halftime: false\n";
       string frame_total =frame_begin + part1 + "Game event reports:\n";
       for (const auto& kv : sorted) {
         frame_total = frame_total + makeSummary2( std::to_string(kv.first) , kv.second , topic);
       }
        if (file.is_open()) {
            file.close();
    
            std::ofstream outfile(name, std::ios::trunc);
            outfile << frame_total;
            outfile.close();
        }
        else {
        std::ofstream outfile(name);
        outfile << frame_total;
        outfile.close();
         
       }
     }
 return " ";

}
 std::map<int,string> StompProtocol::sortByTime(vector<string> events){
   std::map<int,string> sorted;
   for(int i=0;i<((int)events.size());i++){
     vector<string> headers =splitByDelimiter(events.at(i),'\n');
     for(int j=0;j<((int)headers.size());j++){
        try{
           if(splitByDelimiter(headers.at(j),':').at(0)=="time"){   
            sorted[std::stoi(splitByDelimiter(headers.at(j),':').at(1).substr(1))] = events.at(i);
           }
        }
        catch(std::exception& e){

        }
  
     }
     
     
   }
   return sorted;
  }
  string StompProtocol::makeSummary2(string time, string event, string game){
   vector <string> headers = splitByDelimiter(event ,'\n');
   string event_name ="";
    for(int i=0;i<((int)headers.size());i++){
      try{
        vector <string> row = splitByDelimiter(headers.at(i),':'); 
       if(row.at(0)=="event name"){
            event_name = row.at(1).substr(1);
       }
    }
     catch(std::exception& e){}
    } 
     string frame ="";
     string nameEvent=splitByDelimiter(headers.at(3),':').at(1).substr(1);
    
   if(nameEvent==game){
  
     frame = time + " - "+event_name +":\n\n";
    for(int i=0;i<((int)headers.size());i++){
      try{
        vector <string> row = splitByDelimiter(headers.at(i),':'); 
       if(row.at(0)=="description"){
            frame = frame + headers.at(i+1)+"\n\n\n";
       }
    }
     catch(std::exception& e){}
    } 

   }
   return frame;
  }
///
string StompProtocol::CheckGetFrame(string &ans ,ConnectionHandler *handler){

    string Header_title = splitByDelimiter(ans,'\n').at(0); 
    if(Header_title=="CONNECTED") { 
        string Header_title = splitByDelimiter(ans,'\n').at(1);
        handler->connected=true;
        if(Header_title.at(0)=='v'){
            return "Login successful";         // string returner = splitByDelimiter(ans,'\n').at(1);

        } 
        else{
            return Header_title;
        }
    
    }
    else if (Header_title=="MESSAGE") {
          vector<string> curr = splitByDelimiter(ans , '\n');
          string name_user = "";
          for(int i=1;i<((int)curr.size());i++){
            vector<string> pairs = splitByDelimiter(curr.at(i),':');
            try{
            if(pairs.at(0)=="user") {
                name_user = pairs.at(1).substr(1);
      
             }
            }
            catch(std::exception& e){}
          }
   
          if(handler->userToDetails.count(name_user)>0){
             handler->userToDetails[name_user].push_back(ans); //space
             
          }
          else{
            vector<string> toAdd;
            toAdd.push_back(ans);
            handler->userToDetails[name_user]=toAdd;
          }
          
          return ans;
     }
    else if (Header_title=="RECEIPT") { 
            string msg = splitByDelimiter(ans , ':').at(1);
            string response = handler->receiptToResponse[msg];
            return response;
            }
    else if(Header_title=="ERROR")  {
        handler->connected=false;
       // handler->close();
        vector<string> Headers = splitByDelimiter(ans,'\n');
        for(string element : Headers){
            string check = splitByDelimiter(element,':').at(0);
            if(check=="message")
                return splitByDelimiter(element,':').at(1);
        }

        //do nothing bcz the connection is close
     }
    
    return "";


}


vector<string> StompProtocol::splitByDelimiter(string &str, char delimiter){
    std::vector<string> tokens;
    std::stringstream ss(str);
    string token; 
    while(std::getline(ss,token,delimiter)){
       tokens.push_back(token);
    }
    return tokens;
  }

string StompProtocol::return_receipt(ConnectionHandler *handler){
    string id_message = std::to_string(handler->counter);
    handler->counter = handler->counter+1;
    return id_message;
}
string StompProtocol::makeSummary1(std::map<int,string> data, string nameA , string nameB){
  std::map<string,string> team_a;
  std::map<string,string> team_b;
   string part_frame ="";
   for (const auto& kv : data) {
         string event = kv.second;
         vector<string> rows = splitByDelimiter(event,'\n');
         
         for(int i=0;i<((int)rows.size());i++){
          try{
             if(rows.at(i)=="team a updates: "){
              int j =i;
              while(rows.at(j+1).substr(0,2)=="  "){
                vector<string> keys_val = splitByDelimiter(rows.at(j+1),':');
                team_a[keys_val.at(0).substr(2)]=keys_val.at(1);
                j=j+1;
              }
             }
          }
          catch(std::exception &e){}
         }
         
         for(int i=0;i<((int)rows.size());i++){
          try{
             if(rows.at(i)=="team b updates: "){
              int j =i;
              while(rows.at(j+1).substr(0,2)=="  "){
                vector<string> keys_val = splitByDelimiter(rows.at(j+1),':');
                team_b[keys_val.at(0).substr(2)]=keys_val.at(1);
                j=j+1;
              }
             }
          }
          catch(std::exception &e){}
         }
       }

       
        vector<string> keys1;
          for (const auto& kv : team_a) {
          keys1.push_back(kv.first);
        }
        part_frame = nameA + " stats:\n";
        std::sort(keys1.begin(),keys1.end());
        for(int k=0;k<((int)keys1.size());k++){
          part_frame = part_frame + keys1.at(k).substr(1) + ":" + team_a[keys1.at(k)] +"\n";
        }
  

       
         vector<string> keys;
          for (const auto& kv : team_a) {
          keys.push_back(kv.first);
        }
        std::sort(keys.begin(),keys.end());
        part_frame = part_frame + nameB + " stats:\n";
        for(int k=0;k<((int)keys.size());k++){
          part_frame = part_frame + keys.at(k).substr(1) + ":" + team_b[keys.at(k)] +"\n";
        }
        return part_frame;

}
bool StompProtocol::compareKeys(const std::string& a, const std::string &b){
  return a<b;
}

