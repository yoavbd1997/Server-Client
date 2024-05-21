#pragma once

#include <string>
#include <iostream>
#include <boost/asio.hpp>
#include "../include/ConnectionHandler.h"
using std::string;
using std::vector;
#include <vector>

// TODO: implement the STOMP protocol
class StompProtocol
{
private:

public:
    string Connect(string &user,string &passcode);
    string CheckSendFrame(string &ans, int uniqueId,ConnectionHandler *handler);
    std::map<int,string> sortByTime(vector<string> events);
    string makeSummary2(string time, string game_name, string user);
    string CheckGetFrame(string &ans ,ConnectionHandler *handler);
    vector<string> splitByDelimiter(string &s, char delimiter);
    string return_receipt(ConnectionHandler *handler);
    string makeSummary1(std::map<int,string> data,string teamA,string teamB);
     bool compareKeys(const std::string& a, const std::string &b);


};
