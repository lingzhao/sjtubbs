﻿/**
 * 
 */
package com.tyt.bbs;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tyt.bbs.adapter.BoardsAdapter;
import com.tyt.bbs.adapter.MainAdapter;
import com.tyt.bbs.utils.Net;
import com.tyt.bbs.utils.Property;

/**
 * @author tyt2011
 *com.tyt.bbs
 */
public class MainActivity extends BaseActivity implements OnItemClickListener, OnClickListener {

	private static final int DIALOG_NAVIGATION = 0;
	private GridView  mGridView;
	private View dv;
	private ListView lv_colums,lv_boards;
	private BoardsAdapter mColumsAdapter,mBoardsAdapter;
	private AutoCompleteTextView autoCompleteTextView;
	
	private Handler handler= new Handler(){

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what){
			case 0:
				ArrayList<String>title =msg.getData().getStringArrayList("title");
				TextView tv= (TextView)findViewById(R.id.notification);
				tv.setText(getString(R.string.new_message)+title.size()+"封！");
				tv.setVisibility(View.VISIBLE);
				TextView tv_msg_title = (TextView) mGridView.getChildAt(1).findViewById(R.id.message_title);
				tv_msg_title.setVisibility(View.VISIBLE);
				tv_msg_title.setText(title.toString());
				break;
			
			}
		}
		
	};
	
	private int ColumsId[]={
		R.array.region0,	R.array.region1,	R.array.region2,	R.array.region3,	
		R.array.region4,	R.array.region5,	R.array.region6,	R.array.region7,	
		R.array.region8,	R.array.region9,	R.array.region10,R.array.region11 };
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_main);		
		initialViews();
		initialNavigation();
		if(Net.getInstance().getCookie()!=null)
			new getMsg().execute(Property.Base_URL+"/bbsnewmail");
	}

	private void initialViews(){
		mGridView = (GridView) findViewById(R.id.gv_main);
		mGridView.setAdapter(new MainAdapter(this));  
		mGridView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> ap, View v, int pos,
					long id) {
				// TODO Auto-generated method stub
				mainFunctions(pos);
			}

		});
		
		findViewById(R.id.navigation).setOnClickListener(this);
		
	}

	private void initialNavigation(){
		dv = LayoutInflater.from(this).inflate(R.layout.dialog_navigation, null);
		lv_colums = (ListView)dv.findViewById(R.id.lv_colums);
		lv_boards = (ListView)dv.findViewById(R.id.lv_boards);
		mColumsAdapter = new BoardsAdapter(getResources().getStringArray(R.array.colums),this);
		mBoardsAdapter = new BoardsAdapter(getResources().getStringArray(R.array.region0),this);
		lv_boards.setAdapter(mBoardsAdapter);
		lv_colums.setAdapter(mColumsAdapter);
		lv_colums.setOnItemClickListener(this);
		lv_boards.setOnItemClickListener(this);
		dv.findViewById(R.id.btn_ok).setOnClickListener(this);
		dv.findViewById(R.id.btn_cancel).setOnClickListener(this);
		dv.setOnClickListener(this);
		autoCompleteTextView = (AutoCompleteTextView) dv.findViewById(R.id.autoCT_navigation); 
		ArrayAdapter<String> boadersadapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line,getResources().getStringArray(R.array.boards));
		autoCompleteTextView.setAdapter(boadersadapter);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		switch(id){
		case DIALOG_NAVIGATION:
			final Dialog mDialog =  new Dialog(this, R.style.Dialog);
			mDialog.setContentView(dv);
			return mDialog;
		}
		return null;
	}

	
	private class getMsg extends AsyncTask<String,Integer,ArrayList<String>>{

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected ArrayList<String> doInBackground(String... URL) {
			// TODO Auto-generated method stub
			ArrayList<String> title= new ArrayList<String>();
			try {
				String result =  Net.getInstance().getWithCookie(URL[0]);
				Log.i("getMsg", result);
					Elements es= Jsoup.parse(result).select("tr");
					for(Element e:es){
						if(es.get(0).equals(e)) continue;
						String tmp = e.select("td").get(4).child(0).text();
						Log.v("Message", tmp+"");
						title.add(tmp);
					}
					return title;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.i("getMsg", e.toString());
			}
			return title;
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(ArrayList<String> result) {
			// TODO Auto-generated method stub
			if(result.size()!=0){
				Message message  = new Message();
				message.what = 0;
				Bundle data = new Bundle();
				data.putStringArrayList("title", result);
				message.setData(data);
				handler.sendMessage(message);
			}
			super.onPostExecute(result);
		}
		
		
		
	}


	private void mainFunctions(int mode){
		switch(mode){
		case 0://推荐文章
			startActivity(new Intent(this,RecommendActivity.class));
			break;
		case 1://今日十大
			startActivity(new Intent(this,TopTenActivity.class));
			break;
		case 2://板块收藏
			startActivity(new Intent(this,FavoriteActivity.class));
			break;
		case 3://好文收集
			startActivity(new Intent(this,CollectionActivity.class));
			break;
		case 4://个人资料
			startActivity(new Intent(this,ProfileActivity.class));
			break;
		case 5://个人资料
			startActivity(new Intent(this,MessageActivity.class));
			break;
		case 6://系统设置
			startActivity(new Intent(this,SettingActivity.class));
			break;
		}
//		finish();
	}

	/* (non-Javadoc)
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> ap, View v, int pos, long id) {
		// TODO Auto-generated method stub
		switch(ap.getId()){
		case R.id.lv_colums:
			autoCompleteTextView.setText("");
			mColumsAdapter.setSelectpos(pos);
			mColumsAdapter.notifyDataSetChanged();
			mBoardsAdapter.setBoards(getResources().getStringArray(ColumsId[pos]));
			mBoardsAdapter.notifyDataSetChanged();
			break;
		case R.id.lv_boards:
			autoCompleteTextView.setText(mBoardsAdapter.getItem(pos));
			mBoardsAdapter.setSelectpos(pos);
			mBoardsAdapter.notifyDataSetChanged();
			break;
		case R.id.autoCT_navigation:
			String board = autoCompleteTextView.getAdapter().getItem(pos).toString();
			Toast.makeText(this, board+"", 200).show();
			if(board.equalsIgnoreCase(""))
				board = mBoardsAdapter.getItem(mBoardsAdapter.getSelectpos());				
			Intent it = new Intent(this,PostsListActivity.class);
			it.putExtra(Property.Boards, board);
			startActivity(it);
			dismissDialog(DIALOG_NAVIGATION);
			break;
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.btn_ok:
			String board = autoCompleteTextView.getText().toString();
			if(board.equalsIgnoreCase(""))
				board = mBoardsAdapter.getItem(mBoardsAdapter.getSelectpos());				
			Intent it = new Intent(this,PostsListActivity.class);
			it.putExtra(Property.Boards, board);
			startActivity(it);
			dismissDialog(DIALOG_NAVIGATION);
			break;
		case R.id.btn_cancel:
			dismissDialog(DIALOG_NAVIGATION);
			break;
		case R.layout.dialog_navigation:
			dismissDialog(DIALOG_NAVIGATION);
			break;
		case R.id.navigation:
			showDialog(DIALOG_NAVIGATION);
			break;
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if((keyCode == KeyEvent.KEYCODE_BACK))
		{
			finish();
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		return super.onKeyDown(keyCode, event);
	}
	
}
