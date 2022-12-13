package cu.axel.imagecompressor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import android.os.Environment;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.graphics.Bitmap;
import java.text.DecimalFormat;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.io.ByteArrayOutputStream;
import android.graphics.Matrix;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.widget.RadioButton;
import android.graphics.Bitmap.CompressFormat;
import android.widget.SeekBar.OnSeekBarChangeListener;
import androidx.core.content.FileProvider;
import java.io.FileInputStream;
import android.content.res.AssetFileDescriptor;

public class MainActivity extends AppCompatActivity {
    private final int OPEN_REQUEST_CODE=4;
    private final int SAVE_REQUEST_CODE=6;
    private ContentResolver resolver;
    private ImageView previewIv;
    private Bitmap openBitmap;
    private TextView infoTv;
    private File compressedFile;
    private SharedPreferences sp;

    private SeekBar qualitySb, resSb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resolver = getContentResolver();
        previewIv = findViewById(R.id.preview_iv);
        infoTv = findViewById(R.id.image_info_tv);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        final RadioGroup formatGroup = findViewById(R.id.group_formats);
        qualitySb = findViewById(R.id.sb_quality);
        resSb = findViewById(R.id.sb_resolution);
        final TextView resTv = findViewById(R.id.tv_resolution);
        final TextView qualityTv = findViewById(R.id.tv_quality);

        qualitySb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    qualityTv.setText(p2 + "");
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                    compress();
                    sp.edit().putInt("quality", qualitySb.getProgress()).commit();
                }
            });
        resSb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    if (p2 == 0)
                        resSb.setProgress(1);
                    resTv.setText(resSb.getProgress() + "");
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                    compress();
                    sp.edit().putInt("resolution", resSb.getProgress()).commit();
                }
            });
        resSb.setProgress(sp.getInt("resolution", 100));
        qualitySb.setProgress(sp.getInt("quality", 60));
        RadioButton webpBtn = findViewById(R.id.btn_webp);
        RadioButton jpgBtn = findViewById(R.id.btn_jpg);
        webpBtn.setChecked(sp.getString("format", "jpg").equals("webp"));
        jpgBtn.setChecked(sp.getString("format", "jpg").equals("jpg"));

        String format = sp.getString("format", "jpg");
        webpBtn.setChecked(format.equals("webp"));
        jpgBtn.setChecked(format.equals("jpg"));

        formatGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

                @Override
                public void onCheckedChanged(RadioGroup p1, int p2) {
                    RadioButton checkedBtn = findViewById(formatGroup.getCheckedRadioButtonId());
                    sp.edit().putString("format", checkedBtn.getText().toString().toLowerCase()).commit();
                    compress();
                }
            });

    }

    public void open(View v) {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);
    }


    public void share(View v) {
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", compressedFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setData(uri);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_app)));
    }

    public void save(View v) {
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*").putExtra(Intent.EXTRA_TITLE, compressedFile.getName()), SAVE_REQUEST_CODE);
    }

    public void compress() {

        if (openBitmap == null)
            return;

        try {

            //Scale
            Bitmap bitmap = Utils.scaleBitmap(openBitmap, resSb.getProgress() * 0.01f);

            //Compress
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            String cFormat = sp.getString("format", "jpg");
            Bitmap.CompressFormat format = cFormat.equals("jpg") ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.WEBP;
            bitmap.compress(format, qualitySb.getProgress(), bytes);

            compressedFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "out." + cFormat);
            compressedFile.createNewFile();
            FileOutputStream os = new FileOutputStream(compressedFile);
            os.write(bytes.toByteArray());
            os.close();

            infoTv.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + " " + Utils.formatFileSize(compressedFile.length()));
            previewIv.setImageBitmap(BitmapFactory.decodeFile(compressedFile.getAbsolutePath()));

        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                Uri openUri = data.getData();
                resolver.takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //Read the file
                try {
                    InputStream is = resolver.openInputStream(openUri);
                    openBitmap = BitmapFactory.decodeStream(is);
                    is.close();
                    compress();
                } catch (IOException e) {}
            } else if (requestCode == SAVE_REQUEST_CODE) {     
                try {
                    byte buffer[] = new byte[1024];
                    FileInputStream is = new FileInputStream(compressedFile);
                    OutputStream os = resolver.openOutputStream(data.getData());
                    while (is.read(buffer) != -1) {
                        os.write(buffer);   
                    }
                    is.close();
                    os.close();
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    Toast.makeText(this, R.string.save_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_image:
                open(null);
                break;
            case R.id.action_about:
                showAboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(R.layout.dialog_about);
        dialog.show();
    }

}
