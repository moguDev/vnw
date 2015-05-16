package proj.vipdecardgame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import proj.vipdecardgame.model.Card;
import proj.vipdecardgame.model.GameModel;
import proj.vipdecardgame.model.GameModel.ChargeOrDrawItem;
import proj.vipdecardgame.model.Player;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class ObserveActivity extends ActionBarActivity {

	private String VIEW_URL = "http://vipnextwars.php.xdomain.jp/view.php?roomID=";
	// 部屋情報
	int postCnt = -1, roomId;
	// タイマータスク
	Timer timer;
	// ゲームモデル
	GameModel gm;
	// 対戦ログ
	ArrayList<String> logList;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_observe);

		SharedPreferences sp = getSharedPreferences("dev", MODE_PRIVATE);
		if (sp.getBoolean("test", false) && sp.getBoolean("dev", false))
			VIEW_URL = "http://vipnextwars.php.xdomain.jp/test/view.php?roomID=";

		Intent i = getIntent();
		roomId = i.getIntExtra("roomID", -1);

		// ゲームモデル初期化
		gm = new GameModel(this, 1);
		// プレイヤー情報の初期化
		gm.setMe(new Player(10, Card.createTestDeck(this), 3, 3));
		gm.getPlayer(Player.ME).name = i.getStringExtra("host");
		gm.setOpponent(new Player(10, Card.createTestDeck(this), 3, 3));
		gm.getPlayer(Player.OPPONENT).name = i.getStringExtra("guest");
		logList = new ArrayList<String>();
		// 行動取得タイマータスク
		timer = new Timer();
		TimerTask timerTask = new GetActionTask(VIEW_URL + this.roomId);
		timer.schedule(timerTask, 0, 100);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		timer.cancel();
		timer.purge();
		timer = null;
	}

	/*** ゲーム終了 ***/
	public void endGame(boolean win, String message) {
		String title = "";
		if (win)
			title = gm.getPlayer(Player.ME).name + "の勝ち";
		else
			title = gm.getPlayer(Player.OPPONENT).name + "の勝ち";
		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(title)
				.setCancelable(false)
				.setNegativeButton("ロビーへ戻る",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						});
		if (message != null)
			dialog.setMessage(message);
		dialog.show();
	}

	/*** 画面の更新 ***/
	public void updateView() {
		// ログ表示view
		((TextView) findViewById(R.id.message)).setText(this.logList
				.get(logList.size() - 1));
		// 自分のプレイヤーデータの取得
		Player me = gm.getPlayer(Player.ME);
		// 自分のライフ
		((TextView) findViewById(R.id.p1_life)).setText("ライフ：" + me.getLife());
		// 自分のコスト
		((TextView) findViewById(R.id.p1_cost)).setText("草：" + me.getWeed());
		// 自分のデッキ枚数
		((TextView) findViewById(R.id.p1_deck)).setText("デッキ：" + me.deckSize());
		// 自分の墓地の枚数
		((TextView) findViewById(R.id.p1_grave)).setText("墓地："
				+ me.getGrave().size());
		// 自分の手札の枚数
		((TextView) findViewById(R.id.p1_hand_num)).setText("手札："
				+ me.getHand().size());

		// 相手のプレイヤー情報の取得
		Player opponent = gm.getPlayer(Player.OPPONENT);
		// p2のライフの表示
		((TextView) findViewById(R.id.p2_life)).setText("ライフ："
				+ opponent.getLife());
		// p2のコストの表示
		((TextView) findViewById(R.id.p2_cost)).setText("草："
				+ opponent.getWeed());
		// p2のデッキ枚数の表示
		((TextView) findViewById(R.id.p2_deck)).setText("デッキ："
				+ opponent.deckSize());
		// p2の墓地の枚数の表示
		((TextView) findViewById(R.id.p2_grave)).setText("墓地："
				+ opponent.getGrave().size());
		// 相手の手札の枚数
		((TextView) findViewById(R.id.p2_hand_num)).setText("手札："
				+ opponent.getHand().size());
		// アクションバーの表示設定
		ActionBar ab = getSupportActionBar();
		ab.setTitle(me.name + " vs " + opponent.name + " (TURN: "
				+ gm.getTurnCount() + ")");
		// タイム表示view
		TextView timeLabel = (TextView) findViewById(R.id.current_time);
		timeLabel.setTextColor(0xff0000ff);
		// 現在のタイム表示
		switch (gm.getTime()) {
		case GameModel.TimeItem.UP:
			timeLabel.setText(me.name + "のアップタイム");
			break;
		case GameModel.TimeItem.DRAW:
			timeLabel.setText(me.name + "のドロータイム");
			break;
		case GameModel.TimeItem.MAIN:
			timeLabel.setText(me.name + "のメインタイム");
			break;
		case GameModel.TimeItem.ACTION:
			timeLabel.setText(me.name + "のアクションタイム");
			break;
		case GameModel.TimeItem.END:
			timeLabel.setText(me.name + "のエンドタイム");
			break;
		case GameModel.TimeItem.COUNTER:
			timeLabel.setText(me.name + "のカウンタータイム");
			break;
		case GameModel.TimeItem.OPPONENT_UP:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のアップタイム");
			break;
		case GameModel.TimeItem.OPPONENT_DRAW:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のドロータイム");
			break;
		case GameModel.TimeItem.OPPONENT_MAIN:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のメインタイム");
			break;
		case GameModel.TimeItem.OPPONENT_ACTION:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のアクションタイム");
			break;
		case GameModel.TimeItem.OPPONENT_END:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のエンドタイム");
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のカウンタータイム");
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER_END:
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText(opponent.name + "のカウンタータイム");
			break;
		}

		for (int i = 0; i < 5; i++)
			for (int j = 0; j < 5; j++) {
				String str = "field_";
				str += GameModel.numToRowCode(i);
				str += j + 1;
				if (!gm.isBlankCell(i, j)) {
					Card card = gm.getCard(i, j);
					// 指定のセルのviewIdの作成
					int vId = getResources().getIdentifier(str, "id",
							getPackageName());
					ImageView iv = (ImageView) findViewById(vId);
					iv.setImageResource(Card.getIconImgId(card.id, this));
					if (card.tapped || card.controllable(opponent)) {
						int kakudo = 0;
						if (card.tapped)
							kakudo -= 90;
						if (card.controllable(opponent))
							kakudo -= 180;

						// getDrawableメソッドで取り戻したものを、BitmapDrawable形式にキャストする
						BitmapDrawable bd = (BitmapDrawable) iv.getDrawable();
						// getBitmapメソッドでビットマップファイルを取り出す
						Bitmap bmp = bd.getBitmap();
						// 回転させる
						Matrix matrix = new Matrix();
						matrix.postRotate(kakudo);
						// Bitmap回転させる
						Bitmap flippedBmp = Bitmap.createBitmap(bmp, 0, 0,
								bmp.getWidth(), bmp.getHeight(), matrix, false);
						// 加工したBitmapを元のImageViewにセットする
						iv.setImageDrawable(new BitmapDrawable(flippedBmp));
					}
					if (card.controllable(me))
						iv.setBackgroundResource(R.xml.p1_cell);
					else
						iv.setBackgroundResource(R.xml.p2_cell);
					iv.setClickable(true);
				} else {
					// 指定のセルのviewIdの作成
					int vId = getResources().getIdentifier(str, "id",
							getPackageName());
					ImageView iv = (ImageView) findViewById(vId);
					iv.setImageBitmap(null);
					iv.setBackgroundResource(R.xml.cell);
					iv.setClickable(true);
				}
			}
	}

	/*** フィールドクリックイベント ***/
	public void onFieldClick(View sender) {
		int row = 0, column = 0;
		int vId = sender.getId();
		switch (vId) {
		case R.id.field_a1:
			row = 0;
			column = 0;
			break;
		case R.id.field_b1:
			row = 1;
			column = 0;
			break;
		case R.id.field_c1:
			row = 2;
			column = 0;
			break;
		case R.id.field_d1:
			row = 3;
			column = 0;
			break;
		case R.id.field_e1:
			row = 4;
			column = 0;
			break;
		case R.id.field_a2:
			row = 0;
			column = 1;
			break;
		case R.id.field_b2:
			row = 1;
			column = 1;
			break;
		case R.id.field_c2:
			row = 2;
			column = 1;
			break;
		case R.id.field_d2:
			row = 3;
			column = 1;
			break;
		case R.id.field_e2:
			row = 4;
			column = 1;
			break;
		case R.id.field_a3:
			row = 0;
			column = 2;
			break;
		case R.id.field_b3:
			row = 1;
			column = 2;
			break;
		case R.id.field_c3:
			row = 2;
			column = 2;
			break;
		case R.id.field_d3:
			row = 3;
			column = 2;
			break;
		case R.id.field_e3:
			row = 4;
			column = 2;
			break;
		case R.id.field_a4:
			row = 0;
			column = 3;
			break;
		case R.id.field_b4:
			row = 1;
			column = 3;
			break;
		case R.id.field_c4:
			row = 2;
			column = 3;
			break;
		case R.id.field_d4:
			row = 3;
			column = 3;
			break;
		case R.id.field_e4:
			row = 4;
			column = 3;
			break;
		case R.id.field_a5:
			row = 0;
			column = 4;
			break;
		case R.id.field_b5:
			row = 1;
			column = 4;
			break;
		case R.id.field_c5:
			row = 2;
			column = 4;
			break;
		case R.id.field_d5:
			row = 3;
			column = 4;
			break;
		case R.id.field_e5:
			row = 4;
			column = 4;
			break;
		}
		if (!gm.isBlankCell(row, column)) {
			Card card = gm.getCard(row, column);
			updateView();
			showCardPopup(sender, card.pId);
		}
	}

	/*** 場のカードのボップアップ表示 ***/
	public void showCardPopup(View v, int pId) {
		final int row = gm.getRowFromPlayId(pId);
		final int column = gm.getColumnFromPlayId(pId);
		final Card card = gm.getCard(row, column);
		PopupMenu popup = new PopupMenu(this, v);
		// ATK,HPの表示
		if (card.isMonster()) {
			popup.getMenu().add(
					String.format("ATK : %d, HP : %d", card.atk, card.hp));
			popup.getMenu().add(1, 5, 5, "移動範囲表示");
			popup.getMenu().add(1, 6, 6, "攻撃範囲表示");
		} else {
			popup.getMenu().add(1, 6, 6, "範囲表示");
		}
		popup.getMenu().add(1, 7, 7, "カード詳細");
		popup.show();
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
				case 5:
					int[][] movRange = GameModel
							.rangeStringToIntArray(card.movRange);
					if (card.controllable(gm.getPlayer(Player.OPPONENT)))
						movRange = GameModel.rangeConvertToEnemies(movRange);
					movRange = GameModel.rangeApplyCell(movRange, row, column);
					showRangeCell(1, movRange);
					break;
				case 6:
					int[][] atkRange = GameModel
							.rangeStringToIntArray(card.atkRange);
					if (card.controllable(gm.getPlayer(Player.OPPONENT)))
						atkRange = GameModel.rangeConvertToEnemies(atkRange);
					atkRange = GameModel.rangeApplyCell(atkRange, row, column);
					showRangeCell(2, atkRange);
					break;
				case 7:
					showCardDetails(card);
					break;
				}
				return false;
			}
		});
	}

	/*** 範囲表示メソッド ***/
	public void showRangeCell(int mode, int[][] rangeArr) {
		for (int i = 0; i < GameModel.BOARD_SIZE; i++)
			for (int j = 0; j < GameModel.BOARD_SIZE; j++)
				if (rangeArr[j][i] == 1) {
					String string = "field_" + GameModel.numToRowCode(j);
					string += (i + 1);
					int vId = getResources().getIdentifier(string, "id",
							getPackageName());
					int color = 0x55ffff00;
					switch (mode) {
					case 1:
						color = 0x5500ff00;
						break;
					case 2:
						color = 0x55ff5500;
						break;
					}
					((ImageView) findViewById(vId)).setBackgroundColor(color);
				}
	}

	/*** 場のカード詳細表示メソッド ***/
	public void showCardDetails(Card card) {
		// ダイアログタイトル部分に表示するカード情報
		String info;
		if (card.isMonster())
			info = String.format("ATK : %d, HP : %d", card.atk, card.hp);
		else
			info = "マジック";
		// 表示するカード画像Viewの生成
		ImageView iv = new ImageView(this);
		iv.setImageResource(card.getCardImgId(this));
		// カード詳細表示ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(info).setView(iv).setNegativeButton("とじる", null).show();
	}

	/*** 墓地クリックイベント ***/
	public void onGraveClick(View v) {
		int id = v.getId();
		ArrayList<Card> graveList = null;
		String title = null;
		Player p = null;
		switch (id) {
		case R.id.p1_grave:
			p = gm.getPlayer(Player.ME);
			break;
		case R.id.p2_grave:
			p = gm.getPlayer(Player.OPPONENT);
			break;
		}
		graveList = (ArrayList<Card>) p.getGrave();
		title = p.name + "の墓地";
		GridView gv = new GridView(this);
		gv.setNumColumns(5);
		gv.setAdapter(new CardGridAdapter(this, graveList));
		gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ArrayList<Card> graveList = null;
				switch (parent.getId()) {
				case R.id.p1_grave:
					graveList = (ArrayList<Card>) gm.getPlayer(Player.ME)
							.getGrave();
					break;
				case R.id.p2_grave:
					graveList = (ArrayList<Card>) gm.getPlayer(Player.OPPONENT)
							.getGrave();
					break;
				}
				Card card = graveList.get(position);
				showCardDetails(card);
			}
		});

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(title).setView(gv).setNegativeButton("閉じる", null)
				.show();
	}

	/*** 行動取得タスク ***/
	class GetActionTask extends TimerTask {
		private String url;
		private Handler handler;

		public GetActionTask(String url) {
			this.url = url;
			handler = new Handler();
		}

		@Override
		public void run() {
			HttpClient objHttp = new DefaultHttpClient();
			HttpParams params = objHttp.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 2000); // 接続のタイムアウト
			HttpConnectionParams.setSoTimeout(params, 2000); // データ取得のタイムアウト
			String sReturn = "";
			try {
				HttpGet objGet = new HttpGet(url + "&pcnt=" + (postCnt + 1));
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
			}

			try {
				// 取得した文字列からJSONオブジェクトの生成
				JSONObject jsonObj = new JSONObject(sReturn);
				Log.d("GetActionTask:", "sReturn = " + sReturn);
				// 行動ケースの取得
				String sw = jsonObj.getString("sw");
				// プレイヤーIDの取得
				int playerId = jsonObj.getInt("player");
				final Player player = gm.getPlayer(playerId);
				// チャージオアドロー
				if (sw.equals("draw")) {
					// 選択アイテムの取得
					final int val = jsonObj.getInt("val");
					// ドロー処理
					gm.draw(playerId, val);
					// 行動ログの追加
					switch (val) {
					case ChargeOrDrawItem.DRAW1:
						logList.add(player.name + "がカードを1枚ドローしました。");
						break;
					case ChargeOrDrawItem.DRAW2:
						logList.add(player.name + "がカードを2枚ドローしました。");
						break;
					case ChargeOrDrawItem.DRAW1_WEED1:
						logList.add(player.name + "が草を1つ生やし、カードを1枚ドローしました。");
						break;
					case ChargeOrDrawItem.WEED2:
						logList.add(player.name + "が草を2つ生やしました。");
						break;
					}
					gm.toOpponentDrawtime();
				}
				// モンスター召喚、マジック発動
				else if (sw.equals("summon")) {
					// 座標の取得
					String coordinate = jsonObj.getString("val");
					int row = GameModel
							.rowCodeToNum(coordinate.substring(0, 1));
					int column = Integer.parseInt(coordinate.substring(1, 2));
					// プレイされたカードの生成
					Card card = new Card(jsonObj.getInt("ID"),
							ObserveActivity.this);
					// プレイ処理
					if (playerId == 1)
						gm.playCard(playerId, 4 - row, 4 - column, card);
					else
						gm.playCard(playerId, row, column, card);
					// ログに追加
					String s;
					if (card.isMonster())
						s = "を召喚しました";
					else
						s = "を発動しました";
					logList.add(player.name + "が" + card.name + s);
					gm.toOpponentMaintime();
				}
				// モンスター移動
				else if (sw.equals("mov")) {
					// 移動前座標の取得
					String coordinate1 = jsonObj.getString("mover");
					int row1 = GameModel.rowCodeToNum(coordinate1.substring(0,
							1));
					int column1 = Integer.parseInt(coordinate1.substring(1, 2));
					// 移動後座標の取得
					String coordinate2 = jsonObj.getString("val");
					int row2 = GameModel.rowCodeToNum(coordinate2.substring(0,
							1));
					int column2 = Integer.parseInt(coordinate2.substring(1, 2));
					// モンスター移動処理
					boolean arrival;
					if (playerId == 1)
						arrival = gm.move(4 - row1, 4 - column1, 4 - row2,
								4 - column2);
					else
						arrival = gm.move(row1, column1, row2, column2);
					// 移動するカードの取得
					Card card = new Card(jsonObj.getInt("result"),
							ObserveActivity.this);
					// ログ追加
					if (arrival)
						// バックラインに到達した場合
						logList.add(card.name + "がバックラインに到達し、" + player.name
								+ "の草が１つ生えました。");
					else
						logList.add(player.name + "が" + card.name + "を移動しました。");
					gm.toOpponentActiontime();
				}
				// モンスター攻撃
				else if (sw.equals("atk")) {
					// 攻撃モンスターの座標
					String aCoordinate = jsonObj.getString("atker");
					int aRow = GameModel.rowCodeToNum(aCoordinate.substring(0,
							1));
					int aColumn = Integer.parseInt(aCoordinate.substring(1, 2));
					// 防御モンスターの座標
					String bCoordinate = jsonObj.getString("blocker");
					// 攻撃モンスター
					Card attacker = new Card(jsonObj.getInt("atkerID"),
							ObserveActivity.this);
					// ログ追加
					if (bCoordinate.equals("player")) {
						Player anotherPlayer = gm.getAnotherPlayer(playerId);
						// 攻撃処理
						if (playerId == 1)
							gm.attackToPlayer(4 - aRow, 4 - aColumn);
						else
							gm.attackToPlayer(aRow, aColumn);
						logList.add(anotherPlayer.name + "が" + attacker.name
								+ "に直接攻撃されました。");
					} else {
						int bRow = GameModel.rowCodeToNum(bCoordinate
								.substring(0, 1));
						int bColumn = Integer.parseInt(bCoordinate.substring(1,
								2));
						Card blocker = new Card(jsonObj.getInt("reatkerID"),
								ObserveActivity.this);
						// 攻撃処理
						if (playerId == 1)
							gm.attackToMonster(4 - aRow, 4 - aColumn, 4 - bRow,
									4 - bColumn);
						else
							gm.attackToMonster(aRow, aColumn, bRow, bColumn);
						// ログ追加
						if (gm.isBlankCell(bRow, bColumn))
							logList.add(blocker.name + "が" + attacker.name
									+ "に破壊されました。");
						else
							logList.add(blocker.name + "が" + attacker.name
									+ "に攻撃されました。");
					}
					gm.toOpponentActiontime();
				}
				// ターンエンド
				else if (sw.equals("end")) {
					logList.add(player.name + "がターンを終了しました。");
					gm.toOpponentEndtime();
				}
				// メインタイム終了
				else if (sw.equals("endMain")) {
					logList.add(player.name + "がメインタイムを終了しました。");
					gm.toCountertime();
				}
				// カウンタータイム終了
				else if (sw.equals("endCounter")) {
					logList.add(player.name + "がカウンタータイムを終了しました。");
					gm.toOpponentCounterEndtime();
				}
				// 降参
				else if (sw.equals("surrender"))
					handler.post(new Runnable() {
						public void run() {
							endGame(true, player.name + "が降参しました。");
						}
					});
				// 勝敗判定
				handler.post(new Runnable() {
					public void run() {
						if (gm.getPlayer(Player.ME).isDead())
							endGame(false, null);
						else if (gm.getPlayer(Player.OPPONENT).isDead())
							endGame(true, null);
					}
				});
				// 画面の更新
				handler.post(new Runnable() {
					public void run() {
						updateView();
					}
				});
				// pCntの更新
				postCnt = jsonObj.getInt("pcnt");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}