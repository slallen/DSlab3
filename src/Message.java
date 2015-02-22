import java.io.Serializable;

public class Message implements Serializable{
	private static final long serialVersionUID = 1L;
	public static int seq_num = 1; // global seq number starting with 1
	
	public static int seq_multi = 0;
	private String origin;
	public TimeStamp mul_start_timestamp;
	
	private String group;
	private String src;
	
	private String dest;
	private String kind;
	private int seq;
	private int multi_seq;
	private Object data;
	private boolean duplicate = false;
	private boolean send_delay = false;
	private boolean log = false;
	
	public Message(String src,String dest, String kind, Object data){
		this.src = src;
		this.dest = dest;
		this.kind = kind;
		this.data = data;
	}
	public Message(Message recv) {
		this.src = recv.get_src();
		this.dest = recv.get_dest();
		this.kind = recv.get_kind();
		this.seq = recv.get_int_seq();
		this.data = recv.get_data();
		this.log = recv.get_log();
		this.origin = recv.get_origin();
		this.multi_seq = recv.multi_seq;
		this.group = recv.get_group();
		this.mul_start_timestamp = recv.mul_start_timestamp;
		this.duplicate = recv.get_duplicate();
		this.send_delay = recv.get_send_delay();
	}
	public boolean if_already_recv(Message k){
		boolean ret = (MessagePasser.local_name == k.origin) || ((this.multi_seq == k.multi_seq) &&( 0 == this.origin.compareToIgnoreCase(k.origin))
				); 
		return ret;
	}
	public void set_log(boolean b){
		this.log = b;
	}
	public boolean get_log(){
		return this.log;
	}
	public TimeStamp get_mul_timestamp(){
		return this.mul_start_timestamp;
	}
	public void set_mul_timestamp(TimeStamp now){
		this.mul_start_timestamp = now;
	}
	public void set_dest(String dest){
		this.dest =dest;
	}
	public void set_source(String source){
		this.src = source;
	}
	public void set_seqMulti(){
		this.multi_seq = seq_multi;
		seq_multi ++;
	}
	public void set_seqNum(){
		this.seq = seq_num;
		seq_num ++;
	}
	public void set_seqNum(int sequenceNumber){
		this.seq = sequenceNumber;
	}
	public void set_duplicate(boolean dup){
		this.duplicate = dup;
	}
	public void set_send_delay(boolean send_delay){
		this.send_delay = send_delay;
	}
	public void set_origin(String origin){
		this.origin = origin;
	}
	public void set_group(String group){
		this.group = group;
	}
	public String get_group(){return group;}
	public String get_origin(){return origin;}
	public String get_src(){return src;}
	public String get_dest(){return dest;}
	public String get_kind(){return kind;}
	public int get_int_seq(){return seq;}
	public String get_seq(){return String.valueOf(seq);}
	public boolean get_duplicate(){return duplicate;}
	public boolean get_send_delay(){return send_delay;}
	public Object get_data(){return data;}
	public void show(){
		System.out.println("-------------");
		System.out.println(src);
		System.out.println(dest);
		System.out.println(kind);
		System.out.println(this.seq);
		System.out.println(data);
		System.out.println("-------------");
	}
}
