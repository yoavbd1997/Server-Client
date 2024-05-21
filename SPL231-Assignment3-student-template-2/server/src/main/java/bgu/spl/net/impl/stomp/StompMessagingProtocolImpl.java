package bgu.spl.net.impl.stomp;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class StompMessagingProtocolImpl<T> implements StompMessagingProtocol<T> {
    public int id;
    public StompConnection<T> connect;
    public boolean shouldTerminate = false;

    @Override
    public void start(int connectionId, StompConnection<T> connections) {
        id = connectionId;
        this.connect = connections; 
        
    }

    @Override
    public void process(T message) {
        // System.out.println("userToId");
        // System.err.println(connect.userToId.keySet());
        // System.err.println(connect.userToId.values());
        // System.out.println("---");
        // System.out.println("IdTouser");
        // System.err.println(connect.idToUser.keySet());
        // System.err.println(connect.idToUser.values());
        // System.out.println("---");
        // System.out.println("listTopic");
        // System.err.println(connect.listTopic.keySet());
        // System.err.println(connect.listTopic.values());
    
        if(message!=null){
        String mes =(String)message;
        String Header = mes.split("\n")[0];

        String [] Headers = mes.split("\n");
        String receipt_id ="-1";
        if(Headers.length>1){ }
        for(int i=0; i< Headers.length;i++){
            if(Headers[i].equals("receipt")) {receipt_id = Headers[i].split(":")[1];  }
        }


        if(Header.equals("CONNECT")) {
            ConnectFrame(message,connect); }
        else if(connect.idToUser.containsKey(id)){ //Already connected
            if (Header.equals("SEND")) {SendFrame(message,receipt_id); }
            else if (Header.equals("SUBSCRIBE")) {SubscribeTFrame(message,receipt_id); }
            else if (Header.equals("UNSUBSCRIBE")) { UnSubscribeTFrame(message,receipt_id); }
            else if (Header.equals("DISCONNECT")) {DisconnectFrame(message,receipt_id); }
                                                        }
        else{
            //error
            shouldTerminate=false;
        }   
    }
}

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void ConnectFrame(T message, StompConnection<T> connect){
        if(((String) message).split("\n").length<4){
            String summary = "Missing arguments for connection";
            connect.ERROR1((String)message, "-1", summary, id);
        }
        else{
        String login_user = ((String) message).split("\n")[3];//third row
        String user = login_user.split(":")[1];//username

        String login_passcode = ((String) message).split("\n")[4];//forth row
        String passcode = login_passcode.split(":")[1];//password
        
        if(connect.checkLogin(user,passcode,id,(String) message)){
            String frame = "CONNECTED" + "\n" +"version:1.2" + "\n";
            connect.send(id, (T) frame);

        }
        else{
            shouldTerminate=false;
        }
    }
    }
    //SendFrame - done
    public void SendFrame(T message, String receipt_id){
        if(((String) message).split("\n").length>1){
        boolean check = false;
          
        String destination_topic = Arrange_message((String)message, "destination"); //row second
        if(!connect.nameUserToUsers.get(connect.idToUser.get(id)).topicToIdSubscribe.containsKey(destination_topic.substring(1))){
            connect.ERROR1((String)message, receipt_id,"you need to join this channel first!", id);
        }
        else if(destination_topic.equals("")){
            String summary = "malformed frame received - missing destination" +check_receipt(message) ;
            connect.ERROR1((String)message, receipt_id,summary, id);
            shouldTerminate=true;
        }
        else{
            String msg = " ";
            int index = ((String)message).indexOf("\n\n");
            if (index >= 0) {
                msg = ((String)message).substring(index + 2); 
                }
                System.err.println(msg + "msg in sendframe");
            connect.send(destination_topic, (T)msg);
        }
    }
        else{
            String summary = "malformed frame received - missing required header" +check_receipt(message);
            connect.ERROR1((String)message, receipt_id,summary , id);
        }

    }

    public void UnSubscribeTFrame(T message, String receipt_id){
      //  String unsub_id = ((String) message).split("\n")[1];
        String idNumber =  Arrange_message((String)message, "id");
        String user_name = connect.idToUser.get(id);
        String channel_name = connect.nameUserToUsers.get(user_name).IdSubscribeToTopic.get(idNumber); //Give the name of the channel;
        try{
        connect.listTopic.get(channel_name).remove(user_name); //remove the user from the channel
        connect.nameUserToUsers.get(user_name).IdSubscribeToTopic.remove(idNumber); // Delete the channel from HashMap Users class;
        connect.nameUserToUsers.get(user_name).topicToIdSubscribe.remove(channel_name); // Delete the channel from HashMap Users class;
        String frame = "RECEIPT";
        String receipt_ids =  Arrange_message((String)message, "receipt");
        frame = frame + "\nreceipt-id:" +receipt_ids; 
        connect.send(id, (T) frame);
        // if(check_receipt(message)!=""){
        //   String frame = "RECEIPT" +"\n"+ check_receipt(message);
        //   System.out.println("Unsubscribe succssed" + " Exited channel " + channel_name);
        //   connect.send(id,(T)frame);
        // }
    }
    catch(Exception e){}
    
     
    }
    public void SubscribeTFrame(T message, String receipt_id){
        System.err.println(message);
        
      //  String headers = ((String) message).split("\n")[1];//second row
        
        String destination = Arrange_message((String)message, "destination");//topic
        String frame = "";
     //   String id_subscribe = ((String) message).split("\n")[2];//third row
        String idUniuqe = Arrange_message((String)message, "id");//id
       
        if(connect.listTopic.containsKey(destination.substring(1))){//check if the channel/topic exist
            if(connect.listTopic.get(destination.substring(1)).contains(connect.idToUser.get(id))){//the user is already subscribed to this channel
                System.err.println("The client already joined channel" + " " + destination + check_receipt(message)); 
               // connect.send(id, (T) frame);
            }
            else{//the user isnt subscribe yet   
            connect.listTopic.get(destination.substring(1)).add(connect.idToUser.get(id));//add the username to the channel
            connect.nameUserToUsers.get(connect.idToUser.get(id)).IdSubscribeToTopic.put(idUniuqe, destination.substring(1));//add the uniuqe id to the hashmap in the Users class
            connect.nameUserToUsers.get(connect.idToUser.get(id)).topicToIdSubscribe.put(destination.substring(1),idUniuqe );//add the uniuqe id to the hashmap in the Users class
            String receipt_ids =  Arrange_message((String)message, "receipt");
            frame = frame + "RECEIPT\nreceipt-id:" +receipt_ids;
            connect.send(id, (T) frame);

            // System.err.println( connect.listTopic.get(destination.substring(1)).size());
            // System.err.println( connect.listTopic.get(destination.substring(1)).size());
            // if(check_receipt(message)!=""){
            //   frame="RECEIPT" +"\n" + check_receipt(message); 
            //   connect.send(id, (T) frame);
            // }
          }
        }
        else{  //if the topic doesent exist
            List <String> toAdd=new LinkedList<>();//new list
            toAdd.add(connect.idToUser.get(id));//add the username to that list
            connect.nameUserToUsers.get(connect.idToUser.get(id)).IdSubscribeToTopic.put(idUniuqe, destination.substring(1));//add the uniuqe id to the hashmap in the Users class
            connect.nameUserToUsers.get(connect.idToUser.get(id)).topicToIdSubscribe.put(destination.substring(1),idUniuqe );//add the uniuqe id to the hashmap in the Users class
            
            connect.listTopic.put(destination.substring(1), toAdd);//add the new topic to the hash
            int g =connect.listTopic.get(destination.substring(1)).size();
            String receipt_ids =  Arrange_message((String)message, "receipt");
            frame = frame + "RECEIPT\nreceipt-id:" +receipt_ids; 
            connect.send(id, (T) frame);
            // if(check_receipt(message)!=""){
            //     frame="RECEIPT" +"\n" + check_receipt(message); 
            //     System.err.println("subscribe : Joined channel" + " " + destination );
            //     connect.send(id, (T) frame);
            //   }

        }
        int k = connect.listTopic.get(destination.substring(1)).size();
    }
 
    
    public void DisconnectFrame(T message, String receipt_id){
        //idtouser
        //usertoid
        //users: connected=false;
        //users : id=-1;
        //delete from all the channels
        //        connect.nameUserToUsers.remove(user_name); //Delete user from Userlist;
        String user_name = connect.idToUser.get(id);//username
        for(String key: connect.listTopic.keySet()){//check all the topics
            if(connect.listTopic.get(key).contains(user_name)){//if the username in this specipic topic so delete it from the list
                connect.listTopic.get(key).remove(user_name);
            }
        }
        try{
        connect.nameUserToUsers.get(connect.idToUser.get(id)).topicToIdSubscribe.clear();
        connect.nameUserToUsers.get(connect.idToUser.get(id)).IdSubscribeToTopic.clear();
        }
        catch(Exception e){
            
        }
        connect.userToId.remove(user_name);
        connect.idToUser.remove(id);
        //String reciept = ((String) message).split("\n")[1];//second row
        String recieptId = Arrange_message((String)message, "receipt");//uniuqe id
        String frame = "RECEIPT" + "\n" + "receipt-id : "+ recieptId;
        connect.nameUserToUsers.get(user_name).Disconnect();
        connect.send(id, (T)frame);
        connect.disconnect(id);
        shouldTerminate = true;


    }

    public String check_receipt(T msg){
        String [] Headers = ((String) msg).split("\n");
        for(int i=1;i<Headers.length;i++){
            if(Headers[i].split(":")[0].equals("receipt")){
                return "\nreceipt-id:" + Headers[i].split(":")[1];
            }
        }
        return "";
    }

    public String Arrange_message(String msg,String search){
       try{
        String [] Headers = msg.split("\n");
        for(int i=1;i<Headers.length;i++){
            if(Headers[i].length()!=0){
            if(Headers[i].split(":")[0].equals(search)){
                return Headers[i].split(":")[1];
              }
            }
        }
            }catch(Exception e){}
                    return "";
    }


}
