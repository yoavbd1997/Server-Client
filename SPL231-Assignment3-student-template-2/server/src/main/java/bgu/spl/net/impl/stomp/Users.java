package bgu.spl.net.impl.stomp;

import java.util.HashMap;

public class Users {
   
    public String user;
    public String passcode;
    public boolean connection_state = false;
    //public Integer myUinqueId=-1;
    public HashMap<String,String> IdSubscribeToTopic=new HashMap<>();
    public HashMap<String,String> topicToIdSubscribe=new HashMap<>();
    public Users(String user, String passcode){
        this.user = user;
        this.passcode = passcode;
    }

    public void Connect(){
        connection_state = true;
    }

    public void Disconnect(){
        connection_state = false;
    }

    public String Get_passcode(){
        return passcode;
    }

    public boolean GetState(){
        return connection_state;
    }
}
