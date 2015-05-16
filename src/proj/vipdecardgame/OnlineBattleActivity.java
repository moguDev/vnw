package proj.vipdecardgame;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import proj.vipdecardgame.model.Card;
import proj.vipdecardgame.model.GameModel;
import proj.vipdecardgame.model.GameModel.TimeItem;
import proj.vipdecardgame.model.Player;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OnlineBattleActivity extends ActionBarActivity {
	// デッキデータファイル名
	private static final String DECK_FILE_NAME = "deck_data.dat";
	// サーバ通信URL
	private String POST_URL = "http://vipnextwars.php.xdomain.jp/post.php";
	private String VIEW_URL = "http://vipnextwars.php.xdomain.jp/view.php?roomID=";
	private String END_URL = "http://vipnextwars.php.xdomain.jp/room.php?sw=end&roomID=";
	private String CHECK_URL = "http://vipnextwars.php.xdomain.jp/room.php?sw=check&roomID=";
	// 部屋情報など
	private int roomId, myPlayerId;
	// ポストカウント
	private int postCnt;
	// プログレスダイアログ
	ProgressDialog pDialog;
	// タイマー
	Timer timer;
	// モデル
	GameModel gm;
	// 選択された手札のポジション、選択されたマス、行動を起こすマス
	int focusHandPosition, selectRow, selectColumn, actionRow, actionColumn;
	// フラグ
	boolean smnFlag = false, mgcFlag = false, movFlag = false, atkFlag = false;
	// キャンセルボタン
	Button canselButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_game);
		// アクションバーの表示設定
		ActionBar ab = getSupportActionBar();
		ab.setTitle("対戦相手待ち");

		SharedPreferences sp = getSharedPreferences("dev", MODE_PRIVATE);
		if (sp.getBoolean("test", false) && sp.getBoolean("dev", false)) {
			POST_URL = "http://vipnextwars.php.xdomain.jp/test/post.php";
			VIEW_URL = "http://vipnextwars.php.xdomain.jp/test/view.php?roomID=";
			END_URL = "http://vipnextwars.php.xdomain.jp/test/room.php?sw=end&roomID=";
			CHECK_URL = "http://vipnextwars.php.xdomain.jp/test/room.php?sw=check&roomID=";
		}

		// ポストカウントの初期化
		postCnt = -1;
		// 部屋番号、プレイヤーIDを取得
		Intent intent = getIntent();
		roomId = intent.getIntExtra("roomID", -1);
		myPlayerId = intent.getIntExtra("playerID", 1);

		// 自分のデッキ
		ArrayList<Card> myDeck = new ArrayList<Card>();
		// 保存されているデッキデータの取得
		if (getFileStreamPath(DECK_FILE_NAME).exists()) {
			try {
				FileInputStream fis = openFileInput(DECK_FILE_NAME);
				ObjectInputStream ois = new ObjectInputStream(fis);
				DeckData deckData = (DeckData) ois.readObject();
				ois.close();
				for (int i = 0; i < deckData.cardIDStrings.size(); i++)
					myDeck.add(new Card(Integer.parseInt(deckData.cardIDStrings
							.get(i)), this));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// 保存されたデータが存在しない場合サンプルデッキを生成
			myDeck = Card.createTestDeck(this);
		}

		// ゲームモデルの初期化
		gm = new GameModel(this, this.myPlayerId);
		// 自分のプレイヤー情報などの初期化
		gm.setMe(new Player(10, myDeck, 3, 3));
		sp = getSharedPreferences("prof", MODE_PRIVATE);
		gm.getPlayer(Player.ME).name = sp.getString("name", "名無し");

		this.canselButton = (Button) this.findViewById(R.id.negative_button);

		// ホストなら
		if (intent.getBooleanExtra("isHost", false)) {
			// 対戦相手入室待ちダイアログ
			pDialog = new ProgressDialog(this);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setCancelable(false);
			pDialog.setMessage("対戦相手の入室待機中です。");
			pDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "退室",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// activityを終了して戻る
							finish();
						}
					});
			pDialog.show();
			// 対戦相手入室チェックタイマータスク
			timer = new Timer();
			TimerTask timerTask = new CheckGuestTask(CHECK_URL + roomId);
			timer.schedule(timerTask, 0, 500);
		}
		// ゲストなら
		else {
			// 対戦相手情報の初期化
			gm.setOpponent(new Player(10, Card.createTestDeck(this), 3, 3));
			gm.getPlayer(Player.OPPONENT).name = intent.getStringExtra("host");
			gm.getPlayer(Player.OPPONENT).shuffleDeck();
			// ゲーム開始
			this.startGame();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		timer.cancel();
		timer.purge();
		timer = null;
		EndGameTask task = new EndGameTask();
		task.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.online_battle, menu);
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
		// チャット
		case R.id.action_chat:
			break;
		// 降参
		case R.id.action_surrender:
			onSurrenderClick();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		// 戻るボタンが押されたとき
		if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			// ボタンが押されたとき
			if (e.getAction() == KeyEvent.ACTION_DOWN) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setMessage("ダメだ! しょうぶの さいちゅうに あいてに せなかは みせられない!")
						.setNegativeButton("▼", null).show();
			}
		}
		return false;
	}

	/*** 対戦相手が見つかった時の処理 ***/
	public void onFindGuest(String guestName) {
		// タイマーを止める
		this.timer.cancel();
		this.timer = null;
		// 入室待機プログレスダイアログをとじる
		this.pDialog.dismiss();
		// 対戦相手情報に名前を設定
		gm.setOpponent(new Player(10, Card.createTestDeck(this), 3, 3));
		this.gm.getPlayer(Player.OPPONENT).name = guestName;
		// ノーティフィケーションに通知
		this.sendNotification();
		// ゲーム開始
		this.startGame();
	}

	// ゲーム開始メソッド
	public void startGame() {
		// 行動取得タイマータスク
		timer = new Timer();
		TimerTask timerTask = new GetActionTask(VIEW_URL + this.roomId);
		timer.schedule(timerTask, 0, 100);
		// 通信中プログレスダイアログの初期化
		pDialog = new ProgressDialog(this);
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.setCancelable(false);
		pDialog.setMessage("通信中");
		// 対戦開始ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("対戦開始").setPositiveButton("閉じる", null);
		switch (myPlayerId) {
		case 1:
			dialog.setMessage("あなたは 先攻 です。").show();
			gm.toUptime();
			break;
		case 2:
			dialog.setMessage("あなたは 後攻 です。").show();
			gm.toOpponentUptime();
			break;
		}
		this.initFlag();
	}

	/*** ゲーム終了 ***/
	public void endGame(boolean win, String message) {
		String title = "";
		if (win)
			title = "YOU WIN!";
		else
			title = "YOU LOSE...";
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
		// アクションバーの表示設定
		ActionBar ab = getSupportActionBar();
		ab.setTitle("vs " + gm.getPlayer(Player.OPPONENT).name + " (TURN: "
				+ gm.getTurnCount() + ")");
		// タイム表示view
		TextView timeLabel = (TextView) findViewById(R.id.current_time);
		timeLabel.setTextColor(0xff0000ff);
		// タイム移動ボタン
		Button nextBtn = (Button) findViewById(R.id.next_time);
		nextBtn.setVisibility(View.VISIBLE);
		// 現在のタイム表示
		switch (gm.getTime()) {
		case GameModel.TimeItem.UP:
			timeLabel.setText("あなたのアップタイム");
			nextBtn.setText("ドロータイムへ");
			break;
		case GameModel.TimeItem.DRAW:
			timeLabel.setText("あなたのドロータイム");
			nextBtn.setText("メインタイムへ");
			break;
		case GameModel.TimeItem.MAIN:
			timeLabel.setText("あなたのメインタイム");
			nextBtn.setText("メインタイム終了");
			break;
		case GameModel.TimeItem.ACTION:
			timeLabel.setText("あなたのアクションタイム");
			nextBtn.setText("エンドタイムへ");
			break;
		case GameModel.TimeItem.END:
			timeLabel.setText("あなたのエンドタイム");
			nextBtn.setText("ターンエンド");
			break;
		case GameModel.TimeItem.COUNTER:
			timeLabel.setText("あなたのカウンタータイム");
			nextBtn.setText("カウンタータイムを終了");
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER_END:
			timeLabel.setText("相手のカウンタータイム");
			nextBtn.setText("アクションタイムへ");
			break;
		default:
		case GameModel.TimeItem.OPPONENT_UP:
			nextBtn.setVisibility(View.GONE);
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText("相手のアップタイム");
			break;
		case GameModel.TimeItem.OPPONENT_DRAW:
			nextBtn.setVisibility(View.GONE);
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText("相手のドロータイム");
			break;
		case GameModel.TimeItem.OPPONENT_MAIN:
			nextBtn.setVisibility(View.GONE);
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText("相手のメインタイム");
			break;
		case GameModel.TimeItem.OPPONENT_ACTION:
			nextBtn.setVisibility(View.GONE);
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText("相手のアクションタイム");
			break;
		case GameModel.TimeItem.OPPONENT_END:
			nextBtn.setVisibility(View.GONE);
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText("相手のエンドタイム");
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER:
			nextBtn.setVisibility(View.GONE);
			timeLabel.setTextColor(0xffff0000);
			timeLabel.setText("相手のカウンタータイム");
			break;
		}
		// ログ表示view
		((TextView) findViewById(R.id.message)).setText(gm.getLog());
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
		// 自分の手札の表示
		LinearLayout handView = (LinearLayout) findViewById(R.id.hand_list);
		handView.removeAllViews();
		for (int i = 0; i < me.getHand().size(); i++) {
			Card card = me.getHand().get(i);
			ImageView iv = new ImageView(this);
			iv.setPadding(5, 5, 5, 5);
			iv.setScaleType(ScaleType.CENTER_INSIDE);
			iv.setAdjustViewBounds(true);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			iv.setLayoutParams(params);
			iv.setImageResource(Card.getIconImgId(card.id, this));
			iv.setTag("" + i);
			iv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View sender) {
					int position = Integer.parseInt(sender.getTag().toString());
					onHandCardClick(position);
				}
			});
			iv.setClickable(true);
			handView.addView(iv);
		}

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

	/*** フラグの初期化 ***/
	public void initFlag() {
		this.canselButton.setVisibility(View.GONE);
		this.smnFlag = false;
		this.mgcFlag = false;
		this.movFlag = false;
		this.atkFlag = false;
		this.updateView();
	}

	/*** 降参ボタンクリックイベント ***/
	public void onSurrenderClick() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("降参は甘え").setMessage("本当に降参しますか？")
				.setNegativeButton("キャンセル", null)
				.setPositiveButton("はい", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						PostActionTask task = new PostActionTask();
						task.execute("" + PostActionTask.SW_SURRENDER);
					}
				}).show();
	}

	/** キャンセルボタンクリックイベント **/
	public void onNegativeClick(View sender) {
		this.initFlag();
		sender.setVisibility(View.GONE);
	}

	/** タイム移動ボタンクリックイベント **/
	public void onClickNextTime(View v) {
		// 確認ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("確認").setNegativeButton("キャンセル", null);
		// 現在タイム毎の処理
		switch (gm.getTime()) {
		// アップタイム時
		case GameModel.TimeItem.UP:
			gm.toDrawtime();
			this.showChargeOrDrawDialog();
			break;
		// ドロータイム時
		case GameModel.TimeItem.DRAW:
			gm.toMaintime();
			break;
		case GameModel.TimeItem.MAIN:
			dialog.setMessage("メインタイムを終了しますか？")
					.setPositiveButton("はい",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									PostActionTask post = new PostActionTask();
									post.execute("" + PostActionTask.SW_ENDMAIN);
								}
							}).show();
			break;
		case GameModel.TimeItem.COUNTER:
			PostActionTask post = new PostActionTask();
			post.execute("" + PostActionTask.SW_ENDCOUNTER);
			break;
		case GameModel.TimeItem.ACTION:
			dialog.setMessage("アクションタイムを終了しますか？")
					.setPositiveButton("はい",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									gm.toEndtime();
									initFlag();
									// 確認ダイアログ
									AlertDialog.Builder dialog2 = new AlertDialog.Builder(
											OnlineBattleActivity.this);
									dialog2.setTitle("確認")
											.setMessage("ターンを終了します。")
											.setCancelable(false)
											.setPositiveButton(
													"はい",
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															PostActionTask post = new PostActionTask();
															post.execute(""
																	+ PostActionTask.SW_END);
														}
													}).show();
								}
							}).show();
			break;
		case GameModel.TimeItem.END:
			break;
		case GameModel.TimeItem.OPPONENT_COUNTER_END:
			gm.toActiontime();
			break;
		}
		// フラグの初期化
		this.initFlag();
	}

	//
	public void showChargeOrDrawDialog() {
		final String[] items1 = { "カードを２枚引く", "草を１生やしてカードを１枚引く", "草を２生やす。" };
		final String[] items2 = { "草を１生やしてカードを１枚引く", "草を２生やす。" };
		// 選択ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("チャージオアドロータイム");
		// 先攻時
		if (gm.first && gm.getTurnCount() == 1) {
			String[] params = { "" + PostActionTask.SW_DRAW, "4" };
			PostActionTask post = new PostActionTask();
			post.execute(params);
			return;
		}
		// デッキが0枚
		else if (gm.getPlayer(Player.ME).deckSize() == 0) {
			String[] params = { "" + PostActionTask.SW_DRAW, "3" };
			PostActionTask post = new PostActionTask();
			post.execute(params);
			return;
		}
		// デッキが1枚
		else if (gm.getPlayer(Player.ME).deckSize() == 1) {
			dialog.setItems(items2, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String[] params = { "" + PostActionTask.SW_DRAW,
							"" + (which + 2) };
					PostActionTask post = new PostActionTask();
					post.execute(params);
				}
			});
		}
		// デッキが2枚以上
		else {
			dialog.setItems(items1, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String[] params = { "" + PostActionTask.SW_DRAW,
							"" + (which + 1) };
					PostActionTask post = new PostActionTask();
					post.execute(params);
				}
			});
		}
		dialog.setCancelable(false).show();
	}

	/** 手札カードのクリックイベント **/
	public void onHandCardClick(int position) {
		focusHandPosition = position;
		// 選択されたカードの取得
		final Card card = gm.getPlayer(Player.ME).getHand().get(position);
		// 表示するカード画像Viewの生成
		ImageView imageView = new ImageView(this);
		imageView.setImageResource(card.getCardImgId(this));
		// カード情報表示ダイアログ
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("カード詳細");
		dialog.setView(imageView);
		dialog.setNegativeButton("閉じる", null);
		// 召喚ボタンのイベントリスナー
		DialogInterface.OnClickListener onSummonListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 召喚可能判定
				if (card.cost > gm.getPlayer(Player.ME).getWeed())
					Toast.makeText(OnlineBattleActivity.this, "召喚に必要な草が足りません。",
							Toast.LENGTH_SHORT).show();
				// 召喚イベントへ
				else
					onSummonMonsterClick();
			}
		};
		// マジック発動ボタンのイベントリスナー
		DialogInterface.OnClickListener onMagicListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 召喚可能判定
				if (card.cost > gm.getPlayer(Player.ME).getWeed())
					Toast.makeText(OnlineBattleActivity.this, "召喚に必要な草が足りません。",
							Toast.LENGTH_SHORT).show();
				// マジック発動イベントへ
				else
					onPlayMagicClick();
			}
		};

		// メインタイムかつ選択されたカードがモンスター
		if (gm.getTime() == TimeItem.MAIN && card.isMonster())
			dialog.setPositiveButton("召喚", onSummonListener);
		// メインタイムまたはカウンタータイム,かつ選択されたカードがマジック
		else if ((gm.getTime() == TimeItem.COUNTER || gm.getTime() == TimeItem.MAIN)
				&& !card.isMonster())
			dialog.setPositiveButton("発動", onMagicListener);
		// ダイアログの表示
		dialog.show();
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
		// 移動攻撃フラグが立っていれば
		if (movFlag || atkFlag) {
			actionRow = row;
			actionColumn = column;
		} else {
			selectRow = row;
			selectColumn = column;
		}

		Card card = null;
		// 召喚、発動なら選択された手札からカード取得
		if (smnFlag || mgcFlag)
			card = gm.getPlayer(Player.ME).getHand()
					.get(this.focusHandPosition);
		// 移動、攻撃なら選択されたマスからカード取得
		else if (!gm.isBlankCell(selectRow, selectColumn))
			card = gm.getCard(selectRow, selectColumn);
		// 召喚フラグチェック
		if (smnFlag) {
			// 召喚可能なマスか判定
			if (gm.summonableRange[row][column] == 1
					&& gm.isBlankCell(row, column)) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("確認")
						.setMessage(
								"草を " + card.cost + " 支払って " + card.name
										+ " を召喚しますか？")
						.setCancelable(false)
						.setNegativeButton("キャンセル", null)
						.setPositiveButton("召喚",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// 召喚するマスのコードの生成
										String cellCode = GameModel
												.getCellCodeString(
														4 - selectRow,
														4 - selectColumn);
										// パラメータの生成
										String[] params = {
												"" + PostActionTask.SW_SUMMON,
												""
														+ gm.getPlayer(
																Player.ME)
																.getHand()
																.get(focusHandPosition).id,
												cellCode };
										// ポスト処理
										PostActionTask post = new PostActionTask();
										post.execute(params);
									}
								}).show();
			} else
				Toast.makeText(this, "召喚可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		}
		// マジックフラグチェック
		else if (mgcFlag) {
			// 発動可能なマスか判定
			if (gm.magicPlayableRange[row][column] == 1
					&& gm.isBlankCell(row, column)) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("確認")
						.setMessage(
								"草を " + card.cost + " 支払って " + card.name
										+ " を発動しますか？")
						.setCancelable(false)
						.setNegativeButton("キャンセル", null)
						.setPositiveButton("発動",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// 召喚するマスのコードの生成
										String cellCode = GameModel
												.getCellCodeString(
														4 - selectRow,
														4 - selectColumn);
										// パラメータの生成
										String[] params = {
												"" + PostActionTask.SW_SUMMON,
												""
														+ gm.getPlayer(
																Player.ME)
																.getHand()
																.get(focusHandPosition).id,
												cellCode };
										// ポスト処理
										PostActionTask post = new PostActionTask();
										post.execute(params);
									}
								}).show();
			} else
				Toast.makeText(this, "発動可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		}
		// 移動フラグチェック
		else if (movFlag) {
			// 移動モンスターの移動範囲を取得
			int[][] movRange = GameModel.rangeStringToIntArray(card.movRange);
			// 移動範囲を存在するマスに適用変換
			movRange = GameModel.rangeApplyCell(movRange, this.selectRow,
					this.selectColumn);
			// 移動可能なマスか判定
			if (movRange[this.actionRow][this.actionColumn] == 1
					&& gm.isBlankCell(actionRow, actionColumn)) {
				// 移動前のマスのコードの生成
				String cellCode1 = GameModel.getCellCodeString(4 - selectRow,
						4 - selectColumn);
				// 移動後のマスのコードの生成
				String cellCode2 = GameModel.getCellCodeString(4 - actionRow,
						4 - actionColumn);
				// パラメータの生成
				String[] params = { "" + PostActionTask.SW_MOV, cellCode1,
						cellCode2, "" + card.id };
				// ポスト処理
				PostActionTask post = new PostActionTask();
				post.execute(params);
			} else
				Toast.makeText(this, "移動可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		}
		// 攻撃フラグチェック
		else if (atkFlag) {
			// 攻撃モンスターの移動範囲を取得
			int[][] atkRange = GameModel.rangeStringToIntArray(card.atkRange);
			// 攻撃範囲を存在するマスに適用変換
			atkRange = GameModel.rangeApplyCell(atkRange, this.selectRow,
					this.selectColumn);
			// 攻撃可能なマスか判定
			if (atkRange[actionRow][actionColumn] == 1
					&& !gm.getCard(actionRow, actionColumn).controllable(
							gm.getPlayer(Player.ME))) {
				String coordinate1 = GameModel.getCellCodeString(4 - selectRow,
						4 - selectColumn);
				String coordinate2 = GameModel.getCellCodeString(4 - actionRow,
						4 - actionColumn);
				// ポスト処理
				PostActionTask post = new PostActionTask();
				post.execute("" + PostActionTask.SW_ATK, coordinate1,
						coordinate2);
			} else
				Toast.makeText(this, "攻撃可能なマスを選択してください。", Toast.LENGTH_SHORT)
						.show();
		} else if (card != null) {
			this.initFlag();
			this.showCardPopup(sender, card);
		}
	}

	public void onGraveClick(View v) {
		final int vId = v.getId();
		ArrayList<Card> graveList = null;
		String title = null;
		switch (vId) {
		case R.id.p1_grave:
			graveList = (ArrayList<Card>) gm.getPlayer(Player.ME).getGrave();
			title = "あなたの墓地";
			break;
		case R.id.p2_grave:
			graveList = (ArrayList<Card>) gm.getPlayer(Player.OPPONENT)
					.getGrave();
			title = "対戦相手の墓地";
			break;
		}
		GridView gv = new GridView(this);
		gv.setNumColumns(5);
		gv.setAdapter(new CardGridAdapter(this, graveList));
		gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ArrayList<Card> graveList = null;
				switch (vId) {
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

	/*** 場のカードのボップアップ表示 ***/
	public void showCardPopup(View v, Card c) {
		final Card card = c;
		PopupMenu popup = new PopupMenu(this, v);
		// ATK,HPの表示
		if (card.isMonster())
			popup.getMenu().add(
					String.format("ATK : %d, HP : %d", card.atk, card.hp));
		// アクションタイムかつ自分のモンスターなら移動攻撃メニュー表示
		if (gm.getTime() == TimeItem.ACTION
				&& gm.getCard(selectRow, selectColumn).controllable(
						gm.getPlayer(Player.ME))
				&& !gm.getCard(selectRow, selectColumn).tapped) {
			popup.getMenu().add(1, 2, 2, "移動");
			popup.getMenu().add(1, 3, 3, "攻撃");
			if (this.selectColumn == 0)
				popup.getMenu().add(1, 4, 4, "プレイヤーに攻撃");
		} else {
			if (card.isMonster()) {
				popup.getMenu().add(1, 5, 5, "移動範囲表示");
				popup.getMenu().add(1, 6, 6, "攻撃範囲表示");
			} else
				popup.getMenu().add(1, 5, 5, "範囲表示");
		}
		popup.getMenu().add(1, 7, 7, "カード詳細");
		popup.show();
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
				case 2:
					onMoveClick(card);
					break;
				case 3:
					onAttackToMonsterClick(card);
					break;
				case 4:
					onAttackToPlayerClick(card);
					break;
				case 5:
					int[][] movRange = GameModel
							.rangeStringToIntArray(card.movRange);
					if (card.controllable(gm.getPlayer(Player.OPPONENT)))
						movRange = GameModel.rangeConvertToEnemies(movRange);
					movRange = GameModel.rangeApplyCell(movRange, selectRow,
							selectColumn);
					showRangeCell(1, movRange);
					break;
				case 6:
					int[][] atkRange = GameModel
							.rangeStringToIntArray(card.atkRange);
					if (card.controllable(gm.getPlayer(Player.OPPONENT)))
						atkRange = GameModel.rangeConvertToEnemies(atkRange);
					atkRange = GameModel.rangeApplyCell(atkRange, selectRow,
							selectColumn);
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

	/*** モンスター召喚イベント ***/
	public void onSummonMonsterClick() {
		this.smnFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("召喚をキャンセル");
		// 範囲を表示
		int[][] range = gm.summonableRange;
		showRangeCell(0, range);
		// メッセージトースト
		Toast.makeText(this, "召喚するマスを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** マジック発動イベント ***/
	public void onPlayMagicClick() {
		this.mgcFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("発動をキャンセル");
		// 範囲を表示
		int[][] range = gm.magicPlayableRange;
		showRangeCell(0, range);
		// メッセージトースト
		Toast.makeText(this, "発動するマスを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** モンスター移動イベント ***/
	public void onMoveClick(Card card) {
		// 移動フラグを立てる
		this.movFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("移動をキャンセル");
		// 移動可能範囲を表示
		int[][] range = GameModel.rangeStringToIntArray(card.movRange);
		range = GameModel.rangeApplyCell(range, selectRow, selectColumn);
		showRangeCell(0, range);
		// メッセージトースト
		Toast.makeText(this, "移動するマスを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** モンスター攻撃イベント ***/
	public void onAttackToMonsterClick(Card card) {
		// 攻撃フラグを立てる
		this.atkFlag = true;
		canselButton.setVisibility(View.VISIBLE);
		canselButton.setText("攻撃をキャンセル");
		// 攻撃可能範囲を表示
		int[][] range = GameModel.rangeStringToIntArray(card.atkRange);
		range = GameModel.rangeApplyCell(range, selectRow, selectColumn);
		showRangeCell(0, range);
		// メッセージダイアログ
		Toast.makeText(this, "攻撃するモンスターを選択してください", Toast.LENGTH_SHORT).show();
	}

	/*** プレイヤー攻撃イベント ***/
	public void onAttackToPlayerClick(Card card) {
		// 攻撃側のマスのコードの生成
		String coordinate1 = GameModel.getCellCodeString(4 - selectRow,
				4 - selectColumn);
		// パラメータの生成
		String[] params = { "" + PostActionTask.SW_ATK, coordinate1, "player" };
		// ポスト処理
		PostActionTask post = new PostActionTask();
		post.execute(params);
	}

	/*** ノーティフィケーション通知 ***/
	private void sendNotification() {
		// Intent の作成
		Intent intent = new Intent(this, BlankActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// LargeIcon の Bitmap を生成
		Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher);

		// NotificationBuilderを作成
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				getApplicationContext());
		builder.setContentIntent(contentIntent);
		// ステータスバーに表示されるテキスト
		builder.setTicker("対戦相手が入室しました！");
		// アイコン
		builder.setSmallIcon(R.drawable.ic_launcher);
		// Notificationを開いたときに表示されるタイトル
		builder.setContentTitle("対戦相手が入室しました！");
		// Notificationを開いたときに表示されるサブタイトル
		builder.setContentText("タップして対戦画面に戻ります。");
		// Notificationを開いたときに表示されるアイコン
		builder.setLargeIcon(largeIcon);
		// 通知するタイミング
		builder.setWhen(System.currentTimeMillis());
		// 通知時の音・バイブ・ライト
		builder.setDefaults(Notification.DEFAULT_SOUND
				| Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
		// タップするとキャンセル(消える)
		builder.setAutoCancel(true);

		// NotificationManagerを取得
		NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
		// Notificationを作成して通知
		manager.notify(0, builder.build());
	}

	class PostActionTask extends AsyncTask<String, Integer, Integer> {
		public static final int SW_DRAW = 0, SW_SUMMON = 1, SW_MOV = 2,
				SW_ATK = 3, SW_END = 4, SW_ENDMAIN = 5, SW_ENDCOUNTER = 6,
				SW_SURRENDER = 7;

		int sw, chargeOrDraw;
		String coordinate1, coordinate2;

		@Override
		protected void onPreExecute() {
			// プログレスダイアログを表示
			pDialog.show();
		}

		@Override
		protected Integer doInBackground(String... contents) {
			String url = POST_URL;
			sw = Integer.parseInt(contents[0]);
			switch (sw) {
			case SW_DRAW:
				chargeOrDraw = Integer.parseInt(contents[1]);
				url += "?sw=draw&val=" + chargeOrDraw;
				break;
			case SW_SUMMON:
				url += "?sw=summon&ID=" + contents[1] + "&val=" + contents[2];
				break;
			case SW_MOV:
				this.coordinate1 = contents[1];
				this.coordinate2 = contents[2];
				url += "?sw=mov&val=" + coordinate2 + "&mover=" + coordinate1
						+ "&moverID=" + contents[3];
				break;
			case SW_ATK:
				Card atker = gm.getCard(selectRow, selectColumn);
				Card blocker = gm.getCard(actionRow, actionColumn);
				this.coordinate1 = contents[1];
				this.coordinate2 = contents[2];
				if (coordinate2.equals("player")) {
					url += "?sw=atk&atker="
							+ coordinate1
							+ "&blocker="
							+ coordinate2
							+ "&val="
							+ (gm.getPlayer(Player.OPPONENT).getLife() - atker.atk)
							+ "&atkerID=" + atker.id + "&blockerID=-1";
				} else {
					url += "?sw=atk&atker=" + coordinate1 + "&blocker="
							+ coordinate2 + "&val=" + (blocker.hp - atker.atk)
							+ "&atkerID=" + atker.id + "&blockerID="
							+ blocker.id;
				}
				break;
			case SW_END:
				url += "?sw=end";
				break;
			case SW_ENDMAIN:
				url += "?sw=endMain";
				break;
			case SW_ENDCOUNTER:
				url += "?sw=endCounter";
				break;
			case SW_SURRENDER:
				url += "?sw=surrender";
				break;
			}
			url += ("&roomID=" + roomId + "&player=" + myPlayerId);

			HttpClient httpClient = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);

			HttpResponse res = null;

			try {
				res = httpClient.execute(post);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return Integer.valueOf(res.getStatusLine().getStatusCode());
		}

		@Override
		protected void onPostExecute(Integer result) {
			Card slctCard = gm.getCard(selectRow, selectColumn);
			switch (sw) {
			case SW_DRAW:
				// ドロー処理
				gm.draw(Player.ME, chargeOrDraw);
				gm.toMaintime();
				break;
			case SW_SUMMON:
				Card smnCard = gm.getPlayer(Player.ME).getHand()
						.get(focusHandPosition);
				// 召喚、発動処理
				gm.playCard(Player.ME, selectRow, selectColumn,
						focusHandPosition);
				break;
			case SW_MOV:
				// 移動処理
				gm.move(selectRow, selectColumn, actionRow, actionColumn);
				break;
			case SW_ATK:
				if (coordinate2.equals("player")) {
					// 攻撃処理
					gm.attackToPlayer(selectRow, selectColumn);
				} else {
					String blockerName = gm.getCard(actionRow, actionColumn).name;
					// 攻撃処理
					gm.attackToMonster(selectRow, selectColumn, actionRow,
							actionColumn);
				}
				break;
			case SW_END:
				gm.toOpponentUptime();
				break;
			case SW_ENDMAIN:
				gm.toOpponentCountertime();
				break;
			case SW_ENDCOUNTER:
				gm.toOpponentActiontime();
				break;
			case SW_SURRENDER:
				endGame(false, "降参しました。");
				break;
			}
			// 画面を更新
			initFlag();
			// プログレスダイアログを閉じる
			pDialog.dismiss();
			// 勝敗判定
			Handler handler = new Handler();
			handler.post(new Runnable() {
				public void run() {
					if (gm.getPlayer(Player.ME).isDead())
						endGame(false, null);
					else if (gm.getPlayer(Player.OPPONENT).isDead())
						endGame(true, null);
				}
			});
		}
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
				// 自分の行動ならpCntを更新して戻る
				if (jsonObj.getInt("player") == myPlayerId) {
					postCnt = jsonObj.getInt("pcnt");
					return;
				}
				// チャージオアドロー
				if (sw.equals("draw")) {
					// 選択アイテムの取得
					final int val = jsonObj.getInt("val");
					// ドロー処理
					gm.draw(Player.OPPONENT, val);
					gm.toOpponentDrawtime();
				}
				// モンスター召喚、マジック発動
				else if (sw.equals("summon")) {
					// 座標の取得
					String coodinate = jsonObj.getString("val");
					// プレイされたカードの生成
					Card card = new Card(jsonObj.getInt("ID"),
							OnlineBattleActivity.this);
					// プレイ処理
					gm.playCard(Player.OPPONENT, coodinate, card);
					if (gm.getTime() != GameModel.TimeItem.OPPONENT_COUNTER)
						gm.toOpponentMaintime();
				}
				// モンスター移動
				else if (sw.equals("mov")) {
					// 移動前座標の取得
					String coordinate1 = jsonObj.getString("mover");
					// 移動後座標の取得
					String coordinate2 = jsonObj.getString("val");
					// モンスター移動処理
					gm.move(coordinate1, coordinate2);

					gm.toOpponentActiontime();
				}
				// モンスター攻撃
				else if (sw.equals("atk")) {
					// 攻撃モンスターの座標
					String aCoordinate = jsonObj.getString("atker");
					// 防御モンスターの座標
					String bCoordinate = jsonObj.getString("blocker");
					// 攻撃処理
					gm.attack(aCoordinate, bCoordinate);
					// 攻撃モンスター
					gm.toOpponentActiontime();
				}
				// ターンエンド
				else if (sw.equals("end")) {
					gm.toOpponentEndtime();
					handler.post(new Runnable() {
						public void run() {
							AlertDialog.Builder dialog = new AlertDialog.Builder(
									OnlineBattleActivity.this);
							dialog.setTitle("あなたのターン")
									.setMessage("相手がターンを終了しました。")
									.setNegativeButton("OK", null).show();
						}
					});
				}
				// メインタイム終了
				else if (sw.equals("endMain")) {
					gm.toCountertime();
				}
				// カウンタータイム終了
				else if (sw.equals("endCounter")) {
					gm.toOpponentCounterEndtime();
				}
				// 降参
				else if (sw.equals("surrender"))
					handler.post(new Runnable() {
						public void run() {
							endGame(true, "対戦相手が降参しました。");
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
						initFlag();
					}
				});
				// pCntの更新
				postCnt = jsonObj.getInt("pcnt");
			} catch (Exception e) {
			}
		}
	}

	// ゲスト入室確認タイマータスク
	class CheckGuestTask extends TimerTask {
		private Handler handler;
		private String url;

		public CheckGuestTask(String url) {
			handler = new Handler();
			this.url = url;
		}

		@Override
		public void run() {
			HttpClient objHttp = new DefaultHttpClient();
			HttpParams params = objHttp.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 2000); // 接続のタイムアウト
			HttpConnectionParams.setSoTimeout(params, 2000); // データ取得のタイムアウト
			String sReturn = "";
			try {
				HttpGet objGet = new HttpGet(url);
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
				e.printStackTrace();
			}

			try {
				JSONObject jsonObj = new JSONObject(sReturn);
				final String guest = jsonObj.getString("guest");

				if (!guest.equals(""))
					handler.post(new Runnable() {
						public void run() {
							onFindGuest(guest);
						}
					});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 試合終了タスク
	class EndGameTask extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... contents) {
			String url = END_URL + roomId;

			HttpClient httpClient = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);

			HttpResponse res = null;

			try {
				res = httpClient.execute(post);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return Integer.valueOf(res.getStatusLine().getStatusCode());
		}
	}
}
