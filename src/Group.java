import java.util.LinkedList;


public class Group {
	private String name;
	private LinkedList<String> member;
	public Group(){
		name = new String();
		member = new LinkedList<String>();
	}
	public void set_name(String name){
		this.name = name;
	}
	public String get_name(){
		return this.name;
	}
	public LinkedList<String> get_member(){
		return this.member;
	}
	public void print(){
		System.out.println("-----print group-----");
		System.out.println(name);
		for(String dest:member){
			System.out.print(dest + " ");
		}
		System.out.println("\n------end group------");
	}
}
