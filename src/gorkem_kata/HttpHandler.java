package gorkem_kata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class HttpHandler {
	
	private BufferedReader requestMessage;
	private DataOutputStream responseMessage;
	private String requestType;
	private String filePath;
	private String fileType;
	private FileInputStream requestedFile;
	private String responseHeaderLocal;
	private String firstLine;
	private Socket connection;
	private static String currentPath;
	
	public HttpHandler(Socket connectionSocket)
	{
		try
		{
			this.connection=connectionSocket;
			requestMessage = new BufferedReader (new InputStreamReader(connection.getInputStream()));
			responseMessage = new DataOutputStream( connection.getOutputStream());
			firstLine = requestMessage.readLine();
		    currentPath = System.getProperty("user.dir");
			createAdminPage();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// Bu class ýn ana metodu. Parse edilmiþ request e göre response verir. Request in parse edilmesi ise class ýn diðer metodlarý tarafýndan yapýlýr.
	public void generateResponse()
	{
		try {
			MainServer.getLogs().append("\nFirst line of the request is: \n" + firstLine + "\n***************************");
			if(firstLine == null)
			{
				responseMessage.close();
				connection.close();
				return;
			}
			if(this.isRequestValid())
			{
				try
				{
					requestType = this.getRequestType();
					filePath = this.getRequestedFilePath();
					fileType = this.getFileType();
					if(fileType == null)
					{
						filePath = "index.html";
						fileType = "html";
					}
					MainServer.getLogs().append("\nRequest Type: " + requestType + "\nFile type: " + fileType +"\nFile Path: |" + filePath + "|\n*****************");
				}
				catch (IOException e2)
				{
					e2.printStackTrace();
				}

				if(requestType == "GET")
				{
					if(this.isFileTypeSupported(fileType))
					{
					 try
					 {
						if( filePath.equals("admin.html") && !this.checkAdminCrediantials() )
						{
							filePath = "index.html";
						}
						else if(filePath.equals("admin.html") && this.checkAdminCrediantials())
						{
							filePath = "adminPage.html";
						}
						if(filePath.equals("adminPage.html"))
						{
							String fileToDelete=this.getFileToDelete();
							if(fileToDelete != null)
							{
								File deleteFile = new File(currentPath+"/"+fileToDelete);
								deleteFile.delete();
								this.createAdminPage();
							}
						}
						requestedFile = new FileInputStream(filePath);
						responseMessage.writeBytes(this.createResponseHeader(200, fileType, requestType));
						byte [] fileReadBuffer = new byte[1024];
						int writtenBytes=1;
						int readBytes;
						Boolean isEndOfFile=false;
						MainServer.getLogs().append("\nWriting requested file to output...\n");
						while(!isEndOfFile)
						{
							readBytes = requestedFile.read(fileReadBuffer, 0, 1024);
							if(readBytes == -1)
							{
								isEndOfFile=true;
								MainServer.getLogs().append("\nBytes written:" + writtenBytes + " kB\n");
							}
							else
							{
							responseMessage.write(fileReadBuffer, 0, readBytes);
							MainServer.getLogs().append("#");
							}
							if(writtenBytes % 50 == 0)
							{
								MainServer.getLogs().append("\n");
							}
							writtenBytes++;
						}
						responseMessage.close();
						connection.close();
						
					 }
					 catch (FileNotFoundException e)
					 {
						try
						{
							responseMessage.writeBytes(this.createResponseHeader(404, "", ""));
							connection.close();
						} 
						catch (IOException e1)
						{
							e1.printStackTrace();
						}
					 }
					}
					else
					{
						responseMessage.writeBytes(this.createResponseHeader(415, "", ""));
						connection.close();
					}
				}
				else
				{
					responseMessage.writeBytes(this.createResponseHeader(200, "", ""));
					responseMessage.close();
					connection.close();
				}
				
			}
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		


		
	}
    
	// Request type ý belirler.
	public String getRequestType() throws IOException
	{
		String requestType=null;
		String firstLineUPPER = firstLine.toUpperCase();
		String firstWord = firstLineUPPER.substring(0, firstLine.indexOf(' '));// ilk indisle ilk boþluðun indisinin arasýndaki yer request type týr
		switch(firstWord)
		{
		case "GET": requestType="GET"; break;
		case "HEAD": requestType="HEAD"; break;
		case "POST": requestType="POST"; break;
		default: requestType="UNKNOWN";
		}
		return requestType;
	}
	
	public String getRequestedFilePath() throws IOException
	{
		String filePath = null;
		int indexOfSecondSpace = firstLine.indexOf(' ',firstLine.indexOf(' ')+1);
		int indexOfParameters = firstLine.indexOf("?");
		int begin = firstLine.indexOf(' ')+2; // ilk boþluktan 2 karakter sonra dosya adý baþlar. örn: GET /DOSYA_ADI 
		int end = indexOfSecondSpace; // parametre yoksa 2. boþluktan önceki indiste dosya adý biter.
		if(end > indexOfParameters && indexOfParameters != -1) 
		{
			end = indexOfParameters; //parametre var ise dosya adý parametlerin baþlangýcýndan hemen önce bitmiþtir.
		}
		filePath = firstLine.substring(begin, end);
		String encodedFiletPath = URLDecoder.decode(filePath, "UTF-8");
		return encodedFiletPath;
		
	}
	
	public String getFileType() throws IOException
	{
		String fileType=null;
		String filePath=this.getRequestedFilePath();
		for(int index=filePath.length()-1; index>-1 ; index--)
		{
			if(filePath.charAt(index) == '.')
			{
				fileType=filePath.substring(index+1); // file pathte noktadan sonraki kýsým file type olur. path in içinde nokta olamayacaðý için bunu kontrol etmiyoruz.
				break;
			}
						
		}
		return fileType;
	}
	
	public Boolean isRequestValid() throws IOException
	{
		Boolean isValid=true;
		String filePath=null;
     	filePath = this.getRequestedFilePath();
		if(filePath.indexOf("..") != -1 || filePath.indexOf("/.") != -1 || filePath.indexOf("./") != -1 ) // iki nokta kullanarak server makinesinin diðer directory lerine ulaþmasýlmasýný engellemek için.
			isValid=false;                                                                                // bunu yapmazsak örneðin /../../gizli_dosya.docx yaparak 2 üst directory e eriþebilir client.
		return isValid;
	}
	
	
	// Kendisine gönderilen parametlerle bir response header hazýrlar.
	public String createResponseHeader(int statusCode, String fileType, String requestType)
	{
		MainServer.getLogs().append("\nCreating response header...");
		String responseHeader = "HTTP/1.1 ";
		switch(statusCode)
		{
		case 200: responseHeader += "200 OK \r\n"; break;
		case 403: responseHeader += "403 Forbidden \r\n"; break;
		case 404: responseHeader += "404 Not Found \r\n"; break;
		case 415: responseHeader += "415 Media Type \r\n"; break;
		case 500: responseHeader += "500 SERVER ERROR \r\n"; break;
		}
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		responseHeader += "Date: " + dateFormat.format(date) + " \r\n";
		responseHeader += "Server: Kata Server V 1.0 \r\n";
		responseHeader += "Connection: close \r\n";
		if(statusCode == 200 && requestType == "GET")
		{
		   switch(fileType)
		   {
		   case "jpg":
		   case "jpeg": responseHeader += "Content-Type: image/jpg \r\n"; break;
		   case "txt":
		   case "html": responseHeader += "Content-Type: text/html; charset:UTF-8 \r\n"; break;
		   case "gif": responseHeader += "Content-Type: image/gif \r\n"; break;
		   case "ico": responseHeader += "Content-Type: image/ico \r\n"; break;
		   case "avi": responseHeader += "Content-Type: video/x-msvideo \r\n"; break;
		   case "flv": responseHeader += "Content-Type: video/x-flv \r\n"; break;
		   case "docx": responseHeader += "Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document \r\n"; break;
		   }
		}
		responseHeaderLocal = responseHeader;
		responseHeader += "\r\n";
		MainServer.getLogs().append("\nResponse Header Is Created");
		return responseHeader;
	}
	
	// dosya türü destekleniyor mu?
	public Boolean isFileTypeSupported(String fileType)
	{
		switch(fileType.toLowerCase())
		{
		case "jpg":
		case "jpeg":
		case "html":
		case "txt":
		case "avi":
		case "flv": 
		case "ico":
		case "docx":
			return true;
		default: break;
		}
		return false;
	}
	
	// response header ý konsola yazdýrmak için
	public String getResponseHeader()
	{
		return responseHeaderLocal;
	}
	
	// admin baðlantýsýný check eder.
	public Boolean checkAdminCrediantials()
	{
		Boolean canLogin = false;
		if(firstLine.indexOf("usr=admin") != -1 && firstLine.indexOf("pwd=1234") != -1)
		{
			canLogin=true;
		}
		return canLogin;
	}

	// silinecek dosyanýn path ini alýr requestten. 
	public String getFileToDelete(){
		String fileToDelete = null;
		int indexOfParameter = firstLine.indexOf("delete_target");// bu parametreden sonra dosya adý geliyor.
		if(indexOfParameter != -1)
		{
			int indexOfDot = firstLine.indexOf(".");
			int indexOfFileNameEnd = firstLine.indexOf(' ',indexOfDot);
			System.out.println(indexOfParameter);
			System.out.println(indexOfDot);
			System.out.println(indexOfFileNameEnd);
			fileToDelete = firstLine.substring((indexOfParameter+14),indexOfFileNameEnd);
			fileToDelete=fileToDelete.replace('+', ' '); // dosya adýnda boþluk varsa bunlar requestte + þeklinde oluyor. Onu düzeltmek için.
			System.out.println(fileToDelete);
		}
		
		return fileToDelete;
		
	}
	
	// admin page in html ini create eder.
	public void createAdminPage(){
		ArrayList<String> fileList = new ArrayList<String>();
		File folder = new File(currentPath);
		File[] listOfFiles = folder.listFiles();
		    for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile()) {
		    	int dotIndex = listOfFiles[i].getName().indexOf(".");
		    	String fileType = listOfFiles[i].getName().substring(dotIndex+1);
		    	if(this.isFileTypeSupported(fileType))
		    		fileList.add(listOfFiles[i].getName());
		      }
		    }
		    File adminPage = new File(currentPath+"/adminPage.html");

		    try {
			    adminPage.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(adminPage));
				writer.write(
						"<!DOCTYPE html>                                           "+
					"	<html>                                                     "+
					"	<head>                                                     "+
					"   <title>Kata Web Server</title>                             "+
					"   </head>                                                    "+
					"	<body>                                                     "+
					"   <h1>KATA WEB SERVER ADMIN</h1>                             "+
					"   <form id=\"selection\" action=\"adminPage.html\" method=\"get\">      "+
					"	SELECT THE FILE TO DELETE IT<br>                "+
				    "	<br>                                                       "+
					"	<h2>Files in the server:</h2>                              "
					);
					for(int i=0;i<fileList.size();i++){
						writer.write(
					"File: " + fileList.get(i) +" <button type=\"submit\"           "+
				    "name=\"delete_target\" value=\""+fileList.get(i)+"\">Delete</button><br>"
								);
					}
					
					writer.write(
					"	</form>                                                    "+
					"	</body>                                                    "+
					"	</html> "
		
						);
				
				writer.close();
				
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		    	
	}

}
