package pl.piotrskiba.teatime;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TeaInfoFragment extends Fragment {

    private int mTeaIndex;

    @BindView(R.id.tv_tea_name)
    TextView mTeaName;

    @BindView(R.id.tv_tea_description)
    TextView mTeaDescription;

    @BindView(R.id.tv_tea_brewing_instructions)
    TextView mTeaBrewingInstructions;

    public TeaInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_tea_info, container, false);

        ButterKnife.bind(this, rootView);

        populateFragment();

        return rootView;
    }

    public void setTeaIndex(int index){
        mTeaIndex = index;
    }

    private void populateFragment(){
        String tea_name = getResources().getStringArray(R.array.tea_names)[mTeaIndex];
        String tea_description = getResources().getStringArray(R.array.tea_descriptions)[mTeaIndex];
        String tea_brewing_instructions = getResources().getStringArray(R.array.tea_brewing_instructions)[mTeaIndex];

        mTeaName.setText(tea_name);
        mTeaDescription.setText(tea_description);

        mTeaBrewingInstructions.setText(tea_brewing_instructions);
    }
}