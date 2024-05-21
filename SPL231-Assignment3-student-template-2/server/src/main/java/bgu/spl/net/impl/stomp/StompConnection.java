package bgu.spl.net.impl.stomp;

import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.HashMap;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.NonBlockingConnectionHandler;

public class StompConnection <T> implements Connections<T>  {
    public int count=0;
    public int GetidUser=0;
    List <ConnectionHandler<T>>  Handlers=new LinkedList<>(); //List of ConnectionHandlers ; 
    HashMap<Integer,ConnectionHandler<T>> HashConnect =new HashMap<Integer,ConnectionHandler<T>>(); //Map id<->connectionHandler;

    HashMap<String,Integer> userToId=new HashMap<String,Integer>(); //map name user to id;
    HashMap<Integer,String> idToUser=new HashMap<Integer,String>(); //map id to user name;
    public int counterMessageId=100;
    HashMap<String,Users> nameUserToUsers=new HashMap<String,Users>(); //map user name to user class;
    public Integer uniqueIdClient=1;
    //
    HashMap<String,List<String>> listTopic=new HashMap<>(); 
    public StompConnection(){       
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> curr=HashConnect.get(connectionId);
        curr.send(msg);
        return false;
    }

    @Override
    public void send(String channel, T msg) {
        String topic =channel.substring(1); 
       List <String> toSend = listTopic.get(topic);
       for(int i=0;i<toSend.size();i++){
        String toSent="MESSAGE\nsubscription:" + nameUserToUsers.get(toSend.get(i)).topicToIdSubscribe.get(topic)+"\nmessage-id:" +counterMessageId;
        counterMessageId = counterMessageId+1;
        toSent = toSent+"\ndestination:" + channel +"\n\n" + (String)msg;
        HashConnect.get(userToId.get(toSend.get(i))).send((T)toSent);
       }
        
        
    }

    @Override
    public void disconnect(int connectionId) {
         ConnectionHandler toRemove = HashConnect.get(connectionId); 
         HashConnect.remove(connectionId); //Remove from Hashconnect(id<->handler)
         Handlers.remove(toRemove); //Remove from Handler list;
        if(toRemove instanceof StompConnectionsHandler){
         try {
            toRemove.close();
        } catch (IOException e) {
            System.err.println("disconnect");;
        }
    }
    }

    public  void addHandlers(ConnectionHandler<T> con){
        Handlers.add(con);
        HashConnect.put(count, con);
        count = count+1;

    }

    public boolean checkLogin(String user, String passcode,int id, String msg){
        if(nameUserToUsers.containsKey(user)){//the username is in

            if(nameUserToUsers.get(user).Get_passcode().equals(passcode)){//the password is correct
                if(nameUserToUsers.get(user).GetState()){//check if the user/client online
                    if(userToId.get(user)==id){
                         String frame ="User already log in"; 
                         nameUserToUsers.get(user).Disconnect();
                         ERROR1(msg,"-1",frame,id);
                 }
                    else {
                        String frame = "CONNECTED" + "\n" +"The client is already logged in, log out before trying again";
                         send(id, (T)frame);
                 }
                   
                    return false;
                }
                else{//not connected
                    nameUserToUsers.get(user).Connect();//set to connect in users
                    idToUser.put(id, user);//sync
                    userToId.put(user, id);//sync
                    return true;//need to send a frame in protocol
                }
            }

            else{//wrong password
                String frame="Wrong password";
                nameUserToUsers.get(user).Disconnect();
                ERRORCONNECTION(msg,"-1",frame,id);
                return false;
            }

        }
        else{//Create new user
            Users clinet = new Users(user, passcode);
            nameUserToUsers.put(user, clinet);
            nameUserToUsers.get(user).Connect();
            idToUser.put(id, user);
            userToId.put(user, id);
         //   String unique = Integer.toString(uniqueIdClient);
         //   unique = unique +"\n";
          //  Handlers.get(id).send((T)unique);
          //  nameUserToUsers.get(idToUser.get(id)).myUinqueId=uniqueIdClient;  
        //    uniqueIdClient++;
            return true;
        }
   
    }

    public void ERRORCONNECTION(String message,String recipte_id, String summary_problem,int id){
        String frame="ERROR" +"\n" ;
        frame =frame + "message:" + summary_problem + "\n" + "\n" + "The message:\n" + "-----\n" + message;
        HashConnect.get(id);
        HashConnect.get(id).send((T)frame);
        ConnectionHandler toRemove = HashConnect.get(id); 
        if(toRemove instanceof StompConnectionsHandler){

        try {
            HashConnect.get(id).close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
 
        }
            }
        HashConnect.remove(id); //Remove from Hashconnect(id<->handler)

    }
    
    public void ERROR1(String message, String recipte_id , String summary_problem,int id){
        String frame="ERROR";
        if(!recipte_id.equals("-1")){
            frame = frame + "\nreceipt-id:" + recipte_id +"\n";
        }
        frame =frame + "\nmessage:" + summary_problem + "\n" + "\n" + "The message:\n" + "-----\n" + message;
       
        String user_name = idToUser.get(id);//username
        for(String key: listTopic.keySet()){//check all the topics
            if(listTopic.get(key).contains(user_name)){//if the username in this specipic topic so delete it from the list
                listTopic.get(key).remove(user_name);
            }
        }
    
        try{
        nameUserToUsers.get(idToUser.get(id)).topicToIdSubscribe.clear();
        nameUserToUsers.get(idToUser.get(id)).IdSubscribeToTopic.clear();
        }catch(Exception e){}
        nameUserToUsers.get(user_name).Disconnect();
        HashConnect.get(id).send((T)frame);
        ConnectionHandler toRemove = HashConnect.get(id); 
       // HashConnect.remove(id); //Remove from Hashconnect(id<->handler)
        userToId.remove(user_name);
        idToUser.remove(id);
        if(toRemove instanceof StompConnectionsHandler){
        try {
            HashConnect.get(id).close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
        HashConnect.remove(toRemove); //Remove from Handler list;

    }
}
