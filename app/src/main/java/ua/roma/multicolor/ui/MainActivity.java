package ua.roma.multicolor.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import ua.roma.multicolor.R;
import ua.roma.multicolor.view.ColorPickerView;
import ua.roma.multicolor.view.PaintView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = MainActivity.class.getCanonicalName();
    public static final int REQUEST_PERMISSION_CODE = 1;

    private Toolbar toolbar;
    private PaintView paintView;
    private SeekBar seekBar;
    private ColorPickerView colorPickerView;
    private MenuItem undo, redo, clear;
    private BottomSheetBehavior bottomSheetBehavior;
    private ImageButton black,red,green,blue;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.color_menu, menu);
        undo = menu.findItem(R.id.undo);
        redo = menu.findItem(R.id.redo);
        clear = menu.findItem(R.id.clear);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undo:
                paintView.undo();
                return true;
            case R.id.redo:
                paintView.redo();
                return true;
            case R.id.clear:
                paintView.clear();
                return true;
            case R.id.paint:
                colorPickerView.setCenterRadius(seekBar.getProgress());
                colorPickerView.setInitialColor(paintView.getCurrentColor());
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                return true;
            case R.id.saveButton:
                tryToSaveImage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    saveImage();
                }else{
                    Toast.makeText(this,"Unfortunately you don't allow to save image (",Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.black:
                colorPickerView.setInitialColor(Color.BLACK);
                break;
            case R.id.red:
                colorPickerView.setInitialColor(Color.RED);
                break;
            case R.id.green:
                colorPickerView.setInitialColor(Color.GREEN);
                break;
            case R.id.blue:
                colorPickerView.setInitialColor(Color.BLUE);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coordinator_layuot);

        black = findViewById(R.id.black);
        black.setOnClickListener(this);

        red = findViewById(R.id.red);
        red.setOnClickListener(this);

        green = findViewById(R.id.green);
        green.setOnClickListener(this);

        blue = findViewById(R.id.blue);
        blue.setOnClickListener(this);

        paintView = findViewById(R.id.paint);
        paintView.setStateListener(new PaintView.StateListener() {
            @Override
            public void onUndo(boolean undo) {
                if (MainActivity.this.undo != null)
                    MainActivity.this.undo.setEnabled(undo);
            }

            @Override
            public void onRedo(boolean redo) {
                if (MainActivity.this.redo != null)
                    MainActivity.this.redo.setEnabled(redo);
            }

            @Override
            public void onClear(boolean clear) {
                if (MainActivity.this.clear != null)
                    MainActivity.this.clear.setEnabled(!clear);
            }
        });

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_layout));

        colorPickerView = findViewById(R.id.colorPicker);
        colorPickerView.setInitialColor(paintView.getCurrentColor());
        colorPickerView.setCenterRadius(paintView.getStrokeWidth());
        colorPickerView.setListener(new ColorPickerView.OnColorChangedListener() {
            @Override
            public void colorChanged(int color) {
                paintView.setPaintColor(color);
            }
        });

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setProgress(paintView.getStrokeWidth());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG,"progress in seekBar = " + progress);
                colorPickerView.setCenterRadius(progress);
                colorPickerView.setInitialColor(paintView.getCurrentColor());
                paintView.setStrokeWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        setSupportActionBar(toolbar);
    }

    private void saveImage() {
        paintView.setDrawingCacheEnabled(true);
        final Bitmap bitmap = paintView.getDrawingCache();

        SaveTask task =  new SaveTask(bitmap);
        task.execute();
    }

    private void tryToSaveImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){

            }else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        ,REQUEST_PERMISSION_CODE);
            }
        }else{
            saveImage();
        }
    }


    private class SaveTask extends AsyncTask<Void,Void,String>{
        private Bitmap bitmap;

        public SaveTask(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                File f = null;
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File file = new File(Environment.getExternalStorageDirectory()
                            ,getString(R.string.app_name));
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    f = new File(file.getAbsolutePath()
                            + file.separator
                            + "image"
                            +  System.currentTimeMillis()
                            + ".png");
                }
                FileOutputStream ostream = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
                ostream.flush();
                ostream.close();
                return "image was saved";
            } catch (Exception e) {
                e.printStackTrace();
                return "failure";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT).show();
            paintView.setDrawingCacheEnabled(false);
        }
    }
}
