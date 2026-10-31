package computingmusic.chordapp;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import computingmusic.chord.ChordBoard;
import computingmusic.instrument.InstrumentSpec;

/** Type a chord in ABC notation, pick the instrument, and see how it can be played on one fingerboard; step through the ways, cheapest first. */
public class MainActivity extends Activity {
	private final List<InstrumentSpec> bowed = new ArrayList<InstrumentSpec>();
	private EditText chord;
	private Spinner instrument;
	private BoardView board;
	private TextView position, detail;
	private List<ChordBoard.Option> options = new ArrayList<ChordBoard.Option>();
	private int shown;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		for (InstrumentSpec s : InstrumentSpec.ALL) {
			if (s.chordOnNeighbouringStrings) {
				bowed.add(s);
			}
		}
		float dp = getResources().getDisplayMetrics().density;
		int pad = (int) (12 * dp);
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(pad, pad, pad, pad);

		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		chord = new EditText(this);
		chord.setHint("chord, e.g. gdAD");
		chord.setText("gdAD");
		chord.setSingleLine(true);
		chord.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		chord.setImeOptions(EditorInfo.IME_ACTION_DONE);
		chord.setOnEditorActionListener((v, id, e) -> {
			solve();
			return false;
		});
		row.addView(chord, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		instrument = new Spinner(this);
		List<String> names = new ArrayList<String>();
		for (InstrumentSpec s : bowed) {
			names.add(s.name);
		}
		instrument.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names));
		instrument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> a, View v, int i, long id) {
				solve();
			}

			public void onNothingSelected(AdapterView<?> a) {
			}
		});
		row.addView(instrument);
		root.addView(row);

		Button go = new Button(this);
		go.setText("Solve");
		go.setOnClickListener(v -> solve());
		root.addView(go);

		board = new BoardView(this);
		root.addView(board, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

		LinearLayout nav = new LinearLayout(this);
		nav.setOrientation(LinearLayout.HORIZONTAL);
		nav.setGravity(Gravity.CENTER);
		Button prev = new Button(this), next = new Button(this);
		prev.setText("<");
		next.setText(">");
		prev.setOnClickListener(v -> step(-1));
		next.setOnClickListener(v -> step(1));
		position = new TextView(this);
		position.setGravity(Gravity.CENTER);
		position.setTextSize(16);
		nav.addView(prev);
		nav.addView(position, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		nav.addView(next);
		root.addView(nav);

		detail = new TextView(this);
		detail.setTextSize(15);
		detail.setGravity(Gravity.CENTER);
		root.addView(detail);
		setContentView(root);
		solve();
	}

	private void step(int d) {
		if (options.isEmpty()) {
			return;
		}
		shown = (shown + d + options.size()) % options.size();
		render();
	}

	private void solve() {
		InstrumentSpec spec = bowed.get(Math.max(0, instrument.getSelectedItemPosition()));
		options = new ArrayList<ChordBoard.Option>();
		shown = 0;
		try {
			int[] midi = ChordBoard.parse(chord.getText().toString());
			if (midi.length == 0) {
				detail.setText("Type the notes of a chord.");
			} else {
				options = new ChordBoard(spec).solve(midi);
				if (options.isEmpty()) {
					detail.setText("Not playable on the " + spec.name.toLowerCase() + " (a note out of range, or beyond the hand).");
				}
			}
		} catch (IllegalArgumentException e) {
			detail.setText(e.getMessage());
		}
		render();
	}

	private void render() {
		InstrumentSpec spec = bowed.get(Math.max(0, instrument.getSelectedItemPosition()));
		if (options.isEmpty()) {
			board.show(spec, null);
			position.setText("");
			return;
		}
		ChordBoard.Option o = options.get(shown);
		board.show(spec, o);
		position.setText((shown + 1) + " of " + options.size());
		StringBuilder sb = new StringBuilder("fingers (highest string first): ");
		for (int s = spec.openPitches.length - 1; s >= 0; s--) {
			for (int k = 0; k < o.places.length; k++) {
				if (o.places[k].string == s) {
					sb.append(o.fingers[k]);
				}
			}
		}
		sb.append(String.format("\ncost %.2f  strain %.2f", o.cost, o.strain));
		if (o.allowed == 1) {
			sb.append("  (the only fingering of these heights)");
		}
		detail.setText(sb.toString());
	}
}
