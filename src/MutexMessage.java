
public class MutexMessage extends Message {
	public MutexKind type;
	public MutexMessage(Message recv) {
		super(recv);
		type = MutexKind.Init;
	}
	public void set_type(MutexKind type){
		this.type = type;
	}
	public MutexKind get_type(){
		return this.type;
	}
	
}
