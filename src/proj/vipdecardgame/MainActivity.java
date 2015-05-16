package proj.vipdecardgame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	private final static String VERSION_CONFIRM = "http://vipnextwars.php.xdomain.jp/maintenance.php?sw=android";
	private final static String STORE_URL = "https://play.google.com/store/apps/details?id=proj.vipdecardgame";

	private final static String UPDATE_INFO = "・対戦におけるユーザインタフェースなどを改良しました。\n";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((TextView) findViewById(R.id.ver)).setText("Version "
				+ getVersionName(this) + "   Developed by ◆CEeOGi4Lj");

		((TextView) findViewById(R.id.update_info)).setText("【更新情報 Ver "
				+ getVersionName(this) + "】");
		((TextView) findViewById(R.id.update_detail)).setText(UPDATE_INFO);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
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
		case R.id.action_editprofile:
			final SharedPreferences sp = getSharedPreferences("prof",
					MODE_PRIVATE);
			final EditText et = new EditText(this);
			et.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
			et.setText(sp.getString("name", "名無し"));
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("プレイヤー名編集")
					.setView(et)
					.setPositiveButton("DONE",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									if (et.getText().toString().indexOf("＠開発") != -1
											|| et.getText().toString()
													.indexOf("@開発") != -1
											|| et.getText().toString()
													.indexOf("＠社長") != -1
											|| et.getText().toString()
													.indexOf("@社長") != -1)
										Toast.makeText(MainActivity.this,
												"使用出来ない文字列が含まれています。",
												Toast.LENGTH_SHORT).show();
									else {
										Editor e = sp.edit();
										e.putString("name", et.getText()
												.toString());
										e.commit();
									}
								}
							}).show();
			break;
		case R.id.action_about:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	int cnt = 0;

	public void onDevLogin(View v) {
		cnt++;
		if (cnt == 10) {
			final EditText passText = new EditText(this);
			passText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("開発者認証").setCancelable(false)
					.setNegativeButton("Cancel", null);
			final SharedPreferences sp = getSharedPreferences("dev",
					MODE_PRIVATE);
			if (sp.getBoolean("dev", true) && sp.getBoolean("dev", false)) {
				dialog.setMessage("認証を解除しますか？")
						.setPositiveButton("Done",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										Editor editor = sp.edit();
										editor.putBoolean("dev", false);
										editor.commit();
									}
								}).show();
			} else {
				dialog.setMessage("パスワードを入力してください")
						.setView(passText)
						.setPositiveButton("Done",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										Log.d("Input ==>", passText.getText()
												.toString());
										Editor editor = sp.edit();
										if (passText.getText().toString()
												.equals("dev1739")) {
											editor.putBoolean("dev", true);
											editor.commit();
											Toast.makeText(MainActivity.this,
													"開発者認証が完了しました。",
													Toast.LENGTH_SHORT).show();
										} else if (passText.getText()
												.toString()
												.equals("syacho2684")) {
											editor.putBoolean("syacho", true);
											editor.commit();
											Toast.makeText(MainActivity.this,
													"こんにちわ、社長。",
													Toast.LENGTH_SHORT).show();
										} else
											Toast.makeText(MainActivity.this,
													"認証に失敗しました。",
													Toast.LENGTH_SHORT).show();

									}
								}).show();
			}
			cnt = 0;
		}
	}

	// スタートボタン
	public void onClick(View view) {
		Intent intent;
		final SharedPreferences sp = getSharedPreferences("dev", MODE_PRIVATE);
		switch (view.getId()) {
		case R.id.manage_deck:
			intent = new Intent(getApplicationContext(),
					DeckManageActivity.class);
			this.startActivity(intent);
			break;
		case R.id.multi_play:

			if (sp.getBoolean("dev", false)) {
				String[] items = { "User Mode.", "Test Mode." };
				final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("Developer's Option")
						.setItems(items, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Editor editor = sp.edit();
								switch (which) {
								case 0:
									editor.putBoolean("test", false);
									break;
								case 1:
									editor.putBoolean("test", true);
									break;
								}
								editor.commit();
								Intent i;
								i = new Intent(getApplicationContext(),
										LobbyActivity.class);
								MainActivity.this.startActivity(i);
								dialog.dismiss();
							}
						}).show();
			} else {
				VerCheckTask task = new VerCheckTask();
				task.execute();
			}
			break;
		case R.id.solo_play:
			intent = new Intent(getApplicationContext(), SoloBattleActivity.class);
			this.startActivity(intent);
			break;
		case R.id.tutorial:
			if (sp.getBoolean("dev", false)) {
				intent = new Intent(getApplicationContext(),
						TutorialListActivity.class);
				this.startActivity(intent);
			} else
				Toast.makeText(this, "開発中", Toast.LENGTH_SHORT).show();
			break;
		}
	}

	public void EnterOnline(int verCode, int maintenance) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		Log.d("verCode = ", "" + verCode);
		Log.d("getVersionCode() = ", "" + getVersionCode(this));
		if (getVersionCode(this) < verCode)
			dialog.setTitle("バージョン確認")
					.setMessage("新しいバージョンにアップデートしてください。")
					.setNegativeButton("もどる", null)
					.setPositiveButton("アップデートする",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Uri uri = Uri.parse(STORE_URL);
									Intent i = new Intent(Intent.ACTION_VIEW,
											uri);
									startActivity(i);
								}
							}).show();
		else {
			Intent i = new Intent(getApplicationContext(), LobbyActivity.class);
			this.startActivity(i);
		}

	}

	public static int getVersionCode(Context context) {
		PackageManager pm = context.getPackageManager();
		int versionCode = 1;
		try {
			PackageInfo packageInfo = pm.getPackageInfo(
					context.getPackageName(), 0);
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	public static String getVersionName(Context context) {
		PackageManager pm = context.getPackageManager();
		String versionName = "";
		try {
			PackageInfo packageInfo = pm.getPackageInfo(
					context.getPackageName(), 0);
			versionName = packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionName;
	}

	public void showMessageDialog(String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(message).setPositiveButton("はい", null).show();
	}

	public class VerCheckTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... urlStr) {
			HttpClient objHttp = new DefaultHttpClient();
			HttpParams params = objHttp.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 1000); // 接続のタイムアウト
			HttpConnectionParams.setSoTimeout(params, 1000); // データ取得のタイムアウト
			String sReturn = "";
			try {
				HttpGet objGet = new HttpGet(VERSION_CONFIRM);
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
				int verCode = jsonObj.getInt("Android");
				int maintenance = jsonObj.getInt("maintenance");
				EnterOnline(verCode, maintenance);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
