package resourceManagers;

import java.io.BufferedReader;
import java.io.FileReader;

public class Reader {
	
	public String readFile() throws Throwable {

		try(BufferedReader br = new BufferedReader(new FileReader("Blank.txt"))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    String output = sb.toString();
		    return output;
		}
	}
 }
