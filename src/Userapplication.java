import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;


public class Userapplication implements Runnable{
	// When user create message: should call set_seqnumber, set_dest,set_kind
	// server port should choose to what?
	public static MessagePasser mp;
	private static LogicClockService logic_clock;
	private static String clock_type;
	public static LinkedList<Message> recv_mul = new LinkedList<Message>();

	public static State state; // true = held; false = released
	public static boolean voted; // true = voted; false = not voted yet
	public static int ack_count = 0;
	
	public static int send_count = 0;
	public static int recv_count = 0;
	
	public static ArrayList<Message> recv_req = new ArrayList<Message>();
	public static ArrayList<Message> already_handle = new ArrayList<Message>();
	public Userapplication(){
		voted = false;
		state = State.Released;
	}
	public static void main(String[] args) throws IOException{
		//String configuration_addr = "/home/chenshuo/18842/lab0/configuration";
		String configuration_addr;
		String type;
		String localname;
		Scanner scanner = new Scanner(System.in);

		System.out.println("input local name:");
		localname = scanner.nextLine();
		System.out.println("input the type of clock you want to use:");
		type = scanner.nextLine();
		mp = new MessagePasser(null,localname);
		mp.set_clockType(type);
		Userapplication listener = new Userapplication();
		new Thread(listener).start();
		System.out.println("start send (input format: dest \\n kind \\n data \\n log \\n)");
		while(true){
			boolean send_to_logger;
			String dest = scanner.nextLine();
			String kind = scanner.nextLine();
			String data = scanner.nextLine();
			String tolog = scanner.nextLine();	
			if( kind.compareToIgnoreCase("request") != 0 && kind.compareToIgnoreCase("release") != 0){		
	
				Message to_send = new Message(localname,dest,kind,data);
				MessagePasser.get_clock().UpdateTimeStamp(null);
				MessagePasser.get_clock().getTimeStamp().print_clock();
				if(0 == tolog.compareToIgnoreCase("true")) {
					to_send.set_log(true);
				}
				else{ 
					to_send.set_log(false);
				}
				if( kind.compareToIgnoreCase("mul") == 0){
					to_send.set_origin(localname);
					to_send.set_group(dest);
					to_send.set_seqMulti();
					to_send.set_mul_timestamp(MessagePasser.get_clock().getTimeStamp());
					recv_mul.add(to_send);
					boolean verbose = (to_send.get_group().compareToIgnoreCase("ALL") != 0);
					mp.multicast(to_send,dest,verbose);
				}
				else{
					mp.send(to_send,true);
				}
			}
			else if(kind.compareToIgnoreCase("request") == 0){
				request(dest,kind,data);
			}
			else if(kind.compareToIgnoreCase("release") == 0){
				release(dest,kind,data);
			}
		}
	}
	public static void release(String dest,String kind,String data) throws IOException{
		Message release = new Message(mp.local_name,dest,kind,data);
		release.set_origin(mp.local_name);
		release.set_group(dest);
		release.set_seqMulti();
		release.set_mul_timestamp(MessagePasser.get_clock().getTimeStamp());
		// this is a Release type message
		release.set_mutex_kind(MutexKind.Release);
		// set kind from "request" to "mul" because we want to use multicast
		release.set_kind("mul");
		recv_mul.add(release);
		mp.multicast(release,dest,true);
		state = State.Released;
		ack_count = 0;
		recv_count = 0;
	}
	public static void request(String dest,String kind,String data) throws IOException{
		/* TODO: should send timestamp here ? */
		send_count++;
		
		/* first send request to group members, using multicast */
		Message request = new Message(mp.local_name,dest,kind,data);
		request.set_origin(mp.local_name);
		request.set_group(dest);
		request.set_seqMulti();
		request.set_mul_timestamp(MessagePasser.get_clock().getTimeStamp());
		// this is a Request type message
		request.set_mutex_kind(MutexKind.Request);
		// set kind from "request" to "mul" because we want to use multicast
		request.set_kind("mul");
		
		System.out.println("********SEND REQUEST********");
		request.get_mul_timestamp().print_clock();
		System.out.println("**********************************");
		recv_mul.add(request);
		if( !voted && state != State.Held){
			ack_count ++;
			already_handle.add(request);
			System.out.println("I vote for myself first");
		}
		mp.multicast(request,dest,true);
		recv_count += ack_count;
		state = State.Wanted;
		while(state != State.Held){
			System.out.println("waiting for enough ACK's");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("********I have the resource********");
		System.out.println("send = " + send_count + " recv = " + recv_count);
		System.out.println("**********************************");
	}
	public void run() {
		while(true){
			if( mp != null){
				Message recv = null;
				recv = mp.receive();
				if( recv != null){
					boolean verbose =(recv.get_group()!=null) && (recv.get_group().compareToIgnoreCase("ALL") != 0);

					if( recv.get_mutex_kind() != MutexKind.NOT_A_MUTEX_MESSAGE){
						if(recv.get_mutex_kind() == MutexKind.Ack){
							String group_name = recv.get_group();
							Group t = mp.groups.get(group_name);
							int num = t.get_member().size();
							ack_count ++;
							if(state == State.Wanted && ack_count >= Math.sqrt(num)){
								state = State.Held;
							}
						}
						else if(recv.get_mutex_kind() == MutexKind.Release){
							boolean find = false;
							for(int i = 0;i < already_handle.size();i ++){
								Message t = already_handle.get(i);
								if(t.get_origin().compareToIgnoreCase(recv.get_origin()) == 0 && t.get_mul_seq() ==recv.get_mul_seq()){
									find = true;
									break;
								}
							}
							if(find) continue;
							
							if( !recv_req.isEmpty()){
								Message k = recv_req.remove(0);
								String info = new String("Ack from " + mp.local_name);
								Message ack = new Message(mp.local_name,k.get_origin(),"mutex_ack",info);
								ack.set_mutex_kind(MutexKind.Ack);
								ack.set_group(k.get_group());
								try {
									mp.send(ack, true);
								} catch (IOException e) {
									e.printStackTrace();
								}
								System.out.println("%%%%%%%%%%%");
								System.out.println("[RELEASE] I vote for " + k.get_origin());
								System.out.println("%%%%%%%%%%%");
								already_handle.add(recv);
								voted = true;
								//recv_count++;
							}
							else{
								voted = false;
							}
						}
						else if(recv.get_mutex_kind() == MutexKind.Request){
							boolean find = false;
							// due to multicast, if already in the queue, quit this loop
							for(int i = 0;i < recv_req.size(); i ++){
								Message t = recv_req.get(i);
								if(t.get_origin().compareToIgnoreCase(recv.get_origin()) == 0 && t.get_mul_seq() ==recv.get_mul_seq()){
									find = true;
									break;
								}
							}
							if(find) continue;
							
							// if already vote for this message, also quit
							find = false;
							for(int i = 0;i < already_handle.size();i ++){
								Message t = already_handle.get(i);
								if(t.get_origin().compareToIgnoreCase(recv.get_origin()) == 0 && t.get_mul_seq() ==recv.get_mul_seq()){
									find = true;
									break;
								}
							}
							if(find) continue;
							
							/* first insert it into request queue */
							find = false;
							for(int i = 0;i < recv_req.size(); i ++){
							// find a right pos to insert, sort by timestamp
								TimeStampedMessage t = (TimeStampedMessage)recv_req.get(i);
								TimeStamp current_time = t.get_mul_timestamp();
								if( current_time.compare(recv.get_mul_timestamp()) > 0){
									//find first member whose timestamp > timestamp of recv message
									recv_req.add(i,recv);
									find = true;
									break;
								}
							}
							if( !find ) recv_req.add(recv);
							System.out.println("$$$$$$$$$$$$$$$$$$$$");
							for(int i = 0;i < recv_req.size(); i ++){
								System.out.println(recv_req.get(i).get_origin());
								recv_req.get(i).get_mul_timestamp().print_clock();
							}
							System.out.println("$$$$$$$$$$$$$$$$$$$$");

							
							/* only when not voted yet enter next */
							if( !voted && state != State.Held){
								// get the first message from recv_req, who has the smallest timestamp
								Message k = recv_req.remove(0);
								String info = new String("Ack from " + mp.local_name);
								Message ack = new Message(mp.local_name,k.get_origin(),"mutex_ack",info);
								ack.set_mutex_kind(MutexKind.Ack);
								ack.set_group(k.get_group());
								try {
									mp.send(ack, true);
								} catch (IOException e) {
									e.printStackTrace();
								}
								System.out.println("%%%%%%%%%%%");
								System.out.println("[REQUEST] I vote for " + k.get_origin());
								System.out.println("%%%%%%%%%%%");
								voted = true;
								already_handle.add(k);
								//recv_count++;
							}
						}
					}
					if(recv.get_kind().compareToIgnoreCase("mul") != 0)
						System.out.println("[RECV]	"+recv.get_src() +":"+ recv.get_data().toString());
					else{
						if(verbose)
							System.out.println("[RECV multicast] " + recv.get_mul_timestamp().toString() + "  "+recv.get_origin()+" : sent by "+recv.get_src() +"  "+ recv.get_data().toString());
					}
					if(recv != null && recv.get_kind().compareToIgnoreCase("mul") == 0){
						boolean already = false;
						for(Message k : recv_mul){
							if( k.if_already_recv(recv) ){
								already = true;
								break;
							}
						}
						if( !already){
							try {
								mp.multicast(recv,recv.get_group(),verbose);
							} catch (IOException e) {
								e.printStackTrace();
							}
							recv_mul.add(recv);
							if(verbose)
							System.out.println(" [continue multicast]");
						}
						else{
							if(verbose)
							System.out.println(" [already multicasted]");
						}
					}
				}
			}
		}
	}
}
