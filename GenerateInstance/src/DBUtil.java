import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
public class DBUtil {
	public static Connection connect;

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver"); 
		}catch (Exception e) {
			System.out.print("Error loading Mysql Driver!");
			e.printStackTrace();
		}
		try {
			DBUtil.connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/test","root","root");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public static void insert(String sqlString){
		Statement st=null;
		try {
			st=DBUtil.connect.createStatement();
			st.executeUpdate(sqlString);
			st.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static ArrayList<String> select(String sqlString){
		ArrayList<String> list = new ArrayList<String>();
		try{
			Statement stmt = connect.createStatement();
			ResultSet rs = stmt.executeQuery(sqlString);
			while (rs.next()){
				list.add(rs.getString(1));
				list.add(rs.getString(2));
				list.add(rs.getString(3));
				list.add(rs.getString(4));
			}
			rs.close();
			stmt.close();			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return list;
	}
	public void finalize()
	{
		try {
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}