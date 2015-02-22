import java.io.IOException;
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
	public static Queue<Message> pending = new LinkedList<Message>();
	public State state; // true = held; false = released
	public boolean voted; // true = voted; false = not voted yet

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
		System.out.println("pls sinput the type of clock you want to use:");
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
			if( kind.compareToIgnoreCase("vote") != 0){		
	
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
			else{
				vote(dest,kind,data);
			}
			//System.out.println("seq = "+to_send.get_seq());
		}
	}

	public static void vote(String dest,String kind,String data){
		
	}
	public void run() {
		while(true){
			if( mp != null){
				Message recv = null;
				recv = mp.receive();
				if( recv != null){
					boolean verbose = (recv.get_group().compareToIgnoreCase("ALL") != 0);

					if(recv.get_kind().compareToIgnoreCase("mul") != 0)
						System.out.println("[RECV]	"+recv.get_src() +":"+ recv.get_data().toString());
					else{
						if(verbose)
							System.out.println("[RECVmulticast] " + recv.get_mul_timestamp().toString() + "  "+recv.get_origin()+" : sent by "+recv.get_src() +"  "+ recv.get_data().toString());
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
								// TODO Auto-generated catch block
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
