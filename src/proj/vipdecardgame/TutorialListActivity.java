package proj.vipdecardgame;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TutorialListActivity extends ActionBarActivity {

	ListView listView;
	String[] tutorials;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_tutorial_list);

		ActionBar ab = this.getSupportActionBar();
		ab.setTitle("やるおと学ぶＶＮＷ");

		tutorials = getResources().getStringArray(R.array.tutorials);
		for (int i = 0; i < tutorials.length; i++)
			tutorials[i] = String.format("%1$02d.", i + 1) + tutorials[i];
		listView = (ListView) this.findViewById(R.id.list);
		listView.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, tutorials));
	}
	
	
}
