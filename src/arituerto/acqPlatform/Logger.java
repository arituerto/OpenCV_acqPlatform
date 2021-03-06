package arituerto.acqPlatform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Logger {

	private BufferedOutputStream stream;
	
	public Logger(String filename) throws FileNotFoundException {
		this.stream = new BufferedOutputStream(new FileOutputStream(filename));
	}
	
	public Logger(File f) throws FileNotFoundException {
		this.stream = new BufferedOutputStream(new FileOutputStream(f));
	}
	
	public void log(String s) throws IOException {
		// logs string needs to include the timestamp
		this.stream.write(s.getBytes());
		this.stream.write(System.lineSeparator().getBytes());
		this.stream.flush();
	}
	
	public void close() throws IOException {
		this.stream.close();
	}
}
