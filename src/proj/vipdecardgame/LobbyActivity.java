package proj.vipdecardgame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class LobbyActivity extends ActionBarActivity implements
		AdapterView.OnItemClickListener {

	int currentTab = 1;
	String joinRoomHost;

	private String PLAYER_NAME = "名無し";
	private String START_URL = "http://vipnextwars.php.xdomain.jp/room.php?sw=start";
	private String JOIN_URL = "http://vipnextwars.php.xdomain.jp/room.php?sw=join";
	private String VIEW_URL = "http://vipnextwars.php.xdomain.jp/room.php?sw=view";
	private String SPECTATE_URL = "http://vipnextwars.php.xdomain.jp/room.php?sw=spectate";

	// 全角アルファベットと半角アルファベットとの文字コードの差
	private static final int DIFFERENCE = 'Ａ' - 'A';
	// 変換対象半角記号配列
	private static char[] SIGNS1 = { '!', '#', '$', '%', '&', '(', ')', '*',
			'+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[',
			']', '^', '_', '{', '|', '}' };

	class Room {
		int no;
		int roomID;
		String name;
		String host;
		String guest;
		int flagend;
		String pass;
		int hostPlayerID;
	}

	Room selectRoom;
	int myPlayerID;
	boolean joinFlag = false;

	ListView listView;
	ArrayList<Room> roomList;

	ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);

		ActionBar ab = this.getSupportActionBar();
		ab.setTitle("ロビー");

		SharedPreferences sp = getSharedPreferences("prof", MODE_PRIVATE);
		PLAYER_NAME = sp.getString("name", "名無し");

		sp = getSharedPreferences("dev", MODE_PRIVATE);
		if (sp.getBoolean("dev", false)) {
			PLAYER_NAME += "＠開発";
			if (sp.getBoolean("test", false)) {
				START_URL = "http://vipnextwars.php.xdomain.jp/test/room.php?sw=start";
				JOIN_URL = "http://vipnextwars.php.xdomain.jp/test/room.php?sw=join";
				VIEW_URL = "http://vipnextwars.php.xdomain.jp/test/room.php?sw=view";
				SPECTATE_URL = "http://vipnextwars.php.xdomain.jp/test/room.php?sw=spectate";
			}
		} else if (sp.getBoolean("syacho", false)) {
			PLAYER_NAME += "＠社長";
		}

		listView = (ListView) findViewById(R.id.room_list);
		progressDialog = new ProgressDialog(this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("通信中です。");
		progressDialog.setCancelable(false);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		TabListener tabListener = new LobbyTabListener();

		actionBar.addTab(actionBar.newTab().setText("対戦相手待ち").setTag("1")
				.setTabListener(tabListener));

		actionBar.addTab(actionBar.newTab().setText("対戦中").setTag("2")
				.setTabListener(tabListener));
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby, menu);

		for (int i = 0; i < menu.size(); i++)
			menu.getItem(i).setShowAsAction(
					MenuItem.SHOW_AS_ACTION_ALWAYS
							| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
		case R.id.action_newroom:
			createNewRoom();
			break;
		case R.id.action_update:
			getRoomList(currentTab);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public class LobbyTabListener implements ActionBar.TabListener {

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			String tag = tab.getTag().toString();
			if (tag.equals("1"))
				currentTab = 1;
			else if (tag.equals("2"))
				currentTab = 2;
			getRoomList(currentTab);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	public void getRoomList(int i) {
		GetRoomTask task = new GetRoomTask();
		switch (i) {
		case 1:
			task.execute(VIEW_URL);
			break;
		case 2:
			task.execute(SPECTATE_URL);
			break;
		}
	}

	public void createNewRoom() {
		final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(
				R.layout.dialog_newroom, null);
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("部屋を作成").setView(layout)
				.setNegativeButton("キャンセル", null)
				.setPositiveButton("作成", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String[] params = new String[2];
						params[0] = ((EditText) layout
								.findViewById(R.id.room_name)).getText()
								.toString();
						params[1] = ((EditText) layout
								.findViewById(R.id.password)).getText()
								.toString();
						progressDialog.show();
						StartRoomTask task = new StartRoomTask();
						task.execute(params);
					}
				}).show();
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		selectRoom = roomList.get(position);
		switch (currentTab) {
		case 1:
			onItemClickJoin();
			break;
		case 2:
			onItemClickObserve();
			break;
		}
	}

	public void onItemClickJoin() {
		if (selectRoom.hostPlayerID == 1)
			myPlayerID = 2;
		else
			myPlayerID = 1;
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("入室確認").setMessage("「" + selectRoom.name + "」に入室しますか？")
				.setNegativeButton("キャンセル", null)
				.setPositiveButton("入室", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (selectRoom.pass.equals("")) {
							progressDialog.show();
							joinFlag = true;
							getRoomList(currentTab);
						} else {
							final EditText passText = new EditText(
									LobbyActivity.this);
							passText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
							AlertDialog.Builder passAlert = new AlertDialog.Builder(
									LobbyActivity.this);
							passAlert
									.setTitle("入室確認")
									.setMessage("パスワードを入力してください。")
									.setView(passText)
									.setNegativeButton("キャンセル", null)
									.setPositiveButton(
											"確認",
											new DialogInterface.OnClickListener() {

												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													if (passText
															.getText()
															.toString()
															.equals(selectRoom.pass)) {
														progressDialog.show();
														joinFlag = true;
														getRoomList(currentTab);
													} else {
														AlertDialog.Builder missAlert = new AlertDialog.Builder(
																LobbyActivity.this);
														missAlert
																.setTitle(
																		"入室失敗")
																.setMessage(
																		"パスワードが間違っています。")
																.setPositiveButton(
																		"はい",
																		null)
																.show();
													}
												}
											}).show();
						}
					}
				}).show();
	}

	public void onItemClickObserve() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(selectRoom.name)
				.setMessage(selectRoom.host + " vs " + selectRoom.guest)
				.setNegativeButton("もどる", null)
				.setPositiveButton("観戦", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						observeGame();
					}
				}).show();
	}

	public void observeGame() {
		Intent intent = new Intent(getApplicationContext(),
				ObserveActivity.class);
		intent.putExtra("roomID", selectRoom.roomID);
		intent.putExtra("host", selectRoom.host);
		intent.putExtra("guest", selectRoom.guest);
		this.startActivity(intent);
	}

	public class StartRoomTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... contents) {
			String url = START_URL;
			url += "&name="
					+ convert(contents[0].replace(" ", "").replace("　", ""));
			url += "&host=" + PLAYER_NAME.replace(" ", "").replace("　", "");
			url += "&pass=" + contents[1];
			Log.d("url = ", url);
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost post = null;

			HttpResponse res = null;

			try {
				post = new HttpPost(url);
				res = httpClient.execute(post);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String getStr = "";
			try {
				if (res.getStatusLine().getStatusCode() < 400) {
					InputStream objStream = res.getEntity().getContent();
					InputStreamReader objReader = new InputStreamReader(
							objStream);
					BufferedReader objBuf = new BufferedReader(objReader);
					StringBuilder sBuilder = new StringBuilder();
					String sLine;
					while ((sLine = objBuf.readLine()) != null) {
						sBuilder.append(sLine);
					}
					getStr = sBuilder.toString();
					objStream.close();
					Log.d("getStr = ", getStr);
				}
			} catch (Exception e) {
				return null;
			}

			return getStr;
		}

		protected void onPostExecute(String result) {
			try {
				JSONObject jsonObj = new JSONObject(result);
				int roomID = jsonObj.getInt("no");
				int playerID = jsonObj.getInt("turn");

				Intent intent = new Intent(getApplicationContext(),
						OnlineBattleActivity.class);
				intent.putExtra("roomID", roomID);
				intent.putExtra("playerID", playerID);
				intent.putExtra("isHost", true);
				LobbyActivity.this.startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
			progressDialog.dismiss();
		}
	}

	public class JoinRoomTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... contents) {

			String url = JOIN_URL;
			url += "&roomID=" + contents[0];
			url += "&guest=" + contents[1];
			// url += "&pass=" + contents[2];
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);

			HttpResponse res = null;

			try {
				res = httpClient.execute(post);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String getStr = "";
			try {
				if (res.getStatusLine().getStatusCode() < 400) {
					InputStream objStream = res.getEntity().getContent();
					InputStreamReader objReader = new InputStreamReader(
							objStream);
					BufferedReader objBuf = new BufferedReader(objReader);
					StringBuilder sBuilder = new StringBuilder();
					String sLine;
					while ((sLine = objBuf.readLine()) != null) {
						sBuilder.append(sLine);
					}
					getStr = sBuilder.toString();
					objStream.close();
					Log.d("getStr = ", getStr);
				}
			} catch (Exception e) {
				return null;
			}

			return getStr;
		}

		protected void onPostExecute(String result) {
			Intent intent = new Intent(getApplicationContext(),
					OnlineBattleActivity.class);
			intent.putExtra("roomID", selectRoom.roomID);
			intent.putExtra("playerID", myPlayerID);
			intent.putExtra("isHost", false);
			intent.putExtra("host", joinRoomHost);
			LobbyActivity.this.startActivity(intent);
			progressDialog.dismiss();
		}
	}

	public class GetRoomTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... urlStr) {
			HttpClient objHttp = new DefaultHttpClient();
			HttpParams params = objHttp.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 1000); // 接続のタイムアウト
			HttpConnectionParams.setSoTimeout(params, 1000); // データ取得のタイムアウト
			String sReturn = "";
			try {
				HttpGet objGet = new HttpGet(urlStr[0]);
				HttpResponse objResponse = objHttp.execute(objGet);
				if (objResponse.getStatusLine().getStatusCode() < 400) {
					InputStream objStream = objResponse.getEntity()
							.getContent();
					InputStreamReader objReader = new InputStreamReader(
							objStream);
					BufferedReader objBuf = new BufferedReader(objReader);
					StringBuilder objJson = new StringBuilder();
					String sLine;
					while ((sLine = objBuf.readLine()) != null) {
						objJson.append(sLine);
					}
					sReturn = objJson.toString();
					objStream.close();
				}
			} catch (IOException e) {
				return null;
			}
			return sReturn;
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject jsonObj = new JSONObject(result);
				JSONArray roomArr = jsonObj.getJSONArray("room");
				Log.d("roomArr = ", "" + roomArr.length());

				roomList = new ArrayList<Room>();
				for (int i = 0; i < roomArr.length() - 1; i++) {
					Room room = new Room();
					room.roomID = ((JSONObject) roomArr.get(i)).getInt("no");
					Log.d("GetRoomTask:", "roomID = " + room.roomID);
					room.name = ((JSONObject) roomArr.get(i)).getString("name");
					room.host = ((JSONObject) roomArr.get(i)).getString("host");
					room.guest = ((JSONObject) roomArr.get(i))
							.getString("guest");
					room.flagend = ((JSONObject) roomArr.get(i))
							.getInt("flagend");
					room.pass = ((JSONObject) roomArr.get(i)).getString("pass");
					room.hostPlayerID = ((JSONObject) roomArr.get(i))
							.getInt("turn");
					roomList.add(room);
					if (joinFlag && room.roomID == selectRoom.roomID) {
						String[] params = new String[2];
						params[0] = "" + selectRoom.roomID;
						params[1] = PLAYER_NAME;
						joinRoomHost = selectRoom.host;

						JoinRoomTask task = new JoinRoomTask();
						task.execute(params);
						joinFlag = false;
					}
				}
				if (joinFlag) {
					Handler handler = new Handler();
					handler.post(new Runnable() {
						public void run() {
							AlertDialog.Builder dialog = new AlertDialog.Builder(
									LobbyActivity.this);
							dialog.setTitle("入室失敗")
									.setMessage(
											"すでにこの部屋は存在しないか、他のプレイヤーが入室済みです。")
									.setPositiveButton("はい", null).show();
						}
					});
					joinFlag = false;
					progressDialog.dismiss();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			listView.setAdapter(new RoomListAdapter(LobbyActivity.this,
					roomList));
			listView.setOnItemClickListener(LobbyActivity.this);
		}
	}

	class ViewHolder {
		TextView noView;
		TextView nameView;
		TextView hostView;
		TextView passView;
	}

	class RoomListAdapter extends ArrayAdapter<Room> {
		private LayoutInflater inflater;

		public RoomListAdapter(Context context, List<Room> handList) {
			super(context, 0, handList);
			this.inflater = getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Room item = getItem(position);

			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.roomlist_item, null);
				holder = new ViewHolder();
				holder.noView = (TextView) convertView.findViewById(R.id.no);
				holder.nameView = (TextView) convertView
						.findViewById(R.id.name);
				holder.hostView = (TextView) convertView
						.findViewById(R.id.host);
				holder.passView = (TextView) convertView
						.findViewById(R.id.pass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.noView.setText("" + (position + 1));
			holder.nameView.setText(item.name);
			holder.hostView.setText(item.host);
			if (item.host.contains("＠開発"))
				convertView.setBackgroundColor(0x33aaaaff);
			else if (item.host.contains("＠社長"))
				convertView.setBackgroundColor(0x33ff8888);
			else
				convertView.setBackgroundColor(0x00ffffff);
			if (item.pass.equals(""))
				holder.passView.setText("なし");
			else
				holder.passView.setText("あり");
			return convertView;
		}
	}

	/**
	 * 変換対象半角記号かを判定する。
	 * 
	 * @param pc
	 * @return
	 */
	private static boolean is1Sign(char pc) {
		for (char c : LobbyActivity.SIGNS1) {
			if (c == pc) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 文字列のアルファベット・数値を半角文字に変換する。
	 * 
	 * @param str
	 * @return 変換された２バイト文字列
	 */
	public String convert(String str) {
		char[] cc = str.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (char c : cc) {
			char newChar = c;
			if (is1Sign(c)) {
				// 変換対象のcharだった場合に全角文字と半角文字の差分を足す
				newChar = (char) (c + LobbyActivity.DIFFERENCE);
			}

			sb.append(newChar);
		}
		return sb.toString();
	}
}
