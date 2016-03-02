package com.khizhny.spamsender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import com.khizhny.spamsender.Mail;

public class MainActivity extends Activity {

	private String csvPath;
	private String userName;
	private String password;
	private String smtpServer;
	private String smtpPort;
	private String sender;
	private String attachmentPath;
	private int processed;
	private int blockSize;
	private int blockSendDelay;
	private boolean working;
	private static Context context;
	private Button addImage;

	Thread thread = new Thread(new Runnable(){
		@Override
		public void run() {
			working=true;
			// reading emails,subjects and body from file
			InputStream in = null;
			try {
				in = new FileInputStream(csvPath);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				Log.e("MailApp", "File emails not found", e1);
				working=false;
			}
			String line;
			String messageBody="";
			String dstAddress="";
			Mail m = new Mail(userName, password,smtpServer,smtpPort);
			if (in!=null) {
				BufferedReader reader= new BufferedReader(new InputStreamReader(in));
				Boolean timeToSend = false;  // we will set up this flag when file is finished
				for (int i=1;i<10000;i++){ /// i - quantity of emails sent
					line="";
					try {  // reading new message from file
						line = reader.readLine();
						if (line.equals("<<New Mail>>")){  // new message marker found
							m = new Mail(userName, password,smtpServer,smtpPort);
							m.setFrom(sender);
							timeToSend=false;
						}
						if (line.substring(0, 3).equals("To:")){
							dstAddress=line.substring(3);
							m.setTo(dstAddress.split(";"));
						}
						if (line.substring(0, 8).equals("Subject:")){
							m.setSubject(line.substring(8));
						}
						if (line.equals("<<Body Start>>")){
							while (!line.equals("<<Body End>>") ) {
								line=reader.readLine();
								if (!line.equals("<<Body End>>")) {
									messageBody=messageBody+line+(char)10;
								}
							}
							m.setBody(messageBody);
							timeToSend=true;
						}
						if (timeToSend){
							try {
								m.addAttachment(attachmentPath);
							} catch (Exception e) {
								showToast("Could not attach file "+ attachmentPath);
							}
							try {
								m.send();
								showToast("Sending message #"+processed+ " to " + dstAddress);
							} catch (Exception e) {
								showToast("Could not send email #"+processed+" to "+ dstAddress);
							}
							timeToSend=false;
							processed=processed+1;
							if (i%blockSize==0) {
								try {
									Thread.sleep(blockSendDelay);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
					catch (Exception e)
					{
						working=false;
						i=10000;
						addImage.setText("Finished.");
						try {
							reader.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						try {
							wait();
						} catch (InterruptedException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
				}
			}
			working=false;
		}
	});

	public void openMailFile(){
		File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
		FileDialog fileDialog = new FileDialog(this, mPath);
		fileDialog.setFileEndsWith(".txt");
		fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
			public void fileSelected(File file) {
				//Log.d(getClass().getName(), "selected file " + file.toString());
				SharedPreferences settings =PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
				csvPath=settings.getString("csv_file_path", "/storage/sdcard0/emails.txt");
			}
		});
		//fileDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
		//  public void directorySelected(File directory) {
		//      Log.d(getClass().getName(), "selected dir " + directory.toString());
		//  }
		//});
		//fileDialog.setSelectDirectoryOption(false);
		fileDialog.showDialog();
	}

	@Override
	public void onCreate(Bundle icicle) {
		context=this;
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);
		// Restore preferences
		working=false;
		SharedPreferences settings =PreferenceManager.getDefaultSharedPreferences(this);
		csvPath=settings.getString("csv_file_path", "/storage/sdcard1/emails.txt");
		userName=settings.getString("userName", "khizhny.tester@gmail.com");
		password=settings.getString("password", "khizhny.tester1");
		smtpServer=settings.getString("smtpServer", "smtp.gmail.com");
		smtpPort=settings.getString("smtpPort", "465");
		sender=settings.getString("sender", "khizhny.tester@gmail.com");
		attachmentPath=settings.getString("attachmentPath", "/storage/sdcard1/info.txt");
		blockSize=Integer.parseInt(settings.getString("blockSize","2"));
		blockSendDelay=Integer.parseInt(settings.getString("blockSendDelay", "5"));
		addImage = (Button) findViewById(R.id.sendEmail);
		addImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				processed=0;
				if (working==false){
					addImage.setText("Working...");
					thread.start();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		// test
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, PrefActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	public void showToast(final String toast)
	{
		runOnUiThread(new Runnable() {
			public void run()
			{
				Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
			}
		});
	}
}
