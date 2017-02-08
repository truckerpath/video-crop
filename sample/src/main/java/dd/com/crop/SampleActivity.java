package dd.com.crop;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.dd.crop.TextureVideoView;

public class SampleActivity extends Activity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    final int indexCropCenter = 0;
    final int indexCropTop = 1;
    final int indexCropBottom = 2;

    // Video file url
    private static final String FILE_URL = "http://www.w3schools.com/html/mov_bbb.mp4";
    private TextureVideoView mTextureVideoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initView();

        if (!isWIFIOn(getBaseContext())) {
            Toast.makeText(getBaseContext(), "You need internet connection to stream video",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void initView() {
        mTextureVideoView = (TextureVideoView) findViewById(R.id.cropTextureView);

        findViewById(R.id.btnPlay).setOnClickListener(this);
        findViewById(R.id.btnPause).setOnClickListener(this);
        findViewById(R.id.btnStop).setOnClickListener(this);


        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(mSpinnerAdapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:
                mTextureVideoView.play();
                break;
            case R.id.btnPause:
                mTextureVideoView.pause();
                break;
            case R.id.btnStop:
                mTextureVideoView.stop();
                break;
        }
    }

    public static boolean isWIFIOn(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case indexCropCenter:
                mTextureVideoView.stop();
                mTextureVideoView.setScaleType(TextureVideoView.ScaleType.CENTER_CROP);
                mTextureVideoView.setDataSource(FILE_URL);
                mTextureVideoView.play();
                break;
            case indexCropTop:
                mTextureVideoView.stop();
                mTextureVideoView.setScaleType(TextureVideoView.ScaleType.TOP);
                mTextureVideoView.setDataSource(FILE_URL);
                mTextureVideoView.play();
                break;
            case indexCropBottom:
                mTextureVideoView.stop();
                mTextureVideoView.setScaleType(TextureVideoView.ScaleType.BOTTOM);
                mTextureVideoView.setDataSource(FILE_URL);
                mTextureVideoView.play();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
