﻿/**
 * 
 */
package com.tyt.bbs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tyt.bbs.adapter.PostListAdapter;
import com.tyt.bbs.entity.PostItem;
import com.tyt.bbs.parser.PostParser;
import com.tyt.bbs.utils.FileOperate;
import com.tyt.bbs.utils.Login;
import com.tyt.bbs.utils.Net;
import com.tyt.bbs.utils.Property;
import com.tyt.bbs.view.LoadingDrawable;

/**
 * @author tyt2011
 *com.tyt.bbs
 *这个页面主要为了显示各个版区
 */
public class PostsListActivity extends BaseActivity implements OnClickListener {

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	private ProgressBar mProgressBar;


	//判定上传类型
	private int postType=0; //0：是发纯文字  1：是发图片
	private ArrayList<String> filePath;
	private  final int DIALOG_REPLAY= 0x0;
	private  final int DIALOG_LOGIN = 0x1;
	private  final int NEED_LOGIN = 0x0;
	private final int LOAD_POSTS=0x3;
	private final int POST_PIC=0x1;
	private final int POST_PIC_DONE=0x2;

	private EditText replyContentEditText,passwordEditText,usernameEditText;
	private EditText replyTitleEditText;
	private View dv,loginLayout;
	private String user,pswd;
	private String board;
	private PostParser mParser;
	private PostListAdapter mAdapter;
	private ListView postList;
	private Boolean isThemeType;
	private TextView tv_readtype;
	private boolean mGetMore;
	private Toast mToast;
	private  SharedPreferences sp;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_postslist);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		isThemeType =true;
		mGetMore =true;
		initialView();
		initialListView();
		new PostsListload().execute();	

	}

	private void initialView(){
		board= getIntent().getStringExtra(Property.Boards);
		((TextView)findViewById(R.id.tv_boardinfo)).setText(board);
		mProgressBar=(ProgressBar)findViewById(R.id.progressbar);
		mProgressBar.setIndeterminateDrawable(new LoadingDrawable(0,Color.parseColor("#dF337fd3"), Color.parseColor("#0d337fd3"), Color.TRANSPARENT, 200));
		mProgressBar.setVisibility(View.GONE);

		tv_readtype = (TextView)findViewById(R.id.tv_readtype);
		tv_readtype.setText(isThemeType?R.string.themetype:R.string.normaltype);
		findViewById(R.id.btn_More).setOnClickListener(this);
		findViewById(R.id.btn_boardback).setOnClickListener(this);
		registerForContextMenu(findViewById(R.id.btn_More));
		dv = LayoutInflater.from(this).inflate(R.layout.dialog_reply, null);
		replyTitleEditText = (EditText)dv.findViewById(R.id.replaytitle);
		replyContentEditText = (EditText)dv.findViewById(R.id.replycontent);
		user = Property.getPreferences(this).getString(Property.UserName, "");
		pswd = Property.getPreferences(this).getString(Property.Password, "");
		loginLayout  = LayoutInflater.from(this).inflate(R.layout.dialog_login, null);
		usernameEditText = (EditText)loginLayout.findViewById(R.id.UserNameEditText);
		passwordEditText = (EditText)loginLayout.findViewById(R.id.PasswordEditText);
		usernameEditText.setText(user);
		passwordEditText.setText(pswd);
	}

	private void initialListView(){
		postList= ((ListView)findViewById(R.id.lv_postlist));
		postList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> ap, View v, int p,
					long arg3) {
				// TODO Auto-generated method stub
				//				if(isThemeType){
				PostItem tempdata=mAdapter.getItem(p);
				String link=tempdata.getLink();
				if(isThemeType&&link.contains("file=T."))
					link = link.replace("file=T.", "file=M.");
				if(isThemeType){
					link = link.replace(".html", "&showall=true");
					link = link.replace(",board,", "?board=");
					link = link.replace(",reid,", "&reid=");
				}
				Intent i=new Intent(PostsListActivity.this,ArticleActivity.class);
				i.putExtra("link", link);

				if(link.contains("reid")){
					int start =link.indexOf("reid")+5;
					int end = link.indexOf("&",start);
					if(end==-1) end = link.indexOf(",",start);
					if(end==-1) end = link.indexOf(".",start);
					i.putExtra("reid",link.substring(start,end));
				}
				i.putExtra("board",board);	
				startActivity(i);
			}

		});

		postList.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				mGetMore = false;
				if (firstVisibleItem + visibleItemCount == totalItemCount) {
					mGetMore = true;
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				if(mAdapter.getCount()>40)postList.setFastScrollEnabled(true);
				if (mGetMore && scrollState == OnScrollListener.SCROLL_STATE_IDLE&&mProgressBar.getVisibility()==View.GONE) {
					//添加滚动条滚到最底部，加载余下的数据
					// 执行线程异步更新listview的数据 可采用AsyncTask

					Tip("正在加载下一页，请稍后······");
					Message message  = new Message();
					message.what = LOAD_POSTS;
					replayHandler.sendMessage(message);
				}
			} });
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		switch(id){
		case DIALOG_REPLAY:
			final Dialog mDialog = new Dialog(this, R.style.Dialog);
			dv.findViewById(R.id.replysubmit).setOnClickListener(this);
			dv.findViewById(R.id.replyupload).setOnClickListener(this);
			dv.findViewById(R.id.paint).setOnClickListener(this);
			dv.setOnClickListener(this);
			mDialog.setContentView(dv);
			return mDialog;
		case DIALOG_LOGIN:
			final Dialog mLoginDialog = new Dialog(this,R.style.Dialog);
			loginLayout.findViewById(R.id.btn_dialoglogin).setOnClickListener(this);
			mLoginDialog.setContentView(loginLayout);
			return mLoginDialog;
		}
		return null;
	}

	// 获取文章列表 异步执行类
	private class PostsListload extends AsyncTask<String,Integer,Void>
	{


		@Override
		protected void onPreExecute() {
			mProgressBar.setVisibility(View.VISIBLE);
			postList.setClickable(false);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(String... URL) {
			if(mParser==null)
				mParser = new PostParser(board);

			try {
				mAdapter = new PostListAdapter(PostsListActivity.this,mParser.parser(isThemeType));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			postList.setAdapter(mAdapter);
			mProgressBar.setVisibility(View.GONE);
			postList.setClickable(true);
			((TextView)findViewById(R.id.tv_masterinfo)).setText(mParser.getBoardMasters());
			super.onPostExecute(result);
		}
	}



	private Runnable FreshListRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mProgressBar.setVisibility(View.VISIBLE);
			postList.setFastScrollEnabled(false);
			postList.setClickable(false);
			if(mParser==null)
				mParser = new PostParser(board);

			try {
				mAdapter.setPost(mParser.parser(isThemeType));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.i("Paser", e.toString());
			}
			mProgressBar.setVisibility(View.GONE);
			postList.setClickable(true);
		}

	};


	private class DoPost extends AsyncTask<String,Integer,String>
	{	


		@Override
		protected String doInBackground(String... arg0) {

			String result="";
			Message message  = new Message();
			//判断上传类型
			if(0==postType){
				result = newPost();

				if(result.contains("ERROR")){
					if(user.equalsIgnoreCase("")||pswd.equalsIgnoreCase("")){
						message  = new Message();
						message.what = NEED_LOGIN;
						replayHandler.sendMessage(message);
					}else{
						Login.getInstance().login(user, pswd);						
						result = newPost();
					}
				}

			}else if(1==postType){


				int size=filePath.size();
				for(int i=0;i<size;i++){

					String path=filePath.get(i);

					//批量上传提示
					message  = new Message();
					message.what = POST_PIC;
					Bundle data=new Bundle();
					data.putInt("index", i+1);
					data.putString("path", path);
					message.setData(data);
					replayHandler.sendMessage(message);

					result = newPostPic(path);

					//单次图片上传的检测
					if(result.contains("ERROR")){
						if(user.equalsIgnoreCase("")||pswd.equalsIgnoreCase("")){
							message  = new Message();
							message.what = NEED_LOGIN;
							replayHandler.sendMessage(message);
						}else{
							Login.getInstance().login(user, pswd);

							result = newPostPic(path);

						}
					}
					else{
						message  = new Message();
						message.what = POST_PIC_DONE;

						int head=result.indexOf("<font color=green>");
						int end=result.indexOf("</font>", head);
						String fileUrl=result.substring(head, end);
						head=fileUrl.indexOf("http");
						fileUrl=fileUrl.substring(head);

						data.putString("fileUrl", fileUrl);
						message.setData(data);
						Log.i("PostFile Url", fileUrl);
						replayHandler.sendMessage(message);
					}

				}
			}


			return result;

		}


		/**
		 *  执行post图片
		 * @return
		 * @throws IOException
		 */
		private String newPostPic(String path){

			//			java.io.File newFile=FileOperate.readPicFromSDcard("1310212944250061.jpg");

			java.io.File newFile = null;

			String result="";



			if(path==null) return "ERROR";
			try {
				newFile = FileOperate.readFromSDcardByPath(path);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				Log.v("", e1.toString());
			}
			if(newFile==null) return "ERROR";

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("MAX_FILE_SIZE","1048577"));
			params.add( new BasicNameValuePair("board",board));
			params.add( new BasicNameValuePair("level","0"));
			params.add( new BasicNameValuePair("live","180"));
			params.add( new BasicNameValuePair("exp",""));
			params.add( new BasicNameValuePair("up",newFile.getAbsolutePath()));
			params.add( new BasicNameValuePair("filename",newFile.getAbsolutePath()));

			try {
				return  Net.getInstance().postFile("https://bbs.sjtu.edu.cn/bbsdoupload", params);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.v("", e.toString());
				return "ERROR";
			}

		}

		/**
		 * 执行文本发帖
		 * @return
		 */
		private String newPost(){
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add( new BasicNameValuePair("title",replyTitleEditText.getText().toString()));
			params.add( new BasicNameValuePair("text",replyContentEditText.getText().toString()+(sp.getBoolean(Property.Post_Tail, true)?Property.Tail:"")));
			//			params.add( new BasicNameValuePair("text",replyContentEditText.getText().toString()+(sp.getBoolean(Property.Post_Tail, true)?Property.Tail:"")));
			params.add( new BasicNameValuePair("board",board));
			params.add( new BasicNameValuePair("file",""));
			params.add( new BasicNameValuePair("reidstr",""));
			params.add( new BasicNameValuePair("signature","1"));
			params.add( new BasicNameValuePair("autocr","on"));
			params.add( new BasicNameValuePair("live","180"));
			params.add( new BasicNameValuePair("level","0"));
			params.add( new BasicNameValuePair("exp",""));
			params.add( new BasicNameValuePair("MAX_FILE_SIZE","1048577"));
			params.add( new BasicNameValuePair("up",""));
			try {
				return Net.getInstance().post("https://bbs.sjtu.edu.cn/bbssnd", params);
			} catch (Exception e) {
				Log.v("", e.toString());
				return "ERROR";
			}
		}

		@Override
		protected void onPostExecute(String result) {

			if(result.contains("ERROR")){
				Toast.makeText(getApplicationContext(),"发帖失败，需要重新登录!",Toast.LENGTH_SHORT).show();
				Message message  = new Message();
				message.what = NEED_LOGIN;
				replayHandler.sendMessage(message);
			}
			else{
				restoreCookie();


				mParser.setMode(0);
				replayHandler.post(FreshListRunnable);
				Toast.makeText(getApplicationContext(),"发帖成功!",Toast.LENGTH_SHORT).show();

			}


			super.onPostExecute(result);
		}
	}

	private class DoGet extends AsyncTask<String,Integer,String>{

		protected void onPreExecute() {

			Toast.makeText(getApplicationContext(),"操作中...",Toast.LENGTH_SHORT).show();

		}


		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			Log.i("AddFavor", "doInBackground");
			String result=newGetAddFav();
			return result;
		}



		protected void onPostExecute(String result) {
			//			Log.i("AddFavor Get", result);
			Toast.makeText(getApplicationContext(),
					"收藏版区"+(result.contains("ERROR")?"失败":"成功"),
							Toast.LENGTH_SHORT).show();


			super.onPostExecute(result);
		}


		private String newGetAddFav(){

			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("newboard",board));
			params.add(new BasicNameValuePair("select","0"));
			params.add(new BasicNameValuePair("boardfile","main.hck"));

			try {
				return Net.getInstance().get("https://bbs.sjtu.edu.cn/bbsshowhome", params);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.v("", e.toString());
				return "ERROR";
			}
		}

	}

	private Handler replayHandler = new Handler() 
	{

		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
			case NEED_LOGIN:
				showDialog(DIALOG_LOGIN);
				break;
			case POST_PIC:
				Bundle data=msg.getData();
				Toast.makeText(getApplicationContext(),
						"上传图片( "+data.getInt("index")+" )"+"\n"+"路径："+data.getString("path"),
						Toast.LENGTH_LONG).show();
				break;
			case POST_PIC_DONE:
				Bundle info=msg.getData();
				replyContentEditText.append(info.getString("fileUrl")+"\n");
			case LOAD_POSTS:
				mGetMore = false;
				mParser.setMode(1);
				replayHandler.removeCallbacks(FreshListRunnable);
				replayHandler.post(FreshListRunnable);
				break;
			}
		}
	};

	private void restoreCookie(){
		if(user.equalsIgnoreCase("")||user.equalsIgnoreCase("")) return ;
		Editor edit=Property.getPreferences(this).edit();
		edit.putString(Property.UserName, user);
		edit.putString(Property.Password, pswd);
		edit.putString(Property.Cookie, Net.getInstance().getCookie());
		edit.putLong(Property.Cookie_Time, System.currentTimeMillis()/1000);
		edit.commit();
	}


	public void onCreateContextMenu(ContextMenu menu, View v,  
			ContextMenuInfo menuInfo) {  
		super.onCreateContextMenu(menu, v, menuInfo); 
		menu.setHeaderTitle("功能选择");
		getMenuInflater().inflate(R.menu.post_cm, menu);
	}  

	public boolean onContextItemSelected(MenuItem item) {  
		switch (item.getItemId()) {  
		case R.id.m_refresh:
			if(mProgressBar.getVisibility()==View.GONE){
				mParser.setMode(0);
				replayHandler.post(FreshListRunnable);
			}
			return true;
		case R.id.m_typeswitch:
			if(mProgressBar.getVisibility()==View.GONE){
				mParser.setMode(0);
				isThemeType=!isThemeType;
				tv_readtype.setText(isThemeType?R.string.themetype:R.string.normaltype);
				replayHandler.post(FreshListRunnable);
			}
			return true;
		case R.id.m_post:
			if(mProgressBar.getVisibility()==View.VISIBLE)
				replayHandler.removeCallbacks(FreshListRunnable);
			postType=0;
			showDialog(DIALOG_REPLAY);
			return true;
		case R.id.m_fav_board:
			new DoGet().execute();
			return true;
		default:  
			return super.onContextItemSelected(item);  
		}  
	}  
	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.btn_boardback:
			finish();
			break;
		case R.id.btn_More:
			openContextMenu(v);
			break;
		case R.id.replysubmit:
			postType =0;
			new DoPost().execute();
			dismissDialog(DIALOG_REPLAY);
			break;
		case R.id.btn_dialoglogin:
			user=usernameEditText.getText().toString();
			pswd=passwordEditText.getText().toString();
			Boolean success = Login.getInstance().login(user, pswd);
			if(success){
				restoreCookie();
				dismissDialog(DIALOG_LOGIN);
				new DoPost().execute();
			}else
				Tip("登录失败!");
			break;
		case R.id.replyupload:
			Intent i=new Intent();
			i.setClass(PostsListActivity.this, FileListActivity.class);
			startActivityForResult(i, 0);
			break;
		case R.id.paint:
			i=new Intent();
			i.setClass(this, PaintAcitivity.class);
			i.putExtra("board", board);
			startActivityForResult(i, 2);
			break;
		}
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(0==requestCode){
			switch (resultCode){
			case Activity.RESULT_OK:

				filePath=data.getStringArrayListExtra("filePath");
				int size=filePath.size();
				postType =1;
				new DoPost().execute();
				break;
			default:
				break;
			}

		}
		else if(2==requestCode&& Activity.RESULT_OK==resultCode){
			Message message  = new Message();
			message.what = POST_PIC_DONE;
			Bundle b=new Bundle();
			b.putString("fileUrl", data.getStringExtra("FileURL"));
			message.setData(b);
			replayHandler.sendMessage(message);
		}

	}



	public void  Tip(String  string){
		if(mToast != null) {  
			mToast.cancel();  
		}
		mToast=Toast.makeText(this, string, 100);
		mToast.show();
	}


}
