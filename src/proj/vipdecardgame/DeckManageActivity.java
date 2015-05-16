package proj.vipdecardgame;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import proj.vipdecardgame.model.Card;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DeckManageActivity extends ActionBarActivity implements
		OnItemClickListener, View.OnLongClickListener {

	private static final int MONSTER_NUM = 64;
	private static final int MAGIC_NUM = 20012;
	private static final String DECK_FILE_NAME = "deck_data.dat";

	public static DeckData deck;
	public ArrayList<String> cardIdList;

	int focusTab = 1;
	GridView gridView;

	boolean saveFlag = false;
	int sortItem = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_deckmanage);

		ActionBar ab = this.getSupportActionBar();
		ab.setTitle("デッキ管理");

		gridView = (GridView) findViewById(R.id.card_list);
		gridView.setOnItemClickListener(this);

		Button reset = (Button) this.findViewById(R.id.reset);
		reset.setOnLongClickListener(this);

		initCardIdList();

		if (getFileStreamPath(DECK_FILE_NAME).exists()) {
			try {
				FileInputStream fis = openFileInput(DECK_FILE_NAME);
				ObjectInputStream ois = new ObjectInputStream(fis);
				deck = (DeckData) ois.readObject();
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			deck = new DeckData();
			for (int i = 1; i <= 20; i++)
				deck.cardIDStrings.add("" + i);
		}

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		TabListener tabListener = new DeckManageTabListener();

		actionBar.addTab(actionBar.newTab().setText("デッキ").setTag("1")
				.setTabListener(tabListener));

		actionBar.addTab(actionBar.newTab().setText("カード一覧").setTag("2")
				.setTabListener(tabListener));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (saveFlag)
			try {
				FileOutputStream fos = openFileOutput(DECK_FILE_NAME,
						MODE_PRIVATE);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(deck);
				oos.close();
				Toast.makeText(this, "編集を保存しました。", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		// 戻るボタンが押されたとき
		if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			// ボタンが押されたとき
			if (e.getAction() == KeyEvent.ACTION_DOWN) {
				if (deck.cardIDStrings.size() < 20) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(this);
					dialog.setMessage("デッキ枚数が不足しています。編集内容を保存せずに終了しますか？")
							.setNegativeButton("キャンセル", null)
							.setPositiveButton("保存せずに終了",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											saveFlag = false;
											finish();
										}
									}).show();
				} else {
					saveFlag = true;
					finish();
				}
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.deckmanage, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		Toast.makeText(this, "開発中やで。", Toast.LENGTH_SHORT).show();
		// switch (id) {
		// case R.id.action_editprofile:
		// break;
		// case R.id.action_about:
		// break;
		// }
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		final int selectPosition = position;
		Card card = null;
		switch (focusTab) {
		case 1:
			card = new Card(Integer.parseInt(deck.cardIDStrings.get(position)),
					this);
			break;
		case 2:
			card = new Card(Integer.parseInt(cardIdList.get(position)), this);
			break;
		}
		Log.d("card.id = ", "" + card.id);
		int resId = Card.getCardImgId(card.id, this);
		ImageView iv = new ImageView(this);
		iv.setImageResource(resId);
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("カード詳細").setView(iv).setNegativeButton("とじる", null);
		switch (focusTab) {
		case 1:
			dialog.setPositiveButton("デッキから削除",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							deck.cardIDStrings.remove(selectPosition);
							updateView();
						}
					});
			break;
		case 2:
			dialog.setPositiveButton("デッキに追加",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							addDeck(selectPosition);
						}
					});
			break;
		}
		dialog.show();
	}

	@Override
	public boolean onLongClick(View sender) {
		int senderId = sender.getId();
		switch (senderId) {
		case R.id.reset:
			deck = new DeckData();
			updateView();
			Toast.makeText(this, "デッキから全てのカードを削除しました。", Toast.LENGTH_SHORT)
					.show();
			break;
		}
		return true;
	}

	public void initCardIdList() {
		cardIdList = new ArrayList<String>();
		// モンスター
		for (int i = 1; i <= MONSTER_NUM; i++)
			cardIdList.add("" + i);

		// マジック
		for (int i = 20001; i <= MAGIC_NUM; i++)
			cardIdList.add("" + i);
	}

	public void updateView() {
		switch (focusTab) {
		case 1:
			(findViewById(R.id.search)).setVisibility(View.GONE);
			(findViewById(R.id.refine)).setVisibility(View.GONE);
			(findViewById(R.id.reset)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.deck_size))
					.setText(deck.cardIDStrings.size() + "枚");
			gridView.setAdapter(new GraveItemAdapter(this, deck.cardIDStrings));
			gridView.setNumColumns(5);
			break;
		case 2:
			(findViewById(R.id.search)).setVisibility(View.VISIBLE);
			(findViewById(R.id.refine)).setVisibility(View.VISIBLE);
			(findViewById(R.id.reset)).setVisibility(View.GONE);
			((TextView) findViewById(R.id.deck_size)).setText(cardIdList.size()
					+ "種類");
			gridView.setAdapter(new GraveItemAdapter(this, cardIdList));
			gridView.setNumColumns(6);
			break;
		}
	}

	public void addDeck(int position) {
		String id = cardIdList.get(position);

		int cnt = 0;
		for (int i = 0; i < deck.cardIDStrings.size(); i++)
			if (deck.cardIDStrings.get(i).equals(id))
				cnt++;

		if (deck.cardIDStrings.size() == 20) {
			showMessageDialog("これ以上デッキにカードを追加できません。※デッキ枚数が20枚です。");
			return;
		} else if (cnt == 2) {
			showMessageDialog("すでにこのカードは２枚デッキに入っています。");
			return;
		}
		deck.cardIDStrings.add(id);
	}

	public void showMessageDialog(String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(message).setNegativeButton("閉じる", null).show();
	}

	public void onSearchClick(View v) {
		Toast.makeText(this, "まだええやろ？", Toast.LENGTH_SHORT).show();
	}

	public void onRefineClick(View v) {
		Toast.makeText(this, "まだええやろ？", Toast.LENGTH_SHORT).show();
	}

	public void onSortClick(View v) {
		String[] items = { "カードNoで", "コストで", "ATKで", "HPで" };
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("並び替え")
				.setSingleChoiceItems(items, sortItem,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								sortItem = which;
							}
						})
				.setNegativeButton("降順", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ArrayList<String> sortList = null;
						Comparator comparator = null;
						switch (sortItem) {
						case 0:
							comparator = new IdComparator();
							break;
						case 1:
							comparator = new CostComparator();
							break;
						case 2:
							comparator = new AtkComparator();
							break;
						case 3:
							comparator = new HpComparator();
							break;
						}
						switch (focusTab) {
						case 1:
							sortList = deck.cardIDStrings;
							break;
						case 2:
							sortList = cardIdList;
							break;
						}
						Collections.sort(sortList, comparator);
						Collections.reverse(sortList);
						gridView.setAdapter(new GraveItemAdapter(
								DeckManageActivity.this, sortList));
					}
				})
				.setPositiveButton("昇順", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ArrayList<String> sortList = null;
						Comparator comparator = null;
						switch (sortItem) {
						case 0:
							comparator = new IdComparator();
							break;
						case 1:
							comparator = new CostComparator();
							break;
						case 2:
							comparator = new AtkComparator();
							break;
						case 3:
							comparator = new HpComparator();
							break;
						}
						switch (focusTab) {
						case 1:
							sortList = deck.cardIDStrings;
							break;
						case 2:
							sortList = cardIdList;
							break;
						}
						Collections.sort(sortList, comparator);
						gridView.setAdapter(new GraveItemAdapter(
								DeckManageActivity.this, sortList));
					}
				}).show();
	}

	public void onResetClick(View v) {
		Toast.makeText(this, "長押しでデッキを空にします。", Toast.LENGTH_SHORT).show();
	}

	public class DeckManageTabListener implements ActionBar.TabListener {

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			String tag = tab.getTag().toString();
			focusTab = Integer.parseInt(tag);
			updateView();
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	class ViewHolder {
		ImageView iv;
	}

	class GraveItemAdapter extends ArrayAdapter<String> {
		LayoutInflater inflater;

		GraveItemAdapter(Context context, List<String> items) {
			super(context, 0, items);
			inflater = getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int id = Integer.parseInt(getItem(position));
			Card item = new Card(id, DeckManageActivity.this);

			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.card_item, null);
				holder = new ViewHolder();
				holder.iv = (ImageView) convertView;
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.iv.setImageResource(Card.getIconImgId(item.id,
					DeckManageActivity.this));

			return convertView;
		}
	}

	class IdComparator implements Comparator<String> {

		// 比較メソッド（データクラスを比較して-1, 0, 1を返すように記述する）
		public int compare(String a, String b) {
			int id1 = Integer.parseInt(a);
			int id2 = Integer.parseInt(b);

			// こうすると社員番号の昇順でソートされる
			if (id1 > id2)
				return 1;
			else if (id1 == id2)
				return 0;
			else
				return -1;
		}

	}

	class CostComparator implements Comparator<String> {

		// 比較メソッド（データクラスを比較して-1, 0, 1を返すように記述する）
		public int compare(String a, String b) {
			Card c1 = new Card(Integer.parseInt(a), DeckManageActivity.this);
			int cost1 = c1.cost;
			Card c2 = new Card(Integer.parseInt(b), DeckManageActivity.this);
			int cost2 = c2.cost;

			// こうすると社員番号の昇順でソートされる
			if (cost1 > cost2)
				return 1;
			else if (cost1 == cost2)
				return 0;
			else
				return -1;
		}

	}

	class AtkComparator implements Comparator<String> {

		// 比較メソッド（データクラスを比較して-1, 0, 1を返すように記述する）
		public int compare(String a, String b) {
			Card c1 = new Card(Integer.parseInt(a), DeckManageActivity.this);
			int atk1 = c1.atk;
			Card c2 = new Card(Integer.parseInt(b), DeckManageActivity.this);
			int atk2 = c2.atk;

			// こうすると社員番号の昇順でソートされる
			if (atk1 > atk2)
				return 1;
			else if (atk1 == atk2)
				return 0;
			else
				return -1;
		}

	}

	class HpComparator implements Comparator<String> {

		// 比較メソッド（データクラスを比較して-1, 0, 1を返すように記述する）
		public int compare(String a, String b) {
			Card c1 = new Card(Integer.parseInt(a), DeckManageActivity.this);
			int hp1 = c1.hp;
			Card c2 = new Card(Integer.parseInt(b), DeckManageActivity.this);
			int hp2 = c2.hp;

			// こうすると社員番号の昇順でソートされる
			if (hp1 > hp2)
				return 1;
			else if (hp1 == hp2)
				return 0;
			else
				return -1;
		}

	}
}