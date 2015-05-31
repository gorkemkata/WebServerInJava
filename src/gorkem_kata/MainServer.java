package gorkem_kata;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultCaret;

public class MainServer{
	private ServerSocket serverSocket;
	private HttpHandler httpHandler;
	private static JFrame mainWindow;
	private static JPanel mainPanel;
	private static JScrollPane logsScroll;
	private static JTextArea logs;
	private DefaultCaret caret;
	public MainServer()
	{
		mainWindow = new JFrame("KATA WEB SERVER");
		mainPanel = new JPanel();
		mainPanel.setLayout(null);
		logsScroll = new JScrollPane();
		logs = new JTextArea();
		logs.setBounds(20, 90, 450, 300);
		logs.setEditable(false);
		logs.setLineWrap(true);
		logs.setWrapStyleWord(true);
	    caret = (DefaultCaret)logs.getCaret();
	    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	    logsScroll.setBounds(20, 90, 450, 300);
	    logsScroll.setViewportView(logs);
	    mainPanel.add(logsScroll);
	    mainWindow.add(mainPanel);
	    mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	    mainWindow.setSize(500, 430);
	    mainWindow.setVisible(true);
	    mainWindow.setResizable(false);
	    mainWindow.setLocationRelativeTo(null);
		this.run();
	}
	public void run()
	{
		try 
		{
			serverSocket = new ServerSocket(80);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		while(true)
		{
			try
			{
				logs.append("\n -------- WAITING FOR NEW CONNECTION ---------");
				Socket connectionSocket = serverSocket.accept(); // connection dinliyoruz
				logs.append("\n ------------- NEW CONNECTION ---------------");
				logs.append("\n Address Of Connection: " + connectionSocket.getInetAddress().getHostAddress());
				logs.append("\n Name Of The Host: " + connectionSocket.getInetAddress().getHostName());
				logs.append("\n---------------------------------------------");
				httpHandler = new HttpHandler(connectionSocket); // connection gelirse bunu handle etmesi için iþi HttpHandle a býrakýyoruz.
				httpHandler.generateResponse(); // HttpHandler response üretip gönderiyor.
				logs.append("\n*************************\nResponse Header: \n" + httpHandler.getResponseHeader());
				logs.append("\n----------------RESPONSE IS SENT----------\n\n");
				logs.setCaretPosition(logs.getText().length());
				
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static JTextArea getLogs(){
		return logs;
	}


}
