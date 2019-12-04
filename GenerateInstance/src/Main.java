import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Stack;


class StreamDrainer implements Runnable {  
	private InputStream ins;  

	public StreamDrainer(InputStream ins) {  
		this.ins = ins;  
	}  

	public void run() {  
		try {  
			BufferedReader reader = new BufferedReader(  
					new InputStreamReader(ins));  
			String line = null;  
			while ((line = reader.readLine()) != null) {  
				System.out.println(line);  
			}  
		} catch (Exception e) {  
			e.printStackTrace();  
		}  
	}  

}  

public class Main {
	public static String workPath = "c:/test/";
	public static String sourcePath = "areca-7.4.7";
	public static String classPath = "c:/test/classes/";
	public static String staticFuncPath = "C:/test/staticFunc/";
	public static String jarPath = "c:/test/test.jar";
	public static String evosuitePath = "c:/test/evosuite-1.0.3.jar";
	public static int index =0;
	public static void main(String[] args) throws IOException {
		long time1=System.nanoTime(); 
		//runOneCommand("java -jar"+ evosuitePath+"-generateSuite -Dsearch_budget = 240 -Dstopping_condition = 240" );

		parseFiles(new File(sourcePath));

		generateClass(new File(staticFuncPath));

		runOneCommand("jar cvf "+jarPath+" -C "+classPath+" .");
		generateInstances(new File(classPath));
		long time2=System.nanoTime(); 
		System.out.println(time2-time1);

	}
	public static void generateClass(File f) throws IOException{
		File[] files = f.listFiles();
		for(File file:files){
			if(file.isDirectory()){
				generateClass(file);
			}else{
				runOneCommand("javac -d "+classPath+" "+staticFuncPath+file.getName());			
			}
		}
	}




	public static void generateInstances(File f) throws IOException{
		File[] files = f.listFiles();
		for(File file:files){
			if(file.isDirectory()){
				parseFiles(file);
			}else{
				System.out.println(file.getName().substring(0,file.getName().lastIndexOf(".class")));
				runOneCommand("java -jar "+evosuitePath+" -class "+file.getName().substring(0,file.getName().lastIndexOf(".class"))+" -projectCP "+jarPath);
				//runOneCommand("java -jar "+evosuitePath+" -generateSuite "+jarPath);

			}
		}
	}
	public static int runOneCommand(String string){
		try{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(string,null,new File(workPath));

			new Thread(new StreamDrainer(proc.getInputStream())).start();  
			new Thread(new StreamDrainer(proc.getErrorStream())).start();  

			proc.getOutputStream().flush();  
			proc.getOutputStream().close(); 

			int exitVal = proc.waitFor();
			return exitVal;
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	public static void parseFiles(File f) throws IOException{
		File[] files = f.listFiles();
		for(File file:files){
			if(file.isDirectory()){
				parseFiles(file);
			}else{
				parseFile(file);
			}
		}
	}
	public static void parseFile(File file) throws IOException{
		boolean flag = false;
		ArrayList<String> inLinesList = getLines(file.getPath());
		ArrayList<String> outLinesList = new ArrayList<String>();


		Stack<Character> stack = new Stack<Character>();
		for(int i=0;i<inLinesList.size();i++){
			if(inLinesList.get(i).contains("static")){
				if(inLinesList.get(i).contains("(") && inLinesList.get(i).contains("{")){
					outLinesList.clear();
					outLinesList.add("//"+file.getPath());
					parseOneLine(inLinesList.get(i), stack);

					for(int i1=0;i1<inLinesList.size();i1++){
						if(inLinesList.get(i1).startsWith("import")){

							String className=getClassName(inLinesList.get(i1));
							System.out.println(className);
							//if(className.equals("com.beust.jcommander.JCommander")){
							//	System.out.println("sdf");
							//}
							boolean existClassName=isExistClassName(className);
							if(existClassName)
							{
								outLinesList.add(inLinesList.get(i1));
							}





							//if(inLinesList.get(i1).startsWith("import com.sun")||inLinesList.get(i1).startsWith("import java")||inLinesList.get(i1).startsWith("import javax")||inLinesList.get(i1).startsWith("import org.ietf")||inLinesList.get(i1).startsWith("import org.omg")||inLinesList.get(i1).startsWith("import org.w3c")||inLinesList.get(i1).startsWith("import org.xml"))

							//{
							//outLinesList.add(inLinesList.get(i1));
							//}



							//outLinesList.add(inLinesList.get(i1));
						}
					}
					outLinesList.add("public class class"+index+" {");
					flag=false;
					for(int i1=0;i1<inLinesList.size();i1++){
						if(inLinesList.get(i1).contains("public class") && inLinesList.get(i1).contains("{")){
							flag=true;
							continue;
						}
						if(flag==true ){
							if(inLinesList.get(i1).contains("{") && inLinesList.get(i1).contains("(")
									&&((inLinesList.get(i1).contains("private"))||(inLinesList.get(i1).contains("public")))		){
								break;
							}else{
								outLinesList.add(inLinesList.get(i1));
							}
						}
					}
					outLinesList.add(inLinesList.get(i));
				}
			}else if(stack.isEmpty()==false){
				parseOneLine(inLinesList.get(i), stack);
				outLinesList.add(inLinesList.get(i));
				if(stack.isEmpty()==true){
					outLinesList.add("}");
					outLines(staticFuncPath+"class"+index+".java", outLinesList);
					index++;
				}

			}else{
				continue;
			}			
		}

	}
	public static void parseOneLine(String line, Stack<Character> stack){
		if(line.contains("{") || line.contains("}")){
			for(int i=0;i<line.length();i++){
				if(line.charAt(i)=='{'){			
					stack.add(line.charAt(i));
				}else if(line.charAt(i)=='}'){
					stack.pop();
				}else{
					continue;
				}
			}
		}		
	}
	public static ArrayList<String> getLines(String inFileName) throws IOException{
		ArrayList<String> linesList = new ArrayList<String>();
		BufferedReader bufReader = new BufferedReader(new FileReader(inFileName));
		String line;
		while ((line = bufReader.readLine()) != null) {
			linesList.add(line);
		}
		bufReader.close();
		return linesList;
	}
	public static void outLines(String outFileName,ArrayList<String> linesList) throws IOException{


		FileWriter fileWriter = new FileWriter(outFileName);
		BufferedWriter bufWriter = new BufferedWriter(fileWriter);


		for(int i=0;i<linesList.size();i++){
			bufWriter.write(linesList.get(i));
			bufWriter.newLine();
		}

		fileWriter.flush();
		bufWriter.flush();

		bufWriter.close();
		fileWriter.close();
	}

	public static String getClassName(String string)
	{

		int startIndex=string.indexOf(" ");
		int endIndex=string.lastIndexOf(";");

		String newstring=string.substring(startIndex+1, endIndex);

		return newstring;

	}

	public static boolean isExistClassName(String string)
	{
		String sql = "select * from funstatic";
		ArrayList<String> arrayList = DBUtil.select(sql);

		if(string.contains("*"))
		{
			String thestring=string;
			
			int endIndex=string.indexOf(".*");
			thestring=string.substring(0, endIndex);
			for(int i=3;i<arrayList.size();i=i+4){

				String temp = arrayList.get(i);
				if(temp.contains(".")){
					temp = temp.substring(0,temp.lastIndexOf("."));
					if(temp.equals(thestring)){
						return true;
					}
				}else{
					continue;
				}
			}
		}else{
			for(int i=3;i<arrayList.size();i=i+4){
				if(arrayList.get(i).equals(string)){
					return true;
				}				
			}
		}


		return false;

	}
}
