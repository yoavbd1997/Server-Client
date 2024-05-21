#pragma once

#include "../include/ConnectionHandler.h"

// TODO: implement the STOMP protocol
class thread_InputManager
{
private:

public:
    thread_InputManager(ConnectionHandler *handler);
    ConnectionHandler * handler;
    void InputManager();
    void ReadFromSerever();
};
