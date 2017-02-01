/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;
private String pathName;
private Boolean locExists;
private Boolean locDirectory;
private File checkFile;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      writeHTTPHeader(os,"text/html"); //change header type of request
      writeContent(os);                
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
   String line;
   int count = 0;
   
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   
   while (true) {
	   
	   count++;
	   
      try {
    	  
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
          
         //execute once
         if (count == 1)
         {
                 
           String strAr[] = line.split(" "); 
           checkFile = new File(strAr[1].substring(1)); 
           
           //If Root Directory
           if(strAr[1].equals("/")) 
           {
              locExists = true; 
              locDirectory = true;
           }//root
           
           //If File Found
           else if(checkFile.exists())
           {
              locExists = true;
              locDirectory = false;
           }//found
           
           //If File Not Found
           else
           {
              locExists = false;
              locDirectory = false; 
           }//not found
           
           pathName = line.substring(5, line.indexOf(' ', 4)); //global variable with path
         }
         
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   
   if (locExists == true)
      os.write("HTTP/1.1 200 OK\n".getBytes());
   else 
      os.write("HTTP/1.1 404 File Not Found\n".getBytes());
   
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Charles' very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception
{
		//if the html file exists in the given location
		if ((locExists == true) && (locDirectory == false))
		{     
	         String fileContents;
	         
	         //Conversion of pathName to appropriate format
	         FileInputStream fis = new FileInputStream(pathName);
	         DataInputStream dis = new DataInputStream(fis);
	         InputStreamReader isr = new InputStreamReader(dis);
	         BufferedReader br = new BufferedReader(isr);
	   
	         while ((fileContents = br.readLine()) != null) 
	         {
	            //If html file has the tag <cs371date> write date
	            if (fileContents.contains("<cs371date>"))
	               os.write(new Date().toString().getBytes());
	      
	            //If html file has the tag <cs371server> write statement
	            else if (fileContents.contains("<cs371server>"))
	               os.write("You are at the Server (Port: 8080)".getBytes());
	            //write html file contents
	            else
	               os.write(fileContents.getBytes());
	   
	         }  
	   }
	   
	   //If there is no location given other than local host port.
	   else if ((locExists == true) && (locDirectory == true))
	   {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<center><h3>Charles Choi's Web Server</h3></center>\n".getBytes());
			os.write("</body></html>\n".getBytes());
	   }
	   //If location was given but does not exist.
	   else
	      os.write("<center><h3>404 - File Not Found\n</h3></center>\n".getBytes());   
}	

} // end class
