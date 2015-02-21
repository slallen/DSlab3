import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;


public class Userapplication implements Runnable{
	// When user create message: should call set_seqnumber, set_dest,set_kind
	// server port should choose to what?
	public static MessagePasser mp;
	private static LogicClockService logic_clock;
	private static String clock_type;
	public static LinkedList<Message> recv_mul = new LinkedList<Message>();
	
	public Userapplication(){
	}
	public static void main(String[] args) throws IOException{
		//String configuration_addr = "/home/chenshuo/18842/lab0/configuration";
		String configuration_addr;
		String type;
		String localname;
		Scanner scanner = new Scanner(System.in);
		System.out.println("input config file link:");
		configuration_addr = scanner.nextLine();
		System.out.println("input local name:");
		localname = scanner.nextLine();
		System.out.println("input the type of clock you want to use:");
		type = scanner.nextLine();
		mp = new MessagePasser(configuration_addr,localname);
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

			Message to_send = new Message(localname,dest,kind,data);
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
				recv_mul.add(to_send);
				mp.multicast(to_send,dest);
			}
			else{
				mp.send(to_send);
			}
			//System.out.println("seq = "+to_send.get_seq());
		}
	}

	public void run() {
		while(true){
			if( mp != null){
				Message recv = mp.receive();
	
				if( recv != null){
					if(recv.get_kind().compareToIgnoreCase("mul") != 0)
						System.out.println("[RECV]	"+recv.get_src() +":"+ recv.get_data().toString());
					else
						System.out.print("[RECVmulticast]	"+recv.get_origin()+" : sent by "+recv.get_src() +"  "+ recv.get_data().toString());
				
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
								mp.multicast(recv,recv.get_group());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							recv_mul.add(recv);
							System.out.println(" [continue multicast]");
						}
						else{
							System.out.println(" [already multicasted]");
						}
					}
				}
			}
		}
	}
}
